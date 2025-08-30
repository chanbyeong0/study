package mago.study.domain.pdf.dao;

import mago.study.domain.pdf.domain.PdfChunk;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PdfChunkRepository extends MongoRepository<PdfChunk, ObjectId> {
    
    List<PdfChunk> findByPdfDocumentIdOrderByChunkIndex(ObjectId pdfDocumentId);
    
    @Query("{'pdfDocumentId': ?0, 'chunkIndex': ?1}")
    PdfChunk findByPdfDocumentIdAndChunkIndex(ObjectId pdfDocumentId, Integer chunkIndex);
    
    @Query("{'pdfDocumentId': ?0, 'hasFormulas': true}")
    List<PdfChunk> findByPdfDocumentIdAndHasFormulas(ObjectId pdfDocumentId);
    
    @Query("{'text': {$regex: ?0, $options: 'i'}}")
    List<PdfChunk> findByTextContaining(String searchText);
    
    long countByPdfDocumentId(ObjectId pdfDocumentId);
    
    void deleteByPdfDocumentId(ObjectId pdfDocumentId);
}