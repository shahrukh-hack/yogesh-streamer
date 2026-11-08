# Yogesh Streamer — Senior Developer Architecture & Memory Map

> **Context for AI Agents & Developers**: This document serves as the master mental model for the `yogesh-streamer` codebase. Read this first to understand the architecture, core modifications, file map, and working guidelines without having to scan the entire project.

---

## 1. High-Level Ecosystem & Repositories

| Repository | Role | Key Locations |
| :--- | :--- | :--- |
| **`shahrukh-hack/yogesh-streamer`** | Native Android & Android TV client (Fork of CloudStream 3 with major custom subsystems). | `app/`, `library/`, `gradle/libs.versions.toml` |
| **`shahrukh-hack/yogesh-streamer-plugins`** | CloudStream extensions (.cs3 builds) & `repo.json` repository. | `sync-plugins.js`, `repo.json`, `plugins.json` |
| **`shahrukh-hack/yogesh-streamer-bridge`** | Express/Node.js Web Cinema Bridge for browser playback (iOS, PC, Mac). | `server.js`, `public/app.js` |
| **`shahrukh-hack/yogesh-streamer-multiplatform`** | Multiplatform distribution & cinema portal. | `server.js`, `README.md` |

---

## 2. Master Codebase & Subsystems Map

### A. Dedicated Custom Pages
1. **Live Sports Hub (`LiveSportsFragment.kt`)**
   * Path: `app/src/main/java/com/lagradost/cloudstream3/ui/sports/LiveSportsFragment.kt`
   * **Key Architecture**: Uses an in-memory **Stale-While-Revalidate (SWR)** cache for **0ms instant re-opening**. Executes parallel coroutines with a **3.5s per-provider timeout guard** (`withTimeoutOrNull(3500L)`). Emits rows progressively (`submitList`) so fast providers (JioTV, EliteStreams) show matches within 300ms without waiting for slow servers. Matches with `🔴`, `LIVE`, or `vs` are sorted to the top.
2. **Integrated YouTube Media Engine (`YouTubeFragment.kt` & `YouTubeProvider.kt`)**
   * Paths: `app/src/main/java/com/lagradost/cloudstream3/ui/youtube/YouTubeFragment.kt` and `YouTubeProvider.kt`
   * Extractor path: `library/src/jvmCommonMain/kotlin/com/lagradost/cloudstream3/extractors/YoutubeExtractor.jvmCommon.kt`
   * **Key Architecture**: Includes category filter chips (Music, Cricket, Podcasts, etc.), persistent search memory (`YOUTUBE_LAST_SEARCH_KEY`), and SWR caching. **Hardware-accelerated AVC/H.264 stream priority** prevents 100% CPU lockups on Android TV / Fire TV sticks above 480p.

### B. Video & Audio Engine
1. **Audio Dynamic Range Compressor (DRC) / Dialogue Boost**
   * Paths:
     * `app/src/main/java/com/lagradost/cloudstream3/ui/player/DynamicRangeCompressor.kt`
     * `app/src/main/java/com/lagradost/cloudstream3/ui/player/FixedNextRenderersFactory.kt`
     * `app/src/main/java/com/lagradost/cloudstream3/ui/player/CS3IPlayer.kt`
     * `app/src/main/java/com/lagradost/cloudstream3/ui/player/FullScreenPlayer.kt`
   * **Key Architecture**: Media3 `AudioProcessor` single-band feed-forward peak compressor. Features **Dialogue Boost** (-24 dB threshold, +12 dB makeup gain) and **Night Mode** (-30 dB threshold, +16 dB makeup gain). Wired directly into player controls (`ic_compressor_24.xml`, `compressor_dialog.xml`).
2. **Hero Banner Shimmer (Zero Layout Shift)**
   * Paths: `loading_hero_banner.xml`, `HomeParentItemAdapterPreview.kt`, `fragment_home.xml`, `fragment_home_tv.xml`
   * Eliminates Cumulative Layout Shift (CLS) on startup by rendering a 1:1 aspect-ratio matched skeleton banner.

### C. Branding, Intro & Audio Lifecycles
1. **Cinematic Splash Video (`SplashVideoActivity.kt`)**
   * Path: `app/src/main/java/com/lagradost/cloudstream3/ui/SplashVideoActivity.kt`
   * Plays `R.raw.intro_video` with a 5.5s watchdog timeout (can never freeze). User can tap anywhere or press remote to skip. Setting toggle: `play_startup_video_key`.
2. **Managed Startup Sound (`StartupSoundPlayer.kt`)**
   * Path: `app/src/main/java/com/lagradost/cloudstream3/utils/StartupSoundPlayer.kt`
   * **RULE**: Thread-safe singleton with **5-second auto-fade cap**. Automatically stops on user navigation, touch, or video start. **NEVER create unmanaged `MediaPlayer` instances elsewhere.** Setting toggle: `play_startup_sound_key`.

### D. Android TV Remote (D-Pad) Navigation
1. **Dynamic Rail Focus Tracking (`HomeFragment.kt`)**
   * Method: `updateTvRailFocus()` tracks scroll bounds and ensures D-Pad right navigation works across **Home, Live Sports, YouTube, Library, Downloads, and Settings**.
2. **Shared RecycledViewPool**
   * Path: `HomeParentItemAdapter.kt` -> `ParentItemAdapter.Companion.sharedPool` is reused across horizontal rails to save 50MB RAM on TV sticks.

### E. Bundled Plugins & Repositories
1. **Pre-Bundled Plugins**: Located in `app/src/main/assets/plugins/` (`JioTV.cs3`, `CineStream.cs3`, `CastleTvProvider.cs3`, `CricifyProvider.cs3`, `SportzxProvider.cs3`). Extracted on cold boot into `filesDir/bundled_plugins/`.
2. **Official Repository URL**: `https://raw.githubusercontent.com/shahrukh-hack/yogesh-streamer-plugins/builds/repo.json`. Shortcodes: `yogesh`, `official`, `yogeshstreamer`.
3. **In-App Updater**: `InAppUpdater.kt` bound to `shahrukh-hack/yogesh-streamer`.

---

## 3. Engineering Guidelines for Future AI Agents

1. **Brand Integrity**: The app title must strictly remain `Yogesh Streamer` in `strings.xml`. Do NOT alter package name or application ID (`com.lagradost.cloudstream3`) to preserve user data and update paths.
2. **Windows File Manipulation**: The host runs Windows with CRLF line endings. When making non-trivial file modifications, execute self-contained Python scripts writing to `C:\Users\yoges\.gemini\antigravity\brain\<id>\scratch\` rather than complex inline PowerShell strings.
3. **Git & Tag Pushing**: Never run `git push origin --tags` because foreign upstream tags from `recloudstream/cloudstream` will cause rejection. Always push specific tags explicitly: `git push origin master <tag_name>`.
4. **CI/CD Pipeline**: GitHub Actions (`build_yogesh_streamer.yml`) triggers on push to `master` and tags matching `v*`. The output APK is generated as `YogeshStreamer-v<version>.apk`.
