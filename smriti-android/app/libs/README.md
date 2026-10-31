# Vendor SDK files go here (not committed)

Copy these from the private `svsashank/wearable-sdks` repo before building:

| File | From |
|---|---|
| `ChipletRing-1.7.8-release.aar` | ChipletRing-APPSDK `Android/example/ringDemo/app/libs/` (1.7.8 as of 31 Oct 2026; your notes said 1.7.2) |
| Ute Watch SDK `.aar` files (v1.3.5) | the ES100 vendor's SDK zip |

`.gitignore` keeps `*.aar` and `*.jar` out of this repo on purpose: vendor code stays in the private SDK repo.
