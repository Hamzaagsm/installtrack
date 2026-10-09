package com.hamza.installtrack;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * Lock overlay: LOCK ya MESSAGE command par full-screen pegham.
 * showWhenLocked + turnScreenOn flags manifest me lage hain.
 * UNLOCK broadcast aaye to band ho jata hai.
 */
public class LockActivity extends Activity {

    private BroadcastReceiver unlockReceiver;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_lock);

        TextView tvMsg = findViewById(R.id.tvLockMsg);
        Button btnClose = findViewById(R.id.btnCloseMsg);

        String msg = getIntent().getStringExtra("message");
        boolean infoOnly = getIntent().getBooleanExtra("info_only", false);
        if (msg == null || msg.isEmpty()) {
            msg = "Qist ada karein — Hamza Mobile se rabta karein";
        }
        tvMsg.setText("🔒\n\n" + msg);

        // MESSAGE (info) ko buyer band kar sakta hai; LOCK wala seller ke UNLOCK tak rahega
        if (infoOnly) {
            btnClose.setVisibility(View.VISIBLE);
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { finish(); }
            });
        } else {
            btnClose.setVisibility(View.GONE);
        }

        unlockReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) { finish(); }
        };
        registerReceiver(unlockReceiver, new IntentFilter("com.hamza.installtrack.UNLOCKED"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(unlockReceiver); } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        // LOCK screen par back dabane se band na ho
    }
}
