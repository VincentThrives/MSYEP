package com.vincent.msyep.modules.staff;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StaffRepository extends MongoRepository<Staff, String> {
    List<Staff> findByZoneId(String zoneId);
    List<Staff> findByCenterId(String centerId);
}
