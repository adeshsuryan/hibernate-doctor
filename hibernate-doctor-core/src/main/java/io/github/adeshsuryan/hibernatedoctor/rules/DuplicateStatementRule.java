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
 * Same SELECT shape repeated without the single-key N+1 signature — likely
 * redundant work that a single query or cache could serve.
 */
final class DuplicateStatementRule implements Rule {

    static final String ID = "duplicate-statement";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (StatementEvidence s : evidence.statements()) {
            if (!s.isSelect()
                    || s.executions() < config.duplicateThreshold()
                    || Statements.looksLikeSingleKeySelect(s.normalizedSql())) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.MEDIUM,
                    Confidence.EXACT,
                    s.shortSql(90) + "  x" + s.executions(),
                    "The same statement shape ran " + s.executions()
                            + " times in one operation.",
                    List.of(
                            "Check whether the query can run once and be reused.",
                            "If it is an aggregate-per-item pattern, fold it into "
                                    + "the main query.")));
        }
        return out;
    }
}
