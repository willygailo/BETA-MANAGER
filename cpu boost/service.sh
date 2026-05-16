#!/system/bin/sh
# CPU Performance Booster - service.sh
# Runs at boot (late_start) as background daemon
# Supports both rooted (su) and non-rooted (Shizuku)

MODDIR=${0%/*}
LOG="/data/user_de/0/com.android.shell/beta/logs/cpu_boost.log"

log() {
  echo "[$(date '+%H:%M:%S')] $1" >> "$LOG" 2>/dev/null
}

log "CPU Booster service starting..."

# Wait for system to settle
sleep 20

# ===== CPU GOVERNOR: PERFORMANCE =====
apply_cpu() {
  for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
    echo "performance" > "$cpu" 2>/dev/null
  done
  log "CPU governor set to performance"
}

# ===== GPU BOOST =====
apply_gpu() {
  echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null
  echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null
  echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null
  echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null
  log "GPU boost applied"
}

# ===== MEMORY OPTIMIZATION =====
apply_memory() {
  echo 0 > /proc/sys/vm/swappiness 2>/dev/null
  echo 50 > /proc/sys/vm/dirty_ratio 2>/dev/null
  echo 30 > /proc/sys/vm/dirty_background_ratio 2>/dev/null
  echo 1 > /proc/sys/vm/compact_memory 2>/dev/null
  echo 128 > /sys/block/mmcblk0/queue/read_ahead_kb 2>/dev/null
  log "Memory optimization applied"
}

# ===== THERMAL CONTROL =====
apply_thermal() {
  echo "disabled" > /sys/class/thermal/thermal_message/sconfig 2>/dev/null
  echo 0 > /sys/class/thermal/thermal_zone0/mode 2>/dev/null
  log "Thermal throttling disabled"
}

# ===== SCHEDULER =====
apply_scheduler() {
  echo "noop" > /sys/block/mmcblk0/queue/scheduler 2>/dev/null
}

# Apply all tweaks
apply_cpu
apply_gpu
apply_memory
apply_thermal
apply_scheduler
log "All tweaks applied successfully"

# ===== WATCHDOG LOOP =====
# Re-apply every 60 seconds to ensure persistence
while true; do
  sleep 60
  apply_cpu
  apply_gpu
  apply_memory
done
