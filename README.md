<div align="center">
  <br>
  <h1>⚡ BETA MANAGER</h1>
  <p><strong>Universal Module Manager for Android</strong></p>
  <p>Flash ZIP modules · Root + Non-rooted · Magisk/KSU/Axeron Compatible</p>
  <br>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
    <img src="https://img.shields.io/badge/Compose-BOM_2024.12-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose">
    <img src="https://img.shields.io/badge/minSdk-26-00E676?style=for-the-badge&logo=android&logoColor=white" alt="minSdk">
    <img src="https://img.shields.io/badge/Shizuku-13.1.5-8B5CF6?style=for-the-badge&logo=shizuku&logoColor=white" alt="Shizuku">
    <img src="https://img.shields.io/badge/Root-SU_|_Magisk_|_KSU-00E676?style=for-the-badge&logo=android&logoColor=white" alt="Root">
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
| 🧩 **Module System** | Flash ZIP plugins/modules like Magisk/KernelSU — **root or Shizuku** |
| 🤖 **Auto Activation** | Auto-detect: Root→`su`, Non-root→**Shizuku**. One tap activation |
| 📦 **Module Dashboard** | Magisk/KSU-style module list with source badges (BETA/AXRON/MAGISK/KSU) |
| 🔍 **Deep Module Scan** | Scans Beta, AxManager, Magisk, and KernelSU module directories |
| 🎮 **Game Profiles** | Per-app performance profile for MLBB, COD Mobile, PUBG, Genshin, at iba pa |
| 🌐 **WebUI Support** | KernelSU-compatible JavaScript bridge (`ksu.exec`, `ksu.toast`) |
| 🖥️ **Shell Executor** | Interactive terminal via AIDL-backed BetaService |
| 📋 **Service Logs** | View BetaService logs like Magisk Log |
| 🛡️ **Superuser Page** | List installed apps — shows root status (rooted devices) |
| 🧹 **Clean All** | Remove all modules marked for deletion in one tap |
| ⚡ **Boot Automation** | Auto-start service on boot (configurable) |
| 🎨 **Cyberpunk Theme** | Neon cyan/pink/purple gaming aesthetic UI |
| 🚨 **Crash Handling** | Global exception handler + crash report screen |
| 💾 **DataStore Settings** | Persistent toggles that survive app restarts |
| 🔄 **AxManager Compatible** | Supports `betaPlugin` + `axeronPlugin` format, scans AxManager dir |
| ⚡ **Game Boost** | One-tap CPU governor, GPU boost, memory optimization |

</div>

---

## 📸 SCREENS

<div align="center">

| Screen | Preview | Description |
|--------|---------|-------------|
| 🏠 **Home Dashboard** | | Service status, module list with source badges, 6 quick action chips (Install/Clean/Shell/Boost/Logs/SU) |
| 📦 **Plugins** | | Detailed module cards with Action, WebUI, Info, Remove buttons + detail dialog |
| 🎯 **Game Profiles** | | Per-game performance profiles. Auto-detect installed games |
| ⚙️ **Settings** | | Gaming mode, auto-start, thermal, CPU governor, GPU boost, debug — **all elevated** |
| 🖥️ **Shell** | | Interactive terminal with command history, AIDL-connected to BetaService |
| 🌐 **WebUI** | | WebView loading plugin `webroot/index.html` with `ksu.exec` JS bridge |
| 📋 **Logs** | | Service log viewer — like Magisk Log |
| 🛡️ **Superuser** | | App list with root detection (rooted only) |

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
│  │                                                          │    │
│  │   MainActivity ──► NavGraph                              │    │
│  │       │                                                  │    │
│  │       ├── 🏠 HomeDashboard (module list + status)        │    │
│  │       ├── 📦 PluginScreen (module detail manager)        │    │
│  │       ├── 🎯 GameProfilesScreen                          │    │
│  │       ├── ⚙️ SettingsScreen                              │    │
│  │       ├── 🖥️ ShellExecutorScreen                         │    │
│  │       ├── 🌐 WebUIScreen                                 │    │
│  │       ├── 📋 LogScreen                                   │    │
│  │       └── 🛡️ SuperuserScreen                             │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ observes                                   │
│  ┌──────────────────▼──────────────────────────────────────┐    │
│  │               VIEWMODEL LAYER                           │    │
│  │  (HomeViewModel · PluginViewModel · SettingsViewModel    │    │
│  │   GameProfilesViewModel · StateFlow + Coroutines)       │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ calls                                     │
│  ┌──────────────────▼──────────────────────────────────────┐    │
│  │              SERVICE / LOGIC LAYER                      │    │
│  │                                                          │    │
│  │   🖥️ BetaService  ◄────── (AIDL IPC)                    │    │
│  │   📡 AdbClient / AdbActivator (multi-mode activation)    │    │
│  │   📦 PluginManager (deep scan Beta/Axeron/Magisk/KSU)   │    │
│  │   📥 PluginInstaller (ZIP flash + Magisk/KSU support)   │    │
│  │   ⌨️ Shell (command execution)                          │    │
│  │   🔑 ShizukuShell (Shizuku-privileged execution)        │    │
│  │   💾 PreferencesManager (DataStore persistence)          │    │
│  │   🚨 CrashReportActivity (global exception handler)      │    │
│  └─────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

