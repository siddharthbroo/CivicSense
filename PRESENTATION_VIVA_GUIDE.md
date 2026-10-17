# CivicSense – Presentation & Viva Guide

## 1. Project in One Minute

- **What it is:** CivicSense is an AI-powered civic issue management backend that automates how citizen complaints (potholes, garbage, water leaks, broken streetlights) are processed, verified, and prioritized.
- **Problem it solves:** Municipalities are flooded with redundant, unverified, and unstructured complaints. Manual sorting creates backlogs, duplicate work, and slow response times.
- **Who uses it:** Citizens reporting civic problems, and municipal authorities prioritizing and resolving community issues.
- **What makes it useful:** It does not physically fix potholes; it transforms raw complaint photos and descriptions into structured, deduplicated, and prioritized action tickets using AI.
- **One-Minute Viva Pitch:**  
  _"CivicSense is an intelligent civic complaint management platform. When a citizen uploads a photo of an issue like a pothole with GPS coordinates, our system verifies their identity via OTP/OCR, stores the image securely on ImageKit, uses Google Gemini AI to extract category and severity, groups duplicates into clusters using a Python AI microservice, and calculates an objective risk priority score (0–100) using nearby infrastructure data. This allows municipal authorities to address critical, high-impact issues first instead of drowning in duplicate manual reports."_

---

## 2. Overall Architecture

```
User / Citizen
  ↓ (HTTP / Multipart / JSON)
Frontend (React SPA)
  ↓ (REST API + JWT Bearer Token)
Spring Boot Backend (Port 8080 - Business Logic, Validation, Security)
  ├─ PostgreSQL (Database - Users, Complaints, Clusters, Comments)
  ├─ Google Gemini 3.6 Flash (Multimodal AI - Visual Analysis & Duplicate Verification)
  ├─ Python AI Microservice (Port 8001 - Sentence Transformers & CLIP Embeddings)
  ├─ ImageKit (Cloud CDN Media Storage)
  ├─ Geoapify (Geospatial Places & Sensitive Infrastructure API)
  └─ Tesseract OCR (Local Tess4J - Citizen ID Card Extraction)
```

- **React:** Single-page frontend providing user interface for submitting complaints, tracking reports, and community feed.
- **Spring Boot:** Core enterprise backend managing business logic, REST APIs, security, and orchestrating external services.
- **PostgreSQL:** Relational database storing users, complaints, images, AI analysis metadata, clusters, and comments.
- **Python AI Service:** FastAPI service generating text (Sentence Transformers) and image (CLIP) embeddings for duplicate similarity.
- **JWT & Spring Security:** Stateless token-based security protecting sensitive endpoints and authenticating citizens.
- **External APIs:** ImageKit for media storage, Geoapify for spatial POI context, and Google Gemini for vision-language reasoning.

---

## 3. Module-wise Explanation

### Module M1 – Authentication & Identity

- **Purpose:** Verifies citizen identity using phone OTP and government ID card OCR before account creation.
- **Simple Flow:** Send Phone OTP → Verify OTP → Upload ID Card (Aadhaar/Voter ID) → Tess4J OCR extracts data → Confirm & Register User.
- **Main Features:** SecureRandom 6-digit OTP with BCrypt hashing; local Tesseract OCR with image upscaling; pre-registration draft verification records.
- **Input:** Phone number, OTP code, identity document image, confirmed name, email, password.
- **Processing:** Hashes OTP; extracts ID name/number via OCR; validates matches; creates BCrypt-hashed user account.
- **Output:** JWT token (24h validity), user profile, verification status (`CONFIRMED`).
- **Important Concepts:** `IdentityVerification` is kept separate from `User` because it is created before the citizen account exists.
- **Possible Viva Questions:**
  1. _What does this module do?_ Authenticates users and verifies real citizen identity using OTP and ID card OCR.
  2. _Why is it needed?_ Prevents bot spam and ensures accountability for civic complaints.
  3. _How does it work?_ Checks phone via 6-digit OTP hash, parses ID card text via Tess4J, and saves verified user details.
  4. _What happens if OCR fails?_ Citizen can manually edit and confirm their details; failure does not block registration.

### Module M2 – Complaint Intake

