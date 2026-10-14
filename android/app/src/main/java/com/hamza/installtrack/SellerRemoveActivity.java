package com.hamza.installtrack;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Seller-side removal screen — "Qist poori, management hatao".
 *
 * Flow: seller apna PIN dale (wohi PIN jo enrollment ke waqt set hua
 * tha — koi hardcoded/universal PIN NAHI hai) -> confirm -> management
 * hat jayegi (Device Owner clear ya Device Admin remove) -> app
 * uninstall ke liye offer hogi.
 *
 * PIN ghalat ho to kuch nahi hoga. PIN khali (kabhi set hi nahi hua)
 * ho to bhi kuch nahi hoga — seller ko pehle enrollment me PIN set
 * karna hoga.
 */
public class SellerRemoveActivity extends Activity {

    private EditText etPin;
    private TextView tvStatus;
    private Button btnRemove;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_seller_remove);

        etPin = findViewById(R.id.etRemovePin);
        tvStatus = findViewById(R.id.tvRemoveStatus);
        btnRemove = findViewById(R.id.btnDoRemove);

        if (DeviceOwnerHelper.isDeviceOwner(this)) {
            tvStatus.setText("Device Owner ACTIVE hai.\nPIN dalein — management hat jayegi.");
        } else if (DeviceOwnerHelper.isAdminActive(this)) {
            tvStatus.setText("Device Admin ON hai (Device Owner nahi).\nPIN dalein — admin hat jayega.");
        } else {
            tvStatus.setText("Koi management active nahi hai — hatane ko kuch nahi.");
            btnRemove.setEnabled(false);
        }

        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doRemove(); }
        });
    }

    private void doRemove() {
        SharedPreferences p = getSharedPreferences("installtrack", MODE_PRIVATE);
        String storedPin = p.getString("seller_pin", "");
        String entered = etPin.getText().toString().trim();

        if (storedPin.isEmpty()) {
            Toast.makeText(this,
                    "Seller PIN set hi nahi hua (enrollment me set hota hai).",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!storedPin.equals(entered)) {
            Toast.makeText(this, "PIN ghalat hai — management NAHI hati.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean ok = DeviceOwnerHelper.removeManagement(this);
        if (!ok) {
            Toast.makeText(this, "Management hatana fail hua — dobara try karein.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Firebase par status update (best effort)
        final String deviceId = SetupActivity.getDeviceId(this);
        new Thread(new Runnable() {
            @Override public void run() {
                FirebaseRest fb = new FirebaseRest();
                fb.load(SellerRemoveActivity.this);
                if (fb.isConfigured() && deviceId != null) {
                    fb.put("/devices/" + deviceId + "/status",
                            FirebaseRest.quote("released_owner"));
                    fb.put("/devices/" + deviceId + "/deviceOwner", "false");
                    fb.put("/devices/" + deviceId + "/adminActive", "false");
                }
            }
        }).start();

        Toast.makeText(this, "✅ Management hat gayi — ab app uninstall ho sakti hai.",
                Toast.LENGTH_LONG).show();
        try {
            Intent i = new Intent(Intent.ACTION_DELETE);
            i.setData(Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {}
        finish();
    }
}
