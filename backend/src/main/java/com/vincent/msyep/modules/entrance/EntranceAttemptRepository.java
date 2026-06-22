package com.vincent.msyep.modules.entrance;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EntranceAttemptRepository extends MongoRepository<EntranceAttempt, String> {
    List<EntranceAttempt> findByStudentIdOrderByStartedAtDesc(String studentId);
}
