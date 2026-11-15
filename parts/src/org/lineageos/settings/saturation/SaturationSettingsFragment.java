/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.saturation;

import android.os.Bundle;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class SaturationSettingsFragment extends SettingsBasePreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.saturation_settings, rootKey);
        findPreference(SaturationUtils.KEY_SATURATION).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    SaturationUtils.setSaturation((Integer) newValue);
                    return true;
                });
    }
}
