package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.features.CrosshairData;
import com.blairdash.blueclient.features.CrosshairPresets;
import com.blairdash.blueclient.features.CrosshairShare;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/** Custom crosshair settings (right-click the Crosshair toggle in the main menu). Tabs: Normal / Friendly / Enemy. */
public class CrosshairSettingsScreen extends GuiScreen {
    private static int tabIdx = 0;
    private final GuiScreen parent;

    private static final class Ctl {
        final String id, label; final int kind; final boolean global;
        GuiButton button; GuiTextField field;
        Ctl(String id, String label, int kind, boolean global) { this.id = id; this.label = label; this.kind = kind; this.global = global; }
    }

    private final List<Ctl> ctls = new ArrayList<Ctl>();
    private String status = "";
    private long statusUntil;

    public CrosshairSettingsScreen(GuiScreen parent) { this.parent = parent; }

    private String key(Ctl c) { return c.global ? c.id : CrosshairData.PREFIX[tabIdx] + c.id; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        ctls.clear();
        // column 1
        ctls.add(new Ctl("shape", "Shape", CrosshairData.CHOICE, false));
        ctls.add(new Ctl("scale", "Scale", CrosshairData.NUM, false));
        ctls.add(new Ctl("color", "Color", CrosshairData.COLOR, false));
        ctls.add(new Ctl("size", "Size", CrosshairData.NUM, false));
        ctls.add(new Ctl("thickness", "Thickness", CrosshairData.NUM, false));
        ctls.add(new Ctl("gap", "Gap", CrosshairData.NUM, false));
        // column 2
        ctls.add(new Ctl("dot", "Center dot", CrosshairData.BOOL, false));
        ctls.add(new Ctl("dot_size", "Dot size", CrosshairData.NUM, false));
        ctls.add(new Ctl("outline", "Outline", CrosshairData.BOOL, false));
        ctls.add(new Ctl("outline_color", "Outl. color", CrosshairData.COLOR, false));
        ctls.add(new Ctl("outline_thickness", "Outl. thick", CrosshairData.NUM, false));
        ctls.add(new Ctl("custom", "Pixel crosshair", CrosshairData.BOOL, false));
        // column 3
        ctls.add(new Ctl("preset", "Preset", CrosshairData.CHOICE, true));
        ctls.add(new Ctl("dyn_bow", "Bow spread", CrosshairData.NUM, true));
        ctls.add(new Ctl("dyn_attack", "Swing spread", CrosshairData.NUM, true));
        ctls.add(new Ctl("enemy", "Enemy crosshair", CrosshairData.BOOL, true));
        ctls.add(new Ctl("friendly", "Friendly crosshair", CrosshairData.BOOL, true));
        ctls.add(new Ctl("show_f5", "Show in F5", CrosshairData.BOOL, true));

        int colW = 138, gap = 6;
        int total = colW * 3 + gap * 2;
        int x0 = this.width / 2 - total / 2;
        for (int i = 0; i < 3; i++) {
            this.buttonList.add(new GuiButton(10 + i, x0 + i * (colW + gap), 22, colW, 20, new String[] { "Normal", "Friendly", "Enemy" }[i]));
        }
        ((GuiButton) this.buttonList.get(tabIdx)).enabled = false;
        for (int i = 0; i < ctls.size(); i++) {
            Ctl c = ctls.get(i);
            int col = i / 6, row = i % 6;
            int x = x0 + col * (colW + gap), y = 50 + row * 22;
            if (c.id.equals("preset") && tabIdx != 0) continue;
            if (c.kind == CrosshairData.NUM || c.kind == CrosshairData.COLOR) {
                c.field = new GuiTextField(100 + i, this.fontRendererObj, x + 68, y, colW - 68, 20);
                c.field.setMaxStringLength(c.kind == CrosshairData.COLOR ? 8 : 6);
                c.field.setText(c.kind == CrosshairData.COLOR ? String.format("%08X", CrosshairData.col(key(c)))
                        : (CrosshairData.n(key(c)) == Math.rint(CrosshairData.n(key(c))) ? String.valueOf((long) CrosshairData.n(key(c))) : String.valueOf((float) CrosshairData.n(key(c)))));
            } else {
                c.button = new GuiButton(100 + i, x, y, colW, 20, buttonText(c));
                this.buttonList.add(c.button);
            }
        }
        int by = 50 + 6 * 22 + 8;
        this.buttonList.add(new GuiButton(20, x0, by, 100, 20, "Pixel editor"));
        this.buttonList.add(new GuiButton(21, x0 + 106, by, 100, 20, "Copy my code"));
        this.buttonList.add(new GuiButton(22, x0 + 212, by, 100, 20, "Paste a code"));
        this.buttonList.add(new GuiButton(23, x0 + total - 100, by, 100, 20, "Done"));
    }

