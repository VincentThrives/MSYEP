package com.vincent.msyep.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public student self-registration. The student chooses a password here and then signs in with their
 * mobile number or email as the username. (OTP sign-in is currently disabled — see AuthController.)
 */
public class StudentSelfRegister {

    public record Request(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Mobile number is required")
            @Pattern(regexp = "\\d{10}", message = "Enter a valid 10-digit mobile number") String phone,
            @Email(message = "Enter a valid email") String email,
            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters") String password,
            @NotBlank(message = "Please re-enter the password") String confirmPassword,
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
