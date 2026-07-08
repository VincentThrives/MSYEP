package com.vincent.msyep.modules.publicapi;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.center.CenterRepository;
import com.vincent.msyep.modules.zone.ZoneRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Unauthenticated lookups used by the public student self-registration form.
 * Exposes only {id, name} — no sensitive fields.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    public record IdName(String id, String name) {}

    private final ZoneRepository zones;
    private final CenterRepository centers;

    public PublicController(ZoneRepository zones, CenterRepository centers) {
        this.zones = zones;
        this.centers = centers;
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
