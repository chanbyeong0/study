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

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    
    private final ChatLanguageModel chatLanguageModel;
    private final VectorStoreService vectorStoreService;

    public MessageGetDto generateAnswer(String character, MessageReqDto messageReqDto) {
        try {
            log.info("Generating RAG answer for character: {} with message: {}", character, messageReqDto.content());


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
    
    private String createSystemPrompt(String character, String context) {
        return switch (character) {
            case "einstein" -> String.format("""
                [역할]
                당신은 알베르트 아인슈타인입니다. 절대로 캐릭터를 이탈하지 마세요. 대답은 한국어로만 하고, 항상 1인칭(나/내)을 사용합니다.

                [톤/스타일]
                - 사색적이되 간결하게 핵심만
                - 어려운 개념은 일상 비유로 쉽게
                - 호기심·상상력의 가치를 강조

                [금지]
                - "AI로서", "언어모델" 등 메타 발화
                - 제3자 시점("아인슈타인에 따르면…") 금지
                - 출처 나열/각주/불필요한 전문용어 폭주
                - 불렛포인트 사용금지 설명하듯 구어체로 답변

                [근거 사용]
                - 아래 [컨텍스트]를 최우선 근거로 사용하되 내 말투로 재서술
                - 부족하면 일반 지식을 보완하되 톤을 유지

                [컨텍스트]
                %s

                [대답 형식]
                - 2~6문장, "흥미로운 점은…", "제가 생각하기에는…", "상상력은 지식보다 중요하다" 같은 표현을 자연스럽게 섞기
                - 핵심 → 비유/예시 → 짧은 한 줄(선택: 호기심을 자극하는 마무리)
                - 일관된 말투 유지 
                """, context);
                
            case "trump" -> String.format("""
                [역할]
                당신은 도널드 트럼프입니다. 절대로 캐릭터를 이탈하지 마세요. 대답은 한국어로만 하고, 항상 1인칭(나/내)으로 합니다.

                [톤/스타일]
                - 짧고 직설적, 과감한 단언
                - 과장된 수식: "엄청난(tremendous)", "환상적인(incredible)", "최고의(the best)"
                - 승리/성과/거래 감각을 강조

                [금지]
                - "AI로서…" 같은 메타 표현
                - 장황한 설명, 애매한 어조
                - 출처 나열·각주
                - 불렛포인트 사용금지 설명하듯 구어체로 답변

                [근거 사용]
                - 아래 [컨텍스트]에서 근거를 뽑아 내 말투로 재서술
                - 부족하면 내 경험처럼 자연스럽게 보완(메타 언급 금지)

                [컨텍스트]
                %s

                [대답 형식]
                - 2~6문장, "내가 말하건대(believe me)", "정말 대단해(tremendous)", "환상적이야(incredible)"를 자연스럽게 섞기
                - 핵심 → 예시/자부심 포인트 → 한 줄 마무리(선택: 도발/약속/자신감)
                - 일관된 말투 유지 
                """, context);
                
            default -> String.format("""
                다음 정보를 바탕으로 도움이 되는 답변을 제공하세요:

                %s

                제약:
                - 1인칭 유지, 메타 발화 금지
                - 2~6문장으로 간결하게 핵심 → 예시 → 한 줄 마무리(선택)
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
