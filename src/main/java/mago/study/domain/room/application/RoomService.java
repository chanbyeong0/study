package mago.study.domain.room.application;


import lombok.RequiredArgsConstructor;
import mago.study.domain.message.dao.MessageRepository;
import mago.study.domain.room.dao.RoomRepository;
import mago.study.domain.room.domain.RoomDocument;
import mago.study.domain.room.dto.RoomAddDto;
import mago.study.domain.room.dto.RoomCreateRes;
import mago.study.domain.room.dto.RoomGetDto;
import mago.study.global.exception.custom.BusinessException;
import mago.study.global.exception.enums.ErrorCode;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public RoomCreateRes createRoom(RoomAddDto roomAddDto) {
        RoomDocument roomDocument = roomRepository.save(RoomDocument.of(roomAddDto));
        return new RoomCreateRes(roomDocument.getId().toHexString());
    }

    public List<RoomGetDto> getAll() {
        List<RoomDocument> all = roomRepository.findAll();
        return all.stream()
                .map(RoomGetDto::from)
                .toList();
    }

    public RoomGetDto getRoom(ObjectId roomId) {
        RoomDocument room = roomRepository.findById(roomId).orElseThrow(
                () -> new BusinessException(roomId, "roomId", ErrorCode.ROOM_NOT_FOUND)
        );

        return RoomGetDto.from(room);
    }

    public void resetRoom(ObjectId roomId) {
        messageRepository.deleteAllByRoomId(roomId);
    }

    public void deleteRoom(ObjectId roomId) {
        roomRepository.deleteById(roomId);
        messageRepository.deleteAllByRoomId(roomId);
    }
}
