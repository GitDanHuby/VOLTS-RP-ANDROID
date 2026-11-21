package com.voltsrp.mobile.oswrapper;

import android.content.res.AssetManager;
import android.view.Surface;
import android.util.Log;

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

     
    public static void implOnInitialSetup(DeviceInfo deviceInfo, AssetManager assets, String[] assetPackNames, String[] assetPackPaths) {
        final String tag = "VOLTS-JNI-DIAG";
        final ClassLoader nativeLoader =
                com.rockstargames.oswrapper.GameNative.class.getClassLoader();
        try {
            Log.i(tag, "INITIAL_SETUP_BEGIN mobileLoader=" + GameNative.class.getClassLoader()
                    + " nativeLoader=" + nativeLoader);

            Class<?> servicesClass = Class.forName(
                    "com.rockstargames.oswrapper.GamePlatformServices", true, nativeLoader);
            java.lang.reflect.Method quitMethod = servicesClass.getMethod("quit");
            Log.i(tag, "SERVICE_CLASS_OK name=" + servicesClass.getName()
                    + " loader=" + servicesClass.getClassLoader()
                    + " quit=" + quitMethod.toGenericString());

            Class<?> deviceClass = Class.forName(
                    "com.rockstargames.oswrapper.DeviceInfo", true, nativeLoader);
            Log.i(tag, "DEVICE_CLASS_OK name=" + deviceClass.getName()
                    + " loader=" + deviceClass.getClassLoader());

            com.rockstargames.oswrapper.DeviceInfo legacyDevice =
                    new com.rockstargames.oswrapper.DeviceInfo(deviceInfo);
            Log.i(tag, "DEVICE_ADAPTER_OK value=" + legacyDevice
                    + " assets=" + (assets != null)
                    + " assetPackNames=" + (assetPackNames == null ? "null" : assetPackNames.length)
                    + " assetPackPaths=" + (assetPackPaths == null ? "null" : assetPackPaths.length));

            Log.i(tag, "CALLING_LIBGAME_INITIAL_SETUP");
            com.rockstargames.oswrapper.GameNative.implOnInitialSetup(
                    legacyDevice, assets, assetPackNames, assetPackPaths);
            Log.i(tag, "LIBGAME_INITIAL_SETUP_RETURNED");
        } catch (Throwable error) {
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
