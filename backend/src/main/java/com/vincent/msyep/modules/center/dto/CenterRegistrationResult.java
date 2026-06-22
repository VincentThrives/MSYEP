package com.vincent.msyep.modules.center.dto;

import com.vincent.msyep.modules.center.Center;

/** Returned after a center is registered. */
public record CenterRegistrationResult(
        Center center,
        String centerCode,
        String enrollmentNumber,
        String headLoginId,
        boolean emailSent,
        String emailNote
) {}
