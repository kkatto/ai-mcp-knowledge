package com.kou.knowledge.test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ParentAwareTextSplitter {
    private final int chunkSize;          // 目标token估算值（按字符数模拟）
    private final int chunkOverlap;       // 重叠量
    private final List<String> separators;// 分割符优先级
    private final boolean keepSeparator;
    private final int minChunkChars;      // 最小字符数

    public ParentAwareTextSplitter(int chunkSize, int chunkOverlap, List<String> separators,
                                   boolean keepSeparator, int minChunkChars) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.separators = separators != null ? separators : List.of("\n\n", "\n", " ");
        this.keepSeparator = keepSeparator;
        this.minChunkChars = minChunkChars;
    }


    public List<TextChunk> split(String text) {
        // 第一轮：按最高优先级分隔符分父块
        List<TextChunk> parentChunks = splitBySeparator(text, separators.get(0), null);

        // 第二轮：对每个父块分子块
        List<TextChunk> allChunks = new ArrayList<>();
        for (TextChunk parent : parentChunks) {
            List<TextChunk> children = splitChildren(parent);
            allChunks.addAll(children);
        }
        return allChunks;
    }

    private List<TextChunk> splitBySeparator(String text, String separator, String parentId) {
        List<TextChunk> chunks = new ArrayList<>();
        String[] parts = text.split(Pattern.quote(separator));

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isBlank()) continue;

            String chunkText = keepSeparator && i > 0 ?
                    separator + parts[i] : parts[i];
            TextChunk chunk = new TextChunk(chunkText.trim());
            if (parentId != null) chunk.setParentId(parentId);
            chunks.add(chunk);
        }
        return chunks;
    }

    private List<TextChunk> splitChildren(TextChunk parent) {
        // 如果有次级分隔符，优先使用
        if (separators.size() > 1) {
            List<TextChunk> chunks = splitBySeparator(
                    parent.getText(),
                    separators.get(1),
                    parent.getId()
            );

            // 检查是否需要进一步按长度分割
            List<TextChunk> result = new ArrayList<>();
            for (TextChunk chunk : chunks) {
                if (chunk.getText().length() > chunkSize * 4) {
                    splitByLength(chunk, result);
                } else {
                    result.add(chunk);
                }
            }
            return result;
        }
        // 无次级分隔符则直接按长度分割
        else {
            List<TextChunk> chunks = new ArrayList<>();
            splitByLength(parent, chunks);
            return chunks;
        }
    }

    private void splitByLength(TextChunk chunk, List<TextChunk> output) {
        String text = chunk.getText();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize * 4, text.length());
            String subText = text.substring(start, end).trim();

            if (subText.length() >= minChunkChars) {
                TextChunk subChunk = new TextChunk(subText);
                subChunk.setParentId(chunk.getParentId());
                subChunk.getMetadata().putAll(chunk.getMetadata());
                output.add(subChunk);
            }

            start = end - chunkOverlap * 4;
        }
    }
}
