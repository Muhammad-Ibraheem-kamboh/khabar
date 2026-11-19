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
- **Multilingual Summarizer:**
  - AI-powered summaries via any OpenAI-compatible provider (Groq, Gemini, OpenRouter) in **English, Roman Urdu, or Urdu**.
  - On-device offline template-based summarizer fallback with zero network requirements.
  - App chrome remains in clean, simple English.
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

## 🧪 Developer & Demo Tools

To test the complete workflow without connecting real email accounts:
1. Open **Settings > Developer Tools**.
2. Tap **Load Sample Profile** (loads Ali Raza, BSCS Fall 23, enrolled in CS201, CS204, MTH201, SS102).
3. Tap **Load All 30 Realistic Sample Mails** (populates the inbox with categorized emails).
4. Tap any **Simulate Incoming Mail** category to trigger live rules processing and notifications.
5. Tap **Trigger Daily Digest** to preview the morning summary notification.
6. Inspect the **Notification Inspector** to view raw extras captured by the Notification Listener service.

---

## 📄 License
This project is open-source and intended for academic personal productivity.
