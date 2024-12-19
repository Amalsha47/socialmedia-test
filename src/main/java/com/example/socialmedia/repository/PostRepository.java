package com.example.socialmedia.repository;

import java.util.List;
import java.util.Optional;

import com.example.socialmedia.model.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    Optional<List<Post>> findByUserId(String id);

    Optional<List<Post>> findByUserIdOrderByCreatedAtDesc(String id);

}
