package com.vincent.msyep.modules.zone;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.RegexBasedLocationExtractionStrategy;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the franchise MOU PDF from the bundled 47-page template, personalised for a zone:
 * deletes the login/registration annexes, replaces the sample franchisee name + logos with the
 * franchise's uploaded assets, and stamps the franchisee + giver signatures onto every page.
 * (Cryptographic PAdES signing + the certificate are layered on separately.)
 */
@Service
public class FranchisePdfService {

    private static final Logger log = LoggerFactory.getLogger(FranchisePdfService.class);

    private static final String TEMPLATE = "franchise-mou-template.pdf";
    /** Sample franchisee name printed on the template letterhead (p19) that we replace. */
    private static final String SAMPLE_NAME = "HARSHA COMPUTER EDUCATION";
    /**
     * Pages to remove from the MOU (1-based): 19 = the sample "Franchisee" letterhead page,
     * 21 & 22 = center/student login annexes, 27 = the franchise certificate (delivered as its
     * own PDF), 47 = stray trailer.
     */
    private static final int[] DELETE_PAGES = {19, 21, 22, 27, 47};
    /** Pages whose embedded logo is replaced by the franchise logo (1-based, template numbering). */
    private static final int[] LOGO_PAGES = {17, 19, 23, 24, 25};
    /** Template page carrying the Franchisee Certificate (Annexure-10). */
    private static final int CERTIFICATE_PAGE = 27;

    private final String uploadsDir;
    private final com.vincent.msyep.modules.admin.AdminSignatureService adminSignature;

    public FranchisePdfService(@Value("${app.uploads-dir:uploads}") String uploadsDir,
                               com.vincent.msyep.modules.admin.AdminSignatureService adminSignature) {
        this.uploadsDir = uploadsDir;
        this.adminSignature = adminSignature;
    }

