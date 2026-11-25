# Khabar (خبر) 📬

> **"Only the mails that matter to you."**

Khabar is an on-device Android mail assistant built with **Jetpack Compose** and **Material 3 (Apple-inspired design language)** for university students (configured for IBA Karachi, but fully generic and configurable via JSON). It solves academic email and notification overload by reading emails, identifying whether each notice is relevant to the student, categorizing them into priority buckets, synthesizing category-specific notification alert tones, and generating instant summaries in **English, Roman Urdu, and Urdu**.

---

## 🎯 The Problem

University students receive dozens of emails daily from administrations, teachers, and student societies. Most notices are irrelevant (intended for other programs, batches, or courses), while critical updates—such as cancelled classes, quiz announcements, assignment deadlines, registration windows, and fee dues—get buried.

---

## ✨ Features

- **Personalized Onboarding:** Captures program, intake semester, join year, campus, enrolled courses (with ML Kit OCR screenshot scan), and society interests.
- **Rules & Categorization Engine:**
  - Class changes (cancelled, rescheduled, room shift)
  - Exams & quizzes
  - Deadlines & assignments
  - Fees & scholarships
  - Course registration & add/drop
  - Results & grades
  - Attendance warnings
  - Internships & career opportunities
  - Events & societies
  - General notices
- **3 Intelligent Buckets:**
  - **Important (Zaroori):** Urgent, action-oriented, course-specific or broadcast announcements.
  - **Good to Know (Jaanna Acha):** Society updates, campus-wide events matching student interests.
  - **Hidden (Chhupa):** Mails for other programs, batches, or unselected interests.
- **Multilingual Summarizer & Khabri AI (خبری):**
  - AI-powered summaries and deep multi-turn chat assistant via any OpenAI-compatible provider (Groq, Gemini, OpenRouter) in **English, Roman Urdu, or Urdu**.
  - **Zero-Fluff Direct Answer Protocol:** Eliminates AI conversational pleasantries and filler, formatting schedules and announcements into clean, mobile-optimized key-value cards.
  - **Native Markdown AST Parser:** Custom recursive descent parser supporting headings, bold-italic typography, blockquotes, inline chips, and code blocks without layout shifts.
  - **On-Device Offline Fallback:** Campus memory template-based summarizer requiring zero internet or API keys.
- **Authoritative Portals & Sakai LMS 2FA WebSync:**
  - Embedded secure WebView synchronization for university portals (IBA Sakai LMS, UMS) with Microsoft Authenticator 2FA support.
  - **Recursive Multi-Frame / Iframe Scraper (`PortalJsExtractor`):** Recursively penetrates child iframes, Sakai portlet windows, tab aria-labels, site navigation menus, and PeopleSoft enrollment tables.
  - **Continuous Background Auto-Sync:** Zero-tap persistence; automatically detects student full name, program, ERP ID, and enrolled course codes with sections and timings while browsing the portal.
  - **Authoritative Hierarchy & Deduplication:** LMS is treated as the official authority over manual input; auto-resolves section mismatches, normalizes canonical course codes (`UPPER(REPLACE(code, ' ', ''))`), and removes duplicate course rows.
  - **Timetable Clash Detection:** Algorithmic verification that detects overlapping class time slots across enrolled courses.
  - **1-Tap "Done" Action:** Prominent top-bar synchronization button that commits all page changes, verifies differences, and returns to the app dashboard with settings pre-populated.
- **24/7 Time Flexibility & Alarms:**
  - Custom clock dial (12-hour AM/PM TimePickerDialog) for daily briefing delivery with independent enable/disable toggles.
  - Flexible deadline alarm lead-time picker with custom minutes and hours.
  - Battery-friendly AlarmManager registration that cleanly cancels alarms when toggled off.
- **Instant Mode (Notification Listener):** Zero-setup mode using Android's `NotificationListenerService` to immediately capture and parse incoming Outlook and Gmail notifications without requiring OAuth credentials.
- **Synthesized Alert Sounds:**
  - 16-bit 44.1 kHz PCM sine audio synthesizer with smoothed attack and decay curves.
  - Unique melodic signatures for every category (e.g., descending tones for class cancellations, double pulse for exams, major triad for results).
