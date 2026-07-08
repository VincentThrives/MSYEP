package com.vincent.msyep.modules.cv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A student's ₹90 CV-generation payment (one PAID row unlocks unlimited re-downloads). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cv_payments")
public class CvPayment {

    @Id
    private String id;

    @Indexed
    private String studentId;

    /** CREATED → PAID */
    private String status;

    private String orderId;
    private String paymentId;
    private int amountPaise;
    private boolean stub;

    private Instant createdAt;
    private Instant paidAt;
}
