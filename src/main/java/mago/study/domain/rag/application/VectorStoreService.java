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
import mago.study.domain.pdf.dao.PdfChunkRepository;
import mago.study.domain.pdf.domain.PdfChunk;
import mago.study.domain.tweet.dao.TweetTextRepository;
import mago.study.domain.rag.config.RagConfig;
import mago.study.domain.tweet.domain.TweetText;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {
    
    private final EmbeddingModel embeddingModel;
    private final RagConfig ragConfig;
    private final PdfChunkRepository pdfChunkRepository;
    private final TweetTextRepository tweetTextRepository;
    
    private final Map<String, InMemoryEmbeddingStore<TextSegment>> characterStores = new HashMap<>();
    
    public void createEmbeddingsForCharacter(String character) {
        log.info("Creating embeddings for character: {}", character);
        
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        
        List<String> documents = getDocumentsForCharacter(character);
        
        if (documents.isEmpty()) {
            log.warn("No documents found for character: {}", character);
            return;
        }
        
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
        
        if (!segments.isEmpty()) {
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
            characterStores.put(character, embeddingStore);
            log.info("Successfully stored embeddings for character: {}", character);
        }
    }
    
    private List<String> getDocumentsForCharacter(String character) {
        switch (character.toLowerCase()) {
            case "einstein":
                return pdfChunkRepository.findAll().stream()
                        .map(PdfChunk::getText)
                        .filter(Objects::nonNull)
                        .toList();
            case "trump":
                return tweetTextRepository.findAll().stream()
                        .map(TweetText::getText)
                        .filter(Objects::nonNull)
                        .toList();
            default:
                log.warn("Unknown character: {}", character);
                return Collections.emptyList();
        }
    }
    
    public List<String> searchSimilarContent(String character, String query) {
        InMemoryEmbeddingStore<TextSegment> store = characterStores.get(character);
        if (store == null) {
            createEmbeddingsForCharacter(character);
            store = characterStores.get(character);
            if (store == null) {
                log.warn("Failed to create embedding store for character: {}", character);
                return Collections.emptyList();
            }
        }
        
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        List<EmbeddingMatch<TextSegment>> matches = store.findRelevant(
                queryEmbedding, 
                ragConfig.getTopK(),
                0.5
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