<div align="center">

# SweepGallery

**Fast, privacy-first Android gallery cleaner built with Kotlin & Jetpack Compose.**

[![GitHub Downloads](https://img.shields.io/github/downloads/nazimunaeem/SweepGallery/total?style=flat-square&logo=github&label=Downloads&color=06B6D4)](https://github.com/nazimunaeem/SweepGallery/releases)
[![Latest Release](https://img.shields.io/github/v/release/nazimunaeem/SweepGallery?style=flat-square&logo=github&label=Release&color=4F46E5)](https://github.com/nazimunaeem/SweepGallery/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20StateFlow-06B6D4?style=flat-square)](#tech-stack)
[![SupportKori](https://img.shields.io/badge/SupportKori-Buy%20Me%20a%20Coffee-4F46E5?style=flat-square)](https://www.supportkori.com/nazimunaeem)

</div>

---

## Overview

**SweepGallery** (`v1.2.0`) helps you declutter thousands of device photos and videos with effortless swipe gestures—100% offline on your device. Swipe right to keep memories, swipe left to queue clutter for deletion, swipe up to star favorites, compare burst shots side-by-side, inspect full EXIF & directory metadata, and reclaim storage with a single system-confirmed batch delete or 30-day System Trash.

---

## Features

- **Smooth Swipe Deck (Photos & Videos)**
  - Responsive horizontal & upward swipe gestures with real-time fading **KEEP** (right), **DELETE** (left), and **STAR** (up) card overlays, directional border glows, and tactile haptic feedback.
  - Inline **video sweeping** with playback toggle, duration badge, and thumbnail decoding alongside Coil image pre-decoding and a clean **X / Undo / Star / Heart** dock.
  - **Smart Cleanup Categories**: Focus directly on **All**, **Bursts**, **Videos**, **Large (>10 MB)**, **Screenshots**, or **Starred** media.
  - **Burst Detector & Compare Sheet**: Automatically detects rapid shot sequences and opens a side-by-side **Compare Burst** sheet highlighting the sharpest frame to keep while trashing the rest.
  - **Quick-Jump Sheet, Month & Album Filtering**: Filter by month (with green completion checkmarks) or device album, sort by **Newest**, **Oldest**, or **Largest**, and tap the deck counter pill to jump directly to any item.
- **Fullscreen Zoom, Sharing & Inspector `(i)`**
  - Tap any card for a pinch-to-zoom / double-tap fullscreen **Lightbox** with quick Keep/Delete controls.
  - Open the `(i)` metadata sheet to view **Directory** with a one-tap **Show in Files** action, **Share**, **File Details** (format, size, dimensions & megapixels, date/time), and **Camera & EXIF** data (device, focal length, aperture, shutter speed, ISO, coordinates).
- **Persistent Trash Queue & Safe Batch Deletion**
  - Left-swiped items persist across app restarts in a **Trash** grid supporting **Multi-Select** batch restore/delete, individual restore, or **Restore All**.
  - Choose between permanent deletion (`MediaStore.createDeleteRequest`) or recoverable **Move to System Trash (30 days)** (`MediaStore.createTrashRequest` on Android 11+).
  - Supports **Android 14+ Partial Photo Access** (`READ_MEDIA_VISUAL_USER_SELECTED`) with an inline banner to expand media selection anytime, plus automatic `ContentObserver` deck refresh.
- **Studio, Streaks & In-App Auto-Update**
  - Tracks total space reclaimed, kept vs. deleted counts, keep ratio, daily cleanup **Streaks**, and **Weekly Storage Goals** via DataStore Preferences.
  - Space-saving **Month / Album** two-tab square grid browser, Darkroom / Light studio theme toggle, kept-history reset, and sample photo seeder.
  - **In-App Auto-Updater**: Checks GitHub Releases (manually or on launch), downloads the release APK with live progress tracking, and launches Android's package installer directly inside the app.
- **Adaptive Edge-to-Edge UI**
  - Automatically switches between a bottom `NavigationBar` on phones and a `NavigationRail` on tablets/foldables.

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin, Coroutines & Flow |
| **UI Toolkit** | Jetpack Compose, Material Design 3, Canvas |
| **Architecture** | MVVM (`AndroidViewModel` + `StateFlow`), Repository Pattern |
| **Navigation** | Jetpack Navigation Compose |
| **Imaging & Video** | Coil Compose, `MediaMetadataRetriever`, `ExifInterface`, `MediaStore` |
| **Persistence** | Jetpack DataStore Preferences |
| **Updates** | GitHub Releases API + `FileProvider` In-App APK Installer |
| **Typography** | Space Grotesk & Plus Jakarta Sans (bundled locally) |
| **Testing** | JUnit 4, Robolectric, Compose UI Test |

---

## Privacy

SweepGallery runs **100% on-device**. Photos and videos are queried directly from local `MediaStore` storage and are never uploaded, tracked, or shared externally. Network access (`INTERNET`) is used solely to check GitHub Releases and download app updates when requested.

---

## Build & Test

Requires **JDK 21**, **Android SDK** (`compileSdk 36.1`, `minSdk 24`, `targetSdk 36`), and **Gradle 9.3.1**.

```bash
# Build debug APK
gradle assembleDebug

# Run unit & Robolectric UI tests
gradle :app:testDebugUnitTest
```

Output APK path:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Automated Releases (CI/CD)

`.github/workflows/release-apk.yml` runs unit & Robolectric tests on pushes to `main`/`master`, `v*` tags, or manual workflow dispatch, and publishes an APK to **GitHub Releases**:

- Runs `gradle :app:testDebugUnitTest` on every push.
- When `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` secrets are set, builds and publishes `SweepGallery-<tag>.apk` (signed release); otherwise builds and publishes `SweepGallery-<tag>-debug.apk` signed with the repository debug keystore.

---

## Open-Source Licenses

SweepGallery is built on top of the following open-source libraries and typefaces:

| Dependency / Asset | Author / Organization | License |
| :--- | :--- | :--- |
| **Kotlin Standard Library & Coroutines** | JetBrains | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| **AndroidX Core, Activity, Lifecycle, Navigation, DataStore** | Google / Android Open Source Project | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| **Jetpack Compose & Material 3 / Icons** | Google / Android Open Source Project | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| **Coil (`coil-compose`)** | Coil Contributors | [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| **Space Grotesk** (Display Font) | Florian Karsten | [SIL Open Font License 1.1](https://scripts.sil.org/OFL) |
| **Plus Jakarta Sans** (Body Font) | Tokotype | [SIL Open Font License 1.1](https://scripts.sil.org/OFL) |
| **Robolectric** (Test Runner) | Robolectric Contributors | [MIT License](https://opensource.org/licenses/MIT) |
| **JUnit 4** (Test Framework) | JUnit Team | [Eclipse Public License 1.0](https://www.eclipse.org/legal/epl-v10.html) |

---

## Developer & Support

```text
Developer : Md. Nazim Uddin
App       : SweepGallery (Android / Kotlin / Jetpack Compose)
GitHub    : https://github.com/nazimunaeem/SweepGallery
Support   : https://www.supportkori.com/nazimunaeem
```

If **SweepGallery** helped clean up your gallery and free up storage, consider buying me a coffee on **SupportKori**:

[![Support on SupportKori](https://img.shields.io/badge/☕%20Support%20Md.%20Nazim%20Uddin-SupportKori-4F46E5?style=for-the-badge)](https://www.supportkori.com/nazimunaeem)
