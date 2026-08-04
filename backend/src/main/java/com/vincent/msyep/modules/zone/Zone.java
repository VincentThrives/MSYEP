package com.vincent.msyep.modules.zone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A Zone == University. Holds the courses/degrees offered under it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "zones")
public class Zone {

    @Id
    private String id;

    /** University / Zone name. */
    private String name;

    /** Auto-generated franchise code: ZONE-{year}-{NNNN}. */
    private String code;

    private String district;
    private String taluk;
    private String gramPanchayat;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;

    // ----- Login -----
    private String userId;
    /** Plaintext password — used only to create the login, then cleared. */
    private transient String password;

    // ----- Organization -----
    private String organizationName;
    private boolean hasWebsite;
    private String websiteLink;
    private String websiteBudget;
    private String buildingOwnership;        // Own / Rent
    // Org document availability flags (uploads handled separately)
    private boolean hasRegisteredCopy;
    private boolean hasMsme;
    private boolean hasGst;
    private boolean hasNitiAayog;
    private boolean hasNgoDarpan;
    private boolean has12A80G;
    private boolean willingToComply;         // "if not, willing to do with us"

    // ----- Owner -----
    private String ownerName;
    private String ownerDob;
    private String ownerGender;
    private String contactNumber;
    private String alternateNumber;
    private String email;
    private String fullAddress;
    private String city;
    private String state;
    private String pincode;
    private String occupation;
    private String educationalQualification;

    // ----- KYC (numbers; photos uploaded as documents) -----
    private String aadhaarNumber;
    private String panNumber;
    private String bankAccountDetails;

    // ----- Business fit -----
    private String investmentCapacity;       // Below 1 Lakh / 1-5 / 5-10 / Above 10
    private String preferredLocation;
    private String spaceOwnership;            // Own / Rent
    private String spaceSqft;
    private String startTimeline;             // Immediately / Within 3 months / ...

    // ----- Membership / registration -----
    private String membershipTier;           // Silver / Gold / Platinum / Diamond
    private Integer membershipAmount;        // 50000 / 75000 / 100000 / 125000
    private boolean tcAccepted;

    // ----- Franchise certificate / MOU -----
    /** Trade / franchisee name printed on the certificate + letterhead (replaces the template sample). */
    private String franchiseeName;
    /** Registration number shown on the franchise certificate. */
    private String registrationNo;
    /** Certificate issue date (ISO yyyy-MM-dd) — the "from" date. */
    private String issueDate;
    /** Valid till — computed as issueDate + 2 years (ISO yyyy-MM-dd). Not user-editable. */
    private String validTill;
    /** Territory granted, derived from the membership tier (Hobli / Taluk / 3 Taluks / District). */
    private String territory;
    /** PENDING | ACTIVE. */
    @Builder.Default
    private String status = "PENDING";
    private String registrationDate;

    /** Degrees/courses under this zone: PU, SSLC, ITI, DIPLOMA, DEGREE, ... */
    @Builder.Default
    private List<String> courses = new ArrayList<>();

    /** KPMSYEP kit details — fields TBD (left open per spec). */
    @Builder.Default
    private List<String> kitDetails = new ArrayList<>();

    /** Uploaded franchise documents (Aadhaar/PAN/bank photos, logo, org copies, building copy). */
    @Builder.Default
    private List<ZoneDocument> documents = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
