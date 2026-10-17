# CivicSense — Backend Refactor Brain

> Last updated: 2026-10-09T01:31 IST

---

## Project Layout

```
d:\CivicSense\
├── backend/   (Spring Boot 3, Java 21, JPA/Hibernate, PostgreSQL)
└── frontend/  (React + Vite)
```

---

## Architecture Overview — Complaint Pipeline

```
Citizen submits complaint
  → (optional photo, confirmed map pin, description)
  → ComplaintController
  → ComplaintServiceImpl.createComplaint()

  [GEOGRAPHIC RESOLUTION]
  → GeographicHierarchyService.resolveWard(lat, lon)
      • Resolves confirmed map pin to Delhi NCR Zone + Ward
      • Attaches ward to Complaint entity (ward_id foreign key)
      • Officer assignment remains disabled for now

  [INTAKE & EXIF]
  → ExifMetadataExtractorService  (server-side EXIF; never reject if absent)
  → ImageKitService               (upload only if photo present)
  → ComplaintImage saved (EXIF fields attached)

  [M3 — Gemini Analysis]
  → AiAnalysisServiceImpl.analyze()
    → GeminiAiService.analyzeComplaint()
      • With photo  → multimodal prompt (imageAuthenticity, descriptionImageConsistency)
      • Without photo → text-only prompt (imageAuthenticity=NOT_APPLICABLE, descriptionImageConsistency=NOT_APPLICABLE)
    → EvidenceLevelService.compute()   (deterministic, post-Gemini)
    → ComplaintAnalysis saved with:
        evidenceLevel   (E0–E3)
        lowEvidence     (boolean flag)
        descriptionImageMismatch (boolean flag)

  [M4 — Duplicate Detection]
  → ComplaintDuplicateDetectionService.detectDuplicates()
    → ComplaintDuplicateScoringService.calculateCombinedScore()
      • hasImageComparison=false when one/both complaints lack photo
      • IMAGE_WEIGHT redistributed via normalization when no images
    → Stage-2 Gemini (verifyDuplicateComplaints) for DUPLICATE / POTENTIAL_DUPLICATE
      • Safe: null images never sent to Gemini; Parts built conditionally

  [M5 — Risk Assessment & Verification Gate]
  → RiskAssessmentServiceImpl.assess()
    • Severity, Corroboration, Location, Infrastructure weights unchanged
    • Priority is calculated strictly from risk score (never downgraded due to evidence)
    • VerificationGateService.evaluate()   (deterministic verification decision)
        - HIGH/CRITICAL severity or risk + low evidence (E0) -> verificationRequired = true
        - Conflicting / suspicious flags -> verificationRequired = true
        - Normal complaints -> verificationRequired = false
    • Stored in RiskAssessment: verificationRequired (boolean), verificationReason (string)

  [COMMUNITY & CITIZEN CORROBORATION]
  → Nearby Complaints discovery during complaint filing & browsing
    • Discovers nearby complaints based on confirmed incident pins within configurable radius (e.g. 3km / 5km)
    • Encourages "I'm affected too" corroboration instead of creating duplicates
    • Corroborations directly boost M5 Corroboration score and citizen counts
```

---

## What Has Been Done So Far

### Phase 1 — OTP Refactor ✅
- Decoupled OTP from IdentityVerification.
- `DevelopmentOtpDeliveryProvider` active by default; `SmsOtpDeliveryProvider` is stub.
- Dropped `identity_verification_id` column from `otp_verifications` via live migration.

### Phase 2 — Frontend Registration Replacement ✅
- Removed old Aadhaar/OCR flow entirely.
- New flow: Name → Email → Mobile → Password → OTP → Create Account → Login.
- `/register` and `/register/verify` pages implemented.

