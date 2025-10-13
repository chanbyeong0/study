package mago.study.domain.rag.application;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mago.study.domain.pdf.dao.PdfChunkRepository;
import mago.study.domain.pdf.domain.PdfChunk;
import mago.study.domain.rag.config.RagConfig;
import mago.study.domain.rag.dao.VectorEmbeddingRepository;
import mago.study.domain.rag.domain.VectorEmbedding;
import mago.study.domain.tweet.dao.TweetTextRepository;
import mago.study.domain.tweet.domain.TweetText;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MongoVectorStoreService {

    private final EmbeddingModel embeddingModel;
    private final RagConfig ragConfig;
    private final PdfChunkRepository pdfChunkRepository;
    private final TweetTextRepository tweetTextRepository;
    private final VectorEmbeddingRepository vectorEmbeddingRepository;

    public void createEmbeddingsForCharacter(String character) {
        log.info("Creating embeddings for character: {}", character);

        // 기존 임베딩 삭제
        vectorEmbeddingRepository.deleteByCharacter(character);

        List<String> documents = getDocumentsForCharacter(character);

        if (documents.isEmpty()) {
            log.warn("No documents found for character: {}", character);
            return;
        }

        DocumentSplitter splitter = DocumentSplitters.recursive(
                ragConfig.getChunkSize(),
                ragConfig.getChunkOverlap()
        );

        List<VectorEmbedding> vectorEmbeddings = new ArrayList<>();
        int documentIndex = 0;

        for (String docContent : documents) {
            Document document = Document.from(docContent);
            List<TextSegment> docSegments = splitter.split(document);

            List<Embedding> embeddings = embeddingModel.embedAll(docSegments).content();

            for (int i = 0; i < docSegments.size(); i++) {
                TextSegment segment = docSegments.get(i);
                Embedding embedding = embeddings.get(i);

                VectorEmbedding vectorEmbedding = VectorEmbedding.builder()
                        .character(character)
                        .textContent(segment.text())
                        .embedding(embedding.vectorAsList())
                        .documentId("doc_" + documentIndex)
                        .chunkIndex(i)
                        .build();

                vectorEmbeddings.add(vectorEmbedding);
            }
            documentIndex++;
        }

        vectorEmbeddingRepository.saveAll(vectorEmbeddings);
        log.info("Successfully stored {} embeddings for character: {}", vectorEmbeddings.size(), character);
    }

    private List<String> getDocumentsForCharacter(String character) {
        return switch (character.toLowerCase()) {
            case "einstein" -> pdfChunkRepository.findAll().stream()
                    .map(PdfChunk::getText)
                    .filter(Objects::nonNull)
                    .toList();
            case "trump" -> tweetTextRepository.findAll().stream()
                    .map(TweetText::getText)
                    .filter(Objects::nonNull)
                    .toList();
            default -> {
                log.warn("Unknown character: {}", character);
                yield Collections.emptyList();
            }
        };
    }

    public List<String> searchSimilarContent(String character, String query) {
        List<VectorEmbedding> characterEmbeddings = vectorEmbeddingRepository.findByCharacter(character);

        if (characterEmbeddings.isEmpty()) {
            log.info("No embeddings found for character: {}. Creating new embeddings.", character);
            createEmbeddingsForCharacter(character);
            characterEmbeddings = vectorEmbeddingRepository.findByCharacter(character);

            if (characterEmbeddings.isEmpty()) {
                log.warn("Failed to create embeddings for character: {}", character);
                return Collections.emptyList();
            }
        }

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<Float> queryVector = queryEmbedding.vectorAsList();

        // 코사인 유사도 계산 및 정렬
        List<SimilarityResult> similarities = characterEmbeddings.stream()
                .map(embedding -> new SimilarityResult(
                        embedding.getTextContent(),
                        calculateCosineSimilarity(queryVector, embedding.getEmbedding())
                ))
                .filter(result -> result.similarity > 0.5) // 임계값 적용
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(ragConfig.getTopK())
                .toList();

        log.info("Found {} similar chunks for query in character: {}", similarities.size(), character);

        return similarities.stream()
                .map(result -> result.text)
                .collect(Collectors.toList());
    }

    public boolean hasEmbeddingsForCharacter(String character) {
        return vectorEmbeddingRepository.existsByCharacter(character);
    }

    public void removeEmbeddingsForCharacter(String character) {
        vectorEmbeddingRepository.deleteByCharacter(character);
        log.info("Removed embeddings for character: {}", character);
    }

    private double calculateCosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            double a = vectorA.get(i);
            double b = vectorB.get(i);

            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static class SimilarityResult {
        final String text;
        final double similarity;

        SimilarityResult(String text, double similarity) {
            this.text = text;
            this.similarity = similarity;
        }
    }
}