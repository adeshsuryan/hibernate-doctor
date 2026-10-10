package io.github.adeshsuryan.hibernatedoctor;

import io.github.adeshsuryan.hibernatedoctor.jdbc.DoctorDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof of the zero-dependency JDBC capture: real H2 database,
 * real PreparedStatements, no framework.
 */
class JdbcCaptureTest {

    private DataSource raw;
    private DoctorDataSource ds;

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:capture" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        h2.setUser("sa");
        raw = h2;
        ds = DoctorDataSource.wrap(raw);
        try (Connection c = raw.getConnection();
             var st = c.createStatement()) {
            st.execute("CREATE TABLE customer (id INT PRIMARY KEY, name VARCHAR(64))");
            st.execute("CREATE TABLE orders (id INT PRIMARY KEY, customer_id INT)");
            for (int i = 1; i <= 50; i++) {
                st.execute("INSERT INTO customer VALUES (" + i + ", 'c" + i + "')");
                st.execute("INSERT INTO orders VALUES (" + i + ", " + i + ")");
            }
        }
    }

    @Test
    void capturesPreparedStatementShapeAndCount() throws Exception {
        Doctor.Report report = Doctor.get().check("n+1 reproduction", () -> {
            try (Connection c = ds.getConnection()) {
                for (int i = 1; i <= 50; i++) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT * FROM customer WHERE id = ?")) {
                        ps.setInt(1, i);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                // drain
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(1, report.evidence().statements().size());
        assertEquals(50, report.evidence().statements().get(0).executions());
        assertTrue(report.hasFindings());
        assertTrue(report.findings().stream()
                .anyMatch(f -> f.ruleId().equals("n-plus-one-select")));
    }

    @Test
    void bindValuesNeverReachEvidence() throws Exception {
        Doctor.Report report = Doctor.get().check("privacy check", () -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM customer WHERE name = ?")) {
                ps.setString(1, "secret-customer-name");
                ps.executeQuery().close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        for (StatementEvidence s : report.evidence().statements()) {
            assertTrue(!s.normalizedSql().contains("secret-customer-name"));
        }
    }

    @Test
    void noBoundaryMeansNoCapture() throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM customer WHERE id = ?")) {
            ps.setInt(1, 1);
            ps.executeQuery().close();
        }
        // nothing thrown, nothing recorded anywhere — verify via fresh check
        Doctor.Report report = Doctor.get().check("empty", () -> {
        });
        assertEquals(0, report.evidence().totalExecutions());
    }

    @Test
    void concurrentBoundariesDoNotCrossContaminate() throws Exception {
        int threads = 4;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger[] counts = new AtomicInteger[threads];
        for (int t = 0; t < threads; t++) {
            counts[t] = new AtomicInteger();
            final int idx = t;
            new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    return;
                }
                Doctor.Report report = Doctor.get().check("t" + idx, () -> {
                    try (Connection c = ds.getConnection();
                         PreparedStatement ps = c.prepareStatement(
                                 "SELECT * FROM customer WHERE id = ?")) {
                        for (int i = 1; i <= 5 + idx; i++) {
                            ps.setInt(1, i);
                            ps.executeQuery().close();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                counts[idx].set(report.evidence().totalExecutions());
            }).start();
        }
        ready.await();
        go.countDown();
        Thread.sleep(500);
        for (int i = 0; i < threads; i++) {
            assertEquals(5 + i, counts[i].get(),
                    "thread " + i + " saw wrong count — boundaries leaked");
        }
    }

    @Test
    void rowsReturnedCounted() throws Exception {
        Doctor.Report report = Doctor.get().check("unbounded", () -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM customer")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // drain
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(50, report.evidence().statements().get(0).rowsReturned());
    }
}
