package mago.study.domain.pdf.dto;

import lombok.Builder;
import mago.study.domain.pdf.domain.PdfChunk;

import java.time.LocalDateTime;

@Builder
public record PdfChunkResponse(
        String id,
        String pdfDocumentId,
        Integer chunkIndex,
        String text,
        Integer chunkSize,
        Boolean hasFormulas,
        LocalDateTime createdAt
) {
    public static PdfChunkResponse from(PdfChunk chunk) {
        return PdfChunkResponse.builder()
                .id(chunk.getId().toString())
                .pdfDocumentId(chunk.getPdfDocumentId().toString())
                .chunkIndex(chunk.getChunkIndex())
                .text(chunk.getText())
                .chunkSize(chunk.getChunkSize())
                .hasFormulas(chunk.getHasFormulas())
                .createdAt(chunk.getCreateAt())
                .build();
    }
}