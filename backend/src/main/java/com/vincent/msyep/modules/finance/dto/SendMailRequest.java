package com.vincent.msyep.modules.finance.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Send student FW documents to the auto-resolved Gram Panchayat email(s). */
public record SendMailRequest(
        @NotEmpty List<String> studentIds,
        /** Optional override; if blank the GP email mapped to each student is used. */
        String overrideEmail,
        String subject,
        String body
) {}
