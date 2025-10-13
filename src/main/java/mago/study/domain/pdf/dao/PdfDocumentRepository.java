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

    List<PdfDocument> findByProcessingStatus(PdfDocument.ProcessingStatus status);
    
    @Query("{'filePath': ?0}")
    Optional<PdfDocument> findByFilePath(String filePath);


}