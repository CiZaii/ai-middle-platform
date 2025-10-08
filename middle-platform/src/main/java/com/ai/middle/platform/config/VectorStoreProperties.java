package com.ai.middle.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for pgvector stores.
 *
 * Allows declaring multiple logical stores (e.g., default, qa)
 * under the `vector-store` prefix in application.yml.
 */
@Data
@ConfigurationProperties(prefix = "vector-store")
public class VectorStoreProperties {

    private StoreProperties defaultStore = new StoreProperties();
    private StoreProperties qa = new StoreProperties();

    @Data
    public static class StoreProperties {
        /**
         * Fully qualified table name, e.g. public.vector_store
         */
        private String table = "public.vector_store";

        /**
         * Embedding vector dimension.
         */
        private Integer dimension = 1536;

        /**
         * Whether to create index for similarity search.
         */
        private Boolean useIndex = false;

        /**
         * Whether to create table automatically if missing.
         */
        private Boolean createTable = false;

        /**
         * Optional DataSource bean name; when empty, primary DataSource is used.
         */
        private String datasource;
    }
}
