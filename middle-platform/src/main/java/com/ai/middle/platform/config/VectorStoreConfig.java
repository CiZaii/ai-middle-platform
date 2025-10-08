package com.ai.middle.platform.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * PgVector EmbeddingStore configuration.
 *
 * Reads settings from application.yml via VectorStoreProperties and
 * exposes two stores: default and qa. The default store is marked as @Primary
 * to avoid ambiguity when injecting EmbeddingStore<TextSegment> by type.
 */
@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfig {

    private final VectorStoreProperties properties;
    private final ApplicationContext applicationContext;
    private final ObjectProvider<DataSource> dataSourceProvider;

    public VectorStoreConfig(VectorStoreProperties properties,
                             ApplicationContext applicationContext,
                             ObjectProvider<DataSource> dataSourceProvider) {
        this.properties = properties;
        this.applicationContext = applicationContext;
        this.dataSourceProvider = dataSourceProvider;
    }

    private DataSource resolveDataSource(String beanNameOrEmpty) {
        if (beanNameOrEmpty != null && !beanNameOrEmpty.isBlank()) {
            return applicationContext.getBean(beanNameOrEmpty, DataSource.class);
        }
        DataSource ds = dataSourceProvider.getIfAvailable();
        if (ds == null) {
            throw new IllegalStateException("No DataSource configured for PgVectorEmbeddingStore");
        }
        return ds;
    }

    @Bean(name = "embeddingStore")
    @Primary
    public EmbeddingStore<TextSegment> embeddingStore() {
        VectorStoreProperties.StoreProperties cfg = properties.getDefaultStore();
        DataSource dataSource = resolveDataSource(cfg.getDatasource());
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(cfg.getTable())
                .useIndex(Boolean.TRUE.equals(cfg.getUseIndex()))
                .createTable(Boolean.TRUE.equals(cfg.getCreateTable()))
                .dimension(cfg.getDimension())
                .build();
    }

    /**
     * 额外的“QA”向量库，读取 vector-store.qa 配置
     */
    @Bean(name = "embeddingStoreQa")
    public EmbeddingStore<TextSegment> embeddingStoreQa() {
        VectorStoreProperties.StoreProperties cfg = properties.getQa();
        DataSource dataSource = resolveDataSource(cfg.getDatasource());
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(cfg.getTable())
                .useIndex(Boolean.TRUE.equals(cfg.getUseIndex()))
                .createTable(Boolean.TRUE.equals(cfg.getCreateTable()))
                .dimension(cfg.getDimension())
                .build();
    }
}
