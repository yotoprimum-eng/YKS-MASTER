# YKS MASTER — Android starter project

Native Android starter built with Kotlin + Jetpack Compose (Material 3).

## Included in v0.2 — visual refresh
- Custom YKS MASTER adaptive launcher icon (navy / teal graduation-book mark)
- Refined Material 3 visual system with navy, white and turquoise brand colors
- Redesigned home hero panel, clearer hierarchy, rounded shortcut cards and improved spacing
- UI concept preview included at `design/YKS_MASTER_Arayuz_Onizleme.png`

## Included in v0.1
- Drawer navigation (hamburger menu) and home dashboard
- 2027 YKS countdown: TYT 19 June 2027; AYT/YDT 20 June 2027
- TYT/AYT topic checklist with completion toggles
- Weekly sample topic plan
- Mock-exam entry (correct / wrong / blank), net calculation and donut chart
- Student profile: name, 12th grade / graduate, TYT net target
- Local persistence for profile, topic completion and mock exams using SharedPreferences

## Design preview
Open `design/YKS_MASTER_Arayuz_Onizleme.png` to view the visual direction for the app icon, dashboard, topic tracking and mock-exam analytics. The preview is a concept image; the actual Android interface is implemented in Compose and uses live app data.

## Build an APK
1. Install Android Studio (Hedgehog or newer) and Android SDK Platform 35.
2. Open this folder (`YKSMaster`) in Android Studio.
3. Allow Gradle sync and install any requested SDK/build tools.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio will show the generated APK location (normally `app/build/outputs/apk/debug/app-debug.apk`).

## Important notes
- This environment did not have Android SDK or Gradle installed, so a compiled APK could not be produced here. This ZIP is the native Android source project, ready to open and build in Android Studio.
- The countdown uses the phone's local date and a fixed 2027 exam date. Before release, verify against the latest ÖSYM calendar and add a remote-config/update mechanism if the official date changes.
- The included weekly plan and topic catalog are starter examples, not a complete official TYT/AYT curriculum. Expand and review the catalog before public release.
- Data is stored locally on the device; there is no account sync or cloud backup in this first prototype.

## Cloud build option
A GitHub Actions workflow is included at `.github/workflows/android-apk.yml`. After uploading this project to a GitHub repository, run **Actions → Build YKS MASTER APK → Run workflow** (or push to `main`). Download the `yks-master-debug-apk` artifact from the completed workflow run.
