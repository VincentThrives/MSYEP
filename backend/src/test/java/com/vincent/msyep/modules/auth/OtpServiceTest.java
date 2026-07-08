package com.vincent.msyep.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the student OTP login service.
 * Pure logic — no Spring context, no database.
 */
class OtpServiceTest {

    private OtpService otp;

    @BeforeEach
    void setUp() {
        otp = new OtpService();
    }

    @Test
    @DisplayName("generate() returns a fresh 6-digit numeric code")
    void generateReturnsSixDigits() {
        String code = otp.generate("student@example.com");
        assertThat(code).hasSize(6).matches("\\d{6}");
    }

    @Test
    @DisplayName("verify() succeeds for the correct code")
    void verifyCorrectCode() {
        String code = otp.generate("a@b.com");
        assertThat(otp.verify("a@b.com", code)).isTrue();
    }

    @Test
    @DisplayName("verify() is single-use — a second attempt with the same code fails")
    void codeIsConsumedOnSuccess() {
        String code = otp.generate("a@b.com");
        assertThat(otp.verify("a@b.com", code)).isTrue();
        assertThat(otp.verify("a@b.com", code)).isFalse();
    }

    @Test
    @DisplayName("verify() fails for a wrong code but the correct one still works after")
    void wrongThenCorrect() {
        String code = otp.generate("a@b.com");
        assertThat(otp.verify("a@b.com", "000000")).isFalse();
        assertThat(otp.verify("a@b.com", code)).isTrue();
    }

    @Test
    @DisplayName("verify() locks out after 5 wrong attempts, even for the correct code")
    void locksOutAfterMaxAttempts() {
        String code = otp.generate("a@b.com");
        for (int i = 0; i < 5; i++) {
            assertThat(otp.verify("a@b.com", "999999")).isFalse();
        }
        // 6th attempt with the CORRECT code is rejected — entry is locked.
        assertThat(otp.verify("a@b.com", code)).isFalse();
    }

    @Test
    @DisplayName("verify() returns false for an identifier that was never issued an OTP")
    void unknownIdentifier() {
        assertThat(otp.verify("nobody@example.com", "123456")).isFalse();
    }

    @Test
    @DisplayName("identifier is case-insensitive and trimmed")
    void identifierNormalised() {
        String code = otp.generate("  Student@Example.COM  ");
        assertThat(otp.verify("student@example.com", code)).isTrue();
    }

    @Test
    @DisplayName("verify() tolerates whitespace around the submitted code")
    void codeIsTrimmed() {
        String code = otp.generate("a@b.com");
        assertThat(otp.verify("a@b.com", "  " + code + "  ")).isTrue();
    }

    @Test
    @DisplayName("verify() handles null code without throwing")
    void nullCode() {
        otp.generate("a@b.com");
        assertThat(otp.verify("a@b.com", null)).isFalse();
    }

    @Test
    @DisplayName("regenerating issues a fresh, usable code")
    void regenerateIssuesUsableCode() {
        otp.generate("a@b.com");
        String latest = otp.generate("a@b.com"); // replaces the first
        assertThat(otp.verify("a@b.com", latest)).isTrue();
    }
}
