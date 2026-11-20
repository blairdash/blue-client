package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.net.RankService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Blue Client main menu on 1.8.9. Drawn in real screen pixels (about 540 tall) rather than Minecraft
 * GUI pixels, so it looks the same at any GUI scale.
 */
public class BlueTitleScreen extends GuiScreen {
    private static final class Btn {
        final String label; final Runnable action;
        int x, y, w, h;
        Btn(String l, Runnable a) { label = l; action = a; }
    }

    private static final net.minecraft.util.ResourceLocation LOGO = new net.minecraft.util.ResourceLocation("blueclient", "textures/gui/logo.png");
    private static final int LOGO_SIZE = 256;
    private static final String SMP_HOST = "blue-smp.com";
    private static volatile String smpStatus = "checking...";
    private static volatile boolean smpUp;
    private Btn smpBtn;
    private static final java.util.Map<String, String> ICONS = new java.util.HashMap<String, String>();
    static {
        ICONS.put("Singleplayer", "user"); ICONS.put("Multiplayer", "users"); ICONS.put("Join Blue SMP", "server");
        ICONS.put("Options", "settings"); ICONS.put("Blue Client", "info"); ICONS.put("Vanilla menu", "puzzle"); ICONS.put("Quit", "logout");
    }
    private final List<Btn> buttons = new ArrayList<Btn>();
    private boolean acctOpen;
    private String acctNote = "";
    private AccountManager.Login acctLogin;
    private boolean acctOpened;
    private volatile AccountManager.Session acctPending;
    private volatile String acctError;

    private int gs() { return new ScaledResolution(mc).getScaleFactor(); }
    private int uiScale() {
        int fh = mc.displayHeight;
        int s = Math.max(1, Math.round(fh / 540f));
        return (s < 2 && fh >= 440) ? 2 : s; // small windows still get readable text; the layout turns compact
    }
    private boolean compact() { return uh() < 290; }
    private int firstY() { return compact() ? (uh() - 176) / 2 + 70 : uh() / 2 - 24; }
    private int uw() { return mc.displayWidth / uiScale(); }
    private int uh() { return mc.displayHeight / uiScale(); }
    private double toUi(double guiCoord) { return guiCoord * gs() / uiScale(); }

    @Override
    public void initGui() {
        RankService.start();
        buttons.clear();
        boolean cp = compact(); int bw = cp ? 200 : 220, bh = cp ? 18 : 24, gap = cp ? 4 : 5, cx = uw() / 2, y = firstY();
        add("Singleplayer", new Runnable() { public void run() { mc.displayGuiScreen(new GuiSelectWorld(BlueTitleScreen.this)); } }, cx - bw / 2, y, bw, bh);
        y += bh + gap;
        add("Multiplayer", new Runnable() { public void run() { mc.displayGuiScreen(new GuiMultiplayer(BlueTitleScreen.this)); } }, cx - bw / 2, y, bw, bh);
        y += bh + gap;
        smpBtn = new Btn("Join Blue SMP", new Runnable() { public void run() { mc.displayGuiScreen(new GuiConnecting(BlueTitleScreen.this, mc, new ServerData("Blue SMP", SMP_HOST, false))); } });
        smpBtn.x = cx - bw / 2; smpBtn.y = y; smpBtn.w = bw; smpBtn.h = bh; buttons.add(smpBtn);
        y += bh + gap;
        pingSmp();
        int hw = (bw - gap) / 2;
        add("Options", new Runnable() { public void run() { mc.displayGuiScreen(new GuiOptions(BlueTitleScreen.this, mc.gameSettings)); } }, cx - bw / 2, y, hw, bh);
        add("Blue Client", new Runnable() { public void run() { mc.displayGuiScreen(new BlueClientScreen()); } }, cx - bw / 2 + hw + gap, y, hw, bh);
        y += bh + gap;
        add("Vanilla menu", new Runnable() { public void run() { BlueMenuConfig.setBlueMenu(false); mc.displayGuiScreen(new GuiMainMenu()); } }, cx - bw / 2, y, hw, bh);
        add("Quit", new Runnable() { public void run() { mc.shutdown(); } }, cx - bw / 2 + hw + gap, y, hw, bh);
    }

    private void add(String l, Runnable a, int x, int y, int w, int h) {
        Btn b = new Btn(l, a); b.x = x; b.y = y; b.w = w; b.h = h; buttons.add(b);
    }

