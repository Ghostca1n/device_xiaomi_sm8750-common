/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArraySet;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RefreshRateSettingsFragment extends SettingsBasePreferenceFragment {

    private static final class AppEntry {
        final String packageName;
        final CharSequence label;
        final Drawable icon;

        AppEntry(String packageName, CharSequence label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final Context context = requireContext();
        getPreferenceManager().setSharedPreferencesName(RefreshRateUtils.PREFS_NAME);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(context));

        final int[] refreshRates = RefreshRateUtils.getSupportedRefreshRates(context);
        mEntries = new CharSequence[refreshRates.length + 1];
        mEntryValues = new CharSequence[refreshRates.length + 1];
        mEntries[0] = getString(R.string.refresh_rate_default);
        mEntryValues[0] = RefreshRateUtils.REFRESH_RATE_DEFAULT;
        for (int i = 0; i < refreshRates.length; i++) {
            mEntries[i + 1] = getString(R.string.refresh_rate_hz, refreshRates[i]);
            mEntryValues[i + 1] = String.valueOf(refreshRates[i]);
        }

        final PackageManager packageManager = context.getPackageManager();
        ThreadUtils.postOnBackgroundThread(() -> {
            final List<AppEntry> apps = loadLaunchableApps(packageManager);
            ThreadUtils.postOnMainThread(() -> {
                if (isAdded()) {
                    populate(apps);
                }
            });
        });
    }

    private void populate(List<AppEntry> apps) {
        final PreferenceScreen screen = getPreferenceScreen();
        for (AppEntry app : apps) {
            final ListPreference preference = new ListPreference(screen.getContext());
            preference.setKey(app.packageName);
            preference.setTitle(app.label);
            preference.setDialogTitle(app.label);
            preference.setIcon(app.icon);
            preference.setEntries(mEntries);
            preference.setEntryValues(mEntryValues);
            preference.setDefaultValue(RefreshRateUtils.REFRESH_RATE_DEFAULT);
            preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            screen.addPreference(preference);
        }
    }

    private static List<AppEntry> loadLaunchableApps(PackageManager packageManager) {
        final Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        final ArraySet<String> seen = new ArraySet<>();
        final List<AppEntry> apps = new ArrayList<>();
        for (ResolveInfo info : packageManager.queryIntentActivities(intent, 0)) {
            final ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            if (seen.add(appInfo.packageName)) {
                apps.add(new AppEntry(appInfo.packageName, appInfo.loadLabel(packageManager),
                        appInfo.loadIcon(packageManager)));
            }
        }
        final Collator collator = Collator.getInstance();
        apps.sort(Comparator.comparing((AppEntry app) -> app.label.toString(), collator));
        return apps;
    }
}
