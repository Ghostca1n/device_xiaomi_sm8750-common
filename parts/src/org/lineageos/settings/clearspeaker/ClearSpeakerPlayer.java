/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.clearspeaker;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/**
 * Plays a low frequency sweep through the loudspeaker at full media volume,
 * which makes the speaker membrane push out water trapped in the grille.
 */
final class ClearSpeakerPlayer {

    private static final String TAG = "ClearSpeakerPlayer";

    interface Listener {
        /** Called on the main thread with the played fraction, from 0 to 1. */
        void onProgress(float progress);

        /** Called on the main thread once playback ended or was stopped. */
        void onFinished();
    }

    private static final int SAMPLE_RATE = 48000;
    private static final int DURATION_SECONDS = 30;
    private static final int TOTAL_FRAMES = SAMPLE_RATE * DURATION_SECONDS;
    private static final int CHUNK_FRAMES = SAMPLE_RATE / 20;

    // Triangle sweep between these frequencies
    private static final double SWEEP_MIN_HZ = 120;
    private static final double SWEEP_MAX_HZ = 240;
    private static final double SWEEP_PERIOD_SECONDS = 4;

    private static final double AMPLITUDE = 0.9 * Short.MAX_VALUE;
    private static final int FADE_FRAMES = SAMPLE_RATE / 20;
    private static final long DRAIN_TIMEOUT_MS = 1000;

    private final AudioManager mAudioManager;
    private final Listener mListener;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean mStopRequested;

    ClearSpeakerPlayer(Context context, Listener listener) {
        mAudioManager = context.getSystemService(AudioManager.class);
        mListener = listener;
    }

    void start() {
        new Thread(this::play, TAG).start();
    }

    void stop() {
        mStopRequested = true;
    }

    private void play() {
        final int previousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        AudioTrack track = null;
        try {
            track = createAudioTrack();
            track.play();

            final short[] buffer = new short[CHUNK_FRAMES];
            double phase = 0;
            int frame = 0;
            while (frame < TOTAL_FRAMES && !mStopRequested) {
                final int frames = Math.min(CHUNK_FRAMES, TOTAL_FRAMES - frame);
                for (int i = 0; i < frames; i++, frame++) {
                    phase += 2 * Math.PI * getFrequency(frame) / SAMPLE_RATE;
                    if (phase > 2 * Math.PI) {
                        phase -= 2 * Math.PI;
                    }
                    buffer[i] = (short) (AMPLITUDE * getEnvelope(frame) * Math.sin(phase));
                }
                track.write(buffer, 0, frames);

                final float progress = (float) frame / TOTAL_FRAMES;
                mMainHandler.post(() -> mListener.onProgress(progress));
            }

            // Let the queued audio drain unless asked to stop right away
            final long deadline = SystemClock.uptimeMillis() + DRAIN_TIMEOUT_MS;
            while (!mStopRequested && track.getPlaybackHeadPosition() < TOTAL_FRAMES
                    && SystemClock.uptimeMillis() < deadline) {
                Thread.sleep(20);
            }
            track.pause();
            track.flush();
        } catch (InterruptedException | RuntimeException e) {
            Log.e(TAG, "Playback failed", e);
        } finally {
            if (track != null) {
                track.release();
            }
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
            mMainHandler.post(mListener::onFinished);
        }
    }

    private AudioTrack createAudioTrack() {
        final AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(CHUNK_FRAMES * 2 * Short.BYTES)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        // Play through the loudspeaker even if headphones are connected
        for (AudioDeviceInfo device : mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                track.setPreferredDevice(device);
                break;
            }
        }
        return track;
    }

    private static double getFrequency(int frame) {
        final double position = (frame / (double) SAMPLE_RATE) % SWEEP_PERIOD_SECONDS
                / SWEEP_PERIOD_SECONDS;
        final double triangle = position < 0.5 ? position * 2 : (1 - position) * 2;
        return SWEEP_MIN_HZ + (SWEEP_MAX_HZ - SWEEP_MIN_HZ) * triangle;
    }

    /** Short fades at both ends avoid clicks. */
    private static double getEnvelope(int frame) {
        final int framesLeft = TOTAL_FRAMES - frame;
        return Math.min(1.0, Math.min(frame, framesLeft) / (double) FADE_FRAMES);
    }
}
