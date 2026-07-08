package com.vincent.msyep.modules.resourceperson;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ResourcePersonRepository extends MongoRepository<ResourcePersonRequest, String> {
    Optional<ResourcePersonRequest> findByCenterId(String centerId);
}
