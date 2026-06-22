package com.vincent.msyep.modules.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Staff member, scoped to a zone and/or center. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "staff")
public class Staff {

    @Id
    private String id;

    private String name;
    private String designation;
    private String phone;
    private String email;

    @Indexed
    private String zoneId;
    @Indexed
    private String centerId;

    private String district;
    private String taluk;
    private String gramPanchayat;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
