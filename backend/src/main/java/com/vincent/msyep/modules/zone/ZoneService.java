package com.vincent.msyep.modules.zone;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ZoneService {

    private final ZoneRepository repo;
    private final String uploadsDir;

    public ZoneService(ZoneRepository repo, @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.repo = repo;
        this.uploadsDir = uploadsDir;
    }

    public List<Zone> findAll() {
        return repo.findAll();
    }

    public Zone findById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
    }

    public Zone create(Zone zone) {
        zone.setId(null);
        zone.setCreatedAt(Instant.now());
        return repo.save(zone);
    }

    public Zone update(String id, Zone c) {
        Zone e = findById(id);
        e.setName(c.getName());
        // code / id / login immutable once generated.
        e.setDistrict(c.getDistrict());
        e.setTaluk(c.getTaluk());
        e.setGramPanchayat(c.getGramPanchayat());
        e.setContactPerson(c.getContactPerson());
        e.setContactEmail(c.getContactEmail());
        e.setContactPhone(c.getContactPhone());
        // Organization
        e.setOrganizationName(c.getOrganizationName());
        e.setHasWebsite(c.isHasWebsite());
        e.setWebsiteLink(c.getWebsiteLink());
        e.setWebsiteBudget(c.getWebsiteBudget());
        e.setBuildingOwnership(c.getBuildingOwnership());
        e.setHasRegisteredCopy(c.isHasRegisteredCopy());
        e.setHasMsme(c.isHasMsme());
        e.setHasGst(c.isHasGst());
        e.setHasNitiAayog(c.isHasNitiAayog());
        e.setHasNgoDarpan(c.isHasNgoDarpan());
        e.setHas12A80G(c.isHas12A80G());
        e.setWillingToComply(c.isWillingToComply());
        // Owner
        e.setOwnerName(c.getOwnerName());
        e.setOwnerDob(c.getOwnerDob());
        e.setOwnerGender(c.getOwnerGender());
        e.setContactNumber(c.getContactNumber());
        e.setAlternateNumber(c.getAlternateNumber());
        e.setEmail(c.getEmail());
        e.setFullAddress(c.getFullAddress());
        e.setCity(c.getCity());
        e.setState(c.getState());
        e.setPincode(c.getPincode());
        e.setOccupation(c.getOccupation());
        e.setEducationalQualification(c.getEducationalQualification());
        // KYC
        e.setAadhaarNumber(c.getAadhaarNumber());
        e.setPanNumber(c.getPanNumber());
        e.setBankAccountDetails(c.getBankAccountDetails());
        // Business fit
        e.setInvestmentCapacity(c.getInvestmentCapacity());
        e.setPreferredLocation(c.getPreferredLocation());
        e.setSpaceOwnership(c.getSpaceOwnership());
        e.setSpaceSqft(c.getSpaceSqft());
        e.setStartTimeline(c.getStartTimeline());
        // Membership
        e.setMembershipTier(c.getMembershipTier());
        e.setMembershipAmount(c.getMembershipAmount());
        e.setTcAccepted(c.isTcAccepted());
        if (c.getStatus() != null) e.setStatus(c.getStatus());
        if (c.getCourses() != null) e.setCourses(c.getCourses());
        if (c.getKitDetails() != null) e.setKitDetails(c.getKitDetails());
        e.setActive(c.isActive());
        e.setUpdatedAt(Instant.now());
        return repo.save(e);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Zone not found: " + id);
        }
        repo.deleteById(id);
    }

    /** Save an uploaded franchise document to disk and attach it to the zone. */
    public Zone attachDocument(String id, String type, String label, MultipartFile file) {
        Zone zone = findById(id);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("No file provided");
        if (file.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("File exceeds 2 MB limit");
        try {
            Path dir = Paths.get(uploadsDir, "zones", id);
            Files.createDirectories(dir);
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String safe = type + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
            file.transferTo(dir.resolve(safe).toAbsolutePath());
            zone.getDocuments().removeIf(d -> type.equals(d.getType()));
            zone.getDocuments().add(new ZoneDocument(type, label, original, file.getSize(),
                    Paths.get("zones", id, safe).toString()));
            return repo.save(zone);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file: " + ex.getMessage());
        }
    }

    /**
     * Bulk import zones from an .xlsx file.
     * Expected header row (col order): Name | Code | District | Contact Person | Contact Email | Contact Phone | Courses(comma-separated)
     */
    public int importExcel(MultipartFile file) {
        List<Zone> toSave = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            boolean first = true;
            for (Row row : sheet) {
                if (first) { first = false; continue; } // skip header
                String name = cell(row, 0);
                if (name == null || name.isBlank()) continue;
                Zone z = Zone.builder()
                        .name(name)
                        .code(cell(row, 1))
                        .district(cell(row, 2))
                        .contactPerson(cell(row, 3))
                        .contactEmail(cell(row, 4))
                        .contactPhone(cell(row, 5))
                        .courses(splitCsv(cell(row, 6)))
                        .build();
                toSave.add(z);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read Excel file: " + e.getMessage());
        }
        repo.saveAll(toSave);
        return toSave.size();
    }

    /** Render a single zone (with its courses) to a PDF byte array. */
    public byte[] toPdf(String id) {
        Zone z = findById(id);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph("MSYEP — Zone Details").setBold().setFontSize(16));
            doc.add(new Paragraph("University / Zone: " + nz(z.getName())));
            doc.add(new Paragraph("Code: " + nz(z.getCode())));
            doc.add(new Paragraph("District: " + nz(z.getDistrict())));
            doc.add(new Paragraph("Contact: " + nz(z.getContactPerson())
                    + "  " + nz(z.getContactPhone()) + "  " + nz(z.getContactEmail())));

            Table table = new Table(2);
            table.addHeaderCell(new Cell().add(new Paragraph("#").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Course / Degree").setBold()));
            int i = 1;
            for (String course : z.getCourses()) {
                table.addCell(String.valueOf(i++));
                table.addCell(course);
            }
            doc.add(new Paragraph("\nCourses / Degrees:").setBold());
            doc.add(table);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF: " + e.getMessage());
        }
        return out.toByteArray();
    }

    private static String cell(Row row, int idx) {
        var c = row.getCell(idx);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> null;
        };
    }

    private static List<String> splitCsv(String v) {
        if (v == null || v.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
    }

    private static String nz(String v) {
        return v == null ? "-" : v;
    }
}
