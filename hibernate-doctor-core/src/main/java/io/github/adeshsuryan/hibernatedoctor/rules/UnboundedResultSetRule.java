package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.Confidence;
import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatedoctor.Severity;
import io.github.adeshsuryan.hibernatedoctor.StatementEvidence;
import java.util.ArrayList;
import java.util.List;

/** A single statement returned more rows than the threshold with no LIMIT. */
final class UnboundedResultSetRule implements Rule {

    static final String ID = "unbounded-result-set";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (StatementEvidence s : evidence.statements()) {
            if (!s.isSelect()
                    || s.rowsReturned() <= config.unboundedRows()
                    || Statements.hasPagination(s.normalizedSql())) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.MEDIUM,
                    Confidence.EXACT,
                    s.shortSql(90) + "  -> " + s.rowsReturned() + " rows",
                    "A large unbounded result set inflates memory and "
                            + "persistence-context size.",
                    List.of(
                            "Add pagination (Pageable / setMaxResults).",
                            "Project only the columns needed (DTO projection).",
                            "If this is a batch job, use streamed or chunked reads.")));
        }
        return out;
    }
}
