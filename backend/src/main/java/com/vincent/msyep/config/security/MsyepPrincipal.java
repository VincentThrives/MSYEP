package com.vincent.msyep.config.security;

/** The authenticated principal, carried in the SecurityContext. */
public record MsyepPrincipal(
        String userId,
        String email,
        String role,
        String zoneId,
        String centerId,
        String studentId
) {}
