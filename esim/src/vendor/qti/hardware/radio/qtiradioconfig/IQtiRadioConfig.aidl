/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package vendor.qti.hardware.radio.qtiradioconfig;

/*
 * The part of the IQtiRadioConfig HAL that is used here, with its transaction
 * code in the HAL. The response goes to the callbacks set with setCallbacks(),
 * the HAL drops it when there are none.
 */
interface IQtiRadioConfig {
    // One SimType per physical slot: 1 is a physical SIM, 4 the eSIM
    oneway void setSimType(int serial, in int[] simTypes) = 4;
}
