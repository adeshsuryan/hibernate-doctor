package io.github.adeshsuryan.hibernatedoctor;

/**
 * Measured evidence for one SQL statement shape inside an operation.
 * Bind parameter values are never captured: the SQL seen at the JDBC
 * {@code PreparedStatement} boundary already contains {@code ?} placeholders,
 * and raw literals are stripped during normalization.
 */
public final class StatementEvidence {

    private final String normalizedSql;
    private final String fingerprint;
    private int executions;
    private long totalNanos;
    private long maxNanos;
    private long rowsReturned;

    public StatementEvidence(String normalizedSql) {
        this.normalizedSql = normalizedSql;
        this.fingerprint = Integer.toHexString(normalizedSql.hashCode());
    }

    public synchronized void record(long nanos) {
        executions++;
        totalNanos += nanos;
        if (nanos > maxNanos) {
            maxNanos = nanos;
        }
    }

    public synchronized void addRows(long rows) {
        rowsReturned += rows;
    }

    public String normalizedSql() {
        return normalizedSql;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public synchronized int executions() {
        return executions;
    }

    public synchronized long totalNanos() {
        return totalNanos;
    }

    public synchronized long maxNanos() {
        return maxNanos;
    }

    public synchronized long rowsReturned() {
        return rowsReturned;
    }

    public boolean isSelect() {
        return normalizedSql.startsWith("select");
    }

    public boolean isWrite() {
        return normalizedSql.startsWith("insert")
                || normalizedSql.startsWith("update")
                || normalizedSql.startsWith("delete");
    }

    /** Truncated SQL for report lines. */
    public String shortSql(int max) {
        return normalizedSql.length() <= max
                ? normalizedSql
                : normalizedSql.substring(0, max - 3) + "...";
    }
}
