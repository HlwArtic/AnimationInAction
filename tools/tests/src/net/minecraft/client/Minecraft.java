package net.minecraft.client; import net.minecraft.entity.player.EntityPlayer; import net.minecraft.client.multiplayer.ServerData;
public class Minecraft { static Minecraft I=new Minecraft(); public EntityPlayer thePlayer=new EntityPlayer(); public ServerData sd=new ServerData();
 public static Minecraft getMinecraft(){return I;} public ServerData getCurrentServerData(){return sd;} }
