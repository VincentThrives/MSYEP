package com.vincent.msyep.modules.zone.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Send the franchise Certificate + MOU to the selected zones' emails. */
public record ZoneMailRequest(
        @NotEmpty List<String> zoneIds,
        String subject,
        String body
) {}