### Phase 3 — Complaint Intake / Location Foundation ✅
- Photo is **optional** throughout frontend, backend, and Gemini AI analysis.
- **Primary incident location** = citizen-confirmed map pin (`latitude`, `longitude`).
- Device GPS stored as telemetry only (`deviceLatitude`, `deviceLongitude`, `locationSource`, `locationAccuracy`).
- Server-side EXIF extraction (`ExifMetadataExtractorService`): GPS + timestamp stored on `ComplaintImage`; Haversine distance vs confirmed pin; `exifLocationMismatch` flag when > 500 m.
- EXIF GPS never exposed publicly.
- `GeminiAiService`: text-only path safe when no photo; `verifyDuplicateComplaints` safe with null images.
- Tests: `ComplaintIntakeTest` 2/2, `ExifMetadataExtractorServiceTest` 3/3.

### Phase 4 — Evidence Level + AI Pipeline Flags ✅
- Added `NOT_APPLICABLE` to `ImageAuthenticity` & `DescriptionImageConsistency`.
- Added `EvidenceLevel` enum (E0–E3).
- Added `evidenceLevel`, `lowEvidence`, `descriptionImageMismatch` to `ComplaintAnalysis`.
- Created `EvidenceLevelService` for deterministic post-Gemini evidence calculation.
- Normalized weights in `ComplaintDuplicateScoringService` when images are absent.
- Tests: `EvidenceLevelServiceTest` 12/12 passing.

### Phase 5 — Verification Gate Foundation ✅
- Implemented deterministic `VerificationGateService`.
- High severity/risk + low evidence (E0) triggers `verificationRequired = true` (`HIGH_SEVERITY_LOW_EVIDENCE`).
- Priority is strictly preserved and never lowered due to weak evidence.
- Conflicting evidence or suspicious flags trigger verification.
- Added `verificationRequired` and `verificationReason` to `RiskAssessment` entity.
- Tests: `VerificationGateServiceTest` 5/5 passing.

### Phase 6 — Geographic Administrative Foundation (Delhi NCR) ✅
- Implemented clean Delhi NCR administrative hierarchy:
  - `AdministrativeZone` entity (`administrative_zones` table) — Zone name, code, region.
  - `AdministrativeWard` entity (`administrative_wards` table) — Ward name, wardNumber, bounding box (`minLatitude`, `maxLatitude`, `minLongitude`, `maxLongitude`), centroid (`centerLatitude`, `centerLongitude`), extensible `assignedOfficerId`.
  - Relationship: Zone (1) ── (N) Ward.
  - Complaint (N) ── (1) Ward (via `ward_id` on `complaints` table).
- Implemented `GeographicHierarchyService`:
  - Resolves confirmed incident map pin to an `AdministrativeWard`.
  - Falls back gracefully to empty if outside known bounds; tie-breaks overlaps via centroid distance.
  - Device GPS and EXIF are **never** used for administrative routing.
- Implemented `DelhiNcrAdministrativeDataInitializer` (`CommandLineRunner`):
  - Automatically seeds baseline Delhi NCR Zones (South, Central, North/Rohini, East, West) and wards covering major areas if database is unpopulated.
- Officer assignment and routing remain completely disabled for now.
- Tests: `GeographicHierarchyServiceTest` 4/4 passing.

### Phase 7 — Community Module Integration with New Architecture ✅
- Integrated EXISTING Community module with the new complaint intake and architecture:
  - Added nearby discovery endpoint `GET /api/v1/community/complaints/nearby?latitude={lat}&longitude={lon}&radiusKm={radius}`.
  - Haversine distance filtering strictly uses confirmed complaint map pins (`c.latitude`, `c.longitude`), falling back gracefully if coordinates are absent.
  - Added `NearbyCommunityComplaints.jsx` in frontend complaint submission flow when a pin is selected, so citizens can discover nearby issues and click "I'm affected too" directly rather than creating redundant duplicates.
  - Added "📍 Near Me / Nearby Only" toggle in `RecentReportsSection.jsx` / `Home.jsx` requiring explicit geolocation permission.
  - Strictly preserves privacy: citizen's exact GPS location is never exposed or logged.
  - Full corroboration loop: "I'm affected too" calls `supportComplaint(...)` which updates corroboration metrics and automatically contributes to M5 Corroboration score.
  - Tests: `ComplaintCommunityServiceTest` (8/8 PASS), `CommunityControllerTest` (18/18 PASS), `CitizenCorroborationServiceTest` (14/14 PASS). Total suite: 80/80 PASS.

