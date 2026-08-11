package com.vincent.msyep.modules.finance;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MailLogRepository extends MongoRepository<MailLog, String> {
    List<MailLog> findTop100ByOrderBySentAtDesc();
    List<MailLog> findTop100ByChannelOrderBySentAtDesc(String channel);
}
