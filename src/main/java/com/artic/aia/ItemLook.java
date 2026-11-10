package com.artic.aia;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.UUID;

public final class ItemLook {

    public interface CustomProvider {
        String textureValue(String spec);
    }

    public static CustomProvider provider;

    public enum Mode { HEAD, VOXEL }

    public final String skull;
    public final String custom;
    public final String itemName;
    public final int damage;
    public final boolean glint;
    public final int tint;
    private final boolean voxel;
    private boolean headFailed;
    private ItemStack cached;

    private ItemLook(String skull, String custom, String itemName, int damage, boolean glint, int tint, boolean voxel) {
        this.skull = skull;
        this.custom = custom;
        this.itemName = itemName;
        this.damage = damage;
        this.glint = glint;
        this.tint = tint;
        this.voxel = voxel;
    }

    public static ItemLook parse(String kind, String data) {
        data = data == null ? "" : data.trim();
        if ("skull".equals(kind)) return new ItemLook(data, null, null, 0, false, -1, false);
        if ("custom".equals(kind)) {
            String item = null;
            int dmg = 0;
            if (data.startsWith("sprite:")) {
                String[] p = data.substring(7).split("\\|");
                item = p[0];
                try { if (p.length > 1) dmg = Integer.parseInt(p[1].trim()); } catch (NumberFormatException ignored) {}
            }
            return new ItemLook(null, data, item, dmg, false, -1, false);
        }
        String[] p = data.split("\\|", -1);
        String name = p.length > 0 && !p[0].isEmpty() ? p[0] : "minecraft:stick";
        int dmg = 0;
        try { if (p.length > 1) dmg = Integer.parseInt(p[1].trim()); } catch (NumberFormatException ignored) {}
        boolean glint = p.length > 2 && "1".equals(p[2].trim());
        int tint = -1;
        try { if (p.length > 3 && !p[3].trim().isEmpty()) tint = Integer.parseInt(p[3].trim(), 16); } catch (NumberFormatException ignored) {}
        return new ItemLook(null, null, name, dmg, glint, tint, true);
    }

    public boolean hasTexture() { return (skull != null && !skull.isEmpty()) || custom != null; }

    public Mode mode() {
        stack();
        if (voxel) return Mode.VOXEL;
        if (custom != null && headFailed) return Mode.VOXEL;
        return Mode.HEAD;
    }

    public ItemStack stack() {
        if (cached == null) cached = build();
        return cached;
    }

    private ItemStack build() {
        if (skull != null) return head(skull);
        if (custom != null) {
            String value = provider == null ? null : provider.textureValue(custom);
            if (value != null) return head(value);
            headFailed = true;
            if (itemName != null) return vanilla();
            return new ItemStack(Items.skull, 1, 3);
        }
        return vanilla();
    }

    private static ItemStack head(String value) {
        ItemStack s = new ItemStack(Items.skull, 1, 3);
        if (value == null || value.isEmpty()) return s;
        NBTTagCompound tex = new NBTTagCompound();
        tex.setString("Value", value);
        NBTTagList list = new NBTTagList();
        list.appendTag(tex);
        NBTTagCompound props = new NBTTagCompound();
        props.setTag("textures", list);
        NBTTagCompound owner = new NBTTagCompound();
        owner.setString("Id", UUID.nameUUIDFromBytes(value.getBytes()).toString());
        owner.setTag("Properties", props);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("SkullOwner", owner);
        s.setTagCompound(tag);
        return s;
    }

    private ItemStack vanilla() {
        Item it = Item.getByNameOrId(itemName);
        if (it == null) it = Items.stick;
        ItemStack s = new ItemStack(it, 1, damage);
        NBTTagCompound tag = null;
        if (glint) {
            NBTTagCompound e = new NBTTagCompound();
            e.setShort("id", (short) 34);
            e.setShort("lvl", (short) 1);
            NBTTagList ench = new NBTTagList();
            ench.appendTag(e);
            tag = new NBTTagCompound();
            tag.setTag("ench", ench);
        }
        if (tint >= 0) {
            NBTTagCompound display = new NBTTagCompound();
            display.setInteger("color", tint);
            if (tag == null) tag = new NBTTagCompound();
            tag.setTag("display", display);
        }
        if (tag != null) s.setTagCompound(tag);
        return s;
    }
}
