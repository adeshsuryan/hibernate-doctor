package io.github.adeshsuryan.hibernatedoctor.autoconfigure;

import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for hibernate-doctor. All thresholds are per-operation.
 * Bind-parameter values are never captured regardless of settings.
 */
@ConfigurationProperties(prefix = "hibernate-doctor")
public class HibernateDoctorProperties {

    /** Master switch. When false nothing is captured and the DataSource is not wrapped. */
    private boolean enabled = true;

    /** Repeated single-key SELECT executions that suggest an N+1. */
    private int nPlusOneThreshold = DoctorConfig.DEFAULT_N_PLUS_ONE_THRESHOLD;

    /** Total statements per operation before excessive-query-count fires. */
    private int queryCountBudget = DoctorConfig.DEFAULT_QUERY_COUNT_BUDGET;

    /** Slow-query threshold in milliseconds. */
    private long slowQueryMs = DoctorConfig.DEFAULT_SLOW_QUERY_NANOS / 1_000_000;

    /** Rows returned by one statement before unbounded-result-set fires. */
    private int unboundedRows = DoctorConfig.DEFAULT_UNBOUNDED_ROWS;

    /** Repeated non-N+1 SELECT executions before duplicate-statement fires. */
    private int duplicateThreshold = DoctorConfig.DEFAULT_DUPLICATE_THRESHOLD;

    /** Flush count per operation before flush-storm fires. */
    private int flushThreshold = DoctorConfig.DEFAULT_FLUSH_THRESHOLD;

    /** Repeated same-shape write executions before batch-ignored fires. */
    private int batchShapeThreshold = DoctorConfig.DEFAULT_BATCH_SHAPE_THRESHOLD;

    /** Request-scope reporting: log-on-findings, log-always, or off. */
    private String reportMode = "log-on-findings";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getNPlusOneThreshold() {
        return nPlusOneThreshold;
    }

    public void setNPlusOneThreshold(int v) {
        this.nPlusOneThreshold = v;
    }

    public int getQueryCountBudget() {
        return queryCountBudget;
    }

    public void setQueryCountBudget(int v) {
        this.queryCountBudget = v;
    }

    public long getSlowQueryMs() {
        return slowQueryMs;
    }

    public void setSlowQueryMs(long v) {
        this.slowQueryMs = v;
    }

    public int getUnboundedRows() {
        return unboundedRows;
    }

    public void setUnboundedRows(int v) {
        this.unboundedRows = v;
    }

    public int getDuplicateThreshold() {
        return duplicateThreshold;
    }

    public void setDuplicateThreshold(int v) {
        this.duplicateThreshold = v;
    }

    public int getFlushThreshold() {
        return flushThreshold;
    }

    public void setFlushThreshold(int v) {
        this.flushThreshold = v;
    }

    public int getBatchShapeThreshold() {
        return batchShapeThreshold;
    }

    public void setBatchShapeThreshold(int v) {
        this.batchShapeThreshold = v;
    }

    public String getReportMode() {
        return reportMode;
    }

    public void setReportMode(String reportMode) {
        this.reportMode = reportMode;
    }
}
