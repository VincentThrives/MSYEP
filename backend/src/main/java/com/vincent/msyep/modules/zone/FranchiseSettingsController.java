package com.vincent.msyep.modules.zone;

import com.vincent.msyep.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * One-time franchise assets managed by an admin — currently the Giver (YKTK) signature that is
 * stamped on every franchise certificate + MOU. Stored once and reused for every franchise.
 */
@RestController
@RequestMapping("/api/v1/franchise-settings")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class FranchiseSettingsController {

    private final String uploadsDir;

    public FranchiseSettingsController(@Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    private Path giverPath() {
        return Paths.get(uploadsDir, "system", "giver-signature.png");
    }

    private Path approvalPath() {
        return Paths.get(uploadsDir, "system", "approval-signature.png");
    }

    /** Whether the giver / approval signatures have been uploaded. */
    @GetMapping
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of(
                "giverSignature", Files.exists(giverPath()),
                "approvalSignature", Files.exists(approvalPath())));
    }

    /** Upload / replace the giver (YKTK) signature image (PNG/JPG, transparent background preferred). */
    @PostMapping("/giver-signature")
    public ApiResponse<Map<String, Object>> uploadGiverSignature(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file provided");
        if (file.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("File exceeds 2 MB limit");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) throw new IllegalArgumentException("Please upload an image file");
        try {
            Path dir = Paths.get(uploadsDir, "system");
            Files.createDirectories(dir);
            // Bytes are stored as-is; iText detects the real format from content, not the extension.
            file.transferTo(giverPath().toAbsolutePath());
            return ApiResponse.ok("Giver signature saved", Map.of("giverSignature", true));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store giver signature: " + e.getMessage());
        }
    }

    @DeleteMapping("/giver-signature")
    public ApiResponse<Map<String, Object>> removeGiverSignature() {
        try { Files.deleteIfExists(giverPath()); } catch (Exception ignored) { }
        return ApiResponse.ok("Giver signature removed", Map.of("giverSignature", false));
    }

    /** Upload / replace the YKTK approval signature stamped on every Centers Batch Approval PDF. */
    @PostMapping("/approval-signature")
    public ApiResponse<Map<String, Object>> uploadApprovalSignature(@RequestParam("file") MultipartFile file) {
        storeImage(file, approvalPath());
        return ApiResponse.ok("Approval signature saved", Map.of("approvalSignature", true));
    }

    @DeleteMapping("/approval-signature")
    public ApiResponse<Map<String, Object>> removeApprovalSignature() {
        try { Files.deleteIfExists(approvalPath()); } catch (Exception ignored) { }
        return ApiResponse.ok("Approval signature removed", Map.of("approvalSignature", false));
    }

    private void storeImage(MultipartFile file, Path target) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file provided");
        if (file.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("File exceeds 2 MB limit");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) throw new IllegalArgumentException("Please upload an image file");
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store signature: " + e.getMessage());
        }
    }
}
