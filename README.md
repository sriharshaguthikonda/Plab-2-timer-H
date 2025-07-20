# Plab 2 Timer H

This is a simple Android timer application for the "Plab 2" exam practice. It provides countdown timers with phases for practice sessions.

## Features

- Multi-phase countdown timer for PLAB 2 practice
- Interface now avoids overlapping device cutouts such as punch-hole cameras
- Adjustable TTS volume slider up to 200% with a warning when exceeding 100%

## Building

1. Install the Android SDK. On Debian/Ubuntu you can install the
   `android-sdk` package or download Google's command line tools and extract
   them to a directory such as `/usr/lib/android-sdk`.
2. Set the `ANDROID_HOME` environment variable to that directory or create a
   `local.properties` file with `sdk.dir=/path/to/android-sdk` so Gradle can
   locate the SDK.
3. Accept the required platform and build-tool licenses. If `sdkmanager` is
   available you can run `sdkmanager --licenses`; otherwise use the
   `google-android-cmdline-tools` package to accept them.
4. Run `./gradlew assembleDebug` (or build from Android Studio).

## Requirements

* JDK 17 or newer installed and available on your `PATH`.
* Android SDK with at least API level 24 (Android 7.0) since the
  project's `minSdk` is 24.

## Testing

The project contains unit tests and instrumentation tests.

Run all unit tests with:

```bash
./gradlew test
```

Run instrumentation tests on a connected device or emulator with:

```bash
./gradlew connectedAndroidTest
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## Continuous Integration

A GitHub Actions workflow builds the debug APK on every push and pull request to `main`. The resulting APK is uploaded as a build artifact.
