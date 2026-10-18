package io.github.adeshsuryan.hibernatedoctor;

import io.github.adeshsuryan.hibernatedoctor.report.JsonReportRenderer;
import io.github.adeshsuryan.hibernatedoctor.report.TextReportRenderer;
import io.github.adeshsuryan.hibernatedoctor.rules.RuleRegistry;
import java.util.List;
import java.util.function.Supplier;

/**
 * Entry point. Configure once, then open boundaries around the operations
 * you want diagnosed. A lazily-created shared instance backs the static
 * helpers; Spring users get a configured bean instead.
 */
public final class Doctor {

    private static volatile Doctor shared;

    private final DoctorConfig config;
    private final RuleRegistry rules;
    private final Supplier<StatsDelta> statsSource;

    private Doctor(DoctorConfig config, Supplier<StatsDelta> statsSource) {
        this.config = config;
        this.rules = new RuleRegistry();
        this.statsSource = statsSource;
    }

    public static Doctor get() {
        Doctor d = shared;
        if (d == null) {
            synchronized (Doctor.class) {
                d = shared;
                if (d == null) {
                    d = new Doctor(DoctorConfig.defaults(), null);
                    shared = d;
                }
            }
        }
        return d;
    }

    public static Doctor withConfig(DoctorConfig config) {
        return new Doctor(config, null);
    }

    public static Doctor withConfig(DoctorConfig config, Supplier<StatsDelta> statsSource) {
        return new Doctor(config, statsSource);
    }

    /** Only for tests and the Spring starter. */
    public static void setShared(Doctor doctor) {
        shared = doctor;
    }

    public DoctorConfig config() {
        return config;
    }

    /** Open a manual boundary. Close via try-with-resources. */
    public OperationBoundary open(String label) {
        return open(label, Scope.MANUAL, false);
    }

    public OperationBoundary open(String label, Scope scope, boolean readOnly) {
        return new OperationBoundary(scope, label, readOnly, statsSource);
    }

    /** Run an operation inside a TEST boundary; returns evaluated report. */
    public Report check(String label, Runnable operation) {
        OperationEvidence evidence;
        try (OperationBoundary b = open(label, Scope.TEST, false)) {
            operation.run();
            evidence = b.evidence();
        }
        return report(evidence);
    }

    public Report check(String label, Scope scope, boolean readOnly, Runnable operation) {
        OperationEvidence evidence;
        try (OperationBoundary b = open(label, scope, readOnly)) {
            operation.run();
            evidence = b.evidence();
        }
        return report(evidence);
    }

    /** Evaluate rules against captured evidence. */
    public Report report(OperationEvidence evidence) {
        return new Report(evidence, rules.evaluate(evidence, config));
    }

    /** An evaluated report: evidence plus findings plus renderers. */
    public record Report(OperationEvidence evidence, List<Finding> findings) {

        public Report {
            findings = findings == null ? List.of() : List.copyOf(findings);
        }

        public boolean hasFindings() {
            return !findings.isEmpty();
        }

        public String text() {
            return TextReportRenderer.render(this);
        }

        public String json() {
            return JsonReportRenderer.render(this);
        }
    }
}
