package com.vincent.msyep.modules.center;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CenterRepository extends MongoRepository<Center, String> {
    List<Center> findByZoneId(String zoneId);
}
