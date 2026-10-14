# 📱 InstallTrack v2.0 — Device Owner Edition

Qist (installment) par diye gaye phone ki hifazat ke liye tracker app — ab **Device Owner (fully managed device)** mode ke saath.

> ⚠️ **Zaroori:** Ye app sirf apne beche gaye phones par, kharidar ki **likhit razamandi** se use karein. Buyer ko pehle se sab kuch bataya jata hai — koi chhupi tracking nahi, app ka icon nazar aata hai, aur ek hamesha nazar aane wala notification rehta hai.

## (a) v2.0 kya karta hai

**v1.0 wali sab cheezein (barkarar):**
- Remote **Lock** 🔒 / Unlock 🔓 / Ring 🔔 / Pegham 💬 / Wipe 🗑️ (seller PIN)
- Har 15 min live location seller ko (Firebase)
- Qist hisab (kul / ada / baqi) + tamper alert
- Firebase backend (REST, `google-services.json` ki zaroorat nahi)

**v2.0 me naya — Device Owner management:**
- Jab Device Owner active hai to ye restrictions lagti hain (sirf public Android Device Owner APIs se):
  - `DISALLOW_FACTORY_RESET` — Settings se factory reset **block**
  - `DISALLOW_UNINSTALL_APPS` — apps uninstall **block** (Android 9+)
  - `setUninstallBlocked` — InstallTrack khud uninstall **block** (Android 9+)
  - `DISALLOW_ADD_USER` — naya user/profile add **block**
- Phone baqi **poora usable** rehta hai — hum WiFi, calls, Play Store, ya roz ke settings me se **kuch block nahi karte** (na `DISALLOW_CONFIG_WIFI`, na `DISALLOW_OUTGOING_CALLS`, na `DISALLOW_INSTALL_APPS`).
- App icon home screen par nazar aata hai + persistent notification: **"Is phone par InstallTrack management active hai"**.
- First-run **consent screen**: sale agreement (Roman Urdu + English) parhe baghair setup aage nahi barhega; consent accept hone ka record (time + text version) save hota hai.
- Seller PIN: enrollment ke waqt seller khud set karta hai. **Koi hardcoded / universal PIN nahi hai.** PIN plain text me Firebase par kabhi nahi jata — sirf uska SHA-256 hash jata hai (taake dashboard par pata chale ke PIN set hai); PIN khud sirf phone ki private storage me rehta hai.
- Seller-side removal: dashboard se **"Management hatao (PIN)"** ya app ke andar **"Seller: Management hatao"** — dono PIN se. Removal par `clearDeviceOwnerApp()` chalta hai aur saari restrictions khatam ho jati hain.

## (b) Enrollment — step by step

Enrollment ka matlab: InstallTrack ko is phone ka Device Owner banana. **Do tareeqe hain — dono me se aik kaafi hai.**

### Tareeqa A — ADB `dpm set-device-owner` (PRIMARY, shop-friendly)

1. Phone **factory reset** karo.
2. Setup me **koi Google account / Samsung account ADD MAT KARO** — accounts skip karo. (`dpm set-device-owner` sirf tab chalta hai jab phone par koi account na ho aur koi Device Owner pehle se na ho.)
3. Phone ko PC se USB se connect karo, USB debugging ON karo.
4. InstallTrack v2.0 APK install karo:
   ```
   adb install InstallTrack-v2.apk
   ```
5. Device Owner set karo (ek command):
   ```
   adb shell dpm set-device-owner com.hamza.installtrack/.TrackerDeviceAdmin
   ```
   Kamyabi par output aayega: `Success: Device owner set to package com.hamza.installtrack`
6. Ab app kholo → sale agreement parho → consent tick karo → Firebase + qist + seller PIN bharkar setup mukammal karo.
7. Check karo: app ke home screen par **"🛡️ Device Owner ACTIVE"** likha aana chahiye.

Agar `dpm set-device-owner` error de ("Not allowed to set the device owner because there are already some accounts on the device" waghera) to phone dobara factory reset karke step 2 se shuru karo — account add kiye baghair.

### Tareeqa B — QR provisioning (setup-time enrollment)

1. Phone **factory reset** karo.
2. Setup wizard ki **welcome screen par 6 dafa tap** karo — QR provisioning (QR code scan) khul jayegi.
3. Doosre phone/PC par InstallTrack app kholo → **"📷 QR Provisioning code"** — wahan APK download URL check karo (wohi APK host honi chahiye jiska signature is app ka hai) aur QR generate karo.
   - QR ke andar provisioning JSON hoti hai: component name + signature checksum (app apne signature se live compute karti hai) + APK download URL.
   - Dashboard (`dashboard.html`) me bhi JSON ka template diya hua hai.