### Phase 8 — Frontend Motion, Micro-Interactions & Responsive UI Polish ✅ ← COMPLETED
- Installed and configured `gsap` with ScrollTrigger in `src/utils/motion.js`, with central `prefersReducedMotion()` support and safe `gsap.context()` cleanup lifecycle across all animated components.
- Motion implementations:
  - `HeroSection.jsx`: Staggered entrance for headline, badge, subtitle, action buttons, and workflow card steps.
  - `StatsBar.jsx`: ScrollTrigger staggered reveal for metrics counters.
  - `FeaturesSection.jsx`: ScrollTrigger staggered entrance for civic feature cards.
  - `RecentReportsSection.jsx`: ScrollTrigger reveal for header & category filters, plus a touch-friendly snap-scrolling carousel track.
  - `BottomCtaSection.jsx`: ScrollTrigger subtle entrance for bottom CTA container.
  - `AboutPage.jsx` & `HowItWorksPage.jsx`: ScrollTrigger reveals for values, process steps, and technology cards.
  - `ComplaintSuccessModal.jsx` & `RegistrationSuccess.jsx`: Micro-interaction bounce-in on success checkmark badges.
- Micro-interactions & CSS Polish:
  - Active press feedback (`active:scale-[0.98]`) on all buttons, forms, and cards.
  - Hover lift on cards (`card-hoverable`, `hover:-translate-y-0.5 hover:shadow-elevated`).
  - Input focus glow ring (`focus:ring-2 focus:ring-teal-500/20`).
  - OtpInput focus box scale (`focus:scale-[1.03]`).
  - Community support button state transition and touch-target padding.
- Responsive mobile enhancements:
  - `OtpInput.jsx`: Fixed horizontal overflow on 320px screens using responsive `grid grid-cols-6 max-w-sm` and responsive widths.
  - `StatsBar.jsx`: Refined narrow mobile typography and padding to prevent awkward line breaks on 320px screens.
  - `HomeNavbar.jsx`: Responsive brand title with hidden subtitle below 640px to prevent header collapse on 320px screens; touch-friendly mobile drawer links (`min-h-[44px]`).
  - `RecentReportsSection.jsx`: Responsive carousel track (`flex overflow-x-auto snap-x snap-mandatory pb-4 no-scrollbar`) fitting 1 card with peek on mobile, 2 on tablet, 4 on desktop.
  - `CommunityDetailModal.jsx`: Responsive padding (`p-2.5 sm:p-6`), wrapping engagement metadata, and full-width thumb button on mobile.
  - `ComplaintForm.jsx`: Responsive suggested location banner (`flex-col sm:flex-row`) and touch targets for categories, map buttons, and file dropzone.
  - `CivicGuidelines.jsx`: Emergency helpline cards made responsive on mobile (`grid-cols-1 sm:grid-cols-2 lg:grid-cols-4`).
- Accessibility & Reduced Motion:
  - Global `prefers-reduced-motion: reduce` honored in `index.css` and bypassed in GSAP animations.
- Verification:
  - `npm run build`: built clean in `1.97s` (92 modules transformed).
  - `npm run lint`: passed (0 errors, 2 pre-existing context warnings).

### Phase 9 — Interactive Leaflet Map Incident-Location Picker ✅ ← COMPLETED
- Replaced external Google Maps redirection links (`https://www.google.com/maps/search/?api=1&query=...`) with an in-app interactive map picker modal.
- Created `frontend/src/components/complaint/LocationPickerModal.jsx`:
  - Interactive Leaflet map powered by free OpenStreetMap tiles (no paid API keys, zero external cost).
  - High-visibility custom SVG teal marker matching CivicSense brand.
  - Interactive placement: click anywhere on the map or drag the marker with real-time coordinate updates.
  - Free OpenStreetMap Nominatim forward-geocoding search bar with debounce and dropdown selection.
  - "My GPS" locator button within the modal.
  - Strict explicit confirmation pattern: marker movements in the modal do not mutate parent form state until citizen clicks "Confirm Location". Clicking "Cancel" or backdrop/Esc dismisses without changing coordinates.
  - Proper lifecycle cleanup (`map.remove()`) and `map.invalidateSize()` timer on modal open.
