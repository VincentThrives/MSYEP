package com.vincent.msyep.modules.zone;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ZoneRepository extends MongoRepository<Zone, String> {
    boolean existsByCode(String code);
}
