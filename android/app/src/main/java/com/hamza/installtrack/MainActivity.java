package com.hamza.installtrack;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Buyer ka home screen: qist ki maloomat + phone status.
 */
public class MainActivity extends Activity {

    private TextView tvInfo, tvStatus;
    private Button btnReEnable;
    private BroadcastReceiver unlockReceiver;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        tvInfo = findViewById(R.id.tvInfo);
        tvStatus = findViewById(R.id.tvStatus);
        btnReEnable = findViewById(R.id.btnReEnable);

        btnReEnable.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // Wapis setup par bhejo taake admin dobara ON ho sake
                getSharedPreferences("installtrack", MODE_PRIVATE)
                        .edit().putBoolean("setup_done", false).apply();
                startActivity(new Intent(MainActivity.this, SetupActivity.class));
                finish();
            }
        });

        // UNLOCK command aaye to lock overlay band karo
        unlockReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                refreshStatus();
            }
        };
        registerReceiver(unlockReceiver, new IntentFilter("com.hamza.installtrack.UNLOCKED"));

        showQistInfo();
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(unlockReceiver); } catch (Exception ignored) {}
    }

    private void showQistInfo() {
        SharedPreferences p = getSharedPreferences("installtrack", MODE_PRIVATE);
        String buyer = p.getString("buyer_name", "-");
        String total = p.getString("total_qist", "-");
        String monthly = p.getString("monthly", "-");
        String paid = p.getString("paid", "0");

        StringBuilder sb = new StringBuilder();
        sb.append("👤 Kharidar: ").append(buyer).append("\n\n");
        sb.append("💰 Kul Raqam: ").append(total).append("\n");
        sb.append("📅 Mahana Qist: ").append(monthly).append("\n");
        sb.append("✅ Ada Shuda: ").append(paid).append("\n");
        try {
            long t = Long.parseLong(total.replaceAll("[^0-9]", ""));
            long pd = Long.parseLong(paid.replaceAll("[^0-9]", ""));
            sb.append("📌 Baqi: ").append(t - pd);
        } catch (Exception ignored) {}
        tvInfo.setText(sb.toString());
    }

    private void refreshStatus() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, TrackerDeviceAdmin.class);
        boolean adminOn = dpm != null && dpm.isAdminActive(admin);
        if (adminOn) {
            tvStatus.setText("✅ Phone Active — Qist Protection ON");
            btnReEnable.setVisibility(View.GONE);
        } else {
            tvStatus.setText("⚠️ Protection OFF hai!\nQist agreement ke mutabiq dobara ON karein.");
            btnReEnable.setVisibility(View.VISIBLE);
        }
    }
}
