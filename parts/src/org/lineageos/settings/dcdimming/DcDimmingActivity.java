/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.dcdimming;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class DcDimmingActivity extends CollapsingToolbarBaseActivity {

    private static final String TAG_DC_DIMMING = "dc_dimming";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    new DcDimmingSettingsFragment(), TAG_DC_DIMMING).commit();
        }
    }
}
