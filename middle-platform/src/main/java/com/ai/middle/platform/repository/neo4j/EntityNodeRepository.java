package com.ai.middle.platform.repository.neo4j;

import com.ai.middle.platform.entity.graph.EntityNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for entity nodes.
 */
@Repository
public interface EntityNodeRepository extends Neo4jRepository<EntityNode, String> {

    /**
     * Retrieves entities filtered by type.
     */
    @Query("MATCH (e:Entity) WHERE e.type = $type RETURN e")
    List<EntityNode> findByType(@Param("type") String type);

    /**
     * Fetches relations for the specified entity.
     */
    @Query("MATCH (e1:Entity {id: $entityId})-[r:INTERACTS_WITH]->(e2:Entity) RETURN e1, r, e2")
    List<Object> findRelationsByEntityId(@Param("entityId") String entityId);

    /**
     * Performs a full-text search on entities.
     */
    @Query("CALL db.index.fulltext.queryNodes('entity_search', $searchTerm) YIELD node, score RETURN node ORDER BY score DESC LIMIT $limit")
    List<EntityNode> fullTextSearch(@Param("searchTerm") String searchTerm, @Param("limit") Integer limit);
}
