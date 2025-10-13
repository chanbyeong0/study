package mago.study.domain.pdf.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PdfTextChunker {
    
    private static final int DEFAULT_CHUNK_SIZE = 4000;
    private static final int CHUNK_OVERLAP = 200;
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+\\s+");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n|\\n(?=\\s*[A-Z가-힣])");
    private static final Pattern SINGLE_LINE_BREAK = Pattern.compile("\\n");
    private static final Pattern SECTION_PATTERN = Pattern.compile("\\n\\s*(Chapter|Section|\\d+\\.\\d*|[A-Z][^\\n]{10,50})\\n");
    
    public List<ChunkInfo> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE);
    }
    
    public List<ChunkInfo> chunkText(String text, int maxChunkSize) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }
        
        List<ChunkInfo> chunks = new ArrayList<>();
        
        // 문장 단위로 분할
        List<String> sentences = splitBySentences(text);
        int chunkIndex = 0;
        
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                chunks.add(new ChunkInfo(sentence.trim(), chunkIndex++));
            }
        }
        
        return chunks;
    }
    
    private List<String> splitBySentences(String text) {
        List<String> sentences = new ArrayList<>();
        
        // 먼저 줄바꿈을 공백으로 변환하여 텍스트 정리
        String cleanText = text.replaceAll("\\n+", " ").replaceAll("\\s+", " ").trim();
        
        // 마침표 기준으로 문장 분할 (단, 약어나 숫자 뒤의 마침표는 제외)
        String[] parts = cleanText.split("(?<=[.!?])\\s+(?=[A-Z가-힣])");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        
        return sentences;
    }
    
    
    
    
    
    public static class ChunkInfo {
        private final String text;
        private final int chunkIndex;
        
        public ChunkInfo(String text, int chunkIndex) {
            this.text = text;
            this.chunkIndex = chunkIndex;
        }
        
        public String getText() { return text; }
        public int getChunkIndex() { return chunkIndex; }
    }
}