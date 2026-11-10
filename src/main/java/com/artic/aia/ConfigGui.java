package com.artic.aia;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigGui extends GuiScreen {

    private static final int HEADER_H = 30, SIDE_W = 112, ROW_H = 26, RB_W = 80, TB_W = 34, FLAKES = 90, SL_W = 100;
    private static final int C_TEXT = 0xFFEAF7FF, C_DIM = 0xFF6F86A6, C_ICE = 0xFF8FD3FF, C_ACCENT = 0xFF4FC3F7;

    private abstract static class Setting {
        final String label;
        Setting(String label) { this.label = label; }
    }

    private static class Toggle extends Setting {
        final BooleanSupplier get;
        final Consumer<Boolean> set;
        Toggle(String l, BooleanSupplier g, Consumer<Boolean> s) { super(l); get = g; set = s; }
    }

    private static class Slider extends Setting {
        final float min, max, step;
        final String unit;
        final Supplier<Float> get;
        final Consumer<Float> set;
        Slider(String l, float min, float max, float step, String unit, Supplier<Float> g, Consumer<Float> s) {
            super(l); this.min = min; this.max = max; this.step = step; this.unit = unit; get = g; set = s;
        }
        String fmt(float v) {
            return (step >= 1f ? String.valueOf(Math.round(v)) : String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "")) + unit;
        }
    }

    private static class Cycle extends Setting {
        final Supplier<String> value;
        final Consumer<Boolean> step;
        Cycle(String l, Supplier<String> v, Consumer<Boolean> s) { super(l); value = v; step = s; }
    }

    private static class Heading extends Setting {
        Heading(String l) { super(l); }
    }

    private static class Action extends Setting {
        final String button;
        final Runnable run;
        Action(String l, String b, Runnable r) { super(l); button = b; run = r; }
    }

    private final Category[] cats = Category.values();
    private final List<Setting> settings = new ArrayList<Setting>();
    private final float[] fx = new float[FLAKES], fy = new float[FLAKES], fs = new float[FLAKES], fp = new float[FLAKES];
    private final Random rnd = new Random();
    private int tab, px, py, pw, ph, dragging = -1;
    private float scroll, scrollTarget;
    private long lastNanos = System.nanoTime();

    public ConfigGui() {
        settings.add(new Heading("Animation"));
        settings.add(new Toggle("Animations enabled", () -> AIAConfig.master, v -> AIAConfig.master = v));
        settings.add(new Toggle("Screen flash", () -> AIAConfig.flash, v -> AIAConfig.flash = v));
        settings.add(new Toggle("Dim background", () -> AIAConfig.dim, v -> AIAConfig.dim = v));
        settings.add(new Slider("Animation length", 3f, 10f, 0.5f, "s", () -> AIAConfig.durationSec, v -> AIAConfig.durationSec = v));
        settings.add(new Slider("Item size", 3f, 12f, 0.5f, "", () -> AIAConfig.size, v -> AIAConfig.size = v));
        settings.add(new Slider("Spin speed", 30f, 360f, 10f, " deg/s", () -> AIAConfig.spinSpeed, v -> AIAConfig.spinSpeed = v));
        settings.add(new Slider("Ray intensity", 0.2f, 2f, 0.1f, "x", () -> AIAConfig.rayIntensity, v -> AIAConfig.rayIntensity = v));
        settings.add(new Slider("Queue limit", 1f, 10f, 1f, "", () -> (float) AIAConfig.queueLimit, v -> AIAConfig.queueLimit = Math.round(v)));
        settings.add(new Action("Preview a random drop", "Play", new Runnable() {
            @Override public void run() {
                List<DropEntry> pool = new ArrayList<DropEntry>();
                for (DropEntry d : DropCatalog.ALL) if (!d.generic) pool.add(d);
                DropEntry d = pool.get(rnd.nextInt(pool.size()));
                AnimationManager.INSTANCE.submit(d, d.name, d.effectiveDefault(), true);
            }
        }));
        settings.add(new Heading("Sound"));
        settings.add(new Toggle("Play sound", () -> AIAConfig.sound, v -> AIAConfig.sound = v));
        settings.add(new Slider("Sound volume", 0f, 1f, 0.05f, "", () -> AIAConfig.volume, v -> AIAConfig.volume = v));
        settings.add(new Action("Test the sound", "Play", new Runnable() {
            @Override public void run() { SoundPlayer.play(Rarity.LEGENDARY); }
        }));
        settings.add(new Heading("Detection & display"));
        settings.add(new Toggle("Show over open menus", () -> AIAConfig.aboveGui, v -> AIAConfig.aboveGui = v));
        settings.add(new Toggle("Strict anti-spoof check", () -> AIAConfig.strict, v -> AIAConfig.strict = v));
        settings.add(new Toggle("Debug: log chat lines", () -> AIAConfig.debug, v -> AIAConfig.debug = v));
    }


    private int cX() { return px + SIDE_W + 8; }
    private int cW() { return pw - SIDE_W - 14; }
    private int listTop() { return py + HEADER_H + 30; }
    private int listBottom() { return py + ph - 8; }
    private int tabH() { return Math.max(14, Math.min(22, (ph - HEADER_H - 12) / cats.length)); }
    private int rowCount() { return tab == 0 ? settings.size() : DropCatalog.rows(cats[tab]).size(); }
    private float maxScroll() { return Math.max(0, rowCount() * ROW_H - (listBottom() - listTop())); }
    private int rowY(int i) { return listTop() + i * ROW_H - (int) scroll; }
    private int rowRight() { return cX() + cW() - 6; }
    private int sliderX() { return rowRight() - 8 - 40 - SL_W; }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void initGui() {
        pw = Math.min(width - 12, 480);
        ph = Math.min(height - 12, 312);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        for (int i = 0; i < FLAKES; i++) {
            fx[i] = rnd.nextFloat() * width;
            fy[i] = rnd.nextFloat() * height;
            fs[i] = 10f + rnd.nextFloat() * 32f;
            fp[i] = rnd.nextFloat() * 6.28f;
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void onGuiClosed() {
        AIAConfig.save();
    }

    private void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }


    @Override
    public void drawScreen(int mx, int my, float pt) {
        long nowN = System.nanoTime();
        float dt = Math.min(0.1f, (nowN - lastNanos) / 1e9f);
        lastNanos = nowN;
        scrollTarget = Math.max(0f, Math.min(maxScroll(), scrollTarget));
        scroll += (scrollTarget - scroll) * Math.min(1f, dt * 14f);
        if (Math.abs(scrollTarget - scroll) < 0.05f) scroll = scrollTarget;

        drawGradientRect(0, 0, width, height, 0xE0081428, 0xF0204A78);
        stepSnow(dt);
        drawSnow(false);

        drawRect(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF9ADCFF);
        drawGradientRect(px, py, px + pw, py + ph, 0xF011264A, 0xF00A1730);
        drawRect(px + 1, py + 1, px + pw - 1, py + 2, 0x66FFFFFF);

        drawHeader(mx, my);
        drawSidebar(mx, my);
        drawContent(mx, my);
        drawSnow(true);
        super.drawScreen(mx, my, pt);
    }

    private void drawHeader(int mx, int my) {
        drawGradientRect(px + 1, py + 2, px + pw - 1, py + HEADER_H, 0xFF1C4D86, 0xFF0F2A52);
        for (int i = 0, x = px + 6; x < px + pw - 10; i++, x += 13) {
            int len = 4 + ((i * 7) % 5);
            for (int r = 0; r < len; r++) {
                drawRect(x + r / 2, py + HEADER_H + r, x + 7 - r / 2, py + HEADER_H + r + 1, 0xB0EAF7FF);
            }
        }
        drawSnowflake(px + 14, py + 15, 5, 0xFFEAF7FF);

        String title = "Animation in Action";
        GlStateManager.pushMatrix();
        GlStateManager.translate(px + 26, py + 8, 0);
        GlStateManager.scale(1.5f, 1.5f, 1f);
        fontRendererObj.drawStringWithShadow(title, 0, 0, C_TEXT);
        GlStateManager.popMatrix();
        int tw = (int) (fontRendererObj.getStringWidth(title) * 1.5f);
        fontRendererObj.drawStringWithShadow("\u00a7omade by Artic ^-^", px + 26 + tw + 8, py + 14, C_ICE);

        boolean hov = in(mx, my, px + pw - 24, py + 7, 16, 16);
        drawRect(px + pw - 24, py + 7, px + pw - 8, py + 23, hov ? 0xFFE05555 : 0x55FFFFFF);
        fontRendererObj.drawStringWithShadow("x", px + pw - 18, py + 11, 0xFFFFFFFF);

        int sx = px + pw - 24 - 8 - 26;
        drawSwitch(sx, py + 11, AIAConfig.master);
        String lab = "Enabled";
        fontRendererObj.drawStringWithShadow(lab, sx - fontRendererObj.getStringWidth(lab) - 5, py + 13, C_TEXT);
    }

    private void drawSidebar(int mx, int my) {
        drawRect(px + 3, py + HEADER_H + 3, px + SIDE_W + 1, py + ph - 3, 0x66050E1F);
        int th = tabH();
        for (int i = 0; i < cats.length; i++) {
            int ty = py + HEADER_H + 6 + i * th;
            boolean sel = i == tab, hov = in(mx, my, px + 4, ty, SIDE_W - 4, th - 1);
            if (sel) {
                drawGradientRect(px + 4, ty, px + SIDE_W, ty + th - 1, 0xFF2F7FC0, 0xFF1D568F);
                drawRect(px + 4, ty, px + 6, ty + th - 1, 0xFFFFFFFF);
            } else if (hov) {
                drawRect(px + 4, ty, px + SIDE_W, ty + th - 1, 0x33FFFFFF);
            }
            int tx = px + 10;
            if (th >= 18) {
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(cats[i].icon, tx, ty + (th - 16) / 2);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableLighting();
                tx += 20;
            }
            String t = fontRendererObj.trimStringToWidth(cats[i].title, SIDE_W - (tx - px) - 3);
            fontRendererObj.drawStringWithShadow(t, tx, ty + (th - 8) / 2, sel ? 0xFFFFFFFF : 0xFFB9D6F0);
        }
    }

    private void drawContent(int mx, int my) {
        int cx = cX(), cw = cW();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, py + HEADER_H + 8, 0);
        GlStateManager.scale(1.25f, 1.25f, 1f);
        fontRendererObj.drawStringWithShadow(cats[tab].title, 0, 0, C_TEXT);
        GlStateManager.popMatrix();

        if (tab == 0) {
            fontRendererObj.drawStringWithShadow("How animations look and trigger", cx, py + HEADER_H + 21, C_ICE);
        } else {
            List<DropCatalog.Row> list = DropCatalog.rows(cats[tab]);
            int on = 0, total = 0;
            for (DropCatalog.Row r : list) {
                if (r.isHeader()) continue;
                total++;
                if (r.enabled()) on++;
            }
            fontRendererObj.drawStringWithShadow(on + "/" + total + " animations enabled  -  click a row to toggle, rarity to cycle",
                cx, py + HEADER_H + 21, C_ICE);
            int by = py + HEADER_H + 6;
            drawBtn(rowRight() - 62 * 2 - 4, by, 62, 13, "Enable all", C_TEXT, in(mx, my, rowRight() - 62 * 2 - 4, by, 62, 13));
            drawBtn(rowRight() - 62, by, 62, 13, "Disable all", C_TEXT, in(mx, my, rowRight() - 62, by, 62, 13));
        }

        int top = listTop(), bottom = listBottom();
        scissor(cx - 2, top, cw + 2, bottom - top);
        if (tab == 0) drawSettingRows(mx, my, top, bottom);
        else drawDropRows(mx, my, top, bottom);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        float ms = maxScroll();
        if (ms > 0) {
            int h = bottom - top;
            int thumb = Math.max(16, (int) (h * (float) h / (rowCount() * ROW_H)));
            int ty = top + (int) ((h - thumb) * (scroll / ms));
            drawRect(cx + cw - 4, top, cx + cw - 1, bottom, 0x33FFFFFF);
            drawRect(cx + cw - 4, ty, cx + cw - 1, ty + thumb, 0xFF9ADCFF);
        }

    }

    private void drawDropRows(int mx, int my, int top, int bottom) {
        List<DropCatalog.Row> list = DropCatalog.rows(cats[tab]);
        int cx = cX(), cw = cW();
        for (int i = 0; i < list.size(); i++) {
            int y = rowY(i);
            if (y + ROW_H < top || y > bottom) continue;
            DropCatalog.Row row = list.get(i);
            if (row.isHeader()) {
                fontRendererObj.drawStringWithShadow(row.label.toUpperCase(), cx + 4, y + 9, C_ICE);
                drawRect(cx, y + ROW_H - 6, rowRight(), y + ROW_H - 5, 0x663A7BB5);
                continue;
            }
            DropEntry d = row.lead();
            boolean on = row.enabled();
            boolean hov = in(mx, my, cx, y, cw - 6, ROW_H - 2) && my >= top && my < bottom;
            drawRect(cx, y, rowRight(), y + ROW_H - 2, hov ? 0x40FFFFFF : ((i & 1) == 0 ? 0x26FFFFFF : 0x14FFFFFF));
            drawSwitch(cx + 8, y + 7, on);

            drawRowIcon(d, cx + 40, y + 4);

            int nameCol = on ? (0xFF000000 | d.effectiveDefault().primary) : C_DIM;
            int tbX = rowRight() - TB_W - 4, rbX = tbX - RB_W - 4;
            int textW = rbX - (cx + 62) - 6;
            String nm = fontRendererObj.trimStringToWidth(row.label, textW);
            if (row.summary != null) {
                fontRendererObj.drawStringWithShadow(nm, cx + 62, y + 3, nameCol);
                fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(row.summary, textW), cx + 62, y + 13, 0xFF8FA6C4);
            } else {
                fontRendererObj.drawStringWithShadow(nm, cx + 62, y + 9, nameCol);
            }

            String rl = d.override == null ? "AUTO" : d.override.label;
            int rc = d.override == null ? C_ICE : (0xFF000000 | d.override.primary);
            drawBtn(rbX, y + 5, RB_W, 14, rl, rc, in(mx, my, rbX, y + 5, RB_W, 14));
            drawBtn(tbX, y + 5, TB_W, 14, "Test", C_TEXT, in(mx, my, tbX, y + 5, TB_W, 14));
        }
    }

    private void drawSettingRows(int mx, int my, int top, int bottom) {
        int cx = cX(), cw = cW();
        for (int i = 0; i < settings.size(); i++) {
            int y = rowY(i);
            if (y + ROW_H < top || y > bottom) continue;
            Setting s = settings.get(i);
            if (s instanceof Heading) {
                fontRendererObj.drawStringWithShadow(s.label.toUpperCase(), cx + 4, y + 9, C_ICE);
                drawRect(cx, y + ROW_H - 6, rowRight(), y + ROW_H - 5, 0x663A7BB5);
                continue;
            }
            boolean hov = in(mx, my, cx, y, cw - 6, ROW_H - 2) && my >= top && my < bottom;
            drawRect(cx, y, rowRight(), y + ROW_H - 2, hov ? 0x40FFFFFF : ((i & 1) == 0 ? 0x26FFFFFF : 0x14FFFFFF));
            fontRendererObj.drawStringWithShadow(s.label, cx + 8, y + 9, C_TEXT);
            if (s instanceof Toggle) {
                drawSwitch(rowRight() - 8 - 26, y + 7, ((Toggle) s).get.getAsBoolean());
            } else if (s instanceof Slider) {
                Slider sl = (Slider) s;
                float v = sl.get.get();
                float p = (v - sl.min) / (sl.max - sl.min);
                int tx = sliderX();
                drawRect(tx, y + 11, tx + SL_W, y + 13, 0xFF33445F);
                drawRect(tx, y + 11, tx + (int) (SL_W * p), y + 13, C_ACCENT);
                int kx = tx + (int) (SL_W * p);
                drawRect(kx - 2, y + 7, kx + 2, y + 17, 0xFFFFFFFF);
                fontRendererObj.drawStringWithShadow(sl.fmt(v), tx + SL_W + 8, y + 9, C_ICE);
            } else if (s instanceof Cycle) {
                int bw = 130, bx = rowRight() - 8 - bw;
                String v = fontRendererObj.trimStringToWidth(((Cycle) s).value.get(), bw - 8);
                drawBtn(bx, y + 5, bw, 14, v, C_ICE, in(mx, my, bx, y + 5, bw, 14));
            } else if (s instanceof Action) {
                Action a = (Action) s;
                int bx = rowRight() - 8 - 44;
                drawBtn(bx, y + 5, 44, 14, a.button, C_TEXT, in(mx, my, bx, y + 5, 44, 14));
            }
        }
    }


    private void drawRowIcon(DropEntry d, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(d.iconStack(), x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
    }

    private void drawSwitch(int x, int y, boolean on) {
        drawRect(x - 1, y - 1, x + 27, y + 13, on ? 0xFF9ADCFF : 0xFF4A5C7A);
        drawRect(x, y, x + 26, y + 12, on ? 0xFF2BA3E0 : 0xFF26344D);
        int kx = on ? x + 15 : x + 1;
        drawRect(kx, y + 1, kx + 10, y + 11, 0xFFFFFFFF);
    }

    private void drawBtn(int x, int y, int w, int h, String label, int color, boolean hover) {
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, hover ? 0xFFBFE8FF : 0xFF6FB8F0);
        drawGradientRect(x, y, x + w, y + h, hover ? 0xFF2C6FAE : 0xFF1B4C80, hover ? 0xFF1F548A : 0xFF123760);
        int tw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, x + (w - tw) / 2f, y + (h - 8) / 2f, color);
    }

    private void drawSnowflake(int cx, int cy, int r, int color) {
        drawRect(cx - r, cy, cx + r + 1, cy + 1, color);
        drawRect(cx, cy - r, cx + 1, cy + r + 1, color);
        for (int k = -(r - 1); k <= r - 1; k++) {
            drawRect(cx + k, cy + k, cx + k + 1, cy + k + 1, color);
            drawRect(cx + k, cy - k, cx + k + 1, cy - k + 1, color);
        }
    }

    private void stepSnow(float dt) {
        double t = System.nanoTime() / 1e9;
        for (int i = 0; i < FLAKES; i++) {
            fy[i] += fs[i] * dt;
            fx[i] += (float) Math.sin(fp[i] + t * 0.8) * 10f * dt;
            if (fy[i] > height + 2) {
                fy[i] = -3;
                fx[i] = rnd.nextFloat() * width;
            }
        }
    }

    private void drawSnow(boolean front) {
        for (int i = 0; i < FLAKES; i++) {
            if (front && i % 4 != 0) continue;
            int size = fs[i] > 28f ? 2 : 1;
            int a = front ? 110 : 70 + (int) fs[i] * 3;
            int x = (int) fx[i], y = (int) fy[i];
            drawRect(x, y, x + size, y + size, (Math.min(a, 255) << 24) | 0xFFFFFF);
        }
    }

    private void scissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, w * f, h * f);
    }


    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scrollTarget -= Math.signum(d) * ROW_H * 1.5f;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (AnimationInAction.openKey != null && key == AnimationInAction.openKey.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);

        if (in(mx, my, px + pw - 24, py + 7, 16, 16)) { click(); mc.displayGuiScreen(null); return; }
        if (in(mx, my, px + pw - 24 - 8 - 26, py + 11, 26, 12)) { click(); AIAConfig.master = !AIAConfig.master; return; }

        int th = tabH();
        for (int i = 0; i < cats.length; i++) {
            if (in(mx, my, px + 4, py + HEADER_H + 6 + i * th, SIDE_W - 4, th - 1)) {
                if (tab != i) { tab = i; scroll = scrollTarget = 0; dragging = -1; }
                click();
                return;
            }
        }

        if (tab != 0) {
            List<DropCatalog.Row> list = DropCatalog.rows(cats[tab]);
            int by = py + HEADER_H + 6;
            if (in(mx, my, rowRight() - 62 * 2 - 4, by, 62, 13)) { for (DropCatalog.Row r : list) r.setEnabled(true); click(); return; }
            if (in(mx, my, rowRight() - 62, by, 62, 13)) { for (DropCatalog.Row r : list) r.setEnabled(false); click(); return; }
        }

        if (my < listTop() || my >= listBottom() || mx < cX() || mx >= rowRight()) return;
        int idx = (my - listTop() + (int) scroll) / ROW_H;
        if (idx < 0 || idx >= rowCount()) return;
        int y = rowY(idx);

        if (tab == 0) {
            Setting s = settings.get(idx);
            if (s instanceof Toggle) {
                Toggle t = (Toggle) s;
                t.set.accept(!t.get.getAsBoolean());
                click();
            } else if (s instanceof Slider) {
                if (mx >= sliderX() - 4 && mx <= sliderX() + SL_W + 4) { dragging = idx; applySlider((Slider) s, mx); }
            } else if (s instanceof Cycle) {
                int bw = 130, bx = rowRight() - 8 - bw;
                if (in(mx, my, bx, y + 5, bw, 14)) { click(); ((Cycle) s).step.accept(btn != 1); }
            } else if (s instanceof Action) {
                int bx = rowRight() - 8 - 44;
                if (in(mx, my, bx, y + 5, 44, 14)) { click(); ((Action) s).run.run(); }
            }
        } else {
            DropCatalog.Row row = DropCatalog.rows(cats[tab]).get(idx);
            if (row.isHeader()) return;
            int tbX = rowRight() - TB_W - 4, rbX = tbX - RB_W - 4;
            if (in(mx, my, tbX, y + 5, TB_W, 14)) {
                click();
                DropEntry t = row.previewTarget();
                AnimationManager.INSTANCE.submit(t, t.name, t.effectiveDefault(), true);
            } else if (in(mx, my, rbX, y + 5, RB_W, 14)) {
                click();
                row.cycleRarity(btn != 1);
            } else {
                click();
                row.setEnabled(!row.enabled());
            }
        }
    }

    private void applySlider(Slider s, int mx) {
        float p = Math.max(0f, Math.min(1f, (mx - sliderX()) / (float) SL_W));
        float v = s.min + p * (s.max - s.min);
        v = s.min + Math.round((v - s.min) / s.step) * s.step;
        s.set.accept(Math.max(s.min, Math.min(s.max, v)));
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        super.mouseClickMove(mx, my, btn, time);
        if (tab == 0 && dragging >= 0 && dragging < settings.size() && settings.get(dragging) instanceof Slider) {
            applySlider((Slider) settings.get(dragging), mx);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        if (dragging >= 0) { dragging = -1; AIAConfig.save(); }
    }
}
