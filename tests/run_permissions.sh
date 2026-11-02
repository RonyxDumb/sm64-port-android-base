#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
javac -source 8 -target 8 -d "$build" \
    app/src/main/java/com/pipino/sm64port/AppPermissions.java tests/AppPermissionsTest.java
java -cp "$build" com.pipino.sm64port.AppPermissionsTest
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
android = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse('app/src/main/AndroidManifest.xml').getroot()
permissions = {item.get(android + 'name'): item for item in manifest.findall('uses-permission')}
for name in ('ACCESS_COARSE_LOCATION', 'ACCESS_FINE_LOCATION'):
    assert permissions['android.permission.' + name].get(android + 'maxSdkVersion') == '32'
assert permissions['android.permission.NEARBY_WIFI_DEVICES'].get(android + 'usesPermissionFlags') == 'neverForLocation'
assert 'android.permission.POST_NOTIFICATIONS' in permissions
gradle = Path('app/build.gradle').read_text()
assert 'compileSdkVersion 33' in gradle and 'targetSdkVersion 33' in gradle
print('permission manifest and target SDK agree with runtime plan PASS')
PY
