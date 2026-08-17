package com.vincent.msyep.modules.center;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.vincent.msyep.modules.zone.Zone;
import com.vincent.msyep.modules.zone.ZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Fills the bundled 6-page "Centers Batch Approval" template with a single center's own data and
 * frames every page on the YKTK letterhead (header band on top, footer band on bottom).
 *
 * <p>Page 2 is rebuilt from scratch: the old coordinator/strength table is dropped and replaced with
 * the full set of center fields captured on the create-center form (Academic → Location → Details →
 * Courses). Pages 1/3/5 keep their template layout, filled with the center's name/code/MoU dates and
 * the principal + YKTK approval signatures, then scaled to sit between the header and footer.
 */
@Service
public class CenterBatchApprovalPdfService {

    private static final Logger log = LoggerFactory.getLogger(CenterBatchApprovalPdfService.class);
    private static final String TEMPLATE = "center-batch-approval.pdf";
    private static final String LETTERHEAD = "cba-letterhead.pdf";
    /** Letterhead header band height (logos + ISO line + organizer phone) and footer band (addresses). */
    private static final float HEADER_H = 156f;
    private static final float FOOTER_H = 66f;
    private static final DeviceRgb LABEL_BG = new DeviceRgb(238, 243, 240);
    private static final DeviceRgb LINE = new DeviceRgb(200, 210, 205);

    private final CenterRepository centers;
    private final ZoneRepository zones;
    private final com.vincent.msyep.modules.admin.AdminSignatureService adminSignature;
    private final String uploadsDir;

    public CenterBatchApprovalPdfService(CenterRepository centers, ZoneRepository zones,
                                         com.vincent.msyep.modules.admin.AdminSignatureService adminSignature,
                                         @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.centers = centers;
        this.zones = zones;
        this.adminSignature = adminSignature;
        this.uploadsDir = uploadsDir;
    }

    public byte[] build(String centerId) {
        Center c = centers.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Center not found: " + centerId));
        Zone zone = StringUtils.hasText(c.getZoneId()) ? zones.findById(c.getZoneId()).orElse(null) : null;

        String mouFrom = firstNonBlank(c.getDateOfMou(), zone != null ? zone.getIssueDate() : null);
        String mouTo = firstNonBlank(c.getMouEndDate(), zone != null ? zone.getValidTill() : null);
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String place = firstNonBlank(c.getLocality(), c.getGramPanchayat(), c.getTaluk(), c.getDistrict());
        // Clean both signatures to transparent-background stamps (handles PDF / photo uploads) so they
        // never paint a paper box over the approval.
        byte[] principalSig = com.vincent.msyep.common.SignatureImage.clean(readCenterDoc(c, "principalSignature"));
        byte[] adminSig = com.vincent.msyep.common.SignatureImage.clean(adminSignature.get());   // central admin/giver signature

        // Print every page on the full YKTK letterhead (header + grey arc + footer). The static template
        // content is composited as transparent page-images so the letterhead arc shows through uncut;
        // per-center data is drawn on top as vector overlays.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument lh = new PdfDocument(new PdfReader(new ClassPathResource(LETTERHEAD).getInputStream()));
             PdfDocument dst = new PdfDocument(new PdfWriter(out))) {

            // Flat opaque raster of the letterhead so its grey arc never composites over the content.
            PdfImageXObject letterhead = new PdfImageXObject(ImageDataFactory.create(classpathBytes("cba-letterhead.png")));
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);

            java.util.Map<Integer, PdfImageXObject> pageImg = new java.util.HashMap<>();
            for (int p : new int[]{1}) {
                byte[] b = classpathBytes("cba-p" + p + ".png");
                if (b != null) pageImg.put(p, new PdfImageXObject(ImageDataFactory.create(b)));
            }

            Rectangle ref = lh.getPage(1).getPageSize();
            float w = ref.getWidth(), h = ref.getHeight();
            float scale = (h - HEADER_H - FOOTER_H) / h;
            float tx = (w - w * scale) / 2f, ty = FOOTER_H;

