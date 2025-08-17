package mago.study.domain.room.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mago.study.domain.room.dto.RoomAddDto;
import mago.study.global.entity.BaseDocument;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rooms")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDocument extends BaseDocument {
    @Id
    private ObjectId id;
    private String character;
    private Long messageCount;

    public static RoomDocument of(RoomAddDto roomAddDto) {
        return RoomDocument.builder()
                .character(roomAddDto.character())
                .messageCount(0L)
                .build();
    }
}
