package com.vincent.msyep.modules.finance;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GramPanchayatRepository extends MongoRepository<GramPanchayat, String> {
    Optional<GramPanchayat> findByNameIgnoreCase(String name);
}
