package com.blairdash.blueclient.mods;

import com.blairdash.blueclient.BlueClientInit;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Text HUD mods: counters, timers, and small info displays. */
final class HudMods {
    private HudMods() { }

    static void register(List<BlueMod> out) {
        out.add(new ItemCounter());
        out.add(new PotionCounter());
        out.add(new TotemCounter());
        out.add(new DayCounter());
        out.add(new Stopwatch());
        out.add(new Playtime());
        out.add(new ServerAddress());
        out.add(new Cooldowns());
        out.add(new Saturation());
        out.add(new TntCountdown());
        out.add(new ItemTracker());
        out.add(new HorseStats());
        out.add(new Waila());
        out.add(new MiniF3());
    }

    // ---- helpers -----------------------------------------------------------

    static int count(MinecraftClient c, java.util.function.Predicate<ItemStack> p) {
        if (c.player == null) return 0;
        int n = 0;
        var inv = c.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && p.test(s)) n += s.getCount();
        }
        return n;
    }

    static String time(long seconds) {
        long h = seconds / 3600, m = (seconds / 60) % 60, s = seconds % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    static KeyBinding key(String id, int glfwKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.clientmod." + id, InputUtil.Type.KEYSYM, glfwKey, BlueClientInit.CATEGORY));
    }

    // ---- counters ----------------------------------------------------------

    static final class ItemCounter extends TextHudMod {
        private final Setting what = choice("item", "Count", 0, "Held item", "Golden apples", "Ender pearls", "Arrows", "Totems", "Obsidian", "Cobwebs", "End crystals");
        ItemCounter() { super("item_counter", "Item Counter", "HUD", Items.CHEST, "Golden apples: 000"); }
        @Override protected List<String> lines(MinecraftClient c) {
            if (c.player == null) return null;
            Item item;
            switch (what.idx) {
                case 1: item = Items.GOLDEN_APPLE; break;
                case 2: item = Items.ENDER_PEARL; break;
                case 3: item = Items.ARROW; break;
                case 4: item = Items.TOTEM_OF_UNDYING; break;
                case 5: item = Items.OBSIDIAN; break;
                case 6: item = Items.COBWEB; break;
                case 7: item = Items.END_CRYSTAL; break;
                default:
                    ItemStack held = c.player.getMainHandStack();
                    if (held.isEmpty()) return null;
                    item = held.getItem();
            }
            final Item it = item;
            ItemStack shown = new ItemStack(it);
            return List.of(shown.getName().getString() + ": " + count(c, s -> s.isOf(it)));
        }
    }

    static final class PotionCounter extends TextHudMod {
        private final Setting kind = choice("kind", "Potion", 0, "Instant health", "Speed", "Strength", "Fire resistance", "Regeneration", "Any splash");
        private static final String[] KEYS = { "healing", "swiftness", "strength", "fire_resistance", "regeneration", "" };
        PotionCounter() { super("potion_counter", "Potion Counter", "HUD", Items.SPLASH_POTION, "Health potions: 00"); }
        @Override protected List<String> lines(MinecraftClient c) {
            String key = KEYS[kind.idx];
            int n = count(c, s -> {
                if (!s.isOf(Items.SPLASH_POTION)) return false;
                if (key.isEmpty()) return true;
                PotionContentsComponent pc = s.get(DataComponentTypes.POTION_CONTENTS);
                if (pc == null || pc.potion().isEmpty()) return false;
                String id = pc.potion().get().getIdAsString();
                return id.contains(key);
            });
            return List.of(kind.choiceText() + " pots: " + n);
        }
    }

    static final class TotemCounter extends TextHudMod {
        TotemCounter() { super("totem_counter", "Totem Counter", "HUD", Items.TOTEM_OF_UNDYING, "Totems: 00"); }
        @Override protected List<String> lines(MinecraftClient c) {
            return List.of("Totems: " + count(c, s -> s.isOf(Items.TOTEM_OF_UNDYING)));
        }
    }

    static final class Saturation extends TextHudMod {
        private final Setting showNumber = bool("show_number", "Show saturation number", false);
        private final Setting showOverlay = bool("show_overlay", "Show saturation overlay", true);
        private final Setting showTooltip = bool("show_tooltip", "Show food values in tooltip", true);
        private final Setting alwaysTooltip = bool("always_tooltip", "Always show food values in tooltip", true);
        private final Setting showRestored = bool("show_restored", "Show hunger restored from held food", true);
        private final Setting showOffhand = bool("show_offhand", "Show overlays for food in off-hand", true);
        private final Setting exhaustionUnderlay = bool("exhaustion_underlay", "Show exhaustion underlay", true);
        private final Setting healthOverlay = bool("show_health", "Show estimated health overlay", true);
        private final Setting animateIcons = bool("animate_icons", "Animate HUD icons to match Minecraft", true);
        private final Setting maxAlpha = num("max_alpha", "Max alpha of flashing HUD icons", 0, 1, 2, 0.65);        Saturation() { super("saturation", "Saturation", "HUD", Items.COOKED_BEEF, "Sat: 20.0"); enabled = true; registerTooltip(); }
        @Override protected List<String> lines(MinecraftClient c) { return null; }
        @Override public boolean isHud() { return false; }
        @Override public boolean drawsOverlay() { return true; }
        @Override public void render(DrawContext ctx, MinecraftClient c) {
            if (c.player == null || c.options.hudHidden || c.player.isInCreativeMode() || c.player.isSpectator()
                    || c.player.getVehicle() instanceof LivingEntity) return;
            var h = c.player.getHungerManager();
                        int right = Draw.guiW(ctx) / 2 + 91, y = Draw.guiH(ctx) - 39;
            float sat = h.getSaturationLevel();
            int tick = c.inGameHud.getTicks();
            float flash = flashAlpha(tick, (float) maxAlpha.n);
            if (exhaustionUnderlay.b) drawExhaustion(ctx, ((com.blairdash.blueclient.mixin.mods.HudHungerAccessor) h).blueclient$exhaustion(), right, y);
            if (showOverlay.b || showRestored.b || healthOverlay.b) {
                offsets(c, h, tick);
                if (showOverlay.b) drawSaturation(ctx, 0f, sat, right, y, 1f);
                var mainFood = c.player.getMainHandStack().get(DataComponentTypes.FOOD);
                var offFood = c.player.getOffHandStack().get(DataComponentTypes.FOOD);
                var food = (mainFood != null && (mainFood.canAlwaysEat() || h.getFoodLevel() < 20)) ? mainFood
                        : (showOffhand.b && offFood != null && (offFood.canAlwaysEat() || h.getFoodLevel() < 20) ? offFood : null);
                if (food != null) {
                    int nutrition = food.nutrition();
                    float satInc = food.saturation();
                    if (showRestored.b) {
                        drawHunger(ctx, nutrition, h.getFoodLevel(), right, y, flash);
                        if (showOverlay.b) {
                            int newFood = h.getFoodLevel() + nutrition;
                            float newSat = sat + satInc;
                            float gained = newSat > newFood ? newFood - sat : satInc;
                            drawSaturation(ctx, gained, sat, right, y, flash);
                        }
                    }
                    if (healthOverlay.b) drawHealth(ctx, c, h, nutrition, satInc, Draw.guiW(ctx) / 2 - 91, y, flash);
                }
            }
            if (showNumber.b) Draw.text(ctx, c, String.format(Locale.ROOT, "Sat: %.1f", sat), right - 81, y - 11, textColor.color, shadow.b);
        }

        // ---- ported from AppleSkin (public domain): flash timing, bar offsets, health estimate ----
        private static float flashAlpha(int tick, float max) {
            int u = Math.floorMod(tick, 32);
            float tri = u < 16 ? -0.5f + u * 0.125f : 1.5f - (u - 16) * 0.125f;
            return Math.max(0f, Math.min(1f, tri)) * Math.max(0f, Math.min(1f, max));
        }
        private static final int[] FOOD_DY = new int[10];
        private static int[] heartDy = new int[0];
        private static final java.util.Random RNG = new java.util.Random();
        /** Same PRNG order as the vanilla HUD, so the overlays shake together with the real icons. */
        private void offsets(MinecraftClient c, net.minecraft.entity.player.HungerManager h, int tick) {
            float maxHp = c.player.getMaxHealth();
            float absorb = (float) Math.ceil(c.player.getAbsorptionAmount());
            int bars = (int) Math.ceil((maxHp + absorb) / 2.0f);
            if (bars < 0 || bars > 1000) bars = 0;
            boolean animFood = animateIcons.b && h.getSaturationLevel() <= 0.0f && tick % (h.getFoodLevel() * 3 + 1) == 0;
            boolean animHealth = animateIcons.b && Math.ceil(c.player.getHealth()) <= 4;
            RNG.setSeed((long) (tick * 312871));
            if (heartDy.length != bars) heartDy = new int[bars];
            for (int i = bars - 1; i >= 0; --i) heartDy[i] = animHealth ? RNG.nextInt(2) : 0;
            for (int i = 0; i < 10; ++i) FOOD_DY[i] = animFood ? RNG.nextInt(3) - 1 : 0;
        }
        static void blit(DrawContext ctx, int x, int y, int[] px, float alpha) {
            if (alpha <= 0f) return;
            for (int i = 0; i < 81; i++) {
                int p = px[i];
                int a = (int) ((p >>> 24) * alpha);
                if (a <= 3) continue;
                int xx = x + i % 9, yy = y + i / 9;
                ctx.fill(xx, yy, xx + 1, yy + 1, (a << 24) | (p & 0xFFFFFF));
            }
        }
        private static void drawSaturation(DrawContext ctx, float gained, float level, int right, int y, float alpha) {
            if (level + gained < 0) return;
            float mod = Math.max(0f, Math.min(level + gained, 20f));
            int start = 0, end = (int) Math.ceil(mod / 2.0f);
            if (gained != 0) start = (int) Math.max(level / 2.0f, 0f);
            for (int i = start; i < end && i < 10; ++i) {
                float eff = (mod / 2.0f) - i;
                int u = eff >= 1 ? 3 : eff > .5 ? 2 : eff > .25 ? 1 : 0;
                blit(ctx, right - i * 8 - 9, y + FOOD_DY[i], SAT_ICONS[u], alpha);
            }
        }
        private static int[] foodHalf;
        private static void drawHunger(DrawContext ctx, int restored, int foodLevel, int right, int y, float alpha) {
            if (restored <= 0) return;
            if (foodHalf == null) {
                foodHalf = new int[81];
                for (int i = 0; i < 81; i++) foodHalf[i] = (i % 9) < 5 ? FOOD_FULL[i] : FOOD_EMPTY[i];
            }
            int mod = Math.max(0, Math.min(20, foodLevel + restored));
            int start = Math.max(0, foodLevel / 2), end = (int) Math.ceil(mod / 2.0f);
            for (int i = start; i < end && i < 10; ++i) {
                int x = right - i * 8 - 9, yy = y + FOOD_DY[i];
                blit(ctx, x, yy, FOOD_EMPTY, alpha * 0.25f);
                blit(ctx, x, yy, i * 2 + 1 == mod ? foodHalf : FOOD_FULL, alpha);
            }
        }
        private static final int[] HEART_FULL = heart(true), HEART_EMPTY = heart(false);
        private static final int[] HEART_HALF = halfHeart();
        private static int[] heart(boolean full) {
            String[] art = { ".OOO.OOO.", "OHHRRRRRO", "OHRRRRRRO", "ORRRRRRRO", ".ORRRRRO.", "..ORRRO..", "...ORO...", "....O...." };
            int[] out = new int[81];
            for (int r = 0; r < art.length; r++)
                for (int c = 0; c < 9; c++) {
                    char ch = art[r].charAt(c);
                    int v = 0;
                    if (ch == 'O') v = 0xFF000000;
                    else if (ch == 'R') v = full ? 0xFFFF1313 : 0xFF2B2B2B;
                    else if (ch == 'H') v = full ? 0xFFFFB0B0 : 0xFF2B2B2B;
                    out[r * 9 + c] = v;
                }
            return out;
        }
        private static int[] halfHeart() {
            int[] f = heart(true), e = heart(false), out = new int[81];
            for (int i = 0; i < 81; i++) out[i] = (i % 9) < 5 ? f[i] : e[i];
            return out;
        }
                private static int[] pFull, pHalf, pEmpty;
        private static long heartsAt;
        /** Health-restore hearts use the real heart textures from the active resource pack (re-read every few seconds). */
        private static void hearts(MinecraftClient c) {
            long now = System.currentTimeMillis();
            if (pFull != null && now - heartsAt < 3000) return;
            heartsAt = now;
            int[] f = readHeart(c, "full", 52), h = readHeart(c, "half", 61), e = readHeart(c, "container", 16);
            if (f != null && e != null) {
                pFull = f; pEmpty = e;
                if (h == null) { h = new int[81]; for (int i = 0; i < 81; i++) h[i] = (i % 9) < 5 ? f[i] : e[i]; }
                pHalf = h;
            } else if (pFull == null) {
                pFull = HEART_FULL; pEmpty = HEART_EMPTY; pHalf = HEART_HALF;
            }
        }
        private static int[] readHeart(MinecraftClient c, String name, int iconU) {
            for (int pass = 0; pass < 2; pass++) {
                try {
                    String path = pass == 0 ? "minecraft:textures/gui/sprites/hud/heart/" + name + ".png" : "minecraft:textures/gui/icons.png";
                    var res = c.getResourceManager().getResource(net.minecraft.util.Identifier.tryParse(path));
                    if (res.isEmpty()) continue;
                    try (java.io.InputStream in = res.get().getInputStream()) {
                        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
                        if (img == null) continue;
                        double s = pass == 0 ? img.getWidth() / 9.0 : img.getWidth() / 256.0;
                        int ox = pass == 0 ? 0 : iconU;
                        int[] out = new int[81];
                        for (int y = 0; y < 9; y++)
                            for (int x = 0; x < 9; x++) {
                                int sx = Math.min(img.getWidth() - 1, (int) ((ox + x) * s)), sy = Math.min(img.getHeight() - 1, (int) (y * s));
                                out[y * 9 + x] = img.getRGB(sx, sy);
                            }
                        return out;
                    }
                } catch (Throwable ignored) { }
            }
            return null;
        }private static void drawHealth(DrawContext ctx, MinecraftClient c, net.minecraft.entity.player.HungerManager h, int nutrition, float satInc, int left, int y, float alpha) {
            hearts(c);
            float health = c.player.getHealth(), maxHp = c.player.getMaxHealth();
            if (health <= 0 || health >= maxHp || h.getFoodLevel() >= 18) return;
            int foodAfter = Math.min(h.getFoodLevel() + nutrition, 20);
            if (foodAfter < 18) return;
            float satAfter = Math.min(h.getSaturationLevel() + satInc, (float) foodAfter);
            float inc = estimateHealth(foodAfter, satAfter, ((com.blairdash.blueclient.mixin.mods.HudHungerAccessor) h).blueclient$exhaustion());
            float modified = Math.min(health + inc, maxHp);
            if (modified <= health) return;
            int fixedModified = (int) Math.ceil(modified);
            int start = (int) Math.max(0, Math.ceil(health) / 2.0f);
            int end = (int) Math.max(0, Math.ceil(modified / 2.0f));
            for (int i = start; i < end && i < 10; ++i) {
                int x = left + i * 8, yy = y + (i < heartDy.length ? heartDy[i] : 0);
                blit(ctx, x, yy, pEmpty, alpha * 0.25f);
                blit(ctx, x, yy, i * 2 + 1 == fixedModified ? pHalf : pFull, alpha);
            }
        }
        private static float estimateHealth(int foodLevel, float saturationLevel, float exhaustionLevel) {
            float health = 0;
            if (!Float.isFinite(exhaustionLevel) || !Float.isFinite(saturationLevel)) return 0;
            while (foodLevel >= 18) {
                while (exhaustionLevel > 4.0f) {
                    exhaustionLevel -= 4.0f;
                    if (saturationLevel > 0) saturationLevel = Math.max(saturationLevel - 1, 0);
                    else foodLevel -= 1;
                }
                if (foodLevel >= 20 && Float.compare(saturationLevel, Float.MIN_NORMAL) > 0) {
                    float limited = Math.min(saturationLevel, 6.0f);
                    float untilAboveMax = Math.nextUp(4.0f) - exhaustionLevel;
                    int iterations = Math.max(1, (int) Math.ceil(untilAboveMax / limited));
                    health += (limited / 6.0f) * iterations;
                    exhaustionLevel += limited * iterations;
                } else if (foodLevel >= 18) {
                    health += 1;
                    exhaustionLevel += 6.0f;
                }
            }
            return health;
        }
        private static final int[] SIL_L = {2, 1, 0, 0, 1, 2, 3, 6, 6};
        private static final int[] SIL_E = {4, 5, 6, 7, 7, 7, 9, 9, 8};
        private static boolean insideIcon(int px, int py, int right, int y) {
            int row = py - y;
            if (row < 0 || row >= 9) return false;
            for (int k = 0; k < 10; k++) {
                int col = px - (right - k * 8 - 9);
                if (col >= SIL_L[row] && col < SIL_E[row]) return true;
            }
            return false;
        }
        /** The exhaustion checkerboard: drawn only in the gaps, as if it sat behind the hunger icons. */
        private static void drawExhaustion(DrawContext ctx, float exhaustion, int right, int y) {
            float ratio = Math.min(1f, Math.max(0f, exhaustion / 4.0f));
            int width = (int) (ratio * 81);
            for (int i = 0; i < width; i++) {
                int u = 81 - width + i;
                for (int row = 0; row < 9; row++) {
                    if (((u + row) & 1) == 0) continue;
                    int px = right - width + i, py = y + row;
                    if (insideIcon(px, py, right, y)) continue;
                    ctx.fill(px, py, px + 1, py + 1, 0xBF282828);
                }
            }
        }
        private static final int[][] SAT_ICONS = {
        {
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0x00000000, 0xFFC2A100, 0xFFC2A100,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0x00000000, 0xFFC2A100,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000
        },
        {
            0x00000000, 0x00000000, 0x00000000, 0xFFE9D262, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFDDC241, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000, 0xFFC2A100, 0xFFC2A100,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0x00000000, 0xFFC2A100,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000
        },
        {
            0x00000000, 0x00000000, 0xFFE9D262, 0xFFE9D262, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFDDC241, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0xFF9F8609, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0xFF9F8609, 0x00000000, 0xFFC2A100, 0xFFC2A100,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0x00000000, 0xFFC2A100,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000
        },
        {
            0x00000000, 0x00000000, 0xFFE9D262, 0xFFE9D262, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0xFFDDC241, 0x00000000, 0x00000000, 0xFFDDC241, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000,
            0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0xFF9F8609, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0xFF9F8609, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0xFF9F8609, 0x00000000, 0xFFC2A100, 0xFFC2A100,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0x00000000, 0xFFC2A100,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000
        }
        };
        static final int[] FOOD_FULL = {
            0x00000000, 0x00000000, 0xFF000000, 0xFF000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0xFF000000, 0xFFD42A2A, 0xFFB21818, 0xFF000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0xFF000000, 0xFFD42A2A, 0xFFDFB18F, 0xFFD42A2A, 0xFFB88458, 0xFF000000, 0x00000000, 0x00000000, 0x00000000,
            0xFF000000, 0xFFB21818, 0xFFD42A2A, 0xFFB88458, 0xFF9D6D43, 0xFFB88458, 0xFF000000, 0x00000000, 0x00000000,
            0x00000000, 0xFF000000, 0xFF613C1B, 0xFF7B512D, 0xFF9D6D43, 0xFF9D6D43, 0xFF000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0xFF000000, 0xFF613C1B, 0xFF7B512D, 0xFF7B512D, 0xFF000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFFE2D5AA, 0xFF000000, 0xFF000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF000000, 0xFFFFF7DC, 0xFF000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF000000, 0xFF000000, 0x00000000
        };
        static final int[] FOOD_EMPTY = {
            0x00000000, 0x00000000, 0xFF000000, 0xFF000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0xFF000000, 0xFF282828, 0xFF282828, 0xFF000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0xFF000000, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF000000, 0x00000000, 0x00000000, 0x00000000,
            0xFF000000, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF000000, 0x00000000, 0x00000000,
            0x00000000, 0xFF000000, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0xFF000000, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF282828, 0xFF000000, 0xFF000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF000000, 0xFF282828, 0xFF000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF000000, 0xFF000000, 0x00000000
        };
        // ---- tooltip ----
        private static boolean shiftDown() {
            try {
                Class<?> g = Class.forName("org.lwjgl.glfw.GLFW");
                long w = (long) g.getMethod("glfwGetCurrentContext").invoke(null);
                var key = g.getMethod("glfwGetKey", long.class, int.class);
                return (int) key.invoke(null, w, 340) == 1 || (int) key.invoke(null, w, 344) == 1;
            } catch (Throwable t) { return false; }
        }
        private static boolean tipDone;
        private void registerTooltip() {
            if (tipDone) return;
            tipDone = true;
            net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
                if (!enabled || !showTooltip.b) return;
                var food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return;
                if (!alwaysTooltip.b && !shiftDown()) return;
                int nutrition = food.nutrition();
                float satValue = food.saturation();
                lines.add(TipLines.food(nutrition, satValue));
            });
        }
    }

    // ---- time --------------------------------------------------------------

    static final class DayCounter extends TextHudMod {
        DayCounter() { super("day_counter", "Day Counter", "HUD", Items.DAYLIGHT_DETECTOR, "Day: 0000"); }
        @Override protected List<String> lines(MinecraftClient c) {
            if (c.world == null) return null;
            return List.of("Day: " + (c.world.getTimeOfDay() / 24000L + 1));
        }
    }

    static final class Stopwatch extends TextHudMod {
        private final KeyBinding startStop = key("stopwatch_toggle", InputUtil.UNKNOWN_KEY.getCode());
        private final KeyBinding reset = key("stopwatch_reset", InputUtil.UNKNOWN_KEY.getCode());
        private long accumulated, startedAt;
        private boolean running;
        Stopwatch() { super("stopwatch", "Stopwatch", "HUD", Items.CLOCK, "Stopwatch: 0:00.0"); }
        @Override public void tick(MinecraftClient c) {
            while (startStop.wasPressed()) {
                if (running) { accumulated += System.currentTimeMillis() - startedAt; running = false; }
                else { startedAt = System.currentTimeMillis(); running = true; }
            }
            while (reset.wasPressed()) { accumulated = 0; startedAt = System.currentTimeMillis(); }
        }
        @Override protected List<String> lines(MinecraftClient c) {
            long ms = accumulated + (running ? System.currentTimeMillis() - startedAt : 0);
            long s = ms / 1000;
            return List.of(String.format(Locale.ROOT, "Stopwatch: %d:%02d.%d%s", s / 60, s % 60, (ms / 100) % 10, running ? "" : " (paused)"));
        }
    }

    static final class Playtime extends TextHudMod {
        private final Setting total = num("total", "Total seconds", 0, 1e12, 0, 0).hidden();
        private final Setting showTotal = bool("show_total", "Show total playtime", true);
        private long sessionStart = System.currentTimeMillis(), lastCount = System.currentTimeMillis();
        Playtime() { super("playtime", "Playtime", "HUD", Items.RECOVERY_COMPASS, "Session: 0:00:00\nTotal: 0:00:00"); }
        @Override public void tick(MinecraftClient c) {
            long now = System.currentTimeMillis();
            if (now - lastCount >= 1000) { total.n += (now - lastCount) / 1000; lastCount = now; }
        }
        @Override protected int sampleLines() { return showTotal.b ? 2 : 1; }
        @Override protected List<String> lines(MinecraftClient c) {
            List<String> l = new ArrayList<>();
            l.add("Session: " + time((System.currentTimeMillis() - sessionStart) / 1000));
            if (showTotal.b) l.add("Total: " + time((long) total.n));
            return l;
        }
    }

    static final class ServerAddress extends TextHudMod {
        ServerAddress() { super("server_address", "Server Address", "HUD", Items.OAK_SIGN, "play.example.com"); }
        @Override protected List<String> lines(MinecraftClient c) {
            if (c.isInSingleplayer()) return List.of("Singleplayer");
            var e = c.getCurrentServerEntry();
            return e == null ? null : List.of(e.address);
        }
    }

    // ---- combat info -------------------------------------------------------

    static final class Cooldowns extends TextHudMod {
        private final Setting showIcons = bool("show_icons", "Show item icons", true);
        private final Setting showText = bool("show_text", "Show remaining seconds", true);
        private final Setting decimals = num("decimals", "Timer decimals", 0, 2, 0, 1);
        private record Row(ItemStack stack, double seconds) { }
        Cooldowns() { super("cooldowns", "Cooldowns", "HUD", Items.ENDER_PEARL, "Ender Pearl: 00.0s"); }
        private List<Row> rows(MinecraftClient c) {
            List<Row> out = new ArrayList<>();
            if (c.player == null) return out;
            var cm = c.player.getItemCooldownManager();
            var data = (com.blairdash.blueclient.mixin.mods.HudCooldownAccessor) cm;
            Map<Item, Row> unique = new LinkedHashMap<>();
            List<ItemStack> stacks = new ArrayList<>();
            var inv = c.player.getInventory();
            for (int i = 0; i < inv.size(); i++) stacks.add(inv.getStack(i));
            stacks.add(c.player.getOffHandStack());
            for (ItemStack stack : stacks) {
                if (stack.isEmpty() || cm.getCooldownProgress(stack, 0f) <= 0) continue;
                Object entry = data.blueclient$entries().get(cm.getGroup(stack));
                if (!(entry instanceof com.blairdash.blueclient.mixin.mods.HudCooldownEntryAccessor timer)) continue;
                double seconds = (timer.blueclient$endTick() - data.blueclient$tick()) / 20.0;
                if (seconds <= 0) continue;
                Row row = new Row(stack, seconds), old = unique.get(stack.getItem());
                if (old == null || old.seconds < seconds) unique.put(stack.getItem(), row);
            }
            out.addAll(unique.values());
            out.sort(java.util.Comparator.comparingDouble(Row::seconds)
                    .thenComparing(r -> r.stack.getName().getString()));
            return out;
        }
        private String label(Row row) {
            int d = Math.max(0, Math.min(2, (int) decimals.n));
            // Round upwards so an active cooldown never displays 0 seconds.
            double factor = Math.pow(10, d), seconds = Math.ceil(row.seconds * factor) / factor;
            return String.format(Locale.ROOT, "%s: %." + d + "fs", row.stack.getName().getString(), seconds);
        }
        @Override protected List<String> lines(MinecraftClient c) { return List.of(); }
        @Override public int hudW(MinecraftClient c) {
            int w = showIcons.b ? 16 : 0;
            if (showText.b) for (Row row : rows(c)) w = Math.max(w, (showIcons.b ? 20 : 0) + Draw.width(c, label(row)));
            return Math.round((float) (Math.max(w, 16) * scale.n));
        }
        @Override public int hudH(MinecraftClient c) { return Math.round((float) (Math.max(1, rows(c).size()) * (showIcons.b ? 18 : 10) * scale.n)); }
        @Override public void render(DrawContext ctx, MinecraftClient c) {
            if (c.options.hudHidden || (!showIcons.b && !showText.b)) return;
            List<Row> rows = rows(c);
            if (rows.isEmpty()) return;
            float s = (float) scale.n;
            int x = Math.round(x(c, Draw.guiW(ctx), Draw.guiH(ctx)) / s);
            int y = Math.round(y(c, Draw.guiW(ctx), Draw.guiH(ctx)) / s);
            Draw.push(ctx, s);
            for (Row row : rows) {
                if (showIcons.b) ctx.drawItem(row.stack, x, y);
                if (showText.b) Draw.text(ctx, c, label(row), x + (showIcons.b ? 20 : 0), y + (showIcons.b ? 4 : 0), textColor.color, shadow.b);
                y += showIcons.b ? 18 : 10;
            }
            Draw.pop(ctx);
        }
    }

    static final class TntCountdown extends TextHudMod {
        TntCountdown() { super("tnt_countdown", "TNT Countdown", "HUD", Items.TNT, "TNT: 0.0s (00 blocks)"); }
        @Override protected List<String> lines(MinecraftClient c) {
            if (c.world == null || c.player == null) return null;
            TntEntity best = null; double bestD = 1e9;
            for (Entity e : c.world.getEntities()) {
                if (e instanceof TntEntity t) {
                    double d = t.squaredDistanceTo(c.player);
                    if (d < bestD && d < 48 * 48) { bestD = d; best = t; }
                }
            }
            if (best == null) return null;
            return List.of(String.format(Locale.ROOT, "TNT: %.1fs (%d blocks)", best.getFuse() / 20.0, (int) Math.sqrt(bestD)));
        }
    }

    static final class ItemTracker extends TextHudMod {
        private final Map<String, Integer> last = new LinkedHashMap<>();
        private final Map<String, Integer> gained = new LinkedHashMap<>();
        private long nextScan;
        private final KeyBinding reset = key("item_tracker_reset", InputUtil.UNKNOWN_KEY.getCode());
        ItemTracker() { super("item_tracker", "Item Tracker", "HUD", Items.HOPPER, "+00 Diamond\n+00 Iron Ingot"); }
        @Override public void tick(MinecraftClient c) {
            while (reset.wasPressed()) { gained.clear(); last.clear(); }
            long now = System.currentTimeMillis();
            if (!enabled || c.player == null || now < nextScan) return;
            nextScan = now + 500;
            Map<String, Integer> cur = new LinkedHashMap<>();
            var inv = c.player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (!s.isEmpty()) cur.merge(s.getName().getString(), s.getCount(), Integer::sum);
            }
            if (!last.isEmpty()) {
                for (var e : cur.entrySet()) {
                    int before = last.getOrDefault(e.getKey(), 0);
                    if (e.getValue() > before) gained.merge(e.getKey(), e.getValue() - before, Integer::sum);
                }
            }
            last.clear(); last.putAll(cur);
        }
        @Override protected int sampleLines() { return 2; }
        @Override protected List<String> lines(MinecraftClient c) {
            if (gained.isEmpty()) return null;
            List<String> out = new ArrayList<>();
            gained.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(6)
                    .forEach(e -> out.add("+" + e.getValue() + " " + e.getKey()));
            return out;
        }
    }

    static final class HorseStats extends TextHudMod {
        HorseStats() { super("horse_stats", "Horse Stats", "HUD", Items.SADDLE, "Speed: 00.0 b/s\nJump: 0.0 blocks\nHealth: 00"); }
        @Override protected int sampleLines() { return 3; }
        @Override protected List<String> lines(MinecraftClient c) {
            if (c.player == null) return null;
            AbstractHorseEntity h = null;
            if (c.player.getVehicle() instanceof AbstractHorseEntity v) h = v;
            else if (c.crosshairTarget instanceof EntityHitResult r && r.getEntity() instanceof AbstractHorseEntity t) h = t;
            if (h == null) return null;
            double speed = h.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) * 43.17;
            double j = h.getAttributeValue(EntityAttributes.JUMP_STRENGTH);
            double jump = -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367;
            return List.of(String.format(Locale.ROOT, "Speed: %.1f b/s", speed),
                    String.format(Locale.ROOT, "Jump: %.1f blocks", jump),
                    String.format(Locale.ROOT, "Health: %.0f", h.getHealth()));
        }
    }

    static final class Waila extends TextHudMod {
        Waila() { super("waila", "WAILA", "HUD", Items.SPYGLASS, "Diamond Ore\nminecraft:diamond_ore"); }
        @Override protected int sampleLines() { return 2; }
        @Override public int defaultX(MinecraftClient c, int sw, int sh) { return sw / 2 - 40; }
        @Override public int defaultY(MinecraftClient c, int sw, int sh) { return 6; }
        @Override protected List<String> lines(MinecraftClient c) {
            HitResult hit = c.crosshairTarget;
            if (hit == null || c.world == null) return null;
            if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
                BlockState st = c.world.getBlockState(b.getBlockPos());
                if (st.isAir()) return null;
                return List.of(st.getBlock().getName().getString(),
                        net.minecraft.registry.Registries.BLOCK.getId(st.getBlock()).toString());
            }
            if (hit instanceof EntityHitResult e) {
                Entity en = e.getEntity();
                List<String> l = new ArrayList<>();
                l.add(en.getName().getString());
                if (en instanceof LivingEntity le) l.add(String.format(Locale.ROOT, "Health: %.1f / %.1f", le.getHealth(), le.getMaxHealth()));
                return l;
            }
            return null;
        }
    }

    static final class MiniF3 extends TextHudMod {
        private final Setting coordinates = bool("coordinates", "Coordinates", true);
        private final Setting blockPosition = bool("block_position", "Block position", false);
        private final Setting biome = bool("biome", "Biome", true);
        private final Setting light = bool("light_level", "Light level", true);
        private final Setting facing = bool("facing", "Facing / direction", true);
        private final Setting chunk = bool("chunk", "Chunk", true);
        private final Setting fps = bool("fps", "FPS", true);
        private final Setting ping = bool("ping", "Ping", false);
        private final Setting speed = bool("speed", "Speed (blocks/s)", false);
        private final Setting memory = bool("memory", "Memory usage", false);
        private final Setting clock = bool("time", "Time", false);
        private final Setting decimals = num("decimals", "Coordinate decimals", 0, 3, 0, 1);
        private Object trackedPlayer, trackedWorld;
        private double lastX, lastY, lastZ, blocksPerSecond;
        MiniF3() { super("mini_f3", "F3 Display", "HUD", Items.COMMAND_BLOCK, "XYZ: -00000.000 / 000.000 / -00000.000"); }
        @Override public void tick(MinecraftClient c) {
            if (c.player == null) return;
            double x = c.player.getX(), y = c.player.getY(), z = c.player.getZ();
            double distance = Math.sqrt(Math.pow(x - lastX, 2) + Math.pow(y - lastY, 2) + Math.pow(z - lastZ, 2));
            blocksPerSecond = trackedPlayer == c.player && trackedWorld == c.world && distance < 64 ? distance * 20 : 0;
            trackedPlayer = c.player; trackedWorld = c.world;
            lastX = x; lastY = y; lastZ = z;
        }
        @Override public void tickMenu(MinecraftClient c) { trackedPlayer = null; trackedWorld = null; blocksPerSecond = 0; }
        @Override protected int sampleLines() {
            int n = 0;
            for (Setting s : new Setting[] {coordinates, blockPosition, biome, light, facing, chunk, fps, ping, speed, memory, clock}) if (s.b) n++;
            return n;
        }
        @Override protected List<String> lines(MinecraftClient c) {
            if (c.player == null || c.world == null || c.options.hudHidden) return null;
            BlockPos p = c.player.getBlockPos();
            List<String> out = new ArrayList<>();
            int d = Math.max(0, Math.min(3, (int) decimals.n));
            if (coordinates.b) out.add(String.format(Locale.ROOT, "XYZ: %." + d + "f / %." + d + "f / %." + d + "f", c.player.getX(), c.player.getY(), c.player.getZ()));
            if (blockPosition.b) out.add("Block: " + p.getX() + " " + p.getY() + " " + p.getZ());
            if (biome.b) out.add("Biome: " + c.world.getBiome(p).getKey().map(k -> k.getValue().toString()).orElse("?"));
            if (light.b) out.add("Light: " + c.world.getLightLevel(p));
            if (facing.b) out.add("Facing: " + c.player.getHorizontalFacing().asString());
            if (chunk.b) out.add("Chunk: " + (p.getX() >> 4) + " " + (p.getZ() >> 4));
            if (fps.b) out.add("FPS: " + c.getCurrentFps());
            if (ping.b) {
                var entry = c.getNetworkHandler() == null ? null : c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
                out.add("Ping: " + (entry == null ? "?" : entry.getLatency() + " ms"));
            }
            if (speed.b) out.add(String.format(Locale.ROOT, "Speed: %.2f blocks/s", blocksPerSecond));
            if (memory.b) {
                Runtime r = Runtime.getRuntime();
                long used = r.totalMemory() - r.freeMemory(), max = r.maxMemory();
                out.add(String.format(Locale.ROOT, "Memory: %d / %d MiB (%d%%)", used / 1048576, max / 1048576, used * 100 / max));
            }
            if (clock.b) out.add("Time: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            return out;
        }
    }
}
