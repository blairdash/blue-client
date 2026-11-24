package com.blairdash.blueclient.mods;

import com.blairdash.blueclient.mixin.mods.OverlayTextureAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.CameraType;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Visual / world / quality-of-life mods. Mixin-driven ones read their settings from here. */
public final class VisualMods {
    private VisualMods() { }

    static void register(List<BlueMod> out) {
        out.add(new Cosmetics());
        out.add(new FovChanger());
        out.add(new ParticleChanger());
        out.add(new MobSize());
        out.add(new HitColor());
        out.add(new ItemCustomizer());
        out.add(new Snaplook());
        out.add(new TeamView());
        out.add(new NickHider());
        out.add(new DamageTint());
        out.add(new InventoryHud());
        out.add(new AttackIndicator());
        out.add(new Momentum());
        out.add(new PackDisplay());
        out.add(new MenuBlur());
        out.add(new AudioSubtitles());
        out.add(new KillSounds());
        out.add(new ShulkerPreview());
    }

    /** Looks up a mod and returns it only when enabled (for mixins). */
    public static BlueMod active(String id) {
        BlueMod m = ModManager.get(id);
        return m != null && m.enabled ? m : null;
    }

    // ---- Cosmetics ----------------------------------------------------------
    static final class Cosmetics extends BlueMod {
        Cosmetics() { super("cosmetics", "Cosmetics", "COSMETICS", Items.ELYTRA); choice("cape", "Cloak", 1, "None", "Light", "Dark", "Blue"); }
    }

    // ---- FOV ---------------------------------------------------------------
    static final class FovChanger extends BlueMod {
        FovChanger() { super("fov_changer", "FOV Changer", "VISUAL", Items.SPYGLASS); num("mult", "FOV multiplier", 0.5, 1.5, 2, 1.0); }
    }

    // ---- Particles ---------------------------------------------------------
    static final class ParticleChanger extends BlueMod {
        ParticleChanger() { super("particle_changer", "Particle Changer", "VISUAL", Items.FIREWORK_STAR);
            num("amount", "Particle amount %", 0, 100, 0, 100); }
    }

    // ---- Mob size ----------------------------------------------------------
    static final class MobSize extends BlueMod {
        MobSize() { super("mob_size", "Mob Size", "VISUAL", Items.ZOMBIE_HEAD); num("scale", "Mob scale", 0.25, 2.0, 2, 1.0); }
    }

    // ---- Hit color ---------------------------------------------------------
    static final class HitColor extends BlueMod {
        private final Setting hit = color("hit", "Hit color", 0xFFFF5555);
        private final Setting alpha = num("alpha", "Strength", 0.1, 1.0, 2, 0.5);
        private long applied = Long.MIN_VALUE;
        HitColor() { super("hit_color", "Hit Color", "VISUAL", Items.DYE.pick(net.minecraft.world.item.DyeColor.RED)); }
        @Override public void tick(Minecraft c) {
            long sig = (enabled ? 1L : 0L) | ((long) hit.color << 1) | ((long) Math.round(alpha.n * 100) << 40);
            if (sig == applied) return;
            applied = sig;
            try {
                DynamicTexture tex = ((OverlayTextureAccessor) c.gameRenderer.overlayTexture()).blue$texture();
                NativeImage img = tex.getPixels();
                if (img == null) return;
                int a = enabled ? (int) (alpha.n * 255) : 0xB2;
                int argb = enabled ? ((a << 24) | (hit.color & 0xFFFFFF)) : 0xB2FF0000;
                if (!enabled) argb = -1291911168;
                for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setPixel(x, y, argb);
                tex.upload();
            } catch (Throwable t) { applied = sig; System.err.println("[clientmod] Hit Color: " + t); }
        }
    }

