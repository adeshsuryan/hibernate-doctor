package io.github.adeshsuryan.hibernatedoctor;

import java.util.List;

/**
 * One rule result. {@code evidence} is the measured fact,
 * {@code why} explains the concern, {@code investigate} lists suggested
 * next steps. Never claims a proven root cause.
 */
public record Finding(
        String ruleId,
        Severity severity,
        Confidence confidence,
        String evidence,
        String why,
        List<String> investigate) {
}
