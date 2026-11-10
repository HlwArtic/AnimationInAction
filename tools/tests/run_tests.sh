#!/bin/bash
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$HERE/../.."; W=/tmp/aia_tests
rm -rf "$W"; mkdir -p "$W"; cp -r "$HERE/src" "$W/src"
for f in Rarity Category ItemLook DropEntry DropCatalog AIAConfig ChatDetector; do cp "$ROOT/src/main/java/com/artic/aia/$f.java" "$W/src/com/artic/aia/"; done
mkdir -p "$W/src/net/minecraft/init"
{ echo "package net.minecraft.init; import net.minecraft.item.Item; public class Items {"
  grep -rhoE "Items\.[a-z_0-9]+" "$ROOT/src" | sort -u | sed 's/Items\.//' | while read f; do echo " public static Item $f = new Item();"; done; echo "}"; } > "$W/src/net/minecraft/init/Items.java"
mkdir -p "$W/res" && cp -r "$ROOT/src/main/resources/assets" "$W/res/"
java --module jdk.compiler/com.sun.tools.javac.Main -proc:none -d "$W/out" $(find "$W/src" -name "*.java")
java -cp "$W/out:$W/res" com.artic.aia.Harness | grep -E "FAIL|expected|got:|passed|^        "
