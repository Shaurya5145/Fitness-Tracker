# FitArc — Fitness Tracker

**Plan your training. Log your meals. See your progress.**

FitArc is an Android fitness companion that brings workout tracking, reusable routines, nutrition, body weight, and progress photos into one place. It is designed for students, gym users, and anyone who wants a consistent training habit without maintaining several separate logs.

The larger vision connects **training, nutrition, recovery, and physical progress** in a single daily dashboard. The interface uses dark surfaces, electric-blue accents, clear charts, and quick data entry.


## Contents

- [The idea](#the-idea)
- [Features in this repository](#features-in-this-repository)
- [A typical workout flow](#a-typical-workout-flow)
- [Technology and architecture](#technology-and-architecture)
- [Run locally](#run-locally)
- [Configuration](#configuration)
- [Build and test](#build-and-test)
- [Data handling and release readiness](#data-handling-and-release-readiness)
- [Product roadmap](#product-roadmap)
- [Planned Free and Pro model](#planned-free-and-pro-model)
- [Contributing](#contributing)
- [Maintainer and license](#maintainer-and-license)

## The idea

Fitness progress is easier to understand when the activities behind it are recorded together. FitArc aims to answer practical questions: What did I lift last time? Am I meeting my nutrition targets? Is my weight moving toward my goal? How has my physique changed?

The product is built around four principles:

- **One daily view:** bring training, food, weight, and progress together, with recovery tracking in the roadmap.
- **Fast logging:** reusable routines and meal templates reduce repetitive entry.
- **Local-first use:** Room holds the working data on the device; cloud services support account access, backup, AI, and photo storage.
- **Useful feedback:** make trends, training consistency, and exercise progression easy to inspect.

## Features in this repository

### Home and analytics

- Current weight, daily calories, weekly workout count, and workout streak.
- Quick actions, recent progress photos, and a suggested next workout.
- Weight trends, exercise-volume progression, muscle-group training frequency, workout consistency, and calorie/weight analysis.
- Account access and manual cloud backup/restore controls.

The dashboard currently displays a fixed **Build Muscle** goal label. It should not be interpreted as a complete personalized coaching or goal-selection system.

### Workout tracking

- Start an empty workout or begin from a saved routine.
- Name, rename, duplicate, and delete sessions; add session notes.
- Add exercises and record weight and repetitions for each set.
- Track drop sets, and duration/distance for supported cardio exercises.
- See previous-set values and an estimated one-rep maximum.
- Mark sets complete and use a rest timer with an additional 30-second control.
- Record session start/end times and review workout history.
- Categorize exercises by muscle group, with Gemini-assisted mapping when configured.

### Reusable routines

- Create and edit routine names and descriptions.
- Select exercises, change their order, and remove exercises.
- Add/remove sets and configure target weight and repetitions.
- Save a routine and start a workout from it.
- Preserve drafts while editing and warn about unsaved changes.

The routine data model also contains fields for exercise notes, rest periods, superset groups, set types, repetition ranges, and duration/distance targets. These fields are foundations for the fuller routine experience; the current editor does not expose every modeled option.

### Nutrition

- Create reusable meal templates and log meals by date and meal category.
- Record calories and protein, and set daily nutrition targets.
- Edit and delete meals; review daily and weekly summaries.
- Schedule meal reminders through Android notifications.
- Estimate meal details from text descriptions or food photos using Gemini, when configured.

AI-generated calorie and protein values are estimates that users should review before saving.

### Weight and goals

- Record body weight by date and review weight history.
- Set a target weight and target date, with a starting weight for progress tracking.
- Inspect weight charts, compare weight with calorie intake, and remove incorrect records.

### Progress photos

- Import photos from the gallery and store them locally.
- Organize photos as front, side, or back, with date, weight, and notes.
- Browse a timeline and compare before/after photos with a draggable divider.
- Upload photos to Cloudinary when configured and retain remote URLs for use across devices.

Advanced photo alignment, rotation, zoom/pan controls, and richer pose/source metadata are part of the broader product direction, not a promise about this checkout.

### Accounts and backup

- Local use without a permanent account; anonymous Firebase sign-in when configured.
- Email/password registration and sign-in, Google Sign-In, and password-reset emails.
- Link an anonymous account to permanent credentials.
- Clear local account data on sign-out.
- Back up and restore weights, workouts/exercises/sets, exercise mappings, meals/logs, nutrition and weight targets, photo metadata, and routines/set targets.
- Serialize backup and restore operations with a mutex and replace restored local data in a Room transaction.

**Current sync behavior:** backup writes a snapshot to a single Firestore document at `users/{uid}`. Restore replaces the local dataset; it is not a multi-device merge engine. Local-only photo files are not transferred by a metadata backup, so restoring the images on another device depends on successful remote uploads.

## A typical workout flow

1. Open **Workouts** and choose **New Routine**.
2. Name the routine, add exercises, and enter target sets, weights, and reps.
3. Save it and select **Start Routine**, or start an empty session.
4. Log each set, mark it complete, and use the rest timer between sets.
5. Finish the session and review history and dashboard analytics.
6. Add nutrition, weight, and progress-photo records to build a fuller picture over time.

## Technology and architecture

| Layer | Technology |
| --- | --- |
| Android UI | Kotlin, Jetpack Compose, Material 3, Navigation Compose |
| State and async work | ViewModel, Kotlin Coroutines, Flow / StateFlow |
| Local storage | Room, DAOs, schema migrations, KSP |
| Accounts and cloud data | Firebase Authentication, Cloud Firestore |
| Photo hosting | Cloudinary via OkHttp |
| AI | Gemini API via Retrofit and Kotlin serialization |
| Images | Coil |
| Reminders | Android notifications and alarms |
| Test tooling | JUnit, Robolectric, Compose testing, Room testing, Roborazzi |

Current data flow:

```text
Compose screens
    |
FitnessViewModel
    |-- AppRepository --> Room / AppDao / RoutineDao
    |-- AuthManager --> Firebase Authentication
    |-- CloudSyncManager --> Firestore + Cloudinary
    |-- GeminiService / RetrofitClient --> Gemini API
```

Useful entry points:

| Path | Responsibility |
| --- | --- |
| [FitnessApplication.kt](app/src/main/java/com/example/FitnessApplication.kt) | Application setup and manual Firebase initialization |
| [MainScreen.kt](app/src/main/java/com/example/ui/MainScreen.kt) | Navigation between Weight, Workouts, Home, Nutrition, and Progress |
| [FitnessViewModel.kt](app/src/main/java/com/example/ui/FitnessViewModel.kt) | UI state, workout actions, timers, account and sync orchestration |
| [data/](app/src/main/java/com/example/data) | Entities, repositories, DAOs, authentication, AI, and cloud operations |
| [ui/](app/src/main/java/com/example/ui) | Feature screens, routine builder, charts, and theme |
| [MealReminderReceiver.kt](app/src/main/java/com/example/notifications/MealReminderReceiver.kt) | Meal reminder delivery and scheduling |
| [app/schemas/](app/schemas) | Exported Room schemas; current database version is 22 |
| [app/src/test/](app/src/test) | Local JVM/Robolectric tests and screenshot-test scaffolding |
| [app/src/androidTest/](app/src/androidTest) | Device/emulator test scaffolding |

### Build configuration

| Setting | Value in this repository |
| --- | --- |
| Minimum Android version | Android 7.0 / API 24 |
| Compile / target SDK | 36 / 36 |
| Android Gradle Plugin | 8.9.1 |
| Gradle wrapper | 8.11.1 |
| Kotlin | 2.2.10 |
| Java / Kotlin bytecode target | JVM 11 |
| Application ID | `com.aistudio.fitnesstracker.zyxwvu` |
| Source namespace | `com.example` |

Versions come from [app/build.gradle.kts](app/build.gradle.kts) and [gradle/libs.versions.toml](gradle/libs.versions.toml). The Gradle runtime JDK is separate from the JVM bytecode target.

## Run locally

### Prerequisites

- Android Studio with Android SDK Platform 36 and the required SDK build tools.
- JDK 17 for the Gradle/AGP toolchain.
- An emulator or physical Android device running API 24 or later.
- Git and internet access for the initial dependency download.

### Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/Shaurya5145/Fitness-Tracker.git
   cd Fitness-Tracker
   ```

2. Copy [`.env.example`](.env.example) to `.env` in the project root:

   ```powershell
   # Windows PowerShell
   Copy-Item .env.example .env
   ```

   ```bash
   # macOS / Linux
   cp .env.example .env
   ```

3. Open the project root in Android Studio. Select JDK 17 as the Gradle JDK and let Gradle sync finish.
4. Configure the optional integrations below. Placeholder values leave Firebase, AI, and remote photo functionality unavailable; local logging is the starting point for exploring the app.
5. Select the `app` run configuration and launch it on your emulator or device.

## Configuration

The Secrets Gradle Plugin reads `.env`, with `.env.example` as its default properties file. Rebuild after changing values.

| Variable | Used for |
| --- | --- |
| `FIREBASE_API_KEY` | Firebase client configuration |
| `FIREBASE_APP_ID` | Firebase Android application ID |
| `FIREBASE_PROJECT_ID` | Firebase project used for authentication and Firestore |
| `GOOGLE_WEB_CLIENT_ID` | Web OAuth client ID used to request the Google ID token |
| `GEMINI_API_KEY` | Current direct Gemini calls for meal analysis and exercise mapping |
| `CLOUDINARY_URL` | Current direct Cloudinary integration; parsed as `cloudinary://<api_key>:<api_secret>@<cloud_name>` |

### Firebase and Google Sign-In

1. Register an Android app in your Firebase project using the application ID above, or consistently change the application ID and service configuration for your own app.
2. Enable the authentication providers you intend to use: Anonymous, Email/Password, and Google.
3. Create a Firestore database and configure per-user access rules for `users/{uid}`. This repository does not include a deployable Firestore rules file.
4. Set the three Firebase values in `.env`. `FitnessApplication` initializes Firebase programmatically; the current Gradle setup does not apply the Google Services plugin. Simply adding `google-services.json` is not the configuration path used by this checkout.
5. For Google Sign-In, set the **web** OAuth client ID and register the signing certificate SHA-1 and SHA-256 fingerprints. Obtain them with `.\gradlew.bat signingReport` on Windows or `./gradlew signingReport` on macOS/Linux. Use a Google Play-enabled emulator or a device with Google Play services.

### AI, photos, and reminders

- Gemini features need a valid development API key and network access. Manual nutrition entry remains available without AI.
- Cloudinary configuration enables remote photo upload/deletion. Without it, photos remain local.
- Allow notification permission on Android 13+ and check the device's alarm permissions/settings if meal reminders are not delivered.

**Do not ship production secrets in this client configuration.** Values used through `BuildConfig` are packaged in the Android app. Ignoring `.env` in Git does not hide those values in a distributed APK. The protected backend described in the roadmap is required before treating Gemini and Cloudinary credentials as server-only secrets.

## Build and test

Run these commands from the project root after SDK and JDK setup:

```powershell
# Windows
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

On macOS/Linux, use `./gradlew` instead of `.\gradlew.bat`; make the wrapper executable with `chmod +x gradlew` if necessary. Connected tests require a running emulator or attached device. The debug APK is normally written to `app/build/outputs/apk/debug/app-debug.apk`.

The repository includes example tests, a current-schema Room opening test, and screenshot-test scaffolding. This is not comprehensive migration or end-to-end feature coverage, and the presence of test files is not a claim that all checks currently pass.

### Troubleshooting

| Symptom | Check |
| --- | --- |
| SDK or Gradle sync failure | SDK Platform 36, the Gradle JDK, and the versions pinned in the repository |
| Firebase is unavailable | Replace Firebase placeholder values in `.env` and rebuild |
| Google Sign-In fails | Web OAuth client ID, enabled Google provider, package name, signing fingerprints, and Play services |
| AI returns no result | Gemini key, API access/quota, network connection, and the selected model in `GeminiApiService.kt` |
| Photos do not appear on another device | Successful Cloudinary upload, a saved remote URL, and an account backup/restore |
| Release signing fails | Provide a real release keystore and the signing environment variables; see below |

## Data handling and release readiness

- **Local data:** fitness records use Room; imported photos are copied into app storage. Android backup is enabled by the manifest.
- **Cloud data:** configured Firebase services receive account and backup data; Cloudinary receives uploaded progress photos; Gemini receives descriptions or images submitted for AI analysis.
- **Restore semantics:** restore replaces local data transactionally. Sign-out clears local records. Back up data you want to retain before using these operations.
- **Sync limitations:** the current Firestore backup is one account document, with snapshot writes rather than per-record incremental synchronization. Larger histories and concurrent-device edits need a more scalable sync design.
- **Server configuration:** Cloud Functions code, deployed access rules, entitlement enforcement, and purchase verification are not included here.
- **Signing:** release configuration reads `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` and uses the alias `upload`. It otherwise falls back to a root `debug.keystore`, which is ignored by Git. Configure proper release signing before distribution.
- **Distribution:** no published release is currently provided in this repository. Play Store billing, privacy/data-disclosure documentation, and release validation remain release work.

## Product roadmap

The intended full FitArc experience extends this foundation in the following areas:

- [ ] **Sleep and recovery:** sleep/wake times, calculated duration, quality rating (1–5), notes, editable history, seven-day patterns, recovery targets, and sleep debt.
- [ ] **Complete routine controls:** expose configurable rest periods, supersets, exercise notes, warm-up/normal/drop/failure sets, rep ranges, and duration/distance targets throughout the editor and workout flow.
- [ ] **Richer progress photos:** independent before/after alignment, zoom/pan, rotation, positioning, and pose/source metadata.
- [ ] **Unified dashboard:** include sleep and recovery alongside training, nutrition, weight, goals, and photo progress.
- [ ] **Protected backend:** route Gemini calls and Cloudinary signing/deletion through authenticated Firebase Functions, keeping service secrets off the device.
- [ ] **Scalable sync:** separate Firestore records, upload changed data, include sleep records, and improve multi-device recovery/conflict handling.
- [ ] **Pro subscriptions:** server-managed entitlements, client read-only entitlement access, Google Play Billing, and purchase verification.
- [ ] **Release preparation:** stronger feature/migration tests, accessibility and layout checks, privacy documentation, production signing, and store assets.

## Planned Free and Pro model

This is the product plan, **not an active checkout or a statement that these limits are enforced by this repository**.

| Capability | Free plan | Pro plan |
| --- | --- | --- |
| Progress photos | Up to 5 | Unlimited |
| Workout history | Last 15 days | Full history |
| Saved routines | Up to 3 | Unlimited |
| Nutrition entry | Manual | Manual + AI from text and photos |
| Core tracking | Workouts, weight, nutrition, dashboard; sleep in the full product | Core tracking plus Pro benefits |

Planned pricing: **₹99/month · ₹499/six months · ₹799/year**.

The intended entitlement model lets the app read Pro status while trusted server logic grants or revokes access. This checkout does not include Google Play Billing integration or that entitlement backend, so paid checkout is not available here.

## Contributing

Report reproducible problems or suggest improvements through [GitHub Issues](https://github.com/Shaurya5145/Fitness-Tracker/issues). Include the Android version, steps to reproduce, and expected versus actual behavior.

For a change:

1. Create a focused branch and keep the change limited to one purpose.
2. Follow the existing Kotlin/Compose and repository patterns.
3. Run the relevant build, lint, and test tasks; explain any checks you could not run.
4. Include a Room migration and updated schema when changing persisted data.
5. Update documentation and include screenshots for visible UI changes.
6. Keep real keys, account data, signing files, and private progress photos out of commits.

## Maintainer

Maintained by [Shaurya Gupta](https://github.com/Shaurya5145).

