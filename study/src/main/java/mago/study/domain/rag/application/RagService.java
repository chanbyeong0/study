package mago.study.domain.rag.application;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mago.study.domain.message.dao.MessageRepository;
import mago.study.domain.message.domain.MessageDocument;
import mago.study.domain.message.dto.req.MessageReqDto;
import mago.study.domain.message.dto.res.MessageGetDto;
import mago.study.domain.user.domain.Role;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    
    private final ChatLanguageModel chatLanguageModel;
    private final MongoVectorStoreService vectorStoreService;
    private final MessageRepository messageRepository;
    private final CharacterDataService characterDataService;
    
    public MessageGetDto generateAnswer(String character, MessageReqDto messageReqDto) {
        try {
            log.info("Generating RAG answer for character: {} with message: {}", character, messageReqDto.content());
            
            // 캐릭터의 임베딩이 없으면 생성
            if (!vectorStoreService.hasEmbeddingsForCharacter(character)) {
                initializeCharacterKnowledge(character);
            }
            
            // 유사한 컨텐츠 검색
            List<String> similarContents = vectorStoreService.searchSimilarContent(character, messageReqDto.content());
            
            // 컨텍스트 구성
            String context = String.join("\n\n", similarContents);
            
            // 시스템 프롬프트 생성
            String systemPrompt = createSystemPrompt(character, context);
            
            // AI 응답 생성
            String response = chatLanguageModel.generate(systemPrompt + "\n\n사용자 질문: " + messageReqDto.content());
            
            // 응답 메시지 저장 (roomId는 나중에 MessageService에서 처리)
            MessageGetDto result = MessageGetDto.builder()
                    .messageId(new ObjectId().toHexString())
                    .role(Role.ASSISTANT)
                    .content(response)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            log.info("Successfully generated RAG answer for character: {}", character);
            return result;
            
        } catch (Exception e) {
            log.error("Error generating RAG answer for character: {}", character, e);
            return createErrorResponse();
        }
    }
    
    private void initializeCharacterKnowledge(String character) {
        List<String> knowledge = characterDataService.loadCharacterData(character);
        if (!knowledge.isEmpty()) {
            log.info("Initializing knowledge for character: {} with {} documents", character, knowledge.size());
            vectorStoreService.createEmbeddingsForCharacter(character, knowledge);
        } else {
            log.warn("No knowledge found for character: {}", character);
        }
    }
    
    private String createSystemPrompt(String character, String context) {
        return switch (character) {
            case "einstein" -> String.format("""
                당신은 알베르트 아인슈타인입니다. 다음 지식을 바탕으로 답변하세요:
                
                %s
                
                답변 원칙:
                1. 아인슈타인의 성격과 말투로 답변하세요
                2. 과학적 개념을 쉽게 설명하세요
                3. 상상력과 호기심을 강조하세요
                4. "제가 생각하기에는", "흥미롭게도" 같은 표현을 사용하세요
                5. 한국어로 자연스럽게 답변하세요
                """, context);
                
            case "trump" -> String.format("""
                당신은 도널드 트럼프입니다. 다음 지식을 바탕으로 답변하세요:
                
                %s
                
                답변 원칙 (반드시 지키세요!):
                1. 매우 자신감 있고 과장된 표현을 사용하세요 ("정말 대단한", "최고의", "incredible")
                2. "내가 말하건대", "believe me", "정말로" 같은 표현을 자주 사용하세요
                3. 비즈니스 성공담과 딜 메이킹 경험을 자랑하세요
                4. "나는 최고다", "내가 최고의 전문가다" 식의 자화자찬을 포함하세요
                5. 직설적이고 단순명료하게 말하되, 자신의 성공을 강조하세요
                6. 한국어로 답변하되 트럼프의 특징적인 말투를 완전히 재현하세요
                """, context);
                
            default -> String.format("""
                다음 정보를 바탕으로 도움이 되는 답변을 제공하세요:
                
                %s
                
                정확하고 유용한 정보를 한국어로 제공해주세요.
                """, context);
        };
    }
    
    private MessageGetDto createErrorResponse() {
        return MessageGetDto.builder()
                .messageId(new ObjectId().toHexString())
                .role(Role.ASSISTANT)
                .content("죄송합니다. 응답을 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
