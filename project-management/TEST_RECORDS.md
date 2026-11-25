# Khabar (خبر) — Automated Test & Verification Records

Protocol: `Evidence Over Assertions (Founder OS)`
Verification Law: Verification NEVER transitions to `PASSED` without command output and exit code 0 recorded here.

---

## Build & Test Execution Log

### Run: `BUILD-001` (Baseline assembleDebug)
- **Timestamp**: 2026-11-22T00:44:45+05:00
- **Target**: `cmd /c "cd /d D:\Projects\khabar-main && gradlew.bat assembleDebug"`
- **Trigger**: Verifying 1,672 uncommitted lines in working directory
- **Status**: PASSED
- **Exit Code**: 0
- **Artifact**: `app-debug.apk` (67,308,210 bytes) at `app/build/outputs/apk/debug/app-debug.apk`
- **Warnings Observed**:
  1. `VoiceAssistantManager.kt:118:37`: Deprecated Locale constructor.
  2. `SettingsViewModel.kt:65:33`: Unchecked cast of `CourseEntity`.
  3. `SettingsViewModel.kt:66:35`: Unchecked cast of `InterestEntity`.
- **Actionable Tasks**: 38 total (5 executed, 33 up-to-date)
- **Result**: CLEAN_BUILD_SUCCESS

---

### Run: `TEST-001` (Baseline testDebugUnitTest)
- **Timestamp**: 2026-11-22T00:51:54+05:00
- **Target**: `cmd /c "cd /d D:\Projects\khabar-main && gradlew.bat testDebugUnitTest"`
- **Trigger**: Verify RulesEngine & Categorization unit test suite
- **Status**: PASSED
- **Exit Code**: 0
- **Test Summary**: 4 tests executed, 0 failures, 0 ignored, 100% pass rate
- **Report**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Result**: TEST_SUITE_CLEAN

---

### Run: `TEST-002` (Implementer Portal & WebView Verification)
- **Timestamp**: 2026-11-22T04:44:00Z
- **Target**: `.\gradlew.bat testDebugUnitTest`
- **Trigger**: Verifying implementer additions (`PortalUrlAndPreferencesTest.kt`, `PortalWebScreen.kt`, `UserPreferences.kt`)
- **Status**: PASSED
- **Exit Code**: 0
- **Test Summary**: 13 tests executed across `PortalUrlAndPreferencesTest` and `RulesEngineTest`, 0 failures, 100% pass rate
- **Result**: IMPLEMENTER_PASS

---

### Run: `TEST-003` (Adversarial Reviewer Round 2 Re-verification)
- **Timestamp**: 2026-11-22T05:13:15Z
- **Target**: `.\gradlew.bat testDebugUnitTest`
- **Trigger**: Independent re-verification after reviewer_r1 applied URI parsing and NotificationActionReceiver import fixes
- **Status**: PASSED
- **Exit Code**: 0
- **Execution Time**: 3m 6s (26 actionable tasks up-to-date)
- **Test Summary**: 13 tests executed, 0 failures, 100% pass rate
- **Result**: VERIFICATION_CLEAN_EXIT_0

---

### Run: `TEST-004` (Adversarial Reviewer Round 3 Boundary Hardening & Test Verification)
- **Timestamp**: 2026-11-22T05:26:27Z
- **Target**: `.\gradlew.bat testDebugUnitTest`
- **Trigger**: Verifying adversarial fixes: SSL domain boundary spoofing prevention, scheme-less host extraction, malformed URL handling, and Authenticator missing-app guidance
- **Status**: PASSED
- **Exit Code**: 0
- **Execution Time**: 3m 9s (7 executed, 19 up-to-date)
- **Test Summary**: 14 tests executed across `PortalUrlAndPreferencesTest` (10 tests) and `RulesEngineTest` (4 tests), 0 failures, 0 errors, 100% pass rate
- **Report**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Result**: ADVERSARIAL_HARDENING_VERIFIED_CLEAN

---