- **Purpose:** Securely accepts citizen civic complaints, validates inputs, and uploads evidence images to cloud CDN.
- **Simple Flow:** Citizen submits form + photo → Validate fields & GPS → Upload image to ImageKit → Save `Complaint` & `ComplaintImage` in DB.
- **Main Features:** Multipart form processing; GPS boundary validation; ImageKit cloud storage; initial status set to `SUBMITTED`.
- **Input:** Multipart request with image file, title, description, latitude, longitude, and address.
- **Processing:** Validates non-empty fields; uploads photo bytes to ImageKit via official SDK; persists entities.
- **Output:** Created complaint record with unique ID and ImageKit hosted URL.
- **Important Concepts:** Image binary is never stored in PostgreSQL; only ImageKit URL, file ID, and metadata are saved.
- **Possible Viva Questions:**
  1. _What does this module do?_ Receives the complaint and stores photo evidence safely in cloud storage.
  2. _Why is it needed?_ Validates citizen input and ensures image evidence is securely hosted with fast CDN delivery.
  3. _How does it work?_ Streams photo to ImageKit Java SDK, receives CDN URL, and writes complaint metadata to PostgreSQL.
  4. _What happens if ImageKit fails?_ Transaction rolls back and returns HTTP 500 error; unpersisted complaint is rejected.

### Module M3 – AI Complaint Understanding (Gemini)

- **Purpose:** Uses multimodal generative AI to automatically inspect the complaint photo and text.
- **Simple Flow:** Intake completed → Send photo + text to Gemini 3.6 Flash → Parse 14 structured JSON fields → Save `ComplaintAnalysis`.
- **Main Features:** Multimodal vision-language understanding; structured schema generation; automated category, severity, and tag extraction.
- **Input:** Complaint image URL, user title, user description, and GPS coordinates.
- **Processing:** Sends prompt to Google GenAI SDK; requests strict JSON schema; parses 14 attributes; updates complaint title/category.
- **Output:** `ComplaintAnalysis` entity with category, severity (0.0–1.0), urgency, hazard tags, summary, and action recommendations.
- **Important Concepts:** If Gemini fails, fallback logic assigns default category `OTHER`, severity 0.5, and keeps system running.
- **Possible Viva Questions:**
  1. _What does this module do?_ Uses Google Gemini AI to analyze what is wrong in the photo and estimate severity.
  2. _Why is it needed?_ Eliminates manual sorting and standardizes inconsistent citizen descriptions.
  3. _How does it work?_ Prompts Gemini 3.6 Flash with image + text and parses structured JSON response into database fields.
  4. _What happens if Gemini API fails?_ Fallback creates default analysis record; complaint workflow continues without crashing.

### Module M4 – Duplicate & Complaint Intelligence

- **Purpose:** Detects whether a newly filed complaint is reporting an already-known issue nearby.
- **Simple Flow:** Query nearby complaints (±0.005° bounding box) → Stage 1 Python AI similarity score → Stage 2 Gemini visual confirmation → Assign or create cluster.
- **Main Features:** Two-stage detection (statistical/embedding stage + vision verification stage); automated cluster grouping.
- **Input:** New complaint with analysis, location coordinates, and photo.
- **Processing:** Checks candidates within ~550m; computes weighted similarity (title 10%, description 15%, summary 15%, CLIP image 25%, address 15%, distance decay 20%); if score ≥ 0.60, Gemini compares both photos (`SAME`, `DIFFERENT`, `UNCERTAIN`).
- **Output:** Complaint linked to an existing `ComplaintCluster` (count incremented) or a newly created cluster.
- **Important Concepts:** Prevents duplicate municipal work orders while grouping multiple citizen reports together.
- **Possible Viva Questions:**
  1. _What does this module do?_ Identifies and groups duplicate complaints filed by different citizens for the same problem.
  2. _Why is it needed?_ Prevents dispatching multiple crews to the same spot and identifies widely-reported problems.
  3. _How does it work?_ Spatial filtering → Python AI embeddings comparison → Gemini visual confirmation.
  4. _What happens if Python AI service is down?_ Service throws exception; duplicate detection is skipped or marked unclustered.

### Module M5 – Priority & Risk Engine

