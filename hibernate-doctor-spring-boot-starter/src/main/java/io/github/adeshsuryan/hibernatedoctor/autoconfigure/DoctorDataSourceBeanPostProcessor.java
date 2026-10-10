package io.github.adeshsuryan.hibernatedoctor.autoconfigure;

import io.github.adeshsuryan.hibernatedoctor.jdbc.DoctorDataSource;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Wraps every {@link DataSource} bean in a {@link DoctorDataSource} so all
 * statement traffic is observable. This is the only bean the post-processor
 * touches; user service beans are never proxied.
 */
final class DoctorDataSourceBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if (bean instanceof DataSource ds && !(bean instanceof DoctorDataSource)) {
            return DoctorDataSource.wrap(ds);
        }
        return bean;
    }
}
