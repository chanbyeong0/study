package mago.study.domain.rag.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;

@Data
@Builder
@Document(collection = "vector_embeddings")  
public class VectorEmbedding {
    
    @Id
    private String id;
    
    @Indexed
    private String character;
    
    private String textContent;
    
    private List<Float> embedding;
    
    private String documentId;
    
    private Integer chunkIndex;
}