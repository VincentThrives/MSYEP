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

    private String name;
    private String phone;
    private String email;

    /** Course/degree: PU, SSLC, ITI, DIPLOMA, DEGREE. */
    private String course;

    @Indexed
    private String centerId;
    @Indexed
    private String zoneId;

    // Geography — used by the finance wing filters.
    @Indexed
    private String district;
    @Indexed
    private String taluk;
    @Indexed
    private String gramPanchayat;

    /** FW / finance documents and other attachments. */
    @Builder.Default
    private List<StudentDocument> documents = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