- **Actionable Utility & Notifications:**
  - Category-specific primary actions: *Add to Calendar*, *Remind Me*, *Set Alarm*, *Share*, *Open Link / Portal*.
  - Quiet-during-class: silences non-urgent alerts during scheduled class periods.
  - Daily morning digest at 8:00 AM summarizing pending items.
- **Privacy First:**
  - No analytics or tracking. All data stays on the device. The only optional network call is to the AI provider you configure, which receives the email text to summarize.
  - All email processing and data storage remain exclusively on the student's device in local Room database and DataStore.

---

## 🎨 Visual Design

- Calm, premium, Apple-like design language.
- **Light theme:** `#F2F2F7` background, `#FFFFFF` cards, `#000000` text, `#6C6C70` secondary text, `#C6C6C8` hairline dividers.
- **Dark theme:** `#000000` background, `#1C1C1E` cards, `#8E8E93` secondary text.
- Unified `#007AFF` / `#0A84FF` accent color.
- 10 category colors reserved strictly for 40dp rounded-square icon tiles.
- Reusable iOS-styled components: `LargeTitleScaffold`, `SegmentedControl`, `FloatingTabBar`, `Pill`, `CountdownRing`, `GroupedList`, `PrimaryButton`, `SecondaryButton`.

---

## 🛠️ Tech Stack & Architecture

- **Platform:** Native Android (Kotlin 2.0+, Min SDK 26, Target SDK 36)
- **UI Framework:** Jetpack Compose, Material 3
- **Local Database:** Room Database with KSP, Kotlin Coroutines, StateFlow
- **Background Tasks:** WorkManager for periodic background syncing, AlarmManager for exact deadline reminders
- **Machine Learning:** ML Kit On-Device Text Recognition for course screenshot import
- **Networking:** OkHttp, kotlinx.serialization.json
- **Build Optimization:** R8 Full-Mode Minification & Resource Shrinking with custom ProGuard rules

---

## ⚙️ Build Configuration Slots

The app builds and runs fully with all slots empty. When a slot is not configured, tapping the corresponding feature in the UI displays a helpful explanation rather than crashing.

To configure production keys, add them to `local.properties`:

```properties
sdk.dir=/path/to/android/sdk

# Microsoft Graph / MSAL (Optional - Delegated scopes: Mail.Read, User.Read, offline_access)
MS_CLIENT_ID=
MS_SIGNATURE_HASH=

# Google Play Services Gmail API (Optional)
GMAIL_ENABLED=false
GMAIL_CLIENT_ID=

# AI Summaries (Optional - Groq, Gemini, OpenRouter, Ollama)
AI_BASE_URL=https://api.groq.com/openai/v1
AI_MODEL=llama-3.3-70b-versatile
AI_API_KEY=
```

Keys can also be configured dynamically within the app under **Settings > AI Summaries**, which securely encrypts API keys into the **Android Keystore** using AES-GCM.

---

## 🚀 Getting Started & Building

### 1. Prerequisites
- Android Studio Ladybug or newer
- JDK 17, 21, or 25
- Android SDK Platform 36 (build-tools 36.0.0)

### 2. Configuration (`local.properties`)
Copy `local.properties.example` to `local.properties` and fill in optional keys:
```properties
sdk.dir=/path/to/android/sdk

# Microsoft Graph / MSAL (Optional)
MS_CLIENT_ID=
MS_SIGNATURE_HASH=

# Gmail API (Optional)
GMAIL_ENABLED=false
GMAIL_CLIENT_ID=

# AI Summaries (Optional)
AI_BASE_URL=https://api.groq.com/openai/v1
AI_MODEL=llama-3.3-70b-versatile
AI_API_KEY=your_api_key_here
```

### 3. Build & Run
```bash
# Run Unit Tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

---

## 🧪 Developer & Diagnostic Tools

To test and diagnose background automation and notification pipelines:
1. Open **Settings > Developer Tools**.
2. Tap **Purge Any Demo / Sample Emails & Reminders** to wipe test artifacts and ensure a clean production mailbox.
3. Tap any **Simulate Incoming Mail** category to trigger live rules processing, synthesized PCM alert tones, and system notifications.
4. Tap **Trigger Daily Digest** to immediately preview the morning summary notification.
5. Inspect the **Notification Inspector** to view raw extras captured by the Notification Listener service.

---

## 📄 License
This project is open-source and intended for academic personal productivity.
