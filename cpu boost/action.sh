#!/system/bin/sh
# CPU Performance Booster - action.sh
# On-demand status check and tweak application
# Triggered by user tapping "Action" button

echo ""
echo "╔══════════════════════════════════════╗"
echo "║    CPU PERFORMANCE BOOSTER v2.0    ║"
echo "╚══════════════════════════════════════╝"
echo ""

echo "≡ CPU Governors:"
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
  name=$(basename $(dirname "$cpu") 2>/dev/null)
  gov=$(cat "$cpu" 2>/dev/null)
  echo "  ${name}: ${gov}"
done

echo ""
echo "≡ CPU Frequencies:"
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq; do
  name=$(basename $(dirname "$cpu") 2>/dev/null)
  freq=$(cat "$cpu" 2>/dev/null)
  echo "  ${name}: $((freq / 1000)) MHz"
done

echo ""
echo "≡ GPU Status:"
echo "  Force bus:  $(cat /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null || echo 'N/A')"
echo "  Force clk:  $(cat /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null || echo 'N/A')"
echo "  Force rail: $(cat /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null || echo 'N/A')"
echo "  Max pwr:    $(cat /sys/class/kgsl/kgsl-3d0/max_pwrlevel 2>/dev/null || echo 'N/A')"

echo ""
echo "≡ Memory:"
echo "  Swappiness:       $(cat /proc/sys/vm/swappiness 2>/dev/null || echo 'N/A')"
echo "  Dirty ratio:      $(cat /proc/sys/vm/dirty_ratio 2>/dev/null || echo 'N/A')"
echo "  Cache pressure:   $(cat /proc/sys/vm/vfs_cache_pressure 2>/dev/null || echo 'N/A')"

echo ""
echo "≡ Temperature:"
i=0
for zone in /sys/class/thermal/thermal_zone*/temp; do
  temp=$(cat "$zone" 2>/dev/null)
  [ -n "$temp" ] && echo "  Zone ${i}: $((temp / 1000))°C"
  i=$((i + 1))
done

echo ""
echo "≡ Scheduler:"
echo "  I/O: $(cat /sys/block/mmcblk0/queue/scheduler 2>/dev/null | sed 's/\[//;s/\]//' || echo 'N/A')"
echo "  Read ahead: $(cat /sys/block/mmcblk0/queue/read_ahead_kb 2>/dev/null || echo 'N/A') KB"

echo ""
echo "════════════════════════════════════════"
