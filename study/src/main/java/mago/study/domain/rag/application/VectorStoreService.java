package mago.study.domain.rag.application;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mago.study.domain.rag.config.RagConfig;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {
    
    private final EmbeddingModel embeddingModel;
    private final RagConfig ragConfig;
    
    // 캐릭터별 임베딩 스토어 (실제 프로덕션에서는 Redis나 외부 벡터 DB 사용)
    private final Map<String, InMemoryEmbeddingStore<TextSegment>> characterStores = new HashMap<>();
    
    public void createEmbeddingsForCharacter(String character, List<String> documents) {
        log.info("Creating embeddings for character: {}", character);
        
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        
        // 문서 분할
        DocumentSplitter splitter = DocumentSplitters.recursive(
                ragConfig.getChunkSize(),
                ragConfig.getChunkOverlap()
        );
        
        List<TextSegment> segments = new ArrayList<>();
        
        for (String docContent : documents) {
            Document document = Document.from(docContent);
            List<TextSegment> docSegments = splitter.split(document);
            segments.addAll(docSegments);
        }
        
        log.info("Created {} segments for character: {}", segments.size(), character);
        
        // 임베딩 생성 및 저장
        if (!segments.isEmpty()) {
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
            characterStores.put(character, embeddingStore);
            log.info("Successfully stored embeddings for character: {}", character);
        }
    }
    
    public List<String> searchSimilarContent(String character, String query) {
        InMemoryEmbeddingStore<TextSegment> store = characterStores.get(character);
        if (store == null) {
            log.warn("No embedding store found for character: {}", character);
            return Collections.emptyList();
        }
        
        // 쿼리 임베딩 생성
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // 유사도 검색
        List<EmbeddingMatch<TextSegment>> matches = store.findRelevant(
                queryEmbedding, 
                ragConfig.getTopK(),
                0.5 // 최소 유사도 점수
        );
        
        log.info("Found {} similar chunks for query in character: {}", matches.size(), character);
        
        return matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toList());
    }
    
    public boolean hasEmbeddingsForCharacter(String character) {
        return characterStores.containsKey(character);
    }
    
    public void removeEmbeddingsForCharacter(String character) {
        characterStores.remove(character);
        log.info("Removed embeddings for character: {}", character);
    }
} 