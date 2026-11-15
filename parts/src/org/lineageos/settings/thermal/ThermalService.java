/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import org.lineageos.settings.utils.ForegroundAppTracker;

public class ThermalService extends Service implements ForegroundAppTracker.Listener {

    private static final String TAG = "ThermalService";
    private static final boolean DEBUG = false;

    private ThermalUtils mThermalUtils;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mThermalUtils.setDefaultThermalProfile();
                return;
            }
            final String foregroundApp = ForegroundAppTracker.getInstance().getForegroundApp();
            if (foregroundApp != null) {
                mThermalUtils.setThermalProfile(foregroundApp);
            } else {
                mThermalUtils.setDefaultThermalProfile();
            }
        }
    };

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");
        super.onCreate();
        mThermalUtils = new ThermalUtils(this);
        registerReceiver();
        ForegroundAppTracker.getInstance().addListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        ForegroundAppTracker.getInstance().removeListener(this);
        unregisterReceiver(mIntentReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onForegroundAppChanged(String packageName) {
        mThermalUtils.setThermalProfile(packageName);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mIntentReceiver, filter);
    }
}
