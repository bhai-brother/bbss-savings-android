# BBSS Savings Android App — GitHub Ready v2

ভাই-ব্রাদার্স সঞ্চয় সমিতির live website-কে Android app হিসেবে চালানোর জন্য এই repository তৈরি করা হয়েছে।

**Live website:**  
https://bhai-brother.github.io/bbss-savings/bbss_savings_pro.html

## v2-তে যা আছে

- Native Android WebView wrapper
- আপনার দেওয়া BBSS নীল বাংলা লোগো Android app launcher icon হিসেবে সেট করা আছে
- Website update হলে app rebuild ছাড়াই live version load হবে
- Member/Admin login session, cookies এবং DOM/local storage support
- BBSS-এর `rules.html`, `send-money.html`, `committee.html`, `personal-info.html` app-এর ভিতরে open হয়
- External website, `tel:`, `mailto:`, `sms:`, `intent:`, bKash/Nagad scheme system app-এ open হয়
- Member profile image/file picker support
- Website-এর CSV/JSON `Blob` export Android Files dialog দিয়ে save করা যায়
- Website-এর **Print / PDF** button Android system print/PDF dialog open করে
- Android Back / predictive back navigation
- Offline screen + internet ফিরলে automatic retry
- HTTPS-only network policy এবং WebView safe browsing
- Debug WebView inspection শুধু debug build-এ enabled
- GitHub Actions দিয়ে automatic APK build
- CI debug APK একই committed **debug-only** keystore দিয়ে sign হয়, তাই test build-এর signature repeatable
- Optional GitHub Secrets দিয়ে signed release APK/AAB build

## Build configuration

| Item | Value |
|---|---|
| Package | `com.bhaibrother.bbsssavings` |
| Version | `2.0.0` (`versionCode 2`) |
| Minimum Android | Android 7.0 / API 24 |
| Target SDK | 36 |
| Compile SDK | 36 |
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.0 |
| JDK | 17 |

## GitHub-এ upload

1. নতুন repository তৈরি করুন, যেমন `bbss-savings-android`।
2. এই folder-এর সব file repository root-এ upload/push করুন।
3. GitHub → **Actions** → **Build BBSS Android APK** → **Run workflow**।
4. Build complete হলে `BBSS-Savings-v2.0.0-debug` artifact থেকে APK নিন।

Push to `main` করলেও workflow automatic চলবে।

## Android Studio দিয়ে build

1. Android Studio দিয়ে project folder Open করুন।
2. JDK 17 ব্যবহার করুন।
3. Android SDK Platform 36 এবং Build Tools 36.0.0 install করুন।
4. Gradle 9.6.0 ব্যবহার করে sync করুন।
5. **Build > Build APK(s)** দিন।

CLI-তে Gradle 9.6.0 install থাকলে:

```bash
gradle --no-daemon :app:assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Signed release APK/AAB (GitHub Actions)

Repository → **Settings → Secrets and variables → Actions**-এ নিচের secrets দিন:

- `ANDROID_KEYSTORE_BASE64` — release `.jks` file-এর Base64
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

এরপর `v2.0.0`-এর মতো tag push করলে `.github/workflows/release.yml` signed APK/AAB বানাবে এবং GitHub Release-এ attach করবে। Signing secrets না থাকলে workflow installable debug APK তৈরি করবে, signed production release নয়।

## Website URL পরিবর্তন

`app/src/main/java/com/bhaibrother/bbsssavings/MainActivity.java`-এ:

```java
private static final String HOME_URL = "https://bhai-brother.github.io/bbss-savings/bbss_savings_pro.html";
```

একই সঙ্গে `INTERNAL_HOST` এবং `INTERNAL_PATH`-ও প্রয়োজন অনুযায়ী update করুন।

## Security note

- Private Apps Script write key বা **release** signing keystore repository-তে commit করবেন না। `app/debug.keystore` শুধু test/debug build-এর জন্য এবং production signing key নয়।
- App cleartext HTTP traffic block করে।
- Camera/microphone/location WebView permission default deny করা আছে, কারণ বর্তমান BBSS site এগুলো চায় না।
