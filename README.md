# MyFit

MyFit is an AI-powered Android fitness application that generates fully personalized weekly workout plans tailored to each user's goals, available equipment, and experience level. Rather than offering generic, one-size-fits-all routines, MyFit leverages Google's Gemini AI to act as a virtual strength and conditioning coach, building custom 7-day training programs that adapt to what the user actually has access to and what they're trying to achieve.

When a user first signs up, they complete a short onboarding flow where they select their fitness goal (such as building muscle, losing weight, or improving endurance), how many days per week they want to train, what equipment they have available (ranging from bodyweight-only to a full gym), and their experience level. From that point on, MyFit generates a complete weekly plan with specific exercises, sets, and reps for each training day, with rest days distributed throughout the week. Users can begin a workout, check off exercises as they complete them, and mark days as finished. If a workout feels too easy or too hard, they can adjust the difficulty on the fly, and Gemini will regenerate that day's programming while keeping the same exercise selection.

The app also tracks workout history across multiple weeks. Completed weeks are archived and viewable in the History tab, where users can drill into each day to see how many exercises they finished. An Account screen displays profile stats like total completed workouts and current week streak, and allows users to update their training preferences at any time. Changing preferences triggers a fresh plan generation so the workouts always stay aligned with the user's current situation.

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture:** MVVM with ViewModels, StateFlow, and Coroutines
- **Backend:** Firebase Authentication (email/password) and Cloud Firestore
- **AI:** Google Gemini (`gemini-2.5-flash`) via the Firebase AI SDK
- **Navigation:** Jetpack Navigation Compose with type-safe serializable destinations
- **Serialization:** Kotlinx Serialization for Firestore DTO mapping
- **Min SDK:** 28 (Android 9.0+)

## Features

- **AI-Generated Workout Plans** — Full 7-day plans built by Gemini based on user preferences, with a deterministic fallback generator for offline or error scenarios
- **Live Workout Tracking** — Check off exercises during a session, with color-coded day cards showing completion status (completed, partial, missed)
- **On-the-Fly Difficulty Adjustment** — Make any day easier or harder without changing the exercise selection
- **Week Regeneration** — Redo the current week or start a brand new week at any time
- **Workout History** — Browse archived weeks with per-day completion summaries
- **Profile & Stats** — View total completed workouts, week streaks, and update training preferences
- **Onboarding Flow** — Guided setup for goal, training frequency, equipment, and experience level

## How to Run

### Prerequisites

1. **Install Android Studio** — Download and install [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended). This is the standard IDE for Android development and comes bundled with everything you need, including the Kotlin plugin, Gradle, and the Android SDK.

2. **During installation**, make sure the following components are selected (they should be checked by default):
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device (AVD)

### Clone the Repository

Open a terminal and clone the project:

```bash
git clone https://github.com/dburkhart07/MyFit.git
```

### Open the Project

1. Launch **Android Studio**.
2. Select **"Open"** from the welcome screen (not "New Project").
3. Navigate to the cloned `MyFit` folder and select it.
4. Android Studio will begin syncing the Gradle project. This may take a few minutes on the first run as it downloads all dependencies. Wait for the sync to complete — you'll see "BUILD SUCCESSFUL" in the Build output or the progress bar at the bottom will finish.

### Set Up an Android Emulator

If you don't already have an emulator configured:

1. Go to **Tools > Device Manager** in the Android Studio menu bar.
2. Click **"Create Virtual Device"**.
3. Choose a phone model (e.g., **Pixel 7** or **Medium Phone**) and click **Next**.
4. Select a system image with **API level 28 or higher** (e.g., API 34 or 35). If the image isn't already downloaded, click the download icon next to it and wait for it to finish.
5. Click **Next**, then **Finish**.
6. The emulator will now appear in the Device Manager. You can launch it by pressing the play button next to it.

### Run the App

1. In the toolbar at the top of Android Studio, make sure your emulator (or a connected physical device) is selected in the **device dropdown**.
2. Click the **green play button** (Run) or press **Shift+F10**.
3. Android Studio will build the project and install the app on the emulator. The first build may take several minutes.
4. Once it launches, you'll see the MyFit login screen. Create an account to get started, complete the onboarding flow, and your first AI-generated workout plan will be ready.

### Troubleshooting

- **Gradle sync fails:** Make sure you're connected to the internet. Go to **File > Sync Project with Gradle Files** and try again.
- **"SDK not found" errors:** Go to **File > Project Structure > SDK Location** and confirm the Android SDK path is set. Android Studio usually auto-detects this.
- **Emulator won't start:** Ensure hardware virtualization (Intel VT-x or AMD-V) is enabled in your BIOS/UEFI settings. On Windows, also make sure **Windows Hypervisor Platform** is enabled in Windows Features.
- **Build takes a very long time:** This is normal for the first build. Subsequent builds will be much faster due to Gradle caching.

## Project Structure

```
app/src/main/java/com/example/myfit/
├── MainActivity.kt              # App entry point, Compose setup
├── auth/                         # Authentication (Firebase Auth + Firestore)
├── data/                         # Repositories, workout generation, DTO mappers
├── model/                        # Domain models and Firestore DTOs
└── ui/
    ├── navigation/               # NavHost and route destinations
    ├── login/                    # Login and signup screens
    ├── onboarding/               # Preference selection flow
    ├── workouts/                 # Weekly plan view and live workout tracking
    ├── history/                  # Archived week browsing
    ├── account/                  # Profile stats and preference editing
    ├── common/                   # Shared UI components (top bar, bottom bar, etc.)
    └── theme/                    # Material 3 theming, colors, typography
```
