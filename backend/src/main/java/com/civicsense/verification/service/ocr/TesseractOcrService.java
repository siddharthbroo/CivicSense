package com.civicsense.verification.service.ocr;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class TesseractOcrService implements OcrService {

    private final Tesseract tesseract;
    private final AadhaarOcrParser aadhaarOcrParser;

    public TesseractOcrService() {

        tesseract = new Tesseract();

        tesseract.setDatapath(
                "C:\\Program Files\\Tesseract-OCR\\tessdata");

        tesseract.setLanguage("eng");
        // PSM 6 = assume a single uniform block of text (good for ID cards).
        tesseract.setPageSegMode(6);

        aadhaarOcrParser = new AadhaarOcrParser();
    }

    @Override
    public OcrResult extractDetails(MultipartFile document) {

        try {

            BufferedImage image = ImageIO.read(document.getInputStream());

            if (image == null) {
                throw new IllegalArgumentException(
                        "Unsupported or invalid image file");
            }

            System.out.println(
                    "Image size: " + image.getWidth() + "x" + image.getHeight());

            BufferedImage processedImage = preprocessImage(image);

            // Extract raw text using Tesseract
            String text = tesseract.doOCR(processedImage);

            OcrResult result = aadhaarOcrParser.parse(text);

            System.out.println("========== PARSED DATA ==========");
            System.out.println("Name:   " + result.name());
            System.out.println("DOB:    " + result.dateOfBirth());
            System.out.println("Gender: " + result.gender());
            System.out.println("=================================");

            return result;

        } catch (Exception exception) {
            throw new RuntimeException("OCR processing failed", exception);
        }
    }

    /**
     * Converts the image to grayscale, applies tiered upscaling based on image
     * width, and boosts contrast so Tesseract can reliably read all fields —
     * especially short words like "MALE" that disappear on low-resolution scans.
     *
     * <p>
     * Scale tiers (based on original width):
     * <ul>
     * <li>&lt; 500 px → 4× (e.g. 335 px → 1340 px)</li>
     * <li>&lt; 1000 px → 3×</li>
     * <li>&lt; 1800 px → 2×</li>
     * <li>≥ 1800 px → no upscaling</li>
     * </ul>
     */
    private BufferedImage preprocessImage(BufferedImage original) {

        // --- Step 1: determine scale factor based on image width ---
        int scaleFactor;
        int w = original.getWidth();
        if (w < 500) {
            scaleFactor = 4; // very small card photos (e.g. 335 px)
        } else if (w < 1000) {
            scaleFactor = 3;
        } else if (w < 1800) {
            scaleFactor = 2;
        } else {
            scaleFactor = 1; // already large enough
        }

        int width = original.getWidth() * scaleFactor;
        int height = original.getHeight() * scaleFactor;

        // --- Step 2: upscale to grayscale with bicubic interpolation ---
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();

        // --- Step 3: convert to grayscale ---
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D gGray = gray.createGraphics();
        gGray.drawImage(scaled, 0, 0, null);
        gGray.dispose();

        // --- Step 4: boost contrast ---
        // RescaleOp(factor, offset) → newPixel = pixel * factor + offset
        // factor > 1.0 increases contrast; negative offset darkens shadows.
        // This makes light text on light backgrounds crisper for Tesseract.
        java.awt.image.RescaleOp contrastOp = new java.awt.image.RescaleOp(1.5f, -30f, null);
        BufferedImage enhanced = contrastOp.filter(gray, null);

        System.out.println("Preprocessed image: " + width + "x" + height
                + " (scale " + scaleFactor + "x, contrast boosted)");

        return enhanced;
    }
}