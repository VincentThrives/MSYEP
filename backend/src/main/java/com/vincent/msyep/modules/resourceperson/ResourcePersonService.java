package com.vincent.msyep.modules.resourceperson;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.center.Center;
import com.vincent.msyep.modules.center.CenterRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

/** Center resource-person requests: save, fetch, and render the request letter. */
@Service
public class ResourcePersonService {

    private final ResourcePersonRepository repo;
    private final CenterRepository centers;

    public ResourcePersonService(ResourcePersonRepository repo, CenterRepository centers) {
        this.repo = repo;
        this.centers = centers;
    }

    public ResourcePersonRequest get(String centerId) {
        return repo.findByCenterId(centerId).orElse(null);
    }

    public ResourcePersonRequest save(String centerId, int countRequired,
                                      java.util.List<ResourcePersonRequest.Person> persons) {
        ResourcePersonRequest r = repo.findByCenterId(centerId)
                .orElseGet(() -> ResourcePersonRequest.builder()
                        .centerId(centerId).createdAt(Instant.now()).build());
        r.setCountRequired(countRequired);
        r.setPersons(persons == null ? new ArrayList<>() : persons);
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    /** Draft request letter (format to be finalised). */
    public byte[] letterPdf(String centerId) {
        ResourcePersonRequest r = repo.findByCenterId(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("No resource-person request saved"));
        Center center = centers.findById(centerId).orElse(null);
        String centerName = center != null ? nz(center.getName()) : "Center";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("KP-MSYEP").setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Resource Person Requisition Letter").setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(14));

            doc.add(new Paragraph("Date: " + LocalDate.now()).setFontSize(10));
            doc.add(new Paragraph("From: " + centerName).setFontSize(10).setMarginBottom(10));

            doc.add(new Paragraph("To,\nThe Concerned Departments / Organizations,").setFontSize(10));
            doc.add(new Paragraph("Subject: Request for Guest Resource Persons under KP-MSYEP").setBold().setFontSize(10).setMarginTop(6));

            doc.add(new Paragraph(
                    "Respected Sir/Madam,\n\nWe, " + centerName + ", request the nomination of "
                            + r.getCountRequired() + " resource person(s) to guide our students under the "
                            + "Kaushalya Patha - MSYEP programme. The proposed resource persons / organizations are:")
                    .setFontSize(10).setMarginTop(6));

            List list = new List().setFontSize(10).setMarginLeft(10);
            for (ResourcePersonRequest.Person p : r.getPersons()) {
                StringBuilder line = new StringBuilder();
                if (StringUtils.hasText(p.getName())) line.append(p.getName());
                if (StringUtils.hasText(p.getDesignation())) line.append(" (").append(p.getDesignation()).append(")");
                if (StringUtils.hasText(p.getOrganization())) {
                    if (line.length() > 0) line.append(" - ");
                    line.append(p.getOrganization());
                }
                if (StringUtils.hasText(p.getPhone())) line.append(" · ").append(p.getPhone());
                if (line.length() > 0) list.add(new ListItem(line.toString()));
            }
            doc.add(list);

            doc.add(new Paragraph("\nWe look forward to your kind cooperation.\n\nYours sincerely,\n"
                    + centerName).setFontSize(10).setMarginTop(8));
            doc.add(new Paragraph("\n(Draft format — to be finalised.)")
                    .setItalic().setFontSize(8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build letter: " + e.getMessage());
        }
        return out.toByteArray();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
