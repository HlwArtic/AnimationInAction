package com.artic.aia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class CustomSkulls implements ItemLook.CustomProvider {
    private final Map<String, String> values = new HashMap<String, String>();

    @Override
    public String textureValue(String spec) {
        String cached = values.get(spec);
        if (cached != null) return cached;
        String v = null;
        try {
            v = create(spec);
        } catch (Throwable t) {
            AIAConfig.log("custom head failed (" + spec + "): " + t);
        }
        if (v != null) values.put(spec, v);
        return v;
    }

    private String create(String spec) throws Exception {
        BufferedImage img;
        if (spec.startsWith("file:")) img = readBundled(spec.substring(5));
        else if (spec.startsWith("sprite:")) img = fromSprite(spec.substring(7));
        else img = null;
        if (img == null) return null;

        String name = "aia" + Integer.toHexString(spec.hashCode()) + Integer.toHexString(spec.length());
        Minecraft.getMinecraft().getTextureManager().loadTexture(new ResourceLocation("skins/" + name), new DynamicTexture(img));
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + name + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes("UTF-8"));
    }

    private static BufferedImage readBundled(String name) throws Exception {
        InputStream in = CustomSkulls.class.getResourceAsStream("/assets/animationinaction/skulls/" + name + ".png");
        if (in == null) return null;
        try {
            return ImageIO.read(in);
        } finally {
            in.close();
        }
    }

    private static BufferedImage fromSprite(String spec) {
        String[] p = spec.split("\\|");
        int damage = 0;
        try { if (p.length > 1) damage = Integer.parseInt(p[1].trim()); } catch (NumberFormatException ignored) {}
        BufferedImage sprite = Sprites.read(p[0], damage, false);
        if (sprite == null) return null;

        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                int c = sprite.getRGB(x, y);
                if (((c >>> 24) & 255) > 128) {
                    r += (c >> 16) & 255;
                    g += (c >> 8) & 255;
                    b += c & 255;
                    n++;
                }
            }
        }
        Color bg = n == 0 ? new Color(0x303030) : new Color((int) (r / n * 0.5), (int) (g / n * 0.5), (int) (b / n * 0.5));

        BufferedImage skin = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = skin.createGraphics();
        gr.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int[][] faces = {{16, 0}, {32, 0}, {0, 16}, {16, 16}, {32, 16}, {48, 16}};
        for (int[] f : faces) {
            gr.setColor(bg);
            gr.fillRect(f[0], f[1], 16, 16);
            gr.drawImage(sprite, f[0], f[1], 16, 16, null);
        }
        gr.dispose();
        return skin;
    }
}
