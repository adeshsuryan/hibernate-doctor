package io.github.adeshsuryan.hibernatedoctor.report;

import io.github.adeshsuryan.hibernatedoctor.Doctor;
import io.github.adeshsuryan.hibernatedoctor.Finding;

/** Human-readable report. Deterministic for identical evidence. */
public final class TextReportRenderer {

    private TextReportRenderer() {
    }

    public static String render(Doctor.Report report) {
        var ev = report.evidence();
        StringBuilder out = new StringBuilder(512);
        out.append("Hibernate doctor\n\n");
        out.append("OPERATION  ").append(ev.label())
                .append(" (").append(ev.scope().name().toLowerCase()).append(" scope)\n");
        out.append("Statements executed: ").append(ev.totalExecutions())
                .append("   wall time: ").append(ev.wallTimeNanos() / 1_000_000)
                .append(" ms   db time: ").append(ev.dbTimeNanos() / 1_000_000)
                .append(" ms\n\n");
        if (report.findings().isEmpty()) {
            out.append("No findings.\n");
            return out.toString();
        }
        out.append("FINDINGS\n");
        for (Finding f : report.findings()) {
            out.append(pad(f.severity().name(), 4)).append("  ")
                    .append(pad(f.ruleId(), 24)).append("  ")
                    .append(f.evidence()).append('\n');
            out.append("      confidence: ")
                    .append(f.confidence().name().toLowerCase())
                    .append("   ").append(f.why()).append('\n');
        }
        out.append("\nINVESTIGATE\n");
        int i = 1;
        for (Finding f : report.findings()) {
            for (String step : f.investigate()) {
                out.append(i++).append(". ").append(step).append('\n');
            }
        }
        return out.toString();
    }

    private static String pad(String s, int width) {
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }
}
