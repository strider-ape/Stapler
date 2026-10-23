<img src="art/stapler-icon.svg" width="96" alt="Stapler icon">

# Stapler

Turn photos and scans into a clean PDF on Android. Pick your images, put the pages in order, choose margins and quality, name the file and save it.

Stapler works fully offline, needs no storage permission, and has no ads or tracking.

> **Status:** early development. It has not been released yet.

## Features

- **Manage pages:** hold and drag to reorder, rotate, replace an image, move pages earlier or later, and delete with undo.
- **White margins:** None, Small, Medium or Large. The previews show each page exactly as it will be saved.
- **Page size:** A4, Letter, or *Fit image*, where each page takes the shape of its photo. Landscape photos get landscape pages.
- **Quality:** Low, Medium, High (default, print quality at about 300 dpi on A4) or Original. Pages are stored as JPEG, so lower settings give much smaller files.
- **Name on the last step:** saves to `Downloads/Stapler`, or to any folder you choose.
- **Share in:** send photos to Stapler from your gallery or file manager.
- Photos keep their correct orientation, transparent PNGs get a white background, and the colours follow your wallpaper on Android 12+.

## Requirements

- Android 8.0 (API 26) or newer.
- Saving straight to Downloads needs Android 10 or newer. On Android 8–9 you pick the folder with the system save dialog.

## Build

You need JDK 17 or newer and the Android SDK (API 37 platform). The Gradle wrapper downloads everything else.

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install it on a connected phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests with `./gradlew testDebugUnitTest`.

## How it works

Android's built-in `PdfDocument` stores images losslessly, which makes photo PDFs very large and ignores any quality setting. Stapler instead uses a small PDF writer of its own ([`JpegPdfWriter`](app/src/main/java/com/stapler/app/pdf/JpegPdfWriter.kt)) that embeds each page as a JPEG.

Built with Kotlin, Jetpack Compose and Material 3. Drag-to-reorder uses [Reorderable](https://github.com/Calvin-LL/Reorderable).

## License

Stapler is free software under the [GNU General Public License v3.0](LICENSE). You may use, study, change and share it. Any version you distribute must also be released under the GPL-3.0, with its source code.
