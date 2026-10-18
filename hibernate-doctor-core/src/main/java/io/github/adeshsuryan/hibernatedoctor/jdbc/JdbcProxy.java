package io.github.adeshsuryan.hibernatedoctor.jdbc;

import io.github.adeshsuryan.hibernatedoctor.Capture;
import io.github.adeshsuryan.hibernatedoctor.StatementEvidence;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JDK dynamic-proxy interception over Connection/Statement/ResultSet.
 * The SQL captured for {@code PreparedStatement} comes from
 * {@code Connection.prepareStatement(sql)} — it already contains {@code ?}
 * placeholders, so bind parameter values are structurally never visible.
 *
 * Contract notes:
 * <ul>
 *   <li>Every handler unwraps {@link InvocationTargetException} so callers
 *       receive the driver's original {@code SQLException} — required for
 *       Spring's exception translation and {@code @Transactional} rollback
 *       rules to keep working.</li>
 *   <li>{@code equals}/{@code hashCode}/{@code toString}/{@code unwrap}/
 *       {@code isWrapperFor} preserve proxy identity and JDBC {@code Wrapper}
 *       semantics.</li>
 *   <li>{@code executeBatch}/{@code executeLargeBatch} are captured so JDBC
 *       batching is visible to the rules.</li>
 * </ul>
 */
final class JdbcProxy {

    private JdbcProxy() {
    }

    static Connection connection(Connection target) {
        return (Connection) Proxy.newProxyInstance(
                JdbcProxy.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new ConnectionHandler(target));
    }

    /**
     * Handles Object-method identity and JDBC {@code Wrapper} delegation for
     * every proxied type. Returns null when the call is not one of these —
     * callers then fall through to normal handling.
     */
    private static Object identityOrWrapper(
            Object proxy, Object target, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        switch (name) {
            case "equals":
                return proxy == (args == null ? null : args[0]);
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "DoctorProxy[" + target.toString() + "]";
            case "unwrap":
                Class<?> unwrapIface = (Class<?>) args[0];
                if (unwrapIface.isInstance(proxy)) {
                    return proxy;
                }
                return method.invoke(target, args);
            case "isWrapperFor":
                Class<?> wrapIface = (Class<?>) args[0];
                return wrapIface.isInstance(proxy) || Boolean.TRUE.equals(method.invoke(target, args));
            default:
                return null;
        }
    }

    private static boolean isIdentityOrWrapperCall(String name) {
        return "equals".equals(name)
                || "hashCode".equals(name)
                || "toString".equals(name)
                || "unwrap".equals(name)
                || "isWrapperFor".equals(name);
    }

    private static final class ConnectionHandler implements InvocationHandler {
        private final Connection target;

        ConnectionHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            try {
                if (isIdentityOrWrapperCall(name)) {
                    return identityOrWrapper(proxy, target, method, args);
                }
                if ("prepareStatement".equals(name) || "prepareCall".equals(name)) {
                    String sql = args != null && args.length > 0 ? String.valueOf(args[0]) : "";
                    Object result = method.invoke(target, args);
                    return preparedStatement((PreparedStatement) result, sql);
                }
                if ("createStatement".equals(name)) {
                    Statement result = (Statement) method.invoke(target, args);
                    return statement(result);
                }
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

    private static PreparedStatement preparedStatement(PreparedStatement target, String sql) {
        InvocationHandler handler = new StatementHandler(target, sql, true);
        Class<?> iface = target instanceof CallableStatement
                ? CallableStatement.class
                : PreparedStatement.class;
        return (PreparedStatement) Proxy.newProxyInstance(
                JdbcProxy.class.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    private static Statement statement(Statement target) {
        return (Statement) Proxy.newProxyInstance(
                JdbcProxy.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                new StatementHandler(target, null, false));
    }

    private static ResultSet resultSet(ResultSet target, ResultSetHandler handler) {
        return (ResultSet) Proxy.newProxyInstance(
                JdbcProxy.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                handler);
    }

    private static final class StatementHandler implements InvocationHandler {
        private final Statement target;
        private final String preparedSql;
        private final boolean prepared;
        private final Set<String> batchSqls = new LinkedHashSet<>();
        private final List<ResultSetHandler> openResultSets = new CopyOnWriteArrayList<>();

        StatementHandler(Statement target, String preparedSql, boolean prepared) {
            this.target = target;
            this.preparedSql = preparedSql;
            this.prepared = prepared;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            try {
                if (isIdentityOrWrapperCall(name)) {
                    return identityOrWrapper(proxy, target, method, args);
                }
                if ("addBatch".equals(name)) {
                    if (!prepared && args != null && args.length >= 1) {
                        batchSqls.add(String.valueOf(args[0]));
                    }
                    return method.invoke(target, args);
                }
                if ("clearBatch".equals(name)) {
                    batchSqls.clear();
                    return method.invoke(target, args);
                }
                if ("close".equals(name) && args == null) {
                    flushOpenResultSets();
                    return method.invoke(target);
                }
                if (isBatchExecute(name) && args == null) {
                    return executeBatch(method);
                }
                boolean isExecute = prepared
                        ? args == null && isExecuteMethod(name)
                        : args != null && args.length >= 1 && isExecuteMethod(name);
                if (!isExecute) {
                    return method.invoke(target, args);
                }
                String sql = prepared ? preparedSql : String.valueOf(args[0]);
                long start = System.nanoTime();
                Object result = method.invoke(target, args);
                long nanos = System.nanoTime() - start;
                StatementEvidence ev = Capture.record(sql, nanos);
                if (result instanceof ResultSet rs && ev != null) {
                    ResultSetHandler handler = new ResultSetHandler(rs, ev);
                    openResultSets.add(handler);
                    return resultSet(rs, handler);
                }
                return result;
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        private Object executeBatch(Method method) throws Throwable {
            long start = System.nanoTime();
            Object result = method.invoke(target);
            long nanos = System.nanoTime() - start;
            long rows = batchRows(result);
            if (prepared) {
                // One execution of the prepared SQL; row count = batch size.
                StatementEvidence ev = Capture.record(preparedSql, nanos);
                if (ev != null) {
                    ev.addRows(rows);
                }
            } else {
                // Plain Statement batch: one entry per distinct added SQL keeps
                // duplicate-statement detection honest without inflating counts.
                boolean rowsReported = false;
                for (String sql : batchSqls) {
                    StatementEvidence ev = Capture.record(sql, nanos);
                    if (ev != null && !rowsReported) {
                        ev.addRows(rows);
                        rowsReported = true;
                    }
                }
                if (batchSqls.isEmpty()) {
                    Capture.record("(batch)", nanos);
                }
            }
            batchSqls.clear();
            return result;
        }

        private static long batchRows(Object result) {
            long total = 0;
            if (result instanceof long[] counts) {
                for (long c : counts) {
                    total += Math.max(0, c);
                }
            } else if (result instanceof int[] counts) {
                for (int c : counts) {
                    total += Math.max(0, c);
                }
            }
            return total;
        }

        private void flushOpenResultSets() {
            for (ResultSetHandler handler : openResultSets) {
                handler.report();
            }
            openResultSets.clear();
        }

        private static boolean isBatchExecute(String name) {
            return "executeBatch".equals(name) || "executeLargeBatch".equals(name);
        }

        private static boolean isExecuteMethod(String name) {
            return "execute".equals(name)
                    || "executeQuery".equals(name)
                    || "executeUpdate".equals(name)
                    || "executeLargeUpdate".equals(name);
        }
    }

    private static final class ResultSetHandler implements InvocationHandler {
        private final ResultSet target;
        private final StatementEvidence evidence;
        private final AtomicLong rows = new AtomicLong();
        private volatile boolean reported;

        ResultSetHandler(ResultSet target, StatementEvidence evidence) {
            this.target = target;
            this.evidence = evidence;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            try {
                if (isIdentityOrWrapperCall(name)) {
                    return identityOrWrapper(proxy, target, method, args);
                }
                if ("next".equals(name)) {
                    Object result = method.invoke(target, args);
                    if (Boolean.TRUE.equals(result)) {
                        rows.incrementAndGet();
                    }
                    return result;
                }
                if ("close".equals(name)) {
                    report();
                    return method.invoke(target, args);
                }
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        void report() {
            if (!reported) {
                reported = true;
                evidence.addRows(rows.get());
            }
        }
    }
}
