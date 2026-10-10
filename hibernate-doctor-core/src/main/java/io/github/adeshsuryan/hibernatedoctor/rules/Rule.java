package io.github.adeshsuryan.hibernatedoctor.rules;

import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.OperationEvidence;
import java.util.List;

/**
 * One diagnostic rule. Implementations must be deterministic over the same
 * {@link OperationEvidence} and must never throw on missing or partial
 * evidence — silence is better than a false positive.
 */
public interface Rule {

    /** Frozen rule id. Once published, an id never changes meaning. */
    String id();

    List<Finding> evaluate(OperationEvidence evidence, DoctorConfig config);
}
