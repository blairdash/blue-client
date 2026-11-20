package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.ConfigStorage;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Right Shift menu: a card grid of mods (Options + toggle switch),
 * category filters and search, a profiles sidebar behind an icon rail, a
 * Settings tab, and a button to open the drag-to-move HUD layout editor.
 * Styled like the launcher: rounded shapes, same palette.
 */
public class BlueClientScreen extends GuiScreen {
    private enum Tab { MODS, SETTINGS }

    private static final String[] CATEGORIES = { "ALL", ModRegistry.HUD, ModRegistry.MECHANIC, ModRegistry.VISUAL, "COSMETICS" };

    private static final int HEADER_H = 32;
    private static final int RAIL_W = 44;
    private static final int SIDEBAR_W = 128;
    private static final int CARD_H = 92;
    private static final int GAP = 8;

    private Tab tab = Tab.MODS;
    private String category = "ALL";
    private String search = "";
    private boolean searchFocused;

    private final List<ModEntry> mods = new ArrayList<ModEntry>();
    private List<String> profiles = new ArrayList<String>();
    private double scroll;

    // "Save as new profile" name prompt
    private boolean naming;
    private String nameBuffer = "";

    private final List<SliderControl> settingSliders = new ArrayList<SliderControl>();

    private ItemStack[] railIcons;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        mods.clear();
        mods.addAll(ModRegistry.create());
        profiles = ConfigStorage.listProfiles();
        railIcons = new ItemStack[] { new ItemStack(Items.nether_star), new ItemStack(Items.comparator) };

