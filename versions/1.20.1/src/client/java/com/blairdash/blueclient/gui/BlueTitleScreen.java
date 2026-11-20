package com.blairdash.blueclient.gui;

import net.minecraft.client.MinecraftClient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Blue-branded main menu: centered button stack, secondary row, account top-left. */
public class BlueTitleScreen extends Screen {
    private static final net.minecraft.util.Identifier LOGO = net.minecraft.util.Identifier.of("clientmod", "textures/gui/logo.png");
    private static final String SMP_HOST = "blue-smp.com";
    private static volatile String smpStatus = "checking...";
    private static volatile boolean smpUp;

    private static final class Btn {
        final String label; final Runnable action; final boolean primary;
        int x, y, w, h;
        Btn(String l, boolean p, Runnable a) { label = l; primary = p; action = a; }
    }

    private boolean acctOpen;
    private String acctNote = "";
    private AccountManager.Login acctLogin;
    private boolean acctOpened;
    private volatile AccountManager.Session acctPending;
    private volatile String acctError;
        private static final java.util.Map<String, String> ICONS = java.util.Map.of(
        "Singleplayer", "user", "Multiplayer", "users", "Join Blue SMP", "server",
        "Mods", "puzzle", "Options", "settings", "Blue Client", "info", "Quit", "logout");
    private final List<Btn> buttons = new ArrayList<>();
    private final net.minecraft.client.gui.RotatingCubeMapRenderer panorama = new net.minecraft.client.gui.RotatingCubeMapRenderer(net.minecraft.client.gui.screen.TitleScreen.PANORAMA_CUBE_MAP);
    private Btn smpBtn;

    public BlueTitleScreen() { super(Text.literal("Blue Client")); }

    @Override
    protected void init() {
        buttons.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean cp = compact(); int bw = cp ? 200 : 220, bh = cp ? 18 : 24, gap = cp ? 4 : 5;
        int cx = uw() / 2;
        int y = firstY();
        add(new Btn("Singleplayer", false, () -> mc.setScreen(new SelectWorldScreen(this))), cx - bw / 2, y, bw, bh);
        y += bh + gap;
        add(new Btn("Multiplayer", false, () -> mc.setScreen(new MultiplayerScreen(this))), cx - bw / 2, y, bw, bh);
        y += bh + gap;
        smpBtn = new Btn("Join Blue SMP", true, () -> ConnectScreen.connect(this, mc, ServerAddress.parse(SMP_HOST),
                new ServerInfo("Blue SMP", SMP_HOST, ServerInfo.ServerType.OTHER), false, null));
        add(smpBtn, cx - bw / 2, y, bw, bh);
        y += bh + gap;
        int hw = (bw - gap) / 2;
        add(new Btn("Mods", false, () -> mc.setScreen(new BlueModsScreen(this))), cx - bw / 2, y, hw, bh);
        add(new Btn("Options", false, () -> mc.setScreen(new OptionsScreen(this, mc.options))), cx - bw / 2 + hw + gap, y, hw, bh);
        y += bh + gap;
        add(new Btn("Blue Client", false, () -> mc.setScreen(new BlueClientScreen())), cx - bw / 2, y, hw, bh);
        add(new Btn("Quit", false, mc::scheduleStop), cx - bw / 2 + hw + gap, y, hw, bh);
        pingSmp();
    }

    private java.util.function.Supplier<net.minecraft.client.util.SkinTextures> headSkin;
    private void drawHead(DrawContext c, int x, int y, int size) {
        try {
            if (headSkin == null) headSkin = () -> client.getSkinProvider().getSkinTextures(SessionSwap.profile(client.getGameProfile()));
            net.minecraft.client.gui.PlayerSkinDrawer.draw(c, headSkin.get(), x, y, size);
        } catch (Throwable t) {
            Ui.rrect(c, x, y, size, size, 3, Ui.ACCENT);
        }
    }

