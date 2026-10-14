package com.hamza.installtrack;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.UserManager;
import android.util.Base64;

import java.security.MessageDigest;

/**
 * Device Owner (fully managed device) helpers — InstallTrack v2.0.
 *
 * Sab kuch PUBLIC Android Device Owner APIs se hai. Koi root exploit nahi,
 * koi chhupa tareeqa nahi, koi Android security bypass nahi.
 *
 * DEVICE OWNER KYA KARTA HAI (aur kya NAHI):
 *  - DO active hone par neeche wali restrictions lagti hain:
 *      DISALLOW_FACTORY_RESET — Settings se factory reset block
 *      DISALLOW_UNINSTALL_APPS — apps uninstall block (Android 9+, API 28+)
 *      setUninstallBlocked(this app) — InstallTrack khud uninstall block (API 28+)
 *      DISALLOW_ADD_USER — naya user/profile add block
 *  - Phone baqi poora usable rehta hai: WiFi, calls, Play Store, roz ke
 *    settings — hum in me se KUCH block nahi karte (na DISALLOW_CONFIG_WIFI,
 *    na DISALLOW_OUTGOING_CALLS, na DISALLOW_INSTALL_APPS).
 *
 * HONEST LIMITS (README me bhi likhe hain):
 *  - Device Owner recovery-mode factory reset ya genuine stock firmware
 *    flashing se NAHI bachta. Ye Android ka design hai — physical access
 *    wale ke paas aakhri control hota hai.
 *  - Zero-touch auto-re-enrollment is me NAHI hai (Google reseller-portal
 *    registration chahiye hoti hai).
 */
public class DeviceOwnerHelper {

    public static ComponentName adminComponent(Context ctx) {
        return new ComponentName(ctx, TrackerDeviceAdmin.class);
    }

    /** Kya ye app is phone ki Device Owner hai? */
    public static boolean isDeviceOwner(Context ctx) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && dpm.isDeviceOwnerApp(ctx.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    /** Kya app Device Admin (ya Device Owner) ke tor par active hai? */
    public static boolean isAdminActive(Context ctx) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && dpm.isAdminActive(adminComponent(ctx));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Device Owner policies lagao. Sirf tab kuch karega jab ye app Device
     * Owner hai — warna chup chaap false return karega (koi exception nahi).
     */
    public static boolean applyDeviceOwnerPolicies(Context ctx) {
        if (!isDeviceOwner(ctx)) return false;
        DevicePolicyManager dpm = (DevicePolicyManager)
                ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = adminComponent(ctx);
        boolean ok = true;
        try {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET);
        } catch (Exception e) { ok = false; }
        try {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER);
        } catch (Exception e) { ok = false; }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9+ — uninstall block (API 28+). Is se neeche wale
            // Android par ye do calls mumkin nahi; baqi restrictions phir
            // bhi lagti hain.
            try {
                dpm.addUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS);
            } catch (Exception e) { ok = false; }
            try {
                dpm.setUninstallBlocked(admin, ctx.getPackageName(), true);
            } catch (Exception e) { ok = false; }
        }
        return ok;
    }

    /** Device Owner policies hatayo (removal se pehle). Har call best-effort hai. */
    public static void clearDeviceOwnerPolicies(Context ctx) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName admin = adminComponent(ctx);
            if (dpm == null) return;
            try { dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET); }
            catch (Exception ignored) {}
            try { dpm.clearUserRestriction(admin, UserManager.DISALLOW_ADD_USER); }
            catch (Exception ignored) {}
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try { dpm.clearUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS); }
                catch (Exception ignored) {}
                try { dpm.setUninstallBlocked(admin, ctx.getPackageName(), false); }
                catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * Management POORI tarah hatao — seller ka "Qist poori" action.
     *  - Agar Device Owner hai: policies saaf karo + clearDeviceOwnerApp().
     *  - Agar sirf Device Admin hai: removeActiveAdmin().
     *  - Dono me se kuch nahi: true (hatane ko kuch nahi).
     * clearDeviceOwnerApp() sirf Device Owner app khud call kar sakti hai —
     * yehi public API hai, koi backdoor nahi.
     */
    public static boolean removeManagement(Context ctx) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm == null) return false;
            if (dpm.isDeviceOwnerApp(ctx.getPackageName())) {
                clearDeviceOwnerPolicies(ctx);
                dpm.clearDeviceOwnerApp(ctx.getPackageName());
                return true;
            }
            ComponentName admin = adminComponent(ctx);
            if (dpm.isAdminActive(admin)) {
                dpm.removeActiveAdmin(admin);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Seller PIN ka SHA-256 hash (hex). PIN kabhi plain text me Firebase
     * par NAHI bheja jata — sirf ye hash jata hai taake dashboard par pata
     * chale ke PIN set hai. PIN khud sirf on-device private storage me
     * rehta hai (v1.0 ki tarah). Koi hardcoded / universal PIN NAHI hai —
     * PIN seller enrollment ke waqt khud set karta hai.
     */
    public static String pinHashHex(String pin) {
        if (pin == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(pin.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Is app ke signing certificate ka SHA-256 checksum (Base64).
     * QR provisioning ke liye chahiye: system isi se verify karta hai ke
     * download hone wali APK wohi hai jo seller ne enroll ki thi.
     * Ye app ke APNE signature se live compute hota hai — koi hardcode
     * checksum nahi.
     */
    public static String getSignatureChecksum(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            String pkg = ctx.getPackageName();
            Signature sig = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo pi = pm.getPackageInfo(pkg,
                        PackageManager.GET_SIGNING_CERTIFICATES);
                if (pi.signingInfo != null) {
                    Signature[] signers = pi.signingInfo.getApkContentsSigners();
                    if (signers != null && signers.length > 0) sig = signers[0];
                }
            } else {
                @SuppressWarnings("deprecation")
                PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                if (pi.signatures != null && pi.signatures.length > 0) sig = pi.signatures[0];
            }
            if (sig == null) return "";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sig.toByteArray());
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
}
