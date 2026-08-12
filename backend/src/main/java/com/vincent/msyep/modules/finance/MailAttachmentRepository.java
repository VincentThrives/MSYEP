package com.vincent.msyep.modules.finance;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MailAttachmentRepository extends MongoRepository<MailAttachment, String> {
    Optional<MailAttachment> findByLogIdAndIndex(String logId, int index);
    void deleteByLogId(String logId);
}
