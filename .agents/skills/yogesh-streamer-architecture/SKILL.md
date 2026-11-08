---
name: yogesh-streamer-architecture
description: >-
  Master architectural mindmap, codebase navigation, and engineering guidelines for Yogesh Streamer
  (Android & Android TV native client, extensions repository, and web cinema bridge). Use whenever
  inspecting, modifying, debugging, or building Yogesh Streamer code.
---

# Yogesh Streamer — Architecture & Mental Model Skill

Use this skill whenever working on `yogesh-streamer`, `yogesh-streamer-plugins`, or `yogesh-streamer-bridge`. It provides an instant mental model of all systems, code locations, and conventions so you never need to scan the entire codebase from scratch.

---

## 1. Ecosystem Overview

* **Main App Repo**: `shahrukh-hack/yogesh-streamer` (Local: `C:\Users\yoges\.gemini\antigravity\scratch\yogesh-streamer`)
  * Forked from CloudStream 3 with custom subsystems: Live Sports, YouTube Tab, Audio DRC Dialogue Boost, Hero Banner Shimmer, and Custom Intro.
* **Plugins Repo**: `shahrukh-hack/yogesh-streamer-plugins` (Local: `C:\Users\yoges\.gemini\antigravity\scratch\yogesh-streamer-plugins`)
  * Bundles, sanitizes, and publishes `.cs3` files to `builds` branch and `repo.json`.
* **Cinema Bridge**: `shahrukh-hack/yogesh-streamer-bridge` (Local: `C:\Users\yoges\.gemini\antigravity\scratch\yogesh-streamer-bridge`)
  * Node.js/Express web player for iOS, Mac, and PC browsers.
* **Multiplatform Portal**: `shahrukh-hack/yogesh-streamer-multiplatform` (Local: `C:\Users\yoges\.gemini\antigravity\scratch\yogesh-streamer-multiplatform`)

---

## 2. Key Directories & File Navigation

```
yogesh-streamer/
├── app/src/main/
│   ├── java/com/lagradost/cloudstream3/
│   │   ├── ui/
│   │   │   ├── sports/LiveSportsFragment.kt     # ⚡ Live Sports (SWR Cache, 3.5s timeout, progressive flow)
│   │   │   ├── youtube/YouTubeFragment.kt       # 🎬 YouTube UI (Chips, search history, SWR cache)
│   │   │   ├── home/HomeFragment.kt             # 📺 TV D-Pad Rail Focus (Home, Sports, YouTube)
│   │   │   ├── home/HomeParentItemAdapter.kt    # 🧠 Shared RecycledViewPool across rails
│   │   │   ├── home/HomeParentItemAdapterPreview.kt # 🖼️ Hero Banner Shimmer (matches aspect ratio)
│   │   │   ├── player/DynamicRangeCompressor.kt # 🔊 Media3 Audio DSP (Dialogue Boost & Night Mode)
│   │   │   ├── player/FixedNextRenderersFactory.kt # 🎛️ AudioSink config with Compressor
│   │   │   ├── player/CS3IPlayer.kt             # ⏯️ ExoPlayer builder & compressor state
│   │   │   ├── player/FullScreenPlayer.kt       # 🎚️ Player UI & Compressor Dialog
│   │   │   └── SplashVideoActivity.kt           # 🌟 3D Golden Studio Video Intro (5.5s watchdog)
│   │   ├── plugins/
│   │   │   ├── PluginManager.kt                 # 🔌 Bundled plugin extraction & loader
│   │   │   └── RepositoryManager.kt             # 🌐 Repo URLs, download hashes, sha256 check
│   │   ├── utils/
│   │   │   └── StartupSoundPlayer.kt            # 🎵 Thread-safe startup audio (5s fade cap)
│   │   ├── YouTubeProvider.kt                   # 📹 YouTube oEmbed & video parser
│   │   └── MainActivity.kt                      # 📱 App entry, navigation, TV layout bindings
│   ├── assets/plugins/                          # 📦 Bundled .cs3 files (JioTV, CineStream, CastleTv, etc.)
│   ├── res/
│   │   ├── layout/loading_hero_banner.xml       # 🖼️ 1:1 Aspect ratio skeleton shimmer
│   │   ├── layout/compressor_dialog.xml         # 🔊 Dialogue Boost / DRC dialog
│   │   ├── layout/player_custom_layout.xml      # 📱 Phone player layout with compressor button
│   │   ├── layout/player_custom_layout_tv.xml   # 📺 TV player layout with compressor button
│   │   ├── raw/intro_video.mp4                  # 🎥 Startup video
│   │   └── raw/om_namah_shivaya.mp3             # 🎶 Sacred startup sound
│   └── build.gradle.kts                         # ⚙️ App gradle script (versionCode & versionName)
├── gradle/libs.versions.toml                    # 🏷️ Version configuration (e.g. 5.5.0 / 146)
└── .github/workflows/
    └── build_yogesh_streamer.yml                # 🚀 GitHub Actions CI (builds signed release APK)
```

---

## 3. Core Subsystems & Design Patterns

### A. Live Sports Hub (`LiveSportsFragment.kt`)
* **Concurrency Pattern**: Never use `awaitAll()` on raw scraper lists (the slowest server blocks the whole UI). Always use progressive parallel child coroutines with `withTimeoutOrNull(3500L)`.
* **Progressive Dispatch**: Call `sportsAdapter.submitList(...)` immediately as each provider finishes.
* **Cache**: `CopyOnWriteArrayList<ExpandableHomepageList>` Stale-While-Revalidate (SWR) cache renders in 0ms on revisits.

### B. YouTube Media Engine (`YouTubeProvider.kt` & `YouTubeExtractor.jvmCommon.kt`)
* **Hardware AVC/H.264 Priority**: H.264 streams are sorted ahead of VP9 and AV1 to stop dropped frames and high CPU spikes on Android TV / FireStick chipsets.

### C. Audio Dynamic Range Compressor (DRC)
* **Presets**:
  * Dialogue Boost: `threshold = -24f`, `makeupGain = 12f`, `ratio = 8f`.
  * Night Mode (Heavy): `threshold = -30f`, `makeupGain = 16f`, `ratio = 8f`.
  * Light: `threshold = -18f`, `makeupGain = 4f`, `ratio = 8f`.
* Controlled via `DynamicRangeCompressor.kt` AudioProcessor.

### D. Audio & Video Startup Safeguards
* `StartupSoundPlayer.kt` is a singleton with a 5,000ms auto-fade timer.
* Never instantiate raw `MediaPlayer()` in activities.
* `SplashVideoActivity.kt` has a 5,500ms safety timeout and checks `play_startup_video_key`.

---

## 4. Operational Rules for Agents

1. **Brand Name**: Always maintain `Yogesh Streamer` in `strings.xml`.
2. **Git & Releases**: Push code to `origin master`. Push release tags explicitly: `git push origin v<version>`. Do **NOT** use `--tags` (it will try to push foreign tags from upstream).
3. **Line Endings & Windows Scripts**: The workspace is Windows. Use Python scripts for Contiguous String replacements to preserve CRLF line endings.
4. **CI Build Tracking**: Monitor GitHub Actions with `gh run list --repo shahrukh-hack/yogesh-streamer`.
