package com.artic.aia;

import net.minecraft.item.ItemStack;

import java.util.regex.Pattern;

public class DropEntry {
    public final Category category;
    public final String id;
    public final String name;
    public final Pattern pattern;
    public final Rarity defaultRarity;
    public final ItemLook look;

    public String[] headers;
    public boolean book;
    public boolean preferInventory;
    public boolean generic;
    public boolean fixedRarity;
    public String section;
    public String group;

    public boolean enabled = true;
    public Rarity override;

    public DropEntry(Category category, String id, String name, Pattern pattern, Rarity defaultRarity, ItemLook look) {
        this.category = category;
        this.id = id;
        this.name = name;
        this.pattern = pattern;
        this.defaultRarity = defaultRarity;
        this.look = look;
    }

    public boolean matches(String header, String text) {
        if (headers != null) {
            boolean ok = false;
            for (String h : headers) {
                if (h.equalsIgnoreCase(header)) { ok = true; break; }
            }
            if (!ok) return false;
        }
        return pattern == null || pattern.matcher(text).find();
    }

    public ItemStack iconStack() { return look.stack(); }

    public Rarity effectiveDefault() { return override != null ? override : defaultRarity; }

    public void cycleRarity(boolean forward) {
        Rarity[] all = Rarity.values();
        int idx = override == null ? -1 : override.ordinal();
        idx += forward ? 1 : -1;
        if (idx >= all.length) idx = -1;
        if (idx < -1) idx = all.length - 1;
        override = idx < 0 ? null : all[idx];
    }
}
