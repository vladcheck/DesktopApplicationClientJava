#!/usr/bin/env bash
set -euo pipefail

SRC="${1:-src/main/resources/images/app.png}"
OUT="${2:-src/main/resources/images/icons}"

[[ -f "$SRC" ]] || { echo "source not found: $SRC" >&2; exit 1; }

mkdir -p "$OUT/png" "$OUT/app.iconset"

# PNG set
for size in 16 32 48 64 128 256; do
  magick "$SRC" -resize "${size}x${size}" "$OUT/png/app-${size}.png"
done

# ICO
magick "$SRC" -define icon:auto-resize=256,128,64,48,32,16 "$OUT/app.ico"

# ICNS
magick "$SRC" -resize 16x16     "$OUT/app.iconset/icon_16x16.png"
magick "$SRC" -resize 32x32     "$OUT/app.iconset/icon_16x16@2x.png"
magick "$SRC" -resize 32x32     "$OUT/app.iconset/icon_32x32.png"
magick "$SRC" -resize 64x64     "$OUT/app.iconset/icon_32x32@2x.png"
magick "$SRC" -resize 128x128   "$OUT/app.iconset/icon_128x128.png"
magick "$SRC" -resize 256x256   "$OUT/app.iconset/icon_128x128@2x.png"
magick "$SRC" -resize 256x256   "$OUT/app.iconset/icon_256x256.png"
magick "$SRC" -resize 512x512   "$OUT/app.iconset/icon_256x256@2x.png"
magick "$SRC" -resize 512x512   "$OUT/app.iconset/icon_512x512.png"
magick "$SRC" -resize 1024x1024 "$OUT/app.iconset/icon_512x512@2x.png"

iconutil -c icns "$OUT/app.iconset" -o "$OUT/app.icns"
rm -rf "$OUT/app.iconset"

echo "done: $OUT"