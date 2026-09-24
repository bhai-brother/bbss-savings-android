# BBSS Android Release Guide

## 1) Release keystore একবার তৈরি করুন

```bash
keytool -genkeypair -v \
  -keystore bbss-release.jks \
  -alias bbss \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

এই file হারাবেন না। Public GitHub repository-তে upload করবেন না।

## 2) Base64 তৈরি করুন

Linux/macOS:

```bash
base64 < bbss-release.jks | tr -d '\n'
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("bbss-release.jks"))
```

## 3) GitHub Secrets

Repository → Settings → Secrets and variables → Actions:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## 4) Release tag

```bash
git tag v2.0.0
git push origin v2.0.0
```

Tag push হলে `Release BBSS Android` workflow চলবে। Secrets valid হলে signed APK এবং AAB তৈরি হবে।
