package io.github.adeshsuryan.hibernatedoctor;

import java.util.List;

/**
 * Immutable evidence captured inside one operation boundary.
 * All rules evaluate against this object.
 */
public final class OperationEvidence {

    private final Scope scope;
    private final String label;
    private final boolean readOnly;
    private final List<StatementEvidence> statements;
    private final long wallTimeNanos;
    private final StatsDelta statsDelta;

    public OperationEvidence(Scope scope, String label, boolean readOnly,
                             List<StatementEvidence> statements,
                             long wallTimeNanos, StatsDelta statsDelta) {
        this.scope = scope;
        this.label = label;
        this.readOnly = readOnly;
        this.statements = List.copyOf(statements);
        this.wallTimeNanos = wallTimeNanos;
        this.statsDelta = statsDelta;
    }

    public Scope scope() {
        return scope;
    }

    public String label() {
        return label;
    }

    public boolean readOnly() {
        return readOnly;
    }

    public List<StatementEvidence> statements() {
        return statements;
    }

    public long wallTimeNanos() {
        return wallTimeNanos;
    }

    public StatsDelta statsDelta() {
        return statsDelta;
    }

    public int totalExecutions() {
        int total = 0;
        for (StatementEvidence s : statements) {
            total += s.executions();
        }
        return total;
    }

    public long dbTimeNanos() {
        long total = 0;
        for (StatementEvidence s : statements) {
            total += s.totalNanos();
        }
        return total;
    }
}
