package io.github.adeshsuryan.hibernatedoctor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-local dispatch from the JDBC capture layer to the innermost open
 * boundary. The deque is created lazily in {@link #push} and removed again
 * when the stack empties, so threads that never open a boundary allocate
 * nothing — near-zero cost on the cold path.
 */
public final class Capture {

    private static final ThreadLocal<Deque<OperationBoundary>> STACK = new ThreadLocal<>();

    private Capture() {
    }

    static void push(OperationBoundary boundary) {
        Deque<OperationBoundary> stack = STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            STACK.set(stack);
        }
        stack.push(boundary);
    }

    static void pop(OperationBoundary boundary) {
        Deque<OperationBoundary> stack = STACK.get();
        if (stack == null) {
            return;
        }
        if (!stack.isEmpty() && stack.peek() == boundary) {
            stack.pop();
        } else {
            stack.remove(boundary);
        }
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    /** True when any boundary is open on this thread. Cheap hot-path check. */
    public static boolean isActive() {
        Deque<OperationBoundary> stack = STACK.get();
        return stack != null && !stack.isEmpty();
    }

    /**
     * Record one statement execution. Called by the JDBC proxy layer only.
     * Returns a handle the caller may update with row counts later, or null
     * when no boundary is open.
     */
    public static StatementEvidence record(String sql, long nanos) {
        Deque<OperationBoundary> stack = STACK.get();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek().record(sql, nanos);
    }
}
