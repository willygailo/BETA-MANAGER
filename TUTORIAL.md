<div align="center">
  <br>
  <h1>📦 BETA MANAGER PLUGIN TUTORIAL</h1>
  <p><strong>Gawing tama ang iyong plugin module ZIP para sa Beta Manager</strong></p>
  <p>Magisk/KernelSU-style modules · No root required · ADB-level permissions</p>
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

Ang **Beta Manager plugin** ay isang ZIP file na naglalaman ng scripts at binaries na nagma-modify ng system behavior — katulad ng **Magisk/KernelSU modules**, pero **walang kailangan na root**.

| Feature | Magisk Module | Beta Manager Plugin |
|---------|--------------|-------------------|
| Root required | ✅ Yes | ❌ **No** |
| Runtime dir | `/data/adb/modules/` | `/data/user_de/0/com.android.shell/beta/plugins/` |
| Permission level | Root (init) | ADB (`com.android.shell`) |
| Script format | `sh` scripts | `sh` scripts |
| WebUI support | `ksu.exec` bridge | `ksu.exec` bridge |
| BusyBox bundled | Built-in | Built-in |

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
2. I-activate ang service (Wireless Debugging / ADB / Root)
3. Pumunta sa **Plugins** tab
4. I-tap ang **"Install ZIP"** button
5. Piliin ang plugin ZIP file
6. Hintaying mag-success ang installation
7. I-enable ang plugin sa plugin list

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

### Test action.sh manually

```bash
adb shell "sh /data/user_de/0/com.android.shell/beta/plugins/my_perf_module/action.sh"
```

---

## 🎯 SAMPLE PLUGIN

Narito ang kumpletong halimbawa ng **Game Performance Booster** plugin:

### `module.prop`

```properties
id=game_booster
name=Game Performance Booster
version=v1.0.0
versionCode=1
author=Willy Jr. C. Gailo
description=CPU/GPU/memory tweaks para sa gaming
betaPlugin=10001
```

### `service.sh`

```sh
#!/system/bin/sh

MODDIR=${0%/*}

sleep 10

# CPU: Performance governor
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    echo "performance" > $cpu 2>/dev/null
done

# GPU: Force max performance
echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null
echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null
echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null
echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null

# Memory: Aggressive
echo 0 > /proc/sys/vm/swappiness 2>/dev/null
echo 50 > /proc/sys/vm/dirty_ratio 2>/dev/null
echo 30 > /proc/sys/vm/dirty_background_ratio 2>/dev/null
echo 1 > /proc/sys/vm/compact_memory 2>/dev/null

# Scheduler: Reduce latency
echo "noop" > /sys/block/mmcblk0/queue/scheduler 2>/dev/null
echo 128 > /sys/block/mmcblk0/queue/read_ahead_kb 2>/dev/null

# Thermal: Disable throttling (if available)
echo "disabled" > /sys/class/thermal/thermal_message/sconfig 2>/dev/null
echo 0 > /sys/class/thermal/thermal_zone0/mode 2>/dev/null

# Monitor loop
while true; do
    sleep 30
    # Re-apply CPU governor
    for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
        CURRENT=$(cat $cpu 2>/dev/null)
        if [ "$CURRENT" != "performance" ]; then
            echo "performance" > $cpu 2>/dev/null
        fi
    done
done
```

### `action.sh`

```sh
#!/system/bin/sh

echo "=== GAME BOOSTER STATUS ==="
echo ""

echo "CPU Governors:"
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    echo "  $(basename $(dirname $cpu)): $(cat $cpu 2>/dev/null)"
done

echo ""
echo "GPU:"
echo "  Force bus: $(cat /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null)"
echo "  Force clk: $(cat /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null)"

echo ""
echo "Memory:"
echo "  Swappiness: $(cat /proc/sys/vm/swappiness 2>/dev/null)"

echo ""
echo "Temperature:"
cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null | while read temp; do
    echo "  $((temp / 1000))°C"
done

echo ""
echo "=== DONE ==="
```

### `webroot/index.html`

