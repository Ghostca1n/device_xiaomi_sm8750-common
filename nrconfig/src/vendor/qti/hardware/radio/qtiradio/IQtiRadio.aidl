/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package vendor.qti.hardware.radio.qtiradio;

/*
 * The part of the IQtiRadioStable HAL that is used here, with its transaction
 * code in the HAL. The response goes to the callbacks set with setCallbacks(),
 * the HAL drops it when there are none.
 */
interface IQtiRadio {
    // config is one of NrConfig: 0 is SA and NSA, 1 is NSA only, 2 is SA only
    oneway void setNrConfig(int serial, int config) = 5;
}
