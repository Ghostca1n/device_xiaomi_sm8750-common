/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.touch;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import vendor.xiaomi.hw.touchfeature.ITouchFeature;

/**
 * Connection to the touch feature HAL. Saved settings are re-applied
 * whenever the HAL comes back after a restart.
 */
public final class TouchFeature implements IBinder.DeathRecipient {

    private static final String TAG = "TouchFeature";
    private static final String SERVICE_NAME = ITouchFeature.DESCRIPTOR + "/default";

    // Main touchscreen
    private static final int TOUCH_ID = 0;

    private static TouchFeature sInstance;

    private final Context mContext;
    private final Handler mHandler;

    private ITouchFeature mService;

    /** Range of a touch mode as reported by the HAL. */
    public static final class ModeRange {
        public final int min;
        public final int max;
        public final int def;

        ModeRange(int min, int max, int def) {
            this.min = min;
            this.max = max;
            this.def = def;
        }

        boolean isValid() {
            return min < max && def >= min && def <= max;
        }
    }

    private TouchFeature(Context context) {
        mContext = context.getApplicationContext();
        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
    }

    public static synchronized TouchFeature getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TouchFeature(context);
        }
        return sInstance;
    }

    public static boolean isSupported() {
        return ServiceManager.isDeclared(SERVICE_NAME);
    }

    /** Waits for the HAL in the background and applies the saved settings. */
    public void restore() {
        mHandler.post(() -> {
            final IBinder binder = ServiceManager.waitForDeclaredService(SERVICE_NAME);
            if (binder == null) {
                Log.e(TAG, "Touch feature HAL is not available");
                return;
            }
            synchronized (this) {
                if (mService == null) {
                    attach(binder);
                }
            }
            TouchUtils.applySavedSettings(mContext, this);
        });
    }

    @Override
    public void binderDied() {
        Log.w(TAG, "Touch feature HAL died");
        synchronized (this) {
            mService = null;
        }
        restore();
    }

    public void setModeValue(int mode, int value) {
        final ITouchFeature service = getService();
        if (service == null) {
            return;
        }
        try {
            service.setModeValue(TOUCH_ID, mode, value);
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Failed to set mode " + mode + " to " + value, e);
        }
    }

    public void resetMode(int mode) {
        final ITouchFeature service = getService();
        if (service == null) {
            return;
        }
        try {
            service.modeReset(TOUCH_ID, mode);
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Failed to reset mode " + mode, e);
        }
    }

    /** Returns the range of the given mode, or the fallback if the HAL has none. */
    public ModeRange getModeRange(int mode, ModeRange fallback) {
        final ITouchFeature service = getService();
        if (service == null) {
            return fallback;
        }
        try {
            final ModeRange range = new ModeRange(
                    service.getModeMinValue(TOUCH_ID, mode),
                    service.getModeMaxValue(TOUCH_ID, mode),
                    service.getModeDefaultValue(TOUCH_ID, mode));
            if (range.isValid()) {
                return range;
            }
            Log.w(TAG, "Invalid range for mode " + mode + ", using fallback");
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Failed to get range of mode " + mode, e);
        }
        return fallback;
    }

    private synchronized ITouchFeature getService() {
        if (mService == null) {
            final IBinder binder = ServiceManager.checkService(SERVICE_NAME);
            if (binder != null) {
                attach(binder);
            }
        }
        return mService;
    }

    private void attach(IBinder binder) {
        try {
            binder.linkToDeath(this, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Touch feature HAL died while connecting", e);
            return;
        }
        mService = ITouchFeature.Stub.asInterface(binder);
    }
}
