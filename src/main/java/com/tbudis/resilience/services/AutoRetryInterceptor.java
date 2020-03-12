package com.tbudis.resilience.services;

import com.tbudis.resilience.var.AutoRetry;
import com.tbudis.resilience.var.RetryPolicyProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.Arrays;
import java.util.Random;

/**
 * Auto retry interceptor to improve application resiliency.
 * If the target method requires transaction / db access, make sure that the method has new transaction (REQUIRES_NEW)
 * as it would be impossible to retry operation after db connection lost with existing transaction.
 * The new transaction will establish a new connection to the database.
 *
 * @author titus
 */
@AutoRetry(RetryPolicyProfile.CONSTANT_RETRY)
@Interceptor
public class AutoRetryInterceptor {

    /** Maximum random delay for RetryPolicyProfile.RANDOM_RETRY (in ms). */
    private static final int MAX_RANDOM_DELAY = 1000;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @AroundInvoke
    public Object invokeAndRetryIfNeeded(final InvocationContext invocationContext) {

        String label = String.format("%s.%s",
                invocationContext.getMethod().getDeclaringClass().getSimpleName(),
                invocationContext.getMethod().getName());

        // optional RetryPolicy annotation
        AutoRetry retryPolicy = invocationContext.getMethod().getAnnotation(AutoRetry.class);

        // extract various information from RetryPolicy annotation
        boolean nullable = extractNullableInformation(retryPolicy);
        int[] delays = extractDelaysInformation(retryPolicy, label);
        int maxRetries = delays.length;

        // repeat until successful within maxRetries boundary
        int count = 0;
        Object result = null;
        while (true) {
            try {
                // invoke the real method
                result = invocationContext.proceed();

                // result must not be null if null result is not allowed (nullable is false)
                if (result != null || nullable) {
                    break;  // success
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            if (count >= maxRetries) {
                logger.warn("{} > Retried for {} times without success. Giving up now ...", label, count);
                break;      // retry limit reached
            }

            // delay
            int delay = delays[count];
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("{} > Waiting for {} ms", label, delay);
                }
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

            // next try
            count++;
            if (logger.isInfoEnabled()) {
                logger.info("{} > Auto retrying operation #{} after delaying for {} ms...", label, count, delay);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("{} > Result: {}", label, result);
        }
        return result;
    }

    /**
     * Extract nullable flag set by caller.
     *
     * @param retryPolicy
     * @return
     */
    private boolean extractNullableInformation(AutoRetry retryPolicy) {
        if (retryPolicy == null) {
            return true;
        }

        return retryPolicy.nullable();
    }

    /**
     * For RANDOM_RETRY profile, extract delays array with the following steps:
     * 1. Use default 'maxRetries' set in the selected profile
     * 2. If caller sets 'maxRetries', override default value with this value
     * 3. Initialise arrays with 'maxRetries' length and fill it with random integers
     *
     * For other profiles, extract 'delays' array with the following steps:
     * 1. Use default 'delays' array set in the selected profile
     * 2. If caller sets 'delays' array, override default array with this value
     * 3. 'maxRetries' value will be determined from 'delays' length, hence can't be overridden
     *
     * @param retryPolicy
     * @param label
     * @return
     */
    private int[] extractDelaysInformation(AutoRetry retryPolicy, String label) {
        int[] delays;

        if (retryPolicy != null && retryPolicy.value() != null) {
            if (RetryPolicyProfile.RANDOM_RETRY == retryPolicy.value()) {
                int maxRetries = retryPolicy.value().maxRetries;
                if (AutoRetry.DEFAULT_MAX_RETRIES != retryPolicy.maxRetries()) {
                    maxRetries = retryPolicy.maxRetries();
                }

                delays = new int[retryPolicy.maxRetries()];

                Random random = new Random();
                for (int i = 0; i < delays.length; i++) {
                    delays[i] = random.nextInt(MAX_RANDOM_DELAY);
                }
            } else {
                delays = retryPolicy.value().delays;

                if (!Arrays.equals(AutoRetry.DEFAULT_DELAYS, retryPolicy.delays())) {
                    delays = retryPolicy.delays();
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("{} > RetryPolicy is not set. Defaults to {}", label, RetryPolicyProfile.CONSTANT_RETRY);
            }
            delays = RetryPolicyProfile.CONSTANT_RETRY.delays;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("{} > RetryPolicy(value={}, maxRetries={}, delays={}, nullable={})",
                    label,
                    retryPolicy != null ? retryPolicy.value() : RetryPolicyProfile.CONSTANT_RETRY,
                    delays.length,
                    delays,
                    retryPolicy != null ? retryPolicy.nullable() : true);
        }

        return delays;
    }
}
