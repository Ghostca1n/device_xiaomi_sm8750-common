/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.utils;

import android.app.ActivityTaskManager;
import android.app.ActivityTaskManager.RootTaskInfo;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Log;

import java.util.ArrayList;

/**
 * Tracks the package of the focused root task and notifies listeners on the
 * main thread whenever it changes. The task stack listener is only registered
 * while there is at least one listener.
 */
public final class ForegroundAppTracker {

    private static final String TAG = "ForegroundAppTracker";

    public interface Listener {
        void onForegroundAppChanged(String packageName);
    }

    private static ForegroundAppTracker sInstance;

    private final IActivityTaskManager mActivityTaskManager = ActivityTaskManager.getService();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ArraySet<Listener> mListeners = new ArraySet<>();

    private String mForegroundApp;

    private final TaskStackListener mTaskStackListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            mHandler.post(ForegroundAppTracker.this::update);
        }
    };

    private ForegroundAppTracker() {
    }

    public static synchronized ForegroundAppTracker getInstance() {
        if (sInstance == null) {
            sInstance = new ForegroundAppTracker();
        }
        return sInstance;
    }

    /** Must be called on the main thread. */
    public void addListener(Listener listener) {
        if (!mListeners.add(listener)) {
            return;
        }
        if (mListeners.size() == 1) {
            try {
                mActivityTaskManager.registerTaskStackListener(mTaskStackListener);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to register task stack listener", e);
            }
            mForegroundApp = queryForegroundApp();
        }
        if (mForegroundApp != null) {
            listener.onForegroundAppChanged(mForegroundApp);
        }
    }

    /** Must be called on the main thread. */
    public void removeListener(Listener listener) {
        if (!mListeners.remove(listener) || !mListeners.isEmpty()) {
            return;
        }
        try {
            mActivityTaskManager.unregisterTaskStackListener(mTaskStackListener);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to unregister task stack listener", e);
        }
        mForegroundApp = null;
    }

    /** Returns the last known foreground package, or null if not tracking. */
    public String getForegroundApp() {
        return mForegroundApp;
    }

    private void update() {
        if (mListeners.isEmpty()) {
            return;
        }
        final String foregroundApp = queryForegroundApp();
        if (foregroundApp == null || foregroundApp.equals(mForegroundApp)) {
            return;
        }
        mForegroundApp = foregroundApp;
        for (Listener listener : new ArrayList<>(mListeners)) {
            listener.onForegroundAppChanged(foregroundApp);
        }
    }

    private String queryForegroundApp() {
        try {
            final RootTaskInfo info = mActivityTaskManager.getFocusedRootTaskInfo();
            if (info != null && info.topActivity != null) {
                return info.topActivity.getPackageName();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to query the focused root task", e);
        }
        return null;
    }
}
