package com.blairdash.blueclient.mods;

import java.util.List;
import java.util.function.Consumer;

/** The groups of mods that get registered at startup. Add a new group class here. */
final class ModList {
    private ModList() { }

    static List<Consumer<List<BlueMod>>> groups() {
        return List.of(
                HudMods::register,
                VisualMods::register,
                BlueMods::register,
                UtilityMods::register,
                PerfMods::register,
                LegacyOptions::register
        );
    }
}
