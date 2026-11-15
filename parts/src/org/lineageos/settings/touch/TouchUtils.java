/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.touch;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.ComponentUtils;

public final class TouchUtils {

    static final String KEY_GAME_MODE = "touch_game_mode";
    static final String KEY_GLOVE_MODE = "touch_glove_mode";

    static final int MODE_GAME = 0;
    static final int MODE_GLOVE = 101;

    /** A touch mode that is backed by an integer preference. */
    static final class Tunable {
        final String key;
        final int mode;
        final TouchFeature.ModeRange fallback;

        Tunable(String key, int mode, int min, int max, int def) {
            this.key = key;
            this.mode = mode;
            this.fallback = new TouchFeature.ModeRange(min, max, def);
        }
    }

    /** Tunables that only apply while game mode is enabled. */
    static final Tunable[] GAME_MODE_TUNABLES = {
        new Tunable("touch_up_threshold", 2, 0, 4, 3),
        new Tunable("touch_tolerance", 3, 0, 4, 2),
        new Tunable("touch_aim_sensitivity", 4, 0, 4, 2),
        new Tunable("touch_tap_stability", 5, 0, 4, 2),
    };

    static final Tunable EDGE_SUPPRESSION = new Tunable("touch_edge_suppression", 7, 0, 3, 2);

    private TouchUtils() {
        // This class is not supposed to be instantiated
    }

    public static void onBootCompleted(Context context) {
        final boolean supported = TouchFeature.isSupported();
        ComponentUtils.setComponentEnabled(context, TouchActivity.class, supported);
        if (supported) {
            TouchFeature.getInstance(context).restore();
        }
    }

    /** Applies the settings the user changed; untouched modes keep the HAL default. */
    static void applySavedSettings(Context context, TouchFeature touch) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains(KEY_GLOVE_MODE)) {
            touch.setModeValue(MODE_GLOVE, prefs.getBoolean(KEY_GLOVE_MODE, false) ? 1 : 0);
        }
        if (prefs.contains(EDGE_SUPPRESSION.key)) {
            touch.setModeValue(EDGE_SUPPRESSION.mode, getEdgeSuppression(prefs));
        }
        if (prefs.getBoolean(KEY_GAME_MODE, false)) {
            setGameMode(context, touch, true);
        }
    }

    static void setGameMode(Context context, TouchFeature touch, boolean enabled) {
        if (!enabled) {
            touch.setModeValue(MODE_GAME, 0);
            for (Tunable tunable : GAME_MODE_TUNABLES) {
                touch.resetMode(tunable.mode);
            }
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        touch.setModeValue(MODE_GAME, 1);
        for (Tunable tunable : GAME_MODE_TUNABLES) {
            final int def = touch.getModeRange(tunable.mode, tunable.fallback).def;
            touch.setModeValue(tunable.mode, prefs.getInt(tunable.key, def));
        }
    }

    private static int getEdgeSuppression(SharedPreferences prefs) {
        try {
            return Integer.parseInt(prefs.getString(EDGE_SUPPRESSION.key, null));
        } catch (NumberFormatException e) {
            return EDGE_SUPPRESSION.fallback.def;
        }
    }
}
