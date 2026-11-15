/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esim;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.util.Log;

import vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfig;

import java.util.Arrays;
import java.util.List;

/*
 * The eSIM shares the modem's second SIM interface with the second physical
 * SIM slot. The modem switches the interface between them when it's given a
 * new SIM type for that slot through the Qualcomm radio config HAL, like stock
 * does, and then reports the slot as an eUICC.
 */
final class SimTypeController {
    private static final String TAG = "EsimSwitcher";

    private static final String QTI_RADIO_CONFIG =
            "vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfig/default";

    // vendor.qti.hardware.radio.qtiradioconfig.SimType
    private static final int SIM_TYPE_PHYSICAL = 1;
    private static final int SIM_TYPE_ESIM = 4;

    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;
    private final int mEuiccSlot;

    SimTypeController(Context context) {
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        int[] euiccSlots = context.getResources().getIntArray(
                com.android.internal.R.array.non_removable_euicc_slots);
        mEuiccSlot = euiccSlots.length > 0 ? euiccSlots[0] : -1;
    }

    boolean isSupported() {
        return mEuiccSlot >= 0;
    }

    boolean isEsimEnabled() {
        UiccSlotInfo[] slots = mTelephonyManager.getUiccSlotsInfo();
        return slots != null && mEuiccSlot >= 0 && mEuiccSlot < slots.length
                && slots[mEuiccSlot] != null && slots[mEuiccSlot].getIsEuicc();
    }

    boolean isEsimInUse() {
        List<SubscriptionInfo> subscriptions = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptions == null) return false;
        for (SubscriptionInfo subscription : subscriptions) {
            if (subscription.isEmbedded()) return true;
        }
        return false;
    }

    boolean setEsimEnabled(boolean enabled) {
        UiccSlotInfo[] slots = mTelephonyManager.getUiccSlotsInfo();
        if (slots == null || mEuiccSlot < 0 || mEuiccSlot >= slots.length) return false;

        int[] simTypes = new int[slots.length];
        Arrays.fill(simTypes, SIM_TYPE_PHYSICAL);
        simTypes[mEuiccSlot] = enabled ? SIM_TYPE_ESIM : SIM_TYPE_PHYSICAL;

        IBinder binder = ServiceManager.waitForDeclaredService(QTI_RADIO_CONFIG);
        if (binder == null) {
            Log.e(TAG, "No QtiRadioConfig HAL");
            return false;
        }

        Log.i(TAG, "Setting the SIM types to " + Arrays.toString(simTypes));
        try {
            IQtiRadioConfig.Stub.asInterface(binder).setSimType(0, simTypes);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set the SIM types", e);
            return false;
        }
    }
}
