package com.vincent.msyep.modules.student.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Send the document packet to the selected students' emails. */
public record StudentMailRequest(
        @NotEmpty List<String> studentIds,
        String subject,
        String body
) {}
