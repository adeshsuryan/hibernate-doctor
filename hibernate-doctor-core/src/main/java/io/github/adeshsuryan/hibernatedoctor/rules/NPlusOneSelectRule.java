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
 * Same SELECT shape executed many times inside one operation — the N+1
 * signature. Confidence is heuristic: the repetition pattern strongly
 * suggests lazy association loading, but only the developer can confirm
 * which association is responsible (entity attribution arrives in 0.2.0).
 */
final class NPlusOneSelectRule implements Rule {

    static final String ID = "n-plus-one-select";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (StatementEvidence s : evidence.statements()) {
            if (!s.isSelect() || s.executions() < config.nPlusOneThreshold()) {
                continue;
            }
            if (!Statements.looksLikeSingleKeySelect(s.normalizedSql())) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.HIGH,
                    Confidence.HEURISTIC,
                    s.shortSql(90) + "  x" + s.executions(),
                    "One statement shape fired " + s.executions()
                            + " times with a single-key WHERE clause — "
                            + "the classic lazy-load N+1 signature.",
                    List.of(
                            "Find the lazy association loaded inside the loop "
                                    + "(suspect: unresolved — enable attribution when available).",
                            "Try JOIN FETCH, an entity graph, a DTO projection, "
                                    + "or @BatchSize.",
                            "Pin this operation with a query-budget test.")));
        }
        return out;
    }
}
