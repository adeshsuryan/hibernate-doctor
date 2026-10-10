package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.Confidence;
import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatedoctor.Severity;
import io.github.adeshsuryan.hibernatedoctor.StatsDelta;
import java.util.List;

/**
 * More flushes than the threshold inside one operation. Silent when no
 * ORM statistics source is configured — never guesses.
 */
final class FlushStormRule implements Rule {

    static final String ID = "flush-storm";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config) {
        StatsDelta delta = evidence.statsDelta();
        if (delta == null || delta.flushes() == null
                || delta.flushes() <= config.flushThreshold()) {
            return List.of();
        }
        return List.of(new Finding(
                ID,
                Severity.LOW,
                Confidence.EXACT,
                delta.flushes() + " flushes in operation '" + evidence.label() + "'",
                "Repeated flushes usually mean interleaved reads and writes "
                        + "inside one persistence context.",
                List.of(
                        "Reorder work: reads first, writes after.",
                        "Use FlushMode.COMMIT on read-only bulk scans.",
                        "Batch writes to flush once.")));
    }
}