- Integrated into `frontend/src/components/complaint/ComplaintForm.jsx`:
  - Added "Choose on Map" action button directly in the Incident Location header.
  - Replaced "View on Map" links in both suggested GPS and confirmed pin banners with buttons opening `LocationPickerModal`.
  - Added `handleConfirmMapLocation({ latitude, longitude })` handler updating form coordinates, setting `locationSource='MAP_PIN'`, `locationStatus='confirmed'`, and clearing validation errors.
  - Manual numeric coordinate entry fields remain fully intact and editable.
- CSS updates:
  - Added `.custom-map-pin` transparent reset in `frontend/src/index.css` for clean SVG marker rendering.
- Validation:
  - `npm run lint`: passed (0 errors, 2 pre-existing context warnings).
  - `npm run build`: built clean in `1.62s` (95 modules transformed).

### Phase 10 — M5 Risk Assessment Classpath & Compile Fix ✅
- Resolved runtime `NoClassDefFoundError: RiskAssessmentServiceImpl$1` during M5 Risk Assessment.
- Root cause: Stale incremental compiler state dropped the javac-generated synthetic inner class `$1` needed for enum switch map lookup (`switch (severity)`).
- Executed clean Maven build (`./mvnw clean compile`) which regenerated `RiskAssessmentServiceImpl$1.class` and `InfrastructureRiskService$1.class` in `target/classes`.
- Added focused unit test suite `RiskAssessmentServiceImplTest` covering all severities (LOW, MEDIUM, HIGH, CRITICAL, null) — 4/4 tests passing.

---

## Phase 11 — Validation & Integrity: Step 1 (Location & Service Area Validation)

Implemented comprehensive client-side and server-side coordinate validation and service area integrity:

1. **Configurable Service Area & Bounds**:
   - Delhi NCR bounding box: Latitude `[28.20, 29.00]`, Longitude `[76.70, 77.60]`. Configurable via Spring properties (`civicsense.geo.service-area.*`) with defaults.
   - Rejects Null Island `(0, 0)`, non-numerical values, NaNs, infinities, and out-of-range coordinates.
   - Detects transposed/swapped latitude and longitude (e.g. lat in 77.x, lon in 28.x) and returns structured suggestions (`"Did you mean lat, lon?"`) instead of silently swapping.

2. **Water-Body Awareness & Advisories**:
   - Built-in detection for major Delhi water bodies (Sanjay Lake, Bhalswa Lake, Hauz Khas Lake) and Yamuna River corridor segments using segment projection algorithms.
   - Non-blocking advisory: issues warnings for road/infrastructure categories located in water bodies, prompting bridge/flyover confirmation without preventing legitimate reports.

3. **Backend Service & API (`LocationValidationService`)**:
   - `LocationValidationService` runs at Step 1 of complaint intake (`ComplaintServiceImpl.createComplaint()`) before any DB writes or AI processing.
   - Throws `IllegalArgumentException` on invalid or out-of-service-area locations, caught cleanly by `GlobalExceptionHandler`.
   - Lightweight public endpoint `GET /api/v1/complaints/validate-location` provides real-time verification for external clients.
   - 8 unit tests in `LocationValidationServiceTest` cover all boundary conditions, nulls, swapped coordinates, and water bodies.

4. **Frontend Map Picker & Manual Form Enforcement**:
   - `LocationPickerModal.jsx`: Computes validation state directly on pin movement. Displays inline error banners for outside-service-area and swapped coords, provides a "Use corrected" action for swaps, displays water-body advisories, shows reverse-geocoded addresses via Nominatim, and disables the "Confirm Location" button when coordinates are invalid.
   - `ComplaintForm.jsx`: Enforces the same validation rules on manual coordinate entry in `validate()` before dispatching to the backend.