</div>

### 🔑 Dual Elevation Model

<div align="center">

```
         ┌──────────────────────────┐
         │   📱 Device Detection    │
         └────────────┬─────────────┘
                      │
              ┌───────┴───────┐
              ▼               ▼
   ╔════════════════╗  ╔════════════════╗
   ║   ROOTED      ║  ║  NON-ROOTED   ║
   ║               ║  ║                ║
   ║  su -c cmd    ║  ║  Shizuku API  ║
   ║  Magisk/KSU   ║  ║  shell-level   ║
   ║  Full access  ║  ║  privileges    ║
   ╚════════════════╝  ╚════════════════╝
              │               │
              └───────┬───────┘
                      ▼
         ┌──────────────────────┐
         │ ✅ Shell.executeWith │
         │   Elevation()        │
         │   Auto-routes to     │
         │   correct backend    │
         └──────────────────────┘
```

</div>

### 🚀 End-to-End Flow

<div align="center">

```
📱 User installs APK
        │
        ▼
🤖 AUTO-DETECT mode
        │
        ├── ⚡ Root detected ──► su -c start BetaService
        ├── 🔑 Shizuku detected ──► Shizuku.newProcess → start service
        └── 📡 No privilege ──► Show activation options (Wireless/ADB/TCP)
        │
        ▼
🖥️  BetaService starts
  (com.android.shell — shell-level permissions)
        │
        ▼
🔍 Deep scan modules
  ├── 📁 Beta plugins dir
  ├── 📁 AxManager plugins dir
  ├── 📁 Magisk modules dir
  └── 📁 KernelSU modules dir
        │
        ▼
▶️  Run per enabled plugin: post-fs-data.sh → service.sh
        │
        ▼
✅  System ready! Dashboard loaded.
```

</div>

---

## 📁 RUNTIME DIRECTORY

After activation, Beta Manager creates a working directory:

<div align="center">

```
📂 /data/user_de/0/com.android.shell/beta/
│
├── 📁 bin/
│   └── 📄 busybox              ← BusyBox (standalone mode)
│
├── 📁 plugins/                  ← All installed Beta/Axron plugins
│   │
│   └── 📁 <module_id>/         ← Each plugin folder
│       ├── 📄 module.prop      ← 📌 REQUIRED — plugin identity
│       ├── 📄 service.sh       ← ⚡ Late start service script
│       ├── 📄 post-fs-data.sh  ← 🔄 Early boot script
│       ├── 📄 action.sh        ← 🎯 On-demand action button
│       ├── 📄 uninstall.sh     ← 🗑️ Cleanup on remove
│       ├── 📄 system.prop      ← 🐛 Debug setprop values
│       ├── 📄 disable          ← 🚫 Flag: plugin OFF if present
│       ├── 📄 remove           ← ❌ Flag: will delete next boot
│       ├── 📁 system/
│       │   └── 📁 bin/         ← Added to system PATH
│       └── 📁 webroot/
│           └── 📄 index.html   ← 🌐 WebUI (KernelSU API)
│
└── 📁 logs/                     ← Service logs (viewable in-app)
```

</div>

### 📂 Additional scanned directories:

| Directory | Source | Badge |
|-----------|--------|-------|
| `/data/user_de/0/com.android.shell/beta/plugins/` | Beta Manager | `BETA` |
| `/data/user_de/0/com.android.shell/axeron/plugins/` | AxManager | `AXRON` |
| `/data/adb/modules/` | Magisk | `MAGISK` |
| `/data/adb/ksu/modules/` | KernelSU | `KSU` |

---

## 🧩 PLUGIN SYSTEM

### 📦 ZIP Structure

<div align="center">

