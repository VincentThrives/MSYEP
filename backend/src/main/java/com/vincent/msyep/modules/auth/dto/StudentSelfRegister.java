package com.vincent.msyep.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Public student self-registration. Login is OTP-only, so no password is collected. */
public class StudentSelfRegister {

    public record Request(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Mobile number is required")
            @Pattern(regexp = "\\d{10}", message = "Enter a valid 10-digit mobile number") String phone,
            @Email(message = "Enter a valid email") String email,
            String gender,
            String dateOfBirth,
            String educationalQualification,
            String zoneId,
            String centerId,
            String district,
            String taluk,
            String gramPanchayat
    ) {}

    public record Result(
            String registerNo,
            String loginId,
            String message
    ) {}
}
