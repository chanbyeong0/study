package mago.study.domain.tweet.dto;

/**
 * CSV 정제/저장 동작에 필요한 옵션 묶음 (record).
 */
public record ImportOptions(
        int batchSize,
        boolean removeHashtag,
        boolean removeMention,
        boolean removeEmoji,
        boolean failFast
) {}


