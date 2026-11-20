package com.blairdash.blueclient.api;

import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extension API: other Fabric mods call {@link #registerTab} (from their client entrypoint) to add a tab of
 * mods to the Blue Client menu. Tabs show up as extra pills next to HUD / MECHANIC / VISUAL.
 */
public final class BlueClientApi {
    private BlueClientApi() { }

    /** One mod card supplied by an extension. */
    public interface ExtensionMod {
        String name();
        Item icon();
        boolean isEnabled();
        void setEnabled(boolean enabled);
    }

    private static final Map<String, List<ExtensionMod>> TABS = new LinkedHashMap<>();

    /** Adds (or extends) a tab. Keep the title short and upper-case, e.g. "EXT HUD". */
    public static synchronized void registerTab(String title, List<? extends ExtensionMod> mods) {
        TABS.computeIfAbsent(title, k -> new ArrayList<>()).addAll(mods);
    }

    public static synchronized Map<String, List<ExtensionMod>> tabs() {
        Map<String, List<ExtensionMod>> copy = new LinkedHashMap<>();
        TABS.forEach((k, v) -> copy.put(k, Collections.unmodifiableList(new ArrayList<>(v))));
        return copy;
    }
}
