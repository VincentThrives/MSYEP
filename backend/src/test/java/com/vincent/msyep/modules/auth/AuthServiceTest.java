package com.vincent.msyep.modules.auth;

import com.vincent.msyep.config.security.JwtUtil;
import com.vincent.msyep.modules.auth.dto.LoginRequest;
import com.vincent.msyep.modules.auth.dto.StudentSelfRegister;
import com.vincent.msyep.modules.student.Student;
import com.vincent.msyep.modules.student.StudentRegistrationService;
import com.vincent.msyep.modules.student.StudentRepository;
import com.vincent.msyep.modules.student.dto.StudentRegistrationResult;
import com.vincent.msyep.modules.user.Role;
import com.vincent.msyep.modules.user.User;
import com.vincent.msyep.modules.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for password login rejection of students and public self-registration guards. */
class AuthServiceTest {

    private UserRepository users;
    private StudentRepository students;
    private StudentRegistrationService studentReg;
    private PasswordEncoder encoder;
    private JwtUtil jwt;
    private OtpService otp;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        students = mock(StudentRepository.class);
        studentReg = mock(StudentRegistrationService.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtUtil.class);
        otp = mock(OtpService.class);
        auth = new AuthService(users, students, studentReg, encoder, jwt, otp, Optional.empty(), "");
    }

    @Test
    void passwordLoginRejectsStudents() {
        User student = User.builder().email("s@x.com").role(Role.STUDENT).active(true).passwordHash("h").build();
        when(users.findByEmail("s@x.com")).thenReturn(Optional.of(student));
        assertThatThrownBy(() -> auth.login(new LoginRequest("s@x.com", "pw")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void passwordLoginFailsForUnknownEmail() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login(new LoginRequest("nobody@x.com", "pw")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void selfRegisterRejectsMismatchedPasswords() {
        StudentSelfRegister.Request req = new StudentSelfRegister.Request(
                "Name", "9000000001", "m@x.com", "Pass@1234", "Different@1",
                null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> auth.registerStudent(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    void selfRegisterRejectsDuplicateMobile() {
        when(students.findByPhone("9999999999")).thenReturn(Optional.of(new Student()));
        StudentSelfRegister.Request req = new StudentSelfRegister.Request(
                "Name", "9999999999", null, "Pass@1234", "Pass@1234",
                null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> auth.registerStudent(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selfRegisterRejectsExistingEmail() {
        when(students.findByPhone(any())).thenReturn(Optional.empty());
        when(users.existsByEmail("e@x.com")).thenReturn(true);
        StudentSelfRegister.Request req = new StudentSelfRegister.Request(
                "Name", "9000000000", "e@x.com", "Pass@1234", "Pass@1234",
                null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> auth.registerStudent(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selfRegisterSucceeds() {
        when(students.findByPhone("9000000000")).thenReturn(Optional.empty());
        when(users.existsByEmail(any())).thenReturn(false);
        Student saved = new Student();
        saved.setId("S1");
        // Self-registration now passes the chosen password through, so stub the two-argument overload.
        when(studentReg.register(any(Student.class), any()))
                .thenReturn(new StudentRegistrationResult(saved, "MSYEP2026000001", "BATCH-2026-001", "login", "note"));
        StudentSelfRegister.Request req = new StudentSelfRegister.Request(
                "Name", "9000000000", "e@x.com", "Pass@1234", "Pass@1234",
                null, null, null, null, null, null, null, null);

        StudentSelfRegister.Result res = auth.registerStudent(req);

        assertThat(res.registerNo()).isEqualTo("MSYEP2026000001");
        verify(studentReg).register(any(Student.class), any());
    }
}
