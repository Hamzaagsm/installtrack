package com.hamza.installtrack;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Pehli dafa setup screen. Buyer yahan:
 *  1. Firebase config (seller deta hai) + qist ki maloomat bharta hai
 *  2. Device Admin ON karta hai (remote lock ke liye LAZMI)
 *  3. Location permission deta hai
 *
 * Sab kuch saaf likha hai — kuch chhupa nahi.
 */
public class SetupActivity extends Activity {

    private static final int REQ_ADMIN = 100;
    private static final int REQ_LOCATION = 101;

    private EditText etDbUrl, etDbAuth, etSellerPin;
    private EditText etBuyerName, etTotalQist, etMonthly, etPaid;
    private Button btnAdmin, btnLocation, btnFinish, btnQr, btnShowAgreement;
    private TextView tvAdminStatus, tvLocStatus, tvDoStatus, tvConsentFull;
    private android.widget.CheckBox cbConsent;

    public static String getDeviceId(Context ctx) {
        String id = Settings.Secure.getString(ctx.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return (id == null || id.isEmpty()) ? "unknown" : id;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        SharedPreferences p = getSharedPreferences("installtrack", MODE_PRIVATE);
        if (p.getBoolean("setup_done", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_setup);

        etDbUrl = findViewById(R.id.etDbUrl);
        etDbAuth = findViewById(R.id.etDbAuth);
        etSellerPin = findViewById(R.id.etSellerPin);
        etBuyerName = findViewById(R.id.etBuyerName);
        etTotalQist = findViewById(R.id.etTotalQist);
        etMonthly = findViewById(R.id.etMonthly);
        etPaid = findViewById(R.id.etPaid);
        btnAdmin = findViewById(R.id.btnAdmin);
        btnLocation = findViewById(R.id.btnLocation);
        btnFinish = findViewById(R.id.btnFinish);
        btnQr = findViewById(R.id.btnQr);
        btnShowAgreement = findViewById(R.id.btnShowAgreement);
        tvAdminStatus = findViewById(R.id.tvAdminStatus);
        tvLocStatus = findViewById(R.id.tvLocStatus);
        tvDoStatus = findViewById(R.id.tvDoStatus);
        tvConsentFull = findViewById(R.id.tvConsentFull);
        cbConsent = findViewById(R.id.cbConsent);

        btnQr.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SetupActivity.this, QrActivity.class));
            }
        });
        btnShowAgreement.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (tvConsentFull.getVisibility() == View.VISIBLE) {
                    tvConsentFull.setVisibility(View.GONE);
                } else {
                    tvConsentFull.setText(ConsentText.AGREEMENT_URDU
                            + "\n\n" + ConsentText.AGREEMENT_ENGLISH);
                    tvConsentFull.setVisibility(View.VISIBLE);
                }
            }
        });

        btnAdmin.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { requestAdmin(); }
        });
        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { requestLocation(); }
        });
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finishSetup(); }
        });

        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvAdminStatus != null) refreshStatus();
    }

    private boolean isAdminActive() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, TrackerDeviceAdmin.class);
        return dpm != null && dpm.isAdminActive(admin);
    }

    private boolean hasLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshStatus() {
        if (isAdminActive()) {
            tvAdminStatus.setText("✅ Device Admin ON hai");
            btnAdmin.setEnabled(false);
        } else {
            tvAdminStatus.setText("❌ Device Admin OFF hai — ON karna lazmi hai");
            btnAdmin.setEnabled(true);
        }
        // Device Owner status — enrollment (ADB/QR) ka pata chale
        if (DeviceOwnerHelper.isDeviceOwner(this)) {
            tvDoStatus.setText("✅ Device Owner ACTIVE hai — factory reset + uninstall block hai");
        } else {
            tvDoStatus.setText("ℹ️ Device Owner nahi hai — enroll ke liye ADB (dpm set-device-owner) ya QR provisioning use karein (README dekhein)");
        }
        if (hasLocation()) {
            tvLocStatus.setText("✅ Location permission mil gayi");
            btnLocation.setEnabled(false);
        } else {
            tvLocStatus.setText("❌ Location permission darkar hai");
            btnLocation.setEnabled(true);
        }
    }

    private void requestAdmin() {
        ComponentName admin = new ComponentName(this, TrackerDeviceAdmin.class);
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Qist plan ke doran phone ki hifazat ke liye: qist ada na hone par " +
                "seller phone lock kar sakta hai. Qist mukammal hone par ye " +
                "permission khatam kar di jayegi.");
        startActivityForResult(i, REQ_ADMIN);
    }

    private void requestLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, REQ_LOCATION);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        refreshStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refreshStatus();
    }

    private void finishSetup() {
        String dbUrl = etDbUrl.getText().toString().trim();
        if (!dbUrl.startsWith("https://")) {
            Toast.makeText(this, "Database URL ghalat hai (https:// se shuru ho)", Toast.LENGTH_LONG).show();
            return;
        }
        // CONSENT LAZMI: agreement parhe baghair setup aage nahi barhega
        if (cbConsent == null || !cbConsent.isChecked()) {
            Toast.makeText(this, "Pehle sale agreement parhein aur consent checkbox tick karein",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!isAdminActive()) {
            Toast.makeText(this, "Pehle Device Admin ON karein — ye lazmi hai", Toast.LENGTH_LONG).show();
            return;
        }
        if (!hasLocation()) {
            Toast.makeText(this, "Pehle Location permission dein", Toast.LENGTH_LONG).show();
            return;
        }

        String buyer = etBuyerName.getText().toString().trim();
        String total = etTotalQist.getText().toString().trim();
        String monthly = etMonthly.getText().toString().trim();
        String paid = etPaid.getText().toString().trim();
        if (paid.isEmpty()) paid = "0";
        String sellerPin = etSellerPin.getText().toString().trim();

        SharedPreferences p = getSharedPreferences("installtrack", MODE_PRIVATE);
        p.edit()
                .putString("db_url", dbUrl)
                .putString("db_auth", etDbAuth.getText().toString().trim())
                .putString("seller_pin", sellerPin)
                .putString("buyer_name", buyer)
                .putString("total_qist", total)
                .putString("monthly", monthly)
                .putString("paid", paid)
                .putBoolean("consent_accepted", true)
                .putLong("consent_time", System.currentTimeMillis())
                .putString("consent_text_version", ConsentText.CONSENT_VERSION)
                .putBoolean("setup_done", true)
                .apply();

        // Register device in Firebase (PIN ka sirf HASH bhejo — plain PIN kabhi nahi)
        final String fBuyer = buyer, fTotal = total, fMonthly = monthly, fPaid = paid;
        final String fPinHash = DeviceOwnerHelper.pinHashHex(sellerPin);
        new Thread(new Runnable() {
            @Override public void run() {
                FirebaseRest fb = new FirebaseRest();
                fb.load(SetupActivity.this);
                String id = getDeviceId(SetupActivity.this);
                if (fb.isConfigured()) {
                    String info = "{\"name\":" + FirebaseRest.quote(fBuyer)
                            + ",\"total\":" + FirebaseRest.quote(fTotal)
                            + ",\"monthly\":" + FirebaseRest.quote(fMonthly)
                            + ",\"paid\":" + FirebaseRest.quote(fPaid)
                            + ",\"adminActive\":\"true\"}";
                    fb.put("/devices/" + id + "/info", info);
                    fb.put("/devices/" + id + "/status", FirebaseRest.quote("active"));
                    fb.put("/devices/" + id + "/deviceOwner",
                            DeviceOwnerHelper.isDeviceOwner(SetupActivity.this)
                                    ? "true" : "false");
                    fb.put("/devices/" + id + "/consent",
                            "{\"accepted\":true,\"version\":"
                                    + FirebaseRest.quote(ConsentText.CONSENT_VERSION)
                                    + ",\"time\":" + System.currentTimeMillis() + "}");
                    if (!fPinHash.isEmpty()) {
                        fb.put("/devices/" + id + "/sellerPinHash",
                                FirebaseRest.quote(fPinHash));
                    }
                }
            }
        }).start();

        // Start tracker service
        Intent s = new Intent(this, TrackerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(s);
        } else {
            startService(s);
        }

        Toast.makeText(this, "Setup mukammal! Qist Protection active ✅", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
