package com.civicsense.complaint.service;

import com.civicsense.complaint.dto.CreateComplaintRequest;
import com.civicsense.complaint.dto.CreateComplaintResponse;
import com.civicsense.complaint.entity.Complaint;
import com.civicsense.complaint.entity.ComplaintStatus;
import com.civicsense.complaint.entity.analysis.ComplaintAnalysis;
import com.civicsense.complaint.entity.analysis.ComplaintSeverity;
import com.civicsense.complaint.entity.classification.ComplaintCategory;
import com.civicsense.complaint.entity.image.ComplaintImage;
import com.civicsense.complaint.repository.ComplaintAnalysisRepository;
import com.civicsense.complaint.repository.ComplaintImageRepository;
import com.civicsense.complaint.repository.ComplaintRepository;
import com.civicsense.complaint.service.ai.AiAnalysisResult;
import com.civicsense.complaint.service.ai.AiAnalysisService;
import com.civicsense.complaint.service.ai.GeminiAiService;
import com.civicsense.complaint.service.complaintCluster.ComplaintClusterService;
import com.civicsense.complaint.service.complaintCluster.ComplaintDuplicateDetectionService;
import com.civicsense.complaint.service.complaintCluster.ComplaintDuplicateScoringService;
import com.civicsense.complaint.service.image.ExifMetadata;
import com.civicsense.complaint.service.image.ExifMetadataExtractorService;
import com.civicsense.complaint.service.image.ImageKitService;
import com.civicsense.complaint.service.image.ImageUploadResult;
import com.civicsense.complaint.service.priority.RiskAssessmentService;
import com.civicsense.user.entity.User;
import com.civicsense.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintIntakeTest {

    @Mock
    private ComplaintRepository complaintRepository;
    @Mock
    private ComplaintImageRepository complaintImageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageKitService imageKitService;
    @Mock
    private AiAnalysisService aiAnalysisService;
    @Mock
    private ComplaintAnalysisRepository complaintAnalysisRepository;
    @Mock
    private ComplaintDuplicateDetectionService duplicateDetectionService;
    @Mock
    private ComplaintClusterService complaintClusterService;
    @Mock
    private GeminiAiService geminiAiService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RiskAssessmentService riskAssessmentService;
    @Mock
    private ExifMetadataExtractorService exifMetadataExtractorService;
    @Mock
    private com.civicsense.complaint.service.geo.GeographicHierarchyService geographicHierarchyService;
    @Mock
    private com.civicsense.complaint.service.geo.LocationValidationService locationValidationService;
    @Mock
    private com.civicsense.complaint.service.image.ImageModerationService imageModerationService;
    @Mock
    private com.civicsense.complaint.service.image.ImageReuseDetectionService imageReuseDetectionService;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setName("Citizen User");
    }

    @Test
    @DisplayName("Should successfully create complaint WITHOUT photo (No-Photo Flow)")
    void testCreateComplaintWithoutPhoto() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });

        AiAnalysisResult dummyAiResult = new AiAnalysisResult(
                "EN",
                "Pothole on Main Road",
                ComplaintCategory.ROAD_DAMAGE,
                ComplaintSeverity.MEDIUM,
                "A large pothole causing slow traffic",
                null,
                0.95,
                null,
                null,
                ComplaintSeverity.MEDIUM,
                false,
                ComplaintSeverity.MEDIUM,
                "Minor vehicle damage",
                ComplaintSeverity.MEDIUM
        );

        when(aiAnalysisService.analyze(any(Complaint.class), eq(null))).thenReturn(dummyAiResult);

        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setCategory(ComplaintCategory.ROAD_DAMAGE);
        when(complaintAnalysisRepository.findByComplaintId(any(UUID.class))).thenReturn(Optional.of(analysis));

        when(duplicateDetectionService.detectDuplicates(any(Complaint.class), any(ComplaintAnalysis.class)))
                .thenReturn(new ComplaintDuplicateDetectionService.DuplicateDetectionResult(
                        ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                        null, null, null, null, null, null
                ));

        CreateComplaintRequest request = new CreateComplaintRequest(
                "Large pothole near main market circle causing traffic delays",
                28.6139,
                77.2090,
                "Near Market Circle",
                28.6145,
                77.2085,
                "DEVICE_GPS_SUGGESTED",
                12.5
        );

        CreateComplaintResponse response = complaintService.createComplaint(userId, request, null);

        assertNotNull(response);
        assertEquals(ComplaintStatus.SUBMITTED, response.status());
        assertEquals("Complaint submitted successfully", response.message());

        // Verify ImageKit was NEVER called for no-photo complaint
        verify(imageKitService, never()).uploadImage(any());
        verify(complaintImageRepository, never()).save(any());

        // Verify primary incident coordinates vs device telemetry
        ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintRepository, atLeastOnce()).save(captor.capture());
        Complaint saved = captor.getAllValues().get(0);

        assertEquals(28.6139, saved.getLatitude());
        assertEquals(77.2090, saved.getLongitude());
        assertEquals(28.6145, saved.getDeviceLatitude());
        assertEquals(77.2085, saved.getDeviceLongitude());
        assertEquals("DEVICE_GPS_SUGGESTED", saved.getLocationSource());
        assertEquals(12.5, saved.getLocationAccuracy());
    }

    @Test
    @DisplayName("Should successfully create complaint WITH photo and extract EXIF")
    void testCreateComplaintWithPhotoAndExif() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });

        MockMultipartFile photo = new MockMultipartFile("image", "pothole.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(imageModerationService.validateFileIntegrity(photo))
                .thenReturn(com.civicsense.complaint.service.image.ImageModerationResult.approved("OK", "RELEVANT"));
        when(imageModerationService.evaluateImageSafetyAndRelevance(any(byte[].class), any(), any(), any()))
                .thenReturn(com.civicsense.complaint.service.image.ImageModerationResult.approved("OK", "RELEVANT"));
        when(imageReuseDetectionService.detectReuse(any(byte[].class), any(), any(), any(), any()))
                .thenReturn(com.civicsense.complaint.service.image.ImageReuseResult.none("mockhash123", "mockdhash123"));
        when(exifMetadataExtractorService.stripExifMetadata(any(byte[].class), any()))
                .thenAnswer(i -> i.getArgument(0));

        ExifMetadata dummyExif = new ExifMetadata(
                28.6138,
                77.2089,
                null,
                true,
                15.2,
                false
        );
        when(exifMetadataExtractorService.extractMetadata(eq(photo), eq(28.6139), eq(77.2090))).thenReturn(dummyExif);
        when(imageKitService.uploadImage(any(byte[].class), eq("pothole.jpg")))
                .thenReturn(new ImageUploadResult("https://ik.imagekit.io/civic/pothole.jpg", "key_123"));

        AiAnalysisResult dummyAiResult = new AiAnalysisResult(
                "EN",
                "Pothole with Photo",
                ComplaintCategory.ROAD_DAMAGE,
                ComplaintSeverity.HIGH,
                "Pothole confirmed by photo",
                null,
                0.98,
                null,
                null,
                ComplaintSeverity.HIGH,
                false,
                ComplaintSeverity.HIGH,
                "Vehicle rim damage",
                ComplaintSeverity.HIGH
        );
        when(aiAnalysisService.analyze(any(Complaint.class), any(ComplaintImage.class))).thenReturn(dummyAiResult);

        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setCategory(ComplaintCategory.ROAD_DAMAGE);
        when(complaintAnalysisRepository.findByComplaintId(any(UUID.class))).thenReturn(Optional.of(analysis));

        when(duplicateDetectionService.detectDuplicates(any(Complaint.class), any(ComplaintAnalysis.class)))
                .thenReturn(new ComplaintDuplicateDetectionService.DuplicateDetectionResult(
                        ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                        null, null, null, null, null, null
                ));

        CreateComplaintRequest request = new CreateComplaintRequest(
                "Large pothole with photographic evidence attached",
                28.6139,
                77.2090,
                "Sector 14 Main Road",
                null,
                null,
                "MANUAL_PIN",
                null
        );

        CreateComplaintResponse response = complaintService.createComplaint(userId, request, photo);

        assertNotNull(response);
        assertEquals(ComplaintStatus.SUBMITTED, response.status());

        // Verify image and EXIF metadata saved
        ArgumentCaptor<ComplaintImage> imageCaptor = ArgumentCaptor.forClass(ComplaintImage.class);
        verify(complaintImageRepository).save(imageCaptor.capture());
        ComplaintImage savedImage = imageCaptor.getValue();

        assertEquals("https://ik.imagekit.io/civic/pothole.jpg", savedImage.getImageUrl());
        assertTrue(savedImage.getHasExifGps());
        assertEquals(28.6138, savedImage.getExifLatitude());
        assertEquals(77.2089, savedImage.getExifLongitude());
        assertEquals(15.2, savedImage.getExifDistanceMeters());
        assertFalse(savedImage.getExifLocationMismatch());
        assertEquals("mockhash123", savedImage.getImageHash());
        assertEquals(com.civicsense.complaint.entity.image.ImageReuseType.NONE, savedImage.getImageReuseType());
        assertFalse(savedImage.getImageReuseSuspicious());
    }

    // =========================================================================
    // Step 4 (b): Invalid location, prohibited image, or unrelated image rejection
    // =========================================================================

    @Test
    @DisplayName("(b) Should reject complaint when incident location is outside supported service area")
    void testCreateComplaint_RejectsInvalidLocation() {
        doThrow(new IllegalArgumentException("Incident location is outside supported Delhi NCR service area."))
                .when(locationValidationService).validateOrThrow(eq(10.0), eq(10.0), any());

        CreateComplaintRequest invalidRequest = new CreateComplaintRequest(
                "Pothole report outside service area",
                10.0,
                10.0,
                "Unknown Island"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.createComplaint(userId, invalidRequest, null)
        );

        assertTrue(ex.getMessage().contains("outside supported"));
        verify(complaintRepository, never()).save(any());
    }

    @Test
    @DisplayName("(b) Should reject complaint when attached image contains prohibited content")
    void testCreateComplaint_RejectsProhibitedImage() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> {
            Complaint c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        MockMultipartFile prohibitedPhoto = new MockMultipartFile(
                "image", "explicit.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
        );

        when(imageModerationService.validateFileIntegrity(prohibitedPhoto))
                .thenReturn(com.civicsense.complaint.service.image.ImageModerationResult.approved("OK", "RELEVANT"));
        when(imageModerationService.evaluateImageSafetyAndRelevance(any(), any(), any(), any()))
                .thenReturn(com.civicsense.complaint.service.image.ImageModerationResult.prohibited(
                        "Pornographic material detected", "EXPLICIT_NUDITY", 0.98
                ));

        CreateComplaintRequest request = new CreateComplaintRequest(
                "Complaint with prohibited photo",
                28.6139,
                77.2090,
                "Connaught Place"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.createComplaint(userId, request, prohibitedPhoto)
        );

        assertTrue(ex.getMessage().contains("violates safety policies"));
        verify(imageKitService, never()).uploadImage(any(), any());
    }

    @Test
    @DisplayName("(b) Should reject complaint when attached image is clearly unrelated")
    void testCreateComplaint_RejectsUnrelatedImage() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> {
            Complaint c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        MockMultipartFile catPhoto = new MockMultipartFile(
                "image", "cat.jpg", "image/jpeg", new byte[]{5, 6, 7, 8}
        );

        when(imageModerationService.validateFileIntegrity(catPhoto))
                .thenReturn(com.civicsense.complaint.service.image.ImageModerationResult.approved("OK", "RELEVANT"));
        when(imageModerationService.evaluateImageSafetyAndRelevance(any(), any(), any(), any()))
                .thenReturn(com.civicsense.complaint.service.image.ImageModerationResult.unrelated(
                        "Image depicts a household cat, unrelated to road damage", 0.95
                ));

        CreateComplaintRequest request = new CreateComplaintRequest(
                "Pothole complaint with cat photo",
                28.6139,
                77.2090,
                "Connaught Place"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> complaintService.createComplaint(userId, request, catPhoto)
        );

        assertTrue(ex.getMessage().contains("unrelated to the complaint"));
        verify(imageKitService, never()).uploadImage(any(), any());
    }

    // =========================================================================
    // Step 4 (c): Suspicious signals flag verification without blocking dispatch or lowering priority
    // =========================================================================

    @Test
    @DisplayName("(c) Should successfully submit complaint with suspicious image reuse and EXIF conflict, flagging verification without lowering priority")
    void testCreateComplaint_SuspiciousReuseAndExifConflict_FlagsVerificationWithoutBlockingDispatch() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> {
            Complaint c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });

        MockMultipartFile reusedPhoto = new MockMultipartFile(
                "image", "reused_pothole.jpg", "image/jpeg", new byte[]{10, 20, 30}
        );

        when(imageModerationService.validateFileIntegrity(reusedPhoto))
                .thenReturn(com.civicsense.complaint.service.image.ImageModerationResult.approved("OK", "RELEVANT"));
        when(imageModerationService.evaluateImageSafetyAndRelevance(any(), any(), any(), any()))
                .thenReturn(com.civicsense.complaint.service.image.ImageModerationResult.approved("OK", "RELEVANT"));

        // Suspicious reuse across different locations
        when(imageReuseDetectionService.detectReuse(any(), any(), any(), any(), any()))
                .thenReturn(com.civicsense.complaint.service.image.ImageReuseResult.differentLocation(
                        2, true, "sha256_pothole", "dhash_pothole"
                ));

        // EXIF location mismatch (photo taken 15km away)
        ExifMetadata mismatchExif = new ExifMetadata(
                28.7041, 77.1025, null, true, 15400.0, true
        );
        when(exifMetadataExtractorService.extractMetadata(eq(reusedPhoto), eq(28.6139), eq(77.2090)))
                .thenReturn(mismatchExif);
        when(exifMetadataExtractorService.stripExifMetadata(any(), any()))
                .thenAnswer(i -> i.getArgument(0));

        when(imageKitService.uploadImage(any(), any()))
                .thenReturn(new ImageUploadResult("https://ik.imagekit.io/civic/reused.jpg", "key_reused"));

        AiAnalysisResult aiResult = new AiAnalysisResult(
                "EN", "Severe Road Damage", ComplaintCategory.ROAD_DAMAGE,
                ComplaintSeverity.CRITICAL, "Critical road hazard", null, 0.99,
                null, null, ComplaintSeverity.CRITICAL, true,
                ComplaintSeverity.CRITICAL, "High accident risk", ComplaintSeverity.CRITICAL
        );
        when(aiAnalysisService.analyze(any(), any())).thenReturn(aiResult);

        ComplaintAnalysis analysis = new ComplaintAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setCategory(ComplaintCategory.ROAD_DAMAGE);
        analysis.setSeverity(ComplaintSeverity.CRITICAL);
        when(complaintAnalysisRepository.findByComplaintId(any())).thenReturn(Optional.of(analysis));

        when(duplicateDetectionService.detectDuplicates(any(), any()))
                .thenReturn(new ComplaintDuplicateDetectionService.DuplicateDetectionResult(
                        ComplaintDuplicateScoringService.DuplicateDecision.SEPARATE,
                        null, null, null, null, null, null
                ));

        CreateComplaintRequest request = new CreateComplaintRequest(
                "Severe pothole on high-speed road corridor",
                28.6139,
                77.2090,
                "Ring Road Bypass"
        );

        // ACT: Submit complaint
        CreateComplaintResponse response = complaintService.createComplaint(userId, request, reusedPhoto);

        // ASSERT: Dispatch is NOT blocked — status is SUBMITTED
        assertNotNull(response);
        assertEquals(ComplaintStatus.SUBMITTED, response.status());

        // Verify image saved with suspicious signals attached for verification gate
        ArgumentCaptor<ComplaintImage> imgCaptor = ArgumentCaptor.forClass(ComplaintImage.class);
        verify(complaintImageRepository).save(imgCaptor.capture());
        ComplaintImage savedImg = imgCaptor.getValue();

        assertTrue(savedImg.getExifLocationMismatch(), "EXIF location mismatch flag must be preserved");
        assertTrue(savedImg.getImageReuseSuspicious(), "Suspicious reuse flag must be preserved");
        assertEquals(com.civicsense.complaint.entity.image.ImageReuseType.DIFFERENT_LOCATION, savedImg.getImageReuseType());

        // Verify M5 risk assessment was invoked with unlowered severity/risk
        verify(riskAssessmentService).assess(any(Complaint.class), eq(analysis));
    }
}