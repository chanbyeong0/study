package mago.study.domain.pdf.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mago.study.global.entity.BaseDocument;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "pdf_chunks")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "pdf_document_chunk_index", def = "{'pdfDocumentId': 1, 'chunkIndex': 1}")
public class PdfChunk extends BaseDocument {
    @Id
    private ObjectId id;
    
    @Field("pdf_document_id")
    private ObjectId pdfDocumentId;
    
    @Field("chunk_index")
    private Integer chunkIndex;
    
    @Field("text")
    private String text;
    
    @Field("chunk_size")
    private Integer chunkSize;
    
    @Field("has_formulas")
    private Boolean hasFormulas;
    
    public static PdfChunk of(ObjectId pdfDocumentId, int chunkIndex, String processedText, boolean hasFormulas) {
        return PdfChunk.builder()
                .pdfDocumentId(pdfDocumentId)
                .chunkIndex(chunkIndex)
                .text(processedText)
                .chunkSize(processedText != null ? processedText.length() : 0)
                .hasFormulas(hasFormulas)
                .build();
    }
}