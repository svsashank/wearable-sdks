Copied unchanged from https://github.com/BravechipSpace/ChipletRing-APPSDK
commit 1eec756fe0b889337c7355942d90f2bc496deb29 (27 Oct 2026), Android/ and Doc/ only (iOS left out: 1.1 GB).
Only changes, for cloud builds: removed ringDemo/local.properties (vendor's Windows path) and pointed the
Gradle wrapper at services.gradle.org instead of the Tencent mirror.
Demo UI translated to English (all on-screen text and log messages). Dangerous buttons are marked ⛔
(bind/wipe history, firmware update, GoMore/cloud, setting changes).
MainActivity: no longer demands All-files access (MANAGE_EXTERNAL_STORAGE) before scanning; scans even if an optional permission is denied.
