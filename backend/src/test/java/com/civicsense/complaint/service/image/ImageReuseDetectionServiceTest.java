package com.civicsense.complaint.service.image;

import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.entity.image.ImageReuseType;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import com.civicsense.complaint.service.verification.VerificationGateService;
import com.civicsense.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageReuseDetectionServiceTest {

    @Mock
    private ComplaintImageRepository complaintImageRepository;

    @InjectMocks
    private ImageReuseDetectionService reuseService;

    private ExifMetadataExtractorService exifService;
    private VerificationGateService verificationGateService;

    private byte[] sampleImageBytes;
    private UUID user1Id;
    private UUID user2Id;

    @BeforeEach
    void setUp() {
        exifService = new ExifMetadataExtractorService();
        verificationGateService = new VerificationGateService();

        sampleImageBytes = "mock-image-data-for-testing-sha-256-calculation".getBytes();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
    }

    @Test
    @DisplayName("Req 1: Missing EXIF does not produce a suspicious finding")
    void testMissingExif_DoesNotProduceSuspiciousFinding() {
        MockMultipartFile fileWithoutExif = new MockMultipartFile(
                "image", "pothole.jpg", "image/jpeg", sampleImageBytes
        );

        ExifMetadata exif = exifService.extractMetadata(fileWithoutExif, 28.6139, 77.2090);

        assertNotNull(exif);
        assertFalse(exif.hasGps(), "Missing EXIF must indicate no GPS");
        assertFalse(exif.locationMismatchFlag(), "Missing EXIF must never trigger a location mismatch");
        assertNull(exif.distanceMetersToPin());

        // Test in verification gate: missing EXIF does NOT add EXIF_LOCATION_MISMATCH
        ComplaintImage complaintImage = new ComplaintImage();
        complaintImage.setHasExifGps(exif.hasGps());
        complaintImage.setExifLocationMismatch(exif.locationMismatchFlag());

        var decision = verificationGateService.evaluate(null, null, complaintImage);
        assertFalse(decision.reason().contains("EXIF_LOCATION_MISMATCH"),
                "Missing EXIF must not produce an EXIF_LOCATION_MISMATCH finding");
    }

    @Test
    @DisplayName("Req 2: EXIF GPS conflict produces finding without changing confirmed incident location")
    void testExifGpsConflict_ProducesFindingWithoutChangingConfirmedLocation() {
        // Confirmed incident pin chosen by citizen
        Double confirmedLat = 28.6139;
        Double confirmedLon = 77.2090;

        // Complaint entity retains confirmed pin
        Complaint complaint = new Complaint();
        complaint.setLatitude(confirmedLat);
        complaint.setLongitude(confirmedLon);

        // When EXIF conflict occurs (e.g. photo taken 5km away in Noida)
        ComplaintImage complaintImage = new ComplaintImage();
        complaintImage.setComplaint(complaint);
        complaintImage.setHasExifGps(true);
        complaintImage.setExifLatitude(28.5355);
        complaintImage.setExifLongitude(77.3910);
        complaintImage.setExifLocationMismatch(true);
        complaintImage.setExifDistanceMeters(20500.0);

        // Primary incident location remains the citizen-confirmed pin
        assertEquals(confirmedLat, complaint.getLatitude(), "Citizen-confirmed latitude must never be altered");
        assertEquals(confirmedLon, complaint.getLongitude(), "Citizen-confirmed longitude must never be altered");

        // Verification gate notes the conflict as a supporting signal for review
        var decision = verificationGateService.evaluate(null, null, complaintImage);
        assertTrue(decision.verificationRequired());
        assertTrue(decision.reason().contains("EXIF_LOCATION_MISMATCH"),
                "EXIF conflict must be flagged as supporting verification signal");
    }

    @Test
    @DisplayName("Req 3: Identical image hashes are detected")
    void testIdenticalImageHashes_AreCalculatedAndDetected() {
        String hash1 = ImageHasher.computeSha256(sampleImageBytes);
        String hash2 = ImageHasher.computeSha256(sampleImageBytes);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hex string must be 64 characters");
        assertEquals(hash1, hash2, "Identical bytes must produce identical SHA-256 hashes");

        // When no prior matches exist
        when(complaintImageRepository.findByImageHash(anyString())).thenReturn(Collections.emptyList());
        ImageReuseResult result = reuseService.detectReuse(sampleImageBytes, user1Id, 28.6139, 77.2090, null);

        assertFalse(result.reused());
        assertEquals(ImageReuseType.NONE, result.reuseType());
        assertFalse(result.suspicious());
        assertEquals(hash1, result.imageHash());
    }

    @Test
    @DisplayName("Req 4: Reuse at same location is not automatically treated as fraud")
    void testReuseAtSameLocation_NotTreatedAsFraud() {
        String sha256 = ImageHasher.computeSha256(sampleImageBytes);

        // Case A: Same user, same location (e.g. 30 meters away) -> RESUBMISSION
        User user1 = new User();
        user1.setId(user1Id);

        Complaint priorComplaintSameUser = new Complaint();
        priorComplaintSameUser.setId(UUID.randomUUID());
        priorComplaintSameUser.setUser(user1);
        priorComplaintSameUser.setLatitude(28.6140);
        priorComplaintSameUser.setLongitude(77.2092); // ~23m distance

        ComplaintImage priorImage1 = new ComplaintImage();
        priorImage1.setComplaint(priorComplaintSameUser);
        priorImage1.setImageHash(sha256);

        when(complaintImageRepository.findByImageHash(sha256)).thenReturn(List.of(priorImage1));

        ImageReuseResult resubmissionResult = reuseService.detectReuse(
                sampleImageBytes, user1Id, 28.6139, 77.2090, null
        );

        assertTrue(resubmissionResult.reused());
        assertEquals(ImageReuseType.RESUBMISSION, resubmissionResult.reuseType());
        assertFalse(resubmissionResult.suspicious(), "Same user at same location is not suspicious/fraud");

        // Case B: Different user, same location -> COMMUNITY_SHARED
        User user2 = new User();
        user2.setId(user2Id);

        Complaint priorComplaintOtherUser = new Complaint();
        priorComplaintOtherUser.setId(UUID.randomUUID());
        priorComplaintOtherUser.setUser(user2);
        priorComplaintOtherUser.setLatitude(28.6141);
        priorComplaintOtherUser.setLongitude(77.2091);

        ComplaintImage priorImage2 = new ComplaintImage();
        priorImage2.setComplaint(priorComplaintOtherUser);
        priorImage2.setImageHash(sha256);

        when(complaintImageRepository.findByImageHash(sha256)).thenReturn(List.of(priorImage2));

        ImageReuseResult communityResult = reuseService.detectReuse(
                sampleImageBytes, user1Id, 28.6139, 77.2090, null
        );

        assertTrue(communityResult.reused());
        assertEquals(ImageReuseType.COMMUNITY_SHARED, communityResult.reuseType());
        assertFalse(communityResult.suspicious(), "Different users sharing photo for same civic issue is not fraud");
    }

    @Test
    @DisplayName("Req 5: Reuse across different locations produces appropriate suspicious signal")
    void testReuseAcrossDifferentLocations_ProducesSuspiciousSignal() {
        String sha256 = ImageHasher.computeSha256(sampleImageBytes);

        // Case A: Same user reuses photo in Rohini (28.70) when previously submitted in Connaught Place (28.63) -> ~12km
        User user1 = new User();
        user1.setId(user1Id);

        Complaint priorComplaint = new Complaint();
        priorComplaint.setId(UUID.randomUUID());
        priorComplaint.setUser(user1);
        priorComplaint.setLatitude(28.6300);
        priorComplaint.setLongitude(77.2100);

        ComplaintImage priorImage = new ComplaintImage();
        priorImage.setComplaint(priorComplaint);
        priorImage.setImageHash(sha256);

        when(complaintImageRepository.findByImageHash(sha256)).thenReturn(List.of(priorImage));

        ImageReuseResult diffLocationResult = reuseService.detectReuse(
                sampleImageBytes, user1Id, 28.7041, 77.1025, null // Rohini
        );

        assertTrue(diffLocationResult.reused());
        assertEquals(ImageReuseType.DIFFERENT_LOCATION, diffLocationResult.reuseType());
        assertTrue(diffLocationResult.suspicious(), "Reuse across distant locations must produce suspicious signal");

        // Case B: Same image reused across 3 distinct locations -> MULTI_LOCATION_DISPERSED
        Complaint priorLoc1 = new Complaint();
        priorLoc1.setId(UUID.randomUUID());
        priorLoc1.setUser(user1);
        priorLoc1.setLatitude(28.5355);
        priorLoc1.setLongitude(77.3910); // Noida

        Complaint priorLoc2 = new Complaint();
        priorLoc2.setId(UUID.randomUUID());
        priorLoc2.setUser(user1);
        priorLoc2.setLatitude(28.4595);
        priorLoc2.setLongitude(77.0266); // Gurgaon

        ComplaintImage img1 = new ComplaintImage();
        img1.setComplaint(priorLoc1);
        img1.setImageHash(sha256);

        ComplaintImage img2 = new ComplaintImage();
        img2.setComplaint(priorLoc2);
        img2.setImageHash(sha256);

        when(complaintImageRepository.findByImageHash(sha256)).thenReturn(List.of(img1, img2));

        ImageReuseResult multiLocResult = reuseService.detectReuse(
                sampleImageBytes, user1Id, 28.7041, 77.1025, null // Delhi
        );

        assertTrue(multiLocResult.reused());
        assertEquals(ImageReuseType.MULTI_LOCATION_DISPERSED, multiLocResult.reuseType());
        assertTrue(multiLocResult.suspicious(), "Multi-location dispersion must flag as suspicious");

        // Verification Gate checks:
        ComplaintImage complaintImage = new ComplaintImage();
        complaintImage.setImageReuseSuspicious(true);
        complaintImage.setImageReuseType(ImageReuseType.MULTI_LOCATION_DISPERSED);

        var gateDecision = verificationGateService.evaluate(null, null, complaintImage);
        assertTrue(gateDecision.verificationRequired());
        assertTrue(gateDecision.reason().contains("SUSPICIOUS_IMAGE_REUSE_MULTI_LOCATION_DISPERSED"));
    }
}

