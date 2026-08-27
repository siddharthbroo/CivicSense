package com.civicsense.verification.service.ocr;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AadhaarOcrParser {

    private static final Pattern DOB_PATTERN =
            Pattern.compile("\\b\\d{2}[/-]\\d{2}[/-]\\d{4}\\b");

    /**
     * Matches MALE or FEMALE (case-insensitive) even when surrounded by
     * OCR noise characters like '/', '|', '(', ')', ':', spaces.
     * Deliberately excludes single M/F to avoid false positives in names.
     */
    private static final Pattern GENDER_PATTERN =
            Pattern.compile(
                    "(?<![A-Za-z])(MALE|FEMALE)(?![A-Za-z])",
                    Pattern.CASE_INSENSITIVE
            );

    /**
     * Known Tesseract OCR typos for MALE and FEMALE on Aadhaar cards.
     * Add more entries as new misreads are discovered in logs.
     */
    private static final String[] MALE_TYPOS   = { "HALE", "NALE", "MNALE", "MAE", "MLE" };
    private static final String[] FEMALE_TYPOS = { "FEMRLE", "FIMALE", "FENALE", "FEIALE", "FEMLE" };

    public OcrResult parse(String text) {

        // Log raw OCR output for debugging (visible in Spring Boot console)
        System.out.println("========== RAW OCR TEXT ==========");
        System.out.println(text);
        System.out.println("==================================");

        String name = extractName(text);
        LocalDate dateOfBirth = extractDateOfBirth(text);
        String gender = extractGender(text);

        return new OcrResult(name, dateOfBirth, gender);
    }

    private String extractName(String text) {

        String[] lines = text.split("\\R");

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i].trim();

            if (line.toLowerCase().contains("name")) {

                for (int j = i + 1; j < lines.length; j++) {

                    String candidate = lines[j].trim();

                    if (candidate.isEmpty()) {
                        continue;
                    }

                    // Stop if we have reached another known field.
                    if (candidate.toLowerCase().contains("dob")
                            || candidate.matches(".*\\d{2}[/-]\\d{2}[/-]\\d{4}.*")
                            || candidate.equalsIgnoreCase("male")
                            || candidate.equalsIgnoreCase("female")) {
                        break;
                    }

                    // Remove a common OCR artifact:
                    // "J Ravi Rastogi" -> "Ravi Rastogi"
                    if (candidate.matches("^[A-Za-z]\\s+.+")) {
                        candidate = candidate.substring(2).trim();
                    }

                    // Remove leading non-alphabetic OCR junk:
                    // "= Ravi Rastogi" -> "Ravi Rastogi"
                    // "| Ravi Rastogi" -> "Ravi Rastogi"
                    candidate = candidate.replaceAll("^[^A-Za-z]+", "").trim();

                    if (candidate.isEmpty()) {
                        continue;
                    }

                    return candidate;
                }
            }
        }

        return null;
    }

    private LocalDate extractDateOfBirth(String text) {

        Matcher matcher = DOB_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        String date = matcher.group().replace('-', '/');

        try {
            return LocalDate.parse(
                    date,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    /**
     * Extracts gender using three strategies in priority order:
     * <ol>
     *   <li>Line-by-line scan — finds a line whose sole meaningful token is MALE or FEMALE.</li>
     *   <li>Regex scan — finds MALE/FEMALE anywhere in the full text (handles same-line DOB+gender).</li>
     *   <li>Fuzzy fallback — corrects known Tesseract misreads of MALE/FEMALE.</li>
     * </ol>
     */
    private String extractGender(String text) {

        // --- Strategy 1: line-by-line scan ---
        // Aadhaar cards usually print MALE or FEMALE on its own line (or with DOB on same line).
        String[] lines = text.split("\\R");
        for (String line : lines) {
            // Strip all non-alpha characters and check if the remaining word is MALE/FEMALE.
            String stripped = line.replaceAll("[^A-Za-z]", " ").trim();
            for (String token : stripped.split("\\s+")) {
                if (token.equalsIgnoreCase("MALE")) {
                    return "MALE";
                }
                if (token.equalsIgnoreCase("FEMALE")) {
                    return "FEMALE";
                }
            }
        }

        // --- Strategy 2: regex scan across entire text ---
        Matcher matcher = GENDER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }

        // --- Strategy 3: fuzzy correction for known OCR typos ---
        String upperText = text.toUpperCase();
        for (String typo : MALE_TYPOS) {
            if (upperText.contains(typo)) {
                System.out.println("[AadhaarOcrParser] Fuzzy-corrected '" + typo + "' -> MALE");
                return "MALE";
            }
        }
        for (String typo : FEMALE_TYPOS) {
            if (upperText.contains(typo)) {
                System.out.println("[AadhaarOcrParser] Fuzzy-corrected '" + typo + "' -> FEMALE");
                return "FEMALE";
            }
        }

        System.out.println("[AadhaarOcrParser] Gender not found in OCR text.");
        return null;
    }
}