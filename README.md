<div align="center">
  <br>
  <h1>⚡ BETA MANAGER</h1>
  <p><strong>Game Performance Optimizer for Android</strong></p>
  <p>Flash ZIP plugins · Apply performance profiles · Dominate your game</p>
  <br>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
    <img src="https://img.shields.io/badge/Compose-BOM_2024.12-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose">
    <img src="https://img.shields.io/badge/minSdk-26-00E676?style=for-the-badge&logo=android&logoColor=white" alt="minSdk">
    <img src="https://img.shields.io/badge/targetSdk-36-00E5FF?style=for-the-badge&logo=android&logoColor=white" alt="targetSdk">
  </p>
  <br>
</div>

---

<div align="center">
  <h3>👨‍💻 DEVELOPED BY <strong>WILLY JR. C. GAILO</strong> 👨‍💻</h3>
  <p><code>beta.manager</code> · MVVM + Jetpack Compose · Shizuku API · ADB/Root Activation</p>
</div>

---

## 📋 TABLE OF CONTENTS

- [✨ Features](#-features)
- [📸 Screens](#-screens)
- [🏗️ Architecture](#️-architecture)
- [📁 Runtime Directory](#-runtime-directory)
- [🧩 Plugin System](#-plugin-system)
- [⚙️ Activation Flow](#️-activation-flow)
- [🔄 Plugin Lifecycle](#-plugin-lifecycle)
- [🌐 WebUI Flow](#-webui-flow)
- [📦 Tech Stack](#-tech-stack)
- [🔧 Build Instructions](#-build-instructions)
- [📄 License](#-license)

---

## ✨ FEATURES

| Feature | Description |
|---------|-------------|
| 🧩 **Plugin System** | Flash ZIP files para mag-apply ng CPU/GPU/memory tweaks. Katulad ng Magisk/KernelSU modules — **walang root required** |
| 🎮 **Game Profiles** | Per-app performance profile para sa MLBB, COD Mobile, PUBG, Genshin Impact, Wild Rift, Farlight 84, at marami pa |
| 📡 **Multi-Mode Activation** | Wireless Debugging · ADB USB · TCP Mode · Root/SU |
| 🌐 **WebUI Support** | KernelSU-compatible JavaScript bridge (`ksu.exec`, `ksu.toast`) para sa plugin interfaces |
| 🔥 **Warm Boot** | Hot restart ng service nang hindi nagre-reboot ng device |
| 🎨 **Cyberpunk Dark Theme** | Neon cyan/pink/purple gaming aesthetic UI |
| ⚡ **Boot Automation** | Auto-start service at mag-load ng plugins pag-on ng device |
| 📊 **Real-time Status** | Service running indicator, plugin stats, activation log |
| 🛡️ **ADB-Level Privileges** | Gumagana sa ilalim ng `com.android.shell` — secure at hindi kailangan ng root |

---

## 📸 SCREENS

| Screen | Preview | Description |
|--------|---------|-------------|
| 🏠 **Home** | | Service status (running/stopped), plugin count, activation mode selector (Wireless/ADB/TCP/Root), quick actions grid |
| 📦 **Plugins** | | Listahan ng naka-install na plugins, toggle enable/disable, remove, at "Action" button para sa plugin scripts |
| 🎯 **Game Profiles** | | Per-game performance profiles. Apply CPU governor, GPU boost, at memory optimization per app |
| ⚙️ **Settings** | | Gaming mode toggle, auto-start, thermal throttle control, CPU governor override, GPU boost, debug mode |

---

## 🏗️ ARCHITECTURE

```
┌──────────────────────────────────────────────────────────────────┐
│                      BETA MANAGER APP                            │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                  UI LAYER (COMPOSE)                      │    │
│  │                                                          │    │
│  │   MainActivity ──► NavGraph                              │    │
│  │       │                                                  │    │
│  │       ├── 🏠 HomeScreen                                  │    │
│  │       ├── 📦 PluginScreen                                │    │
│  │       ├── 🎯 GameProfilesScreen                          │    │
│  │       └── ⚙️ SettingsScreen                              │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ observes                                   │
│  ┌──────────────────▼──────────────────────────────────────┐    │
│  │               VIEWMODEL LAYER                           │    │
│  │     (StateFlow / Coroutines / Dispatchers.IO)           │    │
│  └──────────────────┬──────────────────────────────────────┘    │
│                     │ calls                                     │
│  ┌──────────────────▼──────────────────────────────────────┐    │
│  │              SERVICE / LOGIC LAYER                      │    │
│  │                                                          │    │
│  │   🖥️ BetaService  ◄────── (AIDL IPC)                    │    │
│  │   📡 AdbClient (suspend + withContext IO)                │    │
│  │   🔌 AdbActivator (multi-stage activation)               │    │
│  │   📦 PluginManager (scan / enable / disable / remove)    │    │
│  │   📥 PluginInstaller (ZIP extract / validate / deploy)   │    │
│  │   ⌨️ Shell (command execution engine)                    │    │
│  └─────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

### 🚀 End-to-End Flow

```
📱 User installs APK
        │
        ▼
🔑 Choose activation method
        │
        ├── 📡 Wireless Debugging ──► Pair via mDNS ──► ADB connect
        ├── 🔌 ADB USB ──► adb shell sh /sdcard/beta_start.sh
        ├── 🌐 TCP Mode ──► adb tcpip 5555 ──► connect via IP
        └── ⚡ Root/SU ──► pm grant / su -c start service
        │
        ▼
🖥️  BetaService starts
  (com.android.shell — ADB-level permissions)
        │
        ▼
📂 Scan plugins directory
        │
        ▼
▶️  Run per plugin: post-fs-data.sh → service.sh
        │
        ▼
✅  System ready! Gaming mode activated.
```

---

## 📁 RUNTIME DIRECTORY

Pagkatapos ma-activate, gumagawa ang Beta Manager ng working directory sa device:

```
📂 /data/user_de/0/com.android.shell/beta/
│
├── 📁 bin/
│   └── 📄 busybox              ← BusyBox (standalone mode)
│
├── 📁 plugins/                  ← All installed ZIP plugins
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
└── 📁 logs/                     ← Service logs
```

> **💡 Why `com.android.shell`?**  
> ADB runs under the `com.android.shell` user. Through this, Beta Manager gets ADB-level permissions **without needing root**.

---

## 🧩 PLUGIN SYSTEM

### 📦 ZIP Structure

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

### 📝 module.prop Format

```properties
id=my_perf_module
name=My Performance Module
version=v1.0.0
versionCode=1
author=YourName
description=CPU/GPU optimization module
betaPlugin=1          ← Must be <= Beta Manager server version
```

### 🌍 Environment Variables

Available sa lahat ng plugin scripts:

| Variable | Value | Description |
|----------|-------|-------------|
| `BETA` | `true` | ✅ Confirms running under Beta Manager |
| `BETAVER` | `int` | 📊 Server version (e.g. `10001`) |
| `MODDIR` | `path` | 📂 Base path ng module |
| `MODPATH` | `path` | 📂 Install target (customize.sh only) |
| `ARCH` | `arm/arm64/x86/x64` | 🖥️ CPU architecture |
| `IS64BIT` | `true/false` | 🔢 64-bit check |
| `API` | `int` | 📱 Android API level (e.g. `34`) |
| `BOOTMODE` | `true` | 🚀 Always true under Beta Manager |

---

## ⚙️ ACTIVATION FLOW

```
📱 User opens Beta Manager
        │
        ▼
🔍 App checks: is BetaService running?
        │
    ❌ NO ──┤
            ▼
        🎯 Choose activation method:
            │
        ┌──┴──┐
        │     │
        ▼     ▼
   ┌─────────────┐  ┌──────────────┐
   │ 📡 Wireless │  │ 🔌 ADB USB   │
   │   Debugging │  │   (Computer) │
   │             │  │              │
   │ 1. Open QS  │  │ adb shell sh │
   │    tile     │  │   /sdcard/   │
   │ 2. mDNS     │  │ beta_start.sh│
   │    pairing  │  │              │
   │ 3. ADB      │  │              │
   │    connect  │  │              │
   └──────┬──────┘  └──────┬───────┘
          │               │
          ▼               ▼
   ┌─────────────┐  ┌──────────────┐
   │ 🌐 TCP Mode │  │ ⚡ Root / SU │
   │             │  │              │
   │ adb tcpip   │  │ su -c start  │
   │ connect via │  │ Auto boot    │
   │ IP address  │  │ init         │
   └──────┬──────┘  └──────┬───────┘
          │               │
          └───────┬───────┘
                  │
                  ▼
        ✅ BetaService RUNNING
        📊 Loading dialog shown
        🔄 Scanning plugins...
```

---

## 🔄 PLUGIN LIFECYCLE

```
┌──────────────────────────────────────────────────────────┐
│ 📥 INSTALL                                                │
│                                                           │
│  User selects ZIP ──► Validate module.prop                │
│      │               ├── Check 'id' format               │
│      │               ├── Check betaPlugin version         │
│      │               └── Validate required fields         │
│      ▼                                                    │
│  Extract ZIP ──► Run customize.sh ──► Deploy to plugins/  │
│                                                           │
│  ✅ Plugin installed!                                      │
├──────────────────────────────────────────────────────────┤
│ 🚀 BOOT (per enabled plugin)                               │
│                                                           │
│  Android BOOT_COMPLETED ──► BetaService starts            │
│      │                                                    │
│      ├── 🔄 post-fs-data.sh  (early boot)                 │
│      │     Set props, mount files, patch system           │
│      │                                                    │
│      └── ⚡ service.sh  (late_start daemon)               │
│            CPU governor, thermal, network tweaks          │
│            Runs in background even when screen is off     │
│                                                           │
├──────────────────────────────────────────────────────────┤
│ 🎯 ACTION BUTTON                                          │
│                                                           │
│  User taps "Action" ──► Runs action.sh                    │
│      │                                                    │
│      └── Output displayed in Action screen                │
│                                                           │
├──────────────────────────────────────────────────────────┤
│ 🚫 DISABLE                                                │
│                                                           │
│  Creates plugins/<id>/disable flag                        │
│  Scripts will NOT run on next boot                        │
│                                                           │
├──────────────────────────────────────────────────────────┤
│ 🗑️ REMOVE                                                 │
│                                                           │
│  Creates plugins/<id>/remove flag                         │
│  Next boot: run uninstall.sh → delete folder              │
└──────────────────────────────────────────────────────────┘
```

---

## 🌐 WEBUI FLOW

```
Plugin may webroot/index.html
        │
        ▼
User taps "WebUI" sa plugin card
        │
        ▼
Beta Manager serves local HTTP server
(KernelSU-compatible implementation)
        │
        ▼
WebView loads index.html
        │
        ▼
JavaScript can call shell commands via:
        │
        ├── 🖥️ ksu.exec(cmd)     → via BetaService
        ├── 💬 ksu.toast(msg)    → Android toast
        └── 🔲 ksu.fullScreen(bool) → UI toggle
```

> **⚡ Difference vs KernelSU:**  
> - Permissions: ADB-level, not root  
> - `webroot` path: local to plugin directory  
> - No need for `/data/adb/modules/<id>/webroot`

---

## 📦 TECH STACK

<div align="center">

| Category | Technology | Version |
|----------|-----------|---------|
| 🗣️ **Language** | Kotlin | 2.1.0 |
| 🎨 **UI Framework** | Jetpack Compose + Material 3 | BOM 2024.12 |
| 🏗️ **Architecture** | MVVM (ViewModel + StateFlow) | — |
| 🖥️ **Backend** | Android Service + AIDL IPC | — |
| 🔑 **Privilege** | Shizuku API | 13.1.5 |
| 🔄 **Async** | Kotlin Coroutines | 1.9.0 |
| 🧭 **Navigation** | Navigation Compose | 2.8.5 |
| ⚙️ **Build** | Gradle + AGP | 8.11.1 / 8.7.3 |
| 📱 **Minimum SDK** | Android 8.0 (Oreo) | API 26 |
| 🎯 **Target SDK** | Android 16 | API 36 |

</div>

---

## 🔧 BUILD INSTRUCTIONS

```bash
# 📥 Clone the repository
git clone <repo-url> BetaManager
cd BetaManager

# 🏗️ Build debug APK
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug

# 📍 APK location:
#    app/build/outputs/apk/debug/app-debug.apk
```

### 📋 Requirements

<div align="center">

| Tool | Version |
|------|---------|
| ☕ **JDK** | 17+ |
| 📱 **Android SDK** | 36 |
| 🐘 **Gradle** | 8.11.1 |

</div>

---

## 📄 LICENSE

```
╔══════════════════════════════════════════════════════╗
║              BETA MANAGER v1.0.0                     ║
║         Game Performance Optimizer for Android       ║
║                                                      ║
║           Copyright © 2026 Willy Jr. C. Gailo        ║
║              All Rights Reserved                     ║
╚══════════════════════════════════════════════════════╝
```

---

<div align="center">
  <p>
    <strong>⚡ BETA MANAGER</strong> — <em>Level up your gaming performance</em>
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
  </p>
</div>
