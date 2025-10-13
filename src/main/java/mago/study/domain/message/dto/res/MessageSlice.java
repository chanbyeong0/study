package mago.study.domain.message.dto.res;

import lombok.Builder;
import org.bson.types.ObjectId;

import java.util.List;

@Builder
public record MessageSlice(
        List<MessageGetDto> messages,
        String nextCursor,
        boolean hasMore
) {
    public static MessageSlice of(List<MessageGetDto> messages, String nextCursor, boolean hasMore) {
        return MessageSlice.builder()
                .messages(messages)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}