        settingSliders.clear();
        settingSliders.add(new SliderControl("Zoom FOV (hold C, scroll to adjust)", 4, 60, 0,
                () -> ModConfig.zoomFov, v -> ModConfig.zoomFov = v));
        settingSliders.add(new SliderControl("Low Fire offset (larger = lower)", 0.0, 0.8, 2,
                () -> (double) ModConfig.lowFireOffset, v -> ModConfig.lowFireOffset = v.floatValue()));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        ConfigStorage.save();
    }

    // --- layout ---------------------------------------------------------

    private FontRenderer fr() { return fontRendererObj; }

    // The menu is laid out in "UI units" of S physical pixels (not GUI-scale units), so it stays
    // sharp at any GUI scale instead of being blown up like Minecraft's own widgets.
    private int gs() { return Math.max(1, new ScaledResolution(mc).getScaleFactor()); }
    private int uiScale() { return Math.max(1, Math.round(mc.displayHeight / 540f)); }
    private int uw() { return mc.displayWidth / uiScale(); }
    private int uh() { return mc.displayHeight / uiScale(); }
    private int mouseUiX() { return Mouse.getX() / uiScale(); }
    private int mouseUiY() { return (mc.displayHeight - Mouse.getY() - 1) / uiScale(); }
    private int pw() { return Math.min(uw() - 16, 660); }
    private int ph() { return Math.min(uh() - 16, 350); }
    private int px() { return (uw() - pw()) / 2; }
    private int py() { return (uh() - ph()) / 2; }
    /** Left edge of the profiles sidebar (right of the icon rail). */
    private int sx() { return px() + RAIL_W; }

    private int bodyTop() { return py() + HEADER_H; }
    private int contentX() { return sx() + SIDEBAR_W + GAP; }
    private int contentW() { return pw() - RAIL_W - SIDEBAR_W - GAP * 2; }
    private int gridTop() { return bodyTop() + 30; }
    private int gridBottom() { return py() + ph() - GAP; }

    private int columns() { return contentW() >= 330 ? 3 : 2; }
    private int cardW() { return (contentW() - (columns() - 1) * GAP) / columns(); }

    private List<ModEntry> visibleMods() {
        List<ModEntry> out = new ArrayList<ModEntry>();
        String q = search.trim().toLowerCase(Locale.ROOT);
        for (ModEntry m : mods) {
            if (!category.equals("ALL") && !m.category.equals(category)) continue;
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    private int contentHeight(int count) {
        int rows = (count + columns() - 1) / columns();
        return rows * (CARD_H + GAP);
    }

    private double maxScroll(int count) {
        return Math.max(0, contentHeight(count) - (gridBottom() - gridTop()));
    }

    private int[] pillBounds(int index) {
        int x = contentX();
        for (int i = 0; i < index; i++) x += Ui.width(CATEGORIES[i]) + 14 + 4;
        return new int[] { x, bodyTop() + 6, Ui.width(CATEGORIES[index]) + 14, 16 };
    }

    private int[] searchBounds() {
        int w = 96;
        return new int[] { contentX() + contentW() - w, bodyTop() + 6, w, 16 };
    }

    private int[] saveProfileBounds() { return new int[] { sx() + 8, py() + ph() - 46, SIDEBAR_W - 16, 16 }; }
    private int[] hudLayoutBounds() { return new int[] { sx() + 8, py() + ph() - 26, SIDEBAR_W - 16, 18 }; }

    /** Icon rail button i (0 = Mods, 1 = Settings), like the launcher's left rail. */
    private int[] tabBounds(int i) { return new int[] { px() + 6, bodyTop() + 8 + i * 38, 32, 32 }; }

    private int[] closeBounds() { return new int[] { px() + pw() - 26, py() + 7, 18, 18 }; }

    private int profileRowY(int i) { return bodyTop() + 22 + i * 19; }

    private int maxProfileRows() { return Math.max(1, (py() + ph() - 56 - (bodyTop() + 22)) / 19); }

    // --- render ---------------------------------------------------------

    @Override
    public void drawScreen(int rawMx, int rawMy, float partialTicks) {
        Ui.fill(0, 0, width, height, Ui.DIM);

        int uiS = uiScale();
        float k = uiS / (float) gs();
        int mx = mouseUiX(), my = mouseUiY();
        Ui.res = uiS;
        GlStateManager.pushMatrix();
        GlStateManager.scale(k, k, 1.0f);

        int px = px(), py = py(), pw = pw(), ph = ph();
        Ui.card(px - 1, py - 1, pw + 2, ph + 2, 14, Ui.PANEL, Ui.PANEL_EDGE);

        renderHeader(mx, my);
        renderRail(mx, my);
        renderSidebar(mx, my);

        if (tab == Tab.MODS) renderMods(mx, my);
        else renderSettings(mx, my);

        if (naming) renderNamePrompt();

        GlStateManager.popMatrix();
        Ui.res = 1;
    }

    private void renderHeader(int mx, int my) {
        int px = px(), py = py(), pw = pw();
        Ui.rrect(px, py, pw, HEADER_H, 13, Ui.HEADER);
        Ui.fill(px, py + 14, px + pw, py + HEADER_H, Ui.HEADER);
        Ui.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, Ui.PANEL_EDGE);
        Ui.rrect(px + 16, py + 11, 10, 10, 5, Ui.ACCENT);
        Ui.text(fr(), "BLUE CLIENT", px + 34, py + 12, Ui.MUTED);

        int[] cb = closeBounds();
        boolean hover = Ui.inside(mx, my, cb[0], cb[1], cb[2], cb[3]);
        if (hover) Ui.rrect(cb[0], cb[1], cb[2], cb[3], 9, 0xFFD9435A);
        Ui.label(fr(), "X", cb[0], cb[1], cb[2], cb[3], hover ? 0xFFFFFFFF : Ui.MUTED);
    }

    private void renderRail(int mx, int my) {
        int px = px(), bt = bodyTop(), bottom = py() + ph();
        Ui.rrect(px, bt, RAIL_W + 14, bottom - bt, 13, Ui.HEADER);
        Ui.fill(px, bt, px + RAIL_W + 14, bt + 14, Ui.HEADER);
        Ui.fill(px + RAIL_W - 1, bt, px + RAIL_W, bottom, Ui.PANEL_EDGE);

        String[] names = { "Mods", "Settings" };
        String tip = null; int[] tipAt = null;
        for (int i = 0; i < 2; i++) {
            int[] b = tabBounds(i);
            boolean active = (i == 0) == (tab == Tab.MODS);
            boolean hover = Ui.inside(mx, my, b[0], b[1], b[2], b[3]);
            if (active) {
                Ui.rrect(b[0], b[1], b[2], b[3], 16, Ui.SELECTED);
                Ui.fill(px, b[1] + 7, px + 2, b[1] + b[3] - 7, Ui.ACCENT);
            } else if (hover) {
                Ui.rrect(b[0], b[1], b[2], b[3], 16, Ui.CARD);
            }
            drawItem(railIcons[i], b[0] + 8, b[1] + 8);
            if (hover) { tip = names[i]; tipAt = b; }
        }
        if (tip != null) {
            int tw = Ui.width(tip) + 12;
            int tx = tipAt[0] + tipAt[2] + 10, ty = tipAt[1] + 8;
            Ui.card(tx, ty, tw, 16, 8, Ui.CARD, Ui.PANEL_EDGE);
            Ui.text(fr(), tip, tx + 6, ty + 4, Ui.TEXT);
        }
    }

    private void drawItem(ItemStack stack, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        this.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
    }
    private void renderSidebar(int mx, int my) {
        int px = sx(), bt = bodyTop(), bottom = py() + ph();
        Ui.fill(px, bt, px + SIDEBAR_W, bottom, Ui.SIDEBAR);
        Ui.fill(px + SIDEBAR_W - 1, bt, px + SIDEBAR_W, bottom, Ui.PANEL_EDGE);
        Ui.text(fr(), "PROFILES", px + 10, bt + 8, Ui.MUTED);

        String active = ConfigStorage.getActiveProfile();
        int maxRows = maxProfileRows();
        for (int i = 0; i < profiles.size() && i < maxRows; i++) {
            String name = profiles.get(i);
            int y = profileRowY(i);
            boolean isActive = name.equals(active);
            boolean hover = Ui.inside(mx, my, px + 6, y, SIDEBAR_W - 12, 17);
            Ui.rrect(px + 6, y, SIDEBAR_W - 13, 17, 8, isActive ? Ui.SELECTED : (hover ? Ui.CARD_HOVER : Ui.CARD));
            if (isActive) Ui.fill(px + 6, y + 4, px + 8, y + 13, Ui.ACCENT);
            boolean deletable = !ConfigStorage.DEFAULT_PROFILE.equals(name);
            int textMax = SIDEBAR_W - 12 - 14 - (deletable ? 14 : 0);
            Ui.text(fr(), Ui.clip(fr(), name, textMax), px + 14, y + 5, isActive ? 0xFFFFFFFF : Ui.TEXT);
            if (deletable) {
                int dx = px + SIDEBAR_W - 6 - 14;
                boolean dh = Ui.inside(mx, my, dx, y, 14, 17);
                Ui.text(fr(), "x", dx + 4, y + 5, dh ? Ui.BAD_HOVER : Ui.MUTED);
            }
        }

        int[] sp = saveProfileBounds();
        boolean h1 = Ui.inside(mx, my, sp[0], sp[1], sp[2], sp[3]);
        Ui.card(sp[0], sp[1], sp[2], sp[3], 8, h1 ? Ui.CARD_HOVER : Ui.CARD, Ui.PANEL_EDGE);
        Ui.label(fr(), "SAVE AS NEW PROFILE", sp[0], sp[1], sp[2], sp[3], Ui.MUTED);

        int[] hb = hudLayoutBounds();
        boolean h2 = Ui.inside(mx, my, hb[0], hb[1], hb[2], hb[3]);
        Ui.rrect(hb[0], hb[1], hb[2], hb[3], 9, h2 ? Ui.ACCENT : Ui.ACCENT_DARK);
        Ui.label(fr(), "EDIT HUD LAYOUT", hb[0], hb[1], hb[2], hb[3], 0xFFFFFFFF);
    }

    private void renderMods(int mx, int my) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            int[] b = pillBounds(i);
            boolean active = CATEGORIES[i].equals(category);
            boolean hover = Ui.inside(mx, my, b[0], b[1], b[2], b[3]);
            Ui.rrect(b[0], b[1], b[2], b[3], 8, active ? Ui.ACCENT : (hover ? Ui.CARD_HOVER : Ui.CARD));
            Ui.label(fr(), CATEGORIES[i], b[0], b[1], b[2], b[3], active ? 0xFFFFFFFF : Ui.MUTED);
        }
        int[] sb = searchBounds();
        Ui.card(sb[0], sb[1], sb[2], sb[3], 8, Ui.FIELD, searchFocused ? Ui.ACCENT : Ui.PANEL_EDGE);
        String shown = search.isEmpty() && !searchFocused ? "Search..." : search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        Ui.text(fr(), Ui.clip(fr(), shown, sb[2] - 10), sb[0] + 5, sb[1] + 4,
                search.isEmpty() && !searchFocused ? Ui.DISABLED_TEXT : Ui.TEXT);

        List<ModEntry> list = visibleMods();
        scroll = Math.max(0, Math.min(scroll, maxScroll(list.size())));
        int gx = contentX(), top = gridTop(), bottom = gridBottom();
        boolean mouseInGrid = mx >= gx && mx < gx + contentW() && my >= top && my < bottom;
        Ui.scissorUi(gx, top, contentW(), bottom - top);
        int cw = cardW();
        for (int i = 0; i < list.size(); i++) {
            int col = i % columns(), row = i / columns();
            int x = gx + col * (cw + GAP);
            int y = top + row * (CARD_H + GAP) - (int) scroll;
            if (y + CARD_H < top || y > bottom) continue;
            renderCard(list.get(i), x, y, cw, mx, my, mouseInGrid);
        }
        if (list.isEmpty()) {
            Ui.centered(fr(), "No mods match.", gx + contentW() / 2, top + 20, Ui.MUTED);
        }
        Ui.endScissor();

        double max = maxScroll(list.size());
        if (max > 0) {
            int trackH = bottom - top;
            int thumbH = Math.max(18, (int) (trackH * (trackH / (double) contentHeight(list.size()))));
            int thumbY = top + (int) ((trackH - thumbH) * (scroll / max));
            Ui.fill(gx + contentW() + 2, top, gx + contentW() + 4, bottom, Ui.FIELD);
            Ui.fill(gx + contentW() + 2, thumbY, gx + contentW() + 4, thumbY + thumbH, Ui.MUTED);
        }
    }
    private void renderCard(ModEntry m, int x, int y, int w, int mx, int my, boolean mouseInGrid) {
        boolean hoverCard = mouseInGrid && Ui.inside(mx, my, x, y, w, CARD_H);
        boolean on = m.enabled.get();
        Ui.card(x, y, w, CARD_H, 14, hoverCard ? Ui.CARD_HOVER : Ui.CARD, on ? 0xFF24406F : Ui.PANEL_EDGE);

        Ui.card(x + w / 2 - 16, y + 6, 32, 32, 16, Ui.FIELD, Ui.PANEL_EDGE);
        drawItem(m.icon, x + w / 2 - 8, y + 14);

        Ui.centered(fr(), Ui.clip(fr(), m.name, w - 8), x + w / 2, y + 41, Ui.TEXT);

        int ox = x + 6, oy = y + CARD_H - 40, ow = w - 12, oh = 16;
        boolean hasOptions = m.options != null;
        boolean hoverOpt = mouseInGrid && hasOptions && Ui.inside(mx, my, ox, oy, ow, oh);
        Ui.card(ox, oy, ow, oh, 8, hoverOpt ? Ui.SELECTED : Ui.FIELD, hoverOpt ? Ui.ACCENT : Ui.PANEL_EDGE);
        Ui.label(fr(), "OPTIONS", ox, oy, ow, oh, hasOptions ? (hoverOpt ? Ui.ACCENT : Ui.TEXT) : Ui.DISABLED_TEXT);

        int ey = y + CARD_H - 20, eh = 20;
        boolean hoverBar = mouseInGrid && Ui.inside(mx, my, x, ey, w, eh);
        Ui.text(fr(), on ? "ENABLED" : "DISABLED", x + 8, ey + 6, on ? Ui.GOOD : Ui.MUTED);
        Ui.toggle(x + w - 26 - 8, ey + 4, on, hoverBar);
    }

    private void renderSettings(int mx, int my) {
        int x = contentX(), y = bodyTop() + 12, w = contentW() - 6;
        Ui.text(fr(), "SETTINGS", x, y, Ui.MUTED);
        y += 18;
        for (SliderControl s : settingSliders) {
            s.place(x, y, w);
            s.render(fr(), mx, my);
            y += SliderControl.HEIGHT + 12;
        }
        y += 4;
        Ui.text(fr(), "Menu: Right Shift     Zoom: hold C (scroll while zoomed)", x, y, Ui.MUTED);
        Ui.text(fr(), "Settings save automatically to the active profile.", x, y + 12, Ui.MUTED);
    }

    private void renderNamePrompt() {
        Ui.fill(0, 0, uw(), uh(), 0x99000000);
        int w = 240, h = 74, x = (uw() - w) / 2, y = (uh() - h) / 2;
        Ui.card(x - 1, y - 1, w + 2, h + 2, 14, 0xFF111624, Ui.PANEL_EDGE);
        Ui.text(fr(), "NEW PROFILE NAME", x + 10, y + 9, Ui.MUTED);
        Ui.card(x + 10, y + 24, w - 20, 18, 9, Ui.FIELD, Ui.ACCENT);
        String shown = nameBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        Ui.text(fr(), shown, x + 15, y + 29, Ui.TEXT);
        Ui.text(fr(), "Enter = save   Esc = cancel", x + 10, y + 52, Ui.DISABLED_TEXT);
    }

    // --- input ----------------------------------------------------------

    @Override
    protected void mouseClicked(int rawMx, int rawMy, int button) throws IOException {
        int mx = mouseUiX(), my = mouseUiY();
        if (naming) return; // modal: swallow clicks
        if (button != 0) return;

        searchFocused = false;

        int[] cb = closeBounds();
        if (Ui.inside(mx, my, cb[0], cb[1], cb[2], cb[3])) { closeMenu(); return; }

        for (int i = 0; i < 2; i++) {
            int[] b = tabBounds(i);
            if (Ui.inside(mx, my, b[0], b[1], b[2], b[3])) { tab = i == 0 ? Tab.MODS : Tab.SETTINGS; return; }
        }

        int px = sx();
        int maxRows = maxProfileRows();
        for (int i = 0; i < profiles.size() && i < maxRows; i++) {
            int y = profileRowY(i);
            if (!Ui.inside(mx, my, px + 6, y, SIDEBAR_W - 12, 17)) continue;
            String name = profiles.get(i);
            boolean deletable = !ConfigStorage.DEFAULT_PROFILE.equals(name);
            if (deletable && Ui.inside(mx, my, px + SIDEBAR_W - 20, y, 14, 17)) {
                ConfigStorage.deleteProfile(name);
            } else {
                ConfigStorage.applyProfile(name);
            }
            profiles = ConfigStorage.listProfiles();
            return;
        }
        int[] sp = saveProfileBounds();
        if (Ui.inside(mx, my, sp[0], sp[1], sp[2], sp[3])) { naming = true; nameBuffer = ""; return; }
        int[] hb = hudLayoutBounds();
        if (Ui.inside(mx, my, hb[0], hb[1], hb[2], hb[3])) {
            this.mc.displayGuiScreen(new HudLayoutScreen(this));
            return;
        }
        if (tab == Tab.SETTINGS) {
            for (SliderControl s : settingSliders) if (s.mouseClicked(mx, my)) return;
            return;
        }

        for (int i = 0; i < CATEGORIES.length; i++) {
            int[] b = pillBounds(i);
            if (Ui.inside(mx, my, b[0], b[1], b[2], b[3])) { category = CATEGORIES[i]; scroll = 0; return; }
        }
        int[] sb = searchBounds();
        if (Ui.inside(mx, my, sb[0], sb[1], sb[2], sb[3])) { searchFocused = true; return; }

        int gx = contentX(), top = gridTop(), bottom = gridBottom();
        if (mx >= gx && mx < gx + contentW() && my >= top && my < bottom) {
            List<ModEntry> list = visibleMods();
            int cw = cardW();
            for (int i = 0; i < list.size(); i++) {
                int x = gx + (i % columns()) * (cw + GAP);
                int y = top + (i / columns()) * (CARD_H + GAP) - (int) scroll;
                ModEntry m = list.get(i);
                if (Ui.inside(mx, my, x + 6, y + CARD_H - 40, cw - 12, 16)) {
                    if (m.options != null) this.mc.displayGuiScreen(m.options.apply(this));
                    return;
                }
                if (Ui.inside(mx, my, x, y + CARD_H - 20, cw, 20)) {
                    m.setEnabled.accept(!m.enabled.get());
                    ConfigStorage.save();
                    return;
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int rawMx, int rawMy, int button, long time) {
        if (tab == Tab.SETTINGS) for (SliderControl s : settingSliders) if (s.mouseDragged(mouseUiX())) return;
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        boolean saved = false;
        for (SliderControl s : settingSliders) saved |= s.mouseReleased();
        if (saved) ConfigStorage.save();
        super.mouseReleased(mx, my, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && tab == Tab.MODS && !naming) {
            scroll -= Math.signum(wheel) * 20;
            scroll = Math.max(0, Math.min(scroll, maxScroll(visibleMods().size())));
        }
    }

    @Override
    protected void keyTyped(char typed, int keyCode) throws IOException {
        if (naming) {
            if (keyCode == Keyboard.KEY_ESCAPE) { naming = false; return; }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                if (ConfigStorage.saveProfileAs(nameBuffer)) {
                    profiles = ConfigStorage.listProfiles();
                    naming = false;
                }
                return;
            }
            if (keyCode == Keyboard.KEY_BACK) {
                if (!nameBuffer.isEmpty()) nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
                return;
            }
            if (nameBuffer.length() < 24 && String.valueOf(typed).matches("[A-Za-z0-9 _-]")) nameBuffer += typed;
            return;
        }
        if (searchFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) { searchFocused = false; return; }
            if (keyCode == Keyboard.KEY_BACK) {
                if (!search.isEmpty()) { search = search.substring(0, search.length() - 1); scroll = 0; }
                return;
            }
            if (search.length() < 20 && typed >= 32 && typed != 127) { search += typed; scroll = 0; }
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) { closeMenu(); return; }
        super.keyTyped(typed, keyCode);
    }

    private void closeMenu() {
        ConfigStorage.save();
        this.mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}