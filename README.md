# Carrom Dual Screen (Native Android Kotlin App)

**Carrom Dual Screen** is a pure Native Android (Kotlin + Material 3 + Android SDK 34) multitasking utility designed to help Android users—especially **Tecno (HiOS)** device owners—manage, configure, and launch **Carrom Pool** (`com.miniclip.carrom`) alongside a second instance using Android's legitimate Multi-Window, Split-Screen, Freeform Window, App Twin / Work Profile, and Floating Control Overlay APIs.

---

## Technical Feasibility & Honest Android Limitations

Before building or deploying, here is the exact, honest technical reality of Android OS and Carrom Pool:

1. **Single-APK Multi-Instance Restriction (`com.miniclip.carrom`)**
   - Standard Android and Unity-based games like **Carrom Pool** run in a single process/task. A third-party app **cannot** force a single installed Carrom Pool APK to spawn two independent game boards simultaneously.
   - **Real Working Solution**: When a second cloned instance exists via **Tecno HiOS App Twin (Dual Apps)** or an **Android Work Profile** (e.g., Island / Shelter), `Carrom Dual Screen` detects both user profiles via `LauncherApps` + `UserManager` and launches **Screen 1** (Primary Profile) and **Screen 2** (Twin / Work Profile) into Android Split-Screen or Freeform bounds.
2. **Overlay (`SYSTEM_ALERT_WINDOW`) Limitation**
   - Android OS security isolates every app's rendering surface (`SurfaceFlinger`). An overlay permission **cannot** embed or copy Carrom Pool's interactive live game canvas into a second floating window.
   - Instead, our **Floating Window Service** provides a genuine, interactive, always-on-top **Dual Control Panel** (Move, Resize, Minimize to bubble, Maximize, Close, Quick-Launch Screen 1 / Screen 2 / Split Mode), while the two game screens themselves run inside Android's real Multi-Window panes.
3. **Touch Input Isolation & Multi-Resume**
   - Android's `InputDispatcher` isolates touch events strictly to the bounds of whichever Split-Screen or Freeform window you touch—touching Screen 1 never leaks into Screen 2.
   - On **Android 10+ (API 29+)**, Multi-Resume is supported so both split windows can remain active. On Android 8–9, the unfocused pane pauses when you tap the other window.
4. **Non-Resizable Game Manifests on Tecno HiOS**
   - Some versions of Carrom Pool declare `resizeableActivity="false"`. On Tecno Android phones, enable **Developer Options → Force activities to be resizable** and **Enable freeform windows** (accessible directly from our **Permission Manager** screen).

---

## Implemented Features

1. **First-Run 6-Step Setup Wizard (`SetupWizardActivity`)**
   - Step 1: Welcome & Transparent Architecture overview
   - Step 2: Device & Tecno HiOS Compatibility Check
   - Step 3: Required Permissions (`SYSTEM_ALERT_WINDOW`, optional Android 13+ `POST_NOTIFICATIONS`)
   - Step 4: Android Multi-Window, Multi-Resume & Freeform Support Check
   - Step 5: Carrom Pool (`com.miniclip.carrom`) & App Twin / Work Profile Detection
   - Step 6: Setup Complete summary
2. **Premium Dark & Emerald Main Dashboard (`MainActivity`)**
   - Live device readiness banner + all 8 requested sections:
     1. **Start Dual Screen**
     2. **Open Carrom Pool**
     3. **Split Screen Setup**
     4. **Floating Window** (live ON/OFF toggle synced via broadcast)
     5. **Window Settings**
     6. **Permission Manager**
     7. **Device Compatibility**
     8. **Help & Support**
3. **Dual Screen System (`DualScreenLauncherActivity` + `MultiWindowHelper`)**
   - Pick Screen 1 & Screen 2 across all detected `UserHandle` profiles (Primary + Tecno App Twin / Work Profile)
   - Split-Screen Adjacent launch (`FLAG_ACTIVITY_LAUNCH_ADJACENT | FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK`)
   - Freeform launch bounds (`ActivityOptions.setLaunchBounds` + `Top/Bottom` or `Left/Right` presets)
4. **Floating Window Service (`FloatingWindowService`)**
   - `TYPE_APPLICATION_OVERLAY` foreground service (`foregroundServiceType="specialUse"` for Android 14 compliance)
   - Drag-to-move header & minimized bubble (with optional edge snapping)
   - Bottom-right corner drag-to-resize
   - Minimize to compact emerald orb, Maximize/Restore, and Close
5. **Tecno Android Optimizations**
   - HiOS & Transsion brand detection
   - Battery optimization check + Tecno Phone Master / DuraSpeed background restriction shortcut with safe fallback

---

## Project Structure

```text
.
├── .github/
│   └── workflows/
│       └── build-apk.yml                # GitHub Actions CI to build Debug & Release APKs
├── gradle/wrapper/
│   └── gradle-wrapper.properties        # Gradle 8.7 configuration
├── build.gradle.kts                     # Root AGP 8.5.2 & Kotlin 1.9.24 config
├── settings.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts                 # compileSdk 34, minSdk 26, targetSdk 34, ViewBinding
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml          # SDK 34 <queries>, overlay & specialUse FGS
│       ├── java/com/carromdual/screen/
│       │   ├── data/PrefsManager.kt
│       │   ├── model/CompatibilityModels.kt
│       │   ├── service/FloatingWindowService.kt
│       │   ├── ui/
│       │   │   ├── MainActivity.kt
│       │   │   ├── SetupWizardActivity.kt
│       │   │   ├── DualScreenLauncherActivity.kt
│       │   │   ├── WindowSettingsActivity.kt
│       │   │   ├── PermissionManagerActivity.kt
│       │   │   ├── DeviceCompatibilityActivity.kt
│       │   │   └── HelpSupportActivity.kt
│       │   └── utils/
│       │       ├── CompatibilityChecker.kt
│       │       ├── MultiWindowHelper.kt
│       │       ├── PermissionHelper.kt
│       │       └── UiAnimHelper.kt
│       └── res/
│           ├── drawable/                # Adaptive icon layers, dark cards, vector icons
│           ├── layout/                  # Native XML layouts for all screens & overlay
│           ├── mipmap-anydpi-v26/       # Adaptive launcher icons
│           └── values/                  # colors.xml, strings.xml, themes.xml, dimens.xml
└── README.md
```

---

## How to Build the APK

### Option 1: Build APK Automatically on GitHub (Recommended)
1. Upload/push this entire repository to a new GitHub repository (`main` or `master` branch).
2. Open the **Actions** tab in your GitHub repository.
3. The included `.github/workflows/build-apk.yml` workflow will automatically:
   - Set up JDK 17, Android SDK 34, and Gradle 8.7
   - Run `gradle assembleDebug --no-daemon`
   - Run `gradle assembleRelease --no-daemon`
4. Once complete, click the workflow run and download **`CarromDualScreen-Debug-APK`** from the **Artifacts** section.
5. Install the downloaded `.apk` on your Tecno Android phone!

### Option 2: Build Locally in Android Studio
1. Open **Android Studio** (Jellyfish / Koala or newer) and choose **Open** → select this project root folder.
2. Wait for **Gradle Sync** to finish (uses JDK 17 and Android SDK 34).
3. Go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Find the generated APK at `app/build/outputs/apk/debug/app-debug.apk`.
