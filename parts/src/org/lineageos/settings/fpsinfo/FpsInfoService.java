/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.fpsinfo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.R;

public class FpsInfoService extends Service {

    private static final String CHANNEL_ID = "fps_info";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_STOP = "org.lineageos.settings.fpsinfo.action.STOP";
    private static final long UPDATE_INTERVAL_MS = 1000;

    private static volatile boolean sRunning;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final StatsReader mStatsReader = new StatsReader();

    private HandlerThread mStatsThread;
    private Handler mStatsHandler;
    private SharedPreferences mPrefs;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private TextView mOverlay;

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            final String text = buildText();
            mMainHandler.post(() -> {
                if (mOverlay != null) {
                    mOverlay.setText(text);
                }
            });
            mStatsHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mStatsHandler.removeCallbacks(mUpdateRunnable);
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mStatsHandler.post(mUpdateRunnable);
            }
        }
    };

    static boolean isRunning() {
        return sRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sRunning = true;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mWindowManager = getSystemService(WindowManager.class);

        startForeground(NOTIFICATION_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        addOverlay();

        mStatsThread = new HandlerThread("FpsInfo");
        mStatsThread.start();
        mStatsHandler = new Handler(mStatsThread.getLooper());
        mStatsHandler.post(mUpdateRunnable);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, filter);

        FpsInfoUtils.requestTileUpdate(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        sRunning = false;
        unregisterReceiver(mScreenReceiver);
        mStatsHandler.removeCallbacks(mUpdateRunnable);
        mStatsThread.quitSafely();
        mWindowManager.removeView(mOverlay);
        mOverlay = null;
        FpsInfoUtils.requestTileUpdate(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        final NotificationManager notificationManager =
                getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                getString(R.string.fps_info_title), NotificationManager.IMPORTANCE_LOW));

        final PendingIntent settingsIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, FpsInfoActivity.class), PendingIntent.FLAG_IMMUTABLE);
        final PendingIntent stopIntent = PendingIntent.getService(this, 0,
                new Intent(this, FpsInfoService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fps_info)
                .setContentTitle(getString(R.string.fps_info_notification_title))
                .setContentIntent(settingsIntent)
                .addAction(new Notification.Action.Builder(null,
                        getString(R.string.fps_info_notification_stop), stopIntent).build())
                .setOngoing(true)
                .build();
    }

    private void addOverlay() {
        mOverlay = (TextView) LayoutInflater.from(this).inflate(R.layout.fps_info_overlay, null);
        mOverlay.setOnTouchListener(new DragListener());

        mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mLayoutParams.setFitInsetsTypes(0);
        mLayoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        mLayoutParams.setTitle(getString(R.string.fps_info_title));
        final int defaultOffset =
                getResources().getDimensionPixelSize(R.dimen.fps_info_default_offset);
        mLayoutParams.x = mPrefs.getInt(FpsInfoUtils.KEY_POSITION_X, defaultOffset);
        mLayoutParams.y = mPrefs.getInt(FpsInfoUtils.KEY_POSITION_Y, defaultOffset);

        mWindowManager.addView(mOverlay, mLayoutParams);
    }

    private String buildText() {
        final StringBuilder text = new StringBuilder();
        final float fps = mStatsReader.readFps();
        text.append(fps >= 0 ? getString(R.string.fps_info_fps, fps)
                : getString(R.string.fps_info_fps_unknown));

        if (mPrefs.getBoolean(FpsInfoUtils.KEY_SHOW_CPU_USAGE, false)) {
            final int usage = mStatsReader.readCpuUsage();
            if (usage >= 0) {
                text.append('\n').append(getString(R.string.fps_info_cpu_usage, usage));
            }
        }
        if (mPrefs.getBoolean(FpsInfoUtils.KEY_SHOW_GPU_BUSY, false)) {
            final int busy = mStatsReader.readGpuBusy();
            if (busy >= 0) {
                text.append('\n').append(getString(R.string.fps_info_gpu_busy, busy));
            }
        }
        if (mPrefs.getBoolean(FpsInfoUtils.KEY_SHOW_GPU_CLOCK, false)) {
            final int clock = mStatsReader.readGpuClockMhz();
            if (clock >= 0) {
                text.append('\n').append(getString(R.string.fps_info_gpu_clock, clock));
            }
        }
        if (mPrefs.getBoolean(FpsInfoUtils.KEY_SHOW_BATTERY_TEMP, false)) {
            final int temp = mStatsReader.readBatteryTemp(this);
            if (temp != Integer.MIN_VALUE) {
                text.append('\n').append(getString(R.string.fps_info_battery_temp, temp / 10f));
            }
        }
        return text.toString();
    }

    private class DragListener implements View.OnTouchListener {
        private int mStartX;
        private int mStartY;
        private float mTouchStartX;
        private float mTouchStartY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mStartX = mLayoutParams.x;
                    mStartY = mLayoutParams.y;
                    mTouchStartX = event.getRawX();
                    mTouchStartY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    mLayoutParams.x = mStartX + Math.round(event.getRawX() - mTouchStartX);
                    mLayoutParams.y = mStartY + Math.round(event.getRawY() - mTouchStartY);
                    mWindowManager.updateViewLayout(view, mLayoutParams);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mPrefs.edit()
                            .putInt(FpsInfoUtils.KEY_POSITION_X, mLayoutParams.x)
                            .putInt(FpsInfoUtils.KEY_POSITION_Y, mLayoutParams.y)
                            .apply();
                    return true;
                default:
                    return false;
            }
        }
    }
}
