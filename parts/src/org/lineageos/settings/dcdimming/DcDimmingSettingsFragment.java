/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.dcdimming;

import android.os.Bundle;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class DcDimmingSettingsFragment extends SettingsBasePreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.dc_dimming_settings, rootKey);
        findPreference(DcDimmingUtils.KEY_DC_DIMMING).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    DcDimmingUtils.setEnabled((Boolean) newValue);
                    return true;
                });
    }
}
