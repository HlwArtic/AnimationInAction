package com.artic.aia;

import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import java.io.File;
import java.util.*;

public class Harness {
    static int pass = 0, fail = 0;
    static ChatDetector det = new ChatDetector();
    static final String P = "\u00a7";
    static int seq = 0;

    static void feed(byte type, final String line) {
        det.onChat(new ClientChatReceivedEvent(type, new IChatComponent() { public String getFormattedText() { return line; } }));
    }
    static void ok(boolean good, String label, String detail) {
        if (good) pass++; else fail++;
        System.out.println((good ? "PASS  " : "FAIL  ") + label + (good ? "" : "\n        " + detail));
    }
    static String line(String header, char colour, String item) {
        return P + "6" + P + "l" + header + " " + P + "r" + P + colour + item + " " + P + "b(+" + (100 + (seq++)) + "% \u272F Magic Find)";
    }
    static void check(String label, String line, String expect) {
        AnimationManager.got.clear();
        feed((byte) 0, line);
        String got = AnimationManager.got.isEmpty() ? null : AnimationManager.got.get(0);
        ok(Objects.equals(expect, got), label, "expected: " + expect + "\n        got:      " + got);
    }
    static String slug(String n) { return n.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", ""); }

    public static void main(String[] a) throws Exception {
        File d = new File("/tmp/aiatest/cfg"); d.mkdirs();
        AIAConfig.init(d);
        DropCatalog.init();
        System.out.println("catalog: " + DropCatalog.ALL.size() + " entries");
        ok(DropCatalog.ALL.size() == 150, "150 entries in total", "got " + DropCatalog.ALL.size());
        for (Category c : Category.values()) System.out.println(String.format("  %-10s %2d entries, %2d GUI rows", c, DropCatalog.byCategory(c).size(), DropCatalog.rows(c).size()));

        System.out.println("\n== pets: ONLY Phoenix, Enderman, Ender Dragon, Bal, Scatha, Rat, Slug, Snowman, Flying Fish ==");
        String[] pets = {"Phoenix","Enderman","Ender Dragon","Bal","Scatha","Rat","Slug","Snowman","Flying Fish"};
        for (String p : pets) check("PET DROP! " + p, line("PET DROP!", '6', p), slug(p) + "_pet|" + p + "|LEGENDARY");
        ok(DropCatalog.byCategory(Category.PETS).size() == 9, "exactly 9 pets in the list", "got " + DropCatalog.byCategory(Category.PETS).size());
        for (String p : new String[]{"Squid","Guardian","Megalodon","Blue Whale","Dolphin","Black Cat","Tiger","Wolf","Zorblax"})
            check("removed pet '" + p + "' no longer triggers", line("PET DROP!", '6', p), null);

        System.out.println("\n== dyes: ONLY the 24 requested, ONE shared toggle ==");
        String[] dyes = {"Aquamarine","Archfiend","Bone","Brick Red","Byzantium","Carmine","Celadon","Celeste","Cyclamen","Dung","Flame","Holly","Livid","Mango","Matcha","Midnight","Mocha","Nadeshiko","Necron","Nyanza","Pearlescent","Periwinkle","Secret","Wild Strawberry"};
        for (String dy : dyes) check(dy + " Dye (green name, like Fakepixel colours dyes)", line("RARE DROP!", 'a', dy + " Dye"), slug(dy) + "_dye|" + dy + " Dye|" + DropCatalog.byId(slug(dy) + "_dye").defaultRarity);
        check("'Holy Dye' spelling also works", line("RARE DROP!", '6', "Holy Dye"), "holly_dye|Holy Dye|" + DropCatalog.byId("holly_dye").defaultRarity);
        check("Necron's Dye wording", line("RARE DROP!", 'd', "Necron's Dye"), "necron_dye|Necron's Dye|" + DropCatalog.byId("necron_dye").defaultRarity);
        for (String dy : new String[]{"Pure Black","Treasure","Jade","Aurora","Warden","Neon","Sangria","Bingo Blue"})
            check("removed dye '" + dy + " Dye' no longer triggers", line("RARE DROP!", '6', dy + " Dye"), null);
        List<DropCatalog.Row> dyeRows = DropCatalog.rows(Category.DYES);
        ok(dyeRows.size() == 1 && dyeRows.get(0).members.size() == 24, "Dyes tab shows ONE row covering all 24 dyes", "rows=" + dyeRows.size());
        ok(dyeRows.get(0).label.equals("All Dyes (24)"), "row label is 'All Dyes (24)'", dyeRows.get(0).label);
        DropCatalog.Row dr = dyeRows.get(0);
        dr.setEnabled(false);
        check("one switch OFF silences every dye (Matcha)", line("RARE DROP!", '6', "Matcha Dye"), null);
        check("... and Secret Dye", line("RARE DROP!", '6', "Secret Dye"), null);
        dr.setEnabled(true);
        check("one switch ON brings them all back", line("RARE DROP!", '6', "Secret Dye"), "secret_dye|Secret Dye|LEGENDARY");
        dr.cycleRarity(true);
        dr.cycleRarity(true);
        boolean same = true; for (DropEntry m : dr.members) if (m.override != Rarity.UNCOMMON) same = false;
        ok(same, "rarity button sets every dye at once", "members differ");
        dr.cycleRarity(false); dr.cycleRarity(false);
        ok(dr.members.get(5).override == null, "... and back to AUTO", "" + dr.members.get(5).override);

        System.out.println("\n== fishing (now 9) ==");
        check("Great White Shark Tooth", line("RARE DROP!", '6', "Great White Shark Tooth"), "great_white_shark_tooth|Great White Shark Tooth|LEGENDARY");
        for (String t : new String[]{"Nurse","Blue","Tiger"}) check(t + " Shark Tooth stays removed", line("RARE DROP!", 'a', t + " Shark Tooth"), null);
        check("Phantom Rod", line("RARE DROP!", '6', "Phantom Rod"), "phantom_rod|Phantom Rod|LEGENDARY");
        check("Deep Sea Orb", line("RARE DROP!", '5', "Deep Sea Orb"), "deep_sea_orb|Deep Sea Orb|EPIC");
        check("Soul Fragment", line("RARE DROP!", '5', "Soul Fragment"), "soul_fragment|Soul Fragment|EPIC");
        check("Vampire Fang", line("RARE DROP!", 'd', "Vampire Fang"), "vampire_fang|Vampire Fang|MYTHIC");
        check("Magma Lord Fragment", line("RARE DROP!", '6', "Magma Lord Fragment"), "magma_lord_fragment|Magma Lord Fragment|LEGENDARY");
        check("Magmalord Fragments (plural, one word)", line("RARE DROP!", '6', "Magmalord Fragments"), "magma_lord_fragment|Magmalord Fragments|LEGENDARY");
        check("Thunder Fragment", line("RARE DROP!", '5', "Thunder Fragment"), "thunder_fragment|Thunder Fragment|EPIC");
        check("Thunder Shards wording", line("RARE DROP!", '5', "Thunder Shards"), "thunder_fragment|Thunder Shards|EPIC");
        check("Radioactive Vial", line("INSANE DROP!", 'd', "Radioactive Vial"), "radioactive_vial|Radioactive Vial|MYTHIC");
        check("Music Rune I", line("RARE DROP!", '5', "Music Rune I"), "music_rune_1|Music Rune I|EPIC");
        ok(DropCatalog.byCategory(Category.FISHING).size() == 9, "Fishing has exactly 9 drops", "got " + DropCatalog.byCategory(Category.FISHING).size());

        System.out.println("\n== dungeons additions ==");
        check("Wither Chestplate", line("RARE REWARD!", '6', "Wither Chestplate"), "wither_chestplate|Wither Chestplate|LEGENDARY");
        check("Shadow Assassin Chestplate", line("RARE REWARD!", '5', "Shadow Assassin Chestplate"), "shadow_assassin_chestplate|Shadow Assassin Chestplate|EPIC");
        check("Shadow Assassin Cloak", line("RARE REWARD!", '5', "Shadow Assassin Cloak"), "shadow_assassin_cloak|Shadow Assassin Cloak|EPIC");
        check("Soulweaver Gloves", line("RARE REWARD!", '5', "Soulweaver Gloves"), "soulweaver_gloves|Soulweaver Gloves|EPIC");
        check("Bonzo's Mask", line("RARE REWARD!", '9', "Bonzo's Mask"), "bonzo_mask|Bonzo's Mask|RARE");
        check("Spirit Mask", line("RARE REWARD!", '5', "Spirit Mask"), "spirit_mask|Spirit Mask|EPIC");
        check("Bonzo's Staff", line("RARE REWARD!", '6', "Bonzo's Staff"), "bonzo_staff|Bonzo's Staff|LEGENDARY");
        check("Starred 'Wither Chestplate \u272A' still matches", line("RARE REWARD!", '6', "Wither Chestplate \u272A\u272A"), "wither_chestplate|Wither Chestplate|LEGENDARY");

        System.out.println("\n== misc (new category) + enderman addition ==");
        check("Shard of Pure Wisdom", line("RARE DROP!", '5', "Shard of Pure Wisdom"), "shard_of_pure_wisdom|Shard of Pure Wisdom|EPIC");
        check("Piece of Skyblock Code", line("RARE DROP!", '6', "Piece of Skyblock Code"), "piece_of_skyblock_code|Piece of Skyblock Code|LEGENDARY");
        check("Dark Candy", line("RARE DROP!", '9', "Dark Candy"), "dark_candy|Dark Candy|RARE");
        ok(DropCatalog.byCategory(Category.MISC).size() == 5, "Misc has 5 items (incl. Golden Dragon Egg)", "got " + DropCatalog.byCategory(Category.MISC).size());
        ok(DropCatalog.byId("shard_of_pure_wisdom").preferInventory && DropCatalog.byId("piece_of_skyblock_code").preferInventory, "Fakepixel-only items look for the exact item in your inventory first", "");
        ok(!DropCatalog.byId("dark_candy").preferInventory, "Dark Candy uses its official head directly", "");
        check("Exceedingly Rare Ender Artifact Upgrader", line("RARE DROP!", '6', "Exceedingly Rare Ender Artifact Upgrader"), "exceedingly_rare_ender_artifact_upgrader|Exceedingly Rare Ender Artifact Upgrader|LEGENDARY");
        check("plain 'Ender Artifact Upgrader' (not exceedingly rare) stays ignored", line("RARE DROP!", '5', "Ender Artifact Upgrader"), null);
        ok(DropCatalog.byCategory(Category.ENDERMAN).size() == 3, "Ender slayer has 3 drops", "got " + DropCatalog.byCategory(Category.ENDERMAN).size());

        System.out.println("\n== head vs 3D model: heads ONLY when the real item is a head ==");
        String[] mustBeHeads = {"golden_dragon_egg","quick_claw","cyclamen_dye","bonzo_mask","spirit_mask","precursor_eye","shadow_assassin_cloak","soulweaver_gloves","spirit_wing","auto_recombobulator","runebook",
            "dragon_claw","dragon_horn","dragon_scale","master_star_1","master_star_5","warden_heart","dark_candy","deep_sea_orb","soul_fragment","magma_lord_fragment","thunder_fragment","divans_fragment","glacite_jewel","perfect_ruby_gem","matcha_dye","phoenix_pet",
            "superior_dragon_helmet","strong_dragon_helmet","young_dragon_helmet","old_dragon_helmet","wise_dragon_helmet","protector_dragon_helmet","unstable_dragon_helmet"};
        for (String id : mustBeHeads) ok(DropCatalog.byId(id) != null && DropCatalog.byId(id).look.skull != null, id + " = official head", "not a head");
        String[] mustBe3D = {"spirit_boots","necromancer_lord_chestplate","wither_chestplate","wither_leggings","shadow_assassin_chestplate",
            "bonzo_staff","adaptive_blade","hyper_cleaver","spirit_sword","spirit_shortbow","livid_dagger","shadow_fury","last_breath","giants_sword","necromancer_sword","giant_cleaver","dark_claymore","aspect_of_the_dragons",
            "diamantes_handle","implosion_scroll","shadow_warp_scroll","wither_shield_scroll","ender_slayer_7","duplex_1","chimera_1","daedalus_stick","necrons_handle","phantom_rod",
            "overflux_capacitor","grizzly_bait","digested_mosquito","sorrow","great_white_shark_tooth","vampire_fang","glossy_gemstone",
            "superior_dragon_chestplate","strong_dragon_leggings","unstable_dragon_boots"};
        for (String id : mustBe3D) { DropEntry e = DropCatalog.byId(id); ok(e != null && e.look.skull == null && e.look.custom == null && e.look.itemName != null, id + " = real 3D model (vanilla item)", "wrongly a head/custom"); }
        Set<String> cus = new TreeSet<String>();
        for (DropEntry e : DropCatalog.ALL) if (e.look.custom != null) cus.add(e.id);
        ok(cus.equals(new TreeSet<String>(Arrays.asList("shard_of_pure_wisdom","piece_of_skyblock_code"))), "the only made-for-the-mod heads are the 2 Fakepixel items", "got " + cus);
        boolean anyPaper = false; for (DropEntry e : DropCatalog.ALL) if ("minecraft:paper".equals(e.look.itemName)) anyPaper = true;
        ok(!anyPaper, "no entry is paper", "found paper");
        ok(DropCatalog.byId("superior_dragon_chestplate").look.tint > 0 && DropCatalog.byId("superior_dragon_chestplate").look.tint != 0x222222, "dragon armour carries its real dye colour", "" + DropCatalog.byId("superior_dragon_chestplate").look.tint);
        ok("minecraft:writable_book".equals(DropCatalog.byId("implosion_scroll").look.itemName), "scrolls are writable books", "");

        System.out.println("\n== dungeons ordering + sections ==");
        List<DropCatalog.Row> dr2 = DropCatalog.rows(Category.DUNGEONS);
        List<String> heads2 = new ArrayList<String>(); for (DropCatalog.Row r : dr2) if (r.isHeader()) heads2.add(r.label);
        ok(heads2.equals(Arrays.asList("Armor", "Swords & weapons", "Equipment & items")), "3 headings in order: Armor, Swords & weapons, Equipment & items", "got " + heads2);
        int iArm = -1, iSw = -1, iEq = -1, iWl = -1, iBs = -1, iDc = -1, iSp = -1;
        for (int i = 0; i < dr2.size(); i++) {
            DropCatalog.Row r = dr2.get(i); if (r.isHeader()) continue; String id = r.lead().id;
            if (id.equals("bonzo_mask")) iArm = i; if (id.equals("wither_leggings")) iWl = i; if (id.equals("bonzo_staff")) iBs = i; if (id.equals("dark_claymore")) iDc = i; if (id.equals("spirit_wing")) iSp = i;
        }
        ok(iArm < iWl && iWl < iBs && iBs < iDc && iDc < iSp, "armour first, then weapons, then equipment/items", iArm + " " + iWl + " " + iBs + " " + iDc + " " + iSp);
        ok(DropCatalog.byCategory(Category.DUNGEONS).size() == 34, "Dungeons has 34 drops", "got " + DropCatalog.byCategory(Category.DUNGEONS).size());
        List<DropCatalog.Row> dg = DropCatalog.rows(Category.DRAGONS);
        int groups = 0; for (DropCatalog.Row r : dg) if (r.isGroup()) groups++;
        ok(groups == 7 && DropCatalog.byCategory(Category.DRAGONS).size() == 32, "Dragons: 7 armour sets (one row each) + sword + claw/horn/scale = 32 drops", groups + " groups, " + DropCatalog.byCategory(Category.DRAGONS).size());
        ok("All Old Dragon Armor (4)".equals(dg.get(1).label) && "Helmet, Chestplate, Leggings, Boots".equals(dg.get(1).summary), "dragon set row: label + summary", dg.get(1).label + " / " + dg.get(1).summary);
        ok(DropCatalog.rows(Category.DYES).get(0).summary.startsWith("Aquamarine, Archfiend, Bone"), "dyes row summary", DropCatalog.rows(Category.DYES).get(0).summary);

        System.out.println("\n== mining: every item has a real model (no paper) ==");
        int paper = 0, heads = 0;
        for (DropEntry e : DropCatalog.byCategory(Category.MINING)) {
            boolean isPaper = !e.look.hasTexture() && "minecraft:paper".equals(e.look.itemName);
            if (isPaper) { paper++; System.out.println("FAIL  " + e.id + " is still paper"); }
            if (e.look.skull != null) heads++;
        }
        ok(paper == 0, "no mining item falls back to paper", paper + " paper items");
        ok(heads == 16, "16 mining items are real heads (Divan x2, Glacite, Quick Claw, 12 Perfect gems)", "heads=" + heads);
        ok("minecraft:ghast_tear".equals(DropCatalog.byId("sorrow").look.itemName), "Sorrow = 3D ghast tear", "");
        ok("minecraft:dye".equals(DropCatalog.byId("glossy_gemstone").look.itemName) && DropCatalog.byId("glossy_gemstone").look.damage == 5, "Glossy Gemstone = 3D purple dye", "");
        Set<String> tex = new HashSet<String>();
        for (DropEntry e : DropCatalog.ALL) if (e.look.skull != null) tex.add(e.look.skull);
        int headEntries = 0; for (DropEntry e : DropCatalog.ALL) if (e.look.skull != null) headEntries++;
        ok(tex.size() == headEntries, "every official head texture is unique (" + headEntries + " heads)", tex.size() + " distinct");
        for (String g : new String[]{"Amber","Amethyst","Aquamarine","Citrine","Jade","Jasper","Onyx","Opal","Peridot","Ruby","Sapphire","Topaz"})
            check("Perfect " + g + " Gemstone", line("RARE DROP!", '6', "Perfect " + g + " Gemstone"), "perfect_" + g.toLowerCase() + "_gem|Perfect " + g + " Gemstone|LEGENDARY");
        check("Divan's Fragment", line("RARE DROP!", '5', "Divan's Fragment"), "divans_fragment|Divan's Fragment|EPIC");
        check("Divan's Alloy", line("RARE DROP!", '6', "Divan's Alloy"), "divan_alloy|Divan's Alloy|LEGENDARY");
        check("Glacite Jewel", line("RARE DROP!", '9', "Glacite Jewel"), "glacite_jewel|Glacite Jewel|RARE");
        check("Glossy Gemstone", line("RARE DROP!", '5', "Glossy Gemstone"), "glossy_gemstone|Glossy Gemstone|EPIC");
        check("Glossy Mineral wording", line("RARE DROP!", '5', "Glossy Mineral"), "glossy_gemstone|Glossy Mineral|EPIC");
        check("Sorrow", line("RARE DROP!", '9', "Sorrow"), "sorrow|Sorrow|RARE");

        System.out.println("\n== everything from before still works ==");
        check("Warden Heart", line("RARE DROP!", '6', "Warden Heart"), "warden_heart|Warden Heart|LEGENDARY");
        check("Grizzly Salmon (new name)", line("RARE DROP!", '9', "Grizzly Salmon"), "grizzly_bait|Grizzly Salmon|RARE");
        check("Judgement Core", line("VERY RARE DROP!", '6', "Judgement Core"), "judgement_core|Judgement Core|LEGENDARY");
        check("Ender Slayer VII", line("RARE DROP!", '5', "Enchanted Book (Ender Slayer VII)"), "ender_slayer_7|Ender Slayer VII|EPIC");
        check("Archfiend Dice", line("RARE DROP!", '5', "Archfiend Dice"), "archfiend_dice|Archfiend Dice|EPIC");
        check("Chimera I", line("RARE DROP!", '6', "Enchanted Book (Chimera I)"), "chimera_1|Chimera I|LEGENDARY");
        check("Daedalus Stick", line("RARE DROP!", '6', "Daedalus Stick"), "daedalus_stick|Daedalus Stick|LEGENDARY");
        check("Giant's Sword", line("RARE REWARD!", '6', "Giant's Sword"), "giants_sword|Giant's Sword|LEGENDARY");
        check("Fifth Master Star", line("RARE REWARD!", '6', "Fifth Master Star"), "master_star_5|Fifth Master Star|LEGENDARY");
        check("Radioactive Vial", line("INSANE DROP!", 'd', "Radioactive Vial"), "radioactive_vial|Radioactive Vial|MYTHIC");
        check("Helianthus", line("RARE DROP!", '6', "Helianthus"), "helianthus|Helianthus|LEGENDARY");
        check("Ender Artifact Upgrader stays removed", line("RARE DROP!", '5', "Ender Artifact Upgrader"), null);
        check("unknown RARE DROP ignored", line("RARE DROP!", '9', "Mystery Gizmo"), null);

        System.out.println("\n== the REAL Fakepixel lines from the screenshot ==");
        check("INSANE RARE DROP! Piece of Skyblock Code (red bold header, gold name)", P+"c"+P+"lINSANE RARE DROP! "+P+"r"+P+"6Piece of Skyblock Code", "piece_of_skyblock_code|Piece of Skyblock Code|LEGENDARY");
        check("'Wow! [MVP<plus>] Always_Rajat found a Piece of Skyblock Code!' broadcast is NOT animated", P+"e"+P+"lWow! "+P+"b[MVP<plus>] Always_Rajat "+P+"efound a "+P+"6Piece of Skyblock Code"+P+"e!", null);
        check("'You found a strange shard emitting a green light.' is ignored", P+"7"+P+"oYou found a strange shard emitting a green light.", null);
        check("'It seems to have fallen from the depths of Skyblock.' is ignored", P+"7"+P+"oIt seems to have fallen from the depths of Skyblock.", null);
        check("any '<WORDS> DROP!' tier works (EXTREMELY RARE DROP!)", P+"c"+P+"lEXTREMELY RARE DROP! "+P+"r"+P+"6Warden Heart", "warden_heart|Warden Heart|LEGENDARY");
        check("INSANE RARE DROP! for another item (Warden Heart)", P+"c"+P+"lINSANE RARE DROP! "+P+"r"+P+"6Warden Heart", "warden_heart|Warden Heart|LEGENDARY");
        check("unknown item under a new header is still ignored", P+"c"+P+"lINSANE RARE DROP! "+P+"r"+P+"6Totally Unknown Thing", null);
        check("a player pasting the fake header is still rejected", "Always_Rajat: INSANE RARE DROP! Piece of Skyblock Code", null);
        AIAConfig.cfg.get("general", "dropHeaders", "x").set("VERY RARE DROP!|RARE DROP!");
        AIAConfig.load();
        boolean hasInsane = false; for (String h : AIAConfig.headers()) if (h.equals("INSANE RARE DROP!")) hasInsane = true;
        ok(hasInsane, "an old saved header list gets 'INSANE RARE DROP!' added on load", Arrays.toString(AIAConfig.headers()));
        AIAConfig.cfg.get("general", "dropHeaders", "x").set("VERY RARE DROP!|RARE DROP!|MY OWN HEADER!");
        AIAConfig.load();
        boolean keepsOwn = false; for (String h : AIAConfig.headers()) if (h.equals("MY OWN HEADER!")) keepsOwn = true;
        ok(keepsOwn, "...and your own custom headers are kept", Arrays.toString(AIAConfig.headers()));

        System.out.println("\n== EXACT Fakepixel lines from your screenshots ==");
        String MFx = P + "b";
        check("INSANE DROP! (1x Primordial Eye) (+255% Magic Find!)", P+"c"+P+"lINSANE DROP! "+P+"r(1x "+P+"6Primordial Eye"+P+"r) "+MFx+"(+255% Magic Find!)", "primordial_eye|Primordial Eye|LEGENDARY");
        check("CRAZY RARE DROP! (1x Judgement Core) (+247% Magic Find!)", P+"d"+P+"lCRAZY RARE DROP! "+P+"r(1x "+P+"6Judgement Core"+P+"r) "+MFx+"(+247% Magic Find!)", "judgement_core|Judgement Core|LEGENDARY");
        check("INSANE DROP! (1x Warden Heart) (+221% Magic Find!)", P+"c"+P+"lINSANE DROP! "+P+"r(1x "+P+"6Warden Heart"+P+"r) "+MFx+"(+221% Magic Find!)", "warden_heart|Warden Heart|LEGENDARY");
        check("CRAZY RARE DROP! (1x Exceedingly Rare Ender Artifact Upgrader) (+229% Magic Find!)", P+"d"+P+"lCRAZY RARE DROP! "+P+"r(1x "+P+"6Exceedingly Rare Ender Artifact Upgrader"+P+"r) "+MFx+"(+229% Magic Find!)", "exceedingly_rare_ender_artifact_upgrader|Exceedingly Rare Ender Artifact Upgrader|LEGENDARY");
        check("slayer junk is ignored: RARE DROP! (1x Foul Flesh)", P+"6"+P+"lRARE DROP! "+P+"r(1x "+P+"9Foul Flesh"+P+"r) "+MFx+"(+220% Magic Find!)", null);
        check("slayer junk is ignored: VERY RARE DROP! (1x Mana Steal I)", P+"9"+P+"lVERY RARE DROP! "+P+"r(1x "+P+"9Mana Steal I"+P+"r) "+MFx+"(+247% Magic Find!)", null);
        check("slayer junk is ignored: RARE DROP! (2x Revenant Viscera)", P+"6"+P+"lRARE DROP! "+P+"r(2x "+P+"9Revenant Viscera"+P+"r) "+MFx+"(+221% Magic Find!)", null);
        check("RARE DROP! Chimera I (+459% Magic Find!)", P+"6"+P+"lRARE DROP! "+P+"r"+P+"dChimera I "+MFx+"(+459% Magic Find!)", "chimera_1|Chimera I|MYTHIC");
        check("RARE DROP! Runebook   (no Magic Find part at all)", P+"6"+P+"lRARE DROP! "+P+"rRunebook", "runebook|Runebook|" + DropCatalog.byId("runebook").defaultRarity);
        check("RARE DROP! Pearlescent Dye (+340% Magic Find!)   [name drawn green]", P+"6"+P+"lRARE DROP! "+P+"r"+P+"aPearlescent Dye "+MFx+"(+340% Magic Find!)", "pearlescent_dye|Pearlescent Dye|" + DropCatalog.byId("pearlescent_dye").defaultRarity);
        check("RARE DROP! Cyclamen Dye (+272% Magic Find!)   [name drawn magenta]", P+"6"+P+"lRARE DROP! "+P+"r"+P+"dCyclamen Dye "+MFx+"(+272% Magic Find!)", "cyclamen_dye|Cyclamen Dye|" + DropCatalog.byId("cyclamen_dye").defaultRarity);
        check("RARE DROP! Bone Dye (+294% Magic Find!)   [name drawn white]", P+"6"+P+"lRARE DROP! "+P+"r"+P+"fBone Dye "+MFx+"(+294% Magic Find!)", "bone_dye|Bone Dye|" + DropCatalog.byId("bone_dye").defaultRarity);
        check("PET DROP! [Lvl 1] Phoenix (+218% Magic Find!)", P+"d"+P+"lPET DROP! "+P+"r"+P+"7[Lvl 1] "+P+"dPhoenix "+MFx+"(+218% Magic Find!)", "phoenix_pet|Phoenix|MYTHIC");
        check("same with a chat-stack counter: ... (+219% Magic Find!) (2)", P+"d"+P+"lPET DROP! "+P+"r"+P+"7[Lvl 1] "+P+"dPhoenix "+MFx+"(+219% Magic Find!) "+P+"7(2)", "phoenix_pet|Phoenix|MYTHIC");
        check("counter on a normal drop: RARE DROP! Quick Claw (+250% Magic Find!) (4)", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Quick Claw "+MFx+"(+250% Magic Find!) "+P+"7(4)", "quick_claw|Quick Claw|LEGENDARY");
        check("INSANE RARE DROP! Golden Dragon Egg", P+"c"+P+"lINSANE RARE DROP! "+P+"r"+P+"6Golden Dragon Egg", "golden_dragon_egg|Golden Dragon Egg|LEGENDARY");
        check("PET DROP! [Lvl 1] Golden Dragon Egg (+100% Magic Find!)", P+"d"+P+"lPET DROP! "+P+"r"+P+"7[Lvl 1] "+P+"6Golden Dragon Egg "+MFx+"(+100% Magic Find!)", "golden_dragon_egg|Golden Dragon Egg|LEGENDARY");
        check("[MVP+] Artic unlocked Necron's Handle!", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"5Necron's Handle"+P+"e!", "necrons_handle|Necron's Handle|EPIC");
        check("[MVP+] Artic unlocked Dark Claymore!", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Dark Claymore"+P+"e!", "dark_claymore|Dark Claymore|LEGENDARY");
        check("[MVP+] Artic unlocked Wither Chestplate!", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Wither Chestplate"+P+"e!", "wither_chestplate|Wither Chestplate|LEGENDARY");
        check("[VIP] Artic unlocked Spirit Mask!  (any rank prefix)", P+"a[VIP] Artic "+P+"eunlocked "+P+"5Spirit Mask"+P+"e!", "spirit_mask|Spirit Mask|EPIC");
        check("Wither Boots (excluded) is ignored", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Wither Boots"+P+"e!", null);
        check("Undead Essence x53 is ignored", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"dUndead Essence "+P+"7x53"+P+"e!", null);
        check("Wither Essence x45 is ignored", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"dWither Essence "+P+"7x45"+P+"e!", null);
        check("ANOTHER player's unlock is ignored", P+"b[MVP"+P+"c+"+P+"b] Always_Rajat "+P+"eunlocked "+P+"5Necron's Handle"+P+"e!", null);
        check("removed: Necromancer Lord Boots", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Necromancer Lord Boots"+P+"e!", null);
        check("removed: Necromancer Lord Leggings", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Necromancer Lord Leggings"+P+"e!", null);
        check("removed: Necromancer Lord Helmet", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Necromancer Lord Helmet"+P+"e!", null);
        check("kept: Necromancer Lord Chestplate", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Necromancer Lord Chestplate"+P+"e!", "necromancer_lord_chestplate|Necromancer Lord Chestplate|LEGENDARY");
        check("removed: Wither Cloak Sword", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Wither Cloak Sword"+P+"e!", null);
        check("removed: L.A.S.R.'s Eye", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"5L.A.S.R.'s Eye"+P+"e!", null);
        check("removed: Jolly Pink Rock", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"5Jolly Pink Rock"+P+"e!", null);
        check("removed: Bigfoot's Bola", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"5Bigfoot's Bola"+P+"e!", null);
        check("kept: Diamante's Handle", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"5Diamante's Handle"+P+"e!", "diamantes_handle|Diamante's Handle|EPIC");
        check("SPOOF: player pastes the slayer line (as in your screenshot)", P+"7[369] "+P+"8\u2016 "+P+"b[MVP+] Always_Rajat"+P+"f: CRAZY RARE DROP! (1x Judgement Core) (+247% Magic Find!)", null);
        check("SPOOF: player pastes 'RARE DROP! Pearlescent Dye'", P+"7[357] "+P+"8\u2016 "+P+"b[MVP+] Always_Rajat"+P+"f: RARE DROP! Pearlescent Dye (+340% Magic Find!)", null);
        check("SPOOF: player pastes an unlock line", P+"7[357] "+P+"b[MVP+] Artic"+P+"f: [MVP+] Artic unlocked Necron's Handle!", null);
        check("SPOOF: unformatted unlock line (strict)", "[MVP+] Artic unlocked Spirit Sword!", null);
        check("'Always_Rajat is holding [Judgement Core]' is ignored", P+"b[MVP+] Always_Rajat "+P+"7is holding "+P+"6[Judgement Core]", null);
        check("'RNG METER! Reselected the Primordial Eye ...' is ignored", P+"d"+P+"lRNG METER! "+P+"aReselected the "+P+"6Primordial Eye"+P+"a for Tarantula Broodfather slayer!", null);
        check("'RNG Meter - 2905973 XP' is ignored", P+"dRNG Meter - 2905973 XP", null);
        check("'Wow! [VIP] ArticBlue7 found a Phoenix pet!' broadcast is ignored", P+"e"+P+"lWow! "+P+"a[VIP] ArticBlue7 "+P+"efound a "+P+"dPhoenix"+P+"e pet!", null);
        check("'You bought [Lvl 1] Golden Dragon Egg for ... Coins!' is ignored", P+"aYou bought "+P+"7[Lvl 1] "+P+"6Golden Dragon Egg"+P+"a for "+P+"6485,000,014.3 Coins"+P+"a!", null);
        check("'Your Kill Combo has expired!' is ignored", P+"cYour Kill Combo has expired! You reached a 25 Kill Combo!", null);

        System.out.println("\n== wording variants (Fakepixel mirrors Hypixel; JEF/Aetheria mods confirm 'RARE DROP! Daedalus Stick' etc.) ==");
        check("Diana: RARE DROP! Daedalus Stick (+MF!)", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Daedalus Stick "+P+"b(+300% Magic Find!)", "daedalus_stick|Daedalus Stick|LEGENDARY");
        check("Diana: RARE DROP! Minos Relic", P+"6"+P+"lRARE DROP! "+P+"r"+P+"5Minos Relic", "minos_relic|Minos Relic|EPIC");
        check("Diana: RARE DROP! Chimera I", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Chimera I "+P+"b(+461% Magic Find!)", "chimera_1|Chimera I|LEGENDARY");
        check("Diana: RARE DROP! You dug out a Daedalus Stick", P+"6"+P+"lRARE DROP! "+P+"r"+P+"eYou dug out a "+P+"6Daedalus Stick", "daedalus_stick|Daedalus Stick|LEGENDARY");
        check("'You found a X' wording is NOT read as another player", P+"6"+P+"lRARE DROP! "+P+"rYou found a "+P+"6Phantom Rod", "phantom_rod|Phantom Rod|LEGENDARY");
        check("RARE CROP! Squash", P+"6"+P+"lRARE CROP! "+P+"r"+P+"9Squash", "squash|Squash|RARE");
        check("RARE CROP! Cropie", P+"6"+P+"lRARE CROP! "+P+"r"+P+"9Cropie "+P+"b(+2% Overbloom)", "cropie|Cropie|RARE");
        check("RARE DROP! Helianthus", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Helianthus", "helianthus|Helianthus|LEGENDARY");
        check("farming armour 'Squash Ring' is not a Squash", P+"6"+P+"lRARE DROP! "+P+"r"+P+"9Squash Ring", null);
        check("'Condensed Fermento' is not Fermento", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Condensed Fermento", null);
        check("'Sorrow Boots' is not Sorrow", P+"6"+P+"lRARE DROP! "+P+"r"+P+"9Sorrow Boots", null);
        check("Mining: RARE DROP! Divan Fragment (Fakepixel spelling)", P+"6"+P+"lRARE DROP! "+P+"r"+P+"5Divan Fragment", "divans_fragment|Divan Fragment|EPIC");
        check("Mining: RARE DROP! Perfect Ruby Gemstone", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Perfect Ruby Gemstone "+P+"b(+120% Magic Find!)", "perfect_ruby_gem|Perfect Ruby Gem|LEGENDARY".replace("Gem|", "Gemstone|").replace("Ruby Gemstone|", "Ruby Gem|").replace("Ruby Gem|", "Ruby Gemstone|"));
        check("Mining: RARE DROP! Glacite Jewel", P+"9"+P+"lRARE DROP! "+P+"r"+P+"9Glacite Jewel", "glacite_jewel|Glacite Jewel|RARE");
        check("Mining: RARE DROP! Quick Claw", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Quick Claw", "quick_claw|Quick Claw|LEGENDARY");
        check("Fishing: RARE DROP! Deep Sea Orb", P+"6"+P+"lRARE DROP! "+P+"r"+P+"5Deep Sea Orb", "deep_sea_orb|Deep Sea Orb|EPIC");
        check("Fishing: VERY RARE DROP! Radioactive Vial", P+"d"+P+"lVERY RARE DROP! "+P+"r"+P+"dRadioactive Vial", "radioactive_vial|Radioactive Vial|MYTHIC");
        check("Fishing: GREAT CATCH! You found a Phantom Rod", P+"6"+P+"lGREAT CATCH! "+P+"r"+P+"bYou found a "+P+"6Phantom Rod", "phantom_rod|Phantom Rod|LEGENDARY");
        check("Fishing: CRAZY RARE DROP! (1x Magma Lord Fragment)", P+"d"+P+"lCRAZY RARE DROP! "+P+"r"+P+"6(1x Magma Lord Fragment) "+P+"b(+88% Magic Find!)", "magma_lord_fragment|Magma Lord Fragment|LEGENDARY");
        check("Dragons: RARE DROP! Superior Dragon Helmet", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Superior Dragon Helmet "+P+"b(+120% Magic Find!)", "superior_dragon_helmet|Superior Dragon Helmet|LEGENDARY");
        check("Dragons: VERY RARE DROP! (1x Young Dragon Boots)", P+"d"+P+"lVERY RARE DROP! "+P+"r"+P+"6(1x Young Dragon Boots)", "young_dragon_boots|Young Dragon Boots|LEGENDARY");
        check("Dragons: RARE DROP! Aspect of the Dragons", P+"6"+P+"lRARE DROP! "+P+"r"+P+"6Aspect of the Dragons", "aspect_of_the_dragons|Aspect of the Dragons|LEGENDARY");
        check("Dragons: RARE DROP! Dragon Claw", P+"9"+P+"lRARE DROP! "+P+"r"+P+"9Dragon Claw", "dragon_claw|Dragon Claw|RARE");
        check("Dragons: dungeon-style 'Artic unlocked Wise Dragon Chestplate!'", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"eunlocked "+P+"6Wise Dragon Chestplate"+P+"e!", "wise_dragon_chestplate|Wise Dragon Chestplate|LEGENDARY");
        check("Dragons: 'Artic received Strong Dragon Leggings!'", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"ereceived "+P+"6Strong Dragon Leggings"+P+"e!", "strong_dragon_leggings|Strong Dragon Leggings|LEGENDARY");
        check("Dungeon: 'Artic has obtained Spirit Wing!'", P+"b[MVP"+P+"c+"+P+"b] Artic "+P+"ehas obtained "+P+"5Spirit Wing"+P+"e!", "spirit_wing|Spirit Wing|EPIC");
        check("Pet: PET DROP! Phoenix with trailing text 'Phoenix!'", P+"6"+P+"lPET DROP! "+P+"r"+P+"6Phoenix"+P+"e!", "phoenix_pet|Phoenix|LEGENDARY");
        check("Pet: PET DROP! You found a Phoenix", P+"6"+P+"lPET DROP! "+P+"r"+P+"eYou found a "+P+"6Phoenix", "phoenix_pet|Phoenix|LEGENDARY");
        check("Pet: PET DROP! Legendary Ender Dragon Pet", P+"6"+P+"lPET DROP! "+P+"r"+P+"6Legendary Ender Dragon Pet", "ender_dragon_pet|Ender Dragon|LEGENDARY");
        check("Misc: RARE DROP! Dark Candy", P+"9"+P+"lRARE DROP! "+P+"r"+P+"9Dark Candy", "dark_candy|Dark Candy|RARE");
        check("Misc: RARE DROP! Golden Dragon Egg", P+"6"+P+"lINSANE RARE DROP! "+P+"r"+P+"6Golden Dragon Egg "+P+"b(+77% Magic Find!)", "golden_dragon_egg|Golden Dragon Egg|LEGENDARY");
        check("Misc: RARE DROP! Shard of Pure Wisdom", P+"5"+P+"lINSANE RARE DROP! "+P+"r"+P+"5Shard of Pure Wisdom", "shard_of_pure_wisdom|Shard of Pure Wisdom|EPIC");
        check("stack suffix: RARE DROP! Dragon Scale x3", P+"6"+P+"lRARE DROP! "+P+"r"+P+"9Dragon Scale "+P+"7x3", "dragon_scale|Dragon Scale|RARE");
        check("stack prefix: RARE DROP! 2x Dragon Horn", P+"6"+P+"lRARE DROP! "+P+"r"+P+"52x Dragon Horn", "dragon_horn|Dragon Horn|EPIC");
        check("SPOOF: pasted 'RARE CROP! Squash' by a player", P+"b[MVP+] Bob"+P+"f: RARE CROP! Squash", null);
        check("SPOOF: pasted 'GREAT CATCH! You found a Phantom Rod'", P+"7[200] "+P+"b[MVP+] Bob"+P+"f: GREAT CATCH! You found a Phantom Rod", null);
        check("SPOOF: pasted 'Artic received Spirit Wing!' by a player", P+"7[200] "+P+"b[MVP+] Artic"+P+"f: Artic received Spirit Wing!", null);
        check("other player 'Bob received Spirit Wing!' ignored", P+"b[MVP"+P+"c+"+P+"b] Bob "+P+"ereceived "+P+"5Spirit Wing"+P+"e!", null);
        check("junk header lines still ignored: DAILY REWARD! 100 Coins", P+"6"+P+"lDAILY REWARD! "+P+"r100 Coins", null);

        System.out.println("\n== anti-spoof ==");
        check("pasted by a player", P+"b[MVP"+P+"c+"+P+"b] Bob"+P+"f: RARE DROP! Warden Heart", null);
        check("pasted pet drop", "Bob: PET DROP! Phoenix", null);
        check("party chat paste", P+"9Party "+P+"8> "+P+"b[MVP+] Bob"+P+"f: RARE DROP! Matcha Dye", null);
        check("plain unformatted paste (strict ON)", "RARE DROP! Warden Heart", null);
        check("line about another player", P+"6"+P+"lRARE REWARD! "+P+"rBob found "+P+"6Necron's Handle", null);

        System.out.println("\n== new defaults + migration of an old saved config ==");
        ok(AIAConfig.spinSpeed == 30f, "default spin speed = 30 deg/s", "" + AIAConfig.spinSpeed);
        AIAConfig.cfg.get("general", "spinSpeedDegPerSec", "x").set("120");
        AIAConfig.cfg.get("general", "configVersion", "x").set("1");
        AIAConfig.load();
        ok(AIAConfig.spinSpeed == 30f, "old saved 120 deg/s is replaced by 30", "" + AIAConfig.spinSpeed);
        AIAConfig.cfg.get("general", "spinSpeedDegPerSec", "x").set("90");
        AIAConfig.load();
        ok(AIAConfig.spinSpeed == 90f, "after migration a speed YOU choose is kept (90)", "" + AIAConfig.spinSpeed);

        System.out.println("\n== catalog integrity ==");
        Set<String> ids = new HashSet<String>(); boolean dupe = false;
        for (DropEntry e : DropCatalog.ALL) if (!ids.add(e.id)) dupe = true;
        ok(!dupe, "all ids unique", "duplicate id");
        int shadow = 0;
        for (DropEntry e : DropCatalog.ALL) {
            String header = e.category == Category.PETS ? "PET DROP!" : "RARE DROP!";
            DropEntry first = null;
            for (DropEntry x : DropCatalog.ALL) if (x.matches(header, e.name)) { first = x; break; }
            if (first != e) { shadow++; System.out.println("FAIL  '" + e.name + "' resolves to " + (first == null ? "nothing" : first.id)); }
        }
        ok(shadow == 0, "every one of the " + DropCatalog.ALL.size() + " entries resolves to itself", shadow + " wrong");
        System.out.println("\n" + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
