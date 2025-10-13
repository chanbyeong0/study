
package mago.study.domain.pdf.dto;

import lombok.Builder;
import mago.study.domain.pdf.domain.PdfDocument;

import java.time.LocalDateTime;

@Builder
public record PdfProcessResponse(
        String id,
        String fileName,
        String title,
        Integer pageCount,
        Long fileSize,
        String filePath,
        Boolean isLargeFile,
        String processingStatus,
        String gridFsId,
        LocalDateTime createdAt
) {
    public static PdfProcessResponse from(PdfDocument document) {
        return PdfProcessResponse.builder()
                .id(document.getId().toString())
                .fileName(document.getFileName())
                .title(document.getTitle())
                .pageCount(document.getPageCount())
                .fileSize(document.getFileSize())
                .filePath(document.getFilePath())
                .isLargeFile(document.getIsLargeFile())
                .processingStatus(document.getProcessingStatus().name())
                .gridFsId(document.getGridFsId())
                .createdAt(document.getCreateAt())
                .build();
    }
}