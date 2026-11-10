package net.minecraft.item; import net.minecraft.nbt.NBTTagCompound;
public class ItemStack { public int stackSize=1; public Item item; public NBTTagCompound tag; public int meta;
 public ItemStack(Item i){item=i;} public ItemStack(Item i,int n,int m){item=i;meta=m;}
 public ItemStack copy(){ItemStack c=new ItemStack(item,1,meta);c.tag=tag;return c;} public void setTagCompound(NBTTagCompound t){tag=t;} public NBTTagCompound getTagCompound(){return tag;} }
