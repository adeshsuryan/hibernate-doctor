package io.github.adeshsuryan.hibernatedoctor.report;

import io.github.adeshsuryan.hibernatedoctor.Doctor;
import io.github.adeshsuryan.hibernatedoctor.Finding;
import io.github.adeshsuryan.hibernatedoctor.StatementEvidence;

/** Machine-readable report. {@code schemaVersion} is stable-additive. */
public final class JsonReportRenderer {

    public static final int SCHEMA_VERSION = 1;

    private JsonReportRenderer() {
    }

    public static String render(Doctor.Report report) {
        var ev = report.evidence();
        StringBuilder out = new StringBuilder(1024);
        out.append("{\n  \"schemaVersion\": ").append(SCHEMA_VERSION).append(",\n");
        out.append("  \"operation\": {\n");
        out.append("    \"label\": ").append(quote(ev.label())).append(",\n");
        out.append("    \"scope\": ").append(quote(ev.scope().name())).append(",\n");
        out.append("    \"readOnly\": ").append(ev.readOnly()).append(",\n");
        out.append("    \"statementsExecuted\": ").append(ev.totalExecutions()).append(",\n");
        out.append("    \"wallTimeMs\": ").append(ev.wallTimeNanos() / 1_000_000).append(",\n");
        out.append("    \"dbTimeMs\": ").append(ev.dbTimeNanos() / 1_000_000).append('\n');
        out.append("  },\n");
        out.append("  \"statements\": [");
        for (int i = 0; i < ev.statements().size(); i++) {
            StatementEvidence s = ev.statements().get(i);
            if (i > 0) {
                out.append(',');
            }
            out.append("\n    {")
                    .append("\"fingerprint\": ").append(quote(s.fingerprint())).append(", ")
                    .append("\"normalizedSql\": ").append(quote(s.normalizedSql())).append(", ")
                    .append("\"executions\": ").append(s.executions()).append(", ")
                    .append("\"maxMs\": ").append(s.maxNanos() / 1_000_000).append(", ")
                    .append("\"rowsReturned\": ").append(s.rowsReturned())
                    .append('}');
        }
        out.append("\n  ],\n  \"findings\": [");
        for (int i = 0; i < report.findings().size(); i++) {
            Finding f = report.findings().get(i);
            if (i > 0) {
                out.append(',');
            }
            out.append("\n    {")
                    .append("\"ruleId\": ").append(quote(f.ruleId())).append(", ")
                    .append("\"severity\": ").append(quote(f.severity().name())).append(", ")
                    .append("\"confidence\": ").append(quote(f.confidence().name())).append(", ")
                    .append("\"evidence\": ").append(quote(f.evidence())).append(", ")
                    .append("\"why\": ").append(quote(f.why())).append(", ")
                    .append("\"investigate\": [");
            for (int j = 0; j < f.investigate().size(); j++) {
                if (j > 0) {
                    out.append(", ");
                }
                out.append(quote(f.investigate().get(j)));
            }
            out.append("]}");
        }
        out.append("\n  ]\n}\n");
        return out.toString();
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder(s.length() + 2);
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        b.append('"');
        return b.toString();
    }
}
