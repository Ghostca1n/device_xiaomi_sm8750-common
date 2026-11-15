/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.touch;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.SeekBarPreference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class TouchSettingsFragment extends SettingsBasePreferenceFragment {

    private TouchFeature mTouchFeature;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.touch_settings, rootKey);
        mTouchFeature = TouchFeature.getInstance(requireContext());
        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

        findPreference(TouchUtils.KEY_GAME_MODE).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    TouchUtils.setGameMode(requireContext(), mTouchFeature, (Boolean) newValue);
                    return true;
                });

        for (TouchUtils.Tunable tunable : TouchUtils.GAME_MODE_TUNABLES) {
            final TouchFeature.ModeRange range =
                    mTouchFeature.getModeRange(tunable.mode, tunable.fallback);
            final SeekBarPreference preference = findPreference(tunable.key);
            preference.setMin(range.min);
            preference.setMax(range.max);
            if (!prefs.contains(tunable.key)) {
                preference.setValue(range.def);
            }
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                if (prefs.getBoolean(TouchUtils.KEY_GAME_MODE, false)) {
                    mTouchFeature.setModeValue(tunable.mode, (Integer) newValue);
                }
                return true;
            });
        }

        setupEdgeSuppression(prefs);

        findPreference(TouchUtils.KEY_GLOVE_MODE).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    mTouchFeature.setModeValue(TouchUtils.MODE_GLOVE, (Boolean) newValue ? 1 : 0);
                    return true;
                });
    }

    private void setupEdgeSuppression(SharedPreferences prefs) {
        final TouchUtils.Tunable tunable = TouchUtils.EDGE_SUPPRESSION;
        final TouchFeature.ModeRange range =
                mTouchFeature.getModeRange(tunable.mode, tunable.fallback);
        final int count = range.max - range.min + 1;
        final CharSequence[] entries = new CharSequence[count];
        final CharSequence[] values = new CharSequence[count];
        for (int i = 0; i < count; i++) {
            final int level = range.min + i;
            entries[i] = getString(R.string.touch_edge_suppression_level, level);
            values[i] = String.valueOf(level);
        }

        final ListPreference preference = findPreference(tunable.key);
        preference.setEntries(entries);
        preference.setEntryValues(values);
        if (!prefs.contains(tunable.key) || preference.findIndexOfValue(preference.getValue()) < 0) {
            preference.setValue(String.valueOf(range.def));
        }
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            mTouchFeature.setModeValue(tunable.mode, Integer.parseInt((String) newValue));
            return true;
        });
    }
}
