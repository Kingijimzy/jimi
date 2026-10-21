# How to get the Jimi APK

There is **no pre-built APK stored in the repo** (binary files are built by CI or Android Studio).

## Option A — Download from GitHub Actions (easiest after CI runs)

1. Open: https://github.com/Kingijimzy/jimi/actions
2. Click the latest **Build APK** workflow run (green check)
3. Scroll to **Artifacts**
4. Download **jimi-debug-apk**
5. Unzip → install `app-debug.apk` on your phone

> First push to `main` triggers the build automatically.  
> You can also go to **Actions → Build APK → Run workflow**.

### Install on Redmi / Android

1. Transfer the APK to the phone
2. Open the file → **Install**
3. If blocked: Settings → allow install from this source
4. Open Jimi → enable **Accessibility** → add API key in Settings

## Option B — Build in Android Studio

1. Clone https://github.com/Kingijimzy/jimi
2. Open folder in Android Studio
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. APK path: `app/build/outputs/apk/debug/app-debug.apk`

## Option C — GitHub Release

When a **Release** is published on the repo, the same workflow attaches the APK to that release page:
https://github.com/Kingijimzy/jimi/releases

## Note

Debug APK is for personal testing. It is signed with the default Android debug key (safe for your own device, not for Play Store).
