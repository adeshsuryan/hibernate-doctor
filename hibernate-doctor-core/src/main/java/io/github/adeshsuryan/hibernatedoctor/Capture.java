package io.github.adeshsuryan.hibernatedoctor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-local dispatch from the JDBC capture layer to the innermost open
 * boundary. Near-zero cost when no boundary is open on the calling thread.
 */
public final class Capture {

    private static final ThreadLocal<Deque<OperationBoundary>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private Capture() {
    }

    static void push(OperationBoundary boundary) {
        STACK.get().push(boundary);
    }

    static void pop(OperationBoundary boundary) {
        Deque<OperationBoundary> stack = STACK.get();
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
