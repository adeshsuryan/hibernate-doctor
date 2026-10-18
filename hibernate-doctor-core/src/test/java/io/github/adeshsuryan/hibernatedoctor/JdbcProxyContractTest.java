package io.github.adeshsuryan.hibernatedoctor;

import io.github.adeshsuryan.hibernatedoctor.jdbc.DoctorDataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Deque;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JDBC contract guarantees of the proxy layer: exception fidelity,
 * Object-method identity, Wrapper delegation, batch capture, and
 * zero ThreadLocal residue on threads that never open a boundary.
 */
class JdbcProxyContractTest {

    private DoctorDataSource ds;

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:contract" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        h2.setUser("sa");
        ds = DoctorDataSource.wrap(h2);
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE item (id INT PRIMARY KEY)");
            st.execute("INSERT INTO item VALUES (999)");
        }
    }

    @Test
    void sqlExceptionsPassThroughUnwrapped() {
        // H2 validates at prepare time — either phase may throw, but the
        // caller must receive the driver's SQLException, never a wrapper.
        SQLException thrown = assertThrows(SQLException.class, () -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM missing_table")) {
                ps.executeQuery();
            }
        });
        assertNotNull(thrown.getMessage());
    }

    @Test
    void proxyPreservesIdentityAndWrapperContract() throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM item");
             PreparedStatement other = c.prepareStatement("SELECT * FROM item")) {
            assertTrue(ps.equals(ps), "proxy must equal itself");
            assertFalse(ps.equals(other), "distinct proxies must not be equal");
            assertEquals(System.identityHashCode(ps), ps.hashCode());
            assertNotNull(ps.toString());
            assertSame(ps, ps.unwrap(PreparedStatement.class), "unwrap to own interface returns proxy");
            assertTrue(ps.isWrapperFor(PreparedStatement.class));
            assertNotNull(c.unwrap(java.sql.Connection.class));
        }
    }

    @Test
    void executeBatchOnPreparedStatementIsCaptured() throws Exception {
        Doctor.Report report = Doctor.get().check("batch", () -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement("INSERT INTO item VALUES (?)")) {
                for (int i = 1; i <= 3; i++) {
                    ps.setInt(1, i);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(1, report.evidence().statements().size());
        StatementEvidence ev = report.evidence().statements().get(0);
        assertTrue(ev.normalizedSql().toLowerCase().contains("insert into item"));
        assertEquals(3, ev.rowsReturned());
    }

    @Test
    void executeBatchOnPlainStatementIsCaptured() throws Exception {
        Doctor.Report report = Doctor.get().check("stmt batch", () -> {
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                st.addBatch("INSERT INTO item VALUES (10)");
                st.addBatch("INSERT INTO item VALUES (11)");
                st.executeBatch();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(report.evidence().totalExecutions() >= 1);
        assertTrue(report.evidence().statements().stream()
                .anyMatch(s -> s.normalizedSql().toLowerCase().contains("insert into item")));
    }

    @Test
    void rowsReportedWhenStatementClosesWithoutResultSetClose() throws Exception {
        Doctor.Report report = Doctor.get().check("stmt-close", () -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM item UNION ALL SELECT * FROM item")) {
                ResultSet rs = ps.executeQuery();
                rs.next(); // read one row, deliberately leave the ResultSet unclosed
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(report.evidence().statements().get(0).rowsReturned() >= 1);
    }

    @Test
    void coldThreadsLeaveNoThreadLocalResidue() throws Exception {
        Thread worker = new Thread(() -> {
            assertFalse(Capture.isActive());
            assertNull(Capture.record("SELECT 1", 1));
            try {
                Field stack = Capture.class.getDeclaredField("STACK");
                stack.setAccessible(true);
                @SuppressWarnings("unchecked")
                ThreadLocal<Deque<OperationBoundary>> tl =
                        (ThreadLocal<Deque<OperationBoundary>>) stack.get(null);
                assertNull(tl.get(), "cold-path access must not bind a deque");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
        worker.start();
        worker.join();
    }
}
