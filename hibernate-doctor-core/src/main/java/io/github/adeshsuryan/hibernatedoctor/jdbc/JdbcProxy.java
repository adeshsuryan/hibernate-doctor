package io.github.adeshsuryan.hibernatedoctor.jdbc;

import io.github.adeshsuryan.hibernatedoctor.Capture;
import io.github.adeshsuryan.hibernatedoctor.StatementEvidence;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JDK dynamic-proxy interception over Connection/Statement/ResultSet.
 * The SQL captured for {@code PreparedStatement} comes from
 * {@code Connection.prepareStatement(sql)} — it already contains {@code ?}
 * placeholders, so bind parameter values are structurally never visible.
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

    private static final class ConnectionHandler implements InvocationHandler {
        private final Connection target;

        ConnectionHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
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

    private static ResultSet resultSet(ResultSet target, StatementEvidence evidence) {
        return (ResultSet) Proxy.newProxyInstance(
                JdbcProxy.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new ResultSetHandler(target, evidence));
    }

    private static final class StatementHandler implements InvocationHandler {
        private final Statement target;
        private final String preparedSql;
        private final boolean prepared;

        StatementHandler(Statement target, String preparedSql, boolean prepared) {
            this.target = target;
            this.preparedSql = preparedSql;
            this.prepared = prepared;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
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
                return resultSet(rs, ev);
            }
            return result;
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
        }

        private void report() {
            if (!reported) {
                reported = true;
                evidence.addRows(rows.get());
            }
        }
    }
}
