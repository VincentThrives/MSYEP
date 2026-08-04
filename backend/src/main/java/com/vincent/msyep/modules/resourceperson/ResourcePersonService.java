package com.vincent.msyep.modules.resourceperson;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.center.Center;
import com.vincent.msyep.modules.center.CenterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

/** Center resource-person requests: save, fetch, and render the request letter. */
@Service
public class ResourcePersonService {

    private static final Logger log = LoggerFactory.getLogger(ResourcePersonService.class);
    private static final String LETTERHEAD = "cba-letterhead.pdf";
    /** Letterhead header band height (logos + ISO line + organizer phone) and footer band (addresses). */
    private static final float HEADER_H = 156f;
    private static final float FOOTER_H = 66f;

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
             PdfDocument pdf = new PdfDocument(writer)) {

            // Print the letter on the YKTK letterhead: the full letterhead page (header + grey arc +
            // footer) is drawn as the background of every page, and the body flows between them.
            boolean framed = applyLetterhead(pdf);

            try (Document doc = new Document(pdf)) {
                doc.setFont(com.itextpdf.kernel.font.PdfFontFactory.createFont(
                        com.itextpdf.io.font.constants.StandardFonts.TIMES_ROMAN));
                if (framed) doc.setMargins(HEADER_H + 12, 45, FOOTER_H + 12, 45);

            doc.add(new Paragraph("Resource Person Requisition Letter").setBold().setFontSize(13)
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

            doc.add(new Paragraph("We look forward to your kind cooperation.").setFontSize(10).setMarginTop(8));
            // Leave blank space above the sign-off for a physical signature.
            doc.add(new Paragraph("Yours sincerely,").setFontSize(10).setMarginTop(48));
            doc.add(new Paragraph(centerName).setFontSize(10).setMarginTop(0));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build letter: " + e.getMessage());
        }
        return out.toByteArray();
    }

    /**
     * Draw the full YKTK letterhead page as the background of every page. Returns true when the
     * letterhead was applied (so the caller can reserve top/bottom margins for it).
     */
    private boolean applyLetterhead(PdfDocument pdf) {
        PdfFormXObject letterhead;
        try (InputStream in = new ClassPathResource(LETTERHEAD).getInputStream();
             PdfDocument lh = new PdfDocument(new PdfReader(in))) {
            letterhead = lh.getPage(1).copyAsFormXObject(pdf);
        } catch (Exception e) {
            log.warn("resource-person letterhead missing: {}", e.getMessage());
            return false;
        }
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            PdfPage page = ((PdfDocumentEvent) event).getPage();
            Rectangle ps = page.getPageSize();
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
            canvas.addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, ps.getWidth(), ps.getHeight()));
        });
        return true;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
