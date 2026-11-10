package com.artic.aia;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

final class Fx {
    private Fx() {}

    private static final double TAU = Math.PI * 2.0;

    static void begin() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    static void end() {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static int a255(float a) {
        int v = (int) (a * 255f);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    static void glow(float cx, float cy, float r, int rgb, float alpha) {
        if (alpha <= 0.002f || r <= 0f) return;
        int R = (rgb >> 16) & 255, G = (rgb >> 8) & 255, B = rgb & 255;
        int A = a255(alpha);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy, 0).color(R, G, B, A).endVertex();
        int seg = 40;
        for (int i = 0; i <= seg; i++) {
            double ang = i * TAU / seg;
            wr.pos(cx + Math.cos(ang) * r, cy + Math.sin(ang) * r, 0).color(R, G, B, 0).endVertex();
        }
        Tessellator.getInstance().draw();
    }

    static void ring(float cx, float cy, float r, float thick, int rgb, float alpha) {
        if (alpha <= 0.002f || r <= 0f) return;
        int R = (rgb >> 16) & 255, G = (rgb >> 8) & 255, B = rgb & 255;
        int A = a255(alpha);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int seg = 56;
        for (int i = 0; i <= seg; i++) {
            double ang = i * TAU / seg;
            double c = Math.cos(ang), s = Math.sin(ang);
            wr.pos(cx + c * (r - thick), cy + s * (r - thick), 0).color(R, G, B, 0).endVertex();
            wr.pos(cx + c * r, cy + s * r, 0).color(R, G, B, A).endVertex();
        }
        Tessellator.getInstance().draw();
        wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= seg; i++) {
            double ang = i * TAU / seg;
            double c = Math.cos(ang), s = Math.sin(ang);
            wr.pos(cx + c * r, cy + s * r, 0).color(R, G, B, A).endVertex();
            wr.pos(cx + c * (r + thick), cy + s * (r + thick), 0).color(R, G, B, 0).endVertex();
        }
        Tessellator.getInstance().draw();
    }

    static WorldRenderer beginTris() {
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        return wr;
    }

    static void endTris() {
        Tessellator.getInstance().draw();
    }

    static void wedge(WorldRenderer wr, float cx, float cy, float ang, float len, float half,
                      int rgb, float aApex, float aTip) {
        int R = (rgb >> 16) & 255, G = (rgb >> 8) & 255, B = rgb & 255;
        wr.pos(cx, cy, 0).color(R, G, B, a255(aApex)).endVertex();
        wr.pos(cx + Math.cos(ang - half) * len, cy + Math.sin(ang - half) * len, 0).color(R, G, B, a255(aTip)).endVertex();
        wr.pos(cx + Math.cos(ang + half) * len, cy + Math.sin(ang + half) * len, 0).color(R, G, B, a255(aTip)).endVertex();
    }

    static void sparkle(WorldRenderer wr, float x, float y, float s, float alpha) {
        int A = a255(alpha);
        int A0 = 0;
        float w = s * 0.14f;
        wr.pos(x - s, y, 0).color(255, 255, 255, A0).endVertex();
        wr.pos(x, y - w, 0).color(255, 255, 255, A).endVertex();
        wr.pos(x + s, y, 0).color(255, 255, 255, A0).endVertex();
        wr.pos(x - s, y, 0).color(255, 255, 255, A0).endVertex();
        wr.pos(x + s, y, 0).color(255, 255, 255, A0).endVertex();
        wr.pos(x, y + w, 0).color(255, 255, 255, A).endVertex();
        wr.pos(x, y - s, 0).color(255, 255, 255, A0).endVertex();
        wr.pos(x - w, y, 0).color(255, 255, 255, A).endVertex();
        wr.pos(x, y + s, 0).color(255, 255, 255, A0).endVertex();
        wr.pos(x, y - s, 0).color(255, 255, 255, A0).endVertex();
        wr.pos(x, y + s, 0).color(255, 255, 255, A0).endVertex();
        wr.pos(x + w, y, 0).color(255, 255, 255, A).endVertex();
    }
}
