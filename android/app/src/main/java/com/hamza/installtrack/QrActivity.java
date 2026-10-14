package com.hamza.installtrack;

import android.app.Activity;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONObject;

/**
 * QR provisioning helper — InstallTrack v2.0.
 *
 * Ye screen Device Owner enrollment ke QR tareeqe ke liye provisioning
 * JSON + QR code banati hai. QR code phone ke SETUP WIZARD me scan hota
 * hai (factory reset ke baad, welcome screen par 6 dafa tap karke QR
 * provisioning khulti hai).
 *
 * AHEM:
 *  - Signature checksum is app ke APNE signature se live compute hota hai
 *    (koi hardcoded checksum nahi).
 *  - APK download URL seller ko khud bharna hai — wohi APK host hona
 *    chahiye jiska signature is phone par hai (warna system verify karke
 *    reject kar dega).
 *  - Ye sirf enrollment ka helper hai. Zero-touch auto-re-enrollment is
 *    me NAHI hai (uske liye Google reseller-portal registration chahiye).
 */
public class QrActivity extends Activity {

    private EditText etApkUrl;
    private ImageView ivQr;
    private TextView tvJson;
    private Button btnGenerate;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_qr);

        etApkUrl = findViewById(R.id.etApkUrl);
        ivQr = findViewById(R.id.ivQr);
        tvJson = findViewById(R.id.tvQrJson);
        btnGenerate = findViewById(R.id.btnGenerateQr);

        // Default: GitHub release URL pattern — seller apni asli URL se badle
        etApkUrl.setText("https://github.com/Hamzaagsm/installtrack/releases/download/v2.0-apk/InstallTrack-v2.apk");

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { generateQr(); }
        });

        generateQr();
    }

    private void generateQr() {
        String apkUrl = etApkUrl.getText().toString().trim();
        if (!apkUrl.startsWith("https://")) {
            Toast.makeText(this, "APK download URL https:// se shuru honi chahiye",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String checksum = DeviceOwnerHelper.getSignatureChecksum(this);
        if (checksum.isEmpty()) {
            Toast.makeText(this, "Signature checksum nahi mila — dobara try karein",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            ComponentName admin = DeviceOwnerHelper.adminComponent(this);
            JSONObject json = new JSONObject();
            json.put("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME",
                    admin.flattenToShortString());
            json.put("android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM",
                    checksum);
            json.put("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION",
                    apkUrl);
            json.put("android.app.extra.PROVISIONING_SKIP_ENCRYPTION", true);
            json.put("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", true);
            json.put("android.app.extra.PROVISIONING_SKIP_EDUCATION", true);

            String jsonStr = json.toString();
            tvJson.setText(jsonStr);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(jsonStr, BarcodeFormat.QR_CODE, 512, 512);
            Bitmap bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivQr.setImageBitmap(bmp);
        } catch (Exception e) {
            Toast.makeText(this, "QR banane me error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
