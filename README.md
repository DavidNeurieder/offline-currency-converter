# Offline Currency Converter

**Version:** 0.1.0

A privacy-focused currency converter Android app that works completely offline. Sync exchange rates from the free Frankfurter API and convert currencies even when you're not connected to the internet.

## Features

- Works completely offline
- Supports 160+ currencies
- Automatic exchange rate sync
- Recent conversion history
- Material Design 3 interface
- No account required
- No tracking or analytics

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt
- **Database:** Room
- **Networking:** Retrofit + OkHttp
- **Background:** WorkManager
- **Build:** Gradle (Kotlin DSL)

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

The release APK will be at `app/build/outputs/apk/release/app-release.apk`

## License

AGPLv3 - See [LICENSE](./LICENSE)