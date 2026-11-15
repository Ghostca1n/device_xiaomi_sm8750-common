/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.dcdimming;

import android.content.Context;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.ComponentUtils;
import org.lineageos.settings.utils.FileUtils;

public final class DcDimmingUtils {

    static final String KEY_DC_DIMMING = "dc_dimming";

    private static final String DISP_PARAM_NODE = "/sys/class/mi_display/disp-DSI-0/disp_param";
    // DISP_FEATURE_DC in the mi_disp driver
    private static final int DISP_FEATURE_DC = 8;

    private DcDimmingUtils() {
        // This class is not supposed to be instantiated
    }

    public static void onBootCompleted(Context context) {
        final boolean supported = isSupported();
        ComponentUtils.setComponentEnabled(context, DcDimmingActivity.class, supported);
        if (supported && PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_DC_DIMMING, false)) {
            setEnabled(true);
        }
    }

    static boolean isSupported() {
        return FileUtils.isFileWritable(DISP_PARAM_NODE);
    }

    static void setEnabled(boolean enabled) {
        FileUtils.writeLine(DISP_PARAM_NODE, DISP_FEATURE_DC + " " + (enabled ? 1 : 0));
    }
}
