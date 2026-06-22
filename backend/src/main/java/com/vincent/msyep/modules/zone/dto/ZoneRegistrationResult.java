package com.vincent.msyep.modules.zone.dto;

import com.vincent.msyep.modules.zone.Zone;

/** Returned after a franchise/zone sign-up. */
public record ZoneRegistrationResult(
        Zone zone,
        String zoneCode,
        String loginId,
        String status,
        Integer membershipAmount,
        String note
) {}
