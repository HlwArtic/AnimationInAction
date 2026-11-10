package net.minecraftforge.common.config; import java.io.File; import java.util.*;
public class Configuration { Map<String,Property> m=new HashMap<>(); public Configuration(File f){}
 public String getString(String n,String c,String d,String cm){ Property p=m.get(c+"/"+n); if(p==null){p=new Property(d);m.put(c+"/"+n,p);} return p.getString(); }
 public Property get(String c,String n,String d){ Property p=m.get(c+"/"+n); if(p==null){p=new Property(d);m.put(c+"/"+n,p);} return p; }
 public void save(){} }
