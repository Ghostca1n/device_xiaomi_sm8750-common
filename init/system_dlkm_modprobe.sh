#! /vendor/bin/sh
#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#
# Load the GKI modules listed in system_dlkm's modules.load. They are
# installed directly in /system_dlkm/lib/modules, without the kernel
# release subdirectory the stock script looks for.

exec /vendor/bin/modprobe -s --all=/system_dlkm/lib/modules/modules.load > /dev/null
