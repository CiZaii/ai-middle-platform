package com.ai.middle.platform.entity.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("Entity")
public class EntityNode {
    @Id
    private String id;

    @Property("name")
    private String name;

    @Property("type")
    private String type;

    @Property("attributes")
    private Map<String, Object> attributes;

    @Property("relationType")
    private String relationType;
}
