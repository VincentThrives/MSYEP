package com.vincent.msyep.modules.student;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {
    List<Student> findByCenterId(String centerId);
    List<Student> findByZoneId(String zoneId);
    Optional<Student> findByPhone(String phone);
}
