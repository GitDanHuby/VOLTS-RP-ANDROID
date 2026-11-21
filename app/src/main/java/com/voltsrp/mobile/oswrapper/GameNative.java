package com.voltsrp.mobile.oswrapper;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Surface;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.bytedance.shadowhook.ShadowHook;


public class GameNative {
    public static GameNative INSTANCE = new GameNative();

    static {
        ShadowHook.init(new ShadowHook.ConfigBuilder()
                .setMode(ShadowHook.Mode.UNIQUE)
                .build());
        System.loadLibrary("Game");
        System.loadLibrary("samp");
    }

    private GameNative() {
    }

    public static native void setStoragePath(String path);

    public static native void initializeSAMP();

    public static native boolean implIsInitialized();


    public static void implOnAccelerometerChanged(float x, float y, float z) {
        com.rockstargames.oswrapper.GameNative.implOnAccelerometerChanged(x, y, z);
    }

     
    public static void implOnActivityCreated(GamePlatformServices services, boolean firstInit) {
        com.rockstargames.oswrapper.GameNative.implOnActivityCreated(new com.rockstargames.oswrapper.GamePlatformServices(services.activity, services.view), firstInit);
    }

     
    public static void implOnActivityDestroyed() {
        com.rockstargames.oswrapper.GameNative.implOnActivityDestroyed();
    }

     
    public static void implOnBackButtonPressed() {
        com.rockstargames.oswrapper.GameNative.implOnBackButtonPressed();
    }

     
    public static void implOnDrawFrame(float dt) {
        com.rockstargames.oswrapper.GameNative.implOnDrawFrame(dt);
    }

     
    public static void implOnGamepadAxesChanged(int controllerId, float x, float y, float z, float rz, float triggerL, float triggerR) {
        com.rockstargames.oswrapper.GameNative.implOnGamepadAxesChanged(controllerId, x, y, z, rz, triggerL, triggerR);
    }

     
    public static void implOnGamepadButtonDown(int controllerId, int buttonId) {
        com.rockstargames.oswrapper.GameNative.implOnGamepadButtonDown(controllerId, buttonId);
    }

     
    public static void implOnGamepadButtonUp(int controllerId, int buttonId) {
        com.rockstargames.oswrapper.GameNative.implOnGamepadButtonUp(controllerId, buttonId);
    }

     
    public static void implOnGamepadCountChanged(int count) {
        com.rockstargames.oswrapper.GameNative.implOnGamepadCountChanged(count);
    }

     
    public static void implOnHttpRequestData(int id, byte[] data) {
        com.rockstargames.oswrapper.GameNative.implOnHttpRequestData(id, data);
    }

     
    public static void implOnHttpRequestError(int id, int statusCode) {
        com.rockstargames.oswrapper.GameNative.implOnHttpRequestError(id, statusCode);
    }

     
    public static void implOnHttpRequestFinished(int id) {
        com.rockstargames.oswrapper.GameNative.implOnHttpRequestFinished(id);
    }

     
    public static void implOnHttpRequestResponse(int id, int statusCode, String statusLine, String[] headerNames, String[] headerValues) {
        com.rockstargames.oswrapper.GameNative.implOnHttpRequestResponse(id, statusCode, statusLine, headerNames, headerValues);
    }

     
    /*
     * Saves a tiny diagnostic directly in Downloads on Android 10+.
     * It deliberately runs before entering libGame.so because a fatal JNI
     * abort cannot be caught by Java.
     */
    private static void saveJniDiagnostic(String details) {
        final String fileName = "VOLTS-JNI-DIAG.txt";
        Context context = GameActivityBase.getInstance();
        if (context == null) {
            Log.e("VOLTS-JNI-DIAG", "No Activity context; diagnostic was not exported");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                Uri fileUri = null;
                String relativePath = Environment.DIRECTORY_DOWNLOADS + "/";
                String[] projection = { MediaStore.MediaColumns._ID };
                String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? AND "
                        + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
                try (Cursor cursor = context.getContentResolver().query(
                        collection, projection, selection,
                        new String[]{fileName, relativePath}, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(0);
                        fileUri = ContentUris.withAppendedId(collection, id);
                    }
                }

                if (fileUri == null) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    fileUri = context.getContentResolver().insert(collection, values);
                }
                if (fileUri == null) throw new IllegalStateException("Downloads insert returned null");

                try (OutputStream out = context.getContentResolver().openOutputStream(fileUri, "wt")) {
                    if (out == null) throw new IllegalStateException("Could not open diagnostic output");
                    out.write(details.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.flush();
                }

                ContentValues ready = new ContentValues();
                ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
                context.getContentResolver().update(fileUri, ready, null, null);
            } else {
                File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (directory == null) throw new IllegalStateException("External files directory unavailable");
                File file = new File(directory, fileName);
                try (FileOutputStream out = new FileOutputStream(file, false)) {
                    out.write(details.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    out.flush();
                }
                Log.i("VOLTS-JNI-DIAG", "Android 9 fallback path=" + file.getAbsolutePath());
            }
            Log.i("VOLTS-JNI-DIAG", "Diagnostic saved: " + fileName);
        } catch (Throwable error) {
            Log.e("VOLTS-JNI-DIAG", "Could not save diagnostic file", error);
        }
    }

