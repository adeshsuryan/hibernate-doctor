package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.Confidence;
import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatedoctor.Severity;
import io.github.adeshsuryan.hibernatedoctor.StatementEvidence;
import java.util.ArrayList;
import java.util.List;

/** Any single statement slower than the configured threshold. */
final class SlowQueryRule implements Rule {

    static final String ID = "slow-query-observation";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (StatementEvidence s : evidence.statements()) {
            if (s.maxNanos() <= config.slowQueryNanos()) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.MEDIUM,
                    Confidence.EXACT,
                    s.shortSql(90) + "  max " + (s.maxNanos() / 1_000_000) + " ms",
                    "One execution exceeded the slow-query threshold.",
                    List.of(
                            "EXPLAIN the statement on production-like data.",
                            "Check index coverage on the filtered/joined columns.",
                            "Confirm the plan is stable, not parameter-sensitive.")));
        }
        return out;
    }
}
