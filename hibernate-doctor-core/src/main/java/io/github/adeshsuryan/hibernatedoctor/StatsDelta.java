package io.github.adeshsuryan.hibernatedoctor;

/**
 * Optional ORM-level counter deltas measured inside an operation boundary
 * (e.g. Hibernate {@code Statistics} snapshot before and after).
 * Null-able fields mean the source was unavailable and dependent rules
 * stay silent rather than guess.
 */
public record StatsDelta(
        Long flushes,
        Long entityLoads,
        Long collectionLoads,
        Long preparedStatements,
        Long connections) {

    public static StatsDelta empty() {
        return new StatsDelta(0L, 0L, 0L, 0L, 0L);
    }
}
