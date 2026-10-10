package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.StatementEvidence;

/** Shared predicates for statement-shape checks. */
final class Statements {

    private Statements() {
    }

    /**
     * Heuristic single-key lookup shape: a SELECT filtered by equality on a
     * bound parameter — the classic lazy-load N+1 signature.
     */
    static boolean looksLikeSingleKeySelect(String normalizedSql) {
        return normalizedSql.startsWith("select")
                && normalizedSql.contains(" where ")
                && (normalizedSql.contains(" = ?") || normalizedSql.contains("=?"));
    }

    static boolean hasPagination(String normalizedSql) {
        return normalizedSql.contains(" limit ")
                || normalizedSql.contains(" fetch first ")
                || normalizedSql.contains(" top ")
                || normalizedSql.contains(" rownum ")
                || normalizedSql.contains(" for update skip locked");
    }

    static boolean isWrite(StatementEvidence s) {
        return s.isWrite();
    }
}
