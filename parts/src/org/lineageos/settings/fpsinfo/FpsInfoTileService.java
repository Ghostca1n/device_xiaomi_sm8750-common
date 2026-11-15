/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.fpsinfo;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class FpsInfoTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile(FpsInfoUtils.isOverlayShown());
    }

    @Override
    public void onClick() {
        super.onClick();
        final boolean shown = !FpsInfoUtils.isOverlayShown();
        FpsInfoUtils.setOverlayShown(this, shown);
        updateTile(shown);
    }

    private void updateTile(boolean shown) {
        final Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(shown ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