```
my_perf_module.zip
│
├── 📄 module.prop         ← 📌 REQUIRED — identity metadata
├── 📄 customize.sh        ← 🛠️ Optional install script
├── 📄 service.sh          ← ⚡ Late start background daemon
├── 📄 post-fs-data.sh     ← 🔄 Early boot system patcher
├── 📄 action.sh           ← 🎯 On-demand interactive action
├── 📄 uninstall.sh        ← 🗑️ Cleanup on removal
├── 📁 system/
│   └── 📁 bin/             ← Binaries added to PATH
└── 📁 webroot/
    └── 📄 index.html      ← 🌐 WebUI page
```

</div>

### 📝 module.prop Format

```properties
id=cpu_boost
name=CPU Performance Booster
version=v2.0.0
versionCode=2
author=Willy Jr. C. Gailo
description=CPU governor tweaks, GPU boost, memory optimization
betaPlugin=10001
axeronPlugin=10001
```

> **🔄 AxManager Compatible:** Pwedeng gumamit ng `axeronPlugin` imbes na `betaPlugin`.  
> Auto-detect ng Beta Manager kung alin ang present.

### 🎯 Sample Plugin

Nasa repo na ang sample plugin: [`cpu boost/`](cpu%20boost/)

```bash
cd "cpu boost"
chmod +x *.sh
./build.sh     # creates cpu_boost_v2.0.0.zip
```

### 🌍 Environment Variables

Available sa lahat ng plugin scripts:

<div align="center">

| Variable | Value | Description |
|----------|-------|-------------|
| `BETA` | `true` | ✅ Confirms running under Beta Manager |
| `BETAVER` | `int` | 📊 Server version (e.g. `10001`) |
| `AXERON` | `true` | 🔄 AxManager compatibility flag |
| `AXERONVER` | `int` | 🔄 AxManager version (same as BETAVER) |
| `MODDIR` | `path` | 📂 Base path ng module |
| `MODPATH` | `path` | 📂 Install target (customize.sh only) |
| `ARCH` | `arm/arm64/x86/x64` | 🖥️ CPU architecture |
| `IS64BIT` | `true/false` | 🔢 64-bit check |
| `API` | `int` | 📱 Android API level (e.g. `34`) |
| `BOOTMODE` | `true` | 🚀 Always true under Beta Manager |

</div>

---

## ⚙️ ACTIVATION FLOW

<div align="center">

```
📱 User opens Beta Manager
        │
        ▼
🔍 App checks: is BetaService running?
        │
    ✅ YES ──► Load dashboard
        │
    ❌ NO ──┤
            ▼
        🤖 AUTO-DETECT PRIVILEGE
            │
        ┌───┴───┐
        ▼       ▼
   ⚡ Root   🔑 Shizuku
   Available  Available
        │       │
        ▼       ▼
   su -c     Shizuku
   start     .newProcess
   service   start service
        │       │
        └───┬───┘
            ▼
    ✅ BetaService RUNNING
    📊 Loading modules...
    🔍 Deep scanning all dirs...
```

</div>

---

## 🔄 PLUGIN LIFECYCLE

<div align="center">

```
┌──────────────────────────────────────────────────────────┐
│ 📥 INSTALL                                                │
│                                                           │
│  User selects ZIP ──► Choose flash target:                │
│      │               ├── 🔵 Beta Manager                  │
│      │               ├── 🟢 Magisk Modules                │
│      │               └── 🟣 KernelSU Modules              │
│      ▼                                                    │
│  Validate module.prop ──► Extract ZIP ──► Run customize.sh│
│                                                           │
│  ✅ Module installed!                                      │
├──────────────────────────────────────────────────────────┤
│ 🚀 BOOT (per enabled plugin)                               │
│                                                           │
│  Android BOOT_COMPLETED ──► BetaService starts            │
│      │                                                    │
│      ├── 🔄 post-fs-data.sh  (early boot)                 │
│      └── ⚡ service.sh  (late_start daemon)               │
│                                                           │
├──────────────────────────────────────────────────────────┤
│ 🎯 ACTION BUTTON                                          │
│  User taps "Action" ──► Runs action.sh                    │
│                                                           │
├──────────────────────────────────────────────────────────┤
│ 🚫 DISABLE                                                │
│  Creates plugins/<id>/disable flag                        │
│                                                           │
├──────────────────────────────────────────────────────────┤
│ 🗑️ REMOVE                                                 │
│  Creates plugins/<id>/remove flag                         │
│  Next boot: run uninstall.sh → delete folder              │
└──────────────────────────────────────────────────────────┘
```

</div>

---

## 🌐 WEBUI FLOW

<div align="center">

```
Plugin may webroot/index.html
        │
        ▼
User taps "WebUI" sa plugin card
        │
        ▼
WebView loads index.html
        │
        ▼
JavaScript can call shell commands via:
        │
        ├── 🖥️ ksu.exec(cmd)     → via BetaService AIDL
        ├── 💬 ksu.toast(msg)    → Android toast
        └── 🔲 ksu.fullScreen(bool) → UI toggle
```

