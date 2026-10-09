# 📱 InstallTrack — Qist Phone Protection

Qist (installment) par diye gaye phone ki hifazat ke liye tracker app.

## Buyer App (APK)
- Setup: Firebase config + qist maloomat + Device Admin + Location (sab wazeh)
- Remote **Lock** 🔒 / Unlock 🔓 / Ring 🔔 / Pegham 💬 / Wipe 🗑️ (seller PIN)
- Har 15 min location seller ko
- **Uninstall protection**: Device Admin ON hai to Android khud uninstall block karta hai.
  Admin hatane ki koshish par seller ko tamper alert jata hai.
- Qist mukammal → seller **Release** karega → app uninstall ho sakegi.

## Seller Dashboard
`dashboard.html` kholo (ya GitHub Pages par host karo):
- Live devices, map location, lock/unlock, pegham, qist hisab, wipe, release

## Firebase Setup
`HANDOFF.md` dekho — Realtime Database + web config chahiye.

## Build
GitHub Actions: `.github/workflows/build-apk.yml` → artifact `InstallTrack-apk`.

## Zaroori Notes
- Buyer ko sab kuch pehle se bataya jata hai — koi chhupi tracking nahi.
- Sirf apne beche gaye phones par, kharidar ki razamandi se use karein.
- UNLOCK se phone ka lock-screen PIN nahi khulta (Android limit) — buyer apna PIN lagayega.
