#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

required="
settings.gradle
build.gradle
gradle.properties
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/java/com/bhaibrother/bbsssavings/MainActivity.java
.github/workflows/build-apk.yml
.github/workflows/release.yml
"

for f in $required; do
  if [ ! -f "$f" ]; then
    echo "Missing: $f" >&2
    exit 1
  fi
done

URL='https://bhai-brother.github.io/bbss-savings/bbss_savings_pro.html'
if ! grep -Fq "$URL" app/src/main/java/com/bhaibrother/bbsssavings/MainActivity.java; then
  echo "HOME_URL missing or changed" >&2
  exit 1
fi

if grep -R -n -E 'BEGIN (RSA |EC |DSA )?PRIVATE KEY|ANDROID_KEYSTORE_PASSWORD=.+' . \
  --exclude-dir=.git --exclude='check-project.sh' >/dev/null 2>&1; then
  echo "Potential secret found in repository" >&2
  exit 1
fi

python3 - <<'PY'
import xml.etree.ElementTree as ET
from pathlib import Path
for p in [
    Path('app/src/main/AndroidManifest.xml'),
    Path('app/src/main/res/xml/network_security_config.xml'),
    Path('app/src/main/res/values/colors.xml'),
    Path('app/src/main/res/values/strings.xml'),
    Path('app/src/main/res/values/styles.xml'),
    Path('app/src/main/res/values-v31/styles.xml'),
    Path('app/src/main/res/drawable/splash_background.xml'),
]:
    ET.parse(p)
print('XML OK')
PY

echo "Project structure OK"
