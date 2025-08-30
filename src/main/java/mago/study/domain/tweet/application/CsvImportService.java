package mago.study.domain.tweet.application;

import lombok.RequiredArgsConstructor;
import mago.study.domain.tweet.dao.TweetTextRepository;
import mago.study.domain.tweet.domain.TweetText;
import mago.study.domain.tweet.dto.FileResult;
import mago.study.domain.tweet.dto.ImportResult;
import mago.study.domain.tweet.dto.ImportOptions;
import mago.study.domain.tweet.dto.ImportCounters;
import mago.study.global.util.TextCleaner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import mago.study.global.exception.custom.BusinessException;
import mago.study.global.exception.enums.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final TweetTextRepository repo;
    private final ResourceLoader resourceLoader;

    // 콤마 한 줄이든(yml) 리스트든 상관없이 List로 주입 (양쪽 공백 무시)
    @Value("#{'${tweet-import.file-paths}'.split('\\s*,\\s*')}")
    private List<String> defaultFilePaths;

    @Value("${tweet-import.batch-size:1000}")
    private int defaultBatchSize;

    @Value("${tweet-import.remove-hashtag:true}")
    private boolean defaultRemoveHashtag;

    @Value("${tweet-import.remove-mention:true}")
    private boolean defaultRemoveMention;

    @Value("${tweet-import.remove-emoji:true}")
    private boolean defaultRemoveEmoji;

    @Value("${tweet-import.fail-fast:false}")
    private boolean defaultFailFast;

    // CSV 헤더 컬럼명
    private static final String COL_TEXT = "Tweet Text";

    /**
     * 설정 파일(application.yml)의 기본 옵션을 사용하여
     * 지정된 모든 CSV 파일을 읽고, 정제 후 MongoDB에 저장한다.
     */
    public ImportResult importByRequest() {
        ImportOptions options = optionsFromDefaults();
        List<FileResult> results = new ArrayList<>(defaultFilePaths.size());

        for (String path : defaultFilePaths) {
            try {
                results.add(processOneFile(path, options));
            } catch (Exception e) {
                if (options.failFast()) {
                    throw new BusinessException(path, "file", ErrorCode.INTERNAL_SERVER_ERROR);
                }
                // 실패를 결과 집계에 포함
                ImportCounters c = new ImportCounters(0, 0, 0, 0);
                results.add(new FileResult(path, c.rowsRead(), c.rowsSaved(), c.rowsSkippedEmpty(), c.errors() + 1, e.getMessage()));
            }
        }

        // 결과에도 사용한 옵션을 그대로 표기
        return ImportResult
                .empty(options.batchSize(), options.removeHashtag(), options.removeMention(), options.removeEmoji())
                .withFiles(results);
    }


    /* ========================= Core ========================= */

    /**
     * 단일 CSV 파일을 처리한다.
     * - CSV 파싱
     * - 텍스트 정제
     * - 배치 단위 저장
     */
    private FileResult processOneFile(String path, ImportOptions options) throws Exception {
        CSVFormat format = csvFormat();
        ImportCounters counters = new ImportCounters(0, 0, 0, 0);

        try (BufferedReader br = openReader(path);
             CSVParser parser = new CSVParser(br, format)) {

            List<TweetText> buffer = new ArrayList<>(options.batchSize());

            for (CSVRecord record : parser) {
                counters = processRecord(record, buffer, counters, options);
                if (buffer.size() >= options.batchSize()) {
                    counters = counters.withRowsSaved(counters.rowsSaved() + flushBuffer(buffer));
                }
            }

            // 남은 레코드 처리
            if (!buffer.isEmpty()) counters = counters.withRowsSaved(counters.rowsSaved() + flushBuffer(buffer));
        }

        return new FileResult(path, counters.rowsRead(), counters.rowsSaved(), counters.rowsSkippedEmpty(), counters.errors(), null);
    }

    private CSVFormat csvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .setQuote('"')
                .build();
    }

    private BufferedReader openReader(String location) throws Exception {
        Resource res = resourceLoader.getResource(location);
        if (res.exists()) {
            return new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8));
        }
        // res가 없으면 일반 파일 경로로 시도
        return new BufferedReader(new FileReader(location, StandardCharsets.UTF_8));
    }

    /**
     * CSV 레코드 하나를 처리하여 버퍼에 적재한다.
     */
    private ImportCounters processRecord(CSVRecord record, List<TweetText> buffer, ImportCounters counters, ImportOptions options) {
        ImportCounters updated = counters.withRowsRead(counters.rowsRead() + 1);

        // 필수 컬럼이 없으면 에러로 집계하고 스킵
        if (!record.isMapped(COL_TEXT)) {
            return updated.withErrors(updated.errors() + 1);
        }

        String raw = record.get(COL_TEXT);
        String cleaned = TextCleaner.clean(raw, options.removeHashtag(), options.removeMention(), options.removeEmoji());

        // 정제 후 비어 있으면 스킵
        if (cleaned == null) {
            return updated.withRowsSkippedEmpty(updated.rowsSkippedEmpty() + 1);
        }

        buffer.add(TweetText.builder().text(cleaned).build());
        return updated;
    }

    /**
     * 버퍼를 저장소에 한 번에 저장하고 비운다.
     */
    private int flushBuffer(List<TweetText> buffer) {
        repo.saveAll(buffer);
        int saved = buffer.size();
        buffer.clear();
        return saved;
    }

    /**
     * 기본 설정값을 구조화된 옵션으로 변환한다.
     */
    private ImportOptions optionsFromDefaults() {
        return new ImportOptions(defaultBatchSize, defaultRemoveHashtag, defaultRemoveMention, defaultRemoveEmoji, defaultFailFast);
    }
}