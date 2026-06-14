package transpadang.spm.transpadang_final.config;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.integration.view.spring.EnableEntityViews;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableEntityViews({
        "com.ilkeiapps.core.autolayout.entityview",
        "transpadang.spm.transpadang_final.view"
})
public class BlazePersistenceConfiguration {

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Bean
    @Lazy(false)
    public CriteriaBuilderFactory createCriteriaBuilderFactory() {
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        return config.createCriteriaBuilderFactory(entityManagerFactory);
    }

    @Bean
    @Lazy(false)
    public EntityViewManager createEntityViewManager( CriteriaBuilderFactory cbf, ObjectProvider<EntityViewConfiguration> evcProvider) {

        EntityViewConfiguration evc = evcProvider.getIfAvailable();
        if (evc == null) {
            throw new IllegalStateException("EntityViewConfiguration tidak ditemukan. Pastikan package di @EnableEntityViews sudah benar.");
        }

        return evc.createEntityViewManager(cbf);
    }
}