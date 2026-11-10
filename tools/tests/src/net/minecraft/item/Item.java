package net.minecraft.item; public class Item { public static Item getByNameOrId(String s){ return s!=null && s.startsWith("minecraft:") ? new Item() : null; } }
