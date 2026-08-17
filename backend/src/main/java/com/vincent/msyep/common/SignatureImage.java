package com.vincent.msyep.common;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Turns ANY uploaded signature into clean, transparent-background PNG bytes so it never paints a paper
 * box over the document — used by every PDF that stamps a signature (MOU, franchise certificate, center
 * batch approval, GP requisition/invoice). It:
 * <ol>
 *   <li>pulls the embedded scan out if the signature was uploaded as a PDF ({@code ImageDataFactory}
 *       cannot read a PDF), then</li>
 *   <li>makes the paper transparent by luminance (ink sits well below the paper on any scan/photo) and
 *       trims to the ink bounding box, then</li>
 *   <li>downscales so it is not re-embedded at full photo resolution on every page.</li>
 * </ol>
 */
public final class SignatureImage {

    private static final Logger log = LoggerFactory.getLogger(SignatureImage.class);

    /** Pixels at/above this luminance are treated as paper and made transparent; ink sits below it. */
    private static final int PAPER_LUM = 100;
    /** Max width of the cleaned stamp — plenty for the small signature boxes, keeps the file small. */
    private static final int MAX_WIDTH = 500;

    private SignatureImage() { }

    /** Clean an uploaded signature to a transparent-background PNG. Returns null if there is nothing usable. */
    public static byte[] clean(byte[] bytes) {
        if (bytes == null || bytes.length < 5) return bytes;
        boolean isPdf = bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
        byte[] imageBytes = isPdf ? extractLargestImage(bytes) : bytes;
        if (imageBytes == null) return null;
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (src == null) return imageBytes;   // not a raster we can clean — use as-is
            BufferedImage cleaned = downscale(cleanTrim(src, PAPER_LUM), MAX_WIDTH);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(cleaned, "png", bo);
            return bo.toByteArray();
        } catch (Exception e) {
            log.warn("signature clean failed: {}", e.getMessage());
            return imageBytes;
        }
    }

    /** Pull the largest embedded raster image out of a (signature) PDF as PNG bytes, or null. */
    private static byte[] extractLargestImage(byte[] pdfBytes) {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
            byte[][] best = { null };
            long[] bestArea = { -1 };
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                collectImages(doc.getPage(i).getResources().getPdfObject(), best, bestArea);
            }
            return best[0];
        } catch (Exception e) {
            log.warn("signature PDF image extract failed: {}", e.getMessage());
            return null;
        }
    }

    private static void collectImages(PdfDictionary res, byte[][] best, long[] bestArea) {
        if (res == null) return;
        PdfDictionary xo = res.getAsDictionary(PdfName.XObject);
        if (xo == null) return;
        for (PdfName k : xo.keySet()) {
            PdfStream st = xo.getAsStream(k);
            if (st == null) continue;
            if (PdfName.Image.equals(st.getAsName(PdfName.Subtype))) {
                try {
                    PdfImageXObject img = new PdfImageXObject(st);
                    long area = (long) img.getWidth() * (long) img.getHeight();
                    if (area > bestArea[0]) { bestArea[0] = area; best[0] = img.getImageBytes(); }
                } catch (Exception ignore) { /* skip unreadable image */ }
            } else {
                collectImages(st.getAsDictionary(PdfName.Resources), best, bestArea);
            }
        }
    }

    /** Make paper (light) pixels transparent by luminance and crop to the ink bounding box. */
    private static BufferedImage cleanTrim(BufferedImage in, int lumThresh) {
        int w = in.getWidth(), h = in.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int argb = in.getRGB(x, y);
            int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
            int lum = (r * 299 + g * 587 + b * 114) / 1000;
            if (a < 24 || lum >= lumThresh) {
                out.setRGB(x, y, 0x00000000);   // paper / transparent
            } else {
                out.setRGB(x, y, 0xFF000000 | (argb & 0xFFFFFF));   // keep ink (force opaque)
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

    /** Downscale to a max width (signature stamps are small and get re-embedded on every page). */
    private static BufferedImage downscale(BufferedImage in, int maxW) {
        if (in.getWidth() <= maxW) return in;
        int w = maxW, h = Math.max(1, in.getHeight() * maxW / in.getWidth());
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(in, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