    private String buttonText(Ctl c) {
        String k = key(c);
        if (c.kind == CrosshairData.BOOL) return c.label + ": " + (CrosshairData.b(k) ? "ON" : "OFF");
        if (c.id.equals("preset")) return "Preset: " + CrosshairPresets.NAMES[Math.max(0, Math.min(CrosshairPresets.NAMES.length - 1, CrosshairData.i("preset")))];
        return c.label + ": " + CrosshairData.SHAPES[Math.max(0, Math.min(CrosshairData.SHAPES.length - 1, CrosshairData.i(k)))];
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= 10 && b.id <= 12) { CrosshairData.save(); tabIdx = b.id - 10; initGui(); return; }
        if (b.id == 20) { CrosshairData.save(); this.mc.displayGuiScreen(new PixelEditorScreen(this, tabIdx)); return; }
        if (b.id == 21) {
            setClipboardString(CrosshairShare.export());
            status = "Copied your crosshair code to the clipboard";
            statusUntil = System.currentTimeMillis() + 3000;
            return;
        }
        if (b.id == 22) {
            boolean ok = CrosshairShare.importCode(getClipboardString());
            status = ok ? "Crosshair imported" : "The clipboard does not hold a crosshair code";
            statusUntil = System.currentTimeMillis() + 3000;
            if (ok) { CrosshairData.save(); initGui(); }
            return;
        }
        if (b.id == 23) { CrosshairData.save(); this.mc.displayGuiScreen(parent); return; }
        for (Ctl c : ctls) {
            if (c.button != b) continue;
            String k = key(c);
            if (c.kind == CrosshairData.BOOL) CrosshairData.setBool(k, !CrosshairData.b(k));
            else if (c.id.equals("preset")) {
                int next = (CrosshairData.i("preset") + 1) % CrosshairPresets.NAMES.length;
                CrosshairData.set("preset", next);
                CrosshairPresets.apply(next);
                CrosshairData.save();
                initGui();
                return;
            } else CrosshairData.set(k, (CrosshairData.i(k) + 1) % CrosshairData.SHAPES.length);
            b.displayString = buttonText(c);
        }
    }

    private void applyField(Ctl c) {
        String t = c.field.getText().trim();
        if (t.isEmpty()) return;
        try {
            if (c.kind == CrosshairData.COLOR) CrosshairData.setCol(key(c), (int) Long.parseLong(t, 16));
            else CrosshairData.set(key(c), Double.parseDouble(t));
        } catch (NumberFormatException ignored) { }
    }

    @Override
    protected void keyTyped(char ch, int code) throws IOException {
        for (Ctl c : ctls) if (c.field != null && c.field.textboxKeyTyped(ch, code)) applyField(c);
        if (code == 1) { CrosshairData.save(); this.mc.displayGuiScreen(parent); return; }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        for (Ctl c : ctls) if (c.field != null) c.field.mouseClicked(mx, my, button);
        super.mouseClicked(mx, my, button);
    }

    @Override
    public void updateScreen() { for (Ctl c : ctls) if (c.field != null) c.field.updateCursorCounter(); }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); CrosshairData.save(); }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, "Custom Crosshair  (colors are ARGB hex, e.g. FFFFFFFF)", this.width / 2, 8, 0xFFFFFFFF);
        int colW = 138, gap = 6, x0 = this.width / 2 - (colW * 3 + gap * 2) / 2;
        for (int i = 0; i < ctls.size(); i++) {
            Ctl c = ctls.get(i);
            if (c.field == null) continue;
            int col = i / 6, row = i % 6;
            drawString(this.fontRendererObj, c.label, x0 + col * (colW + gap), 50 + row * 22 + 6, 0xFFAAAAAA);
            c.field.drawTextBox();
        }
        if (System.currentTimeMillis() < statusUntil) drawCenteredString(this.fontRendererObj, status, this.width / 2, 50 + 6 * 22 + 34, 0xFF55FF55);
        super.drawScreen(mx, my, pt);
    }
}