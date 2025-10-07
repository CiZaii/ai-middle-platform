package com.ai.middle.platform.client;

import com.ai.middle.platform.dto.embedding.EmbeddingReqDTO;
import com.ai.middle.platform.dto.embedding.EmbeddingRespDTO;
import com.dtflys.forest.annotation.Body;
import com.dtflys.forest.annotation.ForestClient;
import com.dtflys.forest.annotation.Header;
import com.dtflys.forest.annotation.Post;
import com.dtflys.forest.annotation.Var;

/**
 * Forest HTTP client for executing embedding requests against dynamically configured endpoints.
 */
@ForestClient
public interface EmbeddingHttpClient {

    /**
     * Create embeddings using a dynamically resolved endpoint and authorization header.
     *
     * @param url           resolved embedding endpoint URL
     * @param authorization authorization header value, for example "Bearer sk-xxx"
     * @param accept        accept header value
     * @param contentType   content type header value
     * @param request       embedding request body
     * @return response that contains embedding vectors
     */
    @Post(url = "{url}")
    EmbeddingRespDTO createEmbedding(
            @Var("url") String url,
            @Header("Authorization") String authorization,
            @Header("Accept") String accept,
            @Header("Content-Type") String contentType,
            @Body EmbeddingReqDTO request
    );
}
