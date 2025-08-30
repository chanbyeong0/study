package mago.study.domain.pdf.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mago.study.global.entity.BaseDocument;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "pdf_documents")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfDocument extends BaseDocument {
    @Id
    private ObjectId id;
    
    @Indexed
    @Field("file_name")
    private String fileName;
    
    @Field("title")
    private String title;
    
    @Field("page_count")
    private Integer pageCount;
    
    @Field("file_size")
    private Long fileSize;
    
    @Field("file_path")
    private String filePath;
    
    @Field("is_large_file")
    private Boolean isLargeFile;
    
    @Field("grid_fs_id")
    private String gridFsId;
    
    @Field("processing_status")
    private ProcessingStatus processingStatus;
    
    @Field("error_message")
    private String errorMessage;
    
    public enum ProcessingStatus {
        PENDING,
        PROCESSING, 
        COMPLETED,
        FAILED
    }
    
    public void updateProcessingStatus(ProcessingStatus status, String errorMessage) {
        this.processingStatus = status;
        this.errorMessage = errorMessage;
    }
    
    public void completeProcessing(String gridFsId) {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.gridFsId = gridFsId;
        this.errorMessage = null;
    }
}