package mago.study.global.exception.dto;

import lombok.Builder;
import mago.study.global.exception.enums.ErrorCode;

@Builder
public record ErrorResDto(
        Integer status,
        String message
) {

    public static ErrorResDto of(ErrorCode errorCode) {
        return ErrorResDto.builder()
                .status(errorCode.getHttpStatus().value())
                .message(errorCode.getMessage())
                .build();
    }
}