    static void paintTheme(int theme, int x1, int y1, int x2, int y2) {
        int top, bottom;
        switch (theme) {
            case 1: top = 0xFF0B0E17; bottom = 0xFF1B2A55; break;
            case 2: top = 0xFF0A2540; bottom = 0xFF2A6FE0; break;
            default: top = 0xFF1E3A22; bottom = 0xFF3C5A8C; break;
        }
        Ui.gradient(x1, y1, x2, y2, top, bottom);
    }

    @Override
    public void drawScreen(int rawMx, int rawMy, float pt) {
        int mx = (int) toUi(rawMx), my = (int) toUi(rawMy);
        int uiS = uiScale();
        float k = uiS / (float) gs();
        Ui.res = uiS;
        GlStateManager.pushMatrix();
        GlStateManager.scale(k, k, 1f);
        paintTheme(BlueMenuConfig.theme(), 0, 0, uw() + 1, uh() + 1);
        int cx = uw() / 2, top = firstY();

        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(LOGO);
        boolean cp = compact();
        int ls = cp ? 36 : 56, lx = cx - ls / 2, ly = top - ls - (cp ? 34 : 49);
        { // soft shadow: one layer per screen pixel instead of per layout unit, so it fades smoothly
            int g = Math.max(1, Ui.res), saveRes = Ui.res;
            Ui.res = 1;
            GlStateManager.pushMatrix();
            GlStateManager.scale(1f / g, 1f / g, 1f);
            int n = 9 * g; float peak = 120f;
            for (int i = n; i >= 1; i--) {
                float t0 = 1f - (i - 1f) / n, t1 = 1f - (float) i / n;
                int al = Math.round(peak * t0 * t0 * t0) - Math.round(peak * t1 * t1 * t1);
                if (al <= 0) continue;
                Ui.rrect(lx * g - i, ly * g + 5 * g - i, ls * g + 2 * i, ls * g + 2 * i, ls * g / 5 + i, al << 24);
            }
            GlStateManager.popMatrix();
            Ui.res = saveRes;
        }
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(LOGO);
        drawScaledCustomSizeModalRect(lx, ly, 20f, 20f, 216, 216, ls, ls, LOGO_SIZE, LOGO_SIZE);
        String name = "BLUE CLIENT";
        int wx = cx - Ui.brandWidth(name) / 2, wy = ly + ls + (cp ? 3 : 6);
        Ui.brand(name, wx + 1, wy + 2, 0x55000000);
        Ui.brand(name, wx, wy + 1, 0x66000000);
        Ui.brand(name, wx, wy, Ui.TEXT);

        for (Btn b : buttons) {
            boolean hov = Ui.inside(mx, my, b.x, b.y, b.w, b.h);
            boolean prim = b == smpBtn;
            Ui.card(b.x, b.y, b.w, b.h, 6, prim ? (hov ? Ui.ACCENT : Ui.ACCENT_DARK) : (hov ? Ui.CARD_HOVER : Ui.CARD), prim ? Ui.ACCENT : (hov ? Ui.ACCENT : Ui.PANEL_EDGE));
            String icn = ICONS.get(b.label);
            if (icn != null) {
                int is = 12, gp = 6, tw = Ui.width(b.label);
                int sx = b.x + (b.w - (is + gp + tw)) / 2;
                if (b == smpBtn) { int statusStart = b.x + b.w - Ui.width(smpStatus) - 18; sx = Math.max(b.x + 6, Math.min(sx, statusStart - 3 - (is + gp + tw))); }
                Ui.icon(icn, sx, b.y + (b.h - is) / 2, is);
                Ui.text(fontRendererObj, b.label, sx + is + gp, b.y + (b.h - 8) / 2, Ui.TEXT);
            } else {
                Ui.label(fontRendererObj, b.label, b.x, b.y, b.w, b.h, Ui.TEXT);
            }
        }
        if (smpBtn != null) {
            String s = smpStatus;
            int tw = Ui.width(s);
            Ui.rrect(smpBtn.x + smpBtn.w - tw - 18, smpBtn.y + smpBtn.h / 2 - 2, 4, 4, 2, smpUp ? Ui.GOOD : Ui.BAD);
            Ui.text(fontRendererObj, s, smpBtn.x + smpBtn.w - tw - 8, smpBtn.y + (smpBtn.h - 8) / 2, 0xFFE8ECF8);
        }

        // apply an account switch that finished on a worker thread

        if (acctPending != null) { AccountManager.Session ps = acctPending; acctPending = null; try { SessionSwap.apply(ps); acctNote = "Now playing as " + ps.username; } catch (Throwable ex) { acctNote = "Could not switch: " + ex.getClass().getSimpleName(); } }

        if (acctError != null) { acctNote = acctError; acctError = null; }

        // account chip (top-left): rank-coloured Blue Client icon + name; hidden icon while the rank backend is down
        String user = mc.getSession().getUsername();
        Integer rankCol = RankService.selfColor();
        int ic = rankCol == null ? 0 : 12;
        List<LauncherAccounts.Account> blueAccts = LauncherAccounts.list();
        int cw = ic + Ui.width(user) + 50;
        Ui.card(8, 8, cw, 22, 6, Ui.CARD, Ui.PANEL_EDGE);
        Heads.draw(mc.getSession().getPlayerID(), 13, 13, 12);
        if (rankCol != null) Ui.rrect(29, 15, 9, 9, 3, rankCol);
        Ui.text(fontRendererObj, user, 30 + ic, 15, Ui.TEXT);
        Ui.text(fontRendererObj, "v", 8 + cw - 15, 15, Ui.MUTED);
        if (acctOpen) {
            int rows = blueAccts.size() + 1, rw = Math.max(cw, 170), ry = 34, rh = 20;
            Ui.card(8, ry, rw, rows * rh + 8, 6, Ui.PANEL, Ui.PANEL_EDGE);
            for (int i = 0; i < rows; i++) {
                int rowY = ry + 4 + i * rh;
                if (Ui.inside(mx, my, 12, rowY, rw - 8, rh - 2)) Ui.rrect(12, rowY, rw - 8, rh - 2, 4, Ui.CARD_HOVER);
                if (i == blueAccts.size()) { Ui.text(fontRendererObj, "+ Add account", 20, rowY + 5, Ui.ACCENT); continue; }
                LauncherAccounts.Account a = blueAccts.get(i);
                boolean cur = a.username.equalsIgnoreCase(user);
                Ui.rrect(17, rowY + 4, 8, 8, 2, cur ? Ui.GOOD : Ui.TRACK_OFF);
                Ui.text(fontRendererObj, a.username, 30, rowY + 5, cur ? Ui.TEXT : Ui.MUTED);
                Ui.text(fontRendererObj, "x", 12 + rw - 8 - 12, rowY + 5, Ui.MUTED);
            }
            if (!acctNote.isEmpty()) Ui.text(fontRendererObj, acctNote, 8, ry + rows * rh + 14, Ui.MUTED);
        } else if (acctLogin == null && !acctNote.isEmpty()) {
            Ui.text(fontRendererObj, acctNote, 8, 36, Ui.MUTED);
        }
        if (acctLogin != null) {
            if (acctLogin.finished) {
                if (acctLogin.session != null) acctPending = acctLogin.session; else acctNote = acctLogin.error == null ? "Sign-in failed" : acctLogin.error;
                acctLogin = null; acctOpened = false;
            } else {
                if (!acctOpened && !acctLogin.code.isEmpty()) { acctOpened = true; AccountManager.copy(acctLogin.code); AccountManager.openUrl(acctLogin.uri); }
                Ui.card(8, 34, 250, 64, 6, Ui.PANEL, Ui.PANEL_EDGE);
                Ui.text(fontRendererObj, acctLogin.code.isEmpty() ? "Contacting Microsoft..." : "Enter this code in your browser:", 16, 42, Ui.MUTED);
                Ui.text(fontRendererObj, acctLogin.code, 16, 56, Ui.TEXT);
                Ui.text(fontRendererObj, acctLogin.status, 16, 74, Ui.MUTED);
                Ui.text(fontRendererObj, "Cancel", 8 + 250 - 44, 56, Ui.MUTED);
            }
        }

        Ui.text(fontRendererObj, "Blue Client V1", 8, uh() - 14, Ui.MUTED);
        String copyright = "Copyright Mojang AB. Do not distribute!";
        Ui.text(fontRendererObj, copyright, uw() - 8 - Ui.width(copyright), uh() - 14, Ui.MUTED);

        // theme paintbrush (top-right) -> Select Theme screen
        boolean th = Ui.inside(mx, my, uw() - 30, 8, 22, 22);
        Ui.card(uw() - 30, 8, 22, 22, 6, th ? Ui.CARD_HOVER : Ui.CARD, th ? Ui.ACCENT : Ui.PANEL_EDGE);
        for (int i = 0; i < 9; i++) Ui.fill(uw() - 24 + i, 24 - i, uw() - 22 + i, 26 - i, i > 5 ? Ui.ACCENT : Ui.TEXT);
        GlStateManager.popMatrix();
        Ui.res = 1;
    }

