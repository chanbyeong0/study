package mago.study.domain.tweet.dto;

import java.util.List;

public record ImportRequest(
        List<String> paths,
        Integer batchSize,
        Boolean removeHashtag,
        Boolean removeMention,
        Boolean removeEmoji
) {
}
