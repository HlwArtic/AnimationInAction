package com.artic.aia;

import net.minecraftforge.common.config.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

public final class DropCatalog {
    private DropCatalog() {}

    public static final List<DropEntry> ALL = new ArrayList<DropEntry>();
    private static final Map<Category, List<DropEntry>> BY_CAT = new EnumMap<Category, List<DropEntry>>(Category.class);
    private static final Map<Category, List<Row>> ROWS = new EnumMap<Category, List<Row>>(Category.class);
    private static final Random RNG = new Random();
    private static boolean built;

    public static final class Row {
        public final String label;
        public final String summary;
        public final boolean header;
        public final List<DropEntry> members = new ArrayList<DropEntry>();

        Row(String label, String summary, boolean header) { this.label = label; this.summary = summary; this.header = header; }

        public boolean isHeader() { return header; }
        public DropEntry lead() { return members.get(0); }
        public boolean enabled() { return !header && lead().enabled; }
        public void setEnabled(boolean on) { for (DropEntry m : members) m.enabled = on; }
        public void cycleRarity(boolean forward) {
            if (header) return;
            lead().cycleRarity(forward);
            Rarity o = lead().override;
            for (DropEntry m : members) m.override = o;
        }
        public DropEntry previewTarget() { return members.get(RNG.nextInt(members.size())); }
        public boolean isGroup() { return members.size() > 1; }
    }

    private static synchronized void build() {
        if (built) return;
        built = true;
        for (Category c : Category.values()) BY_CAT.put(c, new ArrayList<DropEntry>());
        BufferedReader r = null;
        try {
            InputStream in = DropCatalog.class.getResourceAsStream("/assets/animationinaction/drops.tsv");
            if (in == null) {
                AIAConfig.log("drops.tsv is missing from the jar!");
                return;
            }
            r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] c = line.split("\t", -1);
                if (c.length < 9) continue;
                try {
                    Category cat = Category.valueOf(c[1]);
                    Rarity rar = Rarity.valueOf(c[3]);
                    Pattern p = c[4].isEmpty() ? null : Pattern.compile(c[4], Pattern.CASE_INSENSITIVE);
                    DropEntry d = new DropEntry(cat, c[0], c[2], p, rar, ItemLook.parse(c[7], c[8]));
                    if (!c[5].isEmpty()) d.headers = c[5].split("\\|");
                    for (String f : c[6].split(",")) {
                        f = f.trim();
                        if (f.equals("book")) d.book = true;
                        else if (f.equals("inv")) d.preferInventory = true;
                        else if (f.equals("catch")) d.generic = true;
                        else if (f.equals("fixed")) d.fixedRarity = true;
                        else if (f.startsWith("group:")) d.group = f.substring(6);
                        else if (f.startsWith("section:")) d.section = f.substring(8);
                    }
                    if (p == null) d.generic = true;
                    ALL.add(d);
                    BY_CAT.get(cat).add(d);
                } catch (Exception bad) {
                    AIAConfig.log("bad drops.tsv line: " + line.substring(0, Math.min(60, line.length())) + " -> " + bad);
                }
            }
        } catch (Exception e) {
            AIAConfig.log("could not read drops.tsv: " + e);
        } finally {
            if (r != null) try { r.close(); } catch (Exception ignored) {}
        }
        buildRows();
    }

    private static void buildRows() {
        for (Category c : Category.values()) {
            List<Row> rows = new ArrayList<Row>();
            Map<String, List<DropEntry>> groups = new LinkedHashMap<String, List<DropEntry>>();
            List<Object> order = new ArrayList<Object>();
            for (DropEntry d : BY_CAT.get(c)) {
                if (d.section != null) order.add(d.section);
                if (d.group == null) { order.add(d); continue; }
                List<DropEntry> g = groups.get(d.group);
                if (g == null) { g = new ArrayList<DropEntry>(); groups.put(d.group, g); order.add(g); }
                g.add(d);
            }
            for (Object o : order) {
                Row r;
                if (o instanceof String) {
                    r = new Row((String) o, null, true);
                } else if (o instanceof DropEntry) {
                    DropEntry d = (DropEntry) o;
                    r = new Row(d.name, null, false);
                    r.members.add(d);
                } else {
                    @SuppressWarnings("unchecked") List<DropEntry> g = (List<DropEntry>) o;
                    r = new Row("All " + titleCase(g.get(0).group) + " (" + g.size() + ")", summarize(g), false);
                    r.members.addAll(g);
                }
                rows.add(r);
            }
            ROWS.put(c, rows);
        }
    }

    private static String titleCase(String key) {
        StringBuilder sb = new StringBuilder();
        for (String w : key.split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static String summarize(List<DropEntry> g) {
        List<String[]> words = new ArrayList<String[]>();
        int min = Integer.MAX_VALUE;
        for (DropEntry m : g) {
            String[] w = m.name.split("\\s+");
            words.add(w);
            min = Math.min(min, w.length);
        }
        int pre = 0;
        while (pre < min - 1) {
            boolean same = true;
            for (String[] w : words) if (!w[pre].equals(words.get(0)[pre])) { same = false; break; }
            if (!same) break;
            pre++;
        }
        int suf = 0;
        while (suf < min - 1 - pre) {
            boolean same = true;
            for (String[] w : words) if (!w[w.length - 1 - suf].equals(words.get(0)[words.get(0).length - 1 - suf])) { same = false; break; }
            if (!same) break;
            suf++;
        }
        StringBuilder sb = new StringBuilder();
        for (String[] w : words) {
            if (sb.length() > 0) sb.append(", ");
            for (int i = pre; i < w.length - suf; i++) sb.append(i > pre ? " " : "").append(w[i]);
        }
        return sb.toString();
    }

    public static List<Row> rows(Category c) {
        build();
        return ROWS.get(c);
    }

    public static List<DropEntry> byCategory(Category c) {
        build();
        return BY_CAT.get(c);
    }

    public static DropEntry byId(String id) {
        build();
        for (DropEntry d : ALL) if (d.id.equalsIgnoreCase(id)) return d;
        return null;
    }

    public static void init() {
        build();
        Configuration cfg = AIAConfig.cfg;
        for (DropEntry d : ALL) {
            String en = cfg.getString(d.id + "_enabled", "drops", "true", d.name + " animation on/off");
            d.enabled = Boolean.parseBoolean(en);
            String r = cfg.getString(d.id + "_rarity", "drops", "AUTO", d.name + " rarity: AUTO (from chat colour) or " + rarityList());
            d.override = null;
            for (Rarity x : Rarity.values()) if (x.name().equalsIgnoreCase(r.trim())) d.override = x;
        }
        for (Category c : Category.values()) {
            for (Row row : rows(c)) {
                if (!row.isGroup()) continue;
                boolean on = row.lead().enabled;
                Rarity o = row.lead().override;
                for (DropEntry m : row.members) { m.enabled = on; m.override = o; }
            }
        }
        cfg.save();
    }

    public static void saveInto(Configuration cfg) {
        for (DropEntry d : ALL) {
            cfg.get("drops", d.id + "_enabled", "true").set(String.valueOf(d.enabled));
            cfg.get("drops", d.id + "_rarity", "AUTO").set(d.override == null ? "AUTO" : d.override.name());
        }
    }

    private static String rarityList() {
        StringBuilder sb = new StringBuilder();
        for (Rarity r : Rarity.values()) sb.append(sb.length() == 0 ? "" : ", ").append(r.name());
        return sb.toString();
    }
}
