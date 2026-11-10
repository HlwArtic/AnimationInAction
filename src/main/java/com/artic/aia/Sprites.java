package com.artic.aia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

final class Sprites {
    private Sprites() {}

    private static final Map<String, String> KNOWN = new HashMap<String, String>();
    static {
        KNOWN.put("minecraft:quartz|0", "items/quartz");
        KNOWN.put("minecraft:ghast_tear|0", "items/ghast_tear");
        KNOWN.put("minecraft:rotten_flesh|0", "items/rotten_flesh");
        KNOWN.put("minecraft:fish|1", "items/fish_salmon_raw");
        KNOWN.put("minecraft:dye|5", "items/dye_powder_purple");
        KNOWN.put("minecraft:stick|0", "items/stick");
        KNOWN.put("minecraft:blaze_rod|0", "items/blaze_rod");
        KNOWN.put("minecraft:fishing_rod|0", "items/fishing_rod_uncast");
        KNOWN.put("minecraft:enchanted_book|0", "items/book_enchanted");
        KNOWN.put("minecraft:iron_sword|0", "items/iron_sword");
        KNOWN.put("minecraft:stone_sword|0", "items/stone_sword");
        KNOWN.put("minecraft:diamond_sword|0", "items/diamond_sword");
        KNOWN.put("minecraft:golden_sword|0", "items/gold_sword");
        KNOWN.put("minecraft:leather_chestplate|0", "items/leather_chestplate");
        KNOWN.put("minecraft:leather_leggings|0", "items/leather_leggings");
        KNOWN.put("minecraft:leather_boots|0", "items/leather_boots");
        KNOWN.put("minecraft:bow|0", "items/bow_standby");
        KNOWN.put("minecraft:diamond_horse_armor|0", "items/diamond_horse_armor");
        KNOWN.put("minecraft:ender_eye|0", "items/ender_eye");
        KNOWN.put("minecraft:firework_charge|0", "items/fireworks_charge");
        KNOWN.put("minecraft:lead|0", "items/lead");
        KNOWN.put("minecraft:writable_book|0", "items/book_writable");
    }

    private static final Map<String, BufferedImage> CACHE = new HashMap<String, BufferedImage>();

    static BufferedImage read(String itemName, int damage, boolean overlay) {
        String key = itemName + "|" + damage + (overlay ? "|overlay" : "");
        BufferedImage hit = CACHE.get(key);
        if (hit != null) return hit;
        BufferedImage img = null;
        try {
            String path = KNOWN.get(itemName + "|" + damage);
            if (path == null) path = fromModel(itemName, damage);
            if (path != null) img = load(overlay ? path + "_overlay" : path);
        } catch (Throwable t) {
            AIAConfig.log("sprite not readable for " + key + ": " + t);
        }
        if (img != null) CACHE.put(key, img);
        return img;
    }

    private static String fromModel(String itemName, int damage) {
        Item item = Item.getByNameOrId(itemName);
        if (item == null) return null;
        IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getItemModel(new ItemStack(item, 1, damage));
        if (model == null || model.getParticleTexture() == null) return null;
        String icon = model.getParticleTexture().getIconName();
        if (icon == null) return null;
        int colon = icon.indexOf(':');
        return colon >= 0 ? icon.substring(colon + 1) : icon;
    }

    private static BufferedImage load(String path) throws Exception {
        ResourceLocation rl = new ResourceLocation("minecraft", "textures/" + path + ".png");
        IResource res = Minecraft.getMinecraft().getResourceManager().getResource(rl);
        InputStream in = res.getInputStream();
        try {
            return ImageIO.read(in);
        } finally {
            in.close();
        }
    }
}
