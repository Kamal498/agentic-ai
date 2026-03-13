package com.example.agentic_ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
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

}
