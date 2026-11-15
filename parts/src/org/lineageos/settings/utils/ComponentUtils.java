/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public final class ComponentUtils {

    private ComponentUtils() {
        // This class is not supposed to be instantiated
    }

    /**
     * Enables or disables one of our components, e.g. to hide the settings
     * tile of a feature the device does not support.
     */
    public static void setComponentEnabled(Context context, Class<?> cls, boolean enabled) {
        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, cls),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
