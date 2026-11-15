/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.saturation;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

import androidx.preference.PreferenceManager;

public final class SaturationUtils {

    private static final String TAG = "SaturationUtils";

    static final String KEY_SATURATION = "display_saturation";
    static final int DEFAULT_SATURATION = 100;

    private static final String SURFACE_FLINGER_SERVICE = "SurfaceFlinger";
    private static final String SURFACE_COMPOSER_INTERFACE = "android.ui.ISurfaceComposer";
    private static final int SURFACE_FLINGER_TRANSACTION_SATURATION = 1022;

    // Saturation ColorDisplayService applies for the selected color mode
    private static final String PROP_COLOR_MODE_SATURATION = "persist.sys.sf.color_saturation";

    private SaturationUtils() {
        // This class is not supposed to be instantiated
    }

    public static void onBootCompleted(Context context) {
        final int saturation = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_SATURATION, DEFAULT_SATURATION);
        if (saturation != DEFAULT_SATURATION) {
            setSaturation(saturation);
        }
    }

    /** Sets the global saturation in percent; the default restores the color mode's own. */
    static void setSaturation(int percent) {
        final float saturation = percent == DEFAULT_SATURATION
                ? getColorModeSaturation() : percent / 100f;

        final IBinder flinger = ServiceManager.getService(SURFACE_FLINGER_SERVICE);
        if (flinger == null) {
            Log.e(TAG, "SurfaceFlinger is not available");
            return;
        }

        final Parcel data = Parcel.obtain();
        data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE);
        data.writeFloat(saturation);
        try {
            flinger.transact(SURFACE_FLINGER_TRANSACTION_SATURATION, data, null, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set saturation", e);
        } finally {
            data.recycle();
        }
    }

    private static float getColorModeSaturation() {
        try {
            return Float.parseFloat(SystemProperties.get(PROP_COLOR_MODE_SATURATION, "1.0"));
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }
}
