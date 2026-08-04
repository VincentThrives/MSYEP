package com.vincent.msyep.modules.student;

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

/** An individual student lead, belonging to a Center (college). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "students")
public class Student {

    @Id
    private String id;

    // ----- Account -----
    private String name;
    private String email;
    private String phone;
    /** Login username for the student. */
    private String userId;
    /** Plaintext password — used only to create the login, then cleared. */
    private transient String password;

    /** Course/degree (legacy). */
    private String course;

    // ----- Personal details -----
    private String gender;
    private String dateOfBirth;
    private String caste;
    private String fatherName;
    private String motherName;

    // ----- Education & job details -----
    private String educationalQualification;
    private String admissionYear;
    private String interestedInternship;      // Yes / No
    private String technicalSkills;           // comma separated
    @Builder.Default
    private List<String> hobbies = new ArrayList<>();
    @Builder.Default
    private List<String> interestedCourses = new ArrayList<>();
    private String careerGoal;

    // Structured education — SSLC/10th (mandatory), PU/Diploma, Degree (optional)
    // markType = "Percentage" | "CGPA"; the value is stored in the matching *Percent field.
    private String sslcSchool;
    private String sslcMarkType;
    private String sslcPercent;
    private String sslcYear;
    private String puSchool;
    private String puMarkType;
    private String puPercent;
    private String puYear;
    private String puStream;
    private String degreeCollege;
    private String degreeMarkType;
    private String degreePercent;
    private String degreeYear;
    private String degreeStream;

    @Indexed
    private String centerId;
    @Indexed
    private String zoneId;

    // ----- Address (as per Aadhaar) -----
    private String state;
    @Indexed
    private String district;
    @Indexed
    private String taluk;
    @Indexed
    private String gramPanchayat;
    private String pincode;
    private String postalAddress;
    private String nativePlace;
    private String hostelName;

    // ----- MSYEP allotments (auto-generated) -----
    /** MSYEP Register No: MSYEP{year}{NNNNNN}. */
    @Indexed(unique = true, sparse = true)
    private String registerNo;
    /** Batch code: BATCH-{year}-{NNN}. */
    private String batchCode;
    private String courseJoiningDate;
    /** Chosen college / hostel. */
    private String collegeName;

    /** Uploaded student documents (photo, Aadhaar, PAN, marks cards, fee receipt, caste cert). */
    @Builder.Default
    private List<StudentDocument> documents = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