</div>

> **⚡ Difference vs KernelSU:**  
> - Permissions: shell-level (root or Shizuku), not kernel  
> - `webroot` path: local to plugin directory  
> - No need for `/data/adb/modules/<id>/webroot`

---

## 🖥️ SHELL EXECUTOR

<div align="center">

```
┌─────────────────────────────────────────────┐
│  🖥️  Shell Executor                         │
├─────────────────────────────────────────────┤
│  $ echo "Hello from Beta Manager!"          │
│  Hello from Beta Manager!                   │
│                                             │
│  $ cat /proc/cpuinfo | grep processor       │
│  processor  : 0                             │
│  processor  : 1                             │
│  processor  : 2                             │
│  processor  : 3                             │
│                                             │
│  $ perf_status                              │
│  CPU: performance                           │
│  GPU: boosted                               │
│  Temp: 42°C                                 │
│                                             │
├─────────────────────────────────────────────┤
│  ┌───────────────────────────────────────┐  │
│  │  Type command...                    ▶  │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

</div>

> Run shell commands directly from the app with AIDL-backed execution.  
> May command history (last 100), ANSI output, at real-time feedback.

---

## 📦 TECH STACK

<div align="center">

| Category | Technology | Version |
|----------|-----------|---------|
| 🗣️ **Language** | Kotlin | 2.1.0 |
| 🎨 **UI Framework** | Jetpack Compose + Material 3 | BOM 2024.12 |
| 🏗️ **Architecture** | MVVM (ViewModel + StateFlow) | — |
| 🖥️ **Backend** | Android Service + AIDL IPC | — |
| 🔑 **Privilege (Root)** | su / Magisk / KernelSU / APatch | — |
| 🔑 **Privilege (Non-root)** | Shizuku API | 13.1.5 |
| 🔄 **Async** | Kotlin Coroutines | 1.9.0 |
| 🧭 **Navigation** | Navigation Compose | 2.8.5 |
| 💾 **Persistence** | DataStore Preferences | 1.1.3 |
| ⚙️ **Build** | Gradle + AGP | 8.11.1 / 8.7.3 |
| 📱 **Minimum SDK** | Android 8.0 (Oreo) | API 26 |
| 🎯 **Compile SDK** | Android 15 | API 35 |

</div>

---

## 🔧 BUILD INSTRUCTIONS

```bash
# 📥 Clone the repository
git clone https://github.com/willygailo/BETA-MANAGER.git BetaManager
cd BetaManager

# 🏗️ Build debug APK
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug

# 📍 APK location:
#    app/build/outputs/apk/debug/app-debug.apk

# 📱 Install sa connected phone
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 📦 Build Sample Plugin

```bash
cd "cpu boost"
chmod +x *.sh
./build.sh     # creates cpu_boost_v2.0.0.zip

# Push sa phone para i-install via Beta Manager
adb push cpu_boost_v2.0.0.zip /sdcard/
```

<div align="center">

### 📋 Requirements

| Tool | Version |
|------|---------|
| ☕ **JDK** | 21 LTS |
| 📱 **Android SDK** | 35+ |
| 🐘 **Gradle** | 8.11.1 |

</div>

---

## 📄 LICENSE

<div align="center">

```
╔══════════════════════════════════════════════════════╗
║              BETA MANAGER v1.2.0                     ║
║         Universal Module Manager for Android         ║
║                                                      ║
║           Copyright © 2026 Willy Jr. C. Gailo        ║
║              All Rights Reserved                     ║
╚══════════════════════════════════════════════════════╝
```

</div>

---

<div align="center">
  <p>
    <strong>⚡ BETA MANAGER</strong> — <em>Root + Non-rooted Module Manager</em>
  </p>
  <p>
    Built with ❤️ by <strong>Willy Jr. C. Gailo</strong>
  </p>
  <br>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
    <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose">
    <img src="https://img.shields.io/badge/Material_3-0066FF?style=flat-square&logo=materialdesign&logoColor=white" alt="Material3">
    <img src="https://img.shields.io/badge/Android-34A853?style=flat-square&logo=android&logoColor=white" alt="Android">
    <img src="https://img.shields.io/badge/Shizuku-8B5CF6?style=flat-square&logo=shizuku&logoColor=white" alt="Shizuku">
  </p>
  <br>
  <p>
    <a href="https://github.com/willygailo/BETA-MANAGER">GitHub</a> ·
    <a href="#-features">Features</a> ·
    <a href="#️-architecture">Architecture</a>
  </p>
</div>
