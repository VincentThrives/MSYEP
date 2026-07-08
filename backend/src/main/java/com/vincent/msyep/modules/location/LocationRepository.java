package com.vincent.msyep.modules.location;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LocationRepository extends MongoRepository<Location, String> {
    boolean existsByDistrictAndTalukAndGramPanchayat(String district, String taluk, String gramPanchayat);
    boolean existsByDistrict(String district);
}
