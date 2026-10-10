package io.github.adeshsuryan.hibernatedoctor;

/**
 * Thresholds and toggles for rule evaluation. All values have documented
 * defaults; override via the builder or Spring properties.
 */
public final class DoctorConfig {

    public static final int DEFAULT_N_PLUS_ONE_THRESHOLD = 3;
    public static final int DEFAULT_QUERY_COUNT_BUDGET = 50;
    public static final long DEFAULT_SLOW_QUERY_NANOS = 500_000_000L; // 500 ms
    public static final int DEFAULT_UNBOUNDED_ROWS = 1000;
    public static final int DEFAULT_DUPLICATE_THRESHOLD = 3;
    public static final int DEFAULT_FLUSH_THRESHOLD = 5;
    public static final int DEFAULT_BATCH_SHAPE_THRESHOLD = 10;

    private final int nPlusOneThreshold;
    private final int queryCountBudget;
    private final long slowQueryNanos;
    private final int unboundedRows;
    private final int duplicateThreshold;
    private final int flushThreshold;
    private final int batchShapeThreshold;
    private final boolean jdbcBatchConfigured;

    private DoctorConfig(Builder b) {
        this.nPlusOneThreshold = b.nPlusOneThreshold;
        this.queryCountBudget = b.queryCountBudget;
        this.slowQueryNanos = b.slowQueryNanos;
        this.unboundedRows = b.unboundedRows;
        this.duplicateThreshold = b.duplicateThreshold;
        this.flushThreshold = b.flushThreshold;
        this.batchShapeThreshold = b.batchShapeThreshold;
        this.jdbcBatchConfigured = b.jdbcBatchConfigured;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DoctorConfig defaults() {
        return builder().build();
    }

    public int nPlusOneThreshold() {
        return nPlusOneThreshold;
    }

    public int queryCountBudget() {
        return queryCountBudget;
    }

    public long slowQueryNanos() {
        return slowQueryNanos;
    }

    public int unboundedRows() {
        return unboundedRows;
    }

    public int duplicateThreshold() {
        return duplicateThreshold;
    }

    public int flushThreshold() {
        return flushThreshold;
    }

    public int batchShapeThreshold() {
        return batchShapeThreshold;
    }

    public boolean jdbcBatchConfigured() {
        return jdbcBatchConfigured;
    }

    public static final class Builder {
        private int nPlusOneThreshold = DEFAULT_N_PLUS_ONE_THRESHOLD;
        private int queryCountBudget = DEFAULT_QUERY_COUNT_BUDGET;
        private long slowQueryNanos = DEFAULT_SLOW_QUERY_NANOS;
        private int unboundedRows = DEFAULT_UNBOUNDED_ROWS;
        private int duplicateThreshold = DEFAULT_DUPLICATE_THRESHOLD;
        private int flushThreshold = DEFAULT_FLUSH_THRESHOLD;
        private int batchShapeThreshold = DEFAULT_BATCH_SHAPE_THRESHOLD;
        private boolean jdbcBatchConfigured = false;

        public Builder nPlusOneThreshold(int v) {
            this.nPlusOneThreshold = v;
            return this;
        }

        public Builder queryCountBudget(int v) {
            this.queryCountBudget = v;
            return this;
        }

        public Builder slowQueryNanos(long v) {
            this.slowQueryNanos = v;
            return this;
        }

        public Builder unboundedRows(int v) {
            this.unboundedRows = v;
            return this;
        }

        public Builder duplicateThreshold(int v) {
            this.duplicateThreshold = v;
            return this;
        }

        public Builder flushThreshold(int v) {
            this.flushThreshold = v;
            return this;
        }

        public Builder batchShapeThreshold(int v) {
            this.batchShapeThreshold = v;
            return this;
        }

        public Builder jdbcBatchConfigured(boolean v) {
            this.jdbcBatchConfigured = v;
            return this;
        }

        public DoctorConfig build() {
            return new DoctorConfig(this);
        }
    }
}
