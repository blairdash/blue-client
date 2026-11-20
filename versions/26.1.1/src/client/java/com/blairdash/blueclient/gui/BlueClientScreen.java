package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Right Shift menu: a card grid of mods (Options + Enabled/Disabled),
 * category filters and search, a profiles sidebar, a Settings tab, and a
 * button to open the drag-to-move HUD layout editor.
 */
public class BlueClientScreen extends Screen {
    private enum Tab { MODS, SETTINGS }

    private String[] CATEGORIES = { "ALL", ModRegistry.HUD, ModRegistry.MECHANIC, ModRegistry.VISUAL }; // rebuilt in init() to include extension tabs

    private static final int HEADER_H = 32;
    private static final int RAIL_W = 44;
    private static final int SIDEBAR_W = 128;
    private static final int CARD_H = 92;
    private static final int GAP = 8;

    private Tab tab = Tab.MODS;
    private String category = "ALL";
    private String search = "";
    private boolean searchFocused;

    private final List<ModEntry> mods = new ArrayList<>();
    private List<String> profiles = new ArrayList<>();
    private double scroll;

    // "Save as new profile" name prompt
    private boolean naming;
    private String nameBuffer = "";

    private final List<SliderControl> settingSliders = new ArrayList<>();

    public BlueClientScreen() {
        super(Component.literal("BLUE CLIENT"));
    }

    @Override
    protected void init() {
        mods.clear();
        mods.addAll(ModRegistry.create());
        CATEGORIES = ModRegistry.categories();
        profiles = CrosshairConfigStorage.listProfiles();

        settingSliders.clear();
        settingSliders.add(new SliderControl("Zoom FOV (hold C, scroll to adjust)", 4, 60, 0,
                () -> ModConfig.zoomFov, v -> ModConfig.zoomFov = v));
        settingSliders.add(new SliderControl("Low Fire offset (larger = lower)", 0.0, 0.8, 2,
                () -> (double) ModConfig.lowFireOffset, v -> ModConfig.lowFireOffset = v.floatValue()));
        settingSliders.add(new SliderControl("Low Shield offset (larger = lower)", 0.0, 0.8, 2,
                () -> (double) ModConfig.lowShieldOffset, v -> ModConfig.lowShieldOffset = v.floatValue()));
    }

    // --- layout ---------------------------------------------------------

    // The menu is laid out in "UI units" of S physical pixels (not GUI-scale units), so it stays
    // sharp at any GUI scale instead of being blown up 3-4x like Minecraft's own widgets.
    private int gs() { return Math.max(1, minecraft.getWindow().getGuiScale()); }
    private int uiScale() { return Math.max(1, Math.round(minecraft.getWindow().getHeight() / 540f)); }
    private int uw() { return minecraft.getWindow().getWidth() / uiScale(); }
    private int uh() { return minecraft.getWindow().getHeight() / uiScale(); }
    private double toUi(double guiCoord) { return guiCoord * gs() / uiScale(); }

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
        List<ModEntry> out = new ArrayList<>();
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

    // pill / button geometry shared by render + click
    private int[] pillBounds(int index) {
        int x = contentX();
        for (int i = 0; i < index; i++) x += Ui.width(font, CATEGORIES[i]) + 14 + 4;
        return new int[] { x, bodyTop() + 6, Ui.width(font, CATEGORIES[index]) + 14, 16 };
    }

    private int[] searchBounds() {
        int w = 96;
        return new int[] { contentX() + contentW() - w, bodyTop() + 6, w, 16 };
    }

    private int[] saveProfileBounds() { return new int[] { sx() + 8, py() + ph() - 46, SIDEBAR_W - 16, 16 }; }
    private int[] hudLayoutBounds() { return new int[] { sx() + 8, py() + ph() - 26, SIDEBAR_W - 16, 18 }; }

    /** Icon rail button i (0 = Mods, 1 = Settings), like the launcher's left rail. */
    private int[] tabBounds(int i) {
        return new int[] { px() + 6, bodyTop() + 8 + i * 38, 32, 32 };
    }

    private int[] closeBounds() { return new int[] { px() + pw() - 26, py() + 7, 18, 18 }; }

    private int profileRowY(int i) { return bodyTop() + 22 + i * 19; }

    // --- render ---------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor c, int rawMx, int rawMy, float delta) {
        super.extractRenderState(c, rawMx, rawMy, delta);
        c.fill(0, 0, width, height, Ui.DIM);

