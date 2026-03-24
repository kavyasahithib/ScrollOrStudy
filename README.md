# 📱 ScrollOrStudy

**ScrollOrStudy** is an Android study focus and screen-time tracking app that monitors foreground apps, categorizes them as study or distraction, and encourages habit-building through streak tracking and parent supervision.

> This README is updated to match the current repository behavior from source code inspection (Mar 2026). It removes unverified “metric algorithms” and focuses on exactly what exists.

---

## ✅ What this project actually does

- Student login via Google Sign-In (`LoginActivity`) using Firebase Auth.
- Parent login via a built-in portal (password `welcome`) and student UID link.
- Tracks two counters in local DataStore:
  - `studyTimeToday` (seconds)
  - `scrollTimeToday` (seconds)
- Uses an `AccessibilityService` (`AppAccessibilityService`) to detect app changes and record time.
- Supports toggles:
  - `Study Mode` on/off (enabled through dashboard button)
  - `Hardcore Mode` on/off
- Sends daily stats to Firebase Realtime Database (`UserRepository.syncDailyProgress`).
- Listens to realtime Firebase nodes:
  - `ai_motivation` (student messages)
  - `ai_insights` (student insights)
  - `leaderboard` (rank and score)
  - `user_data`, `user_stats`, `weekly_reports` (parent dashboard)
- Displays key stats in Jetpack Compose UI:
  - Student: study/scroll time, streak, rank, cohort messages
  - Parent: linked student stats, weekly chart image

---

## 🗂️ Important source paths

- `app/src/main/java/com/example/scrollorstudy/MainActivity.kt`
- `app/src/main/java/com/example/scrollorstudy/LoginActivity.kt`
- `app/src/main/java/com/example/scrollorstudy/AppAccessibilityService.kt`
- `app/src/main/java/com/example/scrollorstudy/data/local/PreferencesManager.kt`
- `app/src/main/java/com/example/scrollorstudy/data/repository/UserRepository.kt`
- `app/src/main/java/com/example/scrollorstudy/ui/screens/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/example/scrollorstudy/ui/screens/parent/ParentDashboardScreen.kt`
- `app/src/main/java/com/example/scrollorstudy/ui/screens/profile/ProfileScreen.kt`
- `app/src/main/java/com/example/scrollorstudy/di/AppContainer.kt`
- `app/src/main/java/com/example/scrollorstudy/ScrollOrStudyApplication.kt`

---

## 🛠️ Project architecture

1. App starts at `LoginActivity`. If already signed in, it goes to `MainActivity`.
2. In `MainActivity.onCreate()` it checks permissions:
   - `ACTION_MANAGE_OVERLAY_PERMISSION`
   - `ACTION_USAGE_ACCESS_SETTINGS`
   - Notification permission (Android 13+)
3. `MainActivity` reads the user role from `PreferencesManager.userRole` and displays either:
   - `DashboardScreen` (student role)
   - `ParentDashboardScreen` (parent role)
4. `AppAccessibilityService` runs in the background and receives `TYPE_WINDOW_STATE_CHANGED` events.
5. If the new package is in `distractingApps`, the service increments `scrollTimeToday`.
6. If in `usefulApps`, it increments `studyTimeToday`.
7. Every 10 seconds (`System.currentTimeMillis() % 10000 < 1000`) while study mode is active, the service calls `syncDailyProgress()`.
8. `syncDailyProgress()` writes to Firebase with keys under `user_data/<uid>/<date>` and updates `user_stats`.
9. UI observes both local DataStore and Firebase flows through ViewModels.

---

## 📊 Tracking behavior details

### App categories (hardcoded)
- Distracting apps (increments `scrollTimeToday`):
  - YouTube (com.google.android.youtube)
  - Instagram
  - Facebook
  - WhatsApp
  - Telegram
  - Snapchat

- Useful apps (increments `studyTimeToday`):
  - Google Classroom
  - GitHub
  - MS Word
  - MS Excel
  - Google Sheets

- Neutral/other apps: ignored.

### Study mode and hardcore mode
- `Study Mode` must be ON for time updates to be recorded.
- With `Hardcore Mode` ON, once overlay triggers, the app runs countdown, sends one more sync, and performs `GLOBAL_ACTION_HOME` to force user to stop distraction.
- With `Hardcore Mode` OFF, overlay shows AI motivation message and allows dismissal.

### Overlay behavior
- The overlay is shown after `distractionSeconds >= 15` (continuous time on distracting app while study mode active).
- Uses `WindowManager` with overlay permission.
- If `hardcore`, close button hidden and the service forces user home after 5s.

---

## 🔐 Permissions used

- `android.permission.BIND_ACCESSIBILITY_SERVICE` (`AppAccessibilityService`)
- `android.permission.SYSTEM_ALERT_WINDOW` / overlay permission
- `android.permission.PACKAGE_USAGE_STATS` (usage access screen)
- `android.permission.POST_NOTIFICATIONS` (Android 13+)
- Internet access for Firebase and network calls

