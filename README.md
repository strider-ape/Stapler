<p align="center">
  <img src="art/stapler-icon.svg" width="112" alt="Stapler icon">
</p>

<h1 align="center">Stapler</h1>

<p align="center">
  Turn photos and scans into a clean PDF on Android.<br>
  Offline, private, and free.
</p>

<p align="center">
  <a href="https://github.com/strider-ape/Stapler/releases/latest"><img src="https://img.shields.io/github/v/release/strider-ape/Stapler?label=download&color=3D5AFE" alt="Latest release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/strider-ape/Stapler?color=3D5AFE" alt="License: GPL-3.0"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3D5AFE" alt="Android 8.0+">
</p>

Pick your images, put the pages in order, choose margins and quality, name the file and save it. That's all Stapler does, and it tries to do it well.

## Download

Get the APK from the [latest release](https://github.com/strider-ape/Stapler/releases/latest) and open it on your phone. Android will ask you to allow installs from your browser or file manager the first time.

New versions install straight over old ones.

<details>
<summary>Check that the APK is genuine</summary>

Every release is signed with the same key. With the Android SDK's `apksigner`, the certificate's SHA-256 should be:

```
d0c030906b6bbb6abb7d80aecc90d5b8460941922b360717ddbec9f7f4096e72
```

```bash
apksigner verify --print-certs Stapler-vX.Y.Z.apk
```

</details>

## How to use

1. Tap **Select images** and pick your photos or scans, or share them to Stapler from your gallery.
2. Arrange the pages: hold and drag to reorder, and tap a page to rotate, replace or delete it.
3. Choose the page size, white margins and quality. The preview shows each page as it will be saved.
4. Name the file and tap **Create PDF**. Then open or share it straight away.

## Features

- **Manage pages:** drag to reorder, rotate, replace an image, move pages earlier or later, and delete with undo.
- **White margins:** None, Small, Medium or Large.
- **Page size:** A4, Letter, or *Fit image*, where each page takes the shape of its photo. Landscape photos get landscape pages.
- **Quality:** Low, Medium, High (default, print quality at about 300 dpi on A4) or Original. Lower settings give much smaller files.
- **Saving:** to `Downloads/Stapler`, or to any folder you choose.
- **Small touches:** photos keep their correct orientation, transparent PNGs get a white background, colours follow your wallpaper on Android 12+, and the icon supports themed icons on Android 13+.

## Privacy

Stapler has **no internet permission**, so Android itself stops it from sending anything anywhere. It asks for no storage permission either: it only sees the images you pick, and it only writes the PDFs you save. There are no ads, accounts or analytics.

## Requirements

- Android 8.0 (API 26) or newer.
- Saving straight to Downloads needs Android 10 or newer. On Android 8–9 you pick the folder with the system save dialog.

## Build from source

You need JDK 17 or newer and the Android SDK (API 37 platform). The Gradle wrapper downloads everything else.

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install it on a connected phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests with `./gradlew testDebugUnitTest`.

To build a signed release, create a `keystore.properties` file in the project root (it is git-ignored) with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`, then run `./gradlew assembleRelease`. Without that file, release builds are left unsigned.

## How it works

Android's built-in `PdfDocument` stores images losslessly, which makes photo PDFs very large and ignores any quality setting. Stapler instead uses a small PDF writer of its own ([`JpegPdfWriter`](app/src/main/java/com/stapler/app/pdf/JpegPdfWriter.kt)) that embeds each page as a JPEG.

Built with Kotlin, Jetpack Compose and Material 3. Drag-to-reorder uses [Reorderable](https://github.com/Calvin-LL/Reorderable). The icon's source artwork is [`art/stapler-icon.svg`](art/stapler-icon.svg).

## License

Stapler is free software under the [GNU General Public License v3.0](LICENSE). You may use, study, change and share it. Any version you distribute must also be released under the GPL-3.0, with its source code.
