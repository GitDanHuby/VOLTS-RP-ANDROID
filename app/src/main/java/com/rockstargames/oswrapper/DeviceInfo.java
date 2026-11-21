package com.rockstargames.oswrapper;

/**
 * Compatibility type expected by the bundled libGame.so JNI implementation.
 * Keep this class and its members stable: native code resolves this exact
 * package name at runtime.
 */
public final class DeviceInfo {
    public final int cpuFrequency;
    public final String hardware;
    public final boolean hasTouchScreen;
    public final boolean hasVibrator;
    public final boolean isPhone;
    public final boolean isTvDevice;
    public final String manufacturer;
    public final String model;
    public final int osVersion;
    public final String product;

    public DeviceInfo(com.voltsrp.mobile.oswrapper.DeviceInfo source) {
        if (source == null) throw new NullPointerException("source is null");
        this.cpuFrequency = source.cpuFrequency;
        this.hardware = source.hardware;
        this.hasTouchScreen = source.hasTouchScreen;
        this.hasVibrator = source.hasVibrator;
        this.isPhone = source.isPhone;
        this.isTvDevice = source.isTvDevice;
        this.manufacturer = source.manufacturer;
        this.model = source.model;
        this.osVersion = source.osVersion;
        this.product = source.product;
    }
}
