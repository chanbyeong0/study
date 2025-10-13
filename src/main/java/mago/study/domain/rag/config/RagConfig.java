package mago.study.domain.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag.azure-openai")
@Getter
@Setter
public class RagConfig {
    
    private String apiKey;
    private String endpoint;
    private String apiVersion = "2024-12-01-preview";
    private String embeddingDeployment = "text-embedding-ada-002";
    private String chatDeployment = "gpt-4o";
    private Integer maxTokens = 1200;
    private Double temperature = 0.7;
    private Integer topK = 7;
    private Integer chunkSize = 400;
    private Integer chunkOverlap = 50;
} 