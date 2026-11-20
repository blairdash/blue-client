package com.blairdash.blueclient.features;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/** A dedicated buffered line layer keeps width local to hitboxes. */
public final class HitboxLines {
    private static final Map<Float, RenderLayer> LAYERS = new HashMap<>();
    private HitboxLines() { }

    public static RenderLayer layer(float width) {
        return LAYERS.computeIfAbsent(width, w -> RenderLayer.of("blue_hitboxes_" + w,
                VertexFormats.LINES, VertexFormat.DrawMode.LINES, 1536,
                RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.LINES_PROGRAM)
                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(w)))
                        .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .target(RenderPhase.ITEM_ENTITY_TARGET).cull(RenderPhase.DISABLE_CULLING).build(false)));
    }
}
