package com.vincent.msyep.modules.sow;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SowSubmissionRepository extends MongoRepository<SowSubmission, String> {
    List<SowSubmission> findByCenterId(String centerId);
    Optional<SowSubmission> findByCenterIdAndProgramIndex(String centerId, int programIndex);
}
