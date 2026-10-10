package io.github.adeshsuryan.hibernatedoctor.autoconfigure;

import io.github.adeshsuryan.hibernatedoctor.Doctor;
import io.github.adeshsuryan.hibernatedoctor.OperationBoundary;
import io.github.adeshsuryan.hibernatedoctor.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Scopes every HTTP request to an operation boundary and reports findings
 * according to {@code hibernate-doctor.report-mode}.
 */
final class DoctorRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DoctorRequestFilter.class);

    private final Doctor doctor;
    private final String reportMode;

    DoctorRequestFilter(Doctor doctor, String reportMode) {
        this.doctor = doctor;
        this.reportMode = reportMode == null ? "log-on-findings" : reportMode;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        if ("off".equalsIgnoreCase(reportMode)) {
            chain.doFilter(request, response);
            return;
        }
        String label = request.getMethod() + " " + request.getRequestURI();
        Doctor.Report report;
        try (OperationBoundary boundary =
                     doctor.open(label, Scope.REQUEST, isReadOnlyMethod(request))) {
            chain.doFilter(request, response);
            report = doctor.report(boundary.evidence());
        }
        boolean shouldLog = report.hasFindings()
                || "log-always".equalsIgnoreCase(reportMode);
        if (shouldLog) {
            log.info("\n{}", report.text());
        }
    }

    private static boolean isReadOnlyMethod(HttpServletRequest request) {
        String m = request.getMethod();
        return "GET".equals(m) || "HEAD".equals(m) || "OPTIONS".equals(m);
    }
}
