package com.vincent.msyep.modules.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A record of one Finance-wing mail dispatch, shown in the "Sent Mail History". */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mail_logs")
public class MailLog {

    @Id
    private String id;

    /** FINANCE | ZONE | CENTER — which wing sent this mail. */
    private String channel;

    private Instant sentAt;

    private List<String> recipients;   // distinct GP mail IDs the packet went to
    private String subject;
    private String body;

    private String gramPanchayat;
    private String taluk;
    private String district;

    private List<String> studentIds;
    private List<String> studentNames;

    private String attachment;         // e.g. "GP-Blueprint-<gp>.pdf", or null

    private int sent;                  // students successfully dispatched
    private int total;                 // students attempted
    private String status;             // "3/3 sent", "stub — SMTP not configured", ...
    private boolean stub;              // true when SMTP is not configured (logged, not emailed)

    private Map<String, String> results; // per-student outcome (SENT / FAILED / NO_GP_EMAIL / NOT_FOUND)
}
