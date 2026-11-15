/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.fpsinfo;

import android.os.Bundle;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class FpsInfoSettingsFragment extends SettingsBasePreferenceFragment {

    private static final String KEY_SHOW_OVERLAY = "fps_info_show_overlay";

    private MainSwitchPreference mShowOverlayPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fps_info_settings, rootKey);

        mShowOverlayPreference = findPreference(KEY_SHOW_OVERLAY);
        mShowOverlayPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            FpsInfoUtils.setOverlayShown(requireContext(), (Boolean) newValue);
            return true;
        });

        findPreference(FpsInfoUtils.KEY_SHOW_GPU_BUSY).setVisible(
                FileUtils.isFileReadable(FpsInfoUtils.GPU_BUSY_NODE));
        findPreference(FpsInfoUtils.KEY_SHOW_GPU_CLOCK).setVisible(
                FileUtils.isFileReadable(FpsInfoUtils.GPU_CLOCK_NODE));
    }

    @Override
    public void onResume() {
        super.onResume();
        // The overlay may have been toggled from the tile or the notification
        mShowOverlayPreference.setChecked(FpsInfoUtils.isOverlayShown());
    }
}
