package io.github.adeshsuryan.hibernatedoctor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One open operation boundary. Statements executed on this thread while the
 * boundary is open are grouped into it. Close always via try-with-resources
 * or {@link Doctor#check}.
 */
public final class OperationBoundary implements AutoCloseable {

    private final Scope scope;
    private final String label;
    private final boolean readOnly;
    private final long startNanos;
    private final Supplier<StatsDelta> statsSource;
    private final StatsDelta statsBefore;
    private final Map<String, StatementEvidence> statements = new LinkedHashMap<>();
    private volatile OperationEvidence evidence;
    private boolean closed;

    OperationBoundary(Scope scope, String label, boolean readOnly,
                      Supplier<StatsDelta> statsSource) {
        this.scope = scope;
        this.label = label;
        this.readOnly = readOnly;
        this.statsSource = statsSource;
        this.statsBefore = statsSource != null ? statsSource.get() : null;
        this.startNanos = System.nanoTime();
        Capture.push(this);
    }

    StatementEvidence record(String sql, long nanos) {
        if (closed) {
            return null;
        }
        String normalized = SqlNormalizer.normalize(sql);
        if (normalized.isEmpty()) {
            return null;
        }
        StatementEvidence ev;
        synchronized (statements) {
            ev = statements.computeIfAbsent(normalized, StatementEvidence::new);
        }
        ev.record(nanos);
        return ev;
    }

    /** Close the boundary and finalize the evidence. Idempotent. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Capture.pop(this);
            StatsDelta delta = null;
            if (statsSource != null && statsBefore != null) {
                StatsDelta after = statsSource.get();
                delta = new StatsDelta(
                        diff(after.flushes(), statsBefore.flushes()),
                        diff(after.entityLoads(), statsBefore.entityLoads()),
                        diff(after.collectionLoads(), statsBefore.collectionLoads()),
                        diff(after.preparedStatements(), statsBefore.preparedStatements()),
                        diff(after.connections(), statsBefore.connections()));
            }
            List<StatementEvidence> snapshot;
            synchronized (statements) {
                snapshot = new ArrayList<>(statements.values());
            }
            evidence = new OperationEvidence(scope, label, readOnly, snapshot,
                    System.nanoTime() - startNanos, delta);
        }
    }

    private static Long diff(Long after, Long before) {
        if (after == null || before == null) {
            return null;
        }
        return after - before;
    }

    /** Evidence so far; closes the boundary first if still open. */
    public OperationEvidence evidence() {
        if (!closed) {
            close();
        }
        return evidence;
    }
}
