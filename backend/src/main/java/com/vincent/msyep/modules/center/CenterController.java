package com.vincent.msyep.modules.center;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.center.dto.CenterRegistrationResult;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/centers")
public class CenterController {

    private final CenterService service;
    private final CenterRegistrationService registration;

    public CenterController(CenterService service, CenterRegistrationService registration) {
        this.service = service;
        this.registration = registration;
    }

    @GetMapping
    public ApiResponse<List<Center>> list(@RequestParam(required = false) String zoneId) {
        return ApiResponse.ok(zoneId == null ? service.findAll() : service.findByZone(zoneId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Center> get(@PathVariable String id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE')")
    public ApiResponse<CenterRegistrationResult> create(@RequestBody Center center) {
        CenterRegistrationResult result = registration.register(center);
        return ApiResponse.ok(result.emailNote(), result);
    }

    /** Download the registration PDF for a center (same format that is emailed on creation). */
    @GetMapping("/{id}/registration-pdf")
    public ResponseEntity<ByteArrayResource> registrationPdf(@PathVariable String id) {
        byte[] pdf = registration.pdfFor(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=center-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    /** Upload one center document (max 500 KB) into a named slot. */
    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
    public ApiResponse<Center> uploadDocument(
            @PathVariable String id,
            @RequestParam("type") String type,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Document uploaded", service.attachDocument(id, type, label, file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE')")
    public ApiResponse<Center> update(@PathVariable String id, @RequestBody Center center) {
        return ApiResponse.ok("Center updated", service.update(id, center));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok("Center deleted", null);
    }
}
