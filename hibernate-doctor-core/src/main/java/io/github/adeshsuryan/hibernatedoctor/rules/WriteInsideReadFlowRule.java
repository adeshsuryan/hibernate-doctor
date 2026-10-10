package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.Confidence;
import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatedoctor.Severity;
import io.github.adeshsuryan.hibernatedoctor.StatementEvidence;
import java.util.ArrayList;
import java.util.List;

/** Write statements observed inside a boundary marked read-only. */
final class WriteInsideReadFlowRule implements Rule {

    static final String ID = "write-inside-read-flow";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        if (!evidence.readOnly()) {
            return List.of();
        }
        List<Finding> out = new ArrayList<>();
        for (StatementEvidence s : evidence.statements()) {
            if (!s.isWrite()) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.LOW,
                    Confidence.EXACT,
                    s.shortSql(90) + "  x" + s.executions(),
                    "Writes inside a read-marked operation force real "
                            + "transactions and can silently change data on "
                            + "paths expected to be safe.",
                    List.of(
                            "Confirm the write is intentional.",
                            "Mark the boundary as read-write if mutation is "
                                    + "expected.")));
        }
        return out;
    }
}
