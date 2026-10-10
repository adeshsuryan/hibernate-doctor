package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.Confidence;
import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatedoctor.Severity;
import io.github.adeshsuryan.hibernatedoctor.StatementEvidence;
import java.util.ArrayList;
import java.util.List;

/**
 * Same-shape INSERT/UPDATE executed many times while JDBC batching is not
 * configured — batching would collapse the round trips.
 */
final class BatchIgnoredRule implements Rule {

    static final String ID = "batch-ignored";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        if (config.jdbcBatchConfigured()) {
            return List.of();
        }
        List<Finding> out = new ArrayList<>();
        for (StatementEvidence s : evidence.statements()) {
            if (!s.isWrite() || s.executions() < config.batchShapeThreshold()) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.LOW,
                    Confidence.HEURISTIC,
                    s.shortSql(90) + "  x" + s.executions(),
                    "Repeated same-shape writes with hibernate.jdbc.batch_size "
                            + "unset — each row is a separate round trip.",
                    List.of(
                            "Set hibernate.jdbc.batch_size (e.g. 20-50).",
                            "Check order_by_inserts/order_by_updates for "
                                    + "deadlock-safe batching.")));
        }
        return out;
    }
}