- **Purpose:** Computes an objective composite Risk Score (0–100) and Priority Level for municipal triage.
- **Simple Flow:** Read AI severity + corroboration count + query Geoapify for nearby schools/hospitals → Compute formula → Assign Priority.
- **Main Features:** 4-factor risk formula; external geospatial POI integration; transparent sub-scores; priority tiering.
- **Input:** AI severity score, cluster report counts, complaint coordinates.
- **Processing:** Calculates formula: `Risk = (Severity × 35) + (Corroboration × 25) + (Location × 20) + (Infrastructure × 20)`.
- **Output:** Priority level: `LOW` (<25), `MEDIUM` (<50), `HIGH` (<75), or `CRITICAL` (≥75).
- **Important Concepts:** Location & Infrastructure factors query Geoapify API for vulnerable facilities within 500m (schools, hospitals, transit).
- **Possible Viva Questions:**
  1. _What does this module do?_ Calculates which complaints need urgent attention using an objective 0–100 score.
  2. _Why is it needed?_ Ensures critical hazards (e.g. open drain near a primary school) are fixed before minor cosmetic issues.
  3. _How does it work?_ Combines AI visual severity, citizen report counts, and Geoapify sensitive infrastructure proximity.
  4. _What happens if Geoapify is unreachable?_ Infrastructure risk defaults to zero; remaining factors still generate priority.

### Module M6 – Community, Corroboration & Comments

- **Purpose:** Provides a public community feed where citizens view local issues, upvote ("I'm affected too"), and comment.
- **Simple Flow:** Citizen views feed → Upvotes or adds comment → DB updates corroboration and thread → Increases community visibility.
- **Main Features:** Public feed by status/category; citizen upvote support; multi-citizen discussion threads; corroboration scoring.
- **Input:** Complaint ID, citizen authentication token, comment text.
- **Processing:** Increments community support count; records comment entity; updates corroboration score (`uniqueCitizens × 25 + repeatReports × 5 + supporters × 5`, max 100).
- **Output:** Updated comment list and higher community corroboration ranking.
- **Important Concepts:** Comments are for discussion only; they do NOT create duplicate complaints or alter AI clusters.
- **Possible Viva Questions:**
  1. _What does this module do?_ Allows citizens to see, support, and discuss nearby civic complaints.
  2. _Why is it needed?_ Gives visibility to local problems and measures community impact through collective citizen feedback.
  3. _How does it work?_ Stores comments in `ComplaintComment` table and upvotes in corroboration tracking table.
  4. _Can comments change complaint category?_ No, comments are purely informational and separated from AI analysis entities.

---

## 4. Complete Complaint Flow

1. **Citizen Submission:** Citizen uploads photo, title, description, and GPS coordinates via React frontend.
2. **Validation & Security:** Backend verifies JWT token and validates that mandatory form fields and GPS are present.
3. **Cloud Media Storage:** Complaint image binary is uploaded to ImageKit CDN, returning a public secure image URL.
4. **Initial Persistence:** Complaint is saved in PostgreSQL with status `SUBMITTED` and linked to `ComplaintImage`.
5. **AI Multimodal Analysis:** Gemini 3.6 Flash inspects photo and text, returning 14 structured attributes (severity, category, tags).
6. **AI Persistence:** Analysis saved in `ComplaintAnalysis`; complaint title and category are updated to standardized AI values.
7. **Spatial Candidate Search:** Backend queries PostgreSQL for active complaints within a $\pm 0.005^{\circ}$ bounding box (~550 meters).
8. **Stage 1 Similarity (Python AI):** Python microservice computes text embeddings, CLIP visual similarity, address, and distance scores.
9. **Stage 2 Visual Verification (Gemini):** If similarity score $\ge 0.60$, Gemini compares both images to confirm if they are the `SAME`.
10. **Clustering:** If `SAME`, complaint is linked to existing `ComplaintCluster`; otherwise, a new cluster is created.
11. **Risk & Priority Calculation:** 4-factor formula combines severity, corroboration, and Geoapify POIs into priority (`LOW` to `CRITICAL`).
12. **Community Interaction:** Complaint appears on public feed where other citizens can upvote or add comments.

---

## 5. Important AI Features

