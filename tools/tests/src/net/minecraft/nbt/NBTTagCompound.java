package net.minecraft.nbt; import java.util.*;
public class NBTTagCompound { public Map<String,Object> m=new HashMap<>(); public void setString(String k,String v){m.put(k,v);} public void setShort(String k,short v){m.put(k,v);} public void setInteger(String k,int v){m.put(k,v);} public void setTag(String k,Object v){m.put(k,v);} public Object get(String k){return m.get(k);} }
