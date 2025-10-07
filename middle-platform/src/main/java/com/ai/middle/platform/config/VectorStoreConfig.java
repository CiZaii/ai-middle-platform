package com.ai.middle.platform.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class VectorStoreConfig {

    private static final String DEFAULT_TABLE = "public.vector_store";

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(JdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            throw new IllegalStateException("No DataSource configured for PgVectorEmbeddingStore");
        }
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(DEFAULT_TABLE)
                .useIndex(false)
                .createTable(Boolean.TRUE)
                .dimension(1024)
                .build();
    }
}