| AI Feature                             | Service / Model                     | Input                                            | Processing                                                           | Output / Return                                                 | Why It Is Useful                                                      |
| :------------------------------------- | :---------------------------------- | :----------------------------------------------- | :------------------------------------------------------------------- | :-------------------------------------------------------------- | :-------------------------------------------------------------------- |
| **Multimodal Complaint Understanding** | Google Gemini 3.6 Flash             | Photo URL, user title, description, GPS          | Analyzes image visual features + text against structured JSON schema | Category, severity (0–1), hazard tags, summary, recommendations | Converts messy user inputs into standardized civic data automatically |
| **Text Semantic Similarity**           | Python AI (`sentence-transformers`) | New title, description, summary vs candidate     | Generates 384-d vector embeddings and computes cosine similarity     | Text similarity score (0.0 to 1.0)                              | Catches duplicate complaints even if citizens use different words     |
| **Visual Image Similarity**            | Python AI (`clip-ViT-B-32`)         | New image bytes vs candidate image bytes         | Generates visual feature vectors and computes cosine similarity      | Image similarity score (0.0 to 1.0)                             | Compares photos of the same scene taken from different angles         |
| **Stage 2 Visual Verification**        | Google Gemini 3.6 Flash             | Both complaint photos + Stage 1 similarity score | Reasoned comparison of landmarks, road layout, and damage            | Verdict (`SAME`, `DIFFERENT`, `UNCERTAIN`) + confidence         | Eliminates false duplicate matches before grouping clusters           |

---

## 6. Database – Simple Understanding

- **Why needed:** Persists user accounts, complaint histories, uploaded media references, AI analysis results, clusters, and comments.
- **Main Entities:**
  - `User`: Registered citizen account details and BCrypt password hash.
  - `IdentityVerification`: Pre-registration draft storing Aadhaar/Voter ID OCR extraction data.
  - `Complaint`: Core complaint record containing location, user reference, status, and AI category.
  - `ComplaintImage`: ImageKit URLs, file ID, and metadata linked to a complaint.
  - `ComplaintAnalysis`: 14 fields of Gemini AI visual output (severity, urgency, tags, summary).
  - `ComplaintCluster`: Deduplication group tracking total complaints, unique citizens, and primary complaint.
  - `ComplaintComment`: Community comments submitted under a complaint.
  - `OtpVerification`: Temporary 6-digit phone verification records with expiry.
- **Key Relationships:**
  - `User` (1) $\to$ submits $\to$ (Many) `Complaint`
  - `Complaint` (1) $\to$ has $\to$ (Many) `ComplaintImage`
  - `Complaint` (1) $\to$ has $\to$ (1) `ComplaintAnalysis`
  - `ComplaintCluster` (1) $\to$ groups $\to$ (Many) `Complaint`
  - `Complaint` (1) $\to$ has $\to$ (Many) `ComplaintComment`

---

## 7. Authentication & Security

- **Registration Flow:** Citizen verifies phone OTP $\to$ uploads ID document (Tess4J OCR extracts name/ID) $\to$ submits password $\to$ user saved in DB.
- **Login Flow:** Citizen sends email and password $\to$ backend checks BCrypt hash $\to$ generates signed JWT token.
- **What is JWT?** JSON Web Token containing user ID, email, role, and expiration (24h), digitally signed with HMAC-SHA256 secret.
- **What happens when user logs in?** Backend verifies credentials, issues JWT token to frontend. Frontend stores token and sends it in `Authorization: Bearer <token>` header for all future requests.
- **Spring Security Filter Chain:** Every incoming HTTP request passes through `JwtAuthenticationFilter`. If token is valid, user is authenticated into `SecurityContextHolder`; if missing/invalid on protected routes, request is rejected with HTTP 401 Unauthorized.
- **Roles:** Role-based access control (`ROLE_CITIZEN`) configured via Spring Security.

---

## 8. APIs – What Team Members Should Know

- **What is a REST API?** A standardized HTTP interface where frontend sends JSON/Multipart requests to backend URLs and receives structured JSON responses.
- **Request vs Response:** Request = HTTP method + URL + headers + payload sent by client; Response = HTTP status code + JSON data returned by server.
- **Key Backend Endpoints:**
  | Method | Endpoint | Purpose | Request Body / Param | Auth Required |
  | :--- | :--- | :--- | :--- | :--- |
  | `POST` | `/api/otp/send` | Sends 6-digit phone OTP | `{"phoneNumber": "..."}` | No |
  | `POST` | `/api/otp/verify` | Verifies phone OTP code | `{"phoneNumber": "...", "otp": "..."}` | No |
  | `POST` | `/api/identity/upload-document` | Uploads ID card for OCR | Multipart file (`document`) | No |
  | `POST` | `/api/auth/login` | Citizen login | `{"email": "...", "password": "..."}` | No |
  | `POST` | `/api/complaints` | Submits new complaint | Multipart: photo + title, desc, lat, lng | **Yes** (JWT) |
  | `GET` | `/api/complaints/my` | Fetches logged-in user's complaints | None | **Yes** (JWT) |
  | `GET` | `/api/complaints/community` | Public community complaints feed | Query params: status, category, page | No |
  | `POST` | `/api/complaints/{id}/support` | Upvotes community complaint | Complaint ID path variable | **Yes** (JWT) |
  | `POST` | `/api/complaints/{id}/comments` | Adds comment on complaint | `{"comment": "..."}` | **Yes** (JWT) |

