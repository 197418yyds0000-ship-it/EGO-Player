# Changelog

All notable changes to EGO Player will be documented here.

## [v11] - Android 10 Optimization Edition

### Added

- Added Android 10 (API 29) compatibility improvements
- Added custom player font support
- Added custom lyrics font support
- Added font restore-to-default option
- Added Material Expressive UI improvements
- Added Android 11 style stretch overscroll experiment
- Added improved dynamic color compatibility
- Added additional Android 10 optimization patches

### Changed

- Redesigned some UI scaling behavior
- Improved player interface animation handling
- Improved bottom navigation animation
- Improved lyrics page customization system
- Moved font customization options into Appearance settings
- Reduced unnecessary blur effects in player and bottom bar
- Optimized animations for better performance on older devices

### Fixed

- Fixed custom font not applying to all player components
- Fixed custom font settings lacking reset option
- Fixed some Android 10 rendering issues
- Fixed resource compatibility issues during build
- Fixed several UI scaling problems

### Performance

- Removed heavy blur processing from frequently updated UI areas
- Optimized Compose graphics layer usage
- Reduced animation-related performance overhead
- Improved stability on Android 10 devices

### Known Issues

- Some custom fonts may still not apply to every UI component
- Lyrics custom font may cause performance drops on some devices
- Stretch overscroll effect is still experimental
- Advanced blur effects require further optimization

---

## Credits

Based on Pixel Player.

This project is an unofficial fork focused on Android 10 compatibility and UI improvements.
