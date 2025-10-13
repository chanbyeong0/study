package mago.study.global.exception.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //GLOBAL
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류"),

    //CHAT
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 업습니다."),
    
    //PDF
    PDF_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PDF 파일을 찾을 수 없습니다."),
    PDF_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 처리 중 오류가 발생했습니다."),
    PDF_PARSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 파싱에 실패했습니다."),
    PDF_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PDF 문서를 찾을 수 없습니다.");

    //오류 상태코드
    private final HttpStatus httpStatus;
    //오류 메시지
    private final String message;
}
