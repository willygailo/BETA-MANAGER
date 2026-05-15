# AxManager v1.4.7 — Full Architecture & Flow Documentation

> Android system control app na gumagamit ng ADB/Non-Root mode.  
> Package: `frb.axeron.manager` · minSdk 26 · compileSdk 36 · Kotlin + Jetpack Compose · Java 21

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Repository Structure](#2-repository-structure)
3. [Gradle Module Breakdown](#3-gradle-module-breakdown)
4. [App Architecture (manager module)](#4-app-architecture-manager-module)
5. [Runtime Directory (on-device)](#5-runtime-directory-on-device)
6. [Plugin System](#6-plugin-system)
7. [Activation Flow](#7-activation-flow)
8. [Boot & Service Startup Flow](#8-boot--service-startup-flow)
9. [Plugin Lifecycle Flow](#9-plugin-lifecycle-flow)
10. [WebUI Flow](#10-webui-flow)
11. [How Each Key File Works](#11-how-each-key-file-works)

---

## 1. Project Overview

```
User installs AxManager APK
        │
        ▼
Activate via one of 3 modes:
  ┌─────────────┬──────────────┬─────────────┐
  │  ADB / USB  │  Wireless    │  Root / Su  │
  │  (computer) │  Debugging   │  (rooted)   │
  └─────────────┴──────────────┴─────────────┘
        │
        ▼
AxeronService starts
(runs as com.android.shell — ADB-level permission)
        │
        ▼
BusyBox + Plugin System ready
        │
        ▼
Plugins: service.sh → post-fs-data.sh → action.sh
```

AxManager ay hindi root manager. Gumagana siya bilang **ADB daemon** —
nakakakuha ng `com.android.shell` level na permissions nang walang root.
Ang root support ay bonus feature lamang, hindi pangunahing target.

---

## 2. Repository Structure

```
AxManager/
│
├── .github/
│   └── workflows/
│       └── ci.yml                  ← GitHub Actions: auto-build at release APK
│
├── api/                            ← Shared API module
│   ├── manifest.gradle.kts         ← Version code/name definitions
│   └── src/
│       └── main/
│           └── aidl/               ← AIDL interface definitions para sa IPC
│               └── frb/axeron/
│                   └── IAxeronService.aidl
│
├── manager/                        ← Pangunahing Android app module
│   └── src/
│       └── main/
│           ├── kotlin/
│           │   └── frb/axeron/manager/
│           │       ├── MainActivity.kt
│           │       ├── App.kt
│           │       ├── ui/
│           │       │   ├── screen/
│           │       │   ├── viewmodel/
│           │       │   └── component/
│           │       ├── service/
│           │       │   └── AxeronService.kt
│           │       ├── adb/
│           │       │   ├── AdbClient.kt
│           │       │   └── AdbActivator.kt
│           │       ├── plugin/
│           │       │   ├── PluginManager.kt
│           │       │   └── PluginInstaller.kt
│           │       ├── utils/
│           │       └── crash/
│           │           └── CrashReportActivity.kt
│           ├── res/
│           │   ├── values/
│           │   │   └── strings.xml
│           │   ├── layout/
│           │   └── drawable/
│           └── AndroidManifest.xml
│
├── gradle/
│   ├── libs.versions.toml          ← Centralized dependency versions
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                ← Root build config
├── settings.gradle.kts             ← Module inclusions
├── gradle.properties               ← JVM flags, version props
├── gradlew / gradlew.bat           ← Gradle wrapper scripts
├── .gitmodules                     ← Git submodules (Shizuku, hidden API stubs)
├── README.md
└── README_cn.md
```

---

## 3. Gradle Module Breakdown

```
Root build.gradle.kts
│
│  compileSdk  = 36
│  minSdk      = 26  (Android 8.0+)
│  targetSdk   = 36
│  Java        = 21
│  Kotlin      = Android
│  Compose     = enabled
│
├── :api        ← Library module — AIDL + version manifest
└── :manager    ← Application module — lahat ng UI at logic
```

### `api/manifest.gradle.kts`
Dito nakadeclare ang `api_version_code` at `api_version_name`.
Ginagamit ito ng root `build.gradle.kts` para mag-compute ng final `versionName`:

```
versionName = "${api_version_name}.r${gitCommitCount}"
// Halimbawa: "1.4.7.r13213"
```

---

## 4. App Architecture (manager module)

```
┌─────────────────────────────────────────────────────┐
│                   AxManager App                     │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │            UI Layer (Jetpack Compose)        │   │
│  │                                             │   │
│  │  MainActivity ──► NavGraph                  │   │
│  │       │                                     │   │
│  │       ├── HomeScreen                        │   │
│  │       ├── PluginScreen                      │   │
│  │       ├── ShellExecutorScreen               │   │
│  │       ├── WebUIScreen                       │   │
│  │       ├── PrivilegeScreen                   │   │
│  │       ├── AppFilterScreen                   │   │
│  │       └── DeveloperSettingsScreen           │   │
│  └─────────────────┬───────────────────────────┘   │
│                    │ observes                        │
│  ┌─────────────────▼───────────────────────────┐   │
│  │          ViewModel Layer                     │   │
│  │  (StateFlow / coroutines / Dispatchers.IO)  │   │
│  └─────────────────┬───────────────────────────┘   │
│                    │ calls                          │
│  ┌─────────────────▼───────────────────────────┐   │
│  │          Service / Logic Layer               │   │
│  │                                             │   │
│  │  AxeronService  ◄──────────────────────┐   │   │
│  │  AdbClient (suspend + withContext IO)   │   │   │
│  │  AdbActivator                           │   │   │
│  │  PluginManager                          │   │   │
│  │  PluginInstaller                        │   │   │
│  └─────────────────────────────────────────┘   │   │
│                                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │         Crash Handling (v1.4.7 new)          │  │
│  │  GlobalExceptionHandler ──► CrashReportActivity│ │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Key Classes

| Class | Responsibilidad |
|---|---|
| `MainActivity.kt` | Entry point, Compose host, NavController |
| `App.kt` | Application class, GlobalExceptionHandler init |
| `AxeronService.kt` | Pangunahing service — pinagsama ang ShizukuService dito (v1.4.4+) |
| `AdbClient.kt` | ADB connection logic, lahat ng function ay `suspend` + `withContext(IO)` |
| `AdbActivator.kt` | Multi-stage activation: Wireless Debug QS tile, TCP, Root fallback |
| `PluginManager.kt` | Mag-load, mag-enable/disable, mag-list ng plugins |
| `PluginInstaller.kt` | Mag-extract ng ZIP, validate `module.prop`, deploy sa axeron dir |
| `CrashReportActivity.kt` | Nagpapakita ng crash info sa user (v1.4.7) |

---

## 5. Runtime Directory (on-device)

Pagkatapos ma-activate ang AxManager, gumagawa ito ng working directory:

```
/data/user_de/0/com.android.shell/axeron/
│
├── bin/
│   └── busybox                     ← Full BusyBox (Magisk-compiled)
│                                     ASH Standalone Mode — lahat ng commands
│                                     diretso sa BusyBox applet, hindi PATH
│
├── plugins/
│   └── <module_id>/                ← Bawat installed plugin
│       ├── module.prop             ← REQUIRED — metadata ng plugin
│       ├── service.sh              ← late_start service script
│       ├── post-fs-data.sh         ← early boot script
│       ├── action.sh               ← triggered ng Action button sa app
│       ├── uninstall.sh            ← triggered pag na-remove ang module
│       ├── system.prop             ← setprop values (debug mode only)
│       ├── disable                 ← flag: plugin OFF kung nandito ang file
│       ├── remove                  ← flag: aalisin sa susunod na reboot
│       ├── system/
│       │   └── bin/                ← folder na idinagdag sa PATH
│       └── webroot/
│           └── index.html          ← WebUI ng plugin (KernelSU API compatible)
│
└── logs/                           ← Service logs (optional)
```

> **Bakit `com.android.shell`?**  
> Ang ADB ay nagre-run sa ilalim ng `com.android.shell` user.
> Sa pamamagitan nito, nakakakuha si AxManager ng ADB-level permissions
> nang hindi nangangailangan ng root.

---

## 6. Plugin System

### Ano ang Plugin?

Isang ZIP file na naglalaman ng module scripts at optional WebUI.
Katulad ng Magisk/KernelSU module pero **walang kailangang root**.

### module.prop format

```properties
id=my_perf_module
name=My Performance Module
version=v1.0.0
versionCode=1
author=YourName
description=CPU/GPU optimization module
axeronPlugin=1         ← dapat <= AxManager server version
```

### Plugin Directory Layout

```
my_perf_module.zip
├── module.prop         ← REQUIRED
├── customize.sh        ← optional install script
├── service.sh          ← optional
├── post-fs-data.sh     ← optional
├── action.sh           ← optional
├── uninstall.sh        ← optional
├── system/
│   └── bin/
│       └── my_binary   ← magiging available sa PATH
└── webroot/
    └── index.html      ← WebUI
```

### Environment Variables (available sa lahat ng scripts)

| Variable | Value | Paliwanag |
|---|---|---|
| `AXERON` | `true` | Nagpapatunay na tumatakbo sa AxManager |
| `AXERONVER` | int (hal. `10407`) | Server version ng AxManager |
| `MODDIR` | path | Base path ng module — laging gamitin ito |
| `MODPATH` | path | Sa customize.sh — install target path |
| `ARCH` | arm/arm64/x86/x64 | CPU architecture ng device |
| `IS64BIT` | true/false | 64-bit check |
| `API` | int (hal. `34`) | Android API level |
| `BOOTMODE` | `true` | Laging true sa AxManager |

---

## 7. Activation Flow

```
User opens AxManager
        │
        ▼
App checks: is AxeronService running?
        │
   NO ──┤
        ▼
Choose activation method:
        │
        ├── [Wireless Debugging] ──────────────────────────┐
        │   1. Multi-stage fallback para mag-open ng        │
        │      Wireless Debugging QS tile                   │
        │   2. App nag-pair via mDNS (port auto-detect)     │
        │   3. ADB connect → start AxeronService            │
        │                                                   │
        ├── [ADB via Computer] ─────────────────────────────┤
        │   user runs:                                      │
        │   adb shell sh /sdcard/axeron_start.sh            │
        │                                                   │
        ├── [TCP Mode] ─────────────────────────────────────┤
        │   Para sa Android 10 at pababa                    │
        │   adb tcpip <port> → connect via IP               │
        │   Gumagana kahit walang WiFi pagkatapos ng once   │
        │                                                   │
        └── [Root / Su] ────────────────────────────────────┘
            pm grant / su -c → start AxeronService directly
            Auto-start sa boot via root init
                        │
                        ▼
              AxeronService RUNNING ✓
              Loading dialog shown habang nag-a-activate
```

---

## 8. Boot & Service Startup Flow

```
Android BOOT_COMPLETED broadcast received
        │
        ▼
AxManager BroadcastReceiver triggered
        │
        ▼
Check activation mode (ADB / Root / TCP)
        │
        ▼
AxeronService.start()
        │
        ▼
Hot Restart System initialized
(nagre-restart ang service nang hindi nag-reboot ng buong Android)
        │
        ▼
Scan plugins directory:
/data/user_de/0/com.android.shell/axeron/plugins/
        │
        ▼
Para sa bawat plugin (walang `disable` o `remove` flag):
        │
        ├── 1. I-run ang post-fs-data.sh  (first sync)
        │         ↓ environment variables injected
        │         ↓ runs in BusyBox ASH standalone mode
        │
        └── 2. I-run ang service.sh  (late_start service)
                  ↓ background tasks dito (hal. frequency scaling)
                  ↓ gumagana kahit naka-sleep ang screen
```

---

## 9. Plugin Lifecycle Flow

```
[INSTALL]
User pumili ng ZIP file sa AxManager app
        │
        ▼
PluginInstaller.install(zipFile)
        │
        ├── Validate module.prop (id format, axeronPlugin version check)
        ├── Extract ZIP → temp dir
        ├── Run customize.sh (kung mayroon)
        └── Deploy sa plugins/<id>/
                │
                ▼
            Plugin installed ✓


[RUNTIME — bawat boot]
post-fs-data.sh  →  service.sh


[ACTION BUTTON]
User taps Action sa plugin card
        │
        ▼
AxManager runs: sh <MODDIR>/action.sh
        │
        ▼
Output ipinapakita sa Action screen ng app
(ito yung screen na may console rendering issues na dating nire-report)


[DISABLE]
AxManager creates: plugins/<id>/disable
        │
        ▼
Scripts hindi na tatakbo sa susunod na boot


[REMOVE]
AxManager creates: plugins/<id>/remove
        │
        ▼
Sa susunod na boot:
  1. uninstall.sh i-e-execute
  2. Folder deleted
```

---

## 10. WebUI Flow

```
Plugin may webroot/index.html
        │
        ▼
User taps WebUI sa AxManager app
        │
        ▼
AxManager nagse-serve ng local HTTP server
(katulad ng KernelSU WebUI implementation)
        │
        ▼
WebView naglo-load ng index.html
        │
        ▼
JavaScript makaka-call ng shell commands
via KernelSU-compatible API:
        │
        ├── ksu.exec(cmd) → runs via AxeronService
        ├── ksu.toast(msg) → shows Android toast
        └── ksu.fullScreen(bool) → UI toggle


DIFFERENCE vs KernelSU:
  - Permissions: ADB-level lang, hindi root
  - webroot path: local to plugin directory
    (hindi /data/adb/modules/<id>/webroot)
```

---

## 11. How Each Key File Works

### Repository Files

| File | Paliwanag |
|---|---|
| `build.gradle.kts` (root) | Nagde-define ng compileSdk, minSdk, Java version para sa lahat ng submodules. Gumagamit ng version catalog (`libs.versions.toml`). Nag-i-import ng `api/manifest.gradle.kts` para makuha ang version code/name. |
| `api/manifest.gradle.kts` | Single source of truth para sa app version. Dito binabago ang version number bago mag-release. |
| `settings.gradle.kts` | Nagre-register ng `":manager"` at `":api"` modules para makilala ng Gradle. |
| `gradle.properties` | JVM heap size, Kotlin coroutines, AndroidX flags. |
| `.gitmodules` | Nagtutukoy ng external git submodules — malamang Shizuku API at hidden API stubs. |
| `.github/workflows/ci.yml` | Nag-a-auto-build ng debug/release APK sa bawat push at nag-pu-publish ng release assets. |

### Plugin Files (sa device)

| File | Kailan tumatakbo | Paliwanag |
|---|---|---|
| `module.prop` | Install time | Identity ng plugin. Kung wala ito, hindi kikilalanin ng AxManager ang folder bilang valid plugin. |
| `customize.sh` | Install time lang | I-source (hindi i-execute) ng installer. Gamitin para sa device-specific setup, permissions, o `SKIPUNZIP=1` para full custom install. |
| `post-fs-data.sh` | Boot — first sync | Mag-set ng props, mag-mount ng files, mag-patch ng system bago fully available ang Android. |
| `service.sh` | Boot — late_start | Background processes: CPU governor, thermal settings, network tweaks. Tumatakbo as daemon. |
| `action.sh` | On-demand (user tap) | Interactive script. Output nito ang ipinapakita sa Action screen ng app. Pwedeng mag-toggle ng settings, mag-apply ng profile, etc. |
| `uninstall.sh` | On remove | Mag-undo ng lahat ng binago ng plugin: mag-restore ng stock settings, mag-delete ng files. |
| `system.prop` | Debug mode only | `setprop` values. Hindi katulad ng root mode na mag-aapply ang mga ito sa boot — sa AxManager, debug only ito. |
| `system/bin/` | Available agad pagkatapos ng service.sh | Mga binary na idinagdag sa `PATH`. Pwedeng i-call directly sa kahit anong script nang hindi nagsusulat ng full path. |
| `webroot/index.html` | Pag bukas ng WebUI | Entry point ng plugin UI. Pwedeng mag-load ng external JS frameworks (Vue, React) at mag-communicate pabalik sa Android via KernelSU-compatible JS bridge. |
| `disable` | Check sa bawat boot | Flag file. Kung nandito, lalaktawin ang lahat ng scripts ng plugin. Walang content kailangan — ang presence lang nito ang mahalaga. |
| `remove` | Check sa susunod na boot | Flag file. Nagsi-signal na i-run ang `uninstall.sh` tapos idelete ang buong module folder. |

---

## Summary Flow (End-to-End)

```
[Developer]                    [Device]                   [AxManager]
     │                            │                            │
     │ Build module ZIP           │                            │
     │──────────────────────────►│                            │
     │                            │ Transfer via ADB/Share    │
     │                            │──────────────────────────►│
     │                            │                            │ Validate module.prop
     │                            │                            │ Run customize.sh
     │                            │                            │ Deploy to plugins/
     │                            │                            │ ✓ Installed
     │                            │                            │
     │                            │    [REBOOT or Hot Restart]│
     │                            │◄───────────────────────────
     │                            │                            │
     │                            │ BOOT_COMPLETED             │
     │                            │──────────────────────────►│
     │                            │                            │ post-fs-data.sh
     │                            │                            │ service.sh (daemon)
     │                            │                            │ BusyBox ready
     │                            │                            │ WebUI available
     │                            │                            │
     │                       [User taps Action]               │
     │                            │──────────────────────────►│
     │                            │                            │ action.sh runs
     │                            │◄───────────────────────── │ Output → Action screen
     │                            │                            │
     │                       [User removes plugin]            │
     │                            │──────────────────────────►│
     │                            │                            │ Creates `remove` flag
     │                            │    [Next Boot]             │
     │                            │──────────────────────────►│
     │                            │                            │ uninstall.sh runs
     │                            │                            │ Folder deleted ✓
```

---

*AxManager v1.4.7 · fahrez182/AxManager · Documentation by Claude*