---

## Phase 12 — Validation & Integrity: Step 2 (Image Validation, Moderation & Privacy)

Implemented comprehensive image file integrity, civic-aware content moderation, fail-safe quarantine, and EXIF privacy stripping:

1. **Image File Integrity & Format Validation (`ImageModerationService.java`)**:
   - Enforced 10MB file size limit and strict content-type validation (`image/jpeg`, `image/jpg`, `image/png`).
   - Magic number byte verification: checks JPEG `[0xFF, 0xD8, 0xFF]` and PNG `[0x89, 0x50, 0x4E, 0x47]` header signatures to prevent spoofed file extensions.
   - Image decodability check via `ImageIO.read` to block corrupted or unreadable images.

2. **Civic-Aware Multimodal Moderation via Gemini**:
   - Integrated pre-screening using `gemini-3.6-flash` before cloud storage or M3 processing.
   - High-confidence prohibited categories: `EXPLICIT_NUDITY`, `HATE_EXTREMISM`, `HARASSMENT_DOXXING`, `SEVERE_NON_CIVIC_VIOLENCE`.
   - Civic protection policy: explicitly allows legitimate unpleasant civic photographs (sewage, dirty water, rotten garbage, roadkill/carcasses, potholes, minor accident damage, burnt trash).
   - High-confidence prohibited images are rejected immediately and never uploaded to cloud storage or sent to Gemini complaint analysis.

3. **Fail-Safe Quarantine**:
   - If the moderation service fails or is unreachable, the image is **quarantined** (`quarantined = true`), not treated as safe.
   - Quarantined images are withheld from public ImageKit storage and normal AI processing until administrative review.

4. **Security & Privacy (EXIF Stripping)**:
   - Server-side EXIF metadata is extracted as an intake hint, but the image is re-encoded via `ExifMetadataExtractorService.stripExifMetadata()` before uploading to public cloud storage, ensuring no GPS or camera hardware metadata is leaked.

5. **Frontend Submit Button Locking, Live Moderation and Relevance**:
   - `ComplaintForm.jsx`: Disables Submit while screening, or if prohibited, unrelated, or quarantined.
   - Preview border turns red for prohibited/unrelated; amber for quarantined.
   - Inline badges: Screening, Approved, Prohibited, Unrelated Photo, Verification Required.
   - Auto-re-runs moderation when image, category, or description changes (debounced 400ms).
   - Text-only submission always allowed when image is removed.
   - Backend `ComplaintServiceImpl` re-enforces all rules on every request.

6. **Image Relevance Evaluation (Combined Safety + Relevance, Single Gemini Call)**:
   - `ImageModerationService.evaluateImageSafetyAndRelevance()` checks safety policy AND civic relevance in one `gemini-3.6-flash` multimodal call.
   - Four outcomes: `RELEVANT`, `POTENTIALLY_RELEVANT` (allowed), `UNRELATED` (blocks), `UNCERTAIN` (quarantined).
   - Civic exception: outdoor/public-context photos are `POTENTIALLY_RELEVANT` even if the fault is not clearly visible.
   - Clearly unrelated images (cat photos, selfies, food, memes): `UNRELATED` status blocks submission.
   - `ComplaintController.validateImage` accepts optional `category` and `description` params.
   - `ImageModerationResult`: added `boolean unrelated`, `String relevanceStatus`, factory `unrelated(reason, confidence)`.
   - Tests: `ImageModerationServiceTest` 8/8 PASS.

---

## Phase 13 — Validation & Integrity: Step 3 (EXIF Analysis & Image Reuse Detection)

Implemented server-side EXIF provenance extraction, cryptographic & perceptual image hashing, contextual reuse detection, and verification gate integration:

1. **Server-Side EXIF Metadata & Privacy (`ExifMetadataExtractorService.java`)**:
   - Extracts camera GPS coordinates and capture timestamp via `metadata-extractor`.
   - Compares EXIF GPS against citizen-confirmed incident pin using Haversine distance; flags `EXIF_LOCATION_MISMATCH` when distance > 500m.
   - Non-blocking & privacy-safe: missing or unparseable EXIF is completely normal and never generates suspicious findings. Confirmed pin coordinates are NEVER replaced with EXIF coordinates.
   - EXIF metadata is stripped from image bytes before uploading to public ImageKit cloud storage (`stripExifMetadata()`), and raw EXIF fields are omitted from all public API responses.

2. **Cryptographic & Perceptual Hashing (`ImageHasher.java`)**:
   - Computes SHA-256 hash (64 hex characters) on raw image bytes for exact byte-identical duplicate detection.
   - Computes 64-bit Difference Hash (dHash) in pure Java (using standard `BufferedImage` and grayscale 9x8 matrix) for detecting visually identical / re-compressed images across chat platforms (WhatsApp, Telegram).
   - Zero additional external dependencies or vector databases needed.

3. **Contextual Image Reuse Detection (`ImageReuseDetectionService.java`)**:
   - Evaluates prior occurrences of identical hashes across complaints against user identity and location distance (150m threshold).
   - Five distinct classifications (`ImageReuseType`):
     - `NONE`: Unique photo.
     - `RESUBMISSION`: Same user, same spot (<= 150m) — legitimate follow-up; not treated as fraud.
     - `COMMUNITY_SHARED`: Different users, same spot (<= 150m) — legitimate neighborhood collaboration; not treated as fraud.
     - `DIFFERENT_LOCATION`: Reused at a different geographic location (> 150m) — suspicious provenance signal.
     - `MULTI_LOCATION_DISPERSED`: Reused across 3+ distinct locations — strong suspicious signal.
   - Invariant: Image reuse findings NEVER automatically reject a complaint or reduce its priority.

4. **Evidence & Verification Gate Integration (`VerificationGateService.java`, `EvidenceLevelService.java`)**:
   - `VerificationGateService` flags `EXIF_LOCATION_MISMATCH` and `SUSPICIOUS_IMAGE_REUSE_*` for field verification (`verificationRequired = true`), but preserves high/critical priority and never halts automated complaint dispatch.
   - `EvidenceLevelService` prevents promoting suspicious cross-location reused images to `E2` corroboration.
   - Tests: ImageReuseDetectionServiceTest (5/5 PASS), ComplaintIntakeTest (6/6 PASS), EvidenceLevelServiceTest (12/12 PASS), VerificationGateServiceTest (5/5 PASS), ImageModerationServiceTest (8/8 PASS).


---

## Phase 14 — Validation & Integrity: Step 4 (Validation & M2–M5 Pipeline Integration)

Completed full architectural integration of all validation and provenance signals with the M2–M5 pipeline:

1. **Intake Gatekeeping & Blocking**:
   - Location validation runs at Step 1 (locationValidationService.validateOrThrow) before DB writes or AI calls. Out-of-service-area locations and invalid numbers immediately throw IllegalArgumentException (HTTP 400).
   - Image safety moderation and civic relevance check runs at Step 4. High-confidence prohibited images and clearly unrelated photos throw IllegalArgumentException and are blocked from storage and Gemini analysis.
2. **Text-Only Complaint Guarantee**:
   - Issue photographs remain 100% optional. Submitting without a photo skips image storage and hashing, sets EvidenceLevel.E0, and flows through M2, M3, M4, and M5 seamlessly.
3. **Suspicious Findings vs Hard Rejections**:
   - Provenance signals (EXIF distance mismatch > 500m, cross-location image reuse) are treated as advisory review signals, NOT submission blockers.
   - The complaint is submitted normally with status = SUBMITTED and dispatch is never blocked.
4. **Verification Gate & Priority Preservation (M5)**:
   - Suspicious signals (EXIF_LOCATION_MISMATCH, SUSPICIOUS_IMAGE_REUSE_*) trigger erificationRequired = true in VerificationGateService for officer field queues.
   - Priority is computed strictly from the weighted risk formula and is **never downgraded** by suspicious evidence or missing EXIF.
