package io.github.adeshsuryan.hibernatedoctor;

/**
 * How certain the evidence is. {@code EXACT} means a measured fact;
 * {@code HEURISTIC} means a pattern strongly suggests the issue but a human
 * should verify before treating it as the cause.
 */
public enum Confidence {
    EXACT,
    HEURISTIC
}