    // ---- Item customizer ---------------------------------------------------
    static final class ItemCustomizer extends BlueMod {
        ItemCustomizer() { super("item_customizer", "Item Customizer", "VISUAL", Items.DIAMOND_SWORD);
            num("scale", "Size", 0.3, 1.8, 2, 1.0); num("x", "Left / right", -1, 1, 2, 0);
            num("y", "Down / up", -1, 1, 2, 0); num("z", "Away / near", -1, 1, 2, 0); }
    }

    // ---- Snaplook ----------------------------------------------------------
    static final class Snaplook extends BlueMod {
        private final net.minecraft.client.KeyMapping key = HudMods.key("snaplook", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
        private CameraType saved;
        Snaplook() { super("snaplook", "Snaplook", "VISUAL", Items.ENDER_EYE); choice("view", "View", 0, "Third person back", "Third person front"); }
        @Override public void tick(Minecraft c) {
            if (!enabled) { release(c); return; }
            if (key.isDown()) {
                if (saved == null) saved = c.options.getCameraType();
                c.options.setCameraType(setting("view").idx == 0 ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT);
            } else release(c);
        }
        private void release(Minecraft c) { if (saved != null) { c.options.setCameraType(saved); saved = null; } }
    }

    // ---- Team view ---------------------------------------------------------
    public interface TeamViewAccess { boolean glows(Minecraft c, Entity e); }
    public interface NickHiderAccess { Component apply(Component in); Component apply(Component in, int where); }

    static final class TeamView extends BlueMod implements TeamViewAccess {
        TeamView() { super("team_view", "Team View", "VISUAL", Items.LEATHER_CHESTPLATE); bool("players", "Outline teammates only", true); }
        @Override public boolean glows(Minecraft c, Entity e) {
            return c.player != null && e != c.player && e instanceof net.minecraft.world.entity.player.Player && c.player.isAlliedTo(e);
        }
    }

