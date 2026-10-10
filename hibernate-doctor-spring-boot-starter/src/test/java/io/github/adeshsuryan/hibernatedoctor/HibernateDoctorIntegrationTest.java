package io.github.adeshsuryan.hibernatedoctor;

import io.github.adeshsuryan.hibernatedoctor.jdbc.DoctorDataSource;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack proof: real Spring Boot app, real Hibernate ORM, real H2 —
 * the auto-wrapped DataSource captures a genuine lazy-load N+1.
 */
@SpringBootTest(classes = TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:itdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "hibernate-doctor.report-mode=off"
        })
class HibernateDoctorIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Doctor doctor;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private CustomerRepository customers;

    @BeforeEach
    void seed() {
        if (orders.count() > 0) {
            return;
        }
        IntStream.rangeClosed(1, 50).forEach(i -> {
            Customer c = customers.save(new Customer(i, "customer-" + i));
            orders.save(new PurchaseOrder(i, c));
        });
    }

    @Test
    void dataSourceIsWrappedByAutoConfiguration() {
        assertInstanceOf(DoctorDataSource.class, dataSource);
    }

    @Test
    void lazyNPlusOneIsDetected() {
        Doctor.Report report = doctor.check("customer names", orderService::customerNames);
        assertTrue(report.hasFindings(), "expected findings, got:\n" + report.text());
        assertTrue(report.findings().stream()
                        .anyMatch(f -> f.ruleId().equals("n-plus-one-select")),
                "expected n-plus-one-select in:\n" + report.text());
        System.out.println(report.text());
    }

    @Test
    void cleanPathStaysSilent() {
        Doctor.Report report = doctor.check("order count", orderService::count);
        assertFalse(report.hasFindings(), "expected no findings, got:\n" + report.text());
    }
}
