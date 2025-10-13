package mago.study.domain.pdf.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mago.study.domain.pdf.dao.PdfChunkRepository;
import mago.study.domain.pdf.dao.PdfDocumentRepository;
import mago.study.domain.pdf.domain.PdfChunk;
import mago.study.domain.pdf.domain.PdfDocument;
import mago.study.global.exception.custom.BusinessException;
import mago.study.global.exception.enums.ErrorCode;
import mago.study.domain.pdf.util.PdfTextChunker;
import mago.study.global.util.TextCleaner;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingService {
    
    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfChunkRepository pdfChunkRepository;
    private final PdfTextChunker textChunker;
    private final GridFsTemplate gridFsTemplate;
    private final MongoTemplate mongoTemplate;
    
    private static final long LARGE_FILE_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final int MAX_CHUNK_SIZE = 4000;
    private static final int PDF_START_PAGE = 17;
    private static final int PDF_END_PAGE = 518;
    private static final Pattern FORMULA_PATTERN = Pattern.compile(
        "(?i)(\\b(?:equation|formula|theorem|proof|lemma|corollary)\\b|" +
        "[∫∑∏∆∇∂√±×÷≤≥≠≈∞α-ωΑ-Ω]|" +
        "\\$[^$]+\\$|\\\\[a-zA-Z]+\\{[^}]*\\}|" +
        "\\b(?:sin|cos|tan|log|ln|exp|lim|∫|∑)\\b|" +
        "(?:\\d+[\\s]*[+\\-×÷=]\\s*\\d+)|" +
        "(?:[xy]\\s*[=<>]\\s*[\\d\\w\\s+\\-×÷\\(\\)]+))"
    );
    
    @Transactional
    public PdfDocument processPdfFile(String filePath) {
        log.info("PDF 파일 처리 시작: {}", filePath);
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new BusinessException(filePath, "filePath", ErrorCode.PDF_FILE_NOT_FOUND);
            }
            
            Optional<PdfDocument> existingDoc = pdfDocumentRepository.findByFilePath(filePath);
            if (existingDoc.isPresent() && existingDoc.get().getProcessingStatus() == PdfDocument.ProcessingStatus.COMPLETED) {
                log.info("이미 처리된 PDF 파일입니다: {}", filePath);
                return existingDoc.get();
            }
            
            long fileSize = file.length();
            boolean isLargeFile = fileSize > LARGE_FILE_THRESHOLD;
            
            PdfDocument pdfDocument = createOrUpdatePdfDocument(file, fileSize, isLargeFile, existingDoc.orElse(null));
            
            try (PDDocument document = PDDocument.load(file)) {
                String title = extractTitle(document);
                int pageCount = document.getNumberOfPages();
                
                pdfDocument = PdfDocument.builder()
                        .id(pdfDocument.getId())
                        .fileName(file.getName())
                        .title(title)
                        .pageCount(pageCount)
                        .fileSize(fileSize)
                        .filePath(filePath)
                        .isLargeFile(isLargeFile)
                        .processingStatus(PdfDocument.ProcessingStatus.PROCESSING)
                        .build();
                
                pdfDocument = pdfDocumentRepository.save(pdfDocument);
                
                if (isLargeFile) {
                    processLargeFile(document, pdfDocument);
                } else {
                    processRegularFile(document, pdfDocument);
                }
                
                pdfDocument.updateProcessingStatus(PdfDocument.ProcessingStatus.COMPLETED, null);
                pdfDocumentRepository.save(pdfDocument);
                
                log.info("PDF 파일 처리 완료: {} ({}페이지, {}MB)", 
                        file.getName(), pageCount, fileSize / (1024 * 1024));
                
                return pdfDocument;
                
            } catch (IOException e) {
                log.error("PDF 파싱 중 오류 발생: {}", filePath, e);
                pdfDocument.updateProcessingStatus(PdfDocument.ProcessingStatus.FAILED, e.getMessage());
                pdfDocumentRepository.save(pdfDocument);
                throw new BusinessException(ErrorCode.PDF_PARSING_FAILED);
            }
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF 처리 중 예외 발생: {}", filePath, e);
            throw new BusinessException(ErrorCode.PDF_PROCESSING_FAILED);
        }
    }
    
    private PdfDocument createOrUpdatePdfDocument(File file, long fileSize, boolean isLargeFile, PdfDocument existing) {
        if (existing != null) {
            existing.updateProcessingStatus(PdfDocument.ProcessingStatus.PROCESSING, null);
            return pdfDocumentRepository.save(existing);
        }
        
        return PdfDocument.builder()
                .fileName(file.getName())
                .fileSize(fileSize)
                .filePath(file.getAbsolutePath())
                .isLargeFile(isLargeFile)
                .processingStatus(PdfDocument.ProcessingStatus.PENDING)
                .build();
    }
    
    private void processRegularFile(PDDocument document, PdfDocument pdfDocument) throws IOException {
        log.info("일반 파일 처리 시작: {} (페이지 {}~{})", pdfDocument.getFileName(), PDF_START_PAGE, PDF_END_PAGE);
        
        int totalPages = document.getNumberOfPages();
        int endPage = Math.min(PDF_END_PAGE, totalPages);
        
        if (PDF_START_PAGE > totalPages) {
            log.warn("시작 페이지({})가 전체 페이지 수({})보다 큽니다.", PDF_START_PAGE, totalPages);
            return;
        }
        
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(PDF_START_PAGE);
        stripper.setEndPage(endPage);
        String fullText = stripper.getText(document);
        
        List<PdfTextChunker.ChunkInfo> chunks = textChunker.chunkText(fullText, MAX_CHUNK_SIZE);
        
        savePdfChunks(pdfDocument.getId(), chunks, fullText);
    }
    
    private void processLargeFile(PDDocument document, PdfDocument pdfDocument) throws IOException {
        log.info("대용량 파일 처리 시작: {} (GridFS 사용, 페이지 {}~{})", pdfDocument.getFileName(), PDF_START_PAGE, PDF_END_PAGE);
        
        int totalPages = document.getNumberOfPages();
        int actualEndPage = Math.min(PDF_END_PAGE, totalPages);
        
        if (PDF_START_PAGE > totalPages) {
            log.warn("시작 페이지({})가 전체 페이지 수({})보다 큽니다.", PDF_START_PAGE, totalPages);
            return;
        }
        
        int pagesPerChunk = calculatePagesPerChunk(actualEndPage - PDF_START_PAGE + 1);
        
        StringBuilder fullTextBuilder = new StringBuilder();
        List<PdfTextChunker.ChunkInfo> allChunks = new ArrayList<>();
        
        for (int startPage = PDF_START_PAGE; startPage <= actualEndPage; startPage += pagesPerChunk) {
            int endPage = Math.min(startPage + pagesPerChunk - 1, actualEndPage);
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(startPage);
            stripper.setEndPage(endPage);
            
            String pageText = stripper.getText(document);
            fullTextBuilder.append(pageText);
            
            List<PdfTextChunker.ChunkInfo> chunks = textChunker.chunkText(pageText, MAX_CHUNK_SIZE);
            
            for (PdfTextChunker.ChunkInfo chunk : chunks) {
                PdfTextChunker.ChunkInfo adjustedChunk = new PdfTextChunker.ChunkInfo(
                        chunk.getText(),
                        allChunks.size()
                );
                allChunks.add(adjustedChunk);
            }
        }
        
        String fullText = fullTextBuilder.toString();
        String gridFsId = saveToGridFs(pdfDocument.getFileName(), fullText);
        pdfDocument.completeProcessing(gridFsId);
        
        savePdfChunks(pdfDocument.getId(), allChunks, fullText);
    }
    
    private void savePdfChunks(ObjectId pdfDocumentId, List<PdfTextChunker.ChunkInfo> chunks, String fullText) {
        log.info("PDF 청크 저장 시작: {} chunks", chunks.size());
        
        List<PdfChunk> pdfChunks = new ArrayList<>();
        
        for (PdfTextChunker.ChunkInfo chunkInfo : chunks) {
            String originalText = chunkInfo.getText();
            String processedText = processText(originalText);
            boolean hasFormulas = detectFormulas(originalText);
            
            PdfChunk pdfChunk = PdfChunk.of(
                    pdfDocumentId,
                    chunkInfo.getChunkIndex(),
                    processedText,
                    hasFormulas
            );
            
            pdfChunks.add(pdfChunk);
        }
        
        pdfChunkRepository.saveAll(pdfChunks);
        log.info("PDF 청크 저장 완료: {} chunks", pdfChunks.size());
    }
    
    private String processText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String processed = TextCleaner.clean(text, false, false, false);
        
        if (processed == null || processed.trim().isEmpty()) {
            return text;
        }
        
        processed = normalizeFormulas(processed);
        processed = normalizeWhitespace(processed);
        
        return processed;
    }
    
    private String normalizeFormulas(String text) {
        return text
                .replaceAll("\\s*=\\s*", " = ")
                .replaceAll("\\s*\\+\\s*", " + ")
                .replaceAll("\\s*-\\s*", " - ")
                .replaceAll("\\s*\\*\\s*", " * ")
                .replaceAll("\\s*/\\s*", " / ")
                .replaceAll("\\s*<\\s*", " < ")
                .replaceAll("\\s*>\\s*", " > ")
                .replaceAll("\\s*≤\\s*", " ≤ ")
                .replaceAll("\\s*≥\\s*", " ≥ ")
                .replaceAll("\\s*≠\\s*", " ≠ ");
    }
    
    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
    
    private boolean detectFormulas(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        return FORMULA_PATTERN.matcher(text).find();
    }
    
    private String extractTitle(PDDocument document) {
        try {
            if (document.getDocumentInformation() != null && 
                document.getDocumentInformation().getTitle() != null) {
                return document.getDocumentInformation().getTitle();
            }
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String firstPageText = stripper.getText(document);
            
            String[] lines = firstPageText.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.length() > 10 && line.length() < 200 && 
                    !line.toLowerCase().contains("page") && 
                    !line.matches(".*\\d+.*")) {
                    return line;
                }
            }
        } catch (IOException e) {
            log.warn("제목 추출 실패", e);
        }
        
        return "Unknown Title";
    }
    
    private int calculatePagesPerChunk(int totalPages) {
        if (totalPages <= 50) return 10;
        if (totalPages <= 200) return 20;
        return 50;
    }
    
    private String saveToGridFs(String fileName, String content) {
        try {
            return gridFsTemplate.store(
                    new FileInputStream(new File(fileName)), 
                    fileName + "_content", 
                    "text/plain"
            ).toString();
        } catch (Exception e) {
            log.error("GridFS 저장 실패: {}", fileName, e);
            throw new BusinessException(ErrorCode.PDF_PROCESSING_FAILED);
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<PdfDocument> findByFilePath(String filePath) {
        return pdfDocumentRepository.findByFilePath(filePath);
    }
    
    @Transactional(readOnly = true)
    public List<PdfChunk> findChunksByDocumentId(ObjectId documentId) {
        return pdfChunkRepository.findByPdfDocumentIdOrderByChunkIndex(documentId);
    }
    
    @Transactional(readOnly = true)
    public List<PdfChunk> searchInChunks(String searchText) {
        return pdfChunkRepository.findByTextContaining(searchText);
    }
    
    @Transactional
    public void deletePdfDocument(ObjectId documentId) {
        pdfChunkRepository.deleteByPdfDocumentId(documentId);
        pdfDocumentRepository.deleteById(documentId);
        log.info("PDF 문서 및 청크 삭제 완료: {}", documentId);
    }
    
    @Transactional(readOnly = true)
    public Optional<PdfDocument> findById(ObjectId documentId) {
        return pdfDocumentRepository.findById(documentId);
    }
    
    @Transactional(readOnly = true)
    public List<PdfDocument> findAll() {
        return pdfDocumentRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<PdfDocument> findByProcessingStatus(PdfDocument.ProcessingStatus status) {
        return pdfDocumentRepository.findByProcessingStatus(status);
    }
    
    @Transactional(readOnly = true)
    public long countChunksByDocumentId(ObjectId documentId) {
        return pdfChunkRepository.countByPdfDocumentId(documentId);
    }
}