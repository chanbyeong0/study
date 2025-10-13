package mago.study.domain.tweet.dto;

import java.util.List;

public record ImportResult(
        int rowsRead,
        int rowsSaved,
        int rowsSkippedEmpty,
        int errors,
        Integer batchSize,
        Boolean removeHashtag,
        Boolean removeMention,
        Boolean removeEmoji,
        List<FileResult> filesProcessed
) {
    public static ImportResult empty(Integer batchSize, Boolean removeHashtag, Boolean removeMention, Boolean removeEmoji) {
        return new ImportResult(0, 0, 0, 0, batchSize, removeHashtag, removeMention, removeEmoji, List.of());
    }

    public ImportResult withFiles(List<FileResult> files) {
        int read = files.stream().mapToInt(FileResult::rowsRead).sum();
        int saved = files.stream().mapToInt(FileResult::rowsSaved).sum();
        int skipped = files.stream().mapToInt(FileResult::rowsSkippedEmpty).sum();
        int err = files.stream().mapToInt(FileResult::errors).sum();
        return new ImportResult(read, saved, skipped, err, batchSize, removeHashtag, removeMention, removeEmoji, files);
    }
}