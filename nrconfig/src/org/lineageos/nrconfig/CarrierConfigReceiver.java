/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.nrconfig;

import static android.telephony.CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA;
import static android.telephony.CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_CONFIG_APPLIED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.util.ArrayUtils;

import vendor.qti.hardware.radio.qtiradio.IQtiRadio;

/*
 * The framework enables 5G SA (N1 mode) where the carrier config allows it,
 * but the Qualcomm radio HAL doesn't support N1 mode requests, so the modem
 * keeps the NR mode it was last set to. Stock sets it per carrier from its
 * own settings, and only allows SA for a few carriers, which leaves SA and
 * with it VoNR off for every other one.
 *
 * Apply the NR modes of the carrier config through the Qualcomm HAL instead.
 */
public class CarrierConfigReceiver extends BroadcastReceiver {
    private static final String TAG = "QtiNrConfig";

    private static final String QTI_RADIO = "vendor.qti.hardware.radio.qtiradio.IQtiRadioStable/slot";

    // vendor.qti.hardware.radio.qtiradio.NrConfig
    private static final int NR_CONFIG_COMBINED_SA_NSA = 0;
    private static final int NR_CONFIG_NSA = 1;
    private static final int NR_CONFIG_SA = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        int slot = intent.getIntExtra(CarrierConfigManager.EXTRA_SLOT_INDEX,
                SubscriptionManager.INVALID_SIM_SLOT_INDEX);
        int subId = intent.getIntExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        if (slot < 0 || !SubscriptionManager.isValidSubscriptionId(subId)) return;

        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        PersistableBundle config = configManager.getConfigForSubId(subId,
                KEY_CARRIER_CONFIG_APPLIED_BOOL, KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        if (!CarrierConfigManager.isConfigForIdentifiedCarrier(config)) return;

        int[] modes = config.getIntArray(KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        boolean sa = ArrayUtils.contains(modes, CARRIER_NR_AVAILABILITY_SA);
        boolean nsa = ArrayUtils.contains(modes, CARRIER_NR_AVAILABILITY_NSA);
        int nrConfig = !sa ? NR_CONFIG_NSA : nsa ? NR_CONFIG_COMBINED_SA_NSA : NR_CONFIG_SA;

        IBinder binder = ServiceManager.waitForDeclaredService(QTI_RADIO + (slot + 1));
        if (binder == null) {
            Log.e(TAG, "No QtiRadio HAL for slot " + slot);
            return;
        }

        Log.i(TAG, "Setting the NR config of slot " + slot + " to " + nrConfig);
        try {
            IQtiRadio.Stub.asInterface(binder).setNrConfig(0, nrConfig);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set the NR config of slot " + slot, e);
        }
    }
}
