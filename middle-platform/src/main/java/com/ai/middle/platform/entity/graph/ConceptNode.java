package com.ai.middle.platform.entity.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("Concept")
public class ConceptNode {
    @Id
    private String id;

    @Property("name")
    private String name;

    @Property("type")
    private String type;

    @Property("description")
    private String description;
}
