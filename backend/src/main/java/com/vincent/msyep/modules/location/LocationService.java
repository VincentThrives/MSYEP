package com.vincent.msyep.modules.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationRepository repo;
    private final MongoTemplate mongo;

    public LocationService(LocationRepository repo, MongoTemplate mongo) {
        this.repo = repo;
        this.mongo = mongo;
    }

    /**
     * Seed the full Karnataka district / taluk / gram-panchayat master on first boot.
     * Re-seeds automatically if no GP rows exist yet (e.g. upgrading from an older taluk-only seed).
     */
    @PostConstruct
    void seed() {
        long gpCount = mongo.count(
                Query.query(Criteria.where("gramPanchayat").ne(null)), Location.class);
        if (gpCount > 0) return;
        try (InputStream in = new ClassPathResource("karnataka-gram-panchayats.json").getInputStream()) {
            List<Location> rows = new ObjectMapper()
                    .readValue(in, new TypeReference<List<Location>>() {});
            // Dedupe by district|taluk|gp to respect the unique compound index.
            Map<String, Location> uniq = new LinkedHashMap<>();
            for (Location r : rows) {
                uniq.putIfAbsent(r.getDistrict() + "|" + r.getTaluk() + "|" + r.getGramPanchayat(), r);
            }
            repo.deleteAll();
            repo.saveAll(uniq.values());
            log.info("Seeded {} Karnataka gram panchayats (districts/taluks derived)", uniq.size());
        } catch (Exception e) {
            log.warn("Could not seed Karnataka locations: {}", e.getMessage());
        }
    }

    public List<String> districts() {
        return mongo.findDistinct(new Query(), "district", Location.class, String.class)
                .stream().filter(StringUtils::hasText).sorted().toList();
    }

    public List<String> taluks(String district) {
        Query q = Query.query(Criteria.where("district").is(district).and("taluk").ne(null));
        return mongo.findDistinct(q, "taluk", Location.class, String.class)
                .stream().filter(StringUtils::hasText).sorted().toList();
    }

    public List<String> gramPanchayats(String district, String taluk) {
        Query q = Query.query(Criteria.where("district").is(district)
                .and("taluk").is(taluk).and("gramPanchayat").ne(null));
        return mongo.findDistinct(q, "gramPanchayat", Location.class, String.class)
                .stream().filter(StringUtils::hasText).sorted().toList();
    }

    /** Add a new district (top of the hierarchy). Appears in every district dropdown site-wide. */
    public String addDistrict(String name) {
        String d = norm(name);
        if (!StringUtils.hasText(d)) throw new IllegalArgumentException("District name is required");
        if (districts().stream().anyMatch(x -> x.equalsIgnoreCase(d))) {
            throw new IllegalArgumentException("District already exists: " + d);
        }
        repo.save(Location.builder().district(d).build());
        return d;
    }

    /** Add a taluk under a district. */
    public void addTaluk(String district, String taluk) {
        String dd = norm(district), tt = norm(taluk);
        if (!StringUtils.hasText(dd) || !StringUtils.hasText(tt)) {
            throw new IllegalArgumentException("District and Taluk name are required");
        }
        if (taluks(dd).stream().anyMatch(x -> x.equalsIgnoreCase(tt))) {
            throw new IllegalArgumentException("Taluk already exists in " + dd + ": " + tt);
        }
        repo.save(Location.builder().district(dd).taluk(tt).build());
    }

    /** Add a village / gram panchayat under a district + taluk. */
    public void addGramPanchayat(String district, String taluk, String gp) {
        String dd = norm(district), tt = norm(taluk), gg = norm(gp);
        if (!StringUtils.hasText(dd) || !StringUtils.hasText(tt) || !StringUtils.hasText(gg)) {
            throw new IllegalArgumentException("District, Taluk and Village/GP name are required");
        }
        if (gramPanchayats(dd, tt).stream().anyMatch(x -> x.equalsIgnoreCase(gg))) {
            throw new IllegalArgumentException("Village/GP already exists in " + tt + ": " + gg);
        }
        repo.save(Location.builder().district(dd).taluk(tt).gramPanchayat(gg).build());
    }

    private static String norm(String s) {
        return s == null ? null : s.trim();
    }

    // ---- Rename (edit) ----

    /** Rename a district everywhere it appears (all its taluks / GPs follow). */
    public void renameDistrict(String oldName, String newName) {
        String o = norm(oldName), n = norm(newName);
        if (!StringUtils.hasText(o) || !StringUtils.hasText(n)) {
            throw new IllegalArgumentException("Old and new district names are required");
        }
        if (!o.equalsIgnoreCase(n) && districts().stream().anyMatch(x -> x.equalsIgnoreCase(n))) {
            throw new IllegalArgumentException("District already exists: " + n);
        }
        mongo.updateMulti(Query.query(Criteria.where("district").is(o)),
                Update.update("district", n), Location.class);
    }

    /** Rename a taluk within a district (its GPs follow). */
    public void renameTaluk(String district, String oldTaluk, String newTaluk) {
        String d = norm(district), o = norm(oldTaluk), n = norm(newTaluk);
        if (!StringUtils.hasText(d) || !StringUtils.hasText(o) || !StringUtils.hasText(n)) {
            throw new IllegalArgumentException("District, old and new taluk names are required");
        }
        if (!o.equalsIgnoreCase(n) && taluks(d).stream().anyMatch(x -> x.equalsIgnoreCase(n))) {
            throw new IllegalArgumentException("Taluk already exists in " + d + ": " + n);
        }
        mongo.updateMulti(Query.query(Criteria.where("district").is(d).and("taluk").is(o)),
                Update.update("taluk", n), Location.class);
    }

    /** Rename a village / gram panchayat. */
    public void renameGramPanchayat(String district, String taluk, String oldGp, String newGp) {
        String d = norm(district), t = norm(taluk), o = norm(oldGp), n = norm(newGp);
        if (!StringUtils.hasText(d) || !StringUtils.hasText(t) || !StringUtils.hasText(o) || !StringUtils.hasText(n)) {
            throw new IllegalArgumentException("District, taluk, old and new Village/GP names are required");
        }
        if (!o.equalsIgnoreCase(n) && gramPanchayats(d, t).stream().anyMatch(x -> x.equalsIgnoreCase(n))) {
            throw new IllegalArgumentException("Village/GP already exists in " + t + ": " + n);
        }
        mongo.updateMulti(Query.query(Criteria.where("district").is(d).and("taluk").is(t).and("gramPanchayat").is(o)),
                Update.update("gramPanchayat", n), Location.class);
    }

    // ---- Delete ----

    /** Delete a district and everything under it. */
    public void deleteDistrict(String name) {
        String d = norm(name);
        if (!StringUtils.hasText(d)) throw new IllegalArgumentException("District name is required");
        mongo.remove(Query.query(Criteria.where("district").is(d)), Location.class);
    }

    /** Delete a taluk (and its GPs) within a district. */
    public void deleteTaluk(String district, String taluk) {
        String d = norm(district), t = norm(taluk);
        if (!StringUtils.hasText(d) || !StringUtils.hasText(t)) {
            throw new IllegalArgumentException("District and taluk are required");
        }
        mongo.remove(Query.query(Criteria.where("district").is(d).and("taluk").is(t)), Location.class);
    }

    /** Delete a single village / gram panchayat. */
    public void deleteGramPanchayat(String district, String taluk, String gp) {
        String d = norm(district), t = norm(taluk), g = norm(gp);
        if (!StringUtils.hasText(d) || !StringUtils.hasText(t) || !StringUtils.hasText(g)) {
            throw new IllegalArgumentException("District, taluk and Village/GP are required");
        }
        mongo.remove(Query.query(Criteria.where("district").is(d).and("taluk").is(t).and("gramPanchayat").is(g)),
                Location.class);
    }

    /**
     * Import the location master from an .xlsx.
     * Header row, columns: District | Taluk | Gram Panchayat
     */
    public int importExcel(MultipartFile file) {
        int added = 0;
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            boolean first = true;
            for (Row row : sheet) {
                if (first) { first = false; continue; }
                String district = cell(row, 0);
                String taluk = cell(row, 1);
                String gp = cell(row, 2);
                if (!StringUtils.hasText(district)) continue;
                if (!repo.existsByDistrictAndTalukAndGramPanchayat(district, taluk, gp)) {
                    repo.save(Location.builder().district(district).taluk(taluk).gramPanchayat(gp).build());
                    added++;
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read Excel: " + e.getMessage());
        }
        return added;
    }

    private static String cell(Row row, int idx) {
        var c = row.getCell(idx);
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            default -> null;
        };
    }
}
