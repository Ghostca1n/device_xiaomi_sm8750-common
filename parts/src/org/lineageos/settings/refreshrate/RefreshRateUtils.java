/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.refreshrate;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.Display;

import org.lineageos.settings.utils.ComponentUtils;

import java.util.TreeSet;

public final class RefreshRateUtils {

    /** Per-package refresh rate in Hz, keyed by package name. */
    static final String PREFS_NAME = "refresh_rate_apps";
    static final String REFRESH_RATE_DEFAULT = "0";

    // The user's own settings, saved while an app forces a refresh rate
    private static final String STATE_PREFS_NAME = "refresh_rate_state";
    private static final String KEY_OVERRIDE_ACTIVE = "override_active";
    private static final String KEY_SAVED_PEAK_REFRESH_RATE = "saved_peak_refresh_rate";
    private static final String KEY_SAVED_MIN_REFRESH_RATE = "saved_min_refresh_rate";

    private RefreshRateUtils() {
        // This class is not supposed to be instantiated
    }

    public static void onBootCompleted(Context context) {
        final boolean supported = getSupportedRefreshRates(context).length > 1;
        ComponentUtils.setComponentEnabled(context, RefreshRateActivity.class, supported);
        if (supported) {
            context.startServiceAsUser(new Intent(context, RefreshRateService.class),
                    UserHandle.CURRENT);
        } else {
            restoreUserRefreshRate(context);
        }
    }

    /** Distinct refresh rates of the default display at its current resolution. */
    static int[] getSupportedRefreshRates(Context context) {
        final Display display = context.getSystemService(DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);
        final Display.Mode currentMode = display.getMode();
        final TreeSet<Integer> refreshRates = new TreeSet<>();
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() == currentMode.getPhysicalWidth()
                    && mode.getPhysicalHeight() == currentMode.getPhysicalHeight()) {
                refreshRates.add(Math.round(mode.getRefreshRate()));
            }
        }
        return refreshRates.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Returns the refresh rate forced for the package, or 0 if it uses the default. */
    static int getRefreshRateForPackage(Context context, String packageName) {
        final String value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(packageName, REFRESH_RATE_DEFAULT);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static void applyRefreshRate(Context context, int refreshRate) {
        final ContentResolver resolver = context.getContentResolver();
        final SharedPreferences state = getStatePrefs(context);
        if (!state.getBoolean(KEY_OVERRIDE_ACTIVE, false)) {
            // Absent settings are saved as absent and restored as such
            state.edit()
                    .putBoolean(KEY_OVERRIDE_ACTIVE, true)
                    .putString(KEY_SAVED_PEAK_REFRESH_RATE,
                            getSetting(resolver, Settings.System.PEAK_REFRESH_RATE))
                    .putString(KEY_SAVED_MIN_REFRESH_RATE,
                            getSetting(resolver, Settings.System.MIN_REFRESH_RATE))
                    .commit();
        }
        putSetting(resolver, Settings.System.PEAK_REFRESH_RATE, Float.toString(refreshRate));
        putSetting(resolver, Settings.System.MIN_REFRESH_RATE, Float.toString(refreshRate));
    }

    static void restoreUserRefreshRate(Context context) {
        final SharedPreferences state = getStatePrefs(context);
        if (!state.getBoolean(KEY_OVERRIDE_ACTIVE, false)) {
            return;
        }
        final ContentResolver resolver = context.getContentResolver();
        putSetting(resolver, Settings.System.PEAK_REFRESH_RATE,
                state.getString(KEY_SAVED_PEAK_REFRESH_RATE, null));
        putSetting(resolver, Settings.System.MIN_REFRESH_RATE,
                state.getString(KEY_SAVED_MIN_REFRESH_RATE, null));
        state.edit().clear().commit();
    }

    private static SharedPreferences getStatePrefs(Context context) {
        return context.getSharedPreferences(STATE_PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String getSetting(ContentResolver resolver, String name) {
        return Settings.System.getStringForUser(resolver, name, UserHandle.USER_CURRENT);
    }

    private static void putSetting(ContentResolver resolver, String name, String value) {
        Settings.System.putStringForUser(resolver, name, value, UserHandle.USER_CURRENT);
    }
}
