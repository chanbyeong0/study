package mago.study.domain.pdf.dto;

import lombok.Builder;

@Builder
public record PdfSearchRequest(
        String searchText,
        String documentId,
        Boolean hasFormulas,
        Integer pageStart,
        Integer pageEnd
) {
}