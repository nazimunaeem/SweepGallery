# TindPhoto

**TindPhoto** is a fast, privacy-first Android gallery cleaner that lets you declutter your device photos with Tinder-style swipe gestures. Swipe right to keep memories, swipe left to queue clutter for deletion, and reclaim storage space with a single confirmed batch delete.

---

## Features

- **Tinder-Style Swipe Deck**
  - Physics-based drag and tilt card stack with haptic feedback and live **KEEP** (right) and **DELETE** (left) stamp overlays.
  - Tactile bottom action dock for **Undo**, **Delete**, **Inspect**, and **Keep**.
  - Filter by device album (`All Photos`, `Camera`, `Screenshots`, etc.) and sort by **Newest**, **Oldest**, or **Largest** file size.
- **Fullscreen Zoom & Inspector**
  - Tap any card to open a pinch-to-zoom / double-tap fullscreen **Lightbox** with quick Keep/Delete actions.
  - Open the metadata bottom sheet to inspect filename, album, resolution, file size, and date taken.
- **Trash Queue & Safe Batch Deletion**
  - Left-swiped photos collect in a **Trash** grid where you can review file sizes, restore individual photos, or **Restore All** before deleting anything.
  - **Delete Permanently** uses Android's system-confirmed `MediaStore.createDeleteRequest` dialog (with pre-Android 11 fallback) to permanently remove queued photos and report freed storage.
- **Studio & Stats Dashboard**
  - Persistent tracking of total space reclaimed, kept vs. deleted counts, and keep ratio via DataStore Preferences.
  - Quick-launch album breakdown, Darkroom / Light studio theme toggle, kept-history reset, and an **Add Sample Photos** helper for testing on an empty gallery or emulator.
- **Adaptive Edge-to-Edge UI**
  - Switches automatically between a bottom `NavigationBar` on phones and a `NavigationRail` on tablets/foldables.

---

## Tech Stack

- **Language & UI**: Kotlin, Jetpack Compose, Material Design 3
- **Architecture**: MVVM (`ViewModel` + `StateFlow`), Repository pattern
- **Navigation**: Jetpack Navigation Compose
- **Image Loading**: Coil Compose
- **Persistence**: Jetpack DataStore Preferences
- **Media & Storage**: Android `MediaStore` (`READ_MEDIA_IMAGES` on Android 13+, `READ_EXTERNAL_STORAGE` on Android 12 and below)
- **Typography**: Bundled Space Grotesk & Plus Jakarta Sans

---

## Privacy

TindPhoto operates **100% on-device**. Your photos are read directly from local `MediaStore` storage and are never uploaded, analyzed externally, or shared off your device.

---

## Building Locally

Requires **JDK 21**, **Android SDK** (Compile/Target SDK 36.1, Min SDK 24), and **Gradle 9.3.1**.

```bash
# Build debug APK
gradle assembleDebug

# Run unit & Robolectric tests
gradle :app:testDebugUnitTest
```

The debug APK is output to:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Automated GitHub Releases

The included GitHub Actions workflow (`.github/workflows/release-apk.yml`) automatically builds and publishes the APK whenever you push to `main`/`master`, push a `v*` tag, or trigger it manually from the **Actions** tab:

- Uploads `TindPhoto-APK` as a downloadable `.zip` workflow artifact.
- Publishes the `.apk` directly to **GitHub Releases**.
- Builds a signed release APK if `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` repository secrets are configured; otherwise builds the debug APK automatically.
