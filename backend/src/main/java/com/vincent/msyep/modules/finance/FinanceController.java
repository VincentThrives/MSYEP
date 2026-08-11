package com.vincent.msyep.modules.finance;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.finance.dto.FinanceRow;
import com.vincent.msyep.modules.finance.dto.SendMailRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceService service;
    private final GpBlueprintPdfService gpBlueprint;

    public FinanceController(FinanceService service, GpBlueprintPdfService gpBlueprint) {
        this.service = service;
        this.gpBlueprint = gpBlueprint;
    }

    /** GP "Blue Print" financial packet (cover + Kannada requisition letter + invoice) on the letterhead. */
    @GetMapping("/gp-blueprint-pdf")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.ByteArrayResource> gpBlueprint(
            @RequestParam(required = false) String gramPanchayat,
            @RequestParam(required = false) String taluk,
            @RequestParam(required = false) String district) {
        byte[] pdf = gpBlueprint.build(gramPanchayat, taluk, district);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=gp-blueprint.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(new org.springframework.core.io.ByteArrayResource(pdf));
    }

    /** The finance documents table: filter by district / taluk / gram panchayat / center. */
    @GetMapping("/students")
    public ApiResponse<List<FinanceRow>> students(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String taluk,
            @RequestParam(required = false) String gramPanchayat,
            @RequestParam(required = false) String centerId) {
        return ApiResponse.ok(service.fetch(district, taluk, gramPanchayat, centerId));
    }

    /** Dropdown options. */
    @GetMapping("/filters")
    public ApiResponse<Map<String, Object>> filters() {
        return ApiResponse.ok(Map.of(
                "districts", service.distinctDistricts(),
                "taluks", service.distinctTaluks(),
                "gramPanchayats", service.distinctGramPanchayats()
        ));
    }

    /** Auto-load the GP email for a selected gram panchayat. */
    @GetMapping("/gp-email")
    public ApiResponse<Map<String, String>> gpEmail(@RequestParam String gramPanchayat) {
        return ApiResponse.ok(Map.of("email",
                String.valueOf(service.resolveGpEmail(gramPanchayat))));
    }

    @PostMapping("/send-mail")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','STAFF')")
    public ApiResponse<Map<String, String>> sendMail(@Valid @RequestBody SendMailRequest req) {
        return ApiResponse.ok("Mail dispatch complete", service.sendDocuments(req));
    }

    /** Sent-mail history (most recent first) for the Finance wing. */
    @GetMapping("/mail-history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','STAFF')")
    public ApiResponse<List<MailLog>> mailHistory() {
        return ApiResponse.ok(service.mailHistory());
    }

    // ---- Gram Panchayat email mapping management ----
    @GetMapping("/gram-panchayats")
    public ApiResponse<List<GramPanchayat>> listGp() {
        return ApiResponse.ok(service.listGramPanchayats());
    }

    @PostMapping("/gram-panchayats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','STAFF')")
    public ApiResponse<GramPanchayat> saveGp(@RequestBody GramPanchayat gp) {
        return ApiResponse.ok("Saved", service.saveGramPanchayat(gp));
    }

    @DeleteMapping("/gram-panchayats/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE','STAFF')")
    public ApiResponse<Void> deleteGp(@PathVariable String id) {
        service.deleteGramPanchayat(id);
        return ApiResponse.ok("Deleted", null);
    }
}