    private int gs() { return (int) Math.max(1, Math.round(client.getWindow().getScaleFactor())); }
    private int uiScale() {
        int fh = client.getWindow().getFramebufferHeight();
        int s = Math.max(1, Math.round(fh / 540f));
        return (s < 2 && fh >= 440) ? 2 : s; // small windows still get readable text; the layout turns compact
    }
    private boolean compact() { return uh() < 290; }
    private int firstY() { return compact() ? (uh() - 176) / 2 + 70 : uh() / 2 - 24; }
    private int uw() { return client.getWindow().getFramebufferWidth() / uiScale(); }
    private int uh() { return client.getWindow().getFramebufferHeight() / uiScale(); }
    private double toUi(double guiCoord) { return guiCoord * gs() / uiScale(); }

    private void add(Btn b, int x, int y, int w, int h) { b.x = x; b.y = y; b.w = w; b.h = h; buttons.add(b); }

    @Override
    public void render(DrawContext c, int rawMx, int rawMy, float delta) {
        if (BlueMenuConfig.theme() == 0) { panorama.render(delta, 1.0F); MenuBlur.apply(client, 5); }
        super.render(c, rawMx, rawMy, delta);
        int mx = (int) toUi(rawMx), my = (int) toUi(rawMy);
        int uiS = uiScale();
        float k = uiS / (float) gs();
        Ui.res = uiS; Ui.unitPx = uiS;
        c.getMatrices().push();
        c.getMatrices().scale(k, k, 1f);
        if (BlueMenuConfig.theme() == 0) c.fill(0, 0, uw() + 4, uh() + 4, 0x55070A12);
        else paintTheme(c, BlueMenuConfig.theme(), 0, 0, uw(), uh());
        int cx = uw() / 2;
        int top = firstY();

        // logo (Blue Client app icon) + wordmark
        boolean cp = compact();
        int ls = cp ? 36 : 56, lx = cx - ls / 2, ly = top - ls - (cp ? 34 : 49); // logo, gap, 21 wordmark, gap, buttons
        int li = Math.round(ls * 20f / 256f), vx = lx + li, vy = ly + li, vs = ls - 2 * li; // the artwork is inset in its texture
        { // soft shadow: one layer per screen pixel instead of per layout unit, so it fades smoothly instead of in visible steps
            int g = Math.max(1, Ui.res), saveRes = Ui.res;
            Ui.res = 1;
            c.getMatrices().push();
            c.getMatrices().scale(1f / g, 1f / g, 1f);
            int n = 9 * g; float peak = 120f;
            for (int i = n; i >= 1; i--) {
                float t0 = 1f - (i - 1f) / n, t1 = 1f - (float) i / n;
                int al = Math.round(peak * t0 * t0 * t0) - Math.round(peak * t1 * t1 * t1);
                if (al <= 0) continue;
                Ui.rrect(c, vx * g - i, vy * g + 5 * g - i, vs * g + 2 * i, vs * g + 2 * i, Math.round(vs * 0.27f * g) + i, al << 24);
            }
            c.getMatrices().pop();
            Ui.res = saveRes;
        }
        client.getTextureManager().getTexture(LOGO).setFilter(true, false); // smooth edges at any scale
        c.drawTexture(LOGO, lx, ly, ls, ls, 0f, 0f, 256, 256, 256, 256);
        String name = "BLUE CLIENT";
        int wx = cx - Ui.brandWidth(textRenderer, name) / 2, wy = ly + ls + (cp ? 3 : 6);
        Ui.brand(c, textRenderer, name, wx + 1, wy + 2, 0x55000000);
        Ui.brand(c, textRenderer, name, wx, wy + 1, 0x66000000);
        Ui.brand(c, textRenderer, name, cx - Ui.brandWidth(textRenderer, name) / 2, ly + ls + (cp ? 3 : 6), Ui.TEXT);

        for (Btn b : buttons) {
            boolean hov = Ui.inside(mx, my, b.x, b.y, b.w, b.h);
            int fill = b.primary ? (hov ? Ui.ACCENT : Ui.ACCENT_DARK) : (hov ? Ui.CARD_HOVER : Ui.CARD);
            Ui.card(c, b.x, b.y, b.w, b.h, 6, fill, b.primary ? Ui.ACCENT : (hov ? Ui.ACCENT : Ui.PANEL_EDGE));
            String icn = ICONS.get(b.label);
            if (icn != null) {
                int is = 12, gp = 6, tw = Ui.width(textRenderer, b.label);
                int sx = b.x + (b.w - (is + gp + tw)) / 2;
                if (b == smpBtn) { // in small windows keep the label clear of the "N online" status on the right
                    int statusStart = b.x + b.w - Ui.width(textRenderer, smpStatus) - 18;
                    sx = Math.max(b.x + 6, Math.min(sx, statusStart - 3 - (is + gp + tw)));
                }
                Ui.icon(c, icn, sx, b.y + (b.h - is) / 2, is);
                Ui.text(c, textRenderer, b.label, sx + is + gp, b.y + (b.h - 8) / 2, Ui.TEXT);
            } else {
                Ui.label(c, textRenderer, b.label, b.x, b.y, b.w, b.h, Ui.TEXT);
            }
        }
        if (smpBtn != null) {
            String s = smpStatus;
            int tw = Ui.width(textRenderer, s);
            int dot = smpUp ? Ui.GOOD : Ui.BAD;
            Ui.rrect(c, smpBtn.x + smpBtn.w - tw - 18, smpBtn.y + smpBtn.h / 2 - 2, 4, 4, 2, dot);
            Ui.text(c, textRenderer, s, smpBtn.x + smpBtn.w - tw - 8, smpBtn.y + (smpBtn.h - 8) / 2, 0xFFE8ECF8);
        }

        // apply an account switch that finished on a worker thread

        if (acctPending != null) { AccountManager.Session ps = acctPending; acctPending = null; try { SessionSwap.apply(ps); headSkin = null; acctNote = "Now playing as " + ps.username; } catch (Throwable ex) { acctNote = "Could not switch: " + ex.getClass().getSimpleName(); } }

        if (acctError != null) { acctNote = acctError; acctError = null; }

        // account chip, top-left
        String user = client.getSession().getUsername();
        Integer rankCol = com.blairdash.blueclient.net.RankService.selfColor();
        int ic = rankCol == null ? 0 : RankIcon.WIDTH + 3;
        int cw = ic + Ui.width(textRenderer, user) + 50;
        Ui.card(c, 8, 8, cw, 22, 6, Ui.CARD, Ui.PANEL_EDGE);
        drawHead(c, 13, 13, 12);
        if (rankCol != null) RankIcon.draw(c, textRenderer, 29, 15, rankCol);
        Ui.text(c, textRenderer, user, 30 + ic, 15, Ui.TEXT);
        java.util.List<LauncherAccounts.Account> blueAccts = LauncherAccounts.list();
        Ui.text(c, textRenderer, "v", 8 + cw - 15, 15, Ui.MUTED);
        if (acctOpen) {
            int rows = blueAccts.size() + 1, rw = Math.max(cw, 170), ry = 34, rh = 20;
            Ui.card(c, 8, ry, rw, rows * rh + 8, 6, Ui.PANEL, Ui.PANEL_EDGE);
            for (int i = 0; i < rows; i++) {
                int rowY = ry + 4 + i * rh;
                boolean hov = Ui.inside(mx, my, 12, rowY, rw - 8, rh - 2);
                if (hov) Ui.rrect(c, 12, rowY, rw - 8, rh - 2, 4, Ui.CARD_HOVER);
                if (i == blueAccts.size()) { Ui.text(c, textRenderer, "+ Add account", 20, rowY + 5, Ui.ACCENT); continue; }
                LauncherAccounts.Account a = blueAccts.get(i);
                boolean cur = a.username.equalsIgnoreCase(user);
                Ui.rrect(c, 17, rowY + 4, 8, 8, 2, cur ? Ui.GOOD : Ui.TRACK_OFF);
                Ui.text(c, textRenderer, a.username, 30, rowY + 5, cur ? Ui.TEXT : Ui.MUTED);
                Ui.text(c, textRenderer, "x", 12 + rw - 8 - 12, rowY + 5, Ui.MUTED);
            }
            if (!acctNote.isEmpty()) Ui.text(c, textRenderer, acctNote, 8, ry + rows * rh + 14, Ui.MUTED);
        } else if (acctLogin == null && !acctNote.isEmpty()) {
            Ui.text(c, textRenderer, acctNote, 8, 36, Ui.MUTED);
        }
        if (acctLogin != null) {
            if (acctLogin.finished) {
                if (acctLogin.session != null) acctPending = acctLogin.session; else acctNote = acctLogin.error == null ? "Sign-in failed" : acctLogin.error;
                acctLogin = null; acctOpened = false;
            } else {
                if (!acctOpened && !acctLogin.code.isEmpty()) { acctOpened = true; AccountManager.copy(acctLogin.code); AccountManager.openUrl(acctLogin.uri); }
                Ui.card(c, 8, 34, 250, 64, 6, Ui.PANEL, Ui.PANEL_EDGE);
                Ui.text(c, textRenderer, acctLogin.code.isEmpty() ? "Contacting Microsoft..." : "Enter this code in your browser:", 16, 42, Ui.MUTED);
                Ui.text(c, textRenderer, acctLogin.code, 16, 56, Ui.TEXT);
                Ui.text(c, textRenderer, acctLogin.status, 16, 74, Ui.MUTED);
                Ui.text(c, textRenderer, "Cancel", 8 + 250 - 44, 56, Ui.MUTED);
            }
        }

        Ui.text(c, textRenderer, "Blue Client V1", 8, uh() - 14, Ui.MUTED);
        String copyright = "Copyright Mojang AB. Do not distribute!";
        Ui.text(c, textRenderer, copyright, uw() - 8 - Ui.width(textRenderer, copyright), uh() - 14, Ui.MUTED);

        // theme paintbrush, top-right
        boolean bh = Ui.inside(mx, my, uw() - 30, 8, 22, 22);
        Ui.card(c, uw() - 30, 8, 22, 22, 6, bh ? Ui.CARD_HOVER : Ui.CARD, bh ? Ui.ACCENT : Ui.PANEL_EDGE);
        for (int i = 0; i < 9; i++) c.fill(uw() - 24 + i, 24 - i, uw() - 22 + i, 26 - i, i > 5 ? Ui.ACCENT : Ui.TEXT);
        c.getMatrices().pop();
        Ui.res = 1; Ui.unitPx = 0;
    }

    @Override
    public boolean mouseClicked(double rawX, double rawY, int clickButton) {
        double clickX = toUi(rawX), clickY = toUi(rawY);
        if (clickButton == 0) {
            java.util.List<LauncherAccounts.Account> blueAccts = LauncherAccounts.list();
            {
                int cw0 = (com.blairdash.blueclient.net.RankService.selfColor() == null ? 0 : RankIcon.WIDTH + 3) + Ui.width(textRenderer, client.getSession().getUsername()) + 50;
                if (acctLogin != null) {
                    if (Ui.inside(clickX, clickY, 8 + 250 - 48, 52, 44, 16)) { acctLogin.cancel(); acctLogin = null; acctOpened = false; }
                    return true;
                }
                if (Ui.inside(clickX, clickY, 8, 8, cw0, 22)) { acctOpen = !acctOpen; acctNote = ""; return true; }
                if (acctOpen) {
                    int rw = Math.max(cw0, 170), rh = 20, ry = 34, n = blueAccts.size();
                    for (int i = 0; i <= n; i++) {
                        if (!Ui.inside(clickX, clickY, 12, ry + 4 + i * rh, rw - 8, rh - 2)) continue;
                        if (i == n) { acctLogin = AccountManager.startLogin(); acctOpened = false; acctOpen = false; acctNote = ""; return true; }
                        LauncherAccounts.Account a = blueAccts.get(i);
                        if (clickX >= 12 + rw - 8 - 18) { AccountManager.remove(a.uuid); acctNote = "Removed " + a.username; return true; }
                        acctNote = "Switching to " + a.username + "...";
                        AccountManager.switchTo(a.uuid, (s, err) -> { if (s != null) acctPending = s; else acctError = err; });
                        return true;
                    }
                    acctOpen = false;
                }
            }
        }        if (clickButton == 0) {
            if (Ui.inside(clickX, clickY, uw() - 30, 8, 22, 22)) { client.setScreen(new BlueThemeScreen(this)); return true; }
            for (Btn b : buttons) {
                if (Ui.inside(clickX, clickY, b.x, b.y, b.w, b.h)) { b.action.run(); return true; }
            }
        }
        return super.mouseClicked(rawX, rawY, clickButton);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    /** Paints a theme into a rectangle (theme 0 = panorama: just a dim). */
    public static void paintTheme(DrawContext c, int theme, int x1, int y1, int x2, int y2) {
        switch (theme) {
            case 1 -> c.fillGradient(x1, y1, x2, y2, 0xFF0B0E17, 0xFF1B2A55);
            case 2 -> c.fillGradient(x1, y1, x2, y2, 0xFF0A2540, 0xFF2A6FE0);
            default -> { c.fillGradient(x1, y1, x2, y2, 0xFF1E3A22, 0xFF3C5A8C); }
        }
    }

    private static void pingSmp() {
        Thread t = new Thread(() -> {
            try (Socket s = new Socket()) {
                long t0 = System.nanoTime();
                s.connect(new InetSocketAddress(SMP_HOST, 25565), 3000); s.setSoTimeout(3000);
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                DataInputStream in = new DataInputStream(s.getInputStream());
                ByteArrayOutputStream hs = new ByteArrayOutputStream(); DataOutputStream hd = new DataOutputStream(hs);
                hd.writeByte(0); varInt(hd, 767); byte[] hb = SMP_HOST.getBytes(StandardCharsets.UTF_8);
                varInt(hd, hb.length); hd.write(hb); hd.writeShort(25565); varInt(hd, 1);
                varInt(out, hs.size()); out.write(hs.toByteArray());
                out.writeByte(1); out.writeByte(0); out.flush();
                readVar(in); readVar(in); int len = readVar(in);
                byte[] data = new byte[len]; in.readFully(data);
                int ping = (int) ((System.nanoTime() - t0) / 1_000_000L);
                String json = new String(data, StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("\"online\"\\s*:\\s*(\\d+)").matcher(json);
                smpStatus = (m.find() ? m.group(1) : "?") + " online · " + ping + "ms";
                smpUp = true;
            } catch (Throwable e) { smpStatus = "offline"; smpUp = false; }
        }, "BlueSMP-menu-ping");
        t.setDaemon(true); t.start();
    }

    private static void varInt(DataOutputStream o, int v) throws java.io.IOException {
        while ((v & ~0x7F) != 0) { o.writeByte((v & 0x7F) | 0x80); v >>>= 7; }
        o.writeByte(v);
    }

    private static int readVar(DataInputStream in) throws java.io.IOException {
        int v = 0, p = 0, b;
        do { b = in.readUnsignedByte(); v |= (b & 0x7F) << p; p += 7; } while ((b & 0x80) != 0 && p < 35);
        return v;
    }

    /** Blue Client watermark: bottom-right of every screen, sized in real screen pixels (same scale as the Blue menus), not GUI scale. */
    public static void watermark(net.minecraft.client.gui.DrawContext c, int sw, int sh) {
        var w = net.minecraft.client.MinecraftClient.getInstance().getWindow();
        int fw = w.getFramebufferWidth(), fh = w.getFramebufferHeight();
        int s = Math.max(1, Math.round(fh / 540f));
        if (s < 2 && fh >= 440) s = 2;
        float k = s / (float) Math.max(1, (int) Math.round(w.getScaleFactor()));
        int uw = fw / s, uh = fh / s;
        net.minecraft.client.font.TextRenderer tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        String name = "BLUE CLIENT";
        float wk = 0.9f;
        int logo = 26, gap = 6, pad = 10;
        int tw = Math.round(Ui.brandWidth(tr, name) * wk);
        int x = uw - pad - tw, y = uh - pad - logo;
        Ui.res = s; Ui.unitPx = s;
        c.getMatrices().push();
        c.getMatrices().scale(k, k, 1f);
        Ui.logo(c, x - gap - logo, y, logo);
        Ui.brandSmall(c, tr, name, x, y + 3, 0xF2FFFFFF, wk);
        c.getMatrices().pop();
        Ui.res = 1; Ui.unitPx = 0;
    }
}
