package com.vincent.msyep.modules.center.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Send the Batch Approval PDF to the selected centers' emails. */
public record CenterMailRequest(
        @NotEmpty List<String> centerIds,
        String subject,
        String body
) {}
