package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.net.RankService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Puts the Blue Client rank icon in front of a name label. Besides the player's own nametag this covers
 * the floating tags servers (TAB, holograms) attach to a player: armor stands and Text Displays that ride
 * the player. Those are only touched when their text contains the player's name, so a money / rank line
 * under the name does not get an icon of its own.
 */
public final class NametagIcon {
    private NametagIcon() { }

    public static Component decorate(Entity entity, Component original) {
        if (entity == null || original == null) return original;
        Player owner;
        if (entity instanceof Player p) {
            owner = p;
        } else if (entity instanceof ArmorStand || entity instanceof Display) {
            Entity vehicle = entity.getVehicle();
            if (!(vehicle instanceof Player p)) return original;
            owner = p;
            String name = owner.getName().getString().toLowerCase(Locale.ROOT);
            if (name.isEmpty() || !original.getString().toLowerCase(Locale.ROOT).contains(name)) return original;
        } else {
            return original;
        }
        Integer color = RankService.colorFor(owner.getUUID());
        if (color == null) return original; // not a currently-online Blue Client user
        // icon must be a sibling, not the parent: children inherit the parent's font, which would turn the name into missing-glyph boxes
        return Component.empty().append(RankIcon.text(color)).append(Component.literal(" ")).append(original);
    }
}