5. **Incident Location Truth**:
   - The citizen-confirmed map pin remains the immutable source of truth for administrative ward resolution (M2), clustering, and GIS queries. EXIF GPS is never substituted for incident coordinates.
---

## Active Next Steps

1. **Officer Verification Queue & Evidence Promotion (M7 Preparation)**
   - Expose verification-gate filtered complaints for officer review queues (`verificationRequired = true`).
   - Allow officers to conduct field verification and upgrade evidence level from `E0`/`E1`/`E2` to `E3`.

2. **Officer Routing by Ward (When Officer Workflow is Introduced)**
   - Connect `AdministrativeWard.assignedOfficerId` / department mapping to route complaints to specific ward officers.

3. **M4 Duplicate Detection Polish**
   - Candidate duplicate windowing & optional time-decay similarity scoring.

---

## Key Constraints & Invariants

- **Photo is always optional.** No complaint rejected for missing photo.
- **Primary incident location** = `complaint.latitude` / `complaint.longitude` (confirmed pin).
- **Device GPS** = `complaint.deviceLatitude` / `deviceLongitude` (telemetry only).
- **EXIF GPS** = `complaintImage.exifLatitude` / `exifLongitude` (supporting hint; never public).
- **Geographic routing strictly uses confirmed pin** (never device GPS or EXIF).
- **Priority is never downgraded due to low evidence.**
- **Community module** — nearby discovery uses confirmed pins; corroboration feeds directly into M5 risk scoring.
- **Privacy** — user's live device location is never persisted or publicly exposed.
- **Officer routing & assignment** — intentionally disabled / unassigned for now.
- **Animations** — purposeful, subtle, respect `prefers-reduced-motion`, clean `gsap.context()` cleanup on unmount.
- **Map Picker** — in-app modal using Leaflet + OpenStreetMap; explicit citizen confirmation required; no paid map API keys.

---

## File Locations — Key Files

| Purpose | File |
|---|---|
| Interactive Map Picker Modal | [`LocationPickerModal.jsx`](file:///d:/CivicSense/frontend/src/components/complaint/LocationPickerModal.jsx) |
| Complaint Form (Responsive Intake) | [`ComplaintForm.jsx`](file:///d:/CivicSense/frontend/src/components/complaint/ComplaintForm.jsx) |
| Motion Utilities (GSAP & ScrollTrigger) | [`motion.js`](file:///d:/CivicSense/frontend/src/utils/motion.js) |
| Global Styles & Micro-interactions | [`index.css`](file:///d:/CivicSense/frontend/src/index.css) |
| Home Navbar (Responsive Header) | [`HomeNavbar.jsx`](file:///d:/CivicSense/frontend/src/components/home/HomeNavbar.jsx) |
| Hero Section (Staggered Entrance) | [`HeroSection.jsx`](file:///d:/CivicSense/frontend/src/components/home/HeroSection.jsx) |
| Stats Bar (ScrollTrigger Counter) | [`StatsBar.jsx`](file:///d:/CivicSense/frontend/src/components/home/StatsBar.jsx) |
| Features Section (ScrollTrigger Cards) | [`FeaturesSection.jsx`](file:///d:/CivicSense/frontend/src/components/home/FeaturesSection.jsx) |
| Recent Reports Section (Carousel & Filters) | [`RecentReportsSection.jsx`](file:///d:/CivicSense/frontend/src/components/home/RecentReportsSection.jsx) |
| Community Detail Modal (Mobile Sheet) | [`CommunityDetailModal.jsx`](file:///d:/CivicSense/frontend/src/components/home/CommunityDetailModal.jsx) |
| OTP Input (Responsive Mobile Grid) | [`OtpInput.jsx`](file:///d:/CivicSense/frontend/src/components/registration/OtpInput.jsx) |
| Auth Layout (Responsive Shell) | [`AuthLayout.jsx`](file:///d:/CivicSense/frontend/src/components/layout/AuthLayout.jsx) |