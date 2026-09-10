package com.vincent.msyep.modules.zone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * On-disk cache for generated franchise MOUs.
 *
 * <p>Building a MOU rewrites a 42-page document three times and takes ~11 seconds, so repeat
 * downloads of an unchanged zone are pure waste. Entries are keyed by a fingerprint of everything
 * that can change the output (zone fields, its uploaded documents, the admin signature), so a stale
 * PDF can never be served — a changed input simply produces a different key, which misses.
 *
 * <p>Deliberately file-backed rather than in-memory: each MOU is ~7 MB, and holding several in the
 * heap would undo the memory work that stopped this endpoint throwing OutOfMemoryError. On hosts with
 * an ephemeral filesystem the cache simply starts empty after a deploy, which is a miss, not a bug.
 */
@Component
public class MouCache {

    private static final Logger log = LoggerFactory.getLogger(MouCache.class);
    private static final String SEP = "__";

    private final Path dir;

    public MouCache(@Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.dir = Paths.get(uploadsDir, "mou-cache");
    }

    /** Cached PDF for this zone+key, or null when absent/unreadable (caller then rebuilds). */
    public byte[] get(String zoneId, String key) {
        Path f = entry(zoneId, key);
        if (!Files.exists(f)) return null;
        try {
            byte[] pdf = Files.readAllBytes(f);
            log.debug("MOU cache hit for zone {} ({} bytes)", zoneId, pdf.length);
            return pdf;
        } catch (IOException e) {
            log.warn("MOU cache unreadable for zone {}: {}", zoneId, e.getMessage());
            return null;
        }
    }

    /**
     * Store this build and drop any earlier entry for the same zone, so the cache holds at most one
     * PDF per zone instead of growing with every edit.
     */
    public void put(String zoneId, String key, byte[] pdf) {
        if (pdf == null || pdf.length == 0) return;
        try {
            Files.createDirectories(dir);
            prune(zoneId);
            // Write to a temp file first so a crash mid-write can never leave a truncated "hit".
            Path tmp = dir.resolve(entryName(zoneId, key) + ".tmp");
            Files.write(tmp, pdf);
            Files.move(tmp, entry(zoneId, key), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.debug("MOU cached for zone {} ({} bytes)", zoneId, pdf.length);
        } catch (Exception e) {
            // A cache failure must never break the download.
            log.warn("Could not cache MOU for zone {}: {}", zoneId, e.getMessage());
        }
    }

    /** Forget every cached build for a zone (used when the zone or its documents change). */
    public void evict(String zoneId) {
        try {
            prune(zoneId);
        } catch (Exception e) {
            log.warn("Could not evict MOU cache for zone {}: {}", zoneId, e.getMessage());
        }
    }

    private void prune(String zoneId) throws IOException {
        if (!Files.exists(dir)) return;
        String prefix = safe(zoneId) + SEP;
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().startsWith(prefix)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) { }
            });
        }
    }

    private Path entry(String zoneId, String key) {
        return dir.resolve(entryName(zoneId, key) + ".pdf");
    }

    private String entryName(String zoneId, String key) {
        return safe(zoneId) + SEP + safe(key);
    }

    /** Keep file names to characters that are safe on every filesystem. */
    private String safe(String s) {
        return s == null ? "none" : s.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
