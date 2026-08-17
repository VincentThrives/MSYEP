package com.vincent.msyep.modules.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
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

    /** Store a new admin signature: near-white pixels are made transparent and the image is trimmed to the ink. */
    public void save(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) throw new IllegalArgumentException("No file provided");
        if (upload.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("File exceeds the 2 MB limit");
        try {
            BufferedImage src = ImageIO.read(upload.getInputStream());
            if (src == null) throw new IllegalArgumentException("Unsupported image file");
            BufferedImage cleaned = cleanTrim(src, 235);
            Path dir = Paths.get(uploadsDir, "system");
            Files.createDirectories(dir);
            try (OutputStream os = Files.newOutputStream(dir.resolve(FILE))) {
                ImageIO.write(cleaned, "png", os);
            }
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

    /** Make near-white pixels transparent and crop to the ink bounding box (so any photo of a sign works). */
    private static BufferedImage cleanTrim(BufferedImage in, int whiteThresh) {
        int w = in.getWidth(), h = in.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int argb = in.getRGB(x, y);
            int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
            boolean bg = a < 24 || (r >= whiteThresh && g >= whiteThresh && b >= whiteThresh);
            if (bg) {
                out.setRGB(x, y, 0x00000000);
            } else {
                out.setRGB(x, y, argb);
                if (x < minX) minX = x; if (x > maxX) maxX = x;
                if (y < minY) minY = y; if (y > maxY) maxY = y;
            }
        }
        if (maxX < 0) return out;   // fully blank — keep as-is
        int pad = 6;
        minX = Math.max(0, minX - pad); minY = Math.max(0, minY - pad);
        maxX = Math.min(w - 1, maxX + pad); maxY = Math.min(h - 1, maxY + pad);
        return out.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