            for (int i = 1; i <= 6; i++) {
                PdfPage op = dst.addNewPage(new PageSize(w, h));
                PdfCanvas cv = new PdfCanvas(op);
                // Letterhead background (arc runs the full height, uncut).
                cv.addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, w, h));

                if (i == 2) {
                    buildPage2(dst, op, w, h, HEADER_H, FOOTER_H, bold, reg, c, place, today, adminSig);
                    continue;
                }
                if (i == 3) {
                    buildPage3(op, w, h, HEADER_H, FOOTER_H, bold, reg, c, mouFrom, mouTo, today, principalSig, adminSig);
                    continue;
                }
                if (i == 4) {
                    buildPage4(op, w, h, HEADER_H, FOOTER_H, bold, reg);
                    continue;
                }
                if (i == 5) {
                    buildPage5(op, w, h, HEADER_H, FOOTER_H, bold, reg, c, today, principalSig, adminSig);
                    continue;
                }
                if (i == 6) {
                    buildPage6(op, w, h, HEADER_H, FOOTER_H, bold, reg);
                    continue;
                }
                // Transparent static content — the letterhead (and its arc) shows through the whitespace.
                PdfImageXObject pg = pageImg.get(i);
                if (pg != null) cv.addXObjectFittedIntoRectangle(pg, new Rectangle(tx, ty, w * scale, h * scale));
                // Per-center overlays, mapped through the same scale/offset as the page image.
                if (i == 1) overlayPage1(cv, bold, c, scale, tx, ty);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build batch approval PDF: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- page 2 (rebuilt)

    private void buildPage2(PdfDocument dst, PdfPage page, float w, float h, float headerH, float footerH,
                            PdfFont bold, PdfFont reg, Center c, String place, String today, byte[] adminSig) {
        Rectangle band = new Rectangle(30, footerH + 6, w - 60, (h - headerH) - (footerH) - 12);
        try (Canvas canvas = new Canvas(new PdfCanvas(page), band)) {
            canvas.add(new Paragraph("Information of the “Kaushalya Patha” College — Center Details")
                    .setFont(bold).setFontSize(12).setTextAlignment(TextAlignment.CENTER)
                    .setUnderline().setMarginTop(0).setMarginBottom(3));
            canvas.add(new Paragraph("Subject: Complete college information & “Kaushalya Patha” College Co-Ordinator details.")
                    .setFont(bold).setFontSize(8.5f).setMarginBottom(6));

            Table t = new Table(UnitValue.createPercentArray(new float[]{27, 23, 27, 23})).useAllAvailableWidth();
            // Academic & Type
            pair(t, bold, reg, "Academic Year", nz(c.getAcademicYear()), "Academic Period", period(c));
            pair(t, bold, reg, "Center / College Name", nz(c.getName()), "Center Type", nz(c.getCenterType()));
            pair(t, bold, reg, "Center Code", nz(c.getCode()), "Enrollment No", nz(c.getEnrollmentNumber()));
            pair(t, bold, reg, "Center Login ID", nz(c.getUserId()), "Batch Year", nz(c.getBatchYear()));
            // Merged from the (now-removed) registration account PDF — each field appears once here.
            pair(t, bold, reg, "Center ID", nz(c.getId()), "Batch Code", nz(c.getBatchCode()));
            pair(t, bold, reg, "Registration Date", nz(c.getRegistrationDate()), "Date of MOU", nz(c.getDateOfMou()));
            pair(t, bold, reg, "Contract Duration", nz(c.getContractDuration()), "Contact Number", nz(c.getContactNumber()));
            // Location
            pair(t, bold, reg, "District", nz(c.getDistrict()), "Taluk", nz(c.getTaluk()));
            pair(t, bold, reg, "Village / Gram Panchayat", nz(c.getGramPanchayat()), "Pincode", nz(c.getPincode()));
            wide(t, bold, reg, "Address", nz(c.getAddress()));
            pair(t, bold, reg, "Locality", nz(c.getLocality()), "Center Mail-ID", nz(c.getEmail()));
            // Details — contacts
            pair(t, bold, reg, "Office Number", nz(c.getOfficeNumber()), "Website", nz(c.getWebsiteLink()));
            pair(t, bold, reg, "Principal Name", nz(c.getPrincipalName()), "Principal Number", nz(c.getPrincipalNumber()));
            pair(t, bold, reg, "UUCMS / Computer Op. Coordinator", nz(c.getUucmsCoordinatorName()),
                    "UUCMS Coordinator Number", nz(c.getUucmsCoordinatorNumber()));
            pair(t, bold, reg, "SC-ST Cell Coordinator", nz(c.getScstCoordinatorName()),
                    "SC-ST Coordinator Number", nz(c.getScstCoordinatorNumber()));
            pair(t, bold, reg, "Placement (Kaushalya Patha) Coordinator", nz(c.getPlacementCoordinatorName()),
                    "Placement Coordinator Phone", nz(c.getPlacementCoordinatorPhone()));
            // Courses & strengths
            wide(t, bold, reg, "Courses", c.getCourses() == null ? "" : String.join(", ", c.getCourses()));
            pair(t, bold, reg, "College Total Strength", intOr(c.getTotalStrength()),
                    "3rd/4th Sem Total", intOr(c.getStrengthTotal()));
            pair(t, bold, reg, "3rd/4th Sem SC", intOr(c.getStrengthSC()), "3rd/4th Sem ST", intOr(c.getStrengthST()));
            pair(t, bold, reg, "3rd/4th Sem General / Others", intOr(c.getStrengthGeneral()), "", "");
            canvas.add(t);

            String coord = nz(c.getPlacementCoordinatorName());
            canvas.add(new Paragraph().setFontSize(8.5f).setMarginTop(6).setMarginBottom(2)
                    .add(new Text("I on behalf of M/s ").setFont(reg))
                    .add(new Text(coord.isEmpty() ? "____________________" : coord).setFont(bold))
                    .add(new Text(" undertake to inform “Kaushalya patha” MSYEP, in case of any changes in the above mentioned statement.").setFont(reg)));
            canvas.add(new Paragraph().setFontSize(8.5f).setMarginBottom(4)
                    .add(new Text("I am principal of ").setFont(reg))
                    .add(new Text(nz(c.getPrincipalName()).isEmpty() ? "____________________" : nz(c.getPrincipalName())).setFont(bold))
                    .add(new Text(", reffering the above mentioned College details and “Kaushlaya patha” College co-ordinator details.").setFont(reg)));

            // Place / Date on the left, Admin sign on the right.
            Table foot = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth().setMarginTop(6);
            Cell left = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .add(new Paragraph("Place: " + nz(place)).setFont(reg).setFontSize(8.5f).setMargin(0))
                    .add(new Paragraph("Date: " + today).setFont(reg).setFontSize(8.5f).setMargin(0));
            Cell right = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.CENTER);
            if (adminSig != null) {
                try {
                    Image img = new Image(ImageDataFactory.create(adminSig)).setAutoScale(false);
                    img.scaleToFit(120, 38);
                    right.add(new Paragraph().add(img).setMargin(0).setTextAlignment(TextAlignment.CENTER));
                } catch (Exception ignored) { }
            }
            right.add(new Paragraph("Admin Sign/- & Seal").setFont(bold).setFontSize(9).setMargin(0).setTextAlignment(TextAlignment.CENTER));
            foot.addCell(left);
            foot.addCell(right);
            canvas.add(foot);
        } catch (Exception e) {
            log.warn("page 2 rebuild failed: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- page 3 (MoU, rebuilt as native text)

    private void buildPage3(PdfPage page, float w, float h, float headerH, float footerH,
                            PdfFont bold, PdfFont reg, Center c, String mouFrom, String mouTo,
                            String today, byte[] principalSig, byte[] adminSig) {
        Rectangle band = new Rectangle(30, footerH + 6, w - 60, (h - headerH) - footerH - 12);
        try (Canvas canvas = new Canvas(new PdfCanvas(page), band)) {
            float fs = 6.8f;
            canvas.add(new Paragraph("Memorandum of Understanding").setFont(bold).setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER).setUnderline().setMarginBottom(2));
            canvas.add(new Paragraph().setFont(reg).setFontSize(fs).setMarginBottom(2).setTextAlignment(TextAlignment.JUSTIFIED)
                    .add(new Text("The Memorandum of understanding (MoU) is made on "))
                    .add(new Text(fmt(mouFrom)).setFont(bold))
                    .add(new Text(" between “Kaushalya Patha” Yuktha kaushalya Tarabethi Kendra (MSYEP) a Registered Trust under Karnataka Trust Registration NO: "))
                    .add(new Text("(MDG-4-00078-2016-17C.D NO MDGD 202 DATE 14.11.2016) & also An ISO: 9001 2015 Certified Training Institute having Tumkur District Branch at Ward no-35, #79, 1st floor, 6th cross, Vinayaka circle, devarayana Patna main road, batvadi, Tumkur District - 572132.").setFont(bold)));
            canvas.add(new Paragraph().setFont(reg).setFontSize(fs).setMarginBottom(2).setTextAlignment(TextAlignment.JUSTIFIED)
                    .add(new Text("hereinafter reffered to as MSYEP and "))
                    .add(new Text(nz(c.getName()).isEmpty() ? "____________________" : nz(c.getName()).toUpperCase()).setFont(bold))
                    .add(new Text(" hereinafter referred to as “College”")));
            canvas.add(new Paragraph("MSYEP Is a Trust that provides MSYEP Skill Training to the Pre-University Board, ITI, Diploma, GTTC, Tumkur University Affiliated College & also for Hostels students to Empower them to complete career opportunities.")
                    .setFont(bold).setFontSize(fs).setMarginBottom(2).setTextAlignment(TextAlignment.JUSTIFIED));
            canvas.add(new Paragraph("The college has courses such as PUC, ITI, Diploma, BA, BCA, B.Com, BBA, BBM, BSc, MA, M.Com, etc. for boys and/or girls.")
                    .setFont(bold).setFontSize(fs).setMarginBottom(2));
            canvas.add(new Paragraph().setFontSize(fs).setMarginBottom(1)
                    .add(new Text("Project: YKTK ").setFont(reg)).add(new Text("“Kaushalya Patha”").setFont(bold)));
            canvas.add(new Paragraph().setFontSize(fs).setMarginBottom(1)
                    .add(new Text("Program: ").setFont(reg)).add(new Text("MSYEP (Multi Skills Youth Empowerment Program)").setFont(bold)));
            canvas.add(new Paragraph().setFontSize(fs).setMarginBottom(3)
                    .add(new Text("Validity: This MoU is For 2 years, From ").setFont(reg))
                    .add(new Text(fmt(mouFrom)).setFont(bold))
                    .add(new Text(" to ").setFont(reg))
                    .add(new Text(fmt(mouTo)).setFont(bold))
                    .add(new Text(" (After 2 years we can renew the MOU)").setFont(reg)));
            canvas.add(new Paragraph("In Consideration of the intent contained herein, MSYEP & College agrees as follows:")
                    .setFont(bold).setFontSize(fs).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
            canvas.add(new Paragraph().setFontSize(fs).setMarginBottom(2).setTextAlignment(TextAlignment.JUSTIFIED)
                    .add(new Text("1. Objective Of MSYEP: ").setFont(bold))
                    .add(new Text("To Empower Students with the necessary skills and prepare them for careers By Giving them awareness of Govt. Departmental and Companies Information and benefits.").setFont(reg)));
            canvas.add(new Paragraph("Under this agreement, MSYEP shall issue a Statement of Work (hereinafter the “SOW”) mentioning the Scope of work, the schedule, class duration, the KP MSYEP program kit, the Certificate fee, and the Funds for College terms.")
                    .setFont(bold).setFontSize(fs).setMarginBottom(2).setTextAlignment(TextAlignment.JUSTIFIED));
            canvas.add(new Paragraph().setFontSize(fs).setMarginBottom(2).setTextAlignment(TextAlignment.JUSTIFIED)
                    .add(new Text("2. Confidential Information: ").setFont(bold))
                    .add(new Text("The college agrees to hold all confidential information in strict confidence MSYEP.").setFont(reg)));
            canvas.add(new Paragraph("3. Termination of the Agreement:").setFont(bold).setFontSize(fs).setMarginBottom(1));
            for (String s : new String[]{
                    "If there is a violation of the generally accepted code of conduct by either party, the other party reserves the right to cancel this MOU by giving written notice.",
                    "It is understood that before termination, both parties will make every effort to resolve differences amicably before termination.",
                    "Each party will be required to give 90 days written notice to the other party with reasons for normal termination of this MOU before its validity expires.",
                    "The payment will be made within 10 days from the date of batch completion and the KP-MSYEP kit submission."})
                canvas.add(bullet(reg, fs, s));
            canvas.add(new Paragraph().setFontSize(fs).setMarginBottom(2).setTextAlignment(TextAlignment.JUSTIFIED)
                    .add(new Text("4. MSYEP Agreement: ").setFont(bold))
                    .add(new Text("The status of the college “Kaushalya-Patha” Coordinator will not be an employee of MSYEP.").setFont(reg)));
            canvas.add(new Paragraph("5. Roles and Responsibilities :").setFont(bold).setFontSize(fs).setMarginBottom(1));
            for (String s : new String[]{
                    "“Kaushalya-Path” MSYEP will provide a Statement of Work (SOW) guidelines.",
                    "MSYEP training is for PUC, ITI, Diploma, GTTC, 3rd & 4th-semester students (BA, B.com, BCA, BSC, BBA, BBM & etc...)",
                    "MSYEP will provide 02 Coordinators Designation- MSYEP LC(Lab-Coordinator) / DI (Data informative)",
                    "MSYEP will send Resource Persons Like Industrial Association members-representatives / Reputed Companies to give awareness & benefits as part of the MSYEP process.",
                    "MSYEP will provide the name board.",
                    "MSYEP will provide free Career-Guidance Brouchers for students.",
                    "The College Will Encourage 3rd & 4th-semester students to attend the “Kaushalya-Patha” MSYEP training program.",
                    "The college will give instructions to the Skill cell to fill the online & offline SOW and make the program successful.",
                    "The college will provide the necessary Students KYC doc(Passport size, Aadhar card, previous year marks card & Caste certificate Xerox copies) (File) of 3rd/4th-semester students to “Kaushalya-Patha” MSYEP training for the smooth conduct of programs.",
                    "MSYEP will NOT use students’ data for any commercial purpose."})
                canvas.add(bullet(reg, fs, s));
            Table sig = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginTop(4);
            sig.addCell(sigCell(bold, reg, "FOR THE COLLEGE", principalSig, "Signature of College Principal",
                    "Name: " + nz(c.getPrincipalName()), "Date: " + today, null));
            sig.addCell(sigCell(bold, reg, "YKTK(R)", adminSig, "Approved by",
                    "Executive Committee", "Designation : Director", "Date: " + today));
            canvas.add(sig);
        } catch (Exception e) {
            log.warn("page 3 rebuild failed: {}", e.getMessage());
        }
    }

    private Paragraph bullet(PdfFont reg, float fs, String s) {
        return new Paragraph().setFont(reg).setFontSize(fs).setMarginBottom(1).setMarginLeft(12)
                .setFirstLineIndent(-8).setTextAlignment(TextAlignment.JUSTIFIED)
                .add(new Text("•  " + s));
    }

    private Cell sigCell(PdfFont bold, PdfFont reg, String top, byte[] sig, String l1, String l2, String l3, String l4) {
        Cell cell = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        cell.add(new Paragraph(top).setFont(bold).setFontSize(8.5f).setMargin(0).setMarginBottom(1));
        boolean placed = false;
        if (sig != null) {
            try {
                Image img = new Image(ImageDataFactory.create(sig)).setAutoScale(false);
                img.scaleToFit(110, 26);
                cell.add(new Paragraph().add(img).setMargin(0));
                placed = true;
            } catch (Exception ignored) { }
        }
        if (!placed) cell.add(new Paragraph(" ").setFontSize(9).setMargin(0));
        cell.add(new Paragraph(l1).setFont(bold).setFontSize(8.5f).setMargin(0));
        cell.add(new Paragraph(l2).setFont(reg).setFontSize(8.5f).setMargin(0));
        cell.add(new Paragraph(l3).setFont(reg).setFontSize(8.5f).setMargin(0));
        if (l4 != null) cell.add(new Paragraph(l4).setFont(reg).setFontSize(8.5f).setMargin(0));
        return cell;
    }

    // ---------------------------------------------------------------- page 4 (SOW-1, rebuilt as native text)

    private void buildPage4(PdfPage page, float w, float h, float headerH, float footerH, PdfFont bold, PdfFont reg) {
        Rectangle band = new Rectangle(30, footerH + 6, w - 60, (h - headerH) - footerH - 12);
        try (Canvas canvas = new Canvas(new PdfCanvas(page), band)) {
            float fs = 8.5f;
            canvas.add(new Paragraph("-page- 01").setFont(reg).setFontSize(7).setTextAlignment(TextAlignment.RIGHT).setMargin(0));
            canvas.add(new Paragraph("Statement of the work (SOW)").setFont(bold).setFontSize(13)
                    .setTextAlignment(TextAlignment.CENTER).setUnderline().setMarginBottom(4));
            canvas.add(new Paragraph("As part of your role, you are expected to follow norms:").setFont(reg).setFontSize(fs).setMarginBottom(2));
            canvas.add(bullet(reg, fs, "Receive the “Kaushalya-patha” kit & Online Work Book link From MSYEP Lab Coordinator."));
            for (String s : new String[]{"(a) Interaction program guest letter.", "(b) Student entrance test paper.",
                    "(c) Student registration QR code.", "(d) AMD Empty file.", "(e) Notepad.", "(f) 01 pen.",
                    "(g) Whiteboard marker pen."})
                canvas.add(new Paragraph(s).setFont(reg).setFontSize(fs).setMargin(0).setMarginLeft(14).setMarginBottom(1));
            String[][] kpf = {
                    {"KPF1", " - College, Interaction program & KP Coordinator details, "},
                    {"KPF2", " - BA&BCOM (3rd/4th sem) students details & Entrance test file, "},
                    {"KPF3", " - Students KP-MSYEP activity groups. "},
                    {"KPF4", " - KP-MSYEP activity proceeding & program flow 08 sheets, "},
                    {"KPF5", " - KP-MSYEP activity guest inviting letter, "},
                    {"KPF6", " - KP-MSYEP activity Anchor-welcome-interaction questions-vote of thanks scripts sheets module, "},
                    {"KPF7", " - Preamble Copy, "},
                    {"KPF8", " - KP-MSYEP activity guest opinion 08 sheets, "},
                    {"KPF9", " - Students grade test paper, "},
                    {"KPF10", " - Students certificate list (An ISO Certified certificate), "},
                    {"KPF11", " - Students opinion sheet, "},
                    {"KPF12", " - College opinion sheet."}};
            Paragraph hp = new Paragraph().setFont(reg).setFontSize(fs).setMargin(0).setMarginLeft(14).setMarginBottom(2)
                    .setTextAlignment(TextAlignment.JUSTIFIED);
            hp.add(new Text("(h) ")).add(new Text("MSYEP online & offline SOW book: ").setFont(bold)).add(new Text("("));
            for (String[] k : kpf) hp.add(new Text(k[0]).setFont(bold)).add(new Text(k[1]));
            hp.add(new Text(")"));
            canvas.add(hp);
            canvas.add(bullet(bold, fs, "Inform In advance (BA & BCOM) 3rd/4th sem students, to bring KYC documents (Aadhaar, previous marks card & caste certificate)."));
            canvas.add(bullet(bold, fs, "The “Kaushalya-patha” Interaction Program date should be Under the chairmanship of the Principal."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-01");
            canvas.add(num(reg, fs, "1. Principal On the date given by the principal Invite an MSYEP Resource person & as a Chief guest for the Interaction program ( Taluk panchayath EO/ AD/ Panning officer or Concerned Gram Panchayat Development Officer)."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-02");
            canvas.add(num(reg, fs, "1. On the same day of the interaction program, the Entrance Test will be there for students & students should register by scanning the QR code with the KYC documents."));
            canvas.add(num(reg, fs, "2. The “Kaushalya-patha” Coordinator should give us the (BA & BCOM) 3rd/4th sem students’ Entrance Test paper and the KYC (AMD Copies in file)."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-03");
            canvas.add(num(reg, fs, "1. After submission of the AMD file."));
            canvas.add(num(reg, fs, "2. Instructing the 3rd/4th sem BA & BCOM Course students to be divided into 4 groups (BA students 4 groups & BCOM students 4 groups)."));
        } catch (Exception e) {
            log.warn("page 4 rebuild failed: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- page 5 (SOW-2 / Forms 04-12, rebuilt as native text)

    private void buildPage5(PdfPage page, float w, float h, float headerH, float footerH,
                            PdfFont bold, PdfFont reg, Center c, String today, byte[] principalSig, byte[] adminSig) {
        Rectangle band = new Rectangle(30, footerH + 6, w - 60, (h - headerH) - footerH - 12);
        try (Canvas canvas = new Canvas(new PdfCanvas(page), band)) {
            float fs = 8f;
            canvas.add(new Paragraph("-page- 02").setFont(reg).setFontSize(7).setTextAlignment(TextAlignment.RIGHT).setMargin(0));
            canvas.add(new Paragraph("Statement of the work (SOW)").setFont(bold).setFontSize(13)
                    .setTextAlignment(TextAlignment.CENTER).setUnderline().setMarginBottom(4));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-04");
            canvas.add(num(reg, fs, "1. According to Tuesday's proceedings from an Individual Group, instructing & encouraging students to write in English the program proceedings with the topic of students’ needed resource persons (the list has been attached)."));
            canvas.add(num(reg, fs, "2. According to Tuesday's proceedings from an Individual Group, instructing students to fill the Columns in form no - 04 page 01 (students’ names & signatures) page-02 (Confirmed RP details)."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-05");
            canvas.add(num(reg, fs, "1. According to the proceedings instruct the anchoring student to type the guest letter and send it to the KP-MSYEP lab coordinator."));
            canvas.add(num(reg, fs, "2. According to the proceedings after sending the guest letter from your end, further lab coordinator will confirm the RP and share their details."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-06");
            canvas.add(num(reg, fs, "1. According to the proceedings instruct & encourage students to organize the program by typing anchoring, welcoming, student interaction Questions, and voting on thanks script/program flow on the computer (script/program flow module sheet & interaction questions sheet have been attached)."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-07");
            canvas.add(num(reg, fs, "1. Before the program starts All the students, teachers, and guests are requested to read the Indian Constitution preamble. (A preamble copy has been attached)."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-08.");
            canvas.add(num(reg, fs, "1. Take the respective guest opinion in the guest opinion sheets."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-09.");
            canvas.add(num(reg, fs, "1. Give the grade test for the students (the grade test paper has been attached)."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-10");
            canvas.add(num(reg, fs, "1. Give the students’ name list to issue the MSYEP certificate (An ISO certified certificate)."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-11");
            canvas.add(num(reg, fs, "1. Take any 2 students’ MSYEP opinion in the given opinion sheet."));
            formHeading(canvas, bold, fs, "Kaushalya Patha Form no-12");
            canvas.add(num(reg, fs, "1. Take the College principal opinion in the given opinion sheet."));
            Table sig = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginTop(6);
            sig.addCell(sigCell(bold, reg, "FOR THE COLLEGE", principalSig, "Signature of College Principal",
                    "Name: " + nz(c.getPrincipalName()), "Designation :", "Date: " + today));
            sig.addCell(sigCell(bold, reg, "YKTK(R)", adminSig, "Approved by",
                    "Executive Committee", "Director", "Date: " + today));
            canvas.add(sig);
        } catch (Exception e) {
            log.warn("page 5 rebuild failed: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- page 6 (Contact, rebuilt as native text)

    private void buildPage6(PdfPage page, float w, float h, float headerH, float footerH, PdfFont bold, PdfFont reg) {
        Rectangle band = new Rectangle(30, footerH + 6, w - 60, (h - headerH) - footerH - 12);
        try (Canvas canvas = new Canvas(new PdfCanvas(page), band)) {
            float fs = 8.7f;
            canvas.add(new Paragraph("7.0 CONTACT").setFont(bold).setFontSize(13).setUnderline().setMarginBottom(5));
            ln(canvas, reg, fs, 0, 4, "1. For all Franchisee Business Transactions, Registration, Sign up, Office and staff Requirement");
            ln(canvas, bold, fs, 14, 0, "Assistant Managing / Franchisee Co-ordinator / Director / Chief program operator");
            ln(canvas, reg, fs, 14, 0, "Email:- yktkmsyep@gmail.com");
            ln(canvas, reg, fs, 14, 0, "Mob:- 8867754671 / 9986993406 / 6366273089");
            ln(canvas, reg, fs, 0, 4, "2. For Issues of MOU (Letter head format, Poster, Baners SOFT COPY), Applications & Course Material Certificates, Syllabus, Fess - Center ID Password, Mail Id Password, Pilot approval");
            ln(canvas, bold, fs, 14, 0, "Job Development Executive (JDE)");
            ln(canvas, reg, fs, 14, 0, "Email: jdemsyep@gmail.com");
            ln(canvas, reg, fs, 14, 0, "Mob:- 6363872279");
            ln(canvas, reg, fs, 0, 4, "3. For TLC TRAINING JDE, TLC DI Interview, Requisition Letter, Center Information, Student online, Registration SOW Book, Entrance Test 16 curriculum activities, 8 theory 8 practical Resource Persons, Class Advice and guidance");
            ln(canvas, bold, fs, 14, 0, "Head -TLC-KP-MSYEP");
            ln(canvas, reg, fs, 14, 0, "Email: tlcmsyep@gmail.com");
            ln(canvas, reg, fs, 14, 0, "Mob: 6363001869");
            ln(canvas, reg, fs, 0, 4, "4. For Centers  A. Batch Approval - (Centers Login Requisition Received Letter Copy, Center Information Copy, Student online application Residence confirmation letter copy, Student Entrance Test Photo, Students Registration Photo Students KP-MSYEP Introduction Program, Collage Photo )");
            ln(canvas, reg, fs, 30, 0, "B. Royalty Approval ( 16 Groups Activities Photo, Anchoring, welcome, vote of thanks, student interaction, Guest opinion, Students opinion copy, student certificate received photo, Center FeedBack.)");
            ln(canvas, bold, fs, 14, 0, "Head DA-KP-MSYEP");
            ln(canvas, reg, fs, 14, 0, "Email: dacmsyep@gmail.com");
            ln(canvas, reg, fs, 14, 0, "Mob : 6363913497");
            ln(canvas, reg, fs, 0, 4, "5. Concerned Job Developer Executive -");
            ln(canvas, reg, fs, 14, 0, "For all proposals, discussions with Government departments, arranging for tests, evaluation and examination, certification for the issue of certificates, follow up with Head Office Instructions, and any other functions related to MSYEP division.");
            ln(canvas, bold, fs, 0, 6, "WEB SITE : https://yukthakaushalyakar.in/");
            ln(canvas, reg, fs, 0, 0, "Email : yktkmsyep@gmail.com");
            ln(canvas, reg, fs, 0, 0, "Whatapp Help Desk : 8867754671");
        } catch (Exception e) {
            log.warn("page 6 rebuild failed: {}", e.getMessage());
        }
    }

    private void ln(Canvas canvas, PdfFont f, float fs, float ml, float mt, String s) {
        canvas.add(new Paragraph(s).setFont(f).setFontSize(fs).setMargin(0).setMarginLeft(ml)
                .setMarginTop(mt).setMarginBottom(0.8f).setTextAlignment(TextAlignment.JUSTIFIED));
    }

    private void formHeading(Canvas canvas, PdfFont bold, float fs, String s) {
        canvas.add(new Paragraph(s).setFont(bold).setFontSize(fs + 0.5f).setUnderline().setMarginTop(3).setMarginBottom(1));
    }

    private Paragraph num(PdfFont reg, float fs, String s) {
        return new Paragraph(s).setFont(reg).setFontSize(fs).setMargin(0).setMarginLeft(14).setMarginBottom(1)
                .setTextAlignment(TextAlignment.JUSTIFIED);
    }

    private void pair(Table t, PdfFont bold, PdfFont reg, String l1, String v1, String l2, String v2) {
        t.addCell(labelCell(l1, bold));
        t.addCell(valueCell(v1, reg));
        if (l2.isEmpty()) {
            t.addCell(new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            t.addCell(new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        } else {
            t.addCell(labelCell(l2, bold));
            t.addCell(valueCell(v2, reg));
        }
    }

    private void wide(Table t, PdfFont bold, PdfFont reg, String label, String val) {
        t.addCell(labelCell(label, bold));
        Cell v = new Cell(1, 3)
                .add(new Paragraph(val == null || val.isEmpty() ? "-" : val).setFont(reg).setFontSize(7.5f).setMargin(0))
                .setBorder(new SolidBorder(LINE, 0.5f)).setPadding(2).setVerticalAlignment(VerticalAlignment.MIDDLE);
        t.addCell(v);
    }

    private Cell labelCell(String s, PdfFont bold) {
        // No fill — keep the cell transparent so the letterhead's grey arc stays visible behind the table.
        return new Cell().add(new Paragraph(s).setFont(bold).setFontSize(7.5f).setMargin(0))
                .setBorder(new SolidBorder(LINE, 0.5f))
                .setPadding(2).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell valueCell(String s, PdfFont reg) {
        return new Cell().add(new Paragraph(s == null || s.isEmpty() ? "-" : s).setFont(reg).setFontSize(7.5f).setMargin(0))
                .setBorder(new SolidBorder(LINE, 0.5f)).setPadding(2).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    // ---------------------------------------------------------------- template fills (pages 1/3/5)

    private void overlayPage1(PdfCanvas cv, PdfFont bold, Center c, float s, float tx, float ty) {
        drawT(cv, bold, nz(c.getName()), 325, 182, 13, 235, s, tx, ty);
        drawT(cv, bold, nz(c.getCode()), 325, 128, 13, 235, s, tx, ty);
        drawT(cv, bold, nz(c.getBatchYear()), 325, 72, 13, 235, s, tx, ty);
    }

    private void overlayPage3(PdfCanvas cv, PdfFont bold, PdfFont reg, Center c,
                              String mouFrom, String mouTo, String today, byte[] principalSig, byte[] adminSig,
                              float s, float tx, float ty) {
        drawT(cv, bold, fmt(mouFrom), 390, 748, 10, 80, s, tx, ty);     // "made on ___"
        drawT(cv, bold, fmt(mouFrom), 222, 540, 9, 54, s, tx, ty);      // validity from (on the blank)
        drawT(cv, bold, fmt(mouTo), 417, 540, 9, 50, s, tx, ty);        // validity to (on the blank)
        imgT(cv, principalSig, new Rectangle(40, 62, 135, 20), s, tx, ty);
        drawT(cv, reg, nz(c.getPrincipalName()), 70, 32, 10, 240, s, tx, ty);   // after "Name:" (label y=45)
        drawT(cv, reg, today, 62, 19, 10, 120, s, tx, ty);                      // after "Date" (label y=32)
        imgT(cv, adminSig, new Rectangle(427, 62, 115, 20), s, tx, ty);
        drawT(cv, reg, today, 449, 6, 10, 110, s, tx, ty);                      // after right "Date" (label y=19)
    }

    private void overlayPage5(PdfCanvas cv, PdfFont reg, Center c, String today,
                              byte[] principalSig, byte[] adminSig, float s, float tx, float ty) {
        imgT(cv, principalSig, new Rectangle(40, 115, 135, 40), s, tx, ty);
        drawT(cv, reg, nz(c.getPrincipalName()), 80, 74, 10, 240, s, tx, ty);   // after "Name:" (label y=91)
        drawT(cv, reg, today, 66, 38, 10, 120, s, tx, ty);                      // after "Date" (label y=55)
        imgT(cv, adminSig, new Rectangle(427, 115, 115, 40), s, tx, ty);
        drawT(cv, reg, today, 474, 38, 10, 100, s, tx, ty);                     // after right "Date" (label y=55)
    }

    /** Draw text using template-space coordinates, mapped through the page-image scale/offset. */
    private void drawT(PdfCanvas cv, PdfFont f, String text, float x, float y, float size, float maxW,
                       float s, float tx, float ty) {
        drawBaseline(cv, f, text, tx + x * s, ty + y * s, size * s, maxW * s);
    }

    private void imgT(PdfCanvas cv, byte[] img, Rectangle box, float s, float tx, float ty) {
        drawImageFitted(cv, img, new Rectangle(tx + box.getX() * s, ty + box.getY() * s,
                box.getWidth() * s, box.getHeight() * s));
    }

    // ---------------------------------------------------------------- helpers

    private PdfCanvas after(PdfDocument pdf, int pageNum) {
        PdfPage page = pdf.getPage(pageNum);
        return new PdfCanvas(page.newContentStreamAfter(), page.getResources(), page.getDocument());
    }

    private PdfExtGState opaqueState() {
        return new PdfExtGState().setFillOpacity(1f).setStrokeOpacity(1f).setSoftMask(PdfName.None);
    }

    private void coverBox(PdfCanvas canvas, Rectangle r) {
        canvas.saveState().setExtGState(opaqueState()).setFillColor(ColorConstants.WHITE)
                .rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight()).fill().restoreState();
    }

    private void drawBaseline(PdfCanvas canvas, PdfFont font, String text, float x, float y, float size, float maxWidth) {
        if (!StringUtils.hasText(text)) return;
        float fs = size;
        while (fs > 6 && font.getWidth(text, fs) > maxWidth) fs -= 0.5f;
        canvas.saveState().setExtGState(opaqueState()).setFillColor(ColorConstants.BLACK)
                .beginText().setFontAndSize(font, fs)
                .setTextRenderingMode(PdfCanvasConstants.TextRenderingMode.FILL)
                .moveText(x, y).showText(text).endText().restoreState();
    }

    private void drawImageFitted(PdfCanvas canvas, byte[] img, Rectangle box) {
        if (img == null) return;
        try {
            PdfImageXObject xo = new PdfImageXObject(ImageDataFactory.create(img));
            Rectangle fit = fitPreservingAspect(xo.getWidth(), xo.getHeight(), box);
            canvas.saveState().setExtGState(opaqueState());
            canvas.addXObjectFittedIntoRectangle(xo, fit);
            canvas.restoreState();
        } catch (Exception e) {
            log.warn("batch approval image stamp failed: {}", e.getMessage());
        }
    }

    private Rectangle fitPreservingAspect(float iw, float ih, Rectangle box) {
        float scale = Math.min(box.getWidth() / iw, box.getHeight() / ih);
        float ww = iw * scale, hh = ih * scale;
        float x = box.getX() + (box.getWidth() - ww) / 2f;
        float y = box.getY() + (box.getHeight() - hh) / 2f;
        return new Rectangle(x, y, ww, hh);
    }

    private byte[] classpathBytes(String name) {
        try (InputStream in = new ClassPathResource(name).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            log.warn("letterhead asset {} missing: {}", name, e.getMessage());
            return null;
        }
    }

    private byte[] readCenterDoc(Center c, String type) {
        if (c.getDocuments() == null) return null;
        for (CenterDocument d : c.getDocuments()) {
            if (type.equals(d.getType()) && StringUtils.hasText(d.getPath())) {
                try {
                    Path p = Paths.get(uploadsDir).resolve(d.getPath().replace("uploads/", ""));
                    if (Files.exists(p)) return Files.readAllBytes(p);
                } catch (Exception e) {
                    log.warn("center doc {} unreadable: {}", type, e.getMessage());
                }
            }
        }
        return null;
    }

    private static String period(Center c) {
        String a = nz(c.getAcademicStartMonth()), b = nz(c.getAcademicEndMonth());
        if (a.isEmpty() && b.isEmpty()) return "";
        return (a.isEmpty() ? "?" : a) + " to " + (b.isEmpty() ? "?" : b);
    }

    private static String fmt(String iso) {
        if (!StringUtils.hasText(iso)) return "";
        try {
            return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }

    private static String intOr(Integer v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (StringUtils.hasText(v)) return v;
        return "";
    }
}