4. Reset wale phone se ye QR scan karo — system APK download karke InstallTrack ko Device Owner bana dega, phir setup screen khulegi.
5. Sale agreement + consent + Firebase/qist/PIN setup mukammal karo.

Note: QR provisioning ke liye phone ko setup ke doran internet chahiye (APK download ke liye), aur APK download URL seedha `https://` link hona chahiye.

## (c) Seller removal — "Qist poori, management hatao"

Qist mukammal hone par seller **khud** management hatayega. Teen tareeqe — kisi aik se kaam ho jayega:

**1. Dashboard se (sab se aasan):**
- Dashboard kholo → device card par **"🛡️ Management hatao (PIN)"** dabao → seller PIN dalo → confirm karo.
- Phone par management hat jayegi (policies saaf + Device Owner clear), status `released_owner` ho jayega, aur buyer ko uninstall ka option milega.

**2. Phone par app ke andar:**
- App kholo → **"Seller: Management hatao (Qist poori)"** → seller PIN dalo → confirm.
- Management hatne ke baad uninstall ka option aayega.

**3. Dashboard se purana Release (bina PIN):**
- **"✅ Release (Qist Done)"** button bhi management hata deta hai (Device Owner ho to `clearDeviceOwnerApp()` ke zariye). Ye dashboard-access se seller-authenticated hai. PIN wala tareeqa zyada mehfooz hai — isi ko istemal karein.

**Removal ke baad:** saari restrictions (factory-reset block, uninstall block, add-user block) khatam. App normal tarike se uninstall ho sakti hai. Buyer ko koi chhupa lock nahi rehta.

**Seller PIN bhool jaye to?** PIN ka koi master/universal backdoor **nahi** hai. Agar PIN bhool gaya to management hatane ka rasta: phone ko recovery se factory reset (data mit jayega) — ya FRP layer on ho to seller ke Google account se FRP unlock. Is liye PIN ko mehfooz jagah likh kar rakhein.

## (d) Sale agreement (consent text)

Yehi text app ki first-run consent screen par dikhta hai, aur `CONSENT_AGREEMENT.md` me print karke sign karwane ke liye bhi hai.

### Roman Urdu

```
=== QIST PLAN — SALE AGREEMENT (InstallTrack v2.0) ===

1) Is phone par InstallTrack app lagi hui hai. Ye app seller (Hamza Mobile)
ki taraf se qist (installment) plan ke doran phone ki hifazat ke liye lagai
gayi hai.

2) YE APP KYA KARTI HAI:
   - Phone ki location seller ko bhejti hai (har 15 minute).
   - Qist ada na hone par seller phone ko LOCK kar sakta hai aur screen par
     pegham dikha sakta hai.
   - Phone par ek hamesha nazar aane wala notification rehta hai:
     "Is phone par InstallTrack management active hai".
   - App ka icon home screen par nazar aata hai — app chhupi hui NAHI hai.

3) DEVICE OWNER MANAGEMENT:
   - Ye phone Device Owner (fully managed) mode me hai. Is ka matlab hai:
     Settings se factory reset aur app uninstall qist mukammal hone tak
     BLOCK hai.
   - Phone baqi poora usable hai: calls, WiFi, Play Store, roz ke
     settings — in me se kuch block NAHI hai.

4) HATANA (REMOVAL):
   - Qist poori hote hi seller management HATA DEGA — phir app normal
     tarike se uninstall ho sakegi.
   - Buyer khud Settings se isay qist se pehle remove NAHI kar sakta
     (yehi agreement hai).

5) HONEST LIMITS:
   - Recovery-mode factory reset ya genuine stock firmware flashing se ye
     management mit sakta hai. Koi bhi software is se bach nahi sakta.
   - Seller qist poori hone ke baad management hatane ka paband hai.

Buyer ka naam: ______________  Dastakhat: ______________
Seller ka naam: ______________  Dastakhat: ______________
Tareekh: ______________  Phone model/IMEI: ______________
```

### English

```
=== INSTALLMENT PLAN — SALE AGREEMENT (InstallTrack v2.0) ===

1) This phone has the InstallTrack app installed by the seller
(Hamza Mobile) to protect the phone during the installment (qist) plan.

2) WHAT THIS APP DOES:
   - Sends the phone's location to the seller (every 15 minutes).
   - If an installment is not paid, the seller can LOCK the phone and
     show a message on the screen.
   - A permanent notification stays visible: "InstallTrack management
     is active on this phone".
   - The app icon stays visible on the home screen — the app is NOT hidden.

3) DEVICE OWNER MANAGEMENT:
   - This phone is in Device Owner (fully managed) mode. That means
     factory reset from Settings and app uninstall are BLOCKED until the
     installments are complete.
   - The phone stays fully usable otherwise: calls, WiFi, Play Store,
     daily settings — none of these are blocked.

4) REMOVAL:
   - When the installments are complete, the seller WILL remove the
     management — after that the app can be uninstalled normally.
   - The buyer cannot remove it from Settings before the installments
     are complete (that is the agreement).

5) HONEST LIMITS:
   - A recovery-mode factory reset or flashing genuine stock firmware
     can remove this management. No software can prevent that.
   - The seller is obliged to remove the management once the
     installments are complete.

Buyer name: ______________  Signature: ______________
Seller name: ______________  Signature: ______________
Date: ______________  Phone model/IMEI: ______________
```