    @Override
    protected void mouseClicked(int rawMx, int rawMy, int button) throws IOException {
        if (button != 0) { super.mouseClicked(rawMx, rawMy, button); return; }
        double mx = toUi(rawMx), my = toUi(rawMy);
        List<LauncherAccounts.Account> blueAccts = LauncherAccounts.list();
        {
            int cw0 = (RankService.selfColor() == null ? 0 : 11) + Ui.width(mc.getSession().getUsername()) + 38;
            if (acctLogin != null) {
                if (Ui.inside(mx, my, 8 + 250 - 48, 52, 44, 16)) { acctLogin.cancel(); acctLogin = null; acctOpened = false; }
                return;
            }
            if (Ui.inside(mx, my, 8, 8, cw0, 22)) { acctOpen = !acctOpen; acctNote = ""; return; }
            if (acctOpen) {
                int rw = Math.max(cw0, 170), rh = 20, ry = 34, n = blueAccts.size();
                for (int i = 0; i <= n; i++) {
                    if (!Ui.inside(mx, my, 12, ry + 4 + i * rh, rw - 8, rh - 2)) continue;
                    if (i == n) { acctLogin = AccountManager.startLogin(); acctOpened = false; acctOpen = false; acctNote = ""; return; }
                    LauncherAccounts.Account a = blueAccts.get(i);
                    if (mx >= 12 + rw - 8 - 18) { AccountManager.remove(a.uuid); acctNote = "Removed " + a.username; return; }
                    acctNote = "Switching to " + a.username + "...";
                    AccountManager.switchTo(a.uuid, new AccountManager.Callback() {
                        public void done(AccountManager.Session s, String err) { if (s != null) acctPending = s; else acctError = err; }
                    });
                    return;
                }
                acctOpen = false;
            }
        }
        if (Ui.inside(mx, my, uw() - 30, 8, 22, 22)) { mc.displayGuiScreen(new BlueThemeScreen(this)); return; }
        for (Btn b : buttons) if (Ui.inside(mx, my, b.x, b.y, b.w, b.h)) { b.action.run(); return; }
    }

