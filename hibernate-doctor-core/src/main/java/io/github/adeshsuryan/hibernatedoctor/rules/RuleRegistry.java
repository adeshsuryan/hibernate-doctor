package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.OperationEvidence;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Runs every registered rule over one operation's evidence. */
public final class RuleRegistry {

    private final List<Rule> rules = List.of(
            new NPlusOneSelectRule(),
            new ExcessiveQueryCountRule(),
            new UnboundedResultSetRule(),
            new DuplicateStatementRule(),
            new SlowQueryRule(),
            new WriteInsideReadFlowRule(),
            new FlushStormRule(),
            new BatchIgnoredRule());

    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (Rule rule : rules) {
            out.addAll(rule.evaluate(evidence, config));
        }
        out.sort(Comparator
                .comparing((Finding f) -> f.severity().ordinal())
                .reversed()
                .thenComparing(Finding::ruleId));
        return List.copyOf(out);
    }

    public List<Rule> rules() {
        return rules;
    }
}
