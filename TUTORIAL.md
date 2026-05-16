<div align="center">
  <br>
  <h1>📦 BETA MANAGER PLUGIN TUTORIAL</h1>
  <p><strong>Gawing tama ang iyong plugin module ZIP para sa Beta Manager</strong></p>
  <p>Magisk/KernelSU-style modules · Root + Shizuku · Shell-level permissions</p>
  <br>
</div>

---

<div align="center">

## 📋 TABLE OF CONTENTS

[🔧 What is a Plugin?](#-what-is-a-plugin) ·
[📁 Folder Structure](#-folder-structure) ·
[📝 module.prop Guide](#-moduleprop-guide) ·
[📜 Script Files](#-script-files) ·
[🌐 WebUI](#-webui) ·
[⚙️ Environment Variables](#️-environment-variables) ·
[📥 Building the ZIP](#-building-the-zip) ·
[📲 Installing](#-installing) ·
[🔬 Testing](#-testing) ·
[🎯 Sample Plugin](#-sample-plugin)

</div>

---

## 🔧 WHAT IS A PLUGIN?

Ang **Beta Manager plugin** ay isang ZIP file na naglalaman ng scripts at binaries na nagma-modify ng system behavior — katulad ng **Magisk/KernelSU modules**. Gumagana ito sa **both rooted at non-rooted devices**.

| Feature | Magisk Module | Beta Manager Plugin |
|---------|--------------|-------------------|
| Root required | ✅ Yes | ❌ **No** (Shizuku kung non-rooted) |
| Runtime dir | `/data/adb/modules/` | `/data/user_de/0/com.android.shell/beta/plugins/` |
| Scanned dirs | `/data/adb/modules/` | Beta + AxManager + Magisk + KSU |
| Permission level | Root (init) | Shell-level (su or Shizuku) |
| Script format | `sh` scripts | `sh` scripts |
| WebUI support | `ksu.exec` bridge | `ksu.exec` bridge |
| BusyBox bundled | Built-in | Built-in |
| Device support | Rooted only | **Rooted + Non-rooted** |

---

## 📁 FOLDER STRUCTURE

Ito ang tamang folder structure ng Beta Manager plugin ZIP:

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
│   └── 📁 bin/             ← 🖥️ Optional — binaries na idadagdag sa PATH
│       ├── my_binary
│       └── ...
└── 📁 webroot/
    └── 📄 index.html       ← 🌐 Optional — WebUI page
```

> ⚠️ **Important:** Ang `module.prop` LANG ang required. Lahat ng iba ay optional.

---

## 📝 MODULE.PROP GUIDE

Ito ang **identity card** ng plugin mo. Kung wala ito, hindi ito makikilala ng Beta Manager.

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
| `id` | ✅ Yes | Unique identifier. Small letters, numbers, underscores, dots, hyphens lang. |
| `name` | ✅ Yes | Display name na lalabas sa app |
| `version` | ✅ Yes | Version string (e.g. `v1.0.0`) |
| `versionCode` | ✅ Yes | Integer version para sa comparison |
| `author` | ✅ Yes | Pangalan ng developer |
| `description` | ✅ Yes | Maikling description ng plugin |

### Version Compatibility

```properties
betaPlugin=1
```

| Field | Required | Description |
|-------|----------|-------------|
| `betaPlugin` | Recommended | Minimum Beta Manager version na kailangan (`10001` = v1.0.0) |
| `axeronPlugin` | Alternative | Alternative para sa AxManager compatibility |

> 💡 **Tip:** Kung gusto mong gumana ang plugin sa **both Beta Manager at AxManager**, maglagay ka ng `betaPlugin` **at** `axeronPlugin`:
> ```properties
> betaPlugin=10001
> axeronPlugin=10001
> ```

### Rules for `id`

✅ **Allowed:**
- `my_module`
- `game.booster.v2`
- `cpu_tweaks`

❌ **Not Allowed:**
- `My Module` (may space)
- `123module` (nagsisimula sa number)
- `module!` (may special character)

---

## 📜 SCRIPT FILES

### `customize.sh` — Install Script

Tumatakbo **isang beses lang** habang nag-iinstall ng plugin.

```sh
#!/system/bin/sh

# customize.sh — runs during installation
# Available variables: MODPATH, ARCH, API, BETA

echo "Installing for $ARCH device..."

# Set permissions sa binaries
chmod 755 $MODPATH/system/bin/*

# Conditional based on architecture
if [ "$ARCH" = "arm64" ]; then
    mv $MODPATH/system/bin/optimizer64 $MODPATH/system/bin/optimizer
else
    mv $MODPATH/system/bin/optimizer32 $MODPATH/system/bin/optimizer
fi

# Remove unwanted files
rm -f $MODPATH/system/bin/optimizer64
rm -f $MODPATH/system/bin/optimizer32
```

| Variable | Description |
|----------|-------------|
| `MODPATH` | Install target path |
| `ARCH` | CPU architecture (`arm`/`arm64`/`x86`/`x64`) |
| `API` | Android API level |
| `BETA` | Always `true` under Beta Manager |
| `AXERON` | Always `true` for AxManager compat |

> ⚠️ **Note:** Ang `customize.sh` ay **na-ssource (sourced)** hindi nai-execute. Ibig sabihin, kung mag-e-exit ka gamit `exit`, mamamatay ang buong installer.

---

### `post-fs-data.sh` — Early Boot Script

Tumatakbo **sa boot**, bago pa fully loaded ang Android. Gamitin para sa:

```sh
#!/system/bin/sh

# post-fs-data.sh — runs early at boot

# Set system properties
resetprop debug.sf.nobootanimation 1
resetprop ro.config.hw_quickpoweron true

# Mount tweaks
mount -o remount,noatime /data

# Patch build.prop values
setprop persist.sys.power_saving 0
```

> ⚠️ **Limitado ang kaya gawin dito** dahil ADB-level permissions lang.  
> Hindi lahat ng `resetprop` o `mount` commands ay gagana without root.

---

### `service.sh` — Background Service Script

Tumatakbo **pagkatapos ng boot**, sa background. Ito ang **pinaka-importante** para sa performance plugins.

```sh
#!/system/bin/sh

# service.sh — background daemon (late_start)
# Runs even when screen is off

MODDIR=${0%/*}

# Wait for system to settle
sleep 15

# Apply CPU governor tweaks
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    echo "performance" > $cpu 2>/dev/null
done

# GPU boost
echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null
echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null
echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null

# Memory optimization
echo 10 > /proc/sys/vm/swappiness 2>/dev/null
echo 100 > /proc/sys/vm/vfs_cache_pressure 2>/dev/null

# Run indefinitely (watchdog loop)
while true; do
    # Re-apply tweaks every 60 seconds
    sleep 60
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        echo "performance" > $cpu 2>/dev/null
    done
done
```

---

### `action.sh` — On-Demand Action Script

Tumatakbo kapag pinindot ng user ang **"Action" button** sa app.

```sh
#!/system/bin/sh

# action.sh — triggered by user tap
# Output will be displayed in the Action screen

echo "=== Performance Stats ==="
echo ""

# CPU info
echo "CPU Governor(s):"
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    echo "  $(basename $(dirname $cpu)): $(cat $cpu 2>/dev/null)"
done

# GPU info
echo ""
echo "GPU:"
echo "  Max freq: $(cat /sys/class/kgsl/kgsl-3d0/max_gpuclk 2>/dev/null || echo 'N/A')"

# Temperature
echo ""
echo "Temperature:"
cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null | while read temp; do
    echo "  Zone: $temp°C"
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
    echo "schedutil" > $cpu 2>/dev/null
done

# Restore GPU
echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null

echo "Plugin removed. Tweaks reverted."
```

---

## 🌐 WEBUI

Pwede kang magdagdag ng interactive UI para sa plugin mo gamit ang `webroot/index.html`.

### File Structure

```
webroot/
└── index.html
```

### JavaScript Bridge

Ang Beta Manager ay may **KernelSU-compatible** JS bridge:

| Function | Description |
|----------|-------------|
| `ksu.exec(cmd)` | Mag-execute ng shell command at return ang output |
| `ksu.toast(msg)` | Magpakita ng Android toast notification |
| `ksu.fullScreen(bool)` | Toggle fullscreen mode |

### Sample WebUI

```html
<!DOCTYPE html>
<html>
<head>
    <title>My Plugin</title>
    <style>
        body {
            background: #0D0D14;
            color: #E8E8F0;
            font-family: monospace;
            padding: 20px;
        }
        h1 { color: #00E5FF; }
        button {
            background: #00E5FF;
            color: #0D0D14;
            border: none;
            padding: 10px 20px;
            border-radius: 8px;
            font-weight: bold;
        }
        pre {
            background: #1A1A26;
            padding: 10px;
            border-radius: 8px;
        }
    </style>
</head>
<body>
    <h1>⚡ My Performance Plugin</h1>
    <button onclick="applyBoost()">Apply Boost</button>
    <pre id="output"></pre>

    <script>
        function applyBoost() {
            document.getElementById('output').textContent = 'Applying...';
            const result = ksu.exec('for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > $cpu 2>/dev/null; done');
            document.getElementById('output').textContent = result;
            ksu.toast('Boost applied!');
        }
    </script>
</body>
</html>
```

---

## ⚙️ ENVIRONMENT VARIABLES

Available sa **lahat ng scripts** ng plugin mo:

<div align="center">

| Variable | Value | Description |
|----------|-------|-------------|
| `BETA` | `true` | ✅ Confirms running under Beta Manager |
| `BETAVER` | `int` | 📊 Server version (e.g. `10001`) |
| `AXERON` | `true` | 🔄 AxManager compatibility flag |
| `AXERONVER` | `int` | 🔄 AxManager version |
| `MODDIR` | `path` | 📂 Base path ng module — **laging gamitin ito** |
| `MODPATH` | `path` | 📂 Install target (customize.sh only) |
| `ARCH` | `arm/arm64/x86/x64` | 🖥️ CPU architecture |
| `IS64BIT` | `true/false` | 🔢 64-bit check |
| `API` | `int` | 📱 Android API level |
| `BOOTMODE` | `true` | 🚀 Always true under Beta Manager |

</div>

> 💡 **Best Practice:** Always use `$MODDIR` sa scripts para makuha ang base path ng plugin:
> ```sh
> MODDIR=${0%/*}
> echo "Plugin located at: $MODDIR"
> ```

---

## 📥 BUILDING THE ZIP

### Method 1: Manual (Terminal)

```bash
# Create folder structure
mkdir -p my_plugin/system/bin
mkdir -p my_plugin/webroot

# Create module.prop
cat > my_plugin/module.prop << 'EOF'
id=my_perf_module
name=My Performance Module
version=v1.0.0
versionCode=1
author=YourName
description=CPU/GPU optimization module
betaPlugin=10001
EOF

# Create scripts
cat > my_plugin/service.sh << 'EOF'
#!/system/bin/sh
MODDIR=${0%/*}
echo "Service running"
EOF

# Make scripts executable
chmod +x my_plugin/*.sh

# Create ZIP
cd my_plugin
zip -r ../my_perf_module.zip ./*
cd ..
```

### Method 2: Using build script

Create `build.sh` sa project folder:

```bash
#!/bin/bash
# build.sh — build plugin ZIP

PLUGIN_ID="my_perf_module"
VERSION="v1.0.0"

echo "Building $PLUGIN_ID $VERSION..."

# Create temp directory
mkdir -p build/$PLUGIN_ID/system/bin
mkdir -p build/$PLUGIN_ID/webroot

# Copy files
cp module.prop build/$PLUGIN_ID/
cp service.sh build/$PLUGIN_ID/ 2>/dev/null
cp action.sh build/$PLUGIN_ID/ 2>/dev/null
cp webroot/index.html build/$PLUGIN_ID/webroot/ 2>/dev/null

# Set permissions
chmod 644 build/$PLUGIN_ID/module.prop
chmod 755 build/$PLUGIN_ID/*.sh 2>/dev/null

# Create ZIP
cd build
zip -r ../${PLUGIN_ID}_${VERSION}.zip $PLUGIN_ID/
cd ..

# Cleanup
rm -rf build

echo "✅ Done: ${PLUGIN_ID}_${VERSION}.zip"
```

---

## 📲 INSTALLING

### Via Beta Manager App

1. Buksan ang Beta Manager
2. **Auto-activate** ang service (Root→`su`, Non-root→**Shizuku**)
3. Sa **Home Dashboard**, i-tap ang **Install** quick action
4. Piliin ang ZIP file ng module
5. Pumili ng **Flash Target**:
   - 🔵 **Beta Manager** — install sa `/data/user_de/0/com.android.shell/beta/plugins/`
   - 🟢 **Magisk Modules** — flash sa `/data/adb/modules/`
   - 🟣 **KernelSU Modules** — flash sa `/data/adb/ksu/modules/`
6. Hintaying mag-success ang installation
7. I-enable ang module sa dashboard

### Via ADB (Manual)

```bash
# Push plugin sa device
adb push my_perf_module.zip /sdcard/

# I-install via shell
adb shell "cp /sdcard/my_perf_module.zip /data/user_de/0/com.android.shell/beta/ && unzip /data/user_de/0/com.android.shell/beta/my_perf_module.zip -d /data/user_de/0/com.android.shell/beta/plugins/my_perf_module/"
```

---

## 🔬 TESTING

### Check if plugin is loaded

```bash
adb shell "ls -la /data/user_de/0/com.android.shell/beta/plugins/"
```

### Check service logs

```bash
adb shell "cat /data/user_de/0/com.android.shell/beta/logs/*.log"
```

O di kaya sa app mismo: Home → **Logs** quick action → view service logs. Like Magisk Log.

### Test action.sh manually

```bash
adb shell "sh /data/user_de/0/com.android.shell/beta/plugins/my_perf_module/action.sh"
```

---

## 🎯 SAMPLE PLUGIN — CPU BOOST

Ang reference sample ay nasa [`cpu boost/`](cpu%20boost/) directory ng repo. Buuin mo gamit ang:

```bash
cd "cpu boost"
chmod +x *.sh
./build.sh
# → cpu_boost_v2.0.0.zip
```

### `module.prop`

```properties
id=cpu_boost
name=CPU Performance Booster
version=v2.0.0
versionCode=2
author=Willy Jr. C. Gailo
description=CPU governor tweaks, GPU boost, memory optimization, thermal control
betaPlugin=10001
axeronPlugin=10001
```

### `service.sh`

May watchdog loop na nagre-re-apply ng tweaks every 60 seconds:

```sh
#!/system/bin/sh
MODDIR=${0%/*}
LOG="/data/user_de/0/com.android.shell/beta/logs/cpu_boost.log"

log() { echo "[$(date '+%H:%M:%S')] $1" >> "$LOG" 2>/dev/null; }
log "CPU Booster starting..."

sleep 20

# CPU
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
  echo "performance" > "$cpu" 2>/dev/null
done

# GPU
echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null
echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null
echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null

# Memory
echo 0 > /proc/sys/vm/swappiness 2>/dev/null
echo 1 > /proc/sys/vm/compact_memory 2>/dev/null

# Thermal
echo "disabled" > /sys/class/thermal/thermal_message/sconfig 2>/dev/null

# Watchdog
while true; do sleep 60; done
```

Full version with logging: [`cpu boost/service.sh`](cpu%20boost/service.sh)

### `action.sh`

Nagpi-print ng live status ng CPU governors, GPU, memory, temperatura:

```sh
#!/system/bin/sh
echo ""
echo "╔══════════════════════════════════════╗"
echo "║    CPU PERFORMANCE BOOSTER v2.0    ║"
echo "╚══════════════════════════════════════╝"
echo ""
echo "≡ CPU Governors:"
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
  name=$(basename $(dirname "$cpu"))
  echo "  ${name}: $(cat "$cpu" 2>/dev/null)"
done
echo ""
echo "≡ GPU Status:"
echo "  Force bus: $(cat /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null)"
echo ""
echo "≡ Temperature:"
for zone in /sys/class/thermal/thermal_zone*/temp; do
  temp=$(cat "$zone" 2>/dev/null)
  echo "  $((temp / 1000))°C"
done
```

Full version: [`cpu boost/action.sh`](cpu%20boost/action.sh)

### `webroot/index.html`

Interactive WebUI may toggle switches para sa CPU/GPU/Memory/Thermal at live stats:

![WebUI](https://img.shields.io/badge/WebUI-KernelSU_compatible-00E5FF)

Open sa Beta Manager → tap **WebUI** button sa plugin card.

Full source: [`cpu boost/webroot/index.html`](cpu%20boost/webroot/index.html)

---

## ✅ PLUGIN CHECKLIST

Bago mo i-release ang plugin mo, tiyakin na:

- [ ] May `module.prop` na may kumpletong fields
- [ ] Ang `id` ay valid format (no spaces, no special chars)
- [ ] Ang `betaPlugin` version ay tama (`<= 10001`)
- [ ] Pumili ng tamang **Flash Target** (Beta / Magisk / KSU)
- [ ] Lahat ng `.sh` files ay **executable** (`chmod +x`)
- [ ] Lahat ng scripts ay may **shebang** (`#!/system/bin/sh`)
- [ ] Ginagamit ang `$MODDIR` imbes na hardcoded paths
- [ ] May `uninstall.sh` para mag-restore ng stock settings
- [ ] May logging sa `/data/user_de/0/com.android.shell/beta/logs/`
- [ ] Ang ZIP ay **directly zipped** (hindi naka-folder sa loob)
- [ ] Build gamit `build.sh` at test sa Beta Manager app

---

> 💡 **Reference plugin:** Tingnan ang [`cpu boost/`](cpu%20boost/) directory sa repo — complete working sample.

---

<div align="center">
  <hr>
  <p>
    <strong>⚡ BETA MANAGER v1.2.0</strong> — <em>Universal Module Manager (Root + Non-rooted)</em>
  </p>
  <p>
    <a href="https://github.com/willygailo/BETA-MANAGER">GitHub</a> ·
    <a href="README.md">README</a> ·
    <a href="cpu%20boost/build.sh">Sample Plugin</a>
  </p>
  <br>
</div>
