package mago.study.domain.room.dao;

import mago.study.domain.room.domain.RoomDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoomRepository extends MongoRepository<RoomDocument, ObjectId> {
}
