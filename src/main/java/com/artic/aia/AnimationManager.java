package com.artic.aia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Iterator;
import java.util.LinkedList;

public class AnimationManager {
    public static final AnimationManager INSTANCE = new AnimationManager();
    private static final int MAX_WAIT_TICKS = 10;

    private static class Pending {
        final DropEntry entry;
        final String name;
        final Rarity rarity;
        int age;
        Pending(DropEntry e, String n, Rarity r) { entry = e; name = n; rarity = r; }
    }

    private final LinkedList<Pending> pending = new LinkedList<Pending>();
    private final LinkedList<DropAnimation> queue = new LinkedList<DropAnimation>();
    private DropAnimation current;
    private int warmIdx, warmTick;

    public void submit(DropEntry entry, String name, Rarity rarity, boolean immediate) {
        Pending p = new Pending(entry, name, rarity);
        if (!immediate && needsInventory(entry)) pending.add(p);
        else enqueue(build(p));
    }

    private boolean needsInventory(DropEntry e) {
        return e.preferInventory;
    }

    private void enqueue(DropAnimation a) {
        if (queue.size() >= Math.max(1, AIAConfig.queueLimit)) return;
        queue.add(a);
    }

    private DropAnimation build(Pending p) {
        DropEntry e = p.entry;
        ItemLook look = e.look;
        ItemStack st = null;
        VoxelModel vox = null;
        if (e.preferInventory) st = findInInventory(p);
        if (st == null) {
            st = look.stack().copy();
            if (look.mode() == ItemLook.Mode.VOXEL) vox = VoxelModel.get(look);
        }
        st.stackSize = 1;
        SkullTextures.warm(st);
        return new DropAnimation(e, p.name, p.rarity, st, vox);
    }


    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            pending.clear();
            queue.clear();
            current = null;
            warmIdx = 0;
            return;
        }

        if ((++warmTick & 1) == 0 && warmIdx < DropCatalog.ALL.size()) {
            DropEntry d = DropCatalog.ALL.get(warmIdx++);
            if (d.enabled && d.look.hasTexture()) SkullTextures.warm(d.look.stack());
        }

        for (Iterator<Pending> it = pending.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            p.age++;
            if (findInInventory(p) != null || p.age >= MAX_WAIT_TICKS) {
                it.remove();
                enqueue(build(p));
            }
        }

        if (current != null && current.finished(Minecraft.getSystemTime())) current = null;
        if (current == null && !queue.isEmpty()) {
            current = queue.poll();
            SoundPlayer.play(current.rarity);
        }
    }


    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null && AIAConfig.aboveGui) return;
        draw(mc, e.resolution);
    }

    @SubscribeEvent
    public void onScreen(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (!AIAConfig.aboveGui) return;
        Minecraft mc = Minecraft.getMinecraft();
        draw(mc, new ScaledResolution(mc));
    }

    private void draw(Minecraft mc, ScaledResolution sr) {
        DropAnimation a = current;
        if (a == null) return;
        if (a.finished(Minecraft.getSystemTime())) return;
        a.render(mc, sr.getScaledWidth(), sr.getScaledHeight());
    }


    private static String strip(String s) {
        String r = EnumChatFormatting.getTextWithoutFormattingCodes(s);
        return r == null ? "" : r.trim();
    }

    private ItemStack findInInventory(Pending p) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return null;
        ItemStack[] inv = mc.thePlayer.inventory.mainInventory;
        String want = p.name.toLowerCase();
        boolean usePattern = p.entry.pattern != null && !p.entry.generic;
        for (ItemStack st : inv) {
            if (st == null || st.getItem() == null) continue;
            String n = strip(st.getDisplayName());
            String nl = n.toLowerCase();
            if (nl.equals(want) || (want.length() > 3 && nl.contains(want))) return st.copy();
            if (usePattern && p.entry.pattern.matcher(n).find()) return st.copy();
        }
        return null;
    }
}
