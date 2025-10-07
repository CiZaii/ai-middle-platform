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
@Node("Section")
public class SectionNode {
    @Id
    private String id;

    @Property("title")
    private String title;

    @Property("level")
    private Integer level;

    @Property("content")
    private String content;
}
