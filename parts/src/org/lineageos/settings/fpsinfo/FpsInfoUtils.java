/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.fpsinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;

import org.lineageos.settings.utils.ComponentUtils;
import org.lineageos.settings.utils.FileUtils;

public final class FpsInfoUtils {

    static final String FPS_NODE = "/sys/class/drm/card0-sde-crtc-0/measured_fps";
    static final String CPU_STAT_NODE = "/proc/stat";
    static final String GPU_BUSY_NODE = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage";
    static final String GPU_CLOCK_NODE = "/sys/class/kgsl/kgsl-3d0/gpuclk";

    static final String KEY_SHOW_CPU_USAGE = "fps_info_show_cpu_usage";
    static final String KEY_SHOW_GPU_BUSY = "fps_info_show_gpu_busy";
    static final String KEY_SHOW_GPU_CLOCK = "fps_info_show_gpu_clock";
    static final String KEY_SHOW_BATTERY_TEMP = "fps_info_show_battery_temp";
    static final String KEY_POSITION_X = "fps_info_position_x";
    static final String KEY_POSITION_Y = "fps_info_position_y";

    private FpsInfoUtils() {
        // This class is not supposed to be instantiated
    }

    public static void onBootCompleted(Context context) {
        final boolean supported = FileUtils.isFileReadable(FPS_NODE);
        ComponentUtils.setComponentEnabled(context, FpsInfoActivity.class, supported);
        ComponentUtils.setComponentEnabled(context, FpsInfoTileService.class, supported);
    }

    static boolean isOverlayShown() {
        return FpsInfoService.isRunning();
    }

    static void setOverlayShown(Context context, boolean shown) {
        final Intent intent = new Intent(context, FpsInfoService.class);
        if (shown) {
            context.startForegroundService(intent);
        } else {
            context.stopService(intent);
        }
    }

    static void requestTileUpdate(Context context) {
        TileService.requestListeningState(context,
                new ComponentName(context, FpsInfoTileService.class));
    }
}
