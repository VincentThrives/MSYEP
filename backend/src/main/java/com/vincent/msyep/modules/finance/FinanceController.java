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

    public FinanceController(FinanceService service) {
        this.service = service;
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    public ApiResponse<Map<String, String>> sendMail(@Valid @RequestBody SendMailRequest req) {
        return ApiResponse.ok("Mail dispatch complete", service.sendDocuments(req));
    }

    // ---- Gram Panchayat email mapping management ----
    @GetMapping("/gram-panchayats")
    public ApiResponse<List<GramPanchayat>> listGp() {
        return ApiResponse.ok(service.listGramPanchayats());
    }

    @PostMapping("/gram-panchayats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    public ApiResponse<GramPanchayat> saveGp(@RequestBody GramPanchayat gp) {
        return ApiResponse.ok("Saved", service.saveGramPanchayat(gp));
    }

    @DeleteMapping("/gram-panchayats/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    public ApiResponse<Void> deleteGp(@PathVariable String id) {
        service.deleteGramPanchayat(id);
        return ApiResponse.ok("Deleted", null);
    }
}
