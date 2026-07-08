package com.vincent.msyep.modules.zone;

import com.vincent.msyep.common.CounterService;
import com.vincent.msyep.modules.user.UserRepository;
import com.vincent.msyep.modules.zone.dto.ZoneRegistrationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for franchise membership tier pricing and required-name validation. */
class ZoneRegistrationServiceTest {

    private ZoneRepository zones;
    private UserRepository users;
    private CounterService counters;
    private PasswordEncoder encoder;
    private ZoneRegistrationService svc;

    @BeforeEach
    void setUp() {
        zones = mock(ZoneRepository.class);
        users = mock(UserRepository.class);
        counters = mock(CounterService.class);
        encoder = mock(PasswordEncoder.class);
        svc = new ZoneRegistrationService(zones, users, counters, encoder);
        when(counters.next("zone")).thenReturn(1L);
        when(zones.save(any(Zone.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ZoneRegistrationResult registerWithTier(String tier) {
        Zone z = new Zone();
        z.setName("Test Org");
        z.setMembershipTier(tier);
        return svc.register(z);
    }

    @Test
    void silverIs75k() {
        assertThat(registerWithTier("Silver").membershipAmount()).isEqualTo(75000);
    }

    @Test
    void goldIs100k() {
        assertThat(registerWithTier("Gold").membershipAmount()).isEqualTo(100000);
    }

    @Test
    void platinumIs125k() {
        assertThat(registerWithTier("Platinum").membershipAmount()).isEqualTo(125000);
    }

    @Test
    void diamondIs150k() {
        assertThat(registerWithTier("Diamond").membershipAmount()).isEqualTo(150000);
    }

    @Test
    void requiresOrganizationOrZoneName() {
        assertThatThrownBy(() -> svc.register(new Zone())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsToPendingStatus() {
        assertThat(registerWithTier("Gold").status()).isEqualTo("PENDING");
    }
}
