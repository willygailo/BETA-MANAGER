#!/system/bin/sh
# CPU Performance Booster - uninstall.sh
# Restore stock settings on module removal

echo "CPU Booster: Restoring stock settings..."

# Restore CPU governors
for cpu in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
  echo "schedutil" > "$cpu" 2>/dev/null
done

# Restore GPU
echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on 2>/dev/null
echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on 2>/dev/null
echo 0 > /sys/class/kgsl/kgsl-3d0/force_rail_on 2>/dev/null

# Restore memory
echo 60 > /proc/sys/vm/swappiness 2>/dev/null
echo 10 > /proc/sys/vm/dirty_ratio 2>/dev/null

# Restore thermal
echo "enabled" > /sys/class/thermal/thermal_message/sconfig 2>/dev/null

echo "CPU Booster: Stock settings restored."
