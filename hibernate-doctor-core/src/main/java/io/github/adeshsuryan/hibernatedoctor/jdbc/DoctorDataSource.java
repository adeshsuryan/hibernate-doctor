package io.github.adeshsuryan.hibernatedoctor.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * A {@link DataSource} wrapper that proxies every {@link Connection} so
 * statement executions can be timed and grouped by shape. Read-only with
 * respect to the application: it never alters SQL, never reads bind values,
 * and adds no behavior when no operation boundary is open.
 *
 * <p>Wrap any DataSource: {@code DataSource ds = DoctorDataSource.wrap(real);}
 */
public final class DoctorDataSource implements DataSource {

    private final DataSource delegate;

    private DoctorDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    public static DoctorDataSource wrap(DataSource delegate) {
        return new DoctorDataSource(delegate);
    }

    public DataSource unwrapTarget() {
        return delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return JdbcProxy.connection(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return JdbcProxy.connection(delegate.getConnection(username, password));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }
}
