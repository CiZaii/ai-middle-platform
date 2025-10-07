package com.ai.middle.platform.repository.neo4j;

import com.ai.middle.platform.entity.graph.DocumentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for document nodes.
 */
@Repository
public interface DocumentNodeRepository extends Neo4jRepository<DocumentNode, String> {

    /**
     * Fetches a document node by the related file identifier.
     */
    @Query("MATCH (d:Document {id: $fileId}) RETURN d")
    DocumentNode findByFileId(@Param("fileId") String fileId);

    /**
     * Retrieves all entities related to the specified document.
     */
    @Query("MATCH (d:Document {id: $fileId})<-[:BELONGS_TO]-(e:Entity) RETURN e")
    List<Object> findEntitiesByDocumentId(@Param("fileId") String fileId);

    /**
     * Deletes the document and detaches every related node.
     */
    @Query("MATCH (d:Document {id: $fileId}) OPTIONAL MATCH (d)<-[:BELONGS_TO]-(n) DETACH DELETE d, n")
    void deleteDocumentWithRelations(@Param("fileId") String fileId);
}
