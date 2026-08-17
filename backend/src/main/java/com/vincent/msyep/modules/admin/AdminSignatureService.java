package com.vincent.msyep.modules.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vincent.msyep.common.SignatureImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The single, admin-managed "YKTK / Admin" signature used as the GIVER / approval signature across
 * every generated PDF (franchise certificate + MOU, center batch approval, GP requisition + invoice).
 * Uploading a new one here changes it everywhere at once; receiver signatures (zone head / center
 * principal) are separate per-entity uploads and are not affected.
 */
@Service
public class AdminSignatureService {

    private static final Logger log = LoggerFactory.getLogger(AdminSignatureService.class);
    private static final String FILE = "admin-signature.png";
    private static final String DEFAULT_ASSET = "sign-yktk.png";
    private final String uploadsDir;

    public AdminSignatureService(@Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    private Path file() {
        return Paths.get(uploadsDir, "system", FILE);
    }

    /** True when an admin has uploaded a custom signature (vs. the bundled default). */
    public boolean hasCustom() {
        return Files.exists(file());
    }

    /** Current admin/giver signature bytes: the uploaded override, else legacy files, else the bundled default. */
    public byte[] get() {
        if (Files.exists(file())) {
            try { return Files.readAllBytes(file()); } catch (Exception e) { log.warn("admin signature unreadable: {}", e.getMessage()); }
        }
        // Honour any legacy per-flow files that may already exist on disk.
        for (String legacy : new String[]{"giver-signature.png", "approval-signature.png"}) {
            Path lp = Paths.get(uploadsDir, "system", legacy);
            if (Files.exists(lp)) {
                try { return Files.readAllBytes(lp); } catch (Exception ignored) { }
            }
        }
        try (InputStream in = new ClassPathResource(DEFAULT_ASSET).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            log.warn("default signature asset missing: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Store a new admin signature. The upload (image OR a scanned-signature PDF) is normalised to a
     * transparent-background PNG — paper made transparent, trimmed to the ink — so the giver stamp never
     * paints a box over any PDF it appears on.
     */
    public void save(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) throw new IllegalArgumentException("No file provided");
        if (upload.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("File exceeds the 2 MB limit");
        try {
            byte[] cleaned = SignatureImage.clean(upload.getBytes());
            if (cleaned == null) throw new IllegalArgumentException("Unsupported or empty signature file");
            Path dir = Paths.get(uploadsDir, "system");
            Files.createDirectories(dir);
            Files.write(dir.resolve(FILE), cleaned);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store signature: " + e.getMessage());
        }
    }

    /** Revert to the bundled default signature. */
    public void reset() {
        try { Files.deleteIfExists(file()); } catch (Exception e) { log.warn("signature reset failed: {}", e.getMessage()); }
    }

}