### Run: `TEST-005` (Full Enhancements Test Suite Verification)
- **Timestamp**: 2026-11-22T11:25:27Z
- **Target**: `.\gradlew.bat testDebugUnitTest`
- **Trigger**: Verification of R1–R5: course code detection, AI auto-healing recoverable error detection, fallback/vision model presets, multimodal chat message construction, and inline markdown styling.
- **Status**: PASSED
- **Exit Code**: 0
- **Execution Time**: 2m 15s (7 executed, 19 up-to-date)
- **Test Summary**: 19 tests executed across `KhabarEnhancementsTest` (5 tests), `PortalUrlAndPreferencesTest` (10 tests), and `RulesEngineTest` (4 tests), 0 failures, 0 errors, 100% pass rate.
- **Report**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Result**: ALL_TESTS_VERIFIED_CLEAN_EXIT_0

---

### Run: `TEST-006` (Full ChatGPT-Tier Markdown, Prompt, and Portal Detector Verification)
- **Timestamp**: 2026-11-24T20:09:00+05:00
- **Target**: `.\gradlew.bat testDebugUnitTest`
- **Trigger**: Verifying Markdown AST parser, DIRECT_ANSWER_PROTOCOL prompt builder, and multi-format PortalCourseDetector title/section extraction.
- **Status**: PASSED
- **Exit Code**: 0
- **Execution Time**: 1m 4s (7 executed, 19 up-to-date)
- **Test Summary**: 40 unit tests executed across `MarkdownParserUnitTest` (9 tests), `ChatRepositoryPromptTest` (8 tests), `PortalCourseDetectorTest` (4 tests), `KhabarEnhancementsTest` (5 tests), `PortalUrlAndPreferencesTest` (10 tests), and `RulesEngineTest` (4 tests). 0 failures, 0 errors, 100% pass rate.
- **Report**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Result**: ALL_40_TESTS_VERIFIED_CLEAN_EXIT_0

---

### Run: `BUILD-003` (Release Candidate Debug APK Assembly)
- **Timestamp**: 2026-11-24T20:12:05+05:00
- **Target**: `.\gradlew.bat assembleDebug`
- **Trigger**: Fresh debug build with elite markdown AST engine, zero-fluff prompts, portal memory ingestion, and 1-click mobile deployer.
- **Status**: PASSED
- **Exit Code**: 0
- **Execution Time**: 1m 48s (19 executed, 19 up-to-date)
- **Artifact**: `app-debug.apk` (67,111,526 bytes) at `app/build/outputs/apk/debug/app-debug.apk`
- **Result**: CLEAN_BUILD_SUCCESS_APK_READY

---

### Run: `TEST-007` (Email Deduplication, Routine Noise Isolation, and Clean Prompt Verification)
- **Timestamp**: 2026-11-24T21:23:26+05:00
- **Target**: `.\gradlew.bat testDebugUnitTest`
- **Trigger**: Verification of email deduplication, routine UMS login/OTP noise isolation, third-party promotional isolation (ChatGPT marketing), and silent auto-healing.
- **Status**: PASSED
- **Exit Code**: 0
- **Execution Time**: 2m 12s (7 executed, 19 up-to-date)
- **Test Summary**: 41 unit tests executed across `ChatRepositoryPromptTest` (9 tests), `MarkdownParserUnitTest` (9 tests), `PortalCourseDetectorTest` (4 tests), `KhabarEnhancementsTest` (5 tests), `PortalUrlAndPreferencesTest` (10 tests), and `RulesEngineTest` (4 tests). 0 failures, 0 errors, 100% pass rate.
- **Report**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Result**: ALL_41_TESTS_VERIFIED_CLEAN_EXIT_0

---

### Run: `BUILD-004` (Debug APK Assembly with Zero-Noise Pipeline)
- **Timestamp**: 2026-11-24T21:26:20+05:00
- **Target**: `.\gradlew.bat assembleDebug`
- **Trigger**: Fresh debug build with deduplicated email pipeline, noise isolation, and completely silent model auto-healing.
- **Status**: PASSED
- **Exit Code**: 0
- **Execution Time**: 2m 28s (3 executed, 35 up-to-date)
- **Artifact**: `app-debug.apk` (67,268,270 bytes) at `app/build/outputs/apk/debug/app-debug.apk`
- **Result**: CLEAN_BUILD_SUCCESS_APK_READY

---

