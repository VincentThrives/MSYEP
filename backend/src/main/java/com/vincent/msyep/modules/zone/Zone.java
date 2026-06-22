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

    /** University name. */
    private String name;

    private String code;
    private String district;
    private String taluk;
    private String gramPanchayat;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;

    /** Degrees/courses under this zone: PU, SSLC, ITI, DIPLOMA, DEGREE, ... */
    @Builder.Default
    private List<String> courses = new ArrayList<>();

    /** KPMSYEP kit details — fields TBD (left open per spec). */
    @Builder.Default
    private List<String> kitDetails = new ArrayList<>();

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