    /** Personalised MOU bytes (unsigned). */
    public byte[] buildMou(Zone zone) {
        byte[] template = readTemplate();
        if (template == null) throw new IllegalStateException("MOU template not bundled");

        byte[] logo = readZoneDoc(zone, "logo");
        byte[] sign = readZoneDoc(zone, "franchiseeSignature");
        byte[] giver = adminSignature.get();
        // Zone-head / authorised-signatory signature for the "Center Head — Signature with Seal" pages.
        byte[] authSign = readZoneDoc(zone, "authorisedSignatorySignature");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(template)), new PdfWriter(out))) {

            // 1) Delete unwanted pages (descending so numbers stay valid).
            for (int p : sortedDesc(DELETE_PAGES)) {
                if (p <= pdf.getNumberOfPages()) pdf.removePage(p);
            }

            // Template pages shift after deletion — map original template page numbers to current ones.
            // Pages 19, 21 & 22 are removed before p23+, so those shift down by 3.
            int p23 = 23 - 3, p24 = 24 - 3, p25 = 25 - 3;

            // The sample emblem is stripped from EVERY spot regardless of whether the zone uploaded a
            // logo — so it can never appear. When the zone has no logo we composite a transparent pixel,
            // which erases the sample and draws nothing (spot left blank).
            byte[] mark = (logo != null) ? logo : blankPixel();

            // 2) p1 — swap the centre watermark emblem for the zone's own logo, then add the data block.
            replaceP1Watermark(pdf, mark);
            overlayFirstPageData(pdf, 1, zone);

            // 3) Logos — the template's franchise emblem sits at fixed, measured spots on each page.
            int p26 = 26 - 3; // shifts down by 3 (pages 19, 21 & 22 removed before it)
            // p17 & p18 banners: the bundled banner images are already blank (sample emblem erased),
            // so we just composite each zone's own logo into them, then place the banner back.
            recreateBanner(pdf, 17, "banner-p17.png", new Rectangle(82, 377, 453, 225), mark,
                    1575, 32, 150, 150, 0, 0, 0);   // logo sits above the redrawn "Franchisee" label
            recreateBanner(pdf, 18, "banner-p18.png", new Rectangle(9, 366, 575, 223), mark,
                    678, 26, 184, 184, 0, 0, 0);
            overlayLogoAt(pdf, p23, mark, new Rectangle(508, 763, 74, 74));
            overlayLogoAt(pdf, p24, mark, new Rectangle(67, 729, 74, 74));
            overlayLogoAt(pdf, p25, mark, new Rectangle(51, 738, 74, 74));
            overlayLogoAt(pdf, p26, mark, new Rectangle(59, 717, 74, 74));

            // 4) Zone-head signature above the "Center Head" line on Annexure-7/8/9 (template p24/25/26).
            overlaySignatureAt(pdf, p24, authSign, new Rectangle(360, 219, 150, 26));
            overlaySignatureAt(pdf, p25, authSign, new Rectangle(445, 88, 135, 30));
            overlaySignatureAt(pdf, p26, authSign, new Rectangle(390, 282, 140, 32));

            // 4b) p16 — fill the Receiver (zone head) / Giver (YKTK) signature block.
            byte[] receiverSign = (authSign != null) ? authSign : sign;
            byte[] giverSign = (giver != null) ? giver : readAsset("sign-yktk.png");
            fillSignatureBlock(pdf, 16, zone, receiverSign, giverSign);

            // 5) Signatures — footer of every page.
            stampSignaturesAllPages(pdf, sign, giver);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to build MOU: " + e.getMessage(), e);
        }
        // 6) Strip the template's own MAHACHETHANA seal/watermark, then reframe every page INSIDE the
        //    YKTK letterhead — so the document sits cleanly on the letter with no clashing old branding.
        return reframeIntoLetterhead(stripSeals(out.toByteArray()));
    }

    // The old MAHACHETHANA SEVA TRUST seal ships in the template as small raster logos: a 117x113
    // watermark on the cover and a 512x512 crest on the annexure pages. Everything else that is an
    // image (centre-programme photos, scanned government letters) is genuine content we keep.
    private static final int[][] SEAL_DIMS = { {117, 113}, {512, 512} };

    private static boolean isSeal(int w, int h) {
        for (int[] d : SEAL_DIMS) if (d[0] == w && d[1] == h) return true;
        return false;
    }

    /**
     * Blank the template's own seal/watermark images to transparent, in place, leaving all text,
     * tables, photos and scanned letters untouched. Runs in stamping mode so the streams are writable.
     */
    private byte[] stripSeals(byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // 1x1 fully-transparent PNG — a seal image swapped to this draws nothing, at any scale.
            BufferedImage px = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            ByteArrayOutputStream pngBuf = new ByteArrayOutputStream();
            ImageIO.write(px, "png", pngBuf);
            byte[] transparentPng = pngBuf.toByteArray();

            try (PdfDocument doc = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(content)), new PdfWriter(out))) {
                for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                    PdfPage page = doc.getPage(i);
                    stripSealsInResources(page.getResources().getPdfObject(), transparentPng);
                    stripWhiteFills(page, doc);
                }
            }
        } catch (Exception e) {
            log.warn("MOU seal strip failed: {}", e.getMessage());
            return content;   // fall back to the un-stripped content rather than failing the download
        }
        return out.toByteArray();
    }

    private void stripSealsInResources(PdfDictionary res, byte[] transparentPng) {
        if (res == null) return;
        PdfDictionary xobjects = res.getAsDictionary(PdfName.XObject);
        if (xobjects == null) return;
        for (PdfName key : xobjects.keySet()) {
            PdfStream st = xobjects.getAsStream(key);
            if (st == null) continue;
            if (PdfName.Image.equals(st.getAsName(PdfName.Subtype))) {
                Integer w = st.getAsInt(PdfName.Width), h = st.getAsInt(PdfName.Height);
                if (w != null && h != null && isSeal(w, h)) blankImageStream(st, transparentPng);
            } else {
                stripSealsInResources(st.getAsDictionary(PdfName.Resources), transparentPng);   // nested form XObject
            }
        }
    }

    // Every template page (and the content forms nested inside it) paints opaque WHITE rectangles as
    // backgrounds before the text/tables. Those white boxes block the letterhead's grey arc and — where
    // a box's straight edge crosses the arc — chop it into a hard vertical cut. We neutralise the fill of
    // every white rectangle (turn its `f`/`F`/`f*` into a no-op `n`), leaving the text, lines, borders and
    // colours untouched, so the arc sweeps smoothly behind every page exactly like the cover.
    private static final java.util.regex.Pattern WHITE_FILL = java.util.regex.Pattern.compile(
            "(1 1 1 rg[\\s\\S]{0,150}?(?:[\\d.]+ [\\d.]+ [\\d.]+ [\\d.]+ re\\s+)+)[fF]\\*?\\b");

    private byte[] neutralizeWhite(byte[] in) {
        String s = new String(in, java.nio.charset.StandardCharsets.ISO_8859_1);
        String o = WHITE_FILL.matcher(s).replaceAll("$1n");
        return o.equals(s) ? null : o.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    /** Neutralise white background fills in a page's content stream and every nested form XObject. */
    private void stripWhiteFills(PdfPage page, PdfDocument doc) {
        try {
            byte[] nb = neutralizeWhite(page.getContentBytes());
            if (nb != null) {
                PdfStream ns = new PdfStream(nb);
                ns.makeIndirect(doc);
                page.getPdfObject().put(PdfName.Contents, ns);
                page.getPdfObject().setModified();
            }
            stripWhiteInForms(page.getResources().getPdfObject());
        } catch (Exception e) {
            log.warn("MOU white-fill strip failed on a page: {}", e.getMessage());
        }
    }

    private void stripWhiteInForms(PdfDictionary res) {
        if (res == null) return;
        PdfDictionary xobjects = res.getAsDictionary(PdfName.XObject);
        if (xobjects == null) return;
        for (PdfName key : xobjects.keySet()) {
            PdfStream st = xobjects.getAsStream(key);
            if (st == null || PdfName.Image.equals(st.getAsName(PdfName.Subtype))) continue;
            byte[] nb = neutralizeWhite(st.getBytes());
            if (nb != null) {
                st.setData(nb);
                st.setModified();
            }
            stripWhiteInForms(st.getAsDictionary(PdfName.Resources));   // recurse into nested forms
        }
    }

    /** Overwrite an image stream in place with a 1x1 transparent image, keeping its object reference. */
    private void blankImageStream(PdfStream st, byte[] transparentPng) {
        PdfStream t = new PdfImageXObject(ImageDataFactory.create(transparentPng)).getPdfObject();
        for (PdfName k : new java.util.ArrayList<>(st.keySet())) st.remove(k);
        for (PdfName k : t.keySet()) st.put(k, t.get(k));
        st.setData(t.getBytes(false), false);
    }

    private static final float LH_HEADER_H = 156f;
    private static final float LH_FOOTER_H = 66f;

    /**
     * Place each built page's content — scaled and centred — into the safe band between the YKTK
     * letterhead's peacock header and footer address, so the document sits cleanly inside the letter
     * on every page (never overlapping the header or footer).
     */
    private byte[] reframeIntoLetterhead(byte[] content) {
        ByteArrayOutputStream framed = new ByteArrayOutputStream();
        try (PdfDocument src = new PdfDocument(new PdfReader(new ByteArrayInputStream(content)));
             PdfDocument dst = new PdfDocument(new PdfWriter(framed))) {
            // Flat, opaque raster of the letterhead — draws as a plain background so its grey arc stays a
            // smooth curve and never composites over the content (the PDF letterhead's transparency did).
            // Built once and reused on every page so the file doesn't balloon.
            PdfImageXObject letterhead = new PdfImageXObject(ImageDataFactory.create(readAsset("cba-letterhead.png")));
            Rectangle ps = src.getPage(1).getPageSize();
            float w = ps.getWidth(), h = ps.getHeight();
            float s = (h - LH_HEADER_H - LH_FOOTER_H) / h;   // fit content between header and footer
            float tx = (w - w * s) / 2f, ty = LH_FOOTER_H;
            int n = src.getNumberOfPages();
            for (int i = 1; i <= n; i++) {
                PdfFormXObject pageXo = src.getPage(i).copyAsFormXObject(dst);
                PdfPage np = dst.addNewPage(new com.itextpdf.kernel.geom.PageSize(w, h));
                PdfCanvas cv = new PdfCanvas(np);
                cv.addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, w, h));   // opaque letterhead background (reused XObject)
                cv.addXObjectFittedIntoRectangle(pageXo, new Rectangle(tx, ty, w * s, h * s));   // document content on top
            }
        } catch (Exception e) {
            log.warn("MOU reframe failed: {}", e.getMessage());
            return content;   // fall back to the un-framed content rather than failing the download
        }
        return framed.toByteArray();
    }

    /**
     * Franchise Certificate as a standalone PDF, built from the template's certificate page (p27)
     * with the franchise's details, territory, validity, logo and signatures overlaid.
     */
    public byte[] buildCertificate(Zone zone) {
        byte[] template = readTemplate();
        if (template == null) throw new IllegalStateException("MOU template not bundled");

        byte[] giver = adminSignature.get();
        String name = firstNonBlank(zone.getFranchiseeName(), zone.getOrganizationName(), zone.getName());
        String addr = firstNonBlank(zone.getFullAddress(), zone.getCity(), zone.getDistrict());
        // Issue date = the zone's creation date; validity = two years from it.
        String issue = firstNonBlank(zone.getIssueDate(), zone.getRegistrationDate());
        String valid = firstNonBlank(zone.getValidTill(), FranchiseTerms.validTillFrom(issue));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument src = new PdfDocument(new PdfReader(new ByteArrayInputStream(template)));
             PdfDocument dst = new PdfDocument(new PdfWriter(out))) {

            // Copy the certificate page (p27) and draw our values into a fresh content stream layered
            // AFTER it — at the page level (not inside the template's transparency group), so the white
            // covers composite fully opaque on top instead of ghosting through.
            src.copyPagesTo(CERTIFICATE_PAGE, CERTIFICATE_PAGE, dst);
            PdfPage page = dst.getPage(1);
            float w = page.getPageSize().getWidth();
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), page.getDocument());
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Each template line carries a SAMPLE; cover it and draw the franchise's own value.
            // "M/s. Name" line  — sample "MAHA CHETHANA SEVA TRUST" at baseline (204, 436)
            coverBox(canvas, new Rectangle(196, 430, w - 196 - 20, 26));
            drawBaseline(canvas, bold, name, 206, 437, 16, w - 206 - 30);
            // "Address" line     — sample "CHAMARAJANAGARA" at baseline (169, 390)
            coverBox(canvas, new Rectangle(150, 382, w - 150 - 20, 38));
            drawBaseline(canvas, bold, addr, 169, 393, addr.length() > 42 ? 12 : 16, w - 169 - 30);
            // "Issue Date:" value — sample "26nd December 2024" at baseline (122, 273)
            coverBox(canvas, new Rectangle(120, 266, 235, 26));
            drawBaseline(canvas, bold, fmtDate(issue), 122, 273, 15, 230);
            // "Valid up to:" value — sample "26nd December 2025" at baseline (125, 225)
            coverBox(canvas, new Rectangle(122, 218, 235, 26));
            drawBaseline(canvas, bold, fmtDate(valid), 125, 225, 15, 230);
            // Drop the "Page 27 of 47" footer — this is now a standalone certificate.
            coverBox(canvas, new Rectangle(485, 30, 105, 20));

            // Signature, placed just above "State Nodal Operator (sno) YKTK-KP-MSYEP" (y≈106).
            // Prefer an explicit giver signature; otherwise use the YKTK signatory sign asset.
            byte[] sigBytes = giver;
            if (sigBytes == null) {
                try (InputStream in = new ClassPathResource("sign-yktk.png").getInputStream()) {
                    sigBytes = in.readAllBytes();
                } catch (Exception ignore) { }
            }
            if (sigBytes != null) {
                try {
                    PdfImageXObject sig = new PdfImageXObject(ImageDataFactory.create(sigBytes));
                    Rectangle fit = fitPreservingAspect(sig.getWidth(), sig.getHeight(), new Rectangle(365, 120, 175, 56));
                    canvas.saveState().setExtGState(opaqueState());
                    canvas.addXObjectFittedIntoRectangle(sig, fit);
                    canvas.restoreState();
                } catch (Exception ex) {
                    log.warn("certificate signature failed: {}", ex.getMessage());
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to build certificate: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private void footerStampSafe(PdfPage page, byte[] img, Rectangle box, String label) {
        try { footerStamp(page, img, box, label); } catch (Exception e) {
            log.warn("certificate stamp failed: {}", e.getMessage());
        }
    }

    /**
     * A fresh graphics state that undoes the template's leftover transparency: full opacity and no
     * soft mask. Required on BOTH fills and text, else overlays composite through a leftover mask.
     */
    private com.itextpdf.kernel.pdf.extgstate.PdfExtGState opaqueState() {
        return new com.itextpdf.kernel.pdf.extgstate.PdfExtGState()
                .setFillOpacity(1f).setStrokeOpacity(1f)
                .setSoftMask(com.itextpdf.kernel.pdf.PdfName.None);
    }

    /** Paint a fully-opaque white rectangle to hide template content underneath. */
    private void coverBox(PdfCanvas canvas, Rectangle r) {
        canvas.saveState().setExtGState(opaqueState()).setFillColor(ColorConstants.WHITE)
                .rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight()).fill().restoreState();
    }

    /** Draw a single line of black text at an exact baseline, shrinking to fit maxWidth. */
    private void drawBaseline(PdfCanvas canvas, PdfFont font, String text, float x, float y, float size, float maxWidth) {
        if (!StringUtils.hasText(text)) return;
        float fs = size;
        while (fs > 7 && font.getWidth(text, fs) > maxWidth) fs -= 0.5f;
        canvas.saveState().setExtGState(opaqueState()).setFillColor(ColorConstants.BLACK)
                .beginText().setFontAndSize(font, fs)
                .setTextRenderingMode(com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants.TextRenderingMode.FILL)
                .moveText(x, y).showText(text).endText()
                .restoreState();
    }

    // ---------------------------------------------------------------- overlays

    /** Draw a franchise-details block near the bottom of the cover page. */
    private void overlayFirstPageData(PdfDocument pdf, int pageNum, Zone z) {
        PdfPage page = pdf.getPage(pageNum);
        Rectangle box = new Rectangle(60, 40, page.getPageSize().getWidth() - 120, 90);
        try (Canvas c = new Canvas(new PdfCanvas(page), box)) {
            c.add(new Paragraph("Franchisee: " + firstNonBlank(z.getFranchiseeName(), z.getOrganizationName(), z.getName()))
                    .setFontSize(10).setBold().setMargin(0));
            String line2 = "Reg No: " + nz(z.getRegistrationNo()) + "   |   Territory: " + nz(z.getTerritory());
            c.add(new Paragraph(line2).setFontSize(9).setMargin(0));
            String line3 = "Issued: " + nz(z.getIssueDate()) + "   |   Valid till: " + nz(z.getValidTill())
                    + "   |   Membership: " + nz(z.getMembershipTier());
            c.add(new Paragraph(line3).setFontSize(9).setMargin(0));
        }
    }

    /** Cover the first occurrence of {@code from} and draw {@code to} in its place. */
    private void replaceText(PdfDocument pdf, int pageNum, String from, String to) {
        try {
            PdfPage page = pdf.getPage(pageNum);
            var strategy = new RegexBasedLocationExtractionStrategy(java.util.regex.Pattern.quote(from));
            new PdfCanvasProcessor(strategy).processPageContent(page);
            List<IPdfTextLocation> hits = new ArrayList<>();
            strategy.getResultantLocations().forEach(hits::add);
            if (hits.isEmpty()) { log.info("MOU: sample name not found on p{} (already personalised?)", pageNum); return; }
            Rectangle r = hits.get(0).getRectangle();
            PdfCanvas canvas = new PdfCanvas(page);
            // white-out the old text a touch wider than the glyph box
            canvas.saveState().setFillColor(ColorConstants.WHITE)
                    .rectangle(r.getX() - 2, r.getY() - 2, r.getWidth() + 4, r.getHeight() + 4).fill().restoreState();
            try (Canvas c = new Canvas(canvas, new Rectangle(r.getX() - 2, r.getY() - 2, r.getWidth() + 60, r.getHeight() + 4))) {
                c.add(new Paragraph(to).setBold().setFontSize(Math.max(8f, r.getHeight() * 0.8f)).setMargin(0));
            }
        } catch (Exception e) {
            log.warn("MOU: text replace failed on p{}: {}", pageNum, e.getMessage());
        }
    }

    /**
     * Rebuild a banner image with the franchise logo composited into it (in pixel space) over the
     * sample emblem, then draw the whole banner back over its page rectangle. Optionally erase the old
     * emblem with a white disc first (for banners where the franchise logo is smaller than the sample).
     *
     * @param lx,ly,lw,lh  logo box within the banner image (pixels)
     * @param dcx,dcy,dr   erase-disc centre + radius in pixels (dr<=0 → no disc)
     */
    private void recreateBanner(PdfDocument pdf, int pageNum, String bannerRes, Rectangle rect, byte[] logo,
                                int lx, int ly, int lw, int lh, int dcx, int dcy, int dr) {
        try (InputStream in = new ClassPathResource(bannerRes).getInputStream()) {
            BufferedImage banner = ImageIO.read(in);
            BufferedImage lg = ImageIO.read(new ByteArrayInputStream(logo));
            if (banner == null || lg == null) return;
            Graphics2D g = banner.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (dr > 0) {
                g.setColor(Color.WHITE);
                g.fillOval(dcx - dr, dcy - dr, 2 * dr, 2 * dr);
            }
            // fit the logo into the box, preserving aspect ratio
            double s = Math.min((double) lw / lg.getWidth(), (double) lh / lg.getHeight());
            int fw = (int) Math.round(lg.getWidth() * s), fh = (int) Math.round(lg.getHeight() * s);
            g.drawImage(lg, lx + (lw - fw) / 2, ly + (lh - fh) / 2, fw, fh, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(banner, "png", out);
            PdfImageXObject img = new PdfImageXObject(ImageDataFactory.create(out.toByteArray()));
            PdfCanvas canvas = new PdfCanvas(pdf.getPage(pageNum));
            canvas.saveState().setExtGState(opaqueState());
            canvas.addXObjectFittedIntoRectangle(img, rect);
            canvas.restoreState();
        } catch (Exception e) {
            log.warn("MOU: banner rebuild failed on p{}: {}", pageNum, e.getMessage());
        }
    }

    /**
     * Replace page 1's centre watermark emblem with the zone's own logo (faded to a watermark). The
     * emblem is a separate image drawn behind the cover text, so swapping the image resource keeps
     * the text on top and puts the franchise logo in exactly the same position/size.
     */
    private void replaceP1Watermark(PdfDocument pdf, byte[] logo) {
        try {
            byte[] faded = fadeToWatermark(logo, 222, 213);
            PdfImageXObject newImg = new PdfImageXObject(ImageDataFactory.create(faded));
            newImg.getPdfObject().makeIndirect(pdf);
            PdfDictionary xobjs = pdf.getPage(1).getResources().getPdfObject().getAsDictionary(PdfName.XObject);
            if (!swapFirstImage(xobjs, newImg.getPdfObject(), 0)) {
                log.info("MOU p1: watermark image not found to swap");
            }
        } catch (Exception e) {
            log.warn("MOU p1: watermark swap failed: {}", e.getMessage());
        }
    }

    /** Recurse into (nested) XObject dicts and replace the first image resource with {@code replacement}. */
    private boolean swapFirstImage(PdfDictionary xobjs, PdfStream replacement, int depth) {
        if (xobjs == null || depth > 4) return false;
        for (PdfName name : xobjs.keySet()) {
            PdfStream s = xobjs.getAsStream(name);
            if (s == null) continue;
            PdfName sub = s.getAsName(PdfName.Subtype);
            if (PdfName.Image.equals(sub)) {
                xobjs.put(name, replacement);
                return true;
            }
            if (PdfName.Form.equals(sub)) {
                PdfDictionary res = s.getAsDictionary(PdfName.Resources);
                PdfDictionary inner = res == null ? null : res.getAsDictionary(PdfName.XObject);
                if (swapFirstImage(inner, replacement, depth + 1)) return true;
            }
        }
        return false;
    }

    /** A 1×1 fully-transparent PNG — used as the "logo" when a zone has none, so spots end up blank. */
    private byte[] blankPixel() {
        try {
            BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ImageIO.write(img, "png", b);
            return b.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /** A faint, centred watermark version of the logo on a white canvas of the given size. */
    private byte[] fadeToWatermark(byte[] logo, int w, int h) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(logo));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f)); // faint so cover text stays readable
        double s = Math.min((double) w / src.getWidth(), (double) h / src.getHeight());
        int lw = (int) Math.round(src.getWidth() * s), lh = (int) Math.round(src.getHeight() * s);
        g.drawImage(src, (w - lw) / 2, (h - lh) / 2, lw, lh, null);
        g.dispose();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(out, "png", b);
        return b.toByteArray();
    }

    /** Cover the given template logo boxes and draw the franchise logo (aspect-preserved) into each. */
    private void overlayLogoAt(PdfDocument pdf, int pageNum, byte[] logo, Rectangle... boxes) {
        try {
            PdfPage page = pdf.getPage(pageNum);
            PdfImageXObject img = new PdfImageXObject(ImageDataFactory.create(logo));
            PdfCanvas canvas = new PdfCanvas(page);
            for (Rectangle box : boxes) {
                canvas.saveState().setExtGState(opaqueState()).setFillColor(ColorConstants.WHITE)
                        .rectangle(box.getX(), box.getY(), box.getWidth(), box.getHeight()).fill().restoreState();
                Rectangle fit = fitPreservingAspect(img.getWidth(), img.getHeight(), box);
                canvas.saveState().setExtGState(opaqueState());
                canvas.addXObjectFittedIntoRectangle(img, fit);
                canvas.restoreState();
            }
        } catch (Exception e) {
            log.warn("MOU: logo overlay failed on p{}: {}", pageNum, e.getMessage());
        }
    }

    /**
     * Fill the "Receiver / Giver" signature block (template p16): receiver = the zone head (uploaded
     * signature + zone name/phone/company); giver = YKTK (fixed signature + phone/company).
     */
    private void fillSignatureBlock(PdfDocument pdf, int pageNum, Zone z, byte[] receiverSign, byte[] giverSign) {
        if (pageNum < 1 || pageNum > pdf.getNumberOfPages()) return;
        PdfPage page = pdf.getPage(pageNum);
        // Signatures sit just above the "Receiver Signature/-" and "Giver Signature/-" labels.
        overlaySignatureAt(pdf, pageNum, receiverSign, new Rectangle(45, 118, 135, 36));
        overlaySignatureAt(pdf, pageNum, giverSign, new Rectangle(455, 118, 135, 36));
        // Receiver = the zone head.
        drawText(page, firstNonBlank(z.getFranchiseeName(), z.getOwnerName(), z.getName()), 90, 92, 9);
        drawText(page, firstNonBlank(z.getContactPhone(), z.getContactNumber()), 135, 82, 9);
        drawText(page, firstNonBlank(z.getName(), z.getOrganizationName()), 138, 72, 9);
        // Giver = YKTK (phone + company fixed; name intentionally left blank).
        drawText(page, "6366273089", 533, 82, 9);
        drawText(page, "YKTK", 536, 72, 9);
    }

    /** Draw a short text string at an absolute baseline (Helvetica), opaque over the template. */
    private void drawText(PdfPage page, String text, float x, float y, float size) {
        if (!StringUtils.hasText(text)) return;
        try {
            PdfCanvas cv = new PdfCanvas(page);
            cv.saveState().setExtGState(opaqueState()).beginText()
                    .setFontAndSize(PdfFontFactory.createFont(), size)
                    .moveText(x, y).showText(text).endText().restoreState();
        } catch (Exception e) {
            log.warn("MOU: text draw failed: {}", e.getMessage());
        }
    }

    /** Read a bundled classpath asset (e.g. the YKTK giver signature) as bytes, or null. */
    private byte[] readAsset(String name) {
        try (InputStream in = new ClassPathResource(name).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    /** Draw the (aspect-preserved) authorised-signatory signature into a box, if one was uploaded. */
    private void overlaySignatureAt(PdfDocument pdf, int pageNum, byte[] sig, Rectangle box) {
        if (sig == null || pageNum < 1 || pageNum > pdf.getNumberOfPages()) return;
        try {
            PdfPage page = pdf.getPage(pageNum);
            PdfImageXObject img = new PdfImageXObject(ImageDataFactory.create(sig));
            Rectangle fit = fitPreservingAspect(img.getWidth(), img.getHeight(), box);
            PdfCanvas canvas = new PdfCanvas(page);
            canvas.saveState().setExtGState(opaqueState());
            canvas.addXObjectFittedIntoRectangle(img, fit);
            canvas.restoreState();
        } catch (Exception e) {
            log.warn("MOU: signature overlay failed on p{}: {}", pageNum, e.getMessage());
        }
    }

    /** Small franchisee + giver signature stamps in the bottom corners of every page. */
    private void stampSignaturesAllPages(PdfDocument pdf, byte[] sign, byte[] giver) {
        int n = pdf.getNumberOfPages();
        for (int i = 1; i <= n; i++) {
            PdfPage page = pdf.getPage(i);
            float w = page.getPageSize().getWidth();
            try {
                // Bottom corners, small, sitting below the page-number band (~y35) so they don't collide.
                if (giver != null) footerStamp(page, giver, new Rectangle(34, 12, 66, 18), "Giver");
                if (sign != null) footerStamp(page, sign, new Rectangle(w - 100, 12, 66, 18), "Franchisee");
            } catch (Exception e) {
                log.warn("MOU: footer stamp failed on p{}: {}", i, e.getMessage());
            }
        }
    }

    private void footerStamp(PdfPage page, byte[] imgBytes, Rectangle box, String label) throws Exception {
        PdfImageXObject img = new PdfImageXObject(ImageDataFactory.create(imgBytes));
        PdfCanvas canvas = new PdfCanvas(page);
        Rectangle fit = fitPreservingAspect(img.getWidth(), img.getHeight(), box);
        canvas.saveState().setExtGState(opaqueState());
        canvas.addXObjectFittedIntoRectangle(img, fit);
        canvas.restoreState();
        if (StringUtils.hasText(label)) {
            try (Canvas c = new Canvas(canvas, new Rectangle(box.getX(), box.getY() - 9, box.getWidth() + 40, 9))) {
                c.add(new Paragraph(label).setFontSize(5).setFontColor(new DeviceRgb(120, 120, 120)).setMargin(0));
            }
        }
    }

    /** Format an ISO date (yyyy-MM-dd) as e.g. "24 July 2026"; falls back to the raw value. */
    private static String fmtDate(String iso) {
        if (!StringUtils.hasText(iso)) return "—";
        try {
            return java.time.LocalDate.parse(iso.trim())
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH));
        } catch (Exception e) {
            return iso;
        }
    }

    // ---------------------------------------------------------------- helpers

    private Rectangle fitPreservingAspect(float iw, float ih, Rectangle box) {
        if (iw <= 0 || ih <= 0) return box;
        float scale = Math.min(box.getWidth() / iw, box.getHeight() / ih);
        float w = iw * scale, h = ih * scale;
        float x = box.getX() + (box.getWidth() - w) / 2;
        float y = box.getY() + (box.getHeight() - h) / 2;
        return new Rectangle(x, y, w, h);
    }

    private byte[] readTemplate() {
        try (InputStream in = new ClassPathResource(TEMPLATE).getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            log.warn("MOU template missing: {}", e.getMessage());
            return null;
        }
    }

    /** Read a zone-uploaded document's bytes by type (logo / franchiseeSignature), or null. */
    private byte[] readZoneDoc(Zone zone, String type) {
        if (zone.getDocuments() == null) return null;
        return zone.getDocuments().stream()
                .filter(d -> type.equals(d.getType()) && StringUtils.hasText(d.getPath()))
                .findFirst()
                .map(d -> readFile(Paths.get(uploadsDir).resolve(d.getPath())))
                .orElse(null);
    }

    private byte[] readFile(Path p) {
        try {
            return Files.exists(p) ? Files.readAllBytes(p) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static int[] sortedDesc(int[] a) {
        Integer[] boxed = new Integer[a.length];
        for (int i = 0; i < a.length; i++) boxed[i] = a[i];
        java.util.Arrays.sort(boxed, Collections.reverseOrder());
        int[] out = new int[a.length];
        for (int i = 0; i < a.length; i++) out[i] = boxed[i];
        return out;
    }

    private static String firstNonBlank(String... vs) {
        for (String v : vs) if (StringUtils.hasText(v)) return v;
        return "";
    }

    private static String nz(String s) {
        return StringUtils.hasText(s) ? s : "—";
    }
}
