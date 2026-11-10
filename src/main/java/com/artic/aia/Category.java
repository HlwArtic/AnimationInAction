package com.artic.aia;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public enum Category {
    GENERAL("General", new ItemStack(Items.snowball)),
    REVENANT("Revenant Slayer", new ItemStack(Items.rotten_flesh)),
    SVEN("Sven Slayer", new ItemStack(Items.bone)),
    TARANTULA("Tarantula Slayer", new ItemStack(Items.spider_eye)),
    ENDERMAN("Ender Slayer", new ItemStack(Items.ender_pearl)),
    DEMONLORD("Inferno Slayer", new ItemStack(Items.blaze_rod)),
    DIANA("Diana", new ItemStack(Items.emerald)),
    DUNGEONS("Dungeon Chests", new ItemStack(Items.skull, 1, 1)),
    DRAGONS("Dragons", new ItemStack(Items.skull, 1, 5)),
    FISHING("Fishing", new ItemStack(Items.fishing_rod)),
    FARMING("Farming", new ItemStack(Items.diamond_hoe)),
    MINING("Mining", new ItemStack(Items.diamond_pickaxe)),
    DYES("Dyes", new ItemStack(Items.dye, 1, 12)),
    PETS("Pets", new ItemStack(Items.lead)),
    MISC("Misc", new ItemStack(Items.nether_star));

    public final String title;
    public final ItemStack icon;

    Category(String title, ItemStack icon) {
        this.title = title;
        this.icon = icon;
    }
}
