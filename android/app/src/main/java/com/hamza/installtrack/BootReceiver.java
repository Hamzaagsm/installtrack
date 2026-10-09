package com.hamza.installtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/** Restarts the tracker service after phone reboot (if setup was completed). */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences p = context.getSharedPreferences("installtrack", Context.MODE_PRIVATE);
            if (p.getBoolean("setup_done", false)) {
                Intent s = new Intent(context, TrackerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(s);
                } else {
                    context.startService(s);
                }
            }
        }
    }
}
