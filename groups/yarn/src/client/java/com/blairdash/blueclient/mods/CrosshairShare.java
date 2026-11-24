package com.blairdash.blueclient.mods;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** Crosshair share codes: every crosshair setting, deflated and Base64 encoded, starting with "BCC1.". */
public final class CrosshairShare {
    private CrosshairShare() { }

    private static final String PREFIX = "BCC1.";
    private static final int MAX_DECODED = 262144;
    private static int lastCopy, lastPaste;

    private static boolean skip(Setting s) {
        return s.id.equals("preset") || s.id.equals("editor_cell") || s.id.startsWith("share_") || s.id.equals("grid_v2");
    }

    public static String export() {
        BlueMod h = ModManager.get("crosshair_opts");
        StringBuilder sb = new StringBuilder();
        for (Setting s : h.settings) {
            if (skip(s)) continue;
            Object v = s.export();
            if (s.hidden && v instanceof Number && ((Number) v).doubleValue() == 0) continue; // empty pixel rows
            sb.append(s.id).append('=');
            if (v instanceof Boolean) sb.append(((Boolean) v) ? 1 : 0);
            else if (s.kind == Setting.Kind.NUM) sb.append(((Number) v).doubleValue() == Math.rint(((Number) v).doubleValue()) ? String.valueOf(((Number) v).longValue()) : v.toString());
            else sb.append(((Number) v).intValue());
            sb.append('\n');
        }
        byte[] raw = sb.toString().getBytes(StandardCharsets.UTF_8);
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(raw); d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        while (!d.finished()) out.write(buf, 0, d.deflate(buf));
        d.end();
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
    }

    /** Applies a code; false if it is not a valid crosshair code (nothing is changed then). */
    public static boolean importCode(String code) {
        if (code == null) return false;
        code = code.trim();
        if (!code.startsWith(PREFIX)) return false;
        String text;
        try {
            byte[] comp = Base64.getUrlDecoder().decode(code.substring(PREFIX.length()));
            Inflater inf = new Inflater();
            inf.setInput(comp);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                out.write(buf, 0, n);
                if (out.size() > MAX_DECODED) return false;
            }
            inf.end();
            text = out.toString("UTF-8");
        } catch (Exception e) { return false; }
        BlueMod h = ModManager.get("crosshair_opts");
        java.util.Map<String, String> vals = new java.util.HashMap<>();
        for (String line : text.split("\n")) {
            int i = line.indexOf('=');
            if (i > 0) vals.put(line.substring(0, i), line.substring(i + 1));
        }
        if (vals.isEmpty()) return false;
        for (Setting s : h.settings) {
            if (skip(s)) continue;
            String v = vals.get(s.id);
            if (v == null) { if (s.hidden) s.n = 0; continue; }
            try {
                switch (s.kind) {
                    case BOOL: s.b = v.equals("1"); break;
                    case NUM: s.n = Math.max(s.min, Math.min(s.max, Double.parseDouble(v))); break;
                    case CHOICE: s.idx = Math.max(0, Math.min(s.options.length - 1, Integer.parseInt(v))); break;
                    default: s.color = Integer.parseInt(v); break;
                }
            } catch (NumberFormatException ignored) { }
        }
        return true;
    }

    private static String clip() {
        try { return org.lwjgl.glfw.GLFW.glfwGetClipboardString(0L); } catch (Throwable t) { return null; }
    }

    /** Polled from the options holder: clicking the two share buttons performs the action. */
    public static void tick() {
        Setting c = LegacyOptions.s("crosshair_opts", "share_copy"), p = LegacyOptions.s("crosshair_opts", "share_paste");
        if (c.idx != lastCopy) {
            lastCopy = c.idx;
            if (c.idx != 0) {
                try { org.lwjgl.glfw.GLFW.glfwSetClipboardString(0L, export()); } catch (Throwable ignored) { }
            }
        }
        if (p.idx != lastPaste) {
            lastPaste = p.idx;
            if (p.idx != 0) { p.idx = importCode(clip()) ? 1 : 2; lastPaste = p.idx; }
        }
    }
}