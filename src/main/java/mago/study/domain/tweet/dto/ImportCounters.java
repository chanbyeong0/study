package mago.study.domain.tweet.dto;

/**
 * 파일 처리 중 집계 값 보관용 (record, 불변 + with 메서드 제공).
 */
public record ImportCounters(
        int rowsRead,
        int rowsSaved,
        int rowsSkippedEmpty,
        int errors
) {
    public ImportCounters withRowsRead(int v) { return new ImportCounters(v, rowsSaved, rowsSkippedEmpty, errors); }
    public ImportCounters withRowsSaved(int v) { return new ImportCounters(rowsRead, v, rowsSkippedEmpty, errors); }
    public ImportCounters withRowsSkippedEmpty(int v) { return new ImportCounters(rowsRead, rowsSaved, v, errors); }
    public ImportCounters withErrors(int v) { return new ImportCounters(rowsRead, rowsSaved, rowsSkippedEmpty, v); }
}


