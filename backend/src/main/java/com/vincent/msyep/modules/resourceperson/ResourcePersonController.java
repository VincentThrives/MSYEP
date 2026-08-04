package com.vincent.msyep.modules.resourceperson;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.config.security.MsyepPrincipal;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resource-persons")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER','STAFF')")
public class ResourcePersonController {

    private final ResourcePersonService service;

    public ResourcePersonController(ResourcePersonService service) {
        this.service = service;
    }

    private String centerId(MsyepPrincipal p, String requested) {
        if (p != null && "CENTER".equals(p.role()) && StringUtils.hasText(p.centerId())) return p.centerId();
        if (StringUtils.hasText(requested)) return requested;
        if (p != null && StringUtils.hasText(p.centerId())) return p.centerId();
        throw new IllegalArgumentException("No center context — provide centerId");
    }

    @GetMapping
    public ApiResponse<ResourcePersonRequest> get(@RequestParam(required = false) String centerId,
                                                  @AuthenticationPrincipal MsyepPrincipal p) {
        return ApiResponse.ok(service.get(centerId(p, centerId)));
    }

    public record SaveRequest(int countRequired, List<ResourcePersonRequest.Person> persons) {}

    @PostMapping
    public ApiResponse<ResourcePersonRequest> save(@RequestParam(required = false) String centerId,
                                                   @RequestBody SaveRequest req,
                                                   @AuthenticationPrincipal MsyepPrincipal p) {
        return ApiResponse.ok("Resource person request saved",
                service.save(centerId(p, centerId), req.countRequired(), req.persons()));
    }

    @PostMapping("/letter")
    public ResponseEntity<ByteArrayResource> letter(@RequestParam(required = false) String centerId,
                                                    @AuthenticationPrincipal MsyepPrincipal p) {
        byte[] pdf = service.letterPdf(centerId(p, centerId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Resource-Person-Letter.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }
}
