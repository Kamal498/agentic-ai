package com.example.agentic_ai.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@Slf4j
public class AppConfig {

    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        return OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public ChatModel chatModel(OllamaApi ollamaApi,
                                @Value("${spring.ai.ollama.chat.options.model}") String model,
                                @Value("${spring.ai.ollama.chat.options.temperature}") Double temperature) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are an expert assistant for an order management system.
                        You have deep knowledge of orders, inventory, and payment processes.
                        Provide accurate, concise answers based only on the context provided.
                        When referencing order or payment details, be specific.
                        If information is not in the provided context, say so clearly.
                        """)
                .build();
    }

    @Bean
    public CommandLineRunner initializeMilvusCollection(
            MilvusServiceClient milvusClient,
            @Value("${spring.ai.vectorstore.milvus.collection-name}") String collectionName,
            @Value("${spring.ai.vectorstore.milvus.embedding-dimension}") int dimension) {
        
        return args -> {
            try {
                HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build();
                
                if (Boolean.TRUE.equals(milvusClient.hasCollection(hasCollectionParam).getData())) {
                    log.info("Milvus collection '{}' already exists, loading into memory", collectionName);
                    LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build();
                    milvusClient.loadCollection(loadCollectionParam);
                    log.info("Loaded existing collection '{}' into memory", collectionName);
                    return;
                }

                log.info("Creating Milvus collection '{}' with dimension {}", collectionName, dimension);

                FieldType idField = FieldType.newBuilder()
                        .withName("id")
                        .withDataType(DataType.Int64)
                        .withPrimaryKey(true)
                        .withAutoID(true)
                        .build();

                FieldType contentField = FieldType.newBuilder()
                        .withName("content")
                        .withDataType(DataType.VarChar)
                        .withMaxLength(4096)
                        .build();

                FieldType metadataField = FieldType.newBuilder()
                        .withName("metadata")
                        .withDataType(DataType.VarChar)
                        .withMaxLength(4096)
                        .build();

                FieldType embeddingField = FieldType.newBuilder()
                        .withName("embedding")
                        .withDataType(DataType.FloatVector)
                        .withDimension(dimension)
                        .build();

                CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withDescription("Order management RAG documents")
                        .addFieldType(idField)
                        .addFieldType(contentField)
                        .addFieldType(metadataField)
                        .addFieldType(embeddingField)
                        .build();

                milvusClient.createCollection(createCollectionParam);
                log.info("Created collection '{}'", collectionName);

                CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFieldName("embedding")
                        .withIndexType(IndexType.HNSW)
                        .withMetricType(MetricType.COSINE)
                        .withExtraParam("{\"M\": 16, \"efConstruction\": 200}")
                        .build();

                milvusClient.createIndex(createIndexParam);
                log.info("Created HNSW index on '{}' with M=16, efConstruction=200", collectionName);

                LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build();
                milvusClient.loadCollection(loadCollectionParam);
                log.info("Loaded collection '{}' into memory", collectionName);

            } catch (Exception e) {
                log.error("Failed to initialize Milvus collection: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}
