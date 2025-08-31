package mago.study.domain.message.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mago.study.domain.message.dto.req.MessageReqDto;
import mago.study.domain.user.domain.Role;
import mago.study.global.entity.BaseDocument;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "room_desc_id_desc", def = "{'roomId': 1, '_id': -1}")
public class MessageDocument extends BaseDocument {
    @Id
    private ObjectId id;
    private ObjectId roomId;
    private String content;
    // TODO 추후 USER로 이동
    private Role role;

    public static MessageDocument of(MessageReqDto messageReqDto, ObjectId roomId, Role role) {
        return MessageDocument.builder()
                .roomId(roomId)
                .content(messageReqDto.content())
                .role(role)
                .build();
    }
}
