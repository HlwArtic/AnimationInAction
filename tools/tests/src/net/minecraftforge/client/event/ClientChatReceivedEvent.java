package net.minecraftforge.client.event; import net.minecraft.util.IChatComponent;
public class ClientChatReceivedEvent { public IChatComponent message; public final byte type; public ClientChatReceivedEvent(byte t, IChatComponent m){type=t;message=m;} }