## (e) Honest limits — kya v2.0 NAHI karta

- **Recovery-mode factory reset ya genuine stock firmware flashing se Device Owner management MIT jata hai.** Ye Android ka design hai — jiske paas phone physically hai uske paas aakhri control hai. Koi bhi app, chahe Device Owner hi kyun na ho, is se bach nahi sakti. Jo "100% reset-proof" ka dawa kare, wo ghalat kehta hai.
- **Zero-touch auto-re-enrollment is me NAHI hai.** Reset ke baad tracker ka khud-ba-khud wapis install hona (zero-touch) ke liye phone ka Google ke zero-touch reseller portal me registered hona zaroori hai — ye is app ka hissa nahi hai.
- **Uninstall block ko poora faida Android 9+ (API 28+) par hai.** Is se neeche wale Android par `DISALLOW_UNINSTALL_APPS` / `setUninstallBlocked` available nahi — baqi restrictions (factory-reset block, add-user block) phir bhi lagti hain.
- **UNLOCK se phone ka lock-screen PIN nahi khulta** (Android limit) — buyer apna PIN/pattern lagayega; lock overlay zaroor hat jayega.
- **Command delay ~30s** (polling). Foran lock ke liye FCM lagana hoga (future).
- **Battery optimization:** buyer ke phone par battery optimization OFF karna hoga warna service ruk sakti hai.
- **Location background permission** Android 10+ par "Allow all the time" mangta hai.
- **FRP layer (neeche) ke baghair** recovery reset ke baad phone seedha setup par chala jayega — management wapis nahi aayega.

## OPTIONAL FRP layer (seller ke Google account se)

Ye **optional** extra layer hai — app ka hissa nahi, seller ke haath ka amal hai. Iske liye bhi **wohi likhit buyer consent** zaroori hai (agreement ke saath buyer ko batayein ke seller ka Google account phone par signed-in rahega aur FRP active hoga).

**Tareeqa:**
1. Phone handover se pehle, **seller ka apna Google account** phone par signed-in rakhein (setup ke doran add karein, handover se pehle remove NA karein).
2. Is se recovery-mode factory reset ke baad phone setup par **FRP (Factory Reset Protection)** mangega — aage barhne ke liye wohi Google account (ya uska password) chahiye hoga.
3. Qist poori hone par seller ya to account khud remove kare (phone se account hataye), ya buyer ko FRP unlock me madad kare — phir phone buyer ke apne account se setup hoga.

**Seedha sach (honest limits):**
- **Recovery reset phir bhi data mita deta hai.** FRP reset ko rokta NAHI — reset ho jata hai, data chala jata hai. FRP sirf **reset ke BAAD ka deterrent** hai: reset ke baad phone us account ke baghair usable nahi hota.
- **Koi app phone ko "bas normal boot" nahi karwa sakti bajaye reset ke.** Recovery reset hamesha reset hi karega — FRP sirf uske baad setup ko rokta hai.
- **FRP ko bhi professional tools se bypass kiya ja sakta hai** (paid/server tools, model ke hisaab se). Ye rukawat hai, guarantee nahi.
- **Consent zaroori hai:** buyer ko pehle se likhit me pata hona chahiye ke seller ka Google account phone par rahega aur FRP active hoga — warna ye chhupa hua control ban jayega.
- **Account ka khayal rakhein:** seller ka Google account phone par rehne ka matlab hai ke account ki security (password, 2-step) ka khayal rakhna hoga, aur qist poori hone par account hatana seller ki zimmedari hai.

**FRP + Device Owner mil kar:** Device Owner Settings-reset ko block karta hai; FRP recovery-reset ke baad phone ko unusable rakhta hai (account ke baghair). Dono mil kar deterrent mazboot hota hai — lekin dono me se koi bhi genuine stock firmware flashing ko nahi rokta.

## Build

GitHub Actions: `.github/workflows/build-apk.yml` → artifact `InstallTrack-v2-apk` → `InstallTrack-v2.apk`.

## Firebase Setup

`HANDOFF.md` dekho — Realtime Database + web config chahiye. Dashboard (`dashboard.html`) par Firebase config bharein (koi prefilled credentials nahi — apni values dalein).
