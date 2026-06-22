package com.vincent.msyep.modules.center;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A Center == College, belonging to a Zone. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "centers")
public class Center {

    @Id
    private String id;

    // ----- Center details -----
    /** MSYEP Center name. */
    private String name;

    /** Center type (Degree College, PU College, ITI, Polytechnic, GTTC, Hostel, ...). */
    private String centerType;

    /** Assigned Center Head / Owner — id of an existing user. */
    private String centerHeadUserId;

    private String address;
    private String locality;
    private String pincode;

    @Indexed
    private String zoneId;

    private String district;
    private String taluk;
    private String gramPanchayat;

    private String contactNumber;
    private String email;
    private String principalName;
    private String contactEmail;
    private String contactPhone;

    // ----- MSYEP allotments (auto-generated) -----
    /** Business code: CENTER-{year}-{NNNN}, unique 4-digit. */
    @Indexed(unique = true, sparse = true)
    private String code;

    /** Enrollment number: CENENR{year}{NNNNNNN}, unique. */
    @Indexed(unique = true, sparse = true)
    private String enrollmentNumber;

    /** ISO date the center was registered (yyyy-MM-dd). */
    private String registrationDate;

    /** Date of MOU (yyyy-MM-dd). */
    private String dateOfMou;

    /** Contract duration in years. */
    private String contractDuration;

    // ----- Documents -----
    @Builder.Default
    private List<CenterDocument> documents = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
