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

    // ----- Academic & type -----
    private String academicYear;
    private String academicStartMonth;
    private String academicEndMonth;

    /** MSYEP Center / College name. */
    private String name;

    /** Center type / category: University / PUC / ITI-Diploma / VTU / Social Welfare Hostel / GTTC. */
    private String centerType;

    /** Login username for this center (becomes the CENTER login email/id). */
    private String userId;

    /** Plaintext password — used only to create the login, then cleared (never persisted). */
    private transient String password;

    /** Assigned Center Head / Owner — id of an existing user (legacy / optional). */
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

    // ----- Contacts -----
    private String principalName;
    private String principalNumber;
    private String uucmsCoordinatorName;
    private String uucmsCoordinatorNumber;
    private String scstCoordinatorName;
    private String scstCoordinatorNumber;
    private String placementCoordinatorName;
    private String placementCoordinatorPhone;
    private String officeNumber;

    private boolean hasWebsite;
    private String websiteLink;

    private String contactEmail;
    private String contactPhone;

    // ----- Course details -----
    @Builder.Default
    private List<String> courses = new ArrayList<>();
    private Integer totalStrength;
    private Integer strengthTotal;
    private Integer strengthSC;
    private Integer strengthST;
    private Integer strengthGeneral;

    private String batchYear;

    // ----- MSYEP allotments (auto-generated) -----
    /** Business code: CENTER-{year}-{NNNN}, unique 4-digit. */
    @Indexed(unique = true, sparse = true)
    private String code;

    /** Enrollment number: CENENR{year}{NNNNNNN}, unique. */
    @Indexed(unique = true, sparse = true)
    private String enrollmentNumber;

    /** Batch code: BATCH-{year}-{NNNN}. */
    private String batchCode;

    /** ISO date the center was registered (yyyy-MM-dd). */
    private String registrationDate;

    /** MOU start date (yyyy-MM-dd). */
    private String dateOfMou;

    /** MOU end date (yyyy-MM-dd). */
    private String mouEndDate;

    /** Contract duration / Year of contract. */
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
