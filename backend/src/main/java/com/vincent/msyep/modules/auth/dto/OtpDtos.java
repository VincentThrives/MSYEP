package com.vincent.msyep.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpDtos {

    /** Student enters their User ID / email or mobile number. */
    public record OtpRequest(@NotBlank String identifier) {}

    public record OtpVerify(@NotBlank String identifier, @NotBlank String otp) {}

    /** {@code devOtp} is populated only when no delivery channel is configured (local/dev). */
    public record OtpRequestResult(boolean sent, String target, String message, String devOtp) {}
}
