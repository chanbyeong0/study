package mago.study.domain.message.dao;

import mago.study.domain.message.domain.MessageDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<MessageDocument, ObjectId> {
    void deleteAllByRoomId(ObjectId roomId);
}
