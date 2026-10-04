# Android 10 StorageVolume fix

Fixed the Android 10 startup crash caused by calling `StorageVolume.getDirectory()` / `volume.directory` on API 29.

- API 30+: keep using `StorageVolume.directory`.
- API 29: use reflective legacy `StorageVolume.getPath()` and safely return null if unavailable.
- Restored the known-working Termux ARM64 AAPT2 override in `gradle.properties`.

Crash reference: `StorageUtils.getVolumePath(StorageUtils.kt:82)` -> `NoSuchMethodError` on Android 10.
