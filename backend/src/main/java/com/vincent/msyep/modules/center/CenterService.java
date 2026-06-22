package com.vincent.msyep.modules.center;

import com.vincent.msyep.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Service
public class CenterService {

    private final CenterRepository repo;
    private final String uploadsDir;

    public CenterService(CenterRepository repo,
                         @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.repo = repo;
        this.uploadsDir = uploadsDir;
    }

    public List<Center> findAll() {
        return repo.findAll();
    }

    public List<Center> findByZone(String zoneId) {
        return repo.findByZoneId(zoneId);
    }

    public Center findById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found: " + id));
    }

    public Center create(Center center) {
        center.setId(null);
        center.setCreatedAt(Instant.now());
        return repo.save(center);
    }

    public Center update(String id, Center changes) {
        Center e = findById(id);
        e.setName(changes.getName());
        e.setCenterType(changes.getCenterType());
        e.setCenterHeadUserId(changes.getCenterHeadUserId());
        // code / enrollment / batchCode / id / login are immutable once generated.
        e.setAcademicYear(changes.getAcademicYear());
        e.setAcademicStartMonth(changes.getAcademicStartMonth());
        e.setAcademicEndMonth(changes.getAcademicEndMonth());
        e.setZoneId(changes.getZoneId());
        e.setAddress(changes.getAddress());
        e.setLocality(changes.getLocality());
        e.setPincode(changes.getPincode());
        e.setContactNumber(changes.getContactNumber());
        e.setEmail(changes.getEmail());
        e.setDistrict(changes.getDistrict());
        e.setTaluk(changes.getTaluk());
        e.setGramPanchayat(changes.getGramPanchayat());
        e.setBatchYear(changes.getBatchYear());
        e.setPrincipalName(changes.getPrincipalName());
        e.setPrincipalNumber(changes.getPrincipalNumber());
        e.setUucmsCoordinatorName(changes.getUucmsCoordinatorName());
        e.setUucmsCoordinatorNumber(changes.getUucmsCoordinatorNumber());
        e.setScstCoordinatorName(changes.getScstCoordinatorName());
        e.setScstCoordinatorNumber(changes.getScstCoordinatorNumber());
        e.setPlacementCoordinatorName(changes.getPlacementCoordinatorName());
        e.setPlacementCoordinatorPhone(changes.getPlacementCoordinatorPhone());
        e.setOfficeNumber(changes.getOfficeNumber());
        e.setHasWebsite(changes.isHasWebsite());
        e.setWebsiteLink(changes.getWebsiteLink());
        e.setCourses(changes.getCourses());
        e.setTotalStrength(changes.getTotalStrength());
        e.setStrengthTotal(changes.getStrengthTotal());
        e.setStrengthSC(changes.getStrengthSC());
        e.setStrengthST(changes.getStrengthST());
        e.setStrengthGeneral(changes.getStrengthGeneral());
        e.setDateOfMou(changes.getDateOfMou());
        e.setMouEndDate(changes.getMouEndDate());
        e.setContractDuration(changes.getContractDuration());
        e.setContactPhone(changes.getContactPhone());
        e.setActive(changes.isActive());
        e.setUpdatedAt(Instant.now());
        return repo.save(e);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Center not found: " + id);
        }
        repo.deleteById(id);
    }

    /** Save an uploaded document to disk and attach it to the center. */
    public Center attachDocument(String id, String type, String label, MultipartFile file) {
        Center center = findById(id);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }
        if (file.getSize() > 500 * 1024) {
            throw new IllegalArgumentException("File exceeds 500 KB limit");
        }
        try {
            Path dir = Paths.get(uploadsDir, "centers", id);
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String safe = type + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = dir.resolve(safe);
            file.transferTo(target.toAbsolutePath());

            // Replace any existing doc of the same type.
            center.getDocuments().removeIf(d -> type.equals(d.getType()));
            center.getDocuments().add(new CenterDocument(
                    type, label, original, file.getSize(),
                    Paths.get("centers", id, safe).toString()));
            return repo.save(center);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file: " + ex.getMessage());
        }
    }
}
