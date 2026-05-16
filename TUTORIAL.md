<div align="center">
  <br>
  <h1>📦 BETA MANAGER PLUGIN TUTORIAL</h1>
  <p><strong>Gawing tama ang iyong plugin module ZIP para sa Beta Manager v1.3.0</strong></p>
  <p>Magisk/KSU/APatch/Axeron-style modules · Root + Shizuku · Android 8–16</p>
  <br>
</div>

---

<div align="center">

## 📋 TABLE OF CONTENTS

[🔧 What is a Plugin?](#-what-is-a-plugin) ·
[📁 Folder Structure](#-folder-structure) ·
[📝 module.prop Guide](#-moduleprop-guide) ·
[📜 Script Files](#-script-files) ·
[🎯 Flash Targets](#-flash-targets) ·
[🌐 WebUI](#-webui) ·
[⚙️ Environment Variables](#️-environment-variables) ·
[📥 Building the ZIP](#-building-the-zip) ·
[📲 Installing](#-installing) ·
[🔬 Testing](#-testing) ·
[🎯 Sample Plugin](#-sample-plugin)

</div>

---

## 🔧 WHAT IS A PLUGIN?

Ang **Beta Manager plugin** ay isang ZIP file na naglalaman ng scripts at binaries na nagma-modify ng system behavior — katulad ng **Magisk/KernelSU/APatch modules**. Gumagana ito sa **both rooted at non-rooted devices**.

| Feature | Magisk Module | Beta Manager Plugin |
|---------|--------------|-------------------|
| Root required | ✅ Yes | ❌ **No** (Shizuku kung non-rooted) |
| Runtime dir | `/data/adb/modules/` | `/data/user_de/0/com.android.shell/beta/plugins/` |
| Scanned sources | Magisk only | **6 sources**: Beta + AxManager + Magisk + KSU + APatch + Axeron |
| Permission level | Root (init) | Shell-level (su or Shizuku) |
| Script format | `sh` scripts | `sh` scripts (same format) |
| WebUI support | `ksu.exec` bridge | `ksu.exec` bridge (compatible) |
| Flash targets | 1 | **5**: Beta / Magisk / KSU / APatch / Axeron |
| Device support | Rooted only | **Android 8–16, Rooted + Non-rooted** |

---

## 📁 FOLDER STRUCTURE

```
my_plugin.zip
│
├── 📄 module.prop          ← 📌 REQUIRED — identity ng plugin
├── 📄 customize.sh         ← 🛠️ Optional — custom installer
├── 📄 post-fs-data.sh      ← 🔄 Optional — early boot script
├── 📄 service.sh           ← ⚡ Optional — background daemon
├── 📄 action.sh            ← 🎯 Optional — on-demand action
├── 📄 uninstall.sh         ← 🗑️ Optional — cleanup sa removal
├── 📁 system/
│   └── 📁 bin/             ← 🖥️ Optional — binaries added to PATH
│       ├── my_binary
│       └── ...
└── 📁 webroot/
    └── 📄 index.html       ← 🌐 Optional — WebUI page
```

> ⚠️ **Important:** Ang `module.prop` LANG ang required. Lahat ng iba ay optional.

> 💡 Ang ZIP ay dapat **directly zipped** — ibig sabihin kapag i-unzip mo, lalabas agad ang `module.prop`, hindi nakapaloob sa isang folder.

---

## 📝 MODULE.PROP GUIDE

Ito ang **identity card** ng plugin mo.

### Required Fields

```properties
id=my_perf_module
name=My Performance Module
version=v1.0.0
versionCode=1
author=YourName
description=CPU/GPU optimization module
```

| Field | Required | Description |
|-------|----------|-------------|
| `id` | ✅ Yes | Unique ID. Lowercase letters, numbers, underscores, dots, hyphens lang. |
| `name` | ✅ Yes | Display name sa app |
| `version` | ✅ Yes | Version string (e.g. `v1.0.0`) |
| `versionCode` | ✅ Yes | Integer version |
| `author` | ✅ Yes | Pangalan ng developer |
| `description` | ✅ Yes | Maikling description |

### Version Compatibility Fields

```properties
betaPlugin=10004
axeronPlugin=10004
```

| Field | Description |
|-------|-------------|
| `betaPlugin` | Minimum Beta Manager version code (`10004` = v1.3.0) |
| `axeronPlugin` | Same but for AxManager compatibility |

> 💡 Lagyan mo ng **parehong fields** para gumana sa Beta Manager **at** AxManager:
> ```properties
> betaPlugin=10004
> axeronPlugin=10004
> ```

### Rules for `id`

✅ **Allowed:** `my_module`, `game.booster.v2`, `cpu_tweaks`  
❌ **Not Allowed:** `My Module` (space), `123module` (starts with number), `module!` (special char)

---

## 📜 SCRIPT FILES

### `customize.sh` — Install Script

Tumatakbo **isang beses lang** habang nag-iinstall.

```sh
#!/system/bin/sh
# Available: MODPATH, ARCH, API, BETA, AXERON

echo "Installing for $ARCH on Android API $API..."

# Set permissions
chmod 755 "$MODPATH/system/bin/"*

# Architecture conditional
if [ "$ARCH" = "arm64" ]; then
    mv "$MODPATH/system/bin/optimizer64" "$MODPATH/system/bin/optimizer"
else
    mv "$MODPATH/system/bin/optimizer32" "$MODPATH/system/bin/optimizer"
fi
rm -f "$MODPATH/system/bin/optimizer64" "$MODPATH/system/bin/optimizer32"
```

> ⚠️ Ang `customize.sh` ay **na-ssource (sourced)** — huwag gumamit ng bare `exit` or mamamatay ang installer.

---

### `post-fs-data.sh` — Early Boot Script

Tumatakbo **sa boot**, bago pa fully loaded ang Android.

```sh
#!/system/bin/sh
# post-fs-data.sh — runs early at boot

# Set system properties (root only)
resetprop debug.sf.nobootanimation 1
resetprop ro.config.hw_quickpoweron true

# Patch values
setprop persist.sys.power_saving 0
```

> ⚠️ Limitado ang kaya gawin dito — ADB/shell-level permissions lang.

---

### `service.sh` — Background Daemon

Tumatakbo **pagkatapos ng boot**, sa background. Pinaka-importante para sa performance plugins.

```sh
#!/system/bin/sh
# service.sh — background daemon (late_start)

MODDIR=${0%/*}
LOG="/data/user_de/0/com.android.shell/beta/logs/$(basename $MODDIR).log"

log() { echo "[$(date '+%H:%M:%S')] $1" >> "$LOG" 2>/dev/null; }
log "Service starting..."

# Wait for system to settle
sleep 20

# CPU governor
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    echo "performance" > "$cpu" 2>/dev/null
done

# GPU boost (Snapdragon/Adreno)
echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null
echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null
echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null

# Memory optimization
echo 0 > /proc/sys/vm/swappiness 2>/dev/null
echo 1 > /proc/sys/vm/compact_memory 2>/dev/null

log "Tweaks applied. Running watchdog..."

# Watchdog loop
while true; do sleep 60; done
```

---

### `action.sh` — On-Demand Action

Tumatakbo kapag pinindot ng user ang **"Action"** button sa plugin card.

```sh
#!/system/bin/sh
# action.sh — triggered by user tap

echo "╔══════════════════════════════╗"
echo "║   PERFORMANCE STATUS         ║"
echo "╚══════════════════════════════╝"
echo ""

echo "≡ CPU Governors:"
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    name=$(basename "$(dirname "$cpu")")
    echo "  ${name}: $(cat "$cpu" 2>/dev/null)"
done

echo ""
echo "≡ GPU (Adreno):"
echo "  Force bus: $(cat /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null || echo N/A)"
echo "  Max freq:  $(cat /sys/class/kgsl/kgsl-3d0/max_gpuclk 2>/dev/null || echo N/A)"

echo ""
echo "≡ Temperatures:"
for zone in /sys/class/thermal/thermal_zone*/temp; do
    temp=$(cat "$zone" 2>/dev/null)
    [ -n "$temp" ] && echo "  $((temp / 1000))°C"
done

echo ""
echo "=== Done ==="
```

---

### `uninstall.sh` — Cleanup Script

Tumatakbo bago idelete ang plugin folder.

```sh
#!/system/bin/sh
# uninstall.sh — cleanup on remove

# Restore default CPU governor
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    echo "schedutil" > "$cpu" 2>/dev/null
done

# Restore GPU
echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null
echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null

echo "Plugin removed. Tweaks reverted."
```

---

## 🎯 FLASH TARGETS

Kapag mag-iinstall ka ng plugin, may 5 options sa **FlashMode chip selector** ng Plugins screen:

| Chip | Target Path | Para sa |
|------|-------------|---------|
| 🔵 **BETA** | `/data/user_de/0/com.android.shell/beta/plugins/` | Beta Manager only |
| 🟢 **MAGISK** | `/data/adb/modules/` | Magisk rooted devices |
| 🟣 **KSU** | `/data/adb/ksu/modules/` | KernelSU rooted devices |
| 🟡 **APATCH** | `/data/adb/apatch/modules/` | APatch rooted devices |
| 🟠 **AXERON** | `/data/adb/axeron/modules/` | Axeron Manager devices |

> 💡 Para sa maximum compatibility, i-install sa **BETA** target — lahat ng device types ang susuportahan.

---

## 🌐 WEBUI

Pwede kang magdagdag ng interactive HTML UI para sa plugin mo.

```
webroot/
└── index.html
```

### JavaScript Bridge

| Function | Description |
|----------|-------------|
| `ksu.exec(cmd)` | Execute shell command, returns output |
| `ksu.toast(msg)` | Show Android toast notification |
| `ksu.fullScreen(bool)` | Toggle fullscreen mode |

### Sample WebUI

```html
<!DOCTYPE html>
<html>
<head>
    <title>My Plugin</title>
    <style>
        body { background: #0D0D14; color: #E8E8F0; font-family: monospace; padding: 20px; }
        h1 { color: #00E5FF; }
        button {
            background: #00E5FF; color: #0D0D14;
            border: none; padding: 10px 20px;
            border-radius: 8px; font-weight: bold; cursor: pointer;
        }
        pre { background: #1A1A26; padding: 10px; border-radius: 8px; }
    </style>
</head>
<body>
    <h1>⚡ My Performance Plugin</h1>
    <button onclick="applyBoost()">Apply Boost</button>
    <pre id="output"></pre>

    <script>
        function applyBoost() {
            document.getElementById('output').textContent = 'Applying...';
            const result = ksu.exec(
                'for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; ' +
                'do echo performance > $cpu 2>/dev/null; done && echo Done'
            );
            document.getElementById('output').textContent = result;
            ksu.toast('Boost applied!');
        }
    </script>
</body>
</html>
```

---

## ⚙️ ENVIRONMENT VARIABLES

Available sa **lahat ng scripts**:

| Variable | Value | Description |
|----------|-------|-------------|
| `BETA` | `true` | Confirms running under Beta Manager |
| `BETAVER` | `10004` | Server version (v1.3.0) |
| `AXERON` | `true` | AxManager compatibility flag |
| `AXERONVER` | `10004` | AxManager version |
| `MODDIR` | path | Base path ng module — **laging gamitin** |
| `MODPATH` | path | Install target (customize.sh only) |
| `ARCH` | arm/arm64/x86/x64 | CPU architecture |
| `IS64BIT` | true/false | 64-bit check |
| `API` | int | Android API level (26–36) |
| `BOOTMODE` | `true` | Always true under Beta Manager |

> 💡 **Best Practice:** Laging gamitin `$MODDIR` para sa relative paths:
> ```sh
> MODDIR=${0%/*}
> source "$MODDIR/lib/utils.sh"
> ```

---

## 📥 BUILDING THE ZIP

### Method 1: Manual (Terminal)

```bash
mkdir -p my_plugin/system/bin my_plugin/webroot

cat > my_plugin/module.prop << 'EOF'
id=my_perf_module
name=My Performance Module
version=v1.0.0
versionCode=1
author=YourName
description=CPU/GPU optimization module
betaPlugin=10004
axeronPlugin=10004
EOF

cat > my_plugin/service.sh << 'EOF'
#!/system/bin/sh
MODDIR=${0%/*}
sleep 20
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    echo performance > "$cpu" 2>/dev/null
done
EOF

chmod +x my_plugin/*.sh

cd my_plugin && zip -r ../my_perf_module_v1.0.0.zip ./* && cd ..
```

### Method 2: build.sh Script

```bash
#!/bin/bash
# build.sh

PLUGIN_ID="my_perf_module"
VERSION="v1.0.0"

echo "Building $PLUGIN_ID $VERSION..."
mkdir -p "build/$PLUGIN_ID/system/bin" "build/$PLUGIN_ID/webroot"

cp module.prop "build/$PLUGIN_ID/"
cp service.sh "build/$PLUGIN_ID/" 2>/dev/null || true
cp action.sh "build/$PLUGIN_ID/" 2>/dev/null || true
cp uninstall.sh "build/$PLUGIN_ID/" 2>/dev/null || true
cp -r webroot/ "build/$PLUGIN_ID/" 2>/dev/null || true

chmod 644 "build/$PLUGIN_ID/module.prop"
chmod 755 "build/$PLUGIN_ID/"*.sh 2>/dev/null || true

cd build && zip -r "../${PLUGIN_ID}_${VERSION}.zip" "$PLUGIN_ID/" && cd ..
rm -rf build

echo "✅ Done: ${PLUGIN_ID}_${VERSION}.zip"
```

---

## 📲 INSTALLING

### Via Beta Manager App

1. Buksan ang **Beta Manager**
2. Auto-activate ang service (Root → `su`, Non-root → **Shizuku**)
3. Sa **Plugins screen**, tingnan ang **FlashMode chip selector** sa taas:
   - Piliin: 🔵 BETA / 🟢 MAGISK / 🟣 KSU / 🟡 APATCH / 🟠 AXERON
4. I-tap ang **Install Plugin** button (FAB sa ibaba)
5. Piliin ang ZIP file
6. Hintaying mag-success ang installation
7. I-enable ang module sa dashboard

### Via ADB (Manual)

```bash
# Push ZIP
adb push my_perf_module.zip /sdcard/

# Install manually to Beta dir
adb shell "
  DEST=/data/user_de/0/com.android.shell/beta/plugins/my_perf_module
  mkdir -p \$DEST
  unzip -o /sdcard/my_perf_module.zip -d \$DEST
  chmod +x \$DEST/*.sh 2>/dev/null
  echo Done
"
```

---

## 🔬 TESTING

### Check plugin loaded

```bash
adb shell "ls -la /data/user_de/0/com.android.shell/beta/plugins/"
```

### Check service logs

```bash
adb shell "ls /data/user_de/0/com.android.shell/beta/logs/ && cat /data/user_de/0/com.android.shell/beta/logs/*.log 2>/dev/null"
```

O sa app: Home → **Logs** quick action.

### Test action.sh manually

```bash
adb shell "sh /data/user_de/0/com.android.shell/beta/plugins/my_perf_module/action.sh"
```

### Check module.prop parsing

```bash
adb shell "cat /data/user_de/0/com.android.shell/beta/plugins/my_perf_module/module.prop"
```

---

## 🎯 SAMPLE PLUGIN — CPU BOOST

Reference plugin nasa [`cpu boost/`](cpu%20boost/) directory ng repo:

```bash
cd "cpu boost"
chmod +x *.sh
./build.sh
# → cpu_boost_v2.0.0.zip
```

### module.prop

```properties
id=cpu_boost
name=CPU Performance Booster
version=v2.0.0
versionCode=2
author=Willy Jr. C. Gailo
description=CPU governor tweaks, GPU boost, memory optimization, thermal control
betaPlugin=10004
axeronPlugin=10004
```

### service.sh (excerpt)

```sh
#!/system/bin/sh
MODDIR=${0%/*}
LOG="/data/user_de/0/com.android.shell/beta/logs/cpu_boost.log"
log() { echo "[$(date '+%H:%M:%S')] $1" >> "$LOG" 2>/dev/null; }
log "CPU Booster v2.0 starting..."
sleep 20

for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
  echo "performance" > "$cpu" 2>/dev/null
done

echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null
echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null
echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null
echo 0 > /proc/sys/vm/swappiness 2>/dev/null

log "Tweaks applied."
while true; do sleep 60; done
```

---

## ✅ PLUGIN CHECKLIST

Bago mo i-release ang plugin mo:

- [ ] May `module.prop` na may kumpletong required fields
- [ ] Ang `id` ay valid format (no spaces, no special chars)
- [ ] `betaPlugin=10004` at `axeronPlugin=10004` ay naka-set
- [ ] Pinili ang tamang **Flash Target** sa FlashMode chips
- [ ] Lahat ng `.sh` files ay **executable** (`chmod +x`)
- [ ] Lahat ng scripts ay may **shebang** (`#!/system/bin/sh`)
- [ ] Ginagamit ang `$MODDIR` imbes na hardcoded paths
- [ ] May `uninstall.sh` para mag-restore ng stock settings
- [ ] May logging sa `/data/user_de/0/com.android.shell/beta/logs/`
- [ ] Ang ZIP ay **directly zipped** (hindi naka-folder sa loob)
- [ ] Tested sa Beta Manager app (enable/disable/action/remove)

---

> 💡 **Reference plugin:** Tingnan ang [`cpu boost/`](cpu%20boost/) directory — complete working sample.

---

<div align="center">
  <hr>
  <p>
    <strong>⚡ BETA MANAGER v1.3.0</strong> — <em>Universal Module Manager (Android 8–16, Root + Non-rooted)</em>
  </p>
  <p>
    <a href="https://github.com/willygailo/BETA-MANAGER">GitHub</a> ·
    <a href="README.md">README</a> ·
    <a href="cpu%20boost/build.sh">Sample Plugin</a>
  </p>
  <br>
</div>
