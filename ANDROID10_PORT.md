# PixelPlayer Android 10 port

This source tree is adapted from the official PixelPlayer repository for Android 10 / API 29.

Changes:
- app/shared/baselineprofile/wear minSdk: 30 -> 29
- Replaced Android 11-only `MediaStore.Audio.Media.ALBUM_ARTIST` constant references with the legacy column name `album_artist`
- Removed unnecessary API 30 `@RequiresApi` annotations from metadata editing and library screen code
- Kept Android 11+ MediaStore permission request paths behind runtime SDK checks
- Added Gradle TLS 1.2 settings for Termux/Java 21 environments
- Removed the Foojay toolchain resolver and generated daemon JVM pinning that can fail on Termux Linux/aarch64

Build on Termux:
```bash
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk
./gradlew :app:assembleDebug -Ppixelplay.enableAbiSplits=false --no-daemon --no-configuration-cache
```
