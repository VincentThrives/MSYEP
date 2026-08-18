package com.vincent.msyep.modules.publicapi;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.center.Center;
import com.vincent.msyep.modules.center.CenterRegistrationService;
import com.vincent.msyep.modules.center.CenterRepository;
import com.vincent.msyep.modules.center.dto.CenterRegistrationResult;
import com.vincent.msyep.modules.zone.ZoneRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Unauthenticated lookups + self-registration used by the public student and center forms.
 * The lookups expose only {id, name} — no sensitive fields.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    public record IdName(String id, String name) {}

    private final ZoneRepository zones;
    private final CenterRepository centers;
    private final CenterRegistrationService centerRegistration;

    public PublicController(ZoneRepository zones, CenterRepository centers,
                            CenterRegistrationService centerRegistration) {
        this.zones = zones;
        this.centers = centers;
        this.centerRegistration = centerRegistration;
    }

    /**
     * Center self-registration: creates the center AND its CENTER login (active immediately), so the
     * center can sign in right away. Same builder the admin/zone create uses — just callable publicly.
     */
    @PostMapping("/register-center")
    public ApiResponse<CenterRegistrationResult> registerCenter(@RequestBody Center center) {
        center.setId(null);            // never trust a client-supplied id
        center.setActive(true);        // self-registered centers are active at once
        CenterRegistrationResult result = centerRegistration.register(center);
        return ApiResponse.ok("Center registered — you can now sign in with your User ID and Password.", result);
    }

    @GetMapping("/zones")
    public ApiResponse<List<IdName>> zones() {
        List<IdName> list = zones.findAll().stream()
                .map(z -> new IdName(z.getId(), z.getName()))
                .sorted(Comparator.comparing(i -> i.name() == null ? "" : i.name(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/centers")
    public ApiResponse<List<IdName>> centers(@RequestParam(required = false) String zoneId) {
        var source = StringUtils.hasText(zoneId) ? centers.findByZoneId(zoneId) : centers.findAll();
        List<IdName> list = source.stream()
                .map(c -> new IdName(c.getId(), c.getName()))
                .sorted(Comparator.comparing(i -> i.name() == null ? "" : i.name(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return ApiResponse.ok(list);
    }
}
