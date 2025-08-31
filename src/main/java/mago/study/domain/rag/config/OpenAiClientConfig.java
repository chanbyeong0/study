package mago.study.domain.rag.config;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OpenAiClientConfig {
    
    private final RagConfig ragConfig;
    
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Initializing Azure OpenAI Chat Model with deployment: {}", ragConfig.getChatDeployment());
        
        return AzureOpenAiChatModel.builder()
                .apiKey(ragConfig.getApiKey())
                .endpoint(ragConfig.getEndpoint())
                .deploymentName(ragConfig.getChatDeployment())
                .temperature(ragConfig.getTemperature())
                .maxTokens(ragConfig.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .build();
    }
    
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Initializing Azure OpenAI Embedding Model with deployment: {}", ragConfig.getEmbeddingDeployment());
        
        return AzureOpenAiEmbeddingModel.builder()
                .apiKey(ragConfig.getApiKey())
                .endpoint(ragConfig.getEndpoint())
                .deploymentName(ragConfig.getEmbeddingDeployment())
                .timeout(Duration.ofSeconds(60))
                .maxRetries(3)
                .build();
    }
} 