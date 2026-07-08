package com.vincent.msyep.modules.cv;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CvPaymentRepository extends MongoRepository<CvPayment, String> {
    boolean existsByStudentIdAndStatus(String studentId, String status);
    Optional<CvPayment> findByOrderId(String orderId);
}
