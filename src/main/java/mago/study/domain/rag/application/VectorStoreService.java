package mago.study.domain.rag.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {
    
    private final MongoVectorStoreService mongoVectorStoreService;
    
    public void createEmbeddingsForCharacter(String character) {
        mongoVectorStoreService.createEmbeddingsForCharacter(character);
    }
    
    public List<String> searchSimilarContent(String character, String query) {
        return mongoVectorStoreService.searchSimilarContent(character, query);
    }
    
    public boolean hasEmbeddingsForCharacter(String character) {
        return mongoVectorStoreService.hasEmbeddingsForCharacter(character);
    }
    
    public void removeEmbeddingsForCharacter(String character) {
        mongoVectorStoreService.removeEmbeddingsForCharacter(character);
    }
} 