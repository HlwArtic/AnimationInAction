package com.artic.aia; import java.util.*;
public class AnimationManager { public static final AnimationManager INSTANCE=new AnimationManager(); public static List<String> got=new ArrayList<>();
 public void submit(DropEntry e,String name,Rarity r,boolean imm){ got.add(e.id+"|"+name+"|"+r); } }
