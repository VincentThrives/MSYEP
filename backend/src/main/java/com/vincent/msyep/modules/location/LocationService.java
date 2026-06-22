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
