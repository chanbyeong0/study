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
    private final VectorStoreService vectorStoreService;
    private final MessageRepository messageRepository;
    
    // 캐릭터별 기본 지식 데이터 (실제로는 외부 파일이나 DB에서 로드)
    private static final Map<String, List<String>> CHARACTER_KNOWLEDGE = Map.of(
        "einstein", List.of(
            """
            알베르트 아인슈타인 (Albert Einstein, 1879-1955)
            독일 태생의 이론물리학자로, 20세기 가장 영향력 있는 과학자 중 하나입니다.
            
            주요 업적:
            1. 특수상대성이론 (1905): 시간과 공간이 절대적이지 않고 상대적이라는 것을 증명
            2. 일반상대성이론 (1915): 중력을 시공간의 곡률로 설명
            3. 광전효과 연구로 1921년 노벨물리학상 수상
            4. E=mc² 공식으로 질량-에너지 등가성 발견
            
            성격적 특징:
            - 깊은 사색을 즐기는 철학적 사고
            - 상상력을 중시: "상상력은 지식보다 중요하다"
            - 평화주의자이자 인도주의자
            - 호기심이 많고 질문을 멈추지 않는 탐구정신
            """,
            """
            상대성이론에 대한 아인슈타인의 설명:
            
            특수상대성이론의 핵심:
            - 광속은 모든 관성계에서 일정하다
            - 시간 지연(time dilation): 빠르게 움직이는 물체의 시간은 느려진다
            - 길이 수축: 움직이는 물체는 운동 방향으로 수축한다
            - E=mc²: 질량과 에너지는 상호 변환 가능하다
            
            일반상대성이론의 핵심:
            - 중력은 시공간의 곡률이다
            - 질량이 있는 물체는 시공간을 휘게 만든다
            - 빛도 중력의 영향을 받는다
            - 시간은 중력장에서 느려진다
            
            "가장 이해할 수 없는 것은 우주가 이해 가능하다는 것이다."
            """
        ),
        "trump", List.of(
            """
            도널드 트럼프 (Donald Trump, 1946-)
            미국의 기업인이자 제45대 대통령입니다.
            
            주요 경력:
            1. 부동산: 트럼프 오르가니제이션 운영 - "최고의 딜을 만드는 것이 내 전문이다!"
            2. 미디어: 리얼리티 TV 프로그램 "더 어프렌티스" 진행 - "You're fired!"로 유명
            3. 정치: 2017-2021년 미국 대통령 재임 - "Make America Great Again!"
            4. 비즈니스: 트럼프 타워, 골프장, 호텔 등 운영 - "모든 것이 최고급, 최고 품질!"
            
            트럼프의 명언과 특징:
            - "나는 딜의 달인이다. 최고의 딜을 만든다!"
            - "내가 하는 모든 것은 tremendous, fantastic, incredible하다!"
            - "나는 매우 똑똑한 사람이다. 최고의 대학을 나왔다!"
            - "승리하는 것이 중요하다. 나는 항상 승리한다!"
            - "가짜 뉴스는 믿으면 안 된다. 나만 믿어라!"
            
            말투 특징:
            - 자신감 넘치고 과장된 표현을 자주 사용
            - "believe me", "tremendous", "fantastic" 등의 단어를 반복 사용
            - 직설적이고 단순명료한 표현을 선호
            """
        )
    );
    
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
        List<String> knowledge = CHARACTER_KNOWLEDGE.get(character);
        if (knowledge != null) {
            log.info("Initializing knowledge for character: {}", character);
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
