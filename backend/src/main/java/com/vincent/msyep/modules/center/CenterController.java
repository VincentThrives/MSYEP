package com.vincent.msyep.modules.center;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.config.security.MsyepPrincipal;
import com.vincent.msyep.modules.center.dto.CenterMailRequest;
import com.vincent.msyep.modules.center.dto.CenterRegistrationResult;
import com.vincent.msyep.modules.finance.MailLog;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/centers")
public class CenterController {

    private final CenterService service;
    private final CenterRegistrationService registration;
    private final CenterBatchApprovalPdfService batchApproval;
    private final CenterMailService centerMail;

    public CenterController(CenterService service, CenterRegistrationService registration,
                            CenterBatchApprovalPdfService batchApproval, CenterMailService centerMail) {
        this.service = service;
        this.registration = registration;
        this.batchApproval = batchApproval;
        this.centerMail = centerMail;
    }

    @GetMapping
    public ApiResponse<List<Center>> list(@RequestParam(required = false) String zoneId,
                                          @AuthenticationPrincipal MsyepPrincipal p) {
        // ZONE → only its own centers; CENTER → only its own center; admins → all (or ?zoneId).
        if (p != null && "ZONE".equals(p.role()) && StringUtils.hasText(p.zoneId())) {
            return ApiResponse.ok(service.findByZone(p.zoneId()));
        }
        if (p != null && "CENTER".equals(p.role()) && StringUtils.hasText(p.centerId())) {
            try {
                return ApiResponse.ok(List.of(service.findById(p.centerId())));
            } catch (Exception ignored) {
                return ApiResponse.ok(List.of());
            }
        }
        return ApiResponse.ok(StringUtils.hasText(zoneId) ? service.findByZone(zoneId) : service.findAll());
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

    /** Download the filled "Centers Batch Approval" PDF for a center (per-center data). */
    @GetMapping("/{id}/batch-approval-pdf")
    public ResponseEntity<ByteArrayResource> batchApprovalPdf(@PathVariable String id,
                                                              @RequestParam(required = false, defaultValue = "false") boolean inline) {
        byte[] pdf = batchApproval.build(id);
        String disposition = (inline ? "inline" : "attachment") + "; filename=batch-approval-" + id + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    /** Bulk-send the Batch Approval PDF to selected centers (Center Mail page). */
    @PostMapping("/send-mail")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<java.util.Map<String, String>> sendMail(@Valid @RequestBody CenterMailRequest req) {
        List<Center> centers = req.centerIds().stream()
                .map(id -> { try { return service.findById(id); } catch (Exception e) { return null; } })
                .filter(java.util.Objects::nonNull).toList();
        return ApiResponse.ok("Mail dispatch complete", centerMail.sendToCenters(centers, req.subject(), req.body()));
    }

    /** Sent-mail history for the Center wing. */
    @GetMapping("/mail-history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<List<MailLog>> mailHistory() {
        return ApiResponse.ok(centerMail.history());
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
