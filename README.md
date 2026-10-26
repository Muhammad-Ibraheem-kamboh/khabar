# Khabar (خبر) 📬

> **"Only the mails that matter to you."**

Khabar is a native Android application built with **Jetpack Compose** and **Material 3 (Apple-inspired design language)** for university students. It cuts through academic email overload by reading university emails, identifying whether each email is relevant to the student, categorizing them into priority buckets, and generating instant AI summaries in **English, Roman Urdu, and Urdu**.

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
- **3 Intelligent Buckets:**
  - **Important (Zaroori):** Urgent, action-oriented, course-specific or broadcast announcements.
  - **Good to Know (Jaanna Acha):** Society updates, campus-wide events matching interests.
  - **Hidden (Chhupa):** Mails for other programs, batches, or unselected interests.
- **Multilingual Summarizer:**
  - AI-powered summaries via any OpenAI-compatible provider (Groq, Gemini, OpenRouter) in **English, Roman Urdu, or Urdu**.
  - On-device offline summarizer fallback with zero network requirements.
- **Instant Mode (Notification Listener):** Zero-setup mode using Android's `NotificationListenerService` to immediately capture and parse incoming Outlook and Gmail notifications without requiring OAuth credentials.
- **Actionable Utility:**
  - "Add to Calendar" for deadlines and exam dates.
  - "Remind Me" exact alarm scheduling.
  - "Open Link" for direct LMS/portal navigation.
  - Share summary text to WhatsApp or messaging apps.
- **100% On-Device & Private:**
  - Zero external servers or tracking.
  - All email processing and data storage remain exclusively on the student's device in local Room database and DataStore.

---

## 🛠️ Tech Stack & Architecture

- **Platform:** Native Android (Kotlin 2.0+, Min SDK 26, Target SDK 36)
- **UI Framework:** Jetpack Compose, Material 3 with custom iOS-inspired grouped containers, Cupertino-style segment controls, and floating blur bar.
- **Local Database:** Room Database with KSP, Kotlin Coroutines, StateFlow.
- **Background Tasks:** WorkManager for periodic background syncing, AlarmManager for deadline reminders.
- **Machine Learning:** ML Kit On-Device Text Recognition for course screenshot import.
- **Networking:** OkHttp, kotlinx.serialization.json.
- **Build Optimization:** R8 Full-Mode Minification & Resource Shrinking with custom ProGuard rules.

---

## 🚀 Getting Started

### 1. Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 36 (Android 14/15+)

### 2. Configuration (`local.properties`)
Copy `khabar_app/local.properties.example` to `khabar_app/local.properties` and fill in optional keys:
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
AI_MODEL=qwen/qwen3.8-27b
AI_API_KEY=your_api_key_here
```

### 3. Build & Run
```bash
cd khabar_app

# Run Unit Tests
./gradlew testDebugUnitTest

# Build Release APK
./gradlew assembleRelease
```
The output APK will be located at `khabar_app/app/build/outputs/apk/release/app-release.apk`.

---

## 📄 License
This project is open-source and intended for academic personal productivity.
