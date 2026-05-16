#!/system/bin/sh
# CPU Performance Booster - post-fs-data.sh
# Early boot system property patcher

# Disable boot animation for faster boot
setprop debug.sf.nobootanimation 1 2>/dev/null

# Reduce debug logging
setprop logcat.live.disable 1 2>/dev/null

# Force GPU rendering
setprop persist.sys.ui.hw 1 2>/dev/null
setprop debug.composition.type gpu 2>/dev/null

# Disable power saving
setprop persist.sys.power_saving 0 2>/dev/null
