<div align="center">

# SweepGallery

**Fast, privacy-first Android gallery cleaner built with Kotlin & Jetpack Compose.**

[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20StateFlow-06B6D4?style=flat-square)](#tech-stack)
[![SupportKori](https://img.shields.io/badge/SupportKori-Buy%20Me%20a%20Coffee-4F46E5?style=flat-square)](https://www.supportkori.com/nazimunaeem)

</div>

---

## Overview

**SweepGallery** helps you declutter thousands of device photos with effortless swipe gestures—100% offline on your device. Swipe right to keep memories, swipe left to queue clutter for deletion, inspect full EXIF & directory metadata, and reclaim storage with a single system-confirmed batch delete.

---

## Features

- **Smooth Swipe Deck**
  - Responsive horizontal swipe & flick gestures with real-time fading **KEEP** (right) and **DELETE** (left) card overlays, directional border glows, and tactile delete haptic feedback.
  - Zero-wait back-to-back swiping via non-blocking exit animations and Coil image pre-decoding alongside a clean **X / Undo / Heart** dock.
  - **Month & Album Filtering**: Filter by month (with automatic green completion checkmarks) or device album, and sort by **Newest**, **Oldest**, or **Largest** file size.
- **Fullscreen Zoom & Inspector `(i)`**
  - Tap any card for a pinch-to-zoom / double-tap fullscreen **Lightbox** with quick Keep/Delete controls.
  - Open the `(i)` metadata sheet to view **Directory** with a one-tap **Show in Files** action, **File Details** (format, size, dimensions & megapixels, date/time), and **Camera & EXIF** data (device, focal length, aperture, shutter speed, ISO, coordinates).
- **Trash Queue & Safe Batch Deletion**
  - Left-swiped photos collect in a **Trash** grid where you can inspect sizes, restore individual photos, or **Restore All** before deleting anything.
  - **Delete Permanently** uses Android's system-confirmed `MediaStore.createDeleteRequest` dialog (with pre-Android 11 fallback) and tracks freed storage.
- **Studio & Stats**
  - Persistent stats for total space reclaimed, kept vs. deleted counts, and keep ratio via DataStore Preferences.
  - Space-saving **Month / Album** two-tab square grid browser, Darkroom / Light studio theme toggle, kept-history reset, sample photo seeder, and GitHub Releases update checker.
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
| **Imaging & EXIF** | Coil Compose, Android `ExifInterface`, `MediaStore` |
| **Persistence** | Jetpack DataStore Preferences |
| **Typography** | Space Grotesk & Plus Jakarta Sans (bundled locally) |
| **Testing** | JUnit 4, Robolectric, Compose UI Test |

---

## Privacy

SweepGallery runs **100% on-device**. Photos are queried directly from local `MediaStore` storage and are never uploaded, tracked, or shared externally. Network access (`INTERNET`) is used solely to check GitHub Releases for newer app versions.

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

`.github/workflows/release-apk.yml` builds and publishes debug-signed APKs (`app-debug.apk`) on pushes to `main`/`master`, `v*` tags, or manual workflow dispatch:

- Decodes `debug.keystore.base64` so releases share a consistent debug signing key.
- Uploads `SweepGallery-APK` as a workflow artifact.
- Publishes `app-debug.apk` to **GitHub Releases**.

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
Support   : https://www.supportkori.com/nazimunaeem
```

If **SweepGallery** helped clean up your gallery and free up storage, consider buying me a coffee on **SupportKori**:

[![Support on SupportKori](https://img.shields.io/badge/☕%20Support%20Md.%20Nazim%20Uddin-SupportKori-4F46E5?style=for-the-badge)](https://www.supportkori.com/nazimunaeem)
