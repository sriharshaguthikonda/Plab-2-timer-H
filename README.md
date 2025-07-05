# Plab 2 Timer H

This is a simple Android timer application for the "Plab 2" exam practice. It provides countdown timers with phases for practice sessions.

## Building

Use Android Studio or run `./gradlew assembleDebug` to build the project.

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
