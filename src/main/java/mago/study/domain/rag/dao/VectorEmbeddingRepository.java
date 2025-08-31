package mago.study.domain.rag.dao;

import mago.study.domain.rag.domain.VectorEmbedding;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorEmbeddingRepository extends MongoRepository<VectorEmbedding, String> {
    
    List<VectorEmbedding> findByCharacter(String character);
    
    void deleteByCharacter(String character);
    
    boolean existsByCharacter(String character);
}