---

## 9. External Services / Integrations

| Service Name                | Purpose                       | What CivicSense Sends                              | What CivicSense Receives                                   | Why It Is Used                                                         |
| :-------------------------- | :---------------------------- | :------------------------------------------------- | :--------------------------------------------------------- | :--------------------------------------------------------------------- |
| **Google Gemini 3.6 Flash** | Multimodal AI Reasoning       | Complaint image URL + title + description + schema | 14 structured JSON fields (severity, category, tags)       | Automates complaint classification and visual damage assessment        |
| **Python AI Microservice**  | Vector Similarity Computation | Image bytes, titles, descriptions, coordinates     | Cosine similarity scores for text, CLIP image, and address | Provides fast mathematical embeddings for duplicate detection          |
| **ImageKit**                | Media CDN & Storage           | Raw uploaded image bytes via official Java SDK     | Secure hosted CDN URL, file ID, thumbnail link             | Avoids storing heavy image blobs in relational database                |
| **Geoapify Places API**     | Geospatial Risk Intelligence  | Latitude, longitude, radius (500m), POI categories | Nearby schools, hospitals, transit hubs count & details    | Accurately factors sensitive public infrastructure into priority score |
| **Tesseract OCR (Tess4J)**  | ID Document Text Extraction   | Uploaded Aadhaar/Voter ID card image file          | Extracted raw text, citizen name, document number          | Verifies real identity during onboarding without manual human review   |

---

## 10. "What Makes CivicSense Different?"

- **Core Truth:** _"CivicSense does not physically repair a pothole or collect garbage. It improves the complaint-management and decision-support workflow so that authorities can process large volumes of civic complaints more efficiently."_
- **Traditional Portals vs CivicSense:**
  - _Traditional Portals:_ Passive forms where citizens submit complaints that sit in unorganized queues; duplicate complaints create duplicate work orders; priority is strictly first-come-first-served.
  - _CivicSense:_
    1. **AI Visual Understanding:** Validates whether the image really depicts civic damage and categorizes it automatically.
    2. **Intelligent Duplicate Clustering:** Groups 50 reports of the same burst pipe into one single master cluster.
    3. **Objective Risk & Priority Scoring:** Prioritizes issues based on objective hazard severity and proximity to schools/hospitals, not whoever complains loudest.
    4. **Community Corroboration:** Upvoting and comments measure collective community impact without clogging the queue with duplicate tickets.

---

## 11. Tough Teacher Questions & Viva Answers

1. **What problem are you solving?**  
   _Answer:_ Municipal corporations face thousands of unstructured, duplicate, and unverified complaints daily. We automate intake, deduplication, and priority scoring using AI.
2. **Why is this project needed?**  
   _Answer:_ Manual complaint sorting causes slow response times, wasted municipal resources on duplicate inspections, and delayed action on dangerous hazards.
3. **Existing complaint portals (like CPGRAMS or Swachhata) exist. What is different?**  
   _Answer:_ Existing portals are passive forms. CivicSense actively clusters duplicate reports, uses multimodal AI to verify damage, and computes objective risk scores based on nearby infrastructure.
4. **Why use AI in a civic complaint system?**  
   _Answer:_ AI standardizes inconsistent citizen descriptions, assesses visual hazard severity objectively, and compares photos to eliminate duplicate entries.
5. **Why did you choose Spring Boot?**  
   _Answer:_ Spring Boot provides enterprise-grade stability, robust transaction management, strong security filters, and easy integration with relational databases.
