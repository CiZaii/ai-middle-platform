package com.ai.middle.platform.repository.neo4j;

import com.ai.middle.platform.entity.graph.SectionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for section nodes.
 */
@Repository
public interface SectionNodeRepository extends Neo4jRepository<SectionNode, String> {

    /**
     * Retrieves sections filtered by the provided level.
     */
    @Query("MATCH (s:Section) WHERE s.level = $level RETURN s")
    List<SectionNode> findByLevel(@Param("level") Integer level);

    /**
     * Finds the parent section of the given section identifier.
     */
    @Query("MATCH (child:Section {id: $sectionId})<-[:PARENT_OF]-(parent:Section) RETURN parent")
    SectionNode findParentSection(@Param("sectionId") String sectionId);

    /**
     * Fetches the child sections ordered by level.
     */
    @Query("MATCH (parent:Section {id: $sectionId})-[:PARENT_OF]->(child:Section) RETURN child ORDER BY child.level")
    List<SectionNode> findChildSections(@Param("sectionId") String sectionId);
}
