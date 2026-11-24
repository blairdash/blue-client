package com.blairdash.blueclient.features;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** Crosshair share codes ("BCC1." + deflated, Base64URL id=value lines), compatible with the newer versions. */
public final class CrosshairShare {
    private CrosshairShare() { }

    private static final String PREFIX = "BCC1.";
    private static final int MAX_DECODED = 262144;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static boolean skip(String id) { return id.equals("preset") || id.equals("editor_cell"); }

    public static String export() {
        StringBuilder sb = new StringBuilder();
        for (String id : CrosshairData.ids()) {
            if (skip(id)) continue;
            double v = CrosshairData.n(id);
            if (CrosshairData.isRow(id) && v == 0) continue; // empty pixel rows
            sb.append(id).append('=');
            if (CrosshairData.kind(id) == CrosshairData.COLOR) sb.append(CrosshairData.col(id));
            else if (CrosshairData.kind(id) == CrosshairData.NUM) sb.append(v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v));
            else sb.append((long) Math.rint(v));
            sb.append('\n');
        }
        byte[] raw = sb.toString().getBytes(UTF8);
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(raw);
        d.finish();
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
            text = new String(out.toByteArray(), UTF8);
        } catch (Exception e) { return false; }
        Map<String, String> vals = new HashMap<String, String>();
        for (String line : text.split("\n")) {
            int i = line.indexOf('=');
            if (i > 0) vals.put(line.substring(0, i), line.substring(i + 1));
        }
        if (vals.isEmpty()) return false;
        for (String id : CrosshairData.ids()) {
            if (skip(id)) continue;
            String v = vals.get(id);
            if (v == null) { if (CrosshairData.isRow(id)) CrosshairData.set(id, 0); continue; }
            try {
                if (CrosshairData.kind(id) == CrosshairData.COLOR) CrosshairData.setCol(id, Integer.parseInt(v));
                else CrosshairData.set(id, Double.parseDouble(v));
            } catch (NumberFormatException ignored) { }
        }
        return true;
    }
}