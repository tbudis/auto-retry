package com.tbudis.resilience.var;

/**
 * A set of retry policy profiles.
 *
 * @author titus
 * @since 10/8/17
 */
public enum RetryPolicyProfile {

    /*
     * Possible values.
     */

    // custom
    CUSTOM_RETRY(3, 500, 500, 500),

    // retry 1 time with constant delay
    ONE_TIME_RETRY(1, 1000),

    // retry 3 times with constant delay
    CONSTANT_RETRY(3, 500, 500, 500),

    // retry 3 times with random delay
    RANDOM_RETRY(3),

    // 10, 50, 100ms, e.g. for internal API call
    RAPID_RETRY(3, 10, 50, 100),

    // 100, 500, 1000ms, e.g. for external API call
    SLOW_RETRY(3, 100, 500, 1000),

    // 10, 20, 30, 100, 500, 1000, 10000, 30000, 60000ms (10ms out to 1 minute), e.g. for database access
    SLOWING_DOWN_RETRY(9, 10, 20, 30, 100, 500, 1000, 10000, 30000, 60000);

    /*
     * Properties and constructors.
     */
    public Integer maxRetries;

    public int[] delays;

    private RetryPolicyProfile() {

    }

    private RetryPolicyProfile(Integer maxRetries, int... delays) {
        this.maxRetries = maxRetries;
        this.delays = delays;
    }
}
