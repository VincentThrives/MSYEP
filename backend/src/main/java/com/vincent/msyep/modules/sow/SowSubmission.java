package com.vincent.msyep.modules.sow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** One KP-MSYEP SOW (Statement of Work) form for a center + program (1..8). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sow_submissions")
@CompoundIndex(name = "center_program", def = "{'centerId':1,'programIndex':1}", unique = true)
public class SowSubmission {

    @Id
    private String id;

    @Indexed
    private String centerId;

    /** 1..8 */
    private int programIndex;

    /** Text/date field values keyed by field key. */
    @Builder.Default
    private Map<String, String> fields = new HashMap<>();

    /** Photos as base64 data URLs, keyed by field key. */
    @Builder.Default
    private Map<String, String> photos = new HashMap<>();

    private Instant createdAt;
    private Instant updatedAt;
}
