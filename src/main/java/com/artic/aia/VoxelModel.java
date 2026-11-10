package com.artic.aia;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

final class VoxelModel {
    private static final int N = 16;
    private static final int DEPTH = 3;
    private static final Map<String, VoxelModel> CACHE = new HashMap<String, VoxelModel>();

    private final boolean[][] solid = new boolean[N][N];
    private final int[][] rgb = new int[N][N];

    private VoxelModel() {}

    static VoxelModel get(ItemLook look) {
        String key = look.itemName + "|" + look.damage + "|" + look.tint;
        VoxelModel m = CACHE.get(key);
        if (m != null) return m;
        try {
            m = build(look);
        } catch (Throwable t) {
            AIAConfig.log("voxel model failed for " + key + ": " + t);
        }
        if (m != null) CACHE.put(key, m);
        return m;
    }

    private static VoxelModel build(ItemLook look) {
        BufferedImage base = Sprites.read(look.itemName, look.damage, false);
        if (base == null) return null;
        BufferedImage over = look.tint >= 0 ? Sprites.read(look.itemName, look.damage, true) : null;
        VoxelModel m = new VoxelModel();
        boolean overlayTint = look.itemName != null && look.itemName.endsWith("firework_charge");
        boolean any = false;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int c = base.getRGB(Math.min(base.getWidth() - 1, (int) ((x + 0.5f) * base.getWidth() / N)),
                                    Math.min(base.getHeight() - 1, (int) ((y + 0.5f) * base.getHeight() / N)));
                int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                boolean on = ((c >>> 24) & 255) >= 128;
                if (look.tint >= 0 && !overlayTint) {
                    r = r * ((look.tint >> 16) & 255) / 255;
                    g = g * ((look.tint >> 8) & 255) / 255;
                    b = b * (look.tint & 255) / 255;
                }
                if (over != null) {
                    int oc = over.getRGB(Math.min(over.getWidth() - 1, (int) ((x + 0.5f) * over.getWidth() / N)),
                                         Math.min(over.getHeight() - 1, (int) ((y + 0.5f) * over.getHeight() / N)));
                    if (((oc >>> 24) & 255) >= 128) {
                        r = (oc >> 16) & 255;
                        g = (oc >> 8) & 255;
                        b = oc & 255;
                        if (overlayTint) {
                            r = r * ((look.tint >> 16) & 255) / 255;
                            g = g * ((look.tint >> 8) & 255) / 255;
                            b = b * (look.tint & 255) / 255;
                        }
                        on = true;
                    }
                }
                m.solid[x][y] = on;
                m.rgb[x][y] = (r << 16) | (g << 8) | b;
                any |= on;
            }
        }
        return any ? m : null;
    }

    private static float shade(float d) {
        d = Math.max(0f, d);
        return 0.50f + 0.50f * Math.min(1f, d / 0.8f);
    }

    void draw(float cx, float cy, float s, float angleDeg) {
        double rad = Math.toRadians(angleDeg);
        float cs = (float) Math.cos(rad), sn = (float) Math.sin(rad);
        final float lx = -0.42f, ly = -0.55f, lz = 0.72f;
        float bLeft = shade(-cs * lx + sn * lz);
        float bRight = shade(cs * lx - sn * lz);
        float bTop = shade(-ly);
        float bBottom = shade(ly);
        float bFront = shade(sn * lx + cs * lz);
        float bBack = shade(-sn * lx - cs * lz);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.color(1f, 1f, 1f, 1f);

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0f);
        GlStateManager.rotate(angleDeg, 0f, 1f, 0f);

        float x0 = -N * 0.5f * s, y0 = -N * 0.5f * s, zh = DEPTH * 0.5f * s;
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int py = 0; py < N; py++) {
            for (int px = 0; px < N; px++) {
                if (!solid[px][py]) continue;
                int c = rgb[px][py];
                float X0 = x0 + px * s, X1 = X0 + s, Y0 = y0 + py * s, Y1 = Y0 + s;
                quad(wr, X0, Y0, zh, X0, Y1, zh, X1, Y1, zh, X1, Y0, zh, c, bFront);
                quad(wr, X1, Y0, -zh, X1, Y1, -zh, X0, Y1, -zh, X0, Y0, -zh, c, bBack);
                if (px == 0 || !solid[px - 1][py]) quad(wr, X0, Y0, -zh, X0, Y1, -zh, X0, Y1, zh, X0, Y0, zh, c, bLeft);
                if (px == N - 1 || !solid[px + 1][py]) quad(wr, X1, Y0, zh, X1, Y1, zh, X1, Y1, -zh, X1, Y0, -zh, c, bRight);
                if (py == 0 || !solid[px][py - 1]) quad(wr, X0, Y0, zh, X1, Y0, zh, X1, Y0, -zh, X0, Y0, -zh, c, bTop);
                if (py == N - 1 || !solid[px][py + 1]) quad(wr, X0, Y1, -zh, X1, Y1, -zh, X1, Y1, zh, X0, Y1, zh, c, bBottom);
            }
        }
        Tessellator.getInstance().draw();
        GlStateManager.popMatrix();

        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.disableDepth();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void quad(WorldRenderer wr, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int rgb, float b) {
        int r = (int) (((rgb >> 16) & 255) * b), g = (int) (((rgb >> 8) & 255) * b), bl = (int) ((rgb & 255) * b);
        wr.pos(ax, ay, az).color(r, g, bl, 255).endVertex();
        wr.pos(bx, by, bz).color(r, g, bl, 255).endVertex();
        wr.pos(cx, cy, cz).color(r, g, bl, 255).endVertex();
        wr.pos(dx, dy, dz).color(r, g, bl, 255).endVertex();
    }
}
