package io.github.adeshsuryan.hibernatedoctor.autoconfigure;

import io.github.adeshsuryan.hibernatedoctor.Doctor;
import io.github.adeshsuryan.hibernatedoctor.DoctorConfig;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.Filter;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Spring Boot auto-configuration: wraps {@link DataSource} beans in the
 * capture proxy, exposes a configured {@link Doctor}, and registers the
 * request-scope filter on servlet apps. Everything disables cleanly via
 * {@code hibernate-doctor.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass({DataSource.class, Doctor.class})
@EnableConfigurationProperties(HibernateDoctorProperties.class)
public class HibernateDoctorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Doctor hibernateDoctor(HibernateDoctorProperties props,
                           ObjectProvider<EntityManagerFactory> emfProvider,
                           Environment env) {
        boolean batchConfigured = env.containsProperty(
                "spring.jpa.properties.hibernate.jdbc.batch_size");
        DoctorConfig config = DoctorConfig.builder()
                .nPlusOneThreshold(props.getNPlusOneThreshold())
                .queryCountBudget(props.getQueryCountBudget())
                .slowQueryNanos(props.getSlowQueryMs() * 1_000_000L)
                .unboundedRows(props.getUnboundedRows())
                .duplicateThreshold(props.getDuplicateThreshold())
                .flushThreshold(props.getFlushThreshold())
                .batchShapeThreshold(props.getBatchShapeThreshold())
                .jdbcBatchConfigured(batchConfigured)
                .build();
        Doctor doctor = Doctor.withConfig(config, new HibernateStatsBridge(emfProvider));
        Doctor.setShared(doctor);
        return doctor;
    }

    @Bean
    @ConditionalOnProperty(prefix = "hibernate-doctor", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    static DoctorDataSourceBeanPostProcessor doctorDataSourceBeanPostProcessor() {
        return new DoctorDataSourceBeanPostProcessor();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(Filter.class)
    @ConditionalOnProperty(prefix = "hibernate-doctor", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<DoctorRequestFilter> doctorRequestFilter(
            Doctor doctor, HibernateDoctorProperties props) {
        FilterRegistrationBean<DoctorRequestFilter> registration =
                new FilterRegistrationBean<>(
                        new DoctorRequestFilter(doctor, props.getReportMode()));
        registration.setName("hibernateDoctorRequestFilter");
        registration.setOrder(Integer.MAX_VALUE - 100);
        return registration;
    }
}
