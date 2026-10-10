package io.github.adeshsuryan.hibernatedoctor.junit;

import java.util.List;

/**
 * Limits an operation must stay within. At minimum {@link #maxQueries(int)}
 * is the one every query-budget test should pin.
 */
public final class Budget {

    private final Integer maxQueries;
    private final Long maxDbTimeMs;
    private final List<String> forbidRules;

    private Budget(Integer maxQueries, Long maxDbTimeMs, List<String> forbidRules) {
        this.maxQueries = maxQueries;
        this.maxDbTimeMs = maxDbTimeMs;
        this.forbidRules = forbidRules == null ? List.of() : List.copyOf(forbidRules);
    }

    public static Budget maxQueries(int value) {
        return new Budget(value, null, null);
    }

    public static Budget none() {
        return new Budget(null, null, null);
    }

    public Budget maxDbTimeMs(long value) {
        return new Budget(maxQueries, value, forbidRules);
    }

    public Budget forbidRules(String... ruleIds) {
        return new Budget(maxQueries, maxDbTimeMs, List.of(ruleIds));
    }

    public Integer maxQueries() {
        return maxQueries;
    }

    public Long maxDbTimeMs() {
        return maxDbTimeMs;
    }

    public List<String> forbidRules() {
        return forbidRules;
    }
}
