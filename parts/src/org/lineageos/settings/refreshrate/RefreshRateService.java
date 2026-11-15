/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.refreshrate;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.lineageos.settings.utils.ForegroundAppTracker;

public class RefreshRateService extends Service implements ForegroundAppTracker.Listener {

    @Override
    public void onCreate() {
        super.onCreate();
        // Recover the user's settings if we were killed while forcing a refresh rate
        RefreshRateUtils.restoreUserRefreshRate(this);
        ForegroundAppTracker.getInstance().addListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        ForegroundAppTracker.getInstance().removeListener(this);
        RefreshRateUtils.restoreUserRefreshRate(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onForegroundAppChanged(String packageName) {
        final int refreshRate = RefreshRateUtils.getRefreshRateForPackage(this, packageName);
        if (refreshRate > 0) {
            RefreshRateUtils.applyRefreshRate(this, refreshRate);
        } else {
            RefreshRateUtils.restoreUserRefreshRate(this);
        }
    }
}
