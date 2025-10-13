package mago.study.domain.tweet.dao;

import mago.study.domain.tweet.domain.TweetText;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TweetTextRepository extends MongoRepository<TweetText, String> {
}
