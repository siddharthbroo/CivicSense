package com.civicsense.complaint.service.ai;

import com.civicsense.complaint.entity.analysis.DescriptionImageConsistency;
import com.civicsense.complaint.entity.analysis.EvidenceLevel;
import com.civicsense.complaint.entity.image.ComplaintImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvidenceLevelServiceTest {

    private EvidenceLevelService service;

    @BeforeEach
    void setUp() {
        service = new EvidenceLevelService();
    }

    // =========================================================================
    // E0 — no photo
    // =========================================================================

    @Test
    @DisplayName("E0: null image → text-only complaint")
    void e0_nullImage() {
        assertEquals(EvidenceLevel.E0, service.compute(null, null));
    }

    @Test
    @DisplayName("E0: image with blank URL → treated as no photo")
    void e0_blankImageUrl() {
        ComplaintImage img = makeImage("   ");
        assertEquals(EvidenceLevel.E0, service.compute(img, DescriptionImageConsistency.CONSISTENT));
    }

    @Test
    @DisplayName("E0: image present but INCONSISTENT description → not promoted to E1")
    void e0_imageWithInconsistentDescription() {
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        assertEquals(EvidenceLevel.E0, service.compute(img, DescriptionImageConsistency.INCONSISTENT));
    }

    @Test
    @DisplayName("E0: text-only complaint must not be flagged as suspicious by evidence level alone")
    void e0_textOnlyIsNotSuspicious() {
        // E0 is purely an evidence signal — not a fraud/suspicious flag.
        // A future verification gate may use it, but it does not auto-reject.
        EvidenceLevel level = service.compute(null, null);
        assertEquals(EvidenceLevel.E0, level,
                "Text-only complaint must always be E0, never treated as suspicious");
    }

    // =========================================================================
    // E1 — photo present, consistent enough, no EXIF corroboration
    // =========================================================================

    @Test
    @DisplayName("E1: photo + CONSISTENT but no EXIF at all")
    void e1_consistentNoExif() {
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        img.setHasExifGps(false);
        img.setExifTimestamp(null);
        assertEquals(EvidenceLevel.E1, service.compute(img, DescriptionImageConsistency.CONSISTENT));
    }

    @Test
    @DisplayName("E1: photo + UNCERTAIN consistency, no EXIF → not inconsistent, so E1")
    void e1_uncertainConsistency() {
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        img.setHasExifGps(false);
        img.setExifTimestamp(null);
        assertEquals(EvidenceLevel.E1, service.compute(img, DescriptionImageConsistency.UNCERTAIN));
    }

    @Test
    @DisplayName("E1: photo + null consistency (Gemini omitted it) → E1")
    void e1_nullConsistency() {
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        img.setHasExifGps(false);
        img.setExifTimestamp(null);
        assertEquals(EvidenceLevel.E1, service.compute(img, null));
    }

    @Test
    @DisplayName("E1: EXIF GPS present but flagged as mismatch → corroboration absent → E1")
    void e1_exifMismatchFlagged() {
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        img.setHasExifGps(true);
        img.setExifLocationMismatch(true); // >500 m from confirmed pin
        img.setExifTimestamp(null);
        assertEquals(EvidenceLevel.E1, service.compute(img, DescriptionImageConsistency.CONSISTENT));
    }

    // =========================================================================
    // E2 — photo + CONSISTENT + meaningful location/time corroboration
    // =========================================================================

    @Test
    @DisplayName("E2: CONSISTENT + EXIF GPS within 500 m (no mismatch flag)")
    void e2_consistentWithExifGpsNoMismatch() {
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        img.setHasExifGps(true);
        img.setExifLocationMismatch(false);
        img.setExifTimestamp(null);
        assertEquals(EvidenceLevel.E2, service.compute(img, DescriptionImageConsistency.CONSISTENT));
    }

    @Test
    @DisplayName("E2: CONSISTENT + EXIF timestamp present (even without GPS)")
    void e2_consistentWithExifTimestamp() {
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        img.setHasExifGps(false);
        img.setExifTimestamp(LocalDateTime.now().minusMinutes(30));
        assertEquals(EvidenceLevel.E2, service.compute(img, DescriptionImageConsistency.CONSISTENT));
    }

    @Test
    @DisplayName("E2: CONSISTENT + both EXIF GPS (no mismatch) AND timestamp → E2")
    void e2_consistentWithBothExif() {
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        img.setHasExifGps(true);
        img.setExifLocationMismatch(false);
        img.setExifTimestamp(LocalDateTime.now().minusMinutes(5));
        assertEquals(EvidenceLevel.E2, service.compute(img, DescriptionImageConsistency.CONSISTENT));
    }

    @Test
    @DisplayName("E1 (not E2): UNCERTAIN consistency + EXIF timestamp → E1 because not CONSISTENT")
    void e1_uncertainWithExifTimestamp() {
        // E2 requires CONSISTENT; UNCERTAIN + corroboration stays E1
        ComplaintImage img = makeImage("https://cdn/photo.jpg");
        img.setHasExifGps(false);
        img.setExifTimestamp(LocalDateTime.now().minusHours(1));
        assertEquals(EvidenceLevel.E1, service.compute(img, DescriptionImageConsistency.UNCERTAIN));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private ComplaintImage makeImage(String url) {
        ComplaintImage img = new ComplaintImage();
        img.setImageUrl(url);
        img.setContentType("image/jpeg");
        img.setHasExifGps(false);
        img.setExifLocationMismatch(false);
        img.setExifTimestamp(null);
        return img;
    }
}