```html
<!DOCTYPE html>
<html>
<head>
    <title>Game Booster</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            background: #0D0D14;
            color: #E8E8F0;
            font-family: 'Courier New', monospace;
            padding: 20px;
            text-align: center;
        }
        h1 { color: #00E5FF; font-size: 24px; margin: 20px 0; }
        .card {
            background: #1A1A26;
            border-radius: 12px;
            padding: 20px;
            margin: 10px 0;
        }
        .stat {
            display: inline-block;
            margin: 10px;
            padding: 15px;
            background: #252536;
            border-radius: 8px;
            min-width: 120px;
        }
        .stat-value { color: #00E5FF; font-size: 28px; font-weight: bold; }
        .stat-label { color: #6E6E8A; font-size: 12px; margin-top: 4px; }
        button {
            background: linear-gradient(135deg, #00E5FF, #7C4DFF);
            color: #fff;
            border: none;
            padding: 15px 40px;
            border-radius: 12px;
            font-size: 16px;
            font-weight: bold;
            cursor: pointer;
            margin: 10px;
        }
        button:active { transform: scale(0.95); }
        pre {
            background: #0D0D14;
            padding: 15px;
            border-radius: 8px;
            text-align: left;
            font-size: 12px;
            color: #9E9EB8;
            margin-top: 15px;
        }
    </style>
</head>
<body>
    <h1>⚡ GAME BOOSTER</h1>
    <div class="card">
        <button onclick="applyBoost()">APPLY BOOST</button>
        <button onclick="getStatus()" style="background: #252536;">GET STATUS</button>
    </div>
    <div class="card">
        <div class="stat">
            <div class="stat-value" id="cpuStat">--</div>
            <div class="stat-label">CPU Governor</div>
        </div>
        <div class="stat">
            <div class="stat-value" id="gpuStat">--</div>
            <div class="stat-label">GPU State</div>
        </div>
        <div class="stat">
            <div class="stat-value" id="tempStat">--</div>
            <div class="stat-label">Temperature</div>
        </div>
    </div>
    <pre id="output">Ready.</pre>

    <script>
        function applyBoost() {
            const out = document.getElementById('output');
            out.textContent = 'Applying boost...';

            const cpuResult = ksu.exec('for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > $cpu 2>/dev/null; done');
            const gpuResult = ksu.exec('echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null; echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null');

            out.textContent = 'CPU: ' + (cpuResult.includes('ERROR') ? 'Failed' : 'Boosted') + '\n';
            out.textContent += 'GPU: ' + (gpuResult.includes('ERROR') ? 'Failed' : 'Boosted');

            ksu.toast('Game Boost Applied!');
            getStatus();
        }

        function getStatus() {
            const governor = ksu.exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo 'N/A'");
            const gpuForce = ksu.exec("cat /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null || echo 'N/A'");
            const temp = ksu.exec("cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || echo '0'");

            document.getElementById('cpuStat').textContent = governor.trim();
            document.getElementById('gpuStat').textContent = gpuForce.trim() === '1' ? 'Boosted' : 'Normal';
            document.getElementById('tempStat').textContent = Math.floor(parseInt(temp.trim()) / 1000) + '°C';
        }

        getStatus();
    </script>
</body>
</html>
```

---

## ✅ PLUGIN CHECKLIST

Bago mo i-release ang plugin mo, tiyakin na:

- [ ] May `module.prop` na may kumpletong fields
- [ ] Ang `id` ay valid format (no spaces, no special chars)
- [ ] Ang `betaPlugin` version ay tama (`<= 10001`)
- [ ] Lahat ng `.sh` files ay **executable** (`chmod +x`)
- [ ] Lahat ng scripts ay may **shebang** (`#!/system/bin/sh`)
- [ ] Ginagamit ang `$MODDIR` imbes na hardcoded paths
- [ ] May `uninstall.sh` para mag-restore ng stock settings
- [ ] Ang ZIP ay **directly zipped** (hindi naka-folder sa loob)
- [ ] Tested sa Beta Manager app

---

<div align="center">
  <hr>
  <p>
    <strong>⚡ BETA MANAGER</strong> — <em>Level up your gaming performance</em>
  </p>
  <p>
    <a href="https://github.com/willygailo/BETA-MANAGER">GitHub</a> ·
    <a href="README.md">README</a> ·
    <a href="ARCHITECTURE.md">Architecture</a>
  </p>
  <br>
</div>
