package com.hamza.installtrack;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

/**
 * Firebase Realtime Database REST helper.
 * No google-services.json needed — uses the database URL + optional auth token
 * that the seller enters during setup (stored in SharedPreferences).
 */
public class FirebaseRest {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private String dbUrl = "";
    private String auth = "";

    public FirebaseRest() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public void load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences("installtrack", Context.MODE_PRIVATE);
        dbUrl = p.getString("db_url", "").trim();
        if (dbUrl.endsWith("/")) dbUrl = dbUrl.substring(0, dbUrl.length() - 1);
        auth = p.getString("db_auth", "").trim();
    }

    public boolean isConfigured() {
        return dbUrl != null && dbUrl.startsWith("https://");
    }

    private String url(String path) {
        String u = dbUrl + path + ".json";
        if (auth != null && !auth.isEmpty()) u += "?auth=" + auth;
        return u;
    }

    /** GET path -> raw JSON string, or null on failure. */
    public String get(String path) {
        if (!isConfigured()) return null;
        try {
            Request req = new Request.Builder().url(url(path)).get().build();
            Response res = client.newCall(req).execute();
            if (!res.isSuccessful() || res.body() == null) return null;
            return res.body().string();
        } catch (Exception e) {
            return null;
        }
    }

    /** PUT a JSON value at path. Returns true on success. */
    public boolean put(String path, String jsonValue) {
        if (!isConfigured()) return false;
        try {
            RequestBody body = RequestBody.create(jsonValue, JSON);
            Request req = new Request.Builder().url(url(path)).put(body).build();
            Response res = client.newCall(req).execute();
            return res.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /** DELETE path (used to clear a processed command). */
    public boolean delete(String path) {
        if (!isConfigured()) return false;
        try {
            Request req = new Request.Builder().url(url(path)).delete().build();
            Response res = client.newCall(req).execute();
            return res.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /** PATCH a JSON object at path (merges fields). */
    public boolean patch(String path, String jsonObject) {
        if (!isConfigured()) return false;
        try {
            RequestBody body = RequestBody.create(jsonObject, JSON);
            Request req = new Request.Builder().url(url(path)).patch(body).build();
            Response res = client.newCall(req).execute();
            return res.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /** Strips surrounding quotes from a JSON string value. "abc" -> abc ; null -> null */
    public static String unquote(String json) {
        if (json == null) return null;
        json = json.trim();
        if (json.equals("null")) return null;
        if (json.length() >= 2 && json.startsWith("\"") && json.endsWith("\"")) {
            return json.substring(1, json.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n");
        }
        return json;
    }

    /** JSON-escapes a plain string for embedding in a JSON value. */
    public static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