---

## ☁️ Firebase schema (actual observed keys)

- `ai_motivation/<uid>/message`
- `ai_insights/<uid>/message`
- `leaderboard/<uid>/{rank,score}`
- `user_data/<uid>/<yyyy-MM-dd>/{studyTime,scrollTime,streak,lastUpdated,userName}`
- `user_stats/<uid>/{streak,lastStudyDate,userName,role}`
- `weekly_reports/<uid>/latest_chart_url` (optional, shown to parent)

---

## 👤 Roles and login flow

- Student:
  - Google login (via `firebaseAuthWithGoogle`).
  - Role set as `student` by `preferencesManager.setUserRole("student")`.
  - Can edit `userName` and toggle dark mode.
  - Can copy `userId` (UID) for parent linkage.

- Parent:
  - From login screen, enter `Student ID` and password `welcome`.
  - Role set as `parent`; `studentUidForParent` saved to DataStore.
  - Parent dashboard reads `user_data` and `user_stats` for the target student.

---

## 🧪 What this project does NOT include (explicitly)

- No cloud-driven “AI model training” in this repo; all AI text is read from Firebase Realtime nodes.
- No offline, local-only AI generation; server component not present here.
- No explicit leaderboard calculation in Kotlin; it relies on data written under `leaderboard` by an external process.
- No explicit backup/restore for DataStore, no Firestore, no WorkManager.

---

## 🛠️ Local environment setup (Android Studio)

1. Clone repository.
2. Open in Android Studio.
3. Ensure `google-services.json` exists in `app/` (already present).
4. Run Gradle sync.
5. Build and run on device/emulator with:
   - API 24+ (for accessibility overlay behavior in code)
   - 13+ for notification permission behavior
6. Give runtime permissions:
   - Usage access
   - Display over other apps
   - Notification (optional)
7. Enable the accessibility service manually after install:
   - `Settings > Accessibility > ScrollOrStudy > Permit`.

---

## 🧾 Interview-style Questions (with answers)

1. **How does ScrollOrStudy detect app usage?**
   - It uses `AppAccessibilityService` and listens for `TYPE_WINDOW_STATE_CHANGED` events. When the package name changes, it updates the current app and starts a 1Hz loop to accumulate study/scroll time.

2. **What triggers study/scroll time updates?**
   - `isStudyModeActive` must be true in `PreferencesManager`. Then the service increments `studyTimeToday` for useful apps and `scrollTimeToday` for distracting apps each second.

3. **Where is state stored?**
   - In `DataStore` (via `PreferencesManager`), where all statistics and toggles live.

4. **How does data sync to cloud work?**
   - In `AppAccessibilityService` every 10 seconds (approx) it calls `syncToFirebase()` which writes to Realtime Database via `UserRepository.syncDailyProgress`.

5. **How are parent/student linked?**
   - Student UID is copied from profile. Parent login writes `studentUidForParent` into DataStore and `ParentDashboardViewModel` observes it to query the student node.

6. **How is hard mode enforced?**
   - On overlay display, if `isHardcoreModeActive`, it hides close button and calls `performGlobalAction(GLOBAL_ACTION_HOME)` after 5 seconds, then clears overlay.

7. **How are AI messages retrieved?**
   - From Realtime Database paths `ai_insights/<uid>` and `ai_motivation/<uid>` via callback flows in `UserRepository`.

8. **How is UI binding done?**
   - `DashboardViewModel` combines local and remote flows with `combine(..).stateIn(..)`; compose collects with `collectAsState`.

9. **What could break in edge cases?**
   - `AppAccessibilityService` may stop if not granted overlay or usage access. The `distractionSeconds` condition can remain non-zero if the app continues in the same distracting app across activity events. On Android 13+, if notification permission denied, there is no direct functional impact.

10. **What is the parent chart URL and when is it shown?**
    - `weekly_reports/<uid>/latest_chart_url` is optional and only shown if not null in the parent dashboard.

---

## 🧩 Suggested improvements (future)

- Persist streak logic fully in app (e.g., daily reset + increment), instead of a separate external process.
- Add tests for `PreferencesManager` and `AppAccessibilityService` time logic.
- Add explicit in-app onboarding for required permissions.
- Handle `AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED` and multiple window binds for robust detection.

---

## 🧹 Maintainer notes

- If links to QuickChart or Gemini are needed, add an independent cloud worker to populate `ai_motivation`, `ai_insights`, `leaderboard`, and `weekly_reports` nodes.
- Keep `distractingApps` + `usefulApps` lists in `AppAccessibilityService` syncable from remote config in future.
- Use segmented metrics to avoid “false positives” by adding debouncing and event-based classification.

---

## 👨‍💻 Author
*k

