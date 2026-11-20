package com.blairdash.blueclient.update;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Runs before Blue Client initialises (Fabric). */
public class UpdaterPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        BlueUpdater.run();
    }
}