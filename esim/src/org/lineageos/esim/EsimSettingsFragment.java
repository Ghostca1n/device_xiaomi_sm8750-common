/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class EsimSettingsFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    // The modem takes a few seconds to switch, give up waiting after this
    private static final long SWITCH_TIMEOUT_MS = 20000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mSwitchTimeout = this::onSwitchFinished;

    private SimTypeController mController;
    private MainSwitchPreference mSwitch;
    private boolean mSwitching;

    private final BroadcastReceiver mSlotStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSwitching && mSwitch.isChecked() == mController.isEsimEnabled()) {
                onSwitchFinished();
            } else {
                updateState();
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.esim_settings, rootKey);
        mController = new SimTypeController(requireContext());
        mSwitch = findPreference("esim_enable");
        mSwitch.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireContext().registerReceiver(mSlotStatusReceiver,
                new IntentFilter(TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED),
                Context.RECEIVER_EXPORTED);
        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(mSlotStatusReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mSwitchTimeout);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enable = (Boolean) newValue;
        if (!enable && mController.isEsimInUse()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.esim_in_use_title)
                    .setMessage(R.string.esim_in_use_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return false;
        }

        if (!mController.setEsimEnabled(enable)) {
            Toast.makeText(requireContext(), R.string.esim_switch_failed, Toast.LENGTH_SHORT).show();
            return false;
        }

        mSwitching = true;
        mSwitch.setEnabled(false);
        mHandler.postDelayed(mSwitchTimeout, SWITCH_TIMEOUT_MS);
        return true;
    }

    private void onSwitchFinished() {
        mHandler.removeCallbacks(mSwitchTimeout);
        mSwitching = false;
        updateState();
    }

    private void updateState() {
        if (mSwitching) return;
        mSwitch.setChecked(mController.isEsimEnabled());
        mSwitch.setEnabled(mController.isSupported());
    }
}
