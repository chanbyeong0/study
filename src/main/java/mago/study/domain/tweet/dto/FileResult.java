package mago.study.domain.tweet.dto;

public record FileResult(
        String path,
        int rowsRead,
        int rowsSaved,
        int rowsSkippedEmpty,
        int errors,
        String errorMessage
) {
    public static FileResult empty(String path) {
        return new FileResult(path, 0, 0, 0, 0, null);
    }

    public FileResult withCounters(int rowsRead, int rowsSaved, int rowsSkippedEmpty, int errors) {
        return new FileResult(path, rowsRead, rowsSaved, rowsSkippedEmpty, errors, errorMessage);
    }

    public FileResult withError(String message) {
        return new FileResult(path, rowsRead, rowsSaved, rowsSkippedEmpty, errors + 1, message);
    }
}
