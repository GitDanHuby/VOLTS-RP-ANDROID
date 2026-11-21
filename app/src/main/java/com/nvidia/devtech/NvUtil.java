package com.nvidia.devtech;

import android.app.Activity;

/**
 * Compatibility bridge for the original NVIDIA package name expected by libGame.so.
 * The app's implementation lives under com.voltsrp.mobile.nvidia.devtech.
 */
public final class NvUtil {
    private static final NvUtil INSTANCE = new NvUtil();

    private NvUtil() {
    }

    public static NvUtil getInstance() {
        return INSTANCE;
    }

    private com.voltsrp.mobile.nvidia.devtech.NvUtil delegate() {
        return com.voltsrp.mobile.nvidia.devtech.NvUtil.getInstance();
    }

    public String getAppLocalValue(String key) {
        return delegate().getAppLocalValue(key);
    }

    public String getParameter(String key) {
        return delegate().getParameter(key);
    }

    public boolean hasAppLocalValue(String key) {
        return delegate().hasAppLocalValue(key);
    }

    public void setActivity(Activity activity) {
        delegate().setActivity(activity);
    }

    public void setAppLocalValue(String key, String value) {
        delegate().setAppLocalValue(key, value);
    }
}
