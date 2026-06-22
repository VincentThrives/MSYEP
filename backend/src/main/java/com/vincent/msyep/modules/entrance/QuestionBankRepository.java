package com.vincent.msyep.modules.entrance;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuestionBankRepository extends MongoRepository<QuestionBank, String> {
}
