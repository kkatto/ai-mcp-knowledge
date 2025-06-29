package com.kou.knowledge.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {
    private String id;
    private String text;
    private String parentId;
    private Map<String, Object> metadata;

    public TextChunk(String text) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.metadata = new HashMap<>();
    }
}
