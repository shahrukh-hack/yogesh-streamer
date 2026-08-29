# Yogesh Streamer

[![Download APK](https://img.shields.io/badge/Download-YogeshStreamer--v5.2.5.apk-2563EB?style=for-the-badge&logo=android&logoColor=white)](https://github.com/shahrukh-hack/yogesh-streamer/releases/download/v5.2.5/YogeshStreamer-v5.2.5.apk)
[![Latest Release](https://img.shields.io/github/v/release/shahrukh-hack/yogesh-streamer?style=for-the-badge&color=7C3AED)](https://github.com/shahrukh-hack/yogesh-streamer/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Android%20TV%20%7C%20FireStick-22C55E?style=for-the-badge&logo=android)](https://github.com/shahrukh-hack/yogesh-streamer/releases)

**Yogesh Streamer** is a next-generation Android & Android TV media center engineered for extreme performance, modularity, and high-speed streaming. Built upon the powerful open-source CloudStream media foundation, it delivers a custom luxury interface, dedicated Live Sports tracking, permanent extension persistence, and enhanced video link decoders.

---

## 📥 Direct Download & Installation

Anyone can download the ready-to-install APK directly without requiring a login:

* 🚀 **Latest Release (v5.2.5 APK):** **[Download YogeshStreamer-v5.2.5.apk](https://github.com/shahrukh-hack/yogesh-streamer/releases/download/v5.2.5/YogeshStreamer-v5.2.5.apk)**
* 📦 **All Releases & Archives:** [View GitHub Releases](https://github.com/shahrukh-hack/yogesh-streamer/releases)

---

## ✨ Key Features & Highlights

- 🎬 **Cinematic "YM" Video Intro**:
  - Fullscreen 3D Marvel/Prime Video-style animated intro featuring the royal **YM (Yogesh & Monika)** gold crest and holographic cinema/cricket reels.
  - Synchronized with the sacred **Om Namah Shivaya** opening chime.
  - **1-Click Skip**: Press **OK**, **Back**, or any remote key (or tap the phone screen) to jump straight to the Home Screen.
- 🏏 **Dedicated "Live Sports" Section**:
  - Direct Live Sports navigation tab on both Phone (bottom navigation bar) and Android TV (navigation rail).
  - Aggregates live cricket matches, football leagues, and sports TV channels from dedicated sports providers (**Cricify**, **SKTech**, **Sportzx**, **PublicSports**).
- 🏰 **Strict Startup Provider Lock (CastleTV Only)**:
  - Fresh installs package and load **strictly CastleTV** by default.
  - Zero unwanted background downloading of unused extensions at launch.
  - All other 39 extensions stay cleanly in the online repository catalog ready for on-demand installation under **Settings → Extensions**.
- 🔘 **Android TV Remote Navigation & Switcher**:
  - Fully optimized D-pad remote navigation.
  - Interactive **"Select Provider"** button on the TV home header to switch catalogs in real time with 1 click.
- 🎨 **Luxury Gold Branding**:
  - Custom Yogesh Streamer luxury gold 16:9 banner for Android TV / Leanback launcher and gold monogram launcher icons for mobile.
- ⚡ **Enhanced Cloud Video Link Extractors**:
  - Built-in decoders for **HubCloud**, **GDFlix**, **DriveSeed**, **BuzzServer**, **PixelDrain API**, **StreamTape**, and **VidHide** for fast, buffer-free playback.
- 🔄 **1-Tap In-App OTA Updater**:
  - Update your Phone or Android TV directly inside the app under **Settings → Updates**.

---

## 📜 Comprehensive Version Changelog

### 🚀 [v5.2.5] — YM Cinematic Video Intro & CastleTV Home Lock
* **Added Fullscreen YM Video Intro**: Features 3D Marvel/Prime Video-style animation with the royal **YM (Yogesh & Monika)** gold crest and *Om Namah Shivaya* sacred audio.
* **1-Click Skip**: Any D-pad key, remote click, or touch tap instantly skips the intro to the Home Screen.
* **Strict Startup Provider Lock**: Ensured **CastleTV** is strictly the primary default provider loaded on fresh launches.
* **Clean On-Demand Extension State**: All other 39 extensions display the **Download icon (📥)** in Extensions settings.

### 🚀 [v5.2.4] — Video Link Resolver & Cloud Extractor Overhaul
* **Enhanced HubCloud & DriveSeed Decoders**: Added support for modern cloud domains (`hubcloud.ink`, `hubcloud.lol`, `hubcloud.art`, `hubcloud.dad`).
* **PixelDrain API Direct Stream**: High-speed direct stream parsing (`https://pixeldrain.com/api/file/{id}?download`) bypassing timer countdowns.
* **BuzzServer Redirect Resolution**: Handled `hx-redirect` and `location` headers directly to stream HDHub4u, VegaMovies, Bollyflix, and UHDmovies releases.

### 🚀 [v5.2.3] — Live Sports Aggregator Integration
* **Dedicated Live Sports Section**: Integrated `LiveSportsFragment` aggregating live cricket and sports events across installed sports providers.
* **Added Sports Navigation Tab**: Deployed custom sports vector icon to Mobile bottom nav and Android TV rail.

### 🚀 [v5.1.9] — Plugin Persistence Lifecycle Fix
* **Permanent User Extensions**: Fixed plugin storage lifecycle so user-installed extensions persist permanently across app restarts.
* **Type Cleanliness**: Resolved Kotlin types in `PluginManager.kt`.

### 🚀 [v5.1.8] — Android TV Home Provider Dropdown
* **Restored TV Home Switcher**: Added `home_preview_change_api` to `fragment_home_head_tv.xml` with dynamic provider name binding.
* **D-Pad Focus**: Enabled remote control click navigation to bring up the provider switcher dialog on Android TV.

### 🚀 [v5.1.7] — Yogesh Streamer Android TV Banner
* **True PNG Banners**: Generated valid 320x180 bitmaps across all mipmap densities replacing default icons on Leanback launchers.

### 🚀 [v5.1.5] — Android TV ViewBinding Stability
* **Fixed TV Startup Crash**: Synchronized ViewBinding IDs (`home_api_holder`) across mobile and TV layout hierarchies.

### 🚀 [v5.1.0 – v5.1.1] — Permanent Keystore Lock & Single-Plugin Startup
* **Locked Keystore in Git**: Preserved `app/release.jks` permanently to ensure zero signature conflict errors during in-app updates.
* **Initial Asset Bundler**: Streamlined startup asset extraction to strictly load `CastleTvProvider.cs3`.

### 🚀 [v5.0.0 – v5.0.9] — Foundation Release & Custom UI
* **Brand Overhaul**: Rebranded entire UI, strings, theme colors, and layout structure to **Yogesh Streamer**.
* **Sacred Om Namah Shivaya Startup Audio**: Integrated startup chime.
* **Dual Layout Engine**: Adaptive layouts supporting Phones, Tablets, Emulators, and Android TV / Fire TV sticks.

---

## 🔌 Extensions & Repositories

Yogesh Streamer is built on the **CloudStream** extension architecture. You can install extensions from the official repository or third-party community sources:

* **Official Yogesh Streamer Repository**:
  ```text
  https://raw.githubusercontent.com/shahrukh-hack/yogesh-streamer-plugins/builds/repo.json
  ```
* **Supported Community Sources**: Fully compatible with **CNCV**, **PhisherRepo**, and all standard CloudStream `.cs3` repository feeds.

---

## ⚖️ Legal Disclaimer

> [!NOTE]
> **Yogesh Streamer** is an open-source media player and aggregation client based on CloudStream. It does not host, store, broadcast, or transmit any video, audio, or media content. All content is scraped and streamed directly from publicly accessible third-party servers on the internet by user-selected extensions.
