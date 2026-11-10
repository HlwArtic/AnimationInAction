package com.artic.aia;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public final class AIAConfig {
    private AIAConfig() {}

    public static Configuration cfg;
    public static File dir, logFile;

    public static boolean master = true;
    public static boolean flash = true;
    public static boolean dim = true;
    public static boolean sound = true;
    public static boolean aboveGui = true;
    public static boolean strict = true;
    public static boolean debug = false;

    public static float durationSec = 5.5f;
    public static float size = 7f;
    public static float spinSpeed = 30f;
    public static float rayIntensity = 1f;
    public static float volume = 0.6f;
    public static int queueLimit = 5;

    public static String serverFilter = "";
    private static final String DEFAULT_HEADERS = "INSANE RARE DROP!|CRAZY RARE DROP!|VERY RARE DROP!|INSANE DROP!|RARE DROP!|PET DROP!|RARE REWARD!";
    private static String headerRaw = DEFAULT_HEADERS;
    private static String[] headerList;

    public static void init(File configRoot) {
        dir = new File(configRoot, "animationinaction");
        dir.mkdirs();
        logFile = new File(dir, "chatlog.txt");
        cfg = new Configuration(new File(dir, "animationinaction.cfg"));
        load();
    }

    private static String get(String key, String def, String comment) {
        return cfg.getString(key, "general", def, comment);
    }

    private static void put(String key, String val) {
        cfg.get("general", key, val).set(val);
    }

    private static float f(String s, float def, float min, float max) {
        try {
            float v = Float.parseFloat(s.trim());
            return Math.max(min, Math.min(max, v));
        } catch (Exception e) {
            return def;
        }
    }

    public static void load() {
        master = Boolean.parseBoolean(get("enabled", "true", "Master switch"));
        flash = Boolean.parseBoolean(get("screenFlash", "true", "White flashes at the start/end of the animation"));
        dim = Boolean.parseBoolean(get("dimBackground", "true", "Darken the screen behind the item"));
        sound = Boolean.parseBoolean(get("sound", "true", "Play a sound when an animation starts"));
        aboveGui = Boolean.parseBoolean(get("showOverMenus", "true", "Also draw while a menu/chest GUI is open"));
        strict = Boolean.parseBoolean(get("strictAntiSpoof", "true",
            "Only react to lines that START with a drop header AND carry the server's colour/bold formatting. "
          + "Lines typed by players always start with their name, so pasted fakes are ignored."));
        debug = Boolean.parseBoolean(get("debugLogChat", "false", "Write every chat line (with & colour codes) to chatlog.txt"));
        durationSec = f(get("durationSeconds", "5.5", "3 - 10"), 5.5f, 3f, 10f);
        size = f(get("itemSize", "7", "3 - 12"), 7f, 3f, 12f);
        spinSpeed = f(get("spinSpeedDegPerSec", "30", "30 - 360"), 30f, 30f, 360f);
        rayIntensity = f(get("rayIntensity", "1.0", "0.2 - 2"), 1f, 0.2f, 2f);
        volume = f(get("soundVolume", "0.6", "0 - 1"), 0.6f, 0f, 1f);
        int ver = Math.round(f(get("configVersion", "1", "internal - do not edit"), 1f, 0f, 99f));
        if (ver < 2) spinSpeed = 30f;
        put("configVersion", "2");
        queueLimit = Math.round(f(get("queueLimit", "5", "1 - 10"), 5f, 1f, 10f));
        serverFilter = get("onlyOnServerContaining", "",
            "Leave empty to work on every server. Example: fakepixel (matches against the server address you joined with)");
        headerRaw = get("dropHeaders", headerRaw,
            "Chat lines must START with one of these (separated by |) to be treated as a drop. Edit if your server words them differently.");
        rebuildHeaders();
        StringBuilder merged = new StringBuilder(headerRaw);
        for (String d : DEFAULT_HEADERS.split("\\|")) {
            boolean have = false;
            for (String h : headerList) if (h.equalsIgnoreCase(d)) have = true;
            if (!have) merged.append('|').append(d);
        }
        if (!merged.toString().equals(headerRaw)) {
            headerRaw = merged.toString();
            put("dropHeaders", headerRaw);
            rebuildHeaders();
        }
        cfg.save();
    }

    public static void save() {
        put("enabled", String.valueOf(master));
        put("screenFlash", String.valueOf(flash));
        put("dimBackground", String.valueOf(dim));
        put("sound", String.valueOf(sound));
        put("showOverMenus", String.valueOf(aboveGui));
        put("strictAntiSpoof", String.valueOf(strict));
        put("debugLogChat", String.valueOf(debug));
        put("durationSeconds", String.valueOf(durationSec));
        put("itemSize", String.valueOf(size));
        put("spinSpeedDegPerSec", String.valueOf(spinSpeed));
        put("rayIntensity", String.valueOf(rayIntensity));
        put("soundVolume", String.valueOf(volume));
        put("queueLimit", String.valueOf(queueLimit));
        DropCatalog.saveInto(cfg);
        cfg.save();
    }

    private static void rebuildHeaders() {
        List<String> out = new ArrayList<String>();
        for (String s : headerRaw.split("\\|")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        Collections.sort(out, new Comparator<String>() {
            @Override
            public int compare(String a, String b) { return b.length() - a.length(); }
        });
        headerList = out.toArray(new String[out.size()]);
    }

    public static String[] headers() {
        if (headerList == null) rebuildHeaders();
        return headerList;
    }

    public static synchronized void log(String line) {
        if (logFile == null) return;
        FileWriter w = null;
        try {
            w = new FileWriter(logFile, true);
            w.write(new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  " + line + "\n");
        } catch (IOException ignored) {
        } finally {
            if (w != null) try { w.close(); } catch (IOException ignored) {}
        }
    }
}
