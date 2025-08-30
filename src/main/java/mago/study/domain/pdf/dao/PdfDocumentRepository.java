package mago.study.domain.pdf.dao;

import mago.study.domain.pdf.domain.PdfDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PdfDocumentRepository extends MongoRepository<PdfDocument, ObjectId> {
    
    Optional<PdfDocument> findByFileName(String fileName);
    
    List<PdfDocument> findByProcessingStatus(PdfDocument.ProcessingStatus status);
    
    @Query("{'filePath': ?0}")
    Optional<PdfDocument> findByFilePath(String filePath);
    
    List<PdfDocument> findByIsLargeFile(Boolean isLargeFile);
    
    @Query("{'fileSize': {$gte: ?0}}")
    List<PdfDocument> findByFileSizeGreaterThanEqual(Long fileSize);
    
    long countByProcessingStatus(PdfDocument.ProcessingStatus status);
}