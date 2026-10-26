# Khabar (IBA Mail Assistant)
*Only the mails that matter to you.*

Khabar is an on-device Android mail assistant tailored for university students (initially configured for IBA Karachi, but fully generic and configurable via JSON). It solves notification overload by classifying incoming emails into three clear groups (**Important**, **Good to know**, and **Hidden**), extracting key action items (deadlines, quizzes, class cancellations), synthesizing category-specific notification alert tones, and summarizing emails in simple English, Roman Urdu, or Urdu script.

---

## Key Features

1. **100% On-Device & Private by Default:**
   - No analytics, no Firebase, no ads, no external servers.
   - All email content, profile data, reminders, and parsed metadata are stored locally in Room SQLite with full-text search (`FTS4`).
   - The only optional network calls are to an OpenAI-compatible endpoint (Groq, Gemini, OpenRouter) if configured by the user.

2. **Instant Mode (Notification Listener):**
   - Works immediately out-of-the-box without requiring complex OAuth keys or admin API approval.
   - Monitors incoming notifications from Outlook and Gmail, extracts sender and subject/preview, processes them through the local rules engine, and creates actionable native Khabar alerts with deep linking back to the mail app.

3. **Rule-Based Relevance Engine:**
   - **Normalizer**: Removes email reply chains, signatures, greetings, and disclaimers.
   - **Category Detection**: Weighted keyword scoring (3x title, 1x body) across 10 academic categories: Class Changes, Exams & Quizzes, Deadlines, Fees & Scholarships, Registration, Results, Attendance, Internships & Jobs, Events & Societies, and General Notices.
   - **Entity Extraction**: Regex and NLP patterns for course codes (`CS201`, `MTH201`), class sections, room numbers (`CS-04`, `G-12`), campuses (Main / City), deadlines, and URLs.
   - **Audience Matcher**: Matches course enrollment, degree program, academic standing (Freshman, Sophomore, Junior, Senior), campus, and interest toggles to route emails into `Important`, `Good to know`, or `Hidden`.

4. **Multi-Language Summaries:**
   - Offline template-based summarizer generates concise, 1-line headlines and 3-bullet summaries in:
     - **Simple English**
     - **Roman Urdu** (e.g. *"CS201 ki class kal cancel hai"*)
     - **Urdu script** (e.g. *"CS201 کی کلاس کل منسوخ ہے"*)
   - App chrome remains in clean, simple English.

5. **Synthesized Alert Sounds:**
   - 16-bit 44.1 kHz PCM sine audio synthesizer with smoothed attack and decay curves.
   - Unique melodic signatures for every category (e.g. descending tones for class cancellations, double pulse for exams, major triad for results).
   - Saved into MediaStore / private FileProvider to supply individual `NotificationChannel` custom sounds.

6. **Actionable Android Notifications:**
   - Category-specific primary actions: *Add to Calendar*, *Remind me*, *Set alarm*, *Share*, *Open portal*.
   - Quiet-during-class feature: silences non-urgent alerts during scheduled class periods.
   - Daily morning digest at 8:00 AM summarizing pending items.

---

## Visual Design

Based on the Google Stitch design specification:
- Calm, premium, Apple-like design language.
- Light theme: `#F2F2F7` background, `#FFFFFF` cards, `#000000` text, `#6C6C70` secondary text, `#C6C6C8` hairline dividers.
- Dark theme: `#000000` background, `#1C1C1E` cards, `#8E8E93` secondary text.
- Unified `#007AFF` / `#0A84FF` accent color.
- 10 category colors reserved strictly for 40dp rounded-square icon tiles.
- Reusable iOS-styled components: `LargeTitleScaffold`, `SegmentedControl`, `FloatingTabBar`, `Pill`, `CountdownRing`, `GroupedList`, `PrimaryButton`, `SecondaryButton`.

---

## Build Configuration Slots

The app builds and runs fully with all slots empty. When a slot is not configured, tapping the corresponding feature in the UI displays a helpful explanation rather than crashing.

To configure production keys, add them to `local.properties`:

```properties
# Microsoft Graph / Outlook (Delegated scopes: Mail.Read, User.Read, offline_access)
MS_CLIENT_ID=
MS_SIGNATURE_HASH=

# Google Play Services Gmail API
GMAIL_ENABLED=false

# AI Summarizer (OpenAI compatible - Groq, Gemini, OpenRouter, Ollama)
AI_BASE_URL=https://api.groq.com/openai/v1
AI_MODEL=llama-3.3-70b-versatile
AI_API_KEY=
```

Keys can also be configured dynamically within the app under **Settings > AI Summaries**, which securely encrypts API keys into the **Android Keystore** using AES-GCM.

---

## Getting Started & Building

### Prerequisites
- JDK 17, 21, or 25
- Android SDK Platform 36 (build-tools 36.0.0)

### Running Unit Tests
```bash
./gradlew test
```
Runs comprehensive verification including testing all 30 realistic IBA email scenarios from `sample_mails.json` against the rules engine.

### Building the Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Developer & Demo Tools
To test the complete workflow without connecting real email accounts:
1. Open **Settings > Developer Tools**.
2. Tap **Load Sample Profile** (loads Ali Raza, BSCS Fall 23, enrolled in CS201, CS204, MTH201, SS102).
3. Tap **Load All 30 Realistic Sample Mails** (populates the inbox with categorized emails).
4. Tap any **Simulate Incoming Mail** category to trigger live rules processing and notifications.
5. Tap **Trigger Daily Digest** to preview the morning summary notification.
6. Inspect the **Notification Inspector** to view raw extras captured by the Notification Listener service.
