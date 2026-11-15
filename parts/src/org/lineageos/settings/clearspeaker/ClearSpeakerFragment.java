/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.clearspeaker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.lineageos.settings.R;

public class ClearSpeakerFragment extends Fragment implements ClearSpeakerPlayer.Listener {

    private static final int PROGRESS_MAX = 1000;

    private ClearSpeakerPlayer mPlayer;
    private Button mButton;
    private ProgressBar mProgressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.clear_speaker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressBar = view.findViewById(R.id.clear_speaker_progress);
        mProgressBar.setMax(PROGRESS_MAX);
        mButton = view.findViewById(R.id.clear_speaker_button);
        mButton.setOnClickListener(v -> {
            if (mPlayer == null) {
                startPlayback();
            } else {
                mPlayer.stop();
            }
        });
        updateViews();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

    @Override
    public void onProgress(float progress) {
        if (mPlayer != null) {
            mProgressBar.setProgress(Math.round(progress * PROGRESS_MAX));
        }
    }

    @Override
    public void onFinished() {
        mPlayer = null;
        if (getView() != null) {
            updateViews();
        }
    }

    private void startPlayback() {
        mPlayer = new ClearSpeakerPlayer(requireContext(), this);
        mPlayer.start();
        updateViews();
    }

    private void updateViews() {
        final boolean playing = mPlayer != null;
        mButton.setText(playing ? R.string.clear_speaker_stop : R.string.clear_speaker_start);
        mProgressBar.setVisibility(playing ? View.VISIBLE : View.INVISIBLE);
        if (!playing) {
            mProgressBar.setProgress(0);
        }
        if (playing) {
            requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            requireActivity().getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
