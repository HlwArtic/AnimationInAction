package com.artic.aia;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;

import java.util.Map;

public final class SkullTextures {
    private SkullTextures() {}

    public static void warm(ItemStack stack) {
        try {
            NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
            if (tag == null || !tag.hasKey("SkullOwner", 10)) return;
            GameProfile profile = NBTUtil.readGameProfileFromNBT(tag.getCompoundTag("SkullOwner"));
            if (profile == null) return;
            Minecraft mc = Minecraft.getMinecraft();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = mc.getSkinManager().loadSkinFromCache(profile);
            MinecraftProfileTexture skin = map.get(MinecraftProfileTexture.Type.SKIN);
            if (skin != null) mc.getSkinManager().loadSkin(skin, MinecraftProfileTexture.Type.SKIN);
        } catch (Throwable ignored) {
        }
    }
}
