package com.vincent.msyep.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Password sign-in credentials.
 *
 * <p>The {@code email} field is really a username: staff/zone/center accounts sign in with their
 * email or User ID, while students may use either their email or their registered mobile number.
 * It is therefore NOT constrained to the e-mail format — a 10-digit phone must be accepted too.
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