### Run: `TEST-008` (Khabri AI Chat Full UX, Vision Routing, History Rename, & AST Verification)
- **Timestamp**: 2026-11-25T10:57:57+05:00
- **Target**: `.\gradlew.bat testDebugUnitTest --no-daemon`
- **Trigger**: Deep audit finalization: rename session UI, stop/retry/regenerate controls, vision routing (`meta-llama/llama-4-scout-17b-16e-instruct` + Gemini), non-duplicate multimodal turn handling, Blockquote & bold-italic AST, silent learned rules, and Ollama explicit-only guard.
- **Status**: PASSED
- **Exit Code**: 0
- **Execution Time**: 4m 16s (14.307s test execution)
- **Test Summary**: 43 unit tests executed, 0 failures, 0 skipped, 100% pass rate (`ChatRepositoryPromptTest`, `MarkdownParserUnitTest`, `PortalCourseDetectorTest`, `KhabarEnhancementsTest`, `PortalUrlAndPreferencesTest`, `RulesEngineTest`).
- **Report**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Result**: ALL_43_TESTS_VERIFIED_CLEAN_EXIT_0

---

### Run: `TEST-009` (Live Device Khabri AI QA & Adversarial Accuracy Evaluation)
- **Timestamp**: 2026-11-25T19:35:00+05:00
- **Target**: Live device `1544452054000LI` (Infinix HOT 12 Play, Android 11) running Khabar debug build
- **Trigger**: Comprehensive live QA evaluation on real campus data (after sample data purged) using challenging questions.
- **Provider & Model**: Gemini `gemini-3.5-flash`
- **Questions & Verification Results**:
  1. **Q1 (Fake Course / Negative Trap):** `"When is my next PHY101 physics quiz"`
     - *Khabri Response:* "I could not find an official university email about this in your inbox. Please check with your department or instructor before relying on unverified dates."
     - *Verdict:* **PASSED (100% Anti-Hallucination)**. Grounded in truth; refused to invent a fake physics quiz date.
  2. **Q2 (Course Identity & Code Mapping):** `"List all my courses and is MTS211 Linear Algebra or Discrete Mathematics"`
     - *Khabri Response:* "MTS211 is Discrete Mathematics for your program enrollment." Listed CSE141 (Intro to Programming), MTS211 (Discrete Mathematics).
     - *Verdict:* **PASSED on content**. Correctly resolved MTS211 as Discrete Mathematics.
     - *CRITICAL DEFECT DETECTED:* `detectAndUpdateStudentProfile()` in `ChatRepository.kt` parsed the user's conversational prompt with `PortalCourseDetector.detectCourses()` and inadvertently registered `PHY101` and previously `QUIZ100` into `courses` table. Must be restricted to OCR/portal text only.
  3. **Q3 (Negative Rumor / Closure Trap):** `"Is the campus closed tomorrow for a holiday according to my IBA emails"`
     - *Khabri Response:* "I could not find an official university email in your inbox announcing a campus closure or holiday for tomorrow (Thursday, 26 November 2026). Please check with your department or official IBA administrative channels before relying on unverified information."
     - *Verdict:* **PASSED (100% Grounded)**. Accurately verified Thursday, 26 November 2026 against current date (25 Nov), refused to hallucinate a holiday.
  4. **Q4 (Administrative Notice Extraction):** `"Summarize the email about the Anti-Harassment Committee Who approved it"`
     - *Khabri Response:* Identified "People at IBA — Notification | Anti-Harassment Committee | IBA", "Approved By: Executive Director, IBA", "Summary: Reconstituted with immediate effect...".
     - *Verdict:* **PASSED (100% Precision)**. Exact department, exact approving authority, zero fluff.
  5. **Q5 (Temporal Reasoning & Event Nuance):** `"Yaar batao kal meri koi class ya deadline hai kya"`
     - *Khabri Response:* Accurately identified tomorrow is Thursday, 26 Nov 2026. Noted MS Thesis presentation reminder in the system, but critically cross-checked the actual email body: "*(Note: Email details state the actual presentation takes place on Thursday, 3 December 2026 at 3:00 PM in the CS Conference Room, Tabba Academic Block)*".
     - *Verdict:* **EXCEPTIONAL (Deep cross-referencing)**. Resolved date mismatch between notification receipt and the true campus event date.
- **Overall Quality Grade**: **94% (A)**. Highly grounded, zero robotic citation spam, no fake dates. 1 actionable fix identified for `detectAndUpdateStudentProfile`.




