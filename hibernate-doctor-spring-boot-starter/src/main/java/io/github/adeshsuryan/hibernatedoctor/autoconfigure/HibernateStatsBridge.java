package io.github.adeshsuryan.hibernatedoctor.autoconfigure;

import io.github.adeshsuryan.hibernatedoctor.StatsDelta;
import jakarta.persistence.EntityManagerFactory;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Reads Hibernate {@link Statistics} snapshots around operation boundaries.
 * Resolves the {@link EntityManagerFactory} lazily so the Doctor bean can be
 * created before JPA initialization. Silent when JPA is absent.
 */
final class HibernateStatsBridge implements Supplier<StatsDelta> {

    private final ObjectProvider<EntityManagerFactory> emfProvider;

    HibernateStatsBridge(ObjectProvider<EntityManagerFactory> emfProvider) {
        this.emfProvider = emfProvider;
    }

    @Override
    public StatsDelta get() {
        EntityManagerFactory emf = emfProvider.getIfAvailable();
        if (emf == null) {
            return null;
        }
        try {
            SessionFactory sf = emf.unwrap(SessionFactory.class);
            Statistics stats = sf.getStatistics();
            if (!stats.isStatisticsEnabled()) {
                stats.setStatisticsEnabled(true);
            }
            return new StatsDelta(
                    stats.getFlushCount(),
                    stats.getEntityLoadCount(),
                    stats.getCollectionLoadCount(),
                    stats.getPrepareStatementCount(),
                    stats.getConnectCount());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
