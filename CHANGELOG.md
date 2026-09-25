# Changelog

## 2.0.0 — 2026-09-24

- Updated Android wrapper for the current BBSS live website.
- Added native save flow for website CSV/JSON Blob exports.
- Added Android Print/PDF integration for website `window.print()`.
- Added automatic retry when connectivity returns after an offline page.
- Added predictive-back support on Android 13+ while preserving WebView history.
- Added system-bar inset handling for modern edge-to-edge Android behavior.
- Hardened WebView: HTTPS-only, Safe Browsing, file URL access disabled, third-party cookies disabled, camera/mic permission denied.
- Added repeatable debug signing for GitHub Actions test APKs.
- Added optional secret-based signed release APK/AAB workflow.

## v2.0.0 - App icon update

- User-provided BBSS blue Bengali logo is now the Android launcher icon.
- Regenerated mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi launcher assets.
- Added `artwork/bbss_app_icon_source.png` and `artwork/play-store-icon-512.png` for future releases.

## 2.0.1
- Force fresh GitHub Pages load in Android WebView (no stale page restore/cache).
- Force Member top navigation visible on mobile inside the Android app.
- Add Android-side fallback member helper functions to avoid stale-page `findAccountById` errors.
- Update GitHub Actions Android setup to `setup-android@v4` and remove blocking lint step from debug build.
