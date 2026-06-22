package com.vincent.msyep.modules.auth.dto;

public record AuthResponse(
        String token,
        String userId,
        String name,
        String email,
        String role,
        String zoneId,
        String centerId,
        String studentId
) {}
