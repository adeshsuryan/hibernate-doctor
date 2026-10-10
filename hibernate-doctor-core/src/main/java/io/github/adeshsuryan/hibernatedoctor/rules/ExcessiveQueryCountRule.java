package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.Confidence;
import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatedoctor.Severity;
import java.util.List;

/** Total statements inside one operation exceed the configured budget. */
final class ExcessiveQueryCountRule implements Rule {

    static final String ID = "excessive-query-count";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        int total = evidence.totalExecutions();
        if (total <= config.queryCountBudget()) {
            return List.of();
        }
        return List.of(new Finding(
                ID,
                Severity.HIGH,
                Confidence.EXACT,
                total + " statements in operation '" + evidence.label()
                        + "' (budget: " + config.queryCountBudget() + ")",
                "Statement count is a measured fact; the cause is usually "
                        + "repeated lazy loads, per-item processing, or an "
                        + "over-wide use case.",
                List.of(
                        "Group the repeated statements — an N+1 usually dominates.",
                        "Check for per-item service/repository calls in a loop.",
                        "Set an explicit query budget for this operation.")));
    }
}
