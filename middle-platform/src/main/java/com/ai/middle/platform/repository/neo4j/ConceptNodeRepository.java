package com.ai.middle.platform.repository.neo4j;

import com.ai.middle.platform.entity.graph.ConceptNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for concept nodes.
 */
@Repository
public interface ConceptNodeRepository extends Neo4jRepository<ConceptNode, String> {

    /**
     * Retrieves concepts filtered by type.
     */
    @Query("MATCH (c:Concept) WHERE c.type = $type RETURN c")
    List<ConceptNode> findByType(@Param("type") String type);

    /**
     * Finds related concepts ordered by relation strength.
     */
    @Query("MATCH (c1:Concept {id: $conceptId})-[r:RELATED_TO]->(c2:Concept) WHERE r.strength > $minStrength RETURN c2, r.strength AS strength ORDER BY strength DESC")
    List<Object> findRelatedConcepts(@Param("conceptId") String conceptId, @Param("minStrength") Double minStrength);

    /**
     * Performs a full-text search on concepts.
     */
    @Query("CALL db.index.fulltext.queryNodes('concept_search', $searchTerm) YIELD node, score RETURN node ORDER BY score DESC LIMIT $limit")
    List<ConceptNode> fullTextSearch(@Param("searchTerm") String searchTerm, @Param("limit") Integer limit);
}
