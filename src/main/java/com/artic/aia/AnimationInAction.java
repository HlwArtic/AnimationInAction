package com.artic.aia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = AnimationInAction.MODID, name = AnimationInAction.NAME, version = AnimationInAction.VERSION,
     clientSideOnly = true, acceptableRemoteVersions = "*", acceptedMinecraftVersions = "[1.8.9]")
public class AnimationInAction {
    public static final String MODID = "animationinaction";
    public static final String NAME = "Animation in Action";
    public static final String VERSION = "1.0.0";

    public static KeyBinding openKey;
    public static boolean openGuiNextTick;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        AIAConfig.init(e.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        ItemLook.provider = new CustomSkulls();
        DropCatalog.init();
        SoundPlayer.init(AIAConfig.dir);

        openKey = new KeyBinding("Open Animation in Action", Keyboard.KEY_RSHIFT, "Animation in Action");
        ClientRegistry.registerKeyBinding(openKey);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(AnimationManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new ChatDetector());
        ClientCommandHandler.instance.registerCommand(new AiaCommand());
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (openKey != null && openKey.isPressed()) openGuiNextTick = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && openGuiNextTick) {
            openGuiNextTick = false;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null || !(mc.currentScreen instanceof ConfigGui)) {
                mc.displayGuiScreen(new ConfigGui());
            }
        }
    }
}