        int uiS = uiScale();
        float k = uiS / (float) gs();
        int mx = (int) toUi(rawMx), my = (int) toUi(rawMy);
        Ui.res = uiS;
        c.pose().pushMatrix();
        c.pose().scale(k, k);

        int px = px(), py = py(), pw = pw(), ph = ph();
        Ui.card(c, px - 1, py - 1, pw + 2, ph + 2, 14, Ui.PANEL, Ui.PANEL_EDGE);

        renderHeader(c, mx, my);
        renderRail(c, mx, my);
        renderSidebar(c, mx, my);

        if (tab == Tab.MODS) renderMods(c, mx, my);
        else renderSettings(c, mx, my);

        if (naming) renderNamePrompt(c, mx, my);

        c.pose().popMatrix();
        Ui.res = 1;
    }

    private void renderHeader(GuiGraphicsExtractor c, int mx, int my) {
        int px = px(), py = py(), pw = pw();
        // title bar: same as the launcher's (dark strip, accent square + letter-spaced brand, line below)
        Ui.rrect(c, px, py, pw, HEADER_H, 13, Ui.HEADER);
        c.fill(px, py + 14, px + pw, py + HEADER_H, Ui.HEADER);
        c.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, Ui.PANEL_EDGE);
        Ui.logo(c, px + 14, py + 9, 14);
        Ui.spaced(c, font, "BLUE CLIENT", px + 34, py + 12, Ui.MUTED, 0);

        int[] cb = closeBounds();
        boolean hover = Ui.inside(mx, my, cb[0], cb[1], cb[2], cb[3]);
        Ui.rrect(c, cb[0], cb[1], cb[2], cb[3], 9, hover ? 0xFFD9435A : 0x00000000);
        Ui.label(c, font, "X", cb[0], cb[1], cb[2], cb[3], hover ? 0xFFFFFFFF : Ui.MUTED);
    }

    private void renderRail(GuiGraphicsExtractor c, int mx, int my) {
        int px = px(), bt = bodyTop(), bottom = py() + ph();
        // rounded bottom-left to follow the panel; sidebar (drawn next) covers the extra width
        Ui.rrect(c, px, bt, RAIL_W + 14, bottom - bt, 13, Ui.HEADER);
        c.fill(px, bt, px + RAIL_W + 14, bt + 6, Ui.HEADER);
        c.fill(px + RAIL_W - 1, bt, px + RAIL_W, bottom, Ui.PANEL_EDGE);

        net.minecraft.world.item.ItemStack[] icons = {
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPARATOR) };
        String[] names = { "Mods", "Settings" };
        String tip = null; int[] tipAt = null;
        for (int i = 0; i < 2; i++) {
            int[] b = tabBounds(i);
            boolean active = (i == 0) == (tab == Tab.MODS);
            boolean hover = Ui.inside(mx, my, b[0], b[1], b[2], b[3]);
            if (active) {
                Ui.rrect(c, b[0], b[1], b[2], b[3], 16, Ui.SELECTED);
                c.fill(px, b[1] + 7, px + 2, b[1] + b[3] - 7, Ui.ACCENT);
            } else if (hover) {
                Ui.rrect(c, b[0], b[1], b[2], b[3], 16, Ui.CARD);
            }
            c.item(icons[i], b[0] + 8, b[1] + 8);
            if (hover) { tip = names[i]; tipAt = b; }
        }
        if (tip != null) {
            int tw = Ui.width(font, tip) + 12;
            int tx = tipAt[0] + tipAt[2] + 10, ty = tipAt[1] + 8;
            Ui.card(c, tx, ty, tw, 16, 8, Ui.CARD, Ui.PANEL_EDGE);
            Ui.text(c, font, tip, tx + 6, ty + 4, Ui.TEXT);
        }
    }

    private void renderSidebar(GuiGraphicsExtractor c, int mx, int my) {
        int px = sx(), bt = bodyTop(), bottom = py() + ph();
        c.fill(px, bt, px + SIDEBAR_W, bottom, Ui.SIDEBAR);
        c.fill(px + SIDEBAR_W - 1, bt, px + SIDEBAR_W, bottom, Ui.PANEL_EDGE);
        Ui.spaced(c, font, "PROFILES", px + 10, bt + 8, Ui.MUTED, 0);

        String active = CrosshairConfigStorage.getActiveProfile();
        int maxRows = Math.max(1, (py() + ph() - 56 - (bt + 22)) / 19);
        for (int i = 0; i < profiles.size() && i < maxRows; i++) {
            String name = profiles.get(i);
            int y = profileRowY(i);
            boolean isActive = name.equals(active);
            boolean hover = Ui.inside(mx, my, px + 6, y, SIDEBAR_W - 12, 17);
            Ui.rrect(c, px + 6, y, SIDEBAR_W - 13, 17, 8, isActive ? Ui.SELECTED : (hover ? Ui.CARD_HOVER : Ui.CARD));
            if (isActive) c.fill(px + 6, y + 4, px + 8, y + 13, Ui.ACCENT);
            boolean deletable = !CrosshairConfigStorage.DEFAULT_PROFILE.equals(name);
            int textMax = SIDEBAR_W - 12 - 14 - (deletable ? 14 : 0);
            Ui.text(c, font, Ui.clip(font, name, textMax), px + 14, y + 5, isActive ? 0xFFFFFFFF : Ui.TEXT);
            if (deletable) {
                int dx = px + SIDEBAR_W - 6 - 14;
                boolean dh = Ui.inside(mx, my, dx, y, 14, 17);
                Ui.text(c, font, "x", dx + 4, y + 5, dh ? Ui.BAD_HOVER : Ui.MUTED);
            }
        }

        int[] sp = saveProfileBounds();
        boolean h1 = Ui.inside(mx, my, sp[0], sp[1], sp[2], sp[3]);
        Ui.card(c, sp[0], sp[1], sp[2], sp[3], 8, h1 ? Ui.CARD_HOVER : Ui.CARD, Ui.PANEL_EDGE);
        Ui.label(c, font, "SAVE AS NEW PROFILE", sp[0], sp[1], sp[2], sp[3], Ui.MUTED);

        int[] hb = hudLayoutBounds();
        boolean h2 = Ui.inside(mx, my, hb[0], hb[1], hb[2], hb[3]);
        Ui.rrect(c, hb[0], hb[1], hb[2], hb[3], 9, h2 ? Ui.ACCENT : Ui.ACCENT_DARK);
        Ui.label(c, font, "EDIT HUD LAYOUT", hb[0], hb[1], hb[2], hb[3], 0xFFFFFFFF);
    }

    private void renderMods(GuiGraphicsExtractor c, int mx, int my) {
        // category pills
        for (int i = 0; i < CATEGORIES.length; i++) {
            int[] b = pillBounds(i);
            boolean active = CATEGORIES[i].equals(category);
            boolean hover = Ui.inside(mx, my, b[0], b[1], b[2], b[3]);
            Ui.rrect(c, b[0], b[1], b[2], b[3], 8, active ? Ui.ACCENT : (hover ? Ui.CARD_HOVER : Ui.CARD));
            Ui.label(c, font, CATEGORIES[i], b[0], b[1], b[2], b[3], active ? 0xFFFFFFFF : Ui.MUTED);
        }
        // search box
        int[] sb = searchBounds();
        Ui.card(c, sb[0], sb[1], sb[2], sb[3], 8, Ui.FIELD, searchFocused ? Ui.ACCENT : Ui.PANEL_EDGE);
        String shown = search.isEmpty() && !searchFocused ? "Search..." : search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        Ui.text(c, font, Ui.clip(font, shown, sb[2] - 10), sb[0] + 5, sb[1] + 4,
                search.isEmpty() && !searchFocused ? Ui.DISABLED_TEXT : Ui.TEXT);

        // card grid (scrollable)
        List<ModEntry> list = visibleMods();
        scroll = Math.max(0, Math.min(scroll, maxScroll(list.size())));
        int gx = contentX(), top = gridTop(), bottom = gridBottom();
        c.enableScissor(gx, top, gx + contentW(), bottom);
        int cw = cardW();
        for (int i = 0; i < list.size(); i++) {
            int col = i % columns(), row = i / columns();
            int x = gx + col * (cw + GAP);
            int y = top + row * (CARD_H + GAP) - (int) scroll;
            if (y + CARD_H < top || y > bottom) continue;
            renderCard(c, list.get(i), x, y, cw, mx, my, mx >= gx && mx < gx + contentW() && my >= top && my < bottom);
        }
        if (list.isEmpty()) {
            Ui.centered(c, font, "No mods match.", gx + contentW() / 2, top + 20, Ui.MUTED);
        }
        c.disableScissor();

        // scrollbar
        double max = maxScroll(list.size());
        if (max > 0) {
            int trackH = bottom - top;
            int thumbH = Math.max(18, (int) (trackH * (trackH / (double) contentHeight(list.size()))));
            int thumbY = top + (int) ((trackH - thumbH) * (scroll / max));
            c.fill(gx + contentW() + 2, top, gx + contentW() + 4, bottom, Ui.FIELD);
            c.fill(gx + contentW() + 2, thumbY, gx + contentW() + 4, thumbY + thumbH, Ui.MUTED);
        }
    }

    private void renderCard(GuiGraphicsExtractor c, ModEntry m, int x, int y, int w, int mx, int my, boolean mouseInGrid) {
        boolean hoverCard = mouseInGrid && Ui.inside(mx, my, x, y, w, CARD_H);
        boolean on = m.enabled.get();
        Ui.card(c, x, y, w, CARD_H, 14, hoverCard ? Ui.CARD_HOVER : Ui.CARD, on ? 0xFF24406F : Ui.PANEL_EDGE);

        // icon tile
        Ui.card(c, x + w / 2 - 16, y + 6, 32, 32, 16, Ui.FIELD, Ui.PANEL_EDGE);
        c.item(m.icon, x + w / 2 - 8, y + 14);

        // name
        Ui.centered(c, font, Ui.clip(font, m.name, w - 8), x + w / 2, y + 41, Ui.TEXT);

        // options button
        int ox = x + 6, oy = y + CARD_H - 40, ow = w - 12, oh = 16;
        boolean hasOptions = m.options != null;
        boolean hoverOpt = mouseInGrid && hasOptions && Ui.inside(mx, my, ox, oy, ow, oh);
        Ui.card(c, ox, oy, ow, oh, 8, hoverOpt ? Ui.SELECTED : Ui.FIELD, hoverOpt ? Ui.ACCENT : Ui.PANEL_EDGE);
        Ui.label(c, font, "OPTIONS", ox, oy, ow, oh, hasOptions ? (hoverOpt ? Ui.ACCENT : Ui.TEXT) : Ui.DISABLED_TEXT);

        // enable row: status text + launcher-style toggle switch
        int ey = y + CARD_H - 20, eh = 20;
        boolean hoverBar = mouseInGrid && Ui.inside(mx, my, x, ey, w, eh);
        Ui.text(c, font, on ? "ENABLED" : "DISABLED", x + 8, ey + 6, on ? Ui.GOOD : Ui.MUTED);
        Ui.toggle(c, x + w - 26 - 8, ey + 4, on, hoverBar);
    }

    private void renderSettings(GuiGraphicsExtractor c, int mx, int my) {
        int x = contentX(), y = bodyTop() + 12, w = contentW() - 6;
        Ui.spaced(c, font, "SETTINGS", x, y, Ui.MUTED, 0);
        y += 18;
        boolean guiHov = Ui.inside(mx, my, x, y, w, 18);
        Ui.text(c, font, "Use Minecraft GUI (vanilla look instead of the Blue Client look)", x, y + 5, Ui.TEXT);
        Ui.toggle(c, x + w - 26, y + 3, !BlueMenuConfig.blueMenu(), guiHov);
        y += 26;
        for (SliderControl s : settingSliders) {
            s.place(x, y, w);
            s.render(c, font, mx, my);
            y += SliderControl.HEIGHT + 12;
        }
        y += 4;
        Ui.text(c, font, "Menu: Right Shift     Zoom: hold C (scroll while zoomed)", x, y, Ui.MUTED);
        Ui.text(c, font, "Settings save automatically to the active profile.", x, y + 12, Ui.MUTED);
    }

    private void renderNamePrompt(GuiGraphicsExtractor c, int mx, int my) {
        c.fill(0, 0, uw(), uh(), 0x99000000);
        int w = 240, h = 74, x = (uw() - w) / 2, y = (uh() - h) / 2;
        Ui.card(c, x - 1, y - 1, w + 2, h + 2, 14, 0xFF111624, Ui.PANEL_EDGE);
        Ui.spaced(c, font, "NEW PROFILE NAME", x + 10, y + 9, Ui.MUTED, 0);
        Ui.card(c, x + 10, y + 24, w - 20, 18, 9, Ui.FIELD, Ui.ACCENT);
        String shown = nameBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        Ui.text(c, font, shown, x + 15, y + 29, Ui.TEXT);
        Ui.text(c, font, "Enter = save   Esc = cancel", x + 10, y + 52, Ui.DISABLED_TEXT);
    }

    // --- input ----------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = toUi(click.x()), my = toUi(click.y());
        int button = click.button();
        if (naming) return true; // modal: swallow clicks
        if (button != 0) return super.mouseClicked(click, doubled);

        searchFocused = false;

        int[] cb = closeBounds();
        if (Ui.inside(mx, my, cb[0], cb[1], cb[2], cb[3])) { onClose(); return true; }

        for (int i = 0; i < 2; i++) {
            int[] b = tabBounds(i);
            if (Ui.inside(mx, my, b[0], b[1], b[2], b[3])) { tab = i == 0 ? Tab.MODS : Tab.SETTINGS; return true; }
        }

        // sidebar
        int px = sx();
        int maxRows = Math.max(1, (py() + ph() - 56 - (bodyTop() + 22)) / 19);
        for (int i = 0; i < profiles.size() && i < maxRows; i++) {
            int y = profileRowY(i);
            if (!Ui.inside(mx, my, px + 6, y, SIDEBAR_W - 12, 17)) continue;
            String name = profiles.get(i);
            boolean deletable = !CrosshairConfigStorage.DEFAULT_PROFILE.equals(name);
            if (deletable && Ui.inside(mx, my, px + SIDEBAR_W - 20, y, 14, 17)) {
                CrosshairConfigStorage.deleteProfile(name);
            } else {
                CrosshairConfigStorage.applyProfile(name);
            }
            profiles = CrosshairConfigStorage.listProfiles();
            return true;
        }
        int[] sp = saveProfileBounds();
        if (Ui.inside(mx, my, sp[0], sp[1], sp[2], sp[3])) { naming = true; nameBuffer = ""; return true; }
        int[] hb = hudLayoutBounds();
        if (Ui.inside(mx, my, hb[0], hb[1], hb[2], hb[3])) {
            Minecraft.getInstance().setScreen(new HudLayoutScreen(this));
            return true;
        }

        if (tab == Tab.SETTINGS) {
            if (Ui.inside(mx, my, contentX(), bodyTop() + 30, contentW() - 6, 18)) { BlueMenuConfig.setBlueMenu(false); CrosshairConfigStorage.save(); ScreenNav.open(new VanillaModsScreen(null)); return true; }
            for (SliderControl s : settingSliders) if (s.mouseClicked(mx, my)) return true;
            return super.mouseClicked(click, doubled);
        }

        // mods tab
        for (int i = 0; i < CATEGORIES.length; i++) {
            int[] b = pillBounds(i);
            if (Ui.inside(mx, my, b[0], b[1], b[2], b[3])) { category = CATEGORIES[i]; scroll = 0; return true; }
        }
        int[] sb = searchBounds();
        if (Ui.inside(mx, my, sb[0], sb[1], sb[2], sb[3])) { searchFocused = true; return true; }

        int gx = contentX(), top = gridTop(), bottom = gridBottom();
        if (mx >= gx && mx < gx + contentW() && my >= top && my < bottom) {
            List<ModEntry> list = visibleMods();
            int cw = cardW();
            for (int i = 0; i < list.size(); i++) {
                int x = gx + (i % columns()) * (cw + GAP);
                int y = top + (i / columns()) * (CARD_H + GAP) - (int) scroll;
                ModEntry m = list.get(i);
                if (Ui.inside(mx, my, x + 6, y + CARD_H - 40, cw - 12, 16)) {
                    if (m.options != null) Minecraft.getInstance().setScreen(m.options.apply(this));
                    return true;
                }
                if (Ui.inside(mx, my, x, y + CARD_H - 20, cw, 20)) {
                    m.setEnabled.accept(!m.enabled.get());
                    CrosshairConfigStorage.save();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (tab == Tab.SETTINGS) for (SliderControl s : settingSliders) if (s.mouseDragged(toUi(click.x()))) return true;
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        boolean saved = false;
        for (SliderControl s : settingSliders) saved |= s.mouseReleased();
        if (saved) CrosshairConfigStorage.save();
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (tab == Tab.MODS && !naming) {
            scroll -= vertical * 20;
            scroll = Math.max(0, Math.min(scroll, maxScroll(visibleMods().size())));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (naming) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { naming = false; return true; }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (CrosshairConfigStorage.saveProfileAs(nameBuffer)) {
                    profiles = CrosshairConfigStorage.listProfiles();
                    naming = false;
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !nameBuffer.isEmpty()) {
                nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
            }
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                scroll = 0;
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        String s = new String(Character.toChars(input.codepoint()));
        if (naming) {
            if (nameBuffer.length() < 24 && s.matches("[A-Za-z0-9 _-]")) nameBuffer += s;
            return true;
        }
        if (searchFocused) {
            if (search.length() < 20 && !Character.isISOControl(input.codepoint())) { search += s; scroll = 0; }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        CrosshairConfigStorage.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
