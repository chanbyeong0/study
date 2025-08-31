package mago.study.domain.rag.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mago.study.domain.message.dto.req.MessageReqDto;
import mago.study.domain.message.dto.res.MessageGetDto;
import mago.study.domain.rag.application.RagService;
import mago.study.domain.rag.application.VectorStoreService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag/test")
@RequiredArgsConstructor
@Slf4j
public class RagTestController {
    
    private final RagService ragService;
    private final VectorStoreService vectorStoreService;
    
    @PostMapping("/chat/{character}")
    public MessageGetDto testChat(
            @PathVariable String character,
            @RequestBody MessageReqDto messageReqDto
    ) {
        log.info("Testing RAG chat for character: {} with message: {}", character, messageReqDto.content());
        return ragService.generateAnswer(character, messageReqDto);
    }
    
    @GetMapping("/status/{character}")
    public Map<String, Object> getCharacterStatus(@PathVariable String character) {
        boolean hasEmbeddings = vectorStoreService.hasEmbeddingsForCharacter(character);
        return Map.of(
            "character", character,
            "hasEmbeddings", hasEmbeddings,
            "status", hasEmbeddings ? "ready" : "not_initialized"
        );
    }
    
    @PostMapping("/init/{character}")
    public Map<String, String> initializeCharacter(@PathVariable String character) {
        try {
            vectorStoreService.createEmbeddingsForCharacter(character);
            return Map.of("status", "success", "message", "Character embeddings created successfully");
        } catch (Exception e) {
            log.error("Failed to initialize character: {}", character, e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
    
    @PostMapping("/embeddings/{character}")
    public Map<String, String> createEmbeddings(@PathVariable String character) {
        try {
            vectorStoreService.createEmbeddingsForCharacter(character);
            return Map.of("status", "success", "message", "Embeddings created for character: " + character);
        } catch (Exception e) {
            log.error("Failed to create embeddings for character: {}", character, e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
} 