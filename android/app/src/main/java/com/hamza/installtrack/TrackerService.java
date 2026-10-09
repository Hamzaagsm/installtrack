package com.hamza.installtrack;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service: polls Firebase for seller commands and uploads location.
 * Runs as a visible foreground service ("Qist Protection Active") — Android
 * requires this; it cannot be fully hidden, which keeps the setup honest.
 */
public class TrackerService extends Service {

    private static final String CH_ID = "tracker_service";
    private static final long POLL_MS = 30_000;      // command check every 30s
    private static final long LOCATION_MS = 15 * 60_000; // location every 15 min

    private ExecutorService exec;
    private Handler handler;
    private FirebaseRest fb;
    private String deviceId;
    private LocationManager locationManager;
    private MediaPlayer ringPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        exec = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        fb = new FirebaseRest();
        fb.load(this);
        deviceId = SetupActivity.getDeviceId(this);
        createChannel();
        startForeground(1, buildNotification("Qist Protection Active"));
        schedulePoll();
        scheduleLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CH_ID, "Qist Protection",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Qist plan device protection service");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(this, CH_ID)
                : new Notification.Builder(this);
        return b.setContentTitle("Hamza Mobile — Qist Plan")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pi)
                .build();
    }

    // ---------- command polling ----------

    private void schedulePoll() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                exec.execute(new Runnable() {
                    @Override public void run() { pollCommand(); }
                });
                schedulePoll();
            }
        }, POLL_MS);
    }

    private void pollCommand() {
        if (!fb.isConfigured() || deviceId == null) return;
        String raw = fb.get("/devices/" + deviceId + "/command");
        String cmd = FirebaseRest.unquote(raw);
        if (cmd == null || cmd.isEmpty()) return;

        // Clear the command first so it never runs twice
        fb.delete("/devices/" + deviceId + "/command");

        if (cmd.equals("LOCK")) {
            doLock();
        } else if (cmd.equals("UNLOCK")) {
            doUnlock();
        } else if (cmd.equals("RING")) {
            doRing();
        } else if (cmd.startsWith("MESSAGE:")) {
            doMessage(cmd.substring("MESSAGE:".length()));
        } else if (cmd.startsWith("WIPE:")) {
            doWipe(cmd.substring("WIPE:".length()));
        } else if (cmd.equals("RELEASE")) {
            doRelease();
        }
    }

    // ---------- command handlers ----------

    private void doLock() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, TrackerDeviceAdmin.class);
        String msg = fb.get("/devices/" + deviceId + "/lockMessage");
        String lockMsg = FirebaseRest.unquote(msg);
        if (lockMsg == null || lockMsg.isEmpty()) {
            lockMsg = "Qist ada karein — Hamza Mobile se rabta karein";
        }
        final String finalMsg = lockMsg;

        if (dpm != null && dpm.isAdminActive(admin)) {
            try { dpm.lockNow(); } catch (Exception ignored) {}
        }
        fb.put("/devices/" + deviceId + "/status", FirebaseRest.quote("locked"));
        fb.put("/devices/" + deviceId + "/lockMessage", FirebaseRest.quote(finalMsg));

        // Show the lock overlay on next screen-on / immediately
        handler.post(new Runnable() {
            @Override public void run() {
                Intent i = new Intent(TrackerService.this, LockActivity.class);
                i.putExtra("message", finalMsg);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });
    }

    private void doUnlock() {
        fb.put("/devices/" + deviceId + "/status", FirebaseRest.quote("active"));
        handler.post(new Runnable() {
            @Override public void run() {
                Intent i = new Intent("com.hamza.installtrack.UNLOCKED");
                sendBroadcast(i);
            }
        });
    }

    private void doRing() {
        handler.post(new Runnable() {
            @Override public void run() {
                try {
                    stopRing();
                    Uri tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    if (tone == null) tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    ringPlayer = MediaPlayer.create(TrackerService.this, tone);
                    if (ringPlayer != null) {
                        ringPlayer.setLooping(true);
                        ringPlayer.start();
                        handler.postDelayed(new Runnable() {
                            @Override public void run() { stopRing(); }
                        }, 60_000); // ring max 60s
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void stopRing() {
        try {
            if (ringPlayer != null) {
                if (ringPlayer.isPlaying()) ringPlayer.stop();
                ringPlayer.release();
                ringPlayer = null;
            }
        } catch (Exception ignored) {}
    }

    private void doMessage(final String text) {
        handler.post(new Runnable() {
            @Override public void run() {
                Intent i = new Intent(TrackerService.this, LockActivity.class);
                i.putExtra("message", text);
                i.putExtra("info_only", true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });
    }

    private void doWipe(String pin) {
        SharedPreferences p = getSharedPreferences("installtrack", MODE_PRIVATE);
        String sellerPin = p.getString("seller_pin", "");
        if (sellerPin.isEmpty() || !sellerPin.equals(pin)) return; // wrong PIN -> ignore
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, TrackerDeviceAdmin.class);
        if (dpm != null && dpm.isAdminActive(admin)) {
            try { dpm.wipeData(0); } catch (Exception ignored) {}
        }
    }

    /**
     * RELEASE: Qist mukammal! Seller ne dashboard se release kiya.
     * Device Admin deactivate karo taake app uninstall ho sake, phir
     * buyer ko one-tap uninstall ka option do.
     */
    private void doRelease() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, TrackerDeviceAdmin.class);
        try {
            if (dpm != null && dpm.isAdminActive(admin)) {
                dpm.removeActiveAdmin(admin);
            }
        } catch (Exception ignored) {}

        if (fb.isConfigured() && deviceId != null) {
            fb.put("/devices/" + deviceId + "/status", FirebaseRest.quote("released"));
            fb.put("/devices/" + deviceId + "/adminActive", "false");
        }

        handler.post(new Runnable() {
            @Override public void run() {
                // Service rok do — ab tracking ki zaroorat nahi
                stopSelf();
                // Buyer ko one-tap uninstall offer karo
                try {
                    Intent i = new Intent(Intent.ACTION_DELETE);
                    i.setData(android.net.Uri.parse("package:" + getPackageName()));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception ignored) {}
            }
        });
    }

    // ---------- location ----------

    private void scheduleLocation() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                exec.execute(new Runnable() {
                    @Override public void run() { uploadLocation(); }
                });
                scheduleLocation();
            }
        }, LOCATION_MS);
        // also upload once at start
        exec.execute(new Runnable() {
            @Override public void run() { uploadLocation(); }
        });
    }

    private void uploadLocation() {
        if (!fb.isConfigured() || deviceId == null) return;
        try {
            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            }
            Location loc = null;
            try {
                loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } catch (SecurityException ignored) {}

            if (loc != null) {
                long now = System.currentTimeMillis();
                String json = "{\"lat\":" + loc.getLatitude()
                        + ",\"lng\":" + loc.getLongitude()
                        + ",\"acc\":" + loc.getAccuracy()
                        + ",\"time\":" + now + "}";
                fb.put("/devices/" + deviceId + "/location", json);
                fb.put("/devices/" + deviceId + "/lastSeen", String.valueOf(now));
            } else {
                // request a fresh fix; last-known may be stale
                try {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                            new LocationListener() {
                                @Override public void onLocationChanged(Location l) {
                                    long now = System.currentTimeMillis();
                                    String json = "{\"lat\":" + l.getLatitude()
                                            + ",\"lng\":" + l.getLongitude()
                                            + ",\"acc\":" + l.getAccuracy()
                                            + ",\"time\":" + now + "}";
                                    fb.put("/devices/" + deviceId + "/location", json);
                                    fb.put("/devices/" + deviceId + "/lastSeen", String.valueOf(now));
                                }
                                @Override public void onStatusChanged(String s, int i, Bundle b) {}
                                @Override public void onProviderEnabled(String s) {}
                                @Override public void onProviderDisabled(String s) {}
                            }, Looper.getMainLooper());
                } catch (SecurityException ignored) {}
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        stopRing();
        if (exec != null) exec.shutdownNow();
        super.onDestroy();
    }
}
