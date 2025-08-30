package mago.study.domain.pdf.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mago.study.domain.pdf.application.PdfProcessingService;
import mago.study.domain.pdf.domain.PdfDocument;
import mago.study.domain.pdf.dto.PdfChunkResponse;
import mago.study.domain.pdf.dto.PdfProcessResponse;
import mago.study.domain.pdf.dto.PdfSearchRequest;
import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfController {
    
    private final PdfProcessingService pdfProcessingService;
    
    @PostMapping("/process")
    public ResponseEntity<PdfProcessResponse> processPdf() {
        // 절대 경로로 직접 접근
        String filePath = "/Users/mago/IdeaProjects/study/src/main/resources/data/978-0-387-69200-5.pdf";
        
        log.info("PDF 파일 처리 시작: {}", filePath);
        
        PdfDocument document = pdfProcessingService.processPdfFile(filePath);
        PdfProcessResponse response = PdfProcessResponse.from(document);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<PdfProcessResponse> getPdfDocument(@PathVariable String documentId) {
        Optional<PdfDocument> document = pdfProcessingService.findById(new ObjectId(documentId));
        
        if (document.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        PdfProcessResponse response = PdfProcessResponse.from(document.get());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<PdfProcessResponse>> getAllPdfDocuments() {
        List<PdfDocument> documents = pdfProcessingService.findAll();
        
        List<PdfProcessResponse> responses = documents.stream()
                .map(PdfProcessResponse::from)
                .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{documentId}/chunks")
    public ResponseEntity<List<PdfChunkResponse>> getPdfChunks(@PathVariable String documentId) {
        List<PdfChunkResponse> responses = pdfProcessingService.findChunksByDocumentId(new ObjectId(documentId))
                .stream()
                .map(PdfChunkResponse::from)
                .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/search")
    public ResponseEntity<List<PdfChunkResponse>> searchPdfChunks(@RequestBody PdfSearchRequest request) {
        log.info("PDF 텍스트 검색: {}", request.searchText());
        
        List<PdfChunkResponse> responses = pdfProcessingService.searchInChunks(request.searchText())
                .stream()
                .map(PdfChunkResponse::from)
                .toList();
        
        return ResponseEntity.ok(responses);
    }

    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deletePdfDocument(@PathVariable String documentId) {
        pdfProcessingService.deletePdfDocument(new ObjectId(documentId));
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PdfProcessResponse>> getPdfDocumentsByStatus(@PathVariable String status) {
        PdfDocument.ProcessingStatus processingStatus = PdfDocument.ProcessingStatus.valueOf(status.toUpperCase());
        List<PdfDocument> documents = pdfProcessingService.findByProcessingStatus(processingStatus);
        
        List<PdfProcessResponse> responses = documents.stream()
                .map(PdfProcessResponse::from)
                .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String documentId) throws Exception {
        Optional<PdfDocument> document = pdfProcessingService.findById(new ObjectId(documentId));
        
        if (document.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Path filePath = Paths.get(document.get().getFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        
        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + document.get().getFileName() + "\"")
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}