    private static void pingSmp() {
        Thread t = new Thread(new Runnable() { public void run() {
            java.net.Socket s = new java.net.Socket();
            try {
                long t0 = System.nanoTime();
                s.connect(new java.net.InetSocketAddress(SMP_HOST, 25565), 3000); s.setSoTimeout(3000);
                java.io.DataOutputStream out = new java.io.DataOutputStream(s.getOutputStream());
                java.io.DataInputStream in = new java.io.DataInputStream(s.getInputStream());
                java.io.ByteArrayOutputStream hs = new java.io.ByteArrayOutputStream(); java.io.DataOutputStream hd = new java.io.DataOutputStream(hs);
                hd.writeByte(0); varInt(hd, 47); byte[] hb = SMP_HOST.getBytes("UTF-8");
                varInt(hd, hb.length); hd.write(hb); hd.writeShort(25565); varInt(hd, 1);
                varInt(out, hs.size()); out.write(hs.toByteArray());
                out.writeByte(1); out.writeByte(0); out.flush();
                readVar(in); readVar(in); int len = readVar(in);
                byte[] data = new byte[len]; in.readFully(data);
                int ping = (int) ((System.nanoTime() - t0) / 1000000L);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"online\"\\s*:\\s*(\\d+)").matcher(new String(data, "UTF-8"));
                smpStatus = (m.find() ? m.group(1) : "?") + " online - " + ping + "ms";
                smpUp = true;
            } catch (Throwable e) { smpStatus = "offline"; smpUp = false; }
            finally { try { s.close(); } catch (Throwable ignored) { } }
        } }, "BlueSMP-menu-ping");
        t.setDaemon(true); t.start();
    }
    private static void varInt(java.io.DataOutputStream o, int v) throws IOException {
        while ((v & ~0x7F) != 0) { o.writeByte((v & 0x7F) | 0x80); v >>>= 7; }
        o.writeByte(v);
    }
    private static int readVar(java.io.DataInputStream in) throws IOException {
        int v = 0, p = 0, b;
        do { b = in.readUnsignedByte(); v |= (b & 0x7F) << p; p += 7; } while ((b & 0x80) != 0 && p < 35);
        return v;
    }
    @Override
    protected void keyTyped(char typed, int keyCode) throws IOException { /* no Esc on the title screen */ }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
