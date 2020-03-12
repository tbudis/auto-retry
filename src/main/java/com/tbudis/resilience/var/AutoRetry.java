package com.tbudis.resilience.var;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to bind auto retry interceptor and define the auto-retry policy.
 * This anotation will be used at runtime by AutoRetryInterceptor class.
 *
 * @author titus
 * @since 15/8/17
 */
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRetry {

    /** Default max retries. */
    int DEFAULT_MAX_RETRIES = 3;

    /** Default delays. */
    int[] DEFAULT_DELAYS = {500, 500, 500};

    /**
     * Selected retry policy profile.
     */
    @Nonbinding
    RetryPolicyProfile value();

    /**
     * Whether null value is considered as a valid response.
     * If nullable is false, then auto-retry must be performed until getting non-null value.
     */
    @Nonbinding
    boolean nullable() default true;

    /**
     * Maximum retry before giving up (0 = not retrying, 1 = just one retry, n = n retries).
     */
    @Nonbinding
    int maxRetries() default DEFAULT_MAX_RETRIES;

    /**
     * Delay between invocations (in ms).
     */
    @Nonbinding
    int[] delays() default {500, 500, 500};

}
