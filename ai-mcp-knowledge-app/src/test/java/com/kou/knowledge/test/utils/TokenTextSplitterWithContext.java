package com.kou.knowledge.test.utils;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

public class TokenTextSplitterWithContext {

    private final int chunkSize;
    private final int chunkOverlap;

    public TokenTextSplitterWithContext(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<Document> split(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            String[] tokens = tokenize(doc.getText());
            int start = 0;
            while (start < tokens.length) {
                int end = Math.min(start + chunkSize, tokens.length);
                StringBuilder chunkBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    chunkBuilder.append(tokens[i]).append(" ");
                }
                String chunkText = chunkBuilder.toString().trim();
                Document chunkDoc = new Document(chunkText);
                chunkDoc.getMetadata().putAll(doc.getMetadata());
                result.add(chunkDoc);
                start += (chunkSize - chunkOverlap);
            }
        }
        return result;
    }

    private String[] tokenize(String text) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<SegToken> segTokens = segmenter.process(text, JiebaSegmenter.SegMode.INDEX);
        return segTokens.stream().map(token -> token.word).toArray(String[]::new);
    }


}
