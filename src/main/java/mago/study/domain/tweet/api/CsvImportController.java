package mago.study.domain.tweet.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mago.study.domain.tweet.application.CsvImportService;
import mago.study.domain.tweet.dto.FileResult;
import mago.study.domain.tweet.dto.ImportResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tweet")
@RequiredArgsConstructor
@Slf4j
public class CsvImportController {

    private final CsvImportService csvImportService;

    @PostMapping("/process")
    public ResponseEntity<List<FileResult>> importCsv() {
        ImportResult importResult = csvImportService.importByRequest();
        log.info("호출 완료");
        return ResponseEntity.ok(importResult.filesProcessed());
    }
}
