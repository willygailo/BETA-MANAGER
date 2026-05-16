#!/bin/bash
# Build CPU Performance Booster plugin ZIP for Beta Manager
# Usage: ./build.sh

NAME="cpu_boost"
VERSION="v2.0.0"
OUTPUT_DIR=".."

echo "⚡ Building CPU Performance Booster ${VERSION}..."
cd "$(dirname "$0")"

# Make scripts executable
chmod +x *.sh 2>/dev/null
chmod +x system/bin/* 2>/dev/null

# Create ZIP (directly zipped, no parent folder)
zip -r "${OUTPUT_DIR}/${NAME}_${VERSION}.zip" \
  module.prop \
  service.sh \
  post-fs-data.sh \
  action.sh \
  uninstall.sh \
  system/ \
  webroot/ \
  -x "build.sh" \
  -x "*.DS_Store"

echo "✅ Done: ${OUTPUT_DIR}/${NAME}_${VERSION}.zip"
echo ""
echo "Install via Beta Manager:"
echo "  1. Open Beta Manager → Auto Activate"
echo "  2. Tap Install → Select the ZIP"
echo "  3. Choose flash target (Beta/Magisk/KSU)"
echo "  4. Enable module in dashboard"
