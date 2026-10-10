package io.github.adeshsuryan.hibernatedoctor.junit;

import io.github.adeshsuryan.hibernatedoctor.Doctor;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension for query-budget assertions.
 *
 * <pre>{@code
 * @RegisterExtension
 * HibernateDoctorExtension doctor = new HibernateDoctorExtension();
 *
 * @Test
 * void orderLookupStaysInBudget() {
 *     doctor.assertBudget("order lookup",
 *             () -> orderService.findOrders(customerId),
 *             Budget.maxQueries(5));
 * }
 * }</pre>
 *
 * On violation the assertion error embeds the full text report — the
 * failure IS the diagnosis.
 */
public final class HibernateDoctorExtension implements AfterEachCallback {

    private final Doctor doctor;

    public HibernateDoctorExtension() {
        this(Doctor.get());
    }

    public HibernateDoctorExtension(Doctor doctor) {
        this.doctor = doctor;
    }

    /** Run an operation inside a TEST boundary and evaluate rules. */
    public Doctor.Report check(String label, Runnable operation) {
        return doctor.check(label, operation);
    }

    /** Assert the operation stays inside the budget; fails with the report. */
    public void assertBudget(String label, Runnable operation, Budget budget) {
        Doctor.Report report = check(label, operation);
        List<String> violations = violations(report, budget);
        if (!violations.isEmpty()) {
            throw new AssertionError(
                    "hibernate-doctor budget violation in '" + label + "':\n"
                            + String.join("\n", violations)
                            + "\n\n" + report.text());
        }
    }

    private List<String> violations(Doctor.Report report, Budget budget) {
        List<String> out = new ArrayList<>();
        if (budget.maxQueries() != null
                && report.evidence().totalExecutions() > budget.maxQueries()) {
            out.add("  statements executed " + report.evidence().totalExecutions()
                    + " > budget " + budget.maxQueries());
        }
        if (budget.maxDbTimeMs() != null
                && report.evidence().dbTimeNanos() / 1_000_000 > budget.maxDbTimeMs()) {
            out.add("  db time " + report.evidence().dbTimeNanos() / 1_000_000
                    + " ms > budget " + budget.maxDbTimeMs() + " ms");
        }
        for (Finding f : report.findings()) {
            if (budget.forbidRules().contains(f.ruleId())) {
                out.add("  forbidden rule fired: " + f.ruleId()
                        + " (" + f.evidence() + ")");
            }
        }
        return out;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Boundaries auto-close in check(); nothing to clean up here.
        // Hook kept for future per-test reporting modes.
    }
}
