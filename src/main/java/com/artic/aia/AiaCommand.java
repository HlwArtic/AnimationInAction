package com.artic.aia;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;
import java.util.List;

public class AiaCommand extends CommandBase {
    @Override public String getCommandName() { return "aia"; }
    @Override public List<String> getCommandAliases() { return Arrays.asList("animationinaction"); }
    @Override public String getCommandUsage(ICommandSender sender) { return "/aia [test <id> [rarity] | sound | debug]"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    private static void say(String s) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b[Animation in Action] \u00a7f" + s));
        }
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            AnimationInAction.openGuiNextTick = true;
            return;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("test")) {
            DropEntry d = args.length > 1 ? DropCatalog.byId(args[1]) : DropCatalog.byId("warden_heart");
            if (d == null) { say("\u00a7cUnknown id. See config/animationinaction/icons/README.txt for all ids."); return; }
            Rarity r = d.override != null ? d.override : d.defaultRarity;
            if (args.length > 2) {
                for (Rarity x : Rarity.values()) if (x.name().equalsIgnoreCase(args[2])) r = x;
            }
            AnimationManager.INSTANCE.submit(d, d.name, r, true);
        } else if (sub.equals("debug")) {
            AIAConfig.debug = !AIAConfig.debug;
            AIAConfig.save();
            say("Chat logging is now " + (AIAConfig.debug ? "\u00a7aON" : "\u00a7cOFF")
                + "\u00a7f -> " + AIAConfig.logFile.getPath());
        } else if (sub.equals("sound")) {
            SoundPlayer.play(Rarity.LEGENDARY);
        } else {
            say(getCommandUsage(sender));
        }
    }
}
