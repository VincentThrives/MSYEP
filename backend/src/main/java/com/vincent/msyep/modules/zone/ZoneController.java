package com.vincent.msyep.modules.zone;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.zone.dto.ZoneRegistrationResult;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/zones")
public class ZoneController {

    private final ZoneService service;
    private final ZoneRegistrationService registration;
    private final FranchisePdfService franchisePdf;
    private final FranchiseMailService franchiseMail;

    public ZoneController(ZoneService service, ZoneRegistrationService registration,
                          FranchisePdfService franchisePdf, FranchiseMailService franchiseMail) {
        this.service = service;
        this.registration = registration;
        this.franchisePdf = franchisePdf;
        this.franchiseMail = franchiseMail;
    }

    @GetMapping
    public ApiResponse<List<Zone>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Zone> get(@PathVariable String id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE')")
    public ApiResponse<ZoneRegistrationResult> create(@RequestBody Zone zone) {
        ZoneRegistrationResult result = registration.register(zone);
        return ApiResponse.ok(result.note(), result);
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE')")
    public ApiResponse<Zone> uploadDocument(
            @PathVariable String id,
            @RequestParam("type") String type,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Document uploaded", service.attachDocument(id, type, label, file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Zone> update(@PathVariable String id, @RequestBody Zone zone) {
        return ApiResponse.ok("Zone updated", service.update(id, zone));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok("Zone deleted", null);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Map<String, Integer>> importExcel(@RequestParam("file") MultipartFile file) {
        int count = service.importExcel(file);
        return ApiResponse.ok("Imported " + count + " zones", Map.of("imported", count));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<ByteArrayResource> pdf(@PathVariable String id) {
        byte[] pdf = service.toPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zone-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    /** Personalised franchise MOU (template with logos/name/signatures applied). */
    @GetMapping("/{id}/mou")
    public ResponseEntity<ByteArrayResource> mou(@PathVariable String id) {
        byte[] pdf = franchisePdf.buildMou(service.findById(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Franchise-MOU-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    /** Standalone franchise certificate PDF. */
    @GetMapping("/{id}/certificate")
    public ResponseEntity<ByteArrayResource> certificate(@PathVariable String id) {
        byte[] pdf = franchisePdf.buildCertificate(service.findById(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Franchise-Certificate-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    /** Generate the certificate + MOU and email both to the franchise (call after uploads finish). */
    @PostMapping("/{id}/send-documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE')")
    public ApiResponse<Map<String, String>> sendDocuments(@PathVariable String id) {
        String note = franchiseMail.sendDocuments(service.findById(id));
        return ApiResponse.ok(note, Map.of("note", note));
    }
}
