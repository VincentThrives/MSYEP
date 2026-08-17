package com.vincent.msyep.modules.center;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.config.security.MsyepPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Verifies the centers list is scoped by the logged-in role. */
class CenterControllerScopeTest {

    private CenterService service;
    private CenterRegistrationService registration;
    private CenterController controller;

    @BeforeEach
    void setUp() {
        service = mock(CenterService.class);
        registration = mock(CenterRegistrationService.class);
        controller = new CenterController(service, registration,
                mock(CenterBatchApprovalPdfService.class), mock(CenterMailService.class));
    }

    private MsyepPrincipal principal(String role, String zoneId, String centerId) {
        return new MsyepPrincipal("u1", "u@x.com", role, zoneId, centerId, null);
    }

    @Test
    void zoneLoginSeesOnlyItsZoneCenters() {
        when(service.findByZone("Z1")).thenReturn(List.of());
        controller.list(null, principal("ZONE", "Z1", null));
        verify(service).findByZone("Z1");
        verify(service, never()).findAll();
    }

    @Test
    void centerLoginSeesOnlyItsOwnCenter() {
        when(service.findById("C1")).thenReturn(new Center());
        ApiResponse<List<Center>> resp = controller.list(null, principal("CENTER", null, "C1"));
        verify(service).findById("C1");
        assertThat(resp.getData()).hasSize(1);
    }

    @Test
    void adminSeesAll() {
        when(service.findAll()).thenReturn(List.of());
        controller.list(null, principal("SUPER_ADMIN", null, null));
        verify(service).findAll();
    }

    @Test
    void adminCanFilterByZoneParam() {
        when(service.findByZone("Z2")).thenReturn(List.of());
        controller.list("Z2", principal("SUPER_ADMIN", null, null));
        verify(service).findByZone("Z2");
    }
}
