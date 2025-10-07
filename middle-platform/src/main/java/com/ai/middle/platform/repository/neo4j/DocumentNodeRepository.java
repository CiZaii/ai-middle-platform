package com.ai.middle.platform.repository.neo4j;

import com.ai.middle.platform.entity.graph.DocumentNode;
import com.ai.middle.platform.entity.graph.EntityNode;
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
     * Retrieves all entities related to the specified document via BELONGS_TO relationship.
     * Returns a projection with all entity properties explicitly mapped.
     */
    @Query("""
            MATCH (d:Document {id: $fileId})<-[:BELONGS_TO]-(e:Entity)
            RETURN e.id AS id, 
                   e.name AS name, 
                   e.type AS type, 
                   e.externalId AS externalId, 
                   e.description AS description, 
                   e.aliases AS aliases, 
                   e.sourcePages AS sourcePages, 
                   e.documentId AS documentId
            """)
    List<EntityNode> findEntitiesByDocumentId(@Param("fileId") String fileId);

    /**
     * Deletes the document and all entities belonging to it, including all relationships.
     * This ensures complete cleanup when regenerating the knowledge graph.
     * Uses DETACH DELETE to automatically remove all relationships.
     */
    @Query("""
            MATCH (d:Document {id: $fileId})
            OPTIONAL MATCH (d)<-[:BELONGS_TO]-(e:Entity)
            WITH d, collect(e) as entities
            FOREACH (entity IN entities | DETACH DELETE entity)
            DETACH DELETE d
            """)
    void deleteDocumentWithRelations(@Param("fileId") String fileId);
}
