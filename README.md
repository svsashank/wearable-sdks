# wearable-sdks (keep this repo PRIVATE)

Vendor SDKs for the Smriti project, plus a cloud build that turns the test apps into installable APKs.

| Folder | What |
|---|---|
| `chipletring/` | Bravechip ChipletRing APP SDK (Android + docs), see `VENDOR_SOURCE.md` |
| `smriti-android/` | Our Android app, staged here so it can build against the SDK (moves to `svsashank/smriti`) |
| `ute-es100/` | ES100 band Ute Watch SDK v1.3.5 — **to be added** (not public) |

**Get the APKs on a phone:** open `github.com/svsashank/wearable-sdks/releases/latest` (logged in), tap the APK.
Every push to `main` rebuilds them (Actions tab shows progress, ~5–8 min).
