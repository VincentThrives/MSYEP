package com.vincent.msyep.modules.admin;

import com.vincent.msyep.common.ApiResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Admin-managed giver/approval signature used across every generated PDF. */
@RestController
@RequestMapping("/api/v1/admin/signature")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminSignatureController {

    private final AdminSignatureService service;

    public AdminSignatureController(AdminSignatureService service) {
        this.service = service;
    }

    /** Whether a custom signature has been uploaded (vs. the bundled default). */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of("custom", service.hasCustom()));
    }

    /** The current signature image (uploaded override or bundled default) for preview. */
    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<ByteArrayResource> image() {
        byte[] png = service.get();
        if (png == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache())
                .body(new ByteArrayResource(png));
    }

    /** Upload/replace the admin signature; applies to all PDFs immediately. */
    @PostMapping
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        service.save(file);
        return ApiResponse.ok("Admin signature updated — it now applies to all PDFs.", Map.of("custom", true));
    }

    /** Revert to the bundled default signature. */
    @DeleteMapping
    public ApiResponse<Map<String, Object>> reset() {
        service.reset();
        return ApiResponse.ok("Reverted to the default signature.", Map.of("custom", false));
    }
}