    // ---- Nick hider --------------------------------------------------------
    static final class NickHider extends BlueMod implements NickHiderAccess {
        private final Setting name = choice("name", "Replacement name", 0, "You", "Player", "Steve", "Alex", "Anonymous", "???");
        private final Setting chat = bool("chat", "Hide in chat", true);
        private final Setting tab = bool("tab", "Hide in tab list", true);
        private final Setting world = bool("world", "Hide in scoreboard & name tags", true);
        NickHider() { super("nick_hider", "Nick Hider", "VISUAL", Items.NAME_TAG); }
        @Override public Component apply(Component in) { return apply(in, 0); }
        @Override public Component apply(Component in, int where) {
            if (where == 0 ? !chat.b : where == 1 ? !tab.b : !world.b) return in;
            Minecraft c = Minecraft.getInstance();
            String me = c.getUser().getName();
            String fake = name.options[Math.max(0, Math.min(name.idx, name.options.length - 1))];
            if (me == null || me.isEmpty() || !in.getString().contains(me)) return in;
            net.minecraft.network.chat.MutableComponent res = Component.empty();
            in.visit((style, str) -> {
                res.append(Component.literal(str.replace(me, fake)).setStyle(style));
                return java.util.Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
            return res;
        }
    }

    // ---- Damage tint -------------------------------------------------------
    static final class DamageTint extends BlueMod {
        private final Setting col = color("color", "Tint color", 0xFFFF5555);
        private final Setting strength = num("strength", "Strength", 0.1, 1.0, 2, 0.4);
        DamageTint() { super("damage_tint", "Damage Tint", "VISUAL", Items.REDSTONE); }
        @Override public boolean drawsOverlay() { return true; }
        public void drawOverlay(GuiGraphicsExtractor ctx, Minecraft c) {
            if (c.player == null || c.player.hurtTime <= 0) return;
            float f = c.player.hurtTime / (float) Math.max(1, c.player.hurtDuration);
            int a = (int) (f * strength.n * 255) & 0xFF;
            ctx.fill(0, 0, ctx.guiWidth(), ctx.guiHeight(), (a << 24) | (col.color & 0xFFFFFF));
        }
        @Override public void render(GuiGraphicsExtractor ctx, Minecraft c) { drawOverlay(ctx, c); }
    }

    // ---- Inventory HUD -----------------------------------------------------
    static final class InventoryHud extends BlueMod {
        InventoryHud() { super("inventory_hud", "Inventory HUD", "HUD", Items.CHEST_MINECART); bool("background", "Background", true); }
        @Override public boolean isHud() { return true; }
        @Override public int defaultX(Minecraft c, int sw, int sh) { return sw - 9 * 18 - 8; }
        @Override public int defaultY(Minecraft c, int sw, int sh) { return sh - 3 * 18 - 40; }
        @Override public int hudW(Minecraft c) { return 9 * 18; }
        @Override public int hudH(Minecraft c) { return 3 * 18; }
        @Override public void render(GuiGraphicsExtractor ctx, Minecraft c) {
            int sw = ctx.guiWidth(), sh = ctx.guiHeight();
            int x = x(c, sw, sh), y = y(c, sw, sh);
            if (setting("background").b) ctx.fill(x - 2, y - 2, x + 9 * 18 + 1, y + 3 * 18 + 1, 0x66000000);
            var inv = c.player.getInventory();
            for (int i = 0; i < 27; i++) {
                ItemStack s = inv.getItem(9 + i);
                if (s.isEmpty()) continue;
                int ix = x + (i % 9) * 18, iy = y + (i / 9) * 18;
                ctx.item(s, ix, iy);
                ctx.itemDecorations(c.font, s, ix, iy);
            }
        }
    }

    // ---- Attack indicator --------------------------------------------------
    static final class AttackIndicator extends BlueMod {
        AttackIndicator() { super("attack_indicator", "Attack Indicator", "HUD", Items.IRON_SWORD); }
        @Override public boolean isHud() { return true; }
        @Override public int defaultX(Minecraft c, int sw, int sh) { return sw / 2 - 8; }
        @Override public int defaultY(Minecraft c, int sw, int sh) { return sh / 2 + 9; }
        @Override public int hudW(Minecraft c) { return 16; }
        @Override public int hudH(Minecraft c) { return 4; }
        /** Draws vanilla's own attack indicator (same pixels as the game's), movable like any HUD element. */
        @Override public void render(GuiGraphicsExtractor ctx, Minecraft c) {
            int sw = ctx.guiWidth(), sh = ctx.guiHeight();
            com.blairdash.blueclient.features.CrosshairOverlay.drawVanillaIndicator(ctx, c, x(c, sw, sh), y(c, sw, sh));
        }
    }

    // ---- Momentum (speed) --------------------------------------------------
    static final class Momentum extends TextHudMod {
        private final Setting unit = choice("unit", "Unit", 0, "Blocks / second", "km/h");
        private double px, pz; private boolean has; private double speed;
        Momentum() { super("momentum", "Momentum", "HUD", Items.FEATHER, "Speed: 0.00 b/s"); }
        @Override public void tick(Minecraft c) {
            if (c.player == null) { has = false; return; }
            if (has) speed = Math.hypot(c.player.getX() - px, c.player.getZ() - pz) * 20.0;
            px = c.player.getX(); pz = c.player.getZ(); has = true;
        }
        @Override protected List<String> lines(Minecraft c) {
            return List.of(unit.idx == 0 ? String.format(Locale.ROOT, "Speed: %.2f b/s", speed) : String.format(Locale.ROOT, "Speed: %.1f km/h", speed * 3.6));
        }
    }

    // ---- Pack display ------------------------------------------------------
    static final class PackDisplay extends TextHudMod {
        PackDisplay() { super("pack_display", "Pack Display", "HUD", Items.PAINTING, "Packs: Faithful 32x"); }
        @Override protected List<String> lines(Minecraft c) {
            List<String> out = new ArrayList<>();
            for (var p : c.getResourcePackRepository().getSelectedPacks()) {
                String n = p.getTitle().getString();
                if (n.equals("Default") || n.startsWith("Fabric") || n.equals("Vanilla")) continue;
                out.add(n);
            }
            if (out.isEmpty()) out.add("No packs");
            return out;
        }
    }

    // ---- Menu blur ---------------------------------------------------------
    static final class MenuBlur extends BlueMod {
        private final Setting amount = num("amount", "Blur radius", 0, 10, 0, 0);
        MenuBlur() { super("menu_blur", "Menu Blur", "VISUAL", Items.GLASS); }
        @Override public void tick(Minecraft c) {
            if (!enabled) return;
            int v = (int) amount.n;
            if (c.options.menuBackgroundBlurriness().get() != v) c.options.menuBackgroundBlurriness().set(v);
        }
    }

    // ---- Audio subtitles ---------------------------------------------------
    static final class AudioSubtitles extends BlueMod {
        AudioSubtitles() { super("audio_subtitles", "Audio Subtitles", "UTILITY", Items.NOTE_BLOCK); }
        @Override public void tick(Minecraft c) {
            if (enabled && !c.options.showSubtitles().get()) c.options.showSubtitles().set(true);
        }
    }

    // ---- Kill sounds -------------------------------------------------------
    static final class KillSounds extends BlueMod {
        private final Setting sound = choice("sound", "Sound", 0, "Level up", "Experience orb", "Anvil land", "Bell", "Trident thunder");
        private final Setting volume = num("volume", "Volume", 0.1, 1.0, 2, 0.8);
        private Entity target; private int hitTicks;
        private static boolean hooked;
        KillSounds() {
            super("kill_sounds", "Kill Sounds", "UTILITY", Items.NOTE_BLOCK);
            if (!hooked) {
                hooked = true;
                AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
                    BlueMod m = ModManager.get("kill_sounds");
                    if (m instanceof KillSounds k && world.isClientSide() && entity instanceof LivingEntity) { k.target = entity; k.hitTicks = 60; }
                    return net.minecraft.world.InteractionResult.PASS;
                });
            }
        }
        @Override public void tick(Minecraft c) {
            if (target == null) return;
            if (--hitTicks < 0) { target = null; return; }
            if (target instanceof LivingEntity le && (le.isDeadOrDying() || le.getHealth() <= 0f || target.isRemoved())) {
                if (enabled && c.player != null) {
                    var ev = switch (sound.idx) {
                        case 1 -> SoundEvents.EXPERIENCE_ORB_PICKUP;
                        case 2 -> SoundEvents.ANVIL_LAND;
                        case 3 -> SoundEvents.BELL_BLOCK;
                        case 4 -> SoundEvents.TRIDENT_THUNDER.value();
                        default -> SoundEvents.PLAYER_LEVELUP;
                    };
                    c.player.playSound(ev, (float) volume.n, 1.0f);
                }
                target = null;
            }
        }
    }

    // ---- Shulker preview ---------------------------------------------------
    static final class ShulkerPreview extends BlueMod {
        private static boolean hooked;
        ShulkerPreview() {
            super("shulker_preview", "Shulker Preview", "UTILITY", Items.SHULKER_BOX);
            if (!hooked) {
                hooked = true;
                net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
                    BlueMod m = VisualMods.active("shulker_preview");
                    if (m == null) return;
                    ItemContainerContents cc = stack.get(DataComponents.CONTAINER);
                    if (cc == null) return;
                    java.util.List<ItemStack> items = new ArrayList<>();
                    for (ItemStack s : cc.nonEmptyItemCopyStream().toList()) if (!s.isEmpty()) items.add(s);
                    if (items.isEmpty()) return;
                    try {
                        // the grid replaces vanilla's text list of contents
                        lines.removeIf(l -> l.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().startsWith("container.shulkerBox"));
                        lines.add(TipLines.box(items));
                    } catch (UnsupportedOperationException ignored) { }
                });
            }
        }
    }
}