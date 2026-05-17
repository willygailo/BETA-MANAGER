<div align="center">
  <br>
  <h1>⚡ BETA MANAGER</h1>
  <p><strong>Universal Module Manager for Android</strong></p>
  <p>Flash ZIP modules · Root + Non-rooted · Magisk / KSU / APatch / Axeron Compatible</p>
  <br>
  <p>
    <img src="https://img.shields.io/badge/Version-1.4.0-00E5FF?style=for-the-badge" alt="Version">
    <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
    <img src="https://img.shields.io/badge/Compose-BOM_2024.12-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose">
    <img src="https://img.shields.io/badge/minSdk-26_(Android_8)-00E676?style=for-the-badge&logo=android&logoColor=white" alt="minSdk">
    <img src="https://img.shields.io/badge/targetSdk-35_(Android_15)-00E676?style=for-the-badge&logo=android&logoColor=white" alt="targetSdk">
    <img src="https://img.shields.io/badge/Shizuku-13.1.5-8B5CF6?style=for-the-badge" alt="Shizuku">
    <img src="https://img.shields.io/badge/Root-Magisk_|_KSU_|_APatch_|_Axeron-00E676?style=for-the-badge" alt="Root">
  </p>
  <br>
</div>

---

<div align="center">
  <h3>👨‍💻 DEVELOPED BY <strong>WILLY JR. C. GAILO</strong> 👨‍💻</h3>
  <p><code>beta.manager</code> · MVVM + Jetpack Compose · Root/Shizuku Dual Mode</p>
</div>

---

<div align="center">

## 📋 TABLE OF CONTENTS

[✨ Features](#-features) ·
[📸 Screens](#-screens) ·
[🏗️ Architecture](#️-architecture) ·
[📁 Runtime Directory](#-runtime-directory) ·
[🧩 Plugin System](#-plugin-system) ·
[⚙️ Activation Flow](#️-activation-flow) ·
[🔄 Plugin Lifecycle](#-plugin-lifecycle) ·
[🌐 WebUI Flow](#-webui-flow) ·
[🖥️ Shell Executor](#️-shell-executor) ·
[📦 Tech Stack](#-tech-stack) ·
[🔧 Build Instructions](#-build-instructions) ·
[📄 License](#-license)

</div>

---

## ✨ FEATURES

<div align="center">

| Feature | Description |
|---------|-------------|
| 🧩 **Module System** | Flash ZIP plugins/modules — **root or Shizuku**, 5 flash targets |
| 🤖 **Auto Detection** | Auto-detect: Magisk → KSU → APatch → Axeron → Shizuku → ADB |
| 📦 **Module Dashboard** | Module list with source badges: BETA / AXRON / MAGISK / KSU / APATCH / AXERON |
| 🔍 **Deep Module Scan** | Scans **6 directories**: Beta, AxManager, Magisk, KSU, APatch, Axeron |
| 🎮 **Game Profiles** | Per-app performance profile — 22 games supported (MLBB, PUBG, BGMI, COD, HoK, Genshin, etc.) |
| 🌐 **WebUI Support** | KernelSU-compatible JS bridge (`ksu.exec`, `ksu.toast`) |
| 🖥️ **Shell Executor** | Interactive terminal via AIDL-backed BetaService |
| 📋 **Service Logs** | View BetaService logs like Magisk Log |
| 🛡️ **Superuser Page** | List installed apps — shows root status |
| 🧹 **Clean All** | Remove all modules marked for deletion in one tap |
| ⚡ **Boot Automation** | Auto-start service on boot, LOCKED_BOOT_COMPLETED aware |
| 🎨 **Cyberpunk Theme** | Neon cyan/pink/purple/yellow gaming aesthetic UI |
| 🚨 **Crash Handling** | Global exception handler + crash report screen |
| 💾 **DataStore Settings** | Persistent toggles that survive app restarts |
| ⚡ **Game Boost** | One-tap CPU governor, GPU boost, memory optimization — **validates elevation before applying** |
| 🔧 **Version Info** | Query Magisk / KSU / APatch / Axeron version via AIDL |
| 🔑 **Non-Rooted Support** | **Full Shizuku fallback** — app-private directories when elevated access unavailable |
| 🛡️ **Consent-Based Activation** | No auto-activation on first launch — user chooses activation method |

</div>

---

## 📸 SCREENS

<div align="center">

| Screen | Description |
|--------|-------------|
| 🏠 **Home Dashboard** | Service status, module list with 6-source badges, quick action chips |
| 📦 **Plugins** | Module cards with FlashMode chip selector (BETA/MAGISK/KSU/APATCH/AXERON), Action, WebUI, Remove |
| 🎯 **Game Profiles** | 22 games auto-detected, toggle → applies tweaks immediately |
| ⚙️ **Settings** | Gaming mode, Shizuku, Magisk/KSU/Axeron module mode, Thermal, CPU, GPU, Debug |
| 🖥️ **Shell** | Interactive terminal, command history, AIDL-connected |
| 🌐 **WebUI** | WebView loading plugin `webroot/index.html` with `ksu.exec` JS bridge |
| 📋 **Logs** | Service log viewer |
| 🛡️ **Superuser** | App list with root detection |

</div>

---

## 🏗️ ARCHITECTURE

<div align="center">

```
┌──────────────────────────────────────────────────────────────────┐
│                      BETA MANAGER APP                            │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                  UI LAYER (COMPOSE)                      │    │
│  │   MainActivity ──► NavGraph                              │    │
│  │       ├── 🏠 HomeScreen (module list + status)           │    │
│  │       ├── 📦 PluginScreen (FlashMode chip + cards)       │    │
│  │       ├── 🎯 GameProfilesScreen (22 games)               │    │
│  │       ├── ⚙️ SettingsScreen                              │    │
│  │       ├── 🖥️ ShellExecutorScreen                         │    │
│  │       ├── 🌐 WebUIScreen                                 │    │
│  │       ├── 📋 LogScreen                                   │    │
│  │       └── 🛡️ SuperuserScreen                             │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ observes                                   │
│  ┌──────────────────▼──────────────────────────────────────┐    │
│  │               VIEWMODEL LAYER                            │    │
│  │  HomeViewModel · PluginViewModel · SettingsViewModel     │    │
│  │  GameProfilesViewModel · StateFlow + Coroutines          │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ calls                                      │
│  ┌──────────────────▼──────────────────────────────────────┐    │
│  │              SERVICE / LOGIC LAYER                       │    │
│  │   🖥️ BetaService  ◄────── (AIDL IPC)                    │    │
│  │   📡 AdbClient / AdbActivator (multi-mode)               │    │
│  │   📦 PluginManager (6-dir scan)                          │    │
│  │   📥 PluginInstaller (5 flash targets)                   │    │
│  │   ⌨️ Shell (Magisk/KSU/APatch/Axeron/SU detection)       │    │
│  │   🔑 ShizukuShell (public API + reflection fallback)     │    │
│  │   💾 PreferencesManager (DataStore)                      │    │
│  └─────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

</div>

### 🔑 Privilege Detection Hierarchy

<div align="center">

```
📱 Device boot / App launch
         │
         ▼
🔍 AUTO-DETECT (in order)
         │
    ┌────┴──────────────────────────────┐
    ▼                                   ▼
⚡ ROOTED                        🔑 NON-ROOTED
    │                                   │
    ├── 🟢 Magisk (magisk --version)    └── 🟣 Shizuku
    ├── 🔵 KernelSU (ksud / sys prop)        └── ADB Wireless/USB
    ├── 🟡 APatch (apd / ro.apatch.*)
    └── 🟠 Axeron Manager
         │
         ▼
   Shell.executeWithElevation()
   (auto-routes: root → su, non-root → Shizuku)
         │
         ▼
   Directory Strategy:
   ├── Elevated → /data/user_de/0/com.android.shell/beta/
   └── App-private → /data/data/beta.manager/files/beta/
```

</div>

### 🔍 End-to-End Flow

<div align="center">

```
📱 User installs APK
        │
        ▼
🤖 AUTO-DETECT privilege
        ├── 🟢 Magisk  ──► su -c start BetaService
        ├── 🔵 KSU     ──► su -c start BetaService
        ├── 🟡 APatch  ──► su -c start BetaService
        ├── 🟠 Axeron  ──► su -c start BetaService
        ├── 🟣 Shizuku ──► Shizuku.newProcess → start service
        └── 📡 None    ──► Show ADB/Wireless options
        │
        ▼
🖥️  BetaService starts
        │
        ▼
📁 Directory Setup:
   ├── Elevated? → /data/user_de/0/com.android.shell/beta/
   └── No? → App-private /data/data/beta.manager/files/beta/
        │
        ▼
🔍 Deep scan (6 sources):
   ├── 📁 Beta plugins dir          [BETA]
   ├── 📁 AxManager plugins dir     [AXRON]
   ├── 📁 Magisk modules            [MAGISK]
   ├── 📁 KernelSU modules          [KSU]
   ├── 📁 APatch modules            [APATCH]
   └── 📁 Axeron modules            [AXERON]
        │
        ▼
✅  Dashboard loaded! (No auto-activation — user chooses)
```

</div>

---

## 📁 RUNTIME DIRECTORY

### Elevated (Rooted / Shizuku with permission)

```
📂 /data/user_de/0/com.android.shell/beta/
│
├── 📁 bin/
│   └── busybox                   ← BusyBox standalone
│
├── 📁 plugins/                   ← Beta/AxManager plugins
│   └── 📁 <module_id>/
│       ├── module.prop           ← REQUIRED — identity
│       ├── service.sh            ← Late start daemon
│       ├── post-fs-data.sh       ← Early boot script
│       ├── action.sh             ← On-demand action
│       ├── uninstall.sh          ← Cleanup on remove
│       ├── system.prop           ← setprop values
│       ├── disable               ← Flag: plugin OFF
│       ├── remove                ← Flag: delete next boot
│       ├── 📁 system/bin/        ← Added to PATH
│       └── 📁 webroot/index.html ← WebUI
│
└── 📁 logs/                      ← Service logs
```

### App-Private (Non-rooted, no Shizuku permission)

```
📂 /data/data/beta.manager/files/beta/
│
├── 📁 plugins/                   ← Limited plugin storage
│   └── 📁 <module_id>/
│       └── module.prop           ← Identity + metadata
│
└── 📁 logs/                      ← Service logs
```

> 💡 **v1.4.0 Change:** BetaService now automatically falls back to app-private directories when elevated `mkdir` fails. No crash on non-rooted devices.

### 📂 All 6 Scanned Directories

| Directory | Source | Badge | Color |
|-----------|--------|-------|-------|
| `/data/user_de/0/com.android.shell/beta/plugins/` | Beta Manager | `BETA` | 🩵 Cyan |
| `/data/user_de/0/com.android.shell/axeron/plugins/` | AxManager | `AXRON` | 🟠 Orange |
| `/data/adb/modules/` | Magisk | `MAGISK` | 🟢 Green |
| `/data/adb/ksu/modules/` | KernelSU | `KSU` | 🟣 Purple |
| `/data/adb/apatch/modules/` | APatch | `APATCH` | 🟡 Yellow |
| `/data/adb/axeron/modules/` | Axeron Manager | `AXERON` | 🩷 Pink |

---

## 🧩 PLUGIN SYSTEM

### 📦 ZIP Structure

```
my_perf_module.zip
│
├── module.prop         ← REQUIRED
├── customize.sh        ← Optional — install script
├── service.sh          ← Optional — background daemon
├── post-fs-data.sh     ← Optional — early boot
├── action.sh           ← Optional — on-demand action
├── uninstall.sh        ← Optional — cleanup
├── 📁 system/bin/      ← Optional — binaries added to PATH
└── 📁 webroot/
    └── index.html      ← Optional — WebUI
```

### 📝 module.prop Format

```properties
id=cpu_boost
name=CPU Performance Booster
version=v2.0.0
versionCode=2
author=Willy Jr. C. Gailo
description=CPU governor tweaks, GPU boost, memory optimization
betaPlugin=10004
axeronPlugin=10004
```

### 🎯 Flash Targets (5 options)

| Target | Path | Badge |
|--------|------|-------|
| 🔵 Beta Manager | `/data/user_de/0/com.android.shell/beta/plugins/` | BETA |
| 🟢 Magisk | `/data/adb/modules/` | MAGISK |
| 🟣 KernelSU | `/data/adb/ksu/modules/` | KSU |
| 🟡 APatch | `/data/adb/apatch/modules/` | APATCH |
| 🟠 Axeron | `/data/adb/axeron/modules/` | AXERON |

### 🌍 Environment Variables

| Variable | Value | Description |
|----------|-------|-------------|
| `BETA` | `true` | Confirms running under Beta Manager |
| `BETAVER` | `10004` | Server version |
| `AXERON` | `true` | AxManager compatibility flag |
| `AXERONVER` | `10004` | AxManager version |
| `MODDIR` | path | Base path of module |
| `MODPATH` | path | Install target (customize.sh only) |
| `ARCH` | arm/arm64/x86/x64 | CPU architecture |
| `IS64BIT` | true/false | 64-bit check |
| `API` | int | Android API level |
| `BOOTMODE` | `true` | Always true under Beta Manager |

---

## 🎮 SUPPORTED GAMES (22)

| Game | Package |
|------|---------|
| Mobile Legends | `com.mobile.legends` |
| COD Mobile | `com.activision.callofduty.shooter` |
| PUBG Mobile | `com.tencent.ig` |
| BGMI | `com.pubg.imobile` |
| Genshin Impact | `com.miHoYo.GenshinImpact` |
| Wild Rift | `com.riotgames.league.wildrift` |
| Farlight 84 | `com.farlightgames.ig` |
| Free Fire | `com.dts.freefireth` |
| Free Fire Max | `com.dts.freefiremax` |
| Arena of Valor | `com.tencent.tmgp.sgame` |
| Honor of Kings | `com.tencent.tmgp.kr` |
| Valorant Mobile | `com.riotgames.valorant` |
| Honkai: Star Rail | `com.miHoYo.hkrpg` |
| Zenless Zone Zero | `com.miHoYo.zzz` |
| Wuthering Waves | `com.kuro.wutheringwaves` |
| Diablo Immortal | `com.blizzard.diablo.immortal` |
| Apex Legends Mobile | `com.ea.gp.apexlegendsmobilefps` |
| Minecraft | `com.mojang.minecraftpe` |
| Roblox | `com.roblox.client` |
| + CN variants (MLBB CN, COD CN, PUBG CN) | — |

---

## ⚙️ ACTIVATION FLOW

<div align="center">

```
📱 App opens
     │
     ▼
🔍 Is BetaService running?
     │
  ✅ YES ──► Load dashboard
     │
  ❌ NO ──► Show activation options (NO auto-activate)
               │
        ┌──────┼──────┐
        ▼      ▼      ▼
    Root    Shizuku  ADB
    (su)    (newProc) (wireless/usb/tcp)
        │      │      │
        └──────┴──────┘
               ▼
        ✅ BetaService RUNNING
        📊 Loading 6-source scan...
```

</div>

> 🛡️ **v1.4.0 Change:** Removed auto-activation on first launch. User must explicitly choose activation method to avoid permission prompts or crashes before consent.

---

## 🔄 PLUGIN LIFECYCLE

<div align="center">

```
📥 INSTALL
   User selects ZIP → Choose flash target (BETA/MAGISK/KSU/APATCH/AXERON)
   → Validate module.prop → Extract ZIP → Run customize.sh
   → ✅ Installed!

🚀 BOOT (per enabled plugin)
   BOOT_COMPLETED / LOCKED_BOOT_COMPLETED
   → post-fs-data.sh (early boot)
   → service.sh (late_start daemon)

🎯 ACTION BUTTON
   User taps "Action" → Runs action.sh → Output shown in app

🚫 DISABLE
   Creates plugins/<id>/disable flag

🗑️ REMOVE
   Creates plugins/<id>/remove flag
   Next boot: uninstall.sh → delete folder
```

</div>

---

## 🌐 WEBUI FLOW

```
Plugin has webroot/index.html
   → User taps "WebUI" on plugin card
   → WebView loads index.html
   → JS calls:
       ksu.exec(cmd)        → via BetaService AIDL
       ksu.toast(msg)       → Android toast
       ksu.fullScreen(bool) → UI toggle
```

---

## 🖥️ SHELL EXECUTOR

Run shell commands directly from the app with AIDL-backed execution.
Command history (last 100), real-time output, elevated via detected root type.

### Shell Executor Hierarchy (v1.4.0)

```
Shell.executeWithElevation(cmd)
   │
   ├── 1. Root available? → su -c cmd
   ├── 2. Shizuku permission? → ShizukuShell.execute(cmd)
   └── 3. Nothing? → Shell.Result.Error("No elevated access")
```

---

## 📦 TECH STACK

<div align="center">

| Category | Technology | Version |
|----------|-----------|---------|
| 🗣️ **Language** | Kotlin | 2.1.0 |
| 🎨 **UI Framework** | Jetpack Compose + Material 3 | BOM 2024.12 |
| 🏗️ **Architecture** | MVVM (ViewModel + StateFlow) | — |
| 🖥️ **Backend** | Android Service + AIDL IPC | — |
| 🔑 **Root** | Magisk / KernelSU / APatch / Axeron / su | — |
| 🔑 **Non-root** | Shizuku API | 13.1.5 |
| 🔄 **Async** | Kotlin Coroutines | 1.9.0 |
| 🧭 **Navigation** | Navigation Compose | 2.8.5 |
| 💾 **Persistence** | DataStore Preferences | 1.1.3 |
| ⚙️ **Build** | Gradle + AGP | 8.11.1 / 8.7.3 |
| 📱 **Minimum SDK** | Android 8.0 (Oreo) | API 26 |
| 🎯 **Target SDK** | Android 15 | API 35 |
| 🔢 **Version Code** | 10004 | v1.4.0 |

</div>

---

## 🔧 BUILD INSTRUCTIONS

### ✅ Clean Build (Recommended)

```bash
# 1. Go to project folder
cd "/home/willygailo/Documents/BETA MANAGER"

# 2. Set Java 21 (required — Java 21 recommended)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# 3. Set Android SDK path
export ANDROID_HOME=/home/willygailo/Android/Sdk

# 4. Clean build debug APK (removes all cached artifacts)
./gradlew clean assembleDebug 2>&1

# ✅ Expected output: BUILD SUCCESSFUL in ~6s
# 📍 APK location:
#    app/build/outputs/apk/debug/app-debug.apk
```

### 📱 Install to Connected Phone

```bash
# Install via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 🏗️ Build Release APK

```bash
# Build release (requires signing config in build.gradle.kts)
./gradlew assembleRelease 2>&1
```

### 📦 Build Sample Plugin

```bash
cd "cpu boost"
chmod +x *.sh
./build.sh     # creates cpu_boost_v2.0.0.zip

# Push to phone for installation via Beta Manager
adb push cpu_boost_v2.0.0.zip /sdcard/
```

| Tool | Version |
|------|---------|
| ☕ **JDK** | 21 LTS |
| 📱 **Android SDK** | 35+ |
| 🐘 **Gradle** | 8.11.1 |

---

## 📄 LICENSE

<div align="center">

```
╔══════════════════════════════════════════════════════╗
║              BETA MANAGER v1.4.0                     ║
║         Universal Module Manager for Android         ║
║         Android 8 (API 26) → Android 16 (API 36)    ║
║                                                      ║
║           Copyright © 2026 Willy Jr. C. Gailo        ║
║              All Rights Reserved                     ║
╚══════════════════════════════════════════════════════╝
```

</div>

---

<div align="center">
  <p>
    <strong>⚡ BETA MANAGER v1.4.0</strong> — <em>Root + Non-rooted Universal Module Manager</em>
  </p>
  <p>
    Built with ❤️ by <strong>Willy Jr. C. Gailo</strong>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white">
    <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white">
    <img src="https://img.shields.io/badge/Material_3-0066FF?style=flat-square&logo=materialdesign&logoColor=white">
    <img src="https://img.shields.io/badge/Android_8--16-34A853?style=flat-square&logo=android&logoColor=white">
    <img src="https://img.shields.io/badge/Shizuku-8B5CF6?style=flat-square">
  </p>
  <p>
    <a href="https://github.com/willygailo/BETA-MANAGER">GitHub</a> ·
    <a href="#-features">Features</a> ·
    <a href="TUTORIAL.md">Plugin Tutorial</a>
  </p>
</div>
