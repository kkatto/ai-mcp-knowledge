package com.kou.knowledge.config;

import io.micrometer.observation.ObservationRegistry;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClientBuilder;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;


@Configuration
public class OpenAIConfig {

    @Bean("tokenTextSplitter")
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }


    @Bean("openAiApi")
    public OpenAiApi openAiApi(@Value("${spring.ai.openai.base-url}") String baseUrl, @Value("${spring.ai.openai.api-key}") String apikey) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apikey)
                .build();
    }

    @Bean("openAiSimpleVectorStore")
    public SimpleVectorStore vectorStore(OpenAiApi openAiApi) {
        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi);
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * -- 删除旧的表（如果存在）
     * DROP TABLE IF EXISTS public.vector_store_openai;
     *
     * -- 创建新的表，使用UUID作为主键
     * CREATE TABLE public.vector_store_openai (
     *     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     *     content TEXT NOT NULL,
     *     metadata JSONB,
     *     embedding VECTOR(1536)
     * );
     *
     * SELECT * FROM vector_store_openai
     */
    @Bean("openAiPgVectorStore")
    public PgVectorStore openAiPgVectorStore(OpenAiApi openAiApi, JdbcTemplate jdbcTemplate) {
        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName("vector_store_openai")
                .build();
    }

    @Bean("syncMcpToolCallbackProvider")
    public SyncMcpToolCallbackProvider syncMcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
//        mcpClients.remove(0);

        // 用于记录 name 和其对应的 index
        Map<String, Integer> nameToIndexMap = new HashMap<>();
        // 用于记录重复的 index
        Set<Integer> duplicateIndices = new HashSet<>();

        // 遍历 mcpClients 列表
        for (int i = 0; i < mcpClients.size(); i++) {
            String name = mcpClients.get(i).getServerInfo().name();
            if (nameToIndexMap.containsKey(name)) {
                // 如果 name 已经存在，记录当前 index 为重复
                duplicateIndices.add(i);
            } else {
                // 否则，记录 name 和 index
                nameToIndexMap.put(name, i);
            }
        }

        // 删除重复的元素，从后往前删除以避免影响索引
        List<Integer> sortedIndices = new ArrayList<>(duplicateIndices);
        sortedIndices.sort(Collections.reverseOrder());
        for (int index : sortedIndices) {
            mcpClients.remove(index);
        }

        return new SyncMcpToolCallbackProvider(mcpClients);
    }

    @Bean("openAiChatClient")
    public ChatClient chatClient(OpenAiChatModel openAiChatModel, @Qualifier("syncMcpToolCallbackProvider") ToolCallbackProvider syncMcpToolCallbackProvider, ChatMemory chatMemory) {
        DefaultChatClientBuilder defaultChatClientBuilder = new DefaultChatClientBuilder(openAiChatModel, ObservationRegistry.NOOP, (ChatClientObservationConvention) null);
        return defaultChatClientBuilder
                .defaultTools(syncMcpToolCallbackProvider)
//       chat:
//        options:
//          model: gpt-4.1 yml配置方式可以注释掉这里的代码。
//                .defaultOptions(OpenAiChatOptions.builder()
//                        .model("gpt-4.1")
//                        .build())
                .defaultAdvisors(new PromptChatMemoryAdvisor(chatMemory))
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

}
