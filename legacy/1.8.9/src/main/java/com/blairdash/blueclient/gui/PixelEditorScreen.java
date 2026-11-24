package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.features.CrosshairData;
import com.blairdash.blueclient.features.PixelCrosshair;
import java.io.IOException;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

/** Pixel crosshair editor: left click paints, right click erases, tabs pick the Normal / Friendly / Enemy grid. */
public class PixelEditorScreen extends GuiScreen {
    private final GuiScreen parent;
    private int edit;       // 0 Normal, 1 Friendly, 2 Enemy
    private int selected = 1;
    private int dragButton = -1;

    public PixelEditorScreen(GuiScreen parent, int profile) {
        this.parent = parent;
        this.edit = Math.max(0, Math.min(2, profile));
    }

    private int cell() {
        int want = (int) CrosshairData.n("editor_cell");
        int fit = Math.max(4, (this.height - 190) / PixelCrosshair.n());
        return Math.max(4, Math.min(want, fit));
    }
    private int size() { return PixelCrosshair.n() * cell(); }
    private int gx() { return this.width / 2 - size() / 2; }
    private int gy() { return Math.max(24, (this.height - (size() + 130)) / 2 + 14); }
    private int palX() { return this.width / 2 - CrosshairData.PALETTE.length * 14 / 2; }
    private int palY() { return gy() + size() + 10; }
    private int[] btn(int i) { // 0 clear, 1 done, 2 zoom-, 3 zoom+, 4 grid-, 5 grid+
        int y = palY();
        switch (i) {
            case 0: return new int[] { this.width / 2 - 70, y + 22, 60, 18 };
            case 1: return new int[] { this.width / 2 + 10, y + 22, 60, 18 };
            case 2: return new int[] { this.width / 2 - 70, y + 64, 60, 16 };
            case 3: return new int[] { this.width / 2 + 10, y + 64, 60, 16 };
            case 4: return new int[] { this.width / 2 - 70, y + 84, 60, 16 };
            default: return new int[] { this.width / 2 + 10, y + 84, 60, 16 };
        }
    }
    private int[] tab(int i) { return new int[] { this.width / 2 - 72 + i * 50, palY() + 44, 48, 16 }; }
    private static boolean in(int mx, int my, int[] r) { return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]; }

    private void paint(int mx, int my, int button) {
        int c = cell(), n = PixelCrosshair.n(), gx = gx(), gy = gy();
        if (mx < gx || my < gy || mx >= gx + n * c || my >= gy + n * c) return;
        int x = (mx - gx) / c, y = (my - gy) / c;
        PixelCrosshair.set(edit, x, y, button == 0 ? selected : 0);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0) {
            for (int i = 0; i < 3; i++) if (in(mx, my, tab(i))) { edit = i; return; }
            if (in(mx, my, btn(2))) { CrosshairData.set("editor_cell", cell() - 2); return; }
            if (in(mx, my, btn(3))) { CrosshairData.set("editor_cell", cell() + 2); return; }
            if (in(mx, my, btn(4))) { PixelCrosshair.resize(-1); return; }
            if (in(mx, my, btn(5))) { PixelCrosshair.resize(1); return; }
            if (in(mx, my, btn(0))) { PixelCrosshair.clear(edit); return; }
            if (in(mx, my, btn(1))) { finish(); return; }
            for (int i = 0; i < CrosshairData.PALETTE.length; i++) {
                if (in(mx, my, new int[] { palX() + i * 14, palY(), 12, 12 })) { selected = i + 1; return; }
            }
        }
        if (button == 0 || button == 1) { dragButton = button; paint(mx, my, button); }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) { if (dragButton >= 0) paint(mx, my, dragButton); }

    @Override
    protected void mouseReleased(int mx, int my, int state) { dragButton = -1; }

    @Override
    protected void keyTyped(char c, int key) throws IOException { if (key == 1) finish(); }

    private void finish() {
        // drawing something turns the pixel crosshair on for that profile
        String p = CrosshairData.PREFIX[edit];
        boolean any = false;
        int n = PixelCrosshair.n();
        for (int y = 0; y < n && !any; y++) for (int x = 0; x < n; x++) if (PixelCrosshair.get(edit, x, y) != 0) { any = true; break; }
        if (any) CrosshairData.setBool(p + "custom", true);
        CrosshairData.save();
        this.mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private void box(int x1, int y1, int x2, int y2, int color) { Gui.drawRect(x1, y1, x2, y2, color); }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        box(0, 0, this.width, this.height, 0xFF0B1018);
        int n = PixelCrosshair.n(), c = cell(), gx = gx(), gy = gy(), size = n * c;
        drawCenteredString(this.fontRendererObj, "Pixel crosshair editor (left click paints, right click erases)", this.width / 2, 8, 0xFFFFFFFF);
        box(gx - 3, gy - 3, gx + size + 2, gy + size + 2, 0xFF2A3450);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int v = PixelCrosshair.get(edit, x, y);
                int col = v > 0 ? PixelCrosshair.argb(v) : ((x + y) % 2 == 0 ? 0xFF1B2433 : 0xFF161E2B);
                if (v == 0 && n % 2 == 1 && x == n / 2 && y == n / 2) col = 0xFF2F4B7A;
                box(gx + x * c, gy + y * c, gx + (x + 1) * c - 1, gy + (y + 1) * c - 1, col);
            }
        }
        if (mx >= gx && my >= gy && mx < gx + size && my < gy + size) {
            int hx = gx + ((mx - gx) / c) * c, hy = gy + ((my - gy) / c) * c;
            box(hx, hy, hx + c - 1, hy + 1, 0xFFFFFFFF);
            box(hx, hy + c - 2, hx + c - 1, hy + c - 1, 0xFFFFFFFF);
        }
        for (int i = 0; i < CrosshairData.PALETTE.length; i++) {
            int x = palX() + i * 14, y = palY();
            box(x, y, x + 12, y + 12, selected == i + 1 ? 0xFFFFFFFF : 0xFF2A3450);
            box(x + 1, y + 1, x + 11, y + 11, CrosshairData.PALETTE[i]);
        }
        String[] names = { "Clear", "Done", "Zoom -", "Zoom +", "Grid -", "Grid +" };
        for (int i = 0; i < 6; i++) {
            int[] b = btn(i);
            box(b[0], b[1], b[0] + b[2], b[1] + b[3], in(mx, my, b) ? 0xFF3D8BFF : 0xFF22304A);
            drawCenteredString(this.fontRendererObj, names[i], b[0] + b[2] / 2, b[1] + (b[3] - 8) / 2, 0xFFFFFFFF);
        }
        String[] tabs = { "Normal", "Friend", "Enemy" };
        for (int i = 0; i < 3; i++) {
            int[] b = tab(i);
            box(b[0], b[1], b[0] + b[2], b[1] + b[3], i == edit ? 0xFF3D8BFF : (in(mx, my, b) ? 0xFF2F4B7A : 0xFF22304A));
            drawCenteredString(this.fontRendererObj, tabs[i], b[0] + b[2] / 2, b[1] + (b[3] - 8) / 2, 0xFFFFFFFF);
        }
        drawCenteredString(this.fontRendererObj, n + " x " + n, this.width / 2, palY() + 106, 0xFF9DB6E6);
    }
}