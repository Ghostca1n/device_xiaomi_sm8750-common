/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.fpsinfo;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.lineageos.settings.utils.FileUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the values shown by the overlay. The readers return -1 on failure,
 * except the battery temperature, which returns Integer.MIN_VALUE.
 */
final class StatsReader {

    // e.g. "fps: 120.0 duration:1000000 frame_count:120"
    private static final Pattern FPS_PATTERN = Pattern.compile("fps:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^\\s*(\\d+)");

    private long mLastCpuTotal;
    private long mLastCpuIdle;

    float readFps() {
        final String line = FileUtils.readOneLine(FpsInfoUtils.FPS_NODE);
        if (line == null) {
            return -1;
        }
        final Matcher matcher = FPS_PATTERN.matcher(line);
        return matcher.find() ? Float.parseFloat(matcher.group(1)) : -1;
    }

    /** CPU usage in percent since the previous call. */
    int readCpuUsage() {
        // cpu user nice system idle iowait irq softirq steal ...
        final String line = FileUtils.readOneLine(FpsInfoUtils.CPU_STAT_NODE);
        if (line == null || !line.startsWith("cpu ")) {
            return -1;
        }
        final String[] fields = line.trim().split("\\s+");
        if (fields.length < 9) {
            return -1;
        }

        long total = 0;
        try {
            for (int i = 1; i <= 8; i++) {
                total += Long.parseLong(fields[i]);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
        final long idle = Long.parseLong(fields[4]) + Long.parseLong(fields[5]);

        final long totalDelta = total - mLastCpuTotal;
        final long idleDelta = idle - mLastCpuIdle;
        final boolean firstSample = mLastCpuTotal == 0;
        mLastCpuTotal = total;
        mLastCpuIdle = idle;
        if (firstSample || totalDelta <= 0) {
            return -1;
        }
        return (int) (100 * (totalDelta - idleDelta) / totalDelta);
    }

    int readGpuBusy() {
        // e.g. "12 %"
        return readLeadingNumber(FpsInfoUtils.GPU_BUSY_NODE);
    }

    int readGpuClockMhz() {
        final int clockHz = readLeadingNumber(FpsInfoUtils.GPU_CLOCK_NODE);
        return clockHz < 0 ? -1 : clockHz / 1000000;
    }

    /** Battery temperature in tenths of a degree Celsius. */
    int readBatteryTemp(Context context) {
        final Intent intent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return intent != null
                ? intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE)
                : Integer.MIN_VALUE;
    }

    private static int readLeadingNumber(String fileName) {
        final String line = FileUtils.readOneLine(fileName);
        if (line == null) {
            return -1;
        }
        final Matcher matcher = LEADING_NUMBER_PATTERN.matcher(line);
        try {
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
