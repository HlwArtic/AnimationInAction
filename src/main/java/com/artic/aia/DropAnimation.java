package com.artic.aia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class DropAnimation {
    final DropEntry entry;
    final String name;
    final Rarity rarity;
    final ItemStack stack;
    final VoxelModel voxel;
    final float dur;
    long startMs = -1;

    private static final int SPIKES = 26, STREAKS = 7, SPARKS = 14;
    private final float[] spikeOff = new float[SPIKES], spikeLen = new float[SPIKES], spikePh = new float[SPIKES];
    private final float[] stAng = new float[STREAKS], stPer = new float[STREAKS], stOff = new float[STREAKS],
        stSweep = new float[STREAKS], stLen = new float[STREAKS], stW = new float[STREAKS];
    private final float[] spAng = new float[SPARKS], spRad = new float[SPARKS], spPh = new float[SPARKS], spSz = new float[SPARKS];

    DropAnimation(DropEntry entry, String name, Rarity rarity, ItemStack stack, VoxelModel voxel) {
        this.entry = entry;
        this.name = name;
        this.rarity = rarity;
        this.stack = stack;
        this.voxel = voxel;
        this.dur = AIAConfig.durationSec;
        Random r = new Random(System.nanoTime() ^ name.hashCode());
        for (int i = 0; i < SPIKES; i++) {
            spikeOff[i] = (float) (r.nextFloat() * 0.18);
            spikeLen[i] = 0.8f + r.nextFloat() * 1.2f;
            spikePh[i] = r.nextFloat() * 6.28f;
        }
        for (int i = 0; i < STREAKS; i++) {
            stAng[i] = r.nextFloat() * 6.2832f;
            stPer[i] = 0.9f + r.nextFloat() * 1.1f;
            stOff[i] = r.nextFloat();
            stSweep[i] = (r.nextBoolean() ? 1 : -1) * (0.15f + r.nextFloat() * 0.35f);
            stLen[i] = 0.55f + r.nextFloat() * 0.45f;
            stW[i] = 0.012f + r.nextFloat() * 0.02f;
        }
        for (int i = 0; i < SPARKS; i++) {
            spAng[i] = r.nextFloat() * 6.2832f;
            spRad[i] = 1.1f + r.nextFloat() * 1.5f;
            spPh[i] = r.nextFloat();
            spSz[i] = 0.5f + r.nextFloat() * 0.9f;
        }
    }

    boolean finished(long now) {
        return startMs >= 0 && (now - startMs) / 1000f >= dur;
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private static float easeOutBack(float p) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        float q = p - 1f;
        return 1f + c3 * q * q * q + c1 * q * q;
    }

    void render(Minecraft mc, int w, int h) {
        long now = Minecraft.getSystemTime();
        if (startMs < 0) startMs = now;
        float t = (now - startMs) / 1000f;
        float D = dur;

        final float flashIn = 0.22f, burstEnd = 0.62f;
        final float outStart = Math.max(burstEnd + 0.5f, D - 1.0f);
        final float outFlash = Math.max(outStart + 0.2f, D - 0.45f);

        boolean depthWas = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean lightWas = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float S = AIAConfig.size;
        float R = 8f * S;
        float cx = w / 2f;
        float cy = h * 0.42f + (float) Math.sin(t * 1.7f) * 3f;

        if (AIAConfig.dim) {
            float d = clamp01(t / 0.4f);
            if (t > D - 0.5f) d *= clamp01((D - t) / 0.5f);
            int a = (int) (d * 0.6f * 255f);
            if (a > 0) Gui.drawRect(0, 0, w, h, a << 24);
        }

        float appear = t < flashIn ? 0f : clamp01((t - flashIn) / 0.3f);
        float boost = t < flashIn ? 0f : 1f + 1.6f * (float) Math.exp(-(t - flashIn) * 3.2f);
        float fxs = (t >= outFlash ? 0f : appear * boost) * AIAConfig.rayIntensity;
        float pop;
        if (t < flashIn) pop = 0f;
        else if (t < burstEnd) pop = easeOutBack((t - flashIn) / (burstEnd - flashIn));
        else if (t < outFlash) pop = 1f;
        else pop = 0f;
        float pulse = 0.85f + 0.15f * (float) Math.sin(t * 4.0f);

        if (fxs > 0.01f) drawRays(cx, cy, R, t, fxs, pulse, flashIn, w, h);

        if (pop > 0.02f) {
            float angle = t * AIAConfig.spinSpeed;
            drawItem(mc, cx, cy, S * pop, angle);
        }

        if (fxs > 0.01f) drawSparkles(cx, cy, R, t, fxs);

        float ta = t < burstEnd ? 0f : clamp01((t - burstEnd) / 0.3f);
        if (t > outStart) ta *= clamp01((outFlash - t) / (outFlash - outStart));
        int alpha = (int) (ta * 255f);
        if (alpha > 8) {
            FontRenderer fr = mc.fontRendererObj;
            float ns = Math.max(1.4f, Math.min(2.6f, 0.6f + S * 0.2f));
            float ty = cy + R + 10f;
            drawCentered(fr, name, cx, ty, ns, (alpha << 24) | rarity.primary);
            drawCentered(fr, rarity.label, cx, ty + 9f * ns + 3f, ns * 0.6f, (alpha << 24) | rarity.secondary);
        }

        if (AIAConfig.flash) {
            float f;
            if (t < flashIn) f = t / flashIn;
            else if (t < burstEnd) f = 1f - (t - flashIn) / (burstEnd - flashIn);
            else if (t >= outStart && t < outFlash) f = (t - outStart) / (outFlash - outStart);
            else if (t >= outFlash) f = 1f - (t - outFlash) / Math.max(0.05f, D - outFlash);
            else f = 0f;
            int fa = (int) (clamp01(f) * 0.92f * 255f);
            if (fa > 0) Gui.drawRect(0, 0, w, h, (fa << 24) | 0xFFFFFF);
        }

        if (lightWas) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (depthWas) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawRays(float cx, float cy, float R, float t, float fxs, float pulse, float flashIn, int w, int h) {
        int c1 = rarity.primary, c2 = rarity.secondary;
        float maxLen = Math.max(w, h) * 0.65f;
        float since = Math.max(0f, t - flashIn);

        Fx.begin();

        float bubble = clamp01(1f - since / 0.7f);
        if (bubble > 0f) Fx.glow(cx, cy, R * (1.6f + since * 1.5f), 0xFFFFFF, 0.45f * bubble);
        if (since > 0f && since < 0.9f) {
            float p = since / 0.9f;
            Fx.ring(cx, cy, R * (0.7f + 2.6f * p), 3f + 5f * p, c2, (1f - p) * 0.8f * AIAConfig.rayIntensity);
        }

        Fx.glow(cx, cy, R * 2.7f, c1, 0.30f * fxs * pulse);
        Fx.glow(cx, cy, R * 1.6f, c2, 0.50f * fxs * pulse);
        Fx.glow(cx, cy, R * 0.95f, 0xFFFFFF, 0.55f * fxs * pulse);

        WorldRenderer wr = Fx.beginTris();

        for (int i = 0; i < SPIKES; i++) {
            float ang = i * (6.2832f / SPIKES) + spikeOff[i] + t * 0.35f;
            float len = R * (1.2f + spikeLen[i] * 1.1f) * (0.82f + 0.18f * (float) Math.sin(t * 5f + spikePh[i]));
            float half = 0.016f + 0.01f * (i % 3);
            int col = (i % 3 == 0) ? 0xFFFFFF : ((i % 3 == 1) ? c2 : c1);
            Fx.wedge(wr, cx, cy, ang, len, half, col, 0.95f * Math.min(1f, fxs), 0.0f);
        }

        for (int k = 0; k < STREAKS; k++) {
            float ph = ((t / stPer[k]) + stOff[k]) % 1f;
            float life = (float) Math.sin(Math.PI * ph);
            if (life <= 0.02f) continue;
            float ang = stAng[k] + (ph - 0.5f) * stSweep[k];
            float len = Math.min(maxLen, maxLen * stLen[k]);
            int col = (k % 3 == 0) ? c1 : ((k % 3 == 1) ? c2 : 0xFFFFFF);
            Fx.wedge(wr, cx, cy, ang, len, stW[k], col, 0.9f * life * Math.min(1f, fxs), 0.10f * life);
        }
        Fx.endTris();
        Fx.end();
    }

    private void drawSparkles(float cx, float cy, float R, float t, float fxs) {
        Fx.begin();
        WorldRenderer wr = Fx.beginTris();
        for (int i = 0; i < SPARKS; i++) {
            float ph = (t * 0.8f + spPh[i]) % 1f;
            float a = (float) Math.sin(Math.PI * ph);
            if (a <= 0.05f) continue;
            float x = cx + (float) Math.cos(spAng[i] + t * 0.15f) * R * spRad[i];
            float y = cy + (float) Math.sin(spAng[i] + t * 0.15f) * R * spRad[i];
            Fx.sparkle(wr, x, y, (4f + R * 0.16f) * spSz[i] * a, Math.min(1f, a * fxs));
        }
        Fx.endTris();
        Fx.end();
    }

    private void drawItem(Minecraft mc, float cx, float cy, float s, float angleDeg) {
        if (voxel != null) {
            voxel.draw(cx, cy, s, angleDeg);
            return;
        }
        if (stack == null) return;
        RenderItem ri = mc.getRenderItem();
        float oldZ = ri.zLevel;
        ri.zLevel = 0f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0f);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.scale(s, s, s);
        GlStateManager.rotate(angleDeg, 0f, 1f, 0f);
        GlStateManager.translate(-8f, -8f, -150f);
        ri.renderItemAndEffectIntoGUI(stack, 0, 0);
        GlStateManager.popMatrix();

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        ri.zLevel = oldZ;
    }

    private static void drawCentered(FontRenderer fr, String text, float cx, float y, float scale, int argb) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, y, 0f);
        GlStateManager.scale(scale, scale, 1f);
        fr.drawStringWithShadow(text, -fr.getStringWidth(text) / 2f, 0f, argb);
        GlStateManager.popMatrix();
    }
}
