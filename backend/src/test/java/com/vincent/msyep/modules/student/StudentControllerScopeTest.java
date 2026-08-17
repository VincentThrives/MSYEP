package com.vincent.msyep.modules.student;

import com.vincent.msyep.config.security.MsyepPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Verifies the View-Students list is scoped by the logged-in role. */
class StudentControllerScopeTest {

    private StudentService service;
    private StudentRegistrationService registration;
    private StudentExportService export;
    private StudentController controller;

    @BeforeEach
    void setUp() {
        service = mock(StudentService.class);
        registration = mock(StudentRegistrationService.class);
        export = mock(StudentExportService.class);
        controller = new StudentController(service, registration, export, mock(StudentMailService.class));
        when(export.filter(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
    }

    private MsyepPrincipal principal(String role, String zoneId, String centerId, String studentId) {
        return new MsyepPrincipal("u1", "u@x.com", role, zoneId, centerId, studentId);
    }

    @Test
    void centerLoginScopedToOwnCenter() {
        controller.filter(null, null, null, null, null, principal("CENTER", null, "C1", null));
        verify(export).filter(isNull(), isNull(), isNull(), eq("C1"), isNull(), isNull(), isNull());
    }

    @Test
    void zoneLoginScopedToOwnZone() {
        controller.filter(null, null, null, null, null, principal("ZONE", "Z1", null, null));
        verify(export).filter(isNull(), isNull(), isNull(), isNull(), eq("Z1"), isNull(), isNull());
    }

    @Test
    void studentLoginScopedToSelf() {
        controller.filter(null, null, null, null, null, principal("STUDENT", null, null, "S1"));
        verify(export).filter(isNull(), isNull(), isNull(), isNull(), isNull(), eq("S1"), isNull());
    }

    @Test
    void adminSeesAllAndHonoursRequestedCenter() {
        controller.filter(null, null, null, "REQ", null, principal("SUPER_ADMIN", null, null, null));
        verify(export).filter(isNull(), isNull(), isNull(), eq("REQ"), isNull(), isNull(), isNull());
    }
}
