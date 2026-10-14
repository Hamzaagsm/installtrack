package com.hamza.installtrack;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Device Admin receiver — enables remote LOCK and (seller PIN-protected) WIPE.
 *
 * UNINSTALL PROTECTION: Jab tak ye app Device Admin hai, Android khud iska
 * uninstall BLOCK karta hai (Settings me "Uninstall" ka option nahi milta).
 * Admin deactivate kiye baghair app remove karna mumkin nahi — yehi asal
 * protection hai. Is liye neeche onDisableRequested me sakht warning hai.
 *
 * The buyer explicitly enables this during setup; Android shows its own
 * system dialog, so nothing is hidden.
 */
public class TrackerDeviceAdmin extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        // Device Owner ho to policies foran lagao (QR provisioning ke baad
        // bhi yehi call aati hai).
        if (DeviceOwnerHelper.isDeviceOwner(context)) {
            DeviceOwnerHelper.applyDeviceOwnerPolicies(context);
        }
        Toast.makeText(context, "Device Admin ON — Qist Protection active", Toast.LENGTH_LONG).show();
    }

    /**
     * QR provisioning mukammal hone par Android ye call karta hai.
     * Policies lagao aur setup screen kholo taake seller baqi setup
     * (Firebase, qist, consent) poora kar sake.
     */
    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        if (DeviceOwnerHelper.isDeviceOwner(context)) {
            DeviceOwnerHelper.applyDeviceOwnerPolicies(context);
        }
        try {
            Intent i = new Intent(context, SetupActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(i);
        } catch (Exception ignored) {}
    }

    /**
     * Jab koi Device Admin deactivate karne ki koshish kare to Android ye
     * warning dikhata hai (deactivate screen par).
     */
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "⚠️ Ye app qist mukammal hone tak zaroori hai (agreement ke mutabiq).\n\n"
                + "Admin hatane se:\n"
                + "• Seller ko foran ittila chali jayegi\n"
                + "• Phone LOCK ho sakta hai\n\n"
                + "Qist mukammal hone par seller khud app release kar dega.";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        // Tamper alert -> seller ko foran pata chal jaye ke chherkhani hui
        FirebaseRest fb = new FirebaseRest();
        fb.load(context);
        String deviceId = SetupActivity.getDeviceId(context);
        if (fb.isConfigured() && deviceId != null) {
            long now = System.currentTimeMillis();
            fb.put("/devices/" + deviceId + "/tamper",
                    "{\"type\":\"ADMIN_DISABLED\",\"time\":" + now + "}");
            fb.put("/devices/" + deviceId + "/adminActive", "false");
        }
        Toast.makeText(context,
                "Device Admin OFF — seller ko ittila bhej di gayi hai",
                Toast.LENGTH_LONG).show();
    }
}