6. **Why use a separate Python microservice for AI instead of Java?**  
   _Answer:_ Specialized ML libraries like HuggingFace Sentence-Transformers and OpenAI CLIP are native and optimized in Python. Spring Boot handles business logic and delegates vector math to Python.
7. **How does duplicate detection work?**  
   _Answer:_ Two stages: Stage 1 filters nearby complaints within ~550m and computes text/image embedding similarity. If score $\ge 0.60$, Stage 2 asks Gemini AI to visually confirm if both photos show the same issue.
8. **What is a complaint cluster?**  
   _Answer:_ A database entity (`ComplaintCluster`) that groups multiple citizen complaints about the exact same physical issue under one primary master record.
9. **What is corroboration?**  
   _Answer:_ Measuring how many independent citizens confirm an issue via duplicate reports or community upvotes, proving the problem is real and impactful.
10. **How is priority/risk calculated?**  
    _Answer:_ A 4-factor formula: `(Severity × 35%) + (Corroboration × 25%) + (Location × 20%) + (Infrastructure × 20%)`. Scores $\ge 75$ are marked `CRITICAL`.
11. **Why do you use location in risk calculation?**  
    _Answer:_ A pothole in a deserted alley has lower risk than an open pothole outside a school or hospital entrance. Proximity to sensitive POIs increases priority.
12. **What if the AI gives a wrong result?**  
    _Answer:_ System includes safe fallbacks: if AI classification fails, category defaults to `OTHER` and severity to 0.5. Authority administrators can manually override AI fields.
13. **What if duplicate detection makes a mistake?**  
    _Answer:_ The two-stage architecture minimizes errors: Stage 1 uses mathematical embeddings, and Stage 2 uses Gemini vision. If uncertain, it creates a new cluster rather than falsely merging.
14. **What if the Gemini AI or Python service goes down?**  
    _Answer:_ Handled with fallback try-catch logic in services. Complaint creation is not lost; it falls back to default category and unclustered state.
15. **What if the database fails?**  
    _Answer:_ Spring `@Transactional` ensures ACID compliance. If database write fails, the entire transaction rolls back cleanly without partial data corruption.
16. **How do you handle fake or spam complaints?**  
    _Answer:_ Mandatory phone OTP authentication, citizen ID OCR verification, and Gemini visual inspection verify that images actually show civic damage.
17. **What about citizen privacy?**  
    _Answer:_ Passwords and OTPs are BCrypt-hashed; JWT tokens are signed; raw ID document images are kept private and not exposed in public community feeds.
18. **What happens when complaint volume becomes very large?**  
    _Answer:_ Geographic bounding box ($\pm 0.005^\circ$) restricts duplicate queries so the system never scans the entire database; public feeds use pagination.
19. **How does authentication work?**  
    _Answer:_ Stateless JWT authentication. User logs in with email/password, receives a signed 24h JWT, and sends it in the `Authorization` header for protected endpoints.
20. **Why use JWT instead of traditional server sessions?**  
    _Answer:_ JWT is stateless, scalable, and does not consume server memory storing session state across multiple backend instances.
21. **How does the frontend communicate with backend?**  
    _Answer:_ Over HTTP/HTTPS using REST APIs, sending JSON bodies or Multipart form data, receiving structured JSON responses.
22. **How can the system be scaled in the future?**  
    _Answer:_ Spring Boot backend can be horizontally scaled behind a load balancer; PostgreSQL can use read-replicas; Python AI can run on GPU workers.
23. **What is currently completed in the backend?**  
    _Answer:_ Full M1 auth & OCR, M2 intake, M3 Gemini analysis, M4 duplicate clustering with Python AI, M5 risk scoring with Geoapify, and M6 community feed/comments.
24. **What is still planned or future work?**  
    _Answer:_ Production SMS gateway (currently simulated via console), municipal authority dispatch dashboard, and automated worker assignment.
25. **What are the limitations of the current implementation?**  
    _Answer:_ Synchronous external API calls during intake add latency; OCR depends on clear image quality; real SMS delivery requires commercial gateway setup.

---

## 12. Current Status vs Future Work

### Fully Implemented & Documented

- Citizen registration, login, and stateless JWT filter chain.
- 6-digit phone OTP generation and BCrypt hash verification.
- Local Tesseract OCR extraction for identity cards.
- Multipart complaint intake and ImageKit cloud image upload.
- Google Gemini 3.6 Flash multimodal analysis with 14 structured fields.
- Fallback AI categorization for error resilience.
- Two-stage duplicate detection (Python AI embeddings + Gemini visual confirmation).
- Spatial bounding box candidate search ($\pm 0.005^{\circ}$).
- Composite 4-factor Risk and Priority Engine with Geoapify POI integration.
- Public community feed, upvote corroboration tracking, and threaded comments.
- Global exception handling and 66 automated unit/integration tests passing.

### In Progress / Partially Implemented / Stubs

- _SMS Gateway:_ OTP logic is fully implemented, but OTP codes are logged to console/simulated rather than dispatched via external commercial SMS API (e.g. Twilio).
- _Mock OCR Service:_ Exists as an unannotated fallback class; active extraction uses Tess4J.

### Future / Planned Work

- Municipal authority dispatching and worker assignment workflows.
- Real-time citizen push notifications (WebSocket or Firebase).
- Asynchronous task queues (RabbitMQ/Kafka) for AI analysis to reduce intake HTTP latency.
- Dedicated administrative analytics dashboard for city commissioners.

---

## 13. Important Terms – One-Line Definitions

- **REST API:** Standard HTTP communication style where clients interact with server resources using GET, POST, PUT, DELETE.
- **Controller:** Spring Boot class that listens to HTTP endpoints, parses incoming requests, and returns responses.
- **Service:** Layer containing core business logic, formulas, validation rules, and external integrations.
- **Repository:** Interface that executes database read and write queries using Spring Data JPA.
- **Entity:** Java class directly mapped to a relational database table using JPA annotations (`@Entity`, `@Table`).
- **DTO (Data Transfer Object):** Simple Java object used to transport data between frontend requests and backend controllers.
- **JWT (JSON Web Token):** Compact, URL-safe security token signed cryptographically to authenticate users statelessly.
- **Authentication vs Authorization:** Authentication proves _who you are_; Authorization verifies _what you are allowed to do_.
- **OCR (Optical Character Recognition):** Technology that converts printed text from ID card images into machine-readable digital text.
- **AI Classification:** Machine learning process of categorizing raw images and descriptions into defined civic issue types.
- **Duplicate Detection:** Checking whether a new complaint reports a problem that has already been registered nearby.
- **Complaint Cluster:** A group of multiple citizen complaint records linked to the same physical issue.
- **Corroboration:** Multi-citizen confirmation of an issue through duplicate reports or community upvotes.
- **Risk Score:** Numerical metric (0–100) combining hazard severity, public impact, and infrastructure sensitivity.
- **Geospatial Analysis:** Evaluating GPS coordinates and nearby physical amenities (schools, hospitals) using spatial APIs.
- **Escalation:** Raising the priority or urgency of an unresolved civic issue over time.

---

## 14. Final 10 Things to Remember Before Viva

1. **The Core Pitch:** CivicSense is an intelligent decision-support system that turns messy citizen complaint photos into verified, deduplicated, and prioritized action tickets.
2. **We Don't Fix Potholes:** CivicSense optimizes the municipal workflow; it gives authorities actionable intelligence to fix issues faster.
3. **Architecture Stack:** React (Frontend) $\to$ Spring Boot (Backend) $\to$ PostgreSQL (DB) + Gemini (Vision AI) + Python (Embeddings) + ImageKit (Storage).
4. **M1 (Auth):** Phone OTP + ID Card OCR (Tesseract) $\to$ BCrypt password hash $\to$ 24-hour signed JWT token.
5. **M2 (Intake):** Photos go to ImageKit CDN; PostgreSQL only stores CDN image URLs, never raw binary files.
6. **M3 (Gemini AI):** Gemini 3.6 Flash multimodal analyzes photo + text to extract category, severity (0–1), and 14 structured attributes.
7. **M4 (Deduplication):** 2 stages: Stage 1 = 550m bounding box + Python vector embeddings; Stage 2 = Gemini visual verification.
8. **M5 (Priority Formula):** Priority = 35% Severity + 25% Corroboration + 20% Location + 20% Infrastructure (Geoapify POIs).
9. **M6 (Community):** Citizens can upvote ("I'm affected too") and comment; comments do NOT create duplicate complaints.
10. **Honest Status:** Core AI, duplicate detection, risk engine, and community feeds are 100% built and tested; authority dispatching and live SMS gateway are future milestones.
