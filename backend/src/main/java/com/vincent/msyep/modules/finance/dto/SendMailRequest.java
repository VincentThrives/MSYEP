package com.vincent.msyep.modules.finance.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Send student FW documents to the auto-resolved Gram Panchayat email(s). */
public record SendMailRequest(
        @NotEmpty List<String> studentIds,
        /** Optional override; if blank the GP email mapped to each student is used. */
        String overrideEmail,
        /** Explicit recipient GP mail IDs picked in the finance wing; takes precedence over overrideEmail. */
        List<String> recipientEmails,
        String subject,
        String body,
        /** When set, the GP Blue Print packet for this Gram Panchayat is attached to the mail. */
        String gramPanchayat,
        String taluk,
        String district
) {}