    public static void implOnInitialSetup(DeviceInfo deviceInfo, AssetManager assets, String[] assetPackNames, String[] assetPackPaths) {
        final String tag = "VOLTS-JNI-DIAG";
        final ClassLoader nativeLoader =
                com.rockstargames.oswrapper.GameNative.class.getClassLoader();
        StringBuilder report = new StringBuilder();
        report.append("VOLTS RP - JNI diagnostic\n")
                .append("timeMillis=").append(System.currentTimeMillis()).append('\n')
                .append("thread=").append(Thread.currentThread().getName()).append('\n')
                .append("sdk=").append(Build.VERSION.SDK_INT).append('\n')
                .append("mobileLoader=").append(GameNative.class.getClassLoader()).append('\n')
                .append("nativeLoader=").append(nativeLoader).append('\n')
                .append("threadContextLoader=").append(Thread.currentThread().getContextClassLoader()).append('\n');

        try {
            Class<?> servicesClass = Class.forName(
                    "com.rockstargames.oswrapper.GamePlatformServices", true, nativeLoader);
            java.lang.reflect.Method quitMethod = servicesClass.getMethod("quit");
            report.append("SERVICE_CLASS_OK name=").append(servicesClass.getName())
                    .append(" loader=").append(servicesClass.getClassLoader())
                    .append(" quit=").append(quitMethod.toGenericString()).append('\n');

            Class<?> deviceClass = Class.forName(
                    "com.rockstargames.oswrapper.DeviceInfo", true, nativeLoader);
            report.append("DEVICE_CLASS_OK name=").append(deviceClass.getName())
                    .append(" loader=").append(deviceClass.getClassLoader()).append('\n');

            com.rockstargames.oswrapper.DeviceInfo legacyDevice =
                    new com.rockstargames.oswrapper.DeviceInfo(deviceInfo);
            report.append("DEVICE_ADAPTER_OK assets=").append(assets != null)
                    .append(" assetPackNames=").append(assetPackNames == null ? "null" : assetPackNames.length)
                    .append(" assetPackPaths=").append(assetPackPaths == null ? "null" : assetPackPaths.length).append('\n');

            try {
                String nativeProbe = com.rockstargames.oswrapper.GameNative.diagnosticFindClass();
                report.append("SAMP_NATIVE_PROBE=").append(nativeProbe).append('\n');
            } catch (Throwable probeError) {
                report.append("SAMP_NATIVE_PROBE_FAILED=")
                        .append(probeError.getClass().getName()).append(": ")
                        .append(probeError.getMessage()).append('\n');
                Log.e(tag, "Independent libsamp JNI probe failed; continuing to libGame", probeError);
            }

            report.append("CALLING_LIBGAME_INITIAL_SETUP\n");
            saveJniDiagnostic(report.toString());
            Log.i(tag, "Calling libGame initial setup; diagnostic exported");
            com.rockstargames.oswrapper.GameNative.implOnInitialSetup(
                    legacyDevice, assets, assetPackNames, assetPackPaths);
            report.append("LIBGAME_INITIAL_SETUP_RETURNED\n");
            saveJniDiagnostic(report.toString());
        } catch (Throwable error) {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            report.append("INITIAL_SETUP_PREFLIGHT_FAILED\n").append(trace);
            saveJniDiagnostic(report.toString());
            Log.e(tag, "INITIAL_SETUP_PREFLIGHT_FAILED", error);
            throw new IllegalStateException("JNI initial setup diagnostic failed", error);
        }
    }

     
    public static void implOnLowMemory() {
        com.rockstargames.oswrapper.GameNative.implOnLowMemory();
    }

     
    public static void implOnNetworkChanged(int network) {
        com.rockstargames.oswrapper.GameNative.implOnNetworkChanged(network);
    }

     
    public static void implOnPause() {
        com.rockstargames.oswrapper.GameNative.implOnPause();
    }

     
    public static void implOnPlaylistOpenComplete(boolean available, int count) {
        com.rockstargames.oswrapper.GameNative.implOnPlaylistOpenComplete(available, count);
    }

     
    public static void implOnResume() {
        com.rockstargames.oswrapper.GameNative.implOnResume();
    }

     
    public static void implOnRockstarAccountDeletionComplete() {
        com.rockstargames.oswrapper.GameNative.implOnRockstarAccountDeletionComplete();
    }

     
    public static void implOnRockstarCloudDisabledComplete() {
        com.rockstargames.oswrapper.GameNative.implOnRockstarCloudDisabledComplete();
    }

     
    public static void implOnRockstarGateComplete(int id, boolean pass) {
        com.rockstargames.oswrapper.GameNative.implOnRockstarGateComplete(id, pass);
    }

     
    public static void implOnRockstarIdChanged(String id) {
        com.rockstargames.oswrapper.GameNative.implOnRockstarIdChanged(id);
    }

     
    public static void implOnRockstarInitialComplete() {
        com.rockstargames.oswrapper.GameNative.implOnRockstarInitialComplete();
    }

     
    public static void implOnRockstarSetup(String environment, String rockstarId) {
        com.rockstargames.oswrapper.GameNative.implOnRockstarSetup(environment, rockstarId);
    }

     
    public static void implOnRockstarSignInComplete() {
        com.rockstargames.oswrapper.GameNative.implOnRockstarSignInComplete();
    }

     
    public static void implOnRockstarSignOutComplete() {
        com.rockstargames.oswrapper.GameNative.implOnRockstarSignOutComplete();
    }

     
    public static void implOnRockstarStateChanged(boolean pauseGameplay) {
        com.rockstargames.oswrapper.GameNative.implOnRockstarStateChanged(pauseGameplay);
    }

     
    public static void implOnRockstarTicketChanged(String ticket) {
        com.rockstargames.oswrapper.GameNative.implOnRockstarTicketChanged(ticket);
    }

     
    public static void implOnSurfaceChanged(Surface surface, int width, int height) {
        com.rockstargames.oswrapper.GameNative.implOnSurfaceChanged(surface, width, height);
    }

     
    public static void implOnSurfaceCreated() {
        com.rockstargames.oswrapper.GameNative.implOnSurfaceCreated();
    }

     
    public static void implOnSurfaceDestroyed() {
        com.rockstargames.oswrapper.GameNative.implOnSurfaceDestroyed();
    }

     
    public static void implOnTouchEnd(int pointerId, float x, float y) {
        com.rockstargames.oswrapper.GameNative.implOnTouchEnd(pointerId, x, y);
    }

     
    public static void implOnTouchMove(int pointerId, float x, float y) {
        com.rockstargames.oswrapper.GameNative.implOnTouchMove(pointerId, x, y);
    }

     
    public static void implOnTouchStart(int pointerId, float x, float y) {
        com.rockstargames.oswrapper.GameNative.implOnTouchStart(pointerId, x, y);
    }
}
