# Yogesh Streamer

**Yogesh Streamer** is an Android media center and streaming player engineered for performance, modularity, and freedom.

> **Note:** By default, Yogesh Streamer comes clean without third-party sources. You can install any standard CloudStream repository or extension to add custom sources and features.

---

## 🌟 Key Features

- **100% CloudStream Extension Compatibility**: Fully supports all `.cs3` plugins and repositories (Hexated, SuperStream, Sorastream, Sora, Anime, IPTV, etc.).
- **Advanced Video Player**:
  - Double-tap seek and swipe gestures for brightness & volume.
  - Intro & Outro Skip buttons.
  - Subtitle styling, font customization, and sync adjustment.
  - Multi-audio track and quality selector (1080p, 720p, 480p, Auto).
  - Episode selector, next episode auto-play, and playback speed control (0.5x - 2.0x).
- **Phone & Android TV Support**: Dual interface optimized for touchscreens and Android TV D-Pad remotes.
- **Chromecast Support**: Stream directly to your TV or Google Cast device.
- **Bookmarks & History**: Automatic watch progress tracking and watchlist categorization.
- **Zero Tracking**: Clean, open, and private.

---

## 🚀 Building & Releasing the App

### Option 1: Automated GitHub Actions (Recommended)
1. Push this repository to your GitHub account (e.g. `https://github.com/your-username/yogesh-streamer`).
2. Go to the **Actions** tab on GitHub.
3. Select **Build Yogesh Streamer APK** and click **Run workflow**.
4. Once completed, download the generated `YogeshStreamer-APK` artifact directly to your phone or Android TV.

### Option 2: Local Build with Android Studio
1. Open this directory (`yogesh-streamer`) in **Android Studio**.
2. Let Gradle sync dependencies.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Locate the APK in `app/build/outputs/apk/debug/app-debug.apk`.

### Option 3: Command Line (Gradle)
```bash
./gradlew assembleDebug
```
The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔌 Adding Extensions & Repositories

1. Open **Yogesh Streamer**.
2. Go to **Settings > Extensions / Repositories**.
3. Tap **Add Repository** and enter any compatible repository URL (e.g., standard CloudStream repos or `yogeshrepo://` links).
4. Browse and install your favorite plugins!
