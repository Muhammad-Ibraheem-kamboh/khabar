# Khabar (خبر) — 3D Project State Matrix

Protocol: `Founder OS v2.0`
State Tuple: `(Execution, Verification, Approval)`

Allowed Values:
- Execution: `PLANNED` | `IN_PROGRESS` | `DONE` | `BLOCKED`
- Verification: `NOT_RUN` | `PASSED` | `FAILED`
- Approval: `NOT_REQUIRED` | `PENDING` | `APPROVED` | `REJECTED`

---

## Active Tasks (Milestone 1: Baseline Build & Integrity Verification)

| Task ID | Description | Owner | State | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `KHABAR-001` | Gradle Build & Compiler Diagnostic Baseline | `qa-verifier` | `(DONE, PASSED, NOT_REQUIRED)` | Verified 1,672 uncommitted lines; assembleDebug Exit Code 0 |
| `KHABAR-002` | Unit Test Verification (`testDebugUnitTest`) | `qa-verifier` | `(DONE, PASSED, NOT_REQUIRED)` | 4 unit tests passed, 0 failures, Exit Code 0 |
| `KHABAR-003` | Room Database Schema & Migration Verification | `security-auditor` | `(PLANNED, NOT_RUN, NOT_REQUIRED)` | Verify `khabar.db` version bump & entities |
| `KHABAR-004` | APK Debug Assembly & Signing Verification | `qa-verifier` | `(DONE, PASSED, PENDING)` | `app-debug.apk` (67.3 MB) ready for founder verification |

---

## Active Tasks (Milestone 2: University Portal Web Viewer & IBA Karachi Authentication)

| Task ID | Description | Owner | State | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `PORTAL-001` | Fix Portal URLs & Quick Selector (UMS, LMS, Custom) | `self` | `(DONE, PASSED, NOT_REQUIRED)` | Replaced dead `portal.iba.edu.pk` with `ums.iba.edu.pk` & `lms.iba.edu.pk`; added `migrateInvalidPortalUrls()` |
| `PORTAL-002` | WebView SSO, Azure AD Authenticator 2FA & SSL Handshake | `self` | `(DONE, PASSED, NOT_REQUIRED)` | Added `<queries>` in AndroidManifest.xml; institutional SSL trust allowlist; cookie persistence |
| `PORTAL-003` | Memory Extraction to Khabri & Error State UI Overlay | `self` | `(DONE, PASSED, NOT_REQUIRED)` | In-app error UI with Retry & Edit URL; 1-tap "Save to Memory" & "Ask Khabri" via `PortalContentParser` |
| `PORTAL-004` | Adversarial Unit Tests & JVM Decoupling | `qa-verifier` | `(DONE, PASSED, NOT_REQUIRED)` | 10 tests in `PortalUrlAndPreferencesTest` (14 total with RulesEngineTest), 100% pass rate; SSL spoofing & scheme-less parsing verified |

---

## Active Tasks (Milestone 3: Notification Sync, Auto-Healing AI, Course Scraping, Vision & ChatGPT Sessions)

| Task ID | Description | Owner | State | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `FEAT-001` | Real-Time Notification-to-Dashboard Sync | `self` | `(DONE, PASSED, NOT_REQUIRED)` | Emits incoming Outlook/Gmail IBA mails via `mailReceivedEvents` SharedFlow; TodayViewModel updates timestamp and live count; unread filter expanded to IMPORTANT + GOOD_TO_KNOW |
| `FEAT-002` | Zero-Hassle Silent AI Auto-Healing | `self` | `(DONE, PASSED, NOT_REQUIRED)` | `OpenAiCompatibleClient.isRecoverableModelError` detects 404/410/503/429/quota; `AiSummarizer` and `ChatRepository` transparently cycle fallback models and persist working model |
| `FEAT-003` | Portal Course & Announcement Auto-Ingestion | `self` | `(DONE, PASSED, NOT_REQUIRED)` | `PortalCourseDetector` extracts courses (e.g. MTS201, CSE312); `PortalWebScreen` shows 1-tap `CourseSyncConfirmationDialog` and syncs into `ProfileRepository.saveCourses` |
| `FEAT-004` | Multimodal Vision Query Support | `self` | `(DONE, PASSED, NOT_REQUIRED)` | Supports base64 JPEG attachments; serialized as multimodal `image_url` (OpenAI/Gemini/Groq) and `source: base64` (Claude); routes to vision models (e.g. gemini-flash-lite, llama-3.2-11b-vision) |
| `FEAT-005` | ChatGPT-Style Sessions, Renaming & Markdown Formatting | `self` | `(DONE, PASSED, APPROVED)` | Defaults to fresh chat on open; bottom sheet supports session rename/delete and switching; `FormattedChatMessage` provides paragraph spacing, bolding, bullet points, numbered lists, and dark code blocks |

---

## Active Tasks (Milestone 4: Elite ChatGPT/Claude-Tier Khabri AI Intelligence & Typography Engine)

| Task ID | Description | Owner | State | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `KHABRI-001` | Markdown AST Renderer & Typography Engine (`FormattedChatMessage.kt`) | `self` | `(DONE, PASSED, APPROVED)` | Multi-pass AST renderer converting raw syntax, pipe tables into native Compose cards, clean H1-H6 headers, bullet pills, and RTL/LTR Urdu/English alignment |
| `KHABRI-002` | Zero-Fluff System Prompt & Campus Intelligence (`AppConfig.kt`, `ChatRepository.kt`) | `self` | `(DONE, PASSED, APPROVED)` | `DIRECT_ANSWER_PROTOCOL`: eliminated conversational filler, direct answers first, structured campus context injection |
| `KHABRI-003` | Universal Fresh-Chat Navigation & Dynamic Greetings (`NavGraph.kt`, `KhabriChatScreen.kt`) | `self` | `(DONE, PASSED, APPROVED)` | Fresh UUID session on every navigation entry; blank canvas with rotating psychological campus greetings and quick query chips |
| `KHABRI-004` | Autonomous Portal Course & Announcement Scraper (`PortalWebScreen.kt`, `PortalCourseDetector.kt`) | `self` | `(DONE, PASSED, APPROVED)` | Automated JS bridge scraping enrolled courses, section designations, and announcements into Room DB |
| `KHABRI-005` | 1-Click Mobile Deploy & Launch Automation (`install_and_run.bat`) | `self` | `(DONE, PASSED, APPROVED)` | Automated batch launcher detecting USB/wireless devices, installing APK, and starting MainActivity |
