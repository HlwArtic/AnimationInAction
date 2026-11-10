package com.artic.aia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatDetector {
    private static final String CODE = "\u00a7[0-9a-fk-orA-FK-OR]";
    private static final Pattern CODES = Pattern.compile(CODE);
    private static final Pattern STYLE_ONLY_PREFIX = Pattern.compile("(?:\\s|" + CODE + ")*");
    private static final Pattern HAS_STYLE = Pattern.compile(".*\u00a7[0-9a-fA-FlL].*", Pattern.DOTALL);
    private static final Pattern NAME_COLOUR = Pattern.compile("\u00a7([0-9a-fA-F])");
    private static final Pattern MF_TAIL = Pattern.compile(
        "\\s*\\([^()]*(?:Magic\\s*Find|\\bMF\\b|\u272F)[^()]*\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_JUNK = Pattern.compile("^[\\s:!\\-\u2013\u2014]+");
    private static final Pattern OTHER_PLAYER = Pattern.compile(
        "(?:^|\\s)(?:\\[[^\\]]{1,12}\\]\\s*)?([A-Za-z0-9_]{3,16})\\s+(?:has\\s+)?(?:found|got|obtained|received|unlocked|dropped|opened|looted)\\b",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern DROP_HEADER = Pattern.compile("^((?:[A-Z]{2,}\\s+){0,3}(?:DROP|CROP|CATCH|FIND|REWARD)!)");

    private static final Pattern UNLOCK = Pattern.compile(
        "^(?:\\[[^\\]]{1,16}\\]\\s*)*([A-Za-z0-9_]{3,16})\\s+(?:has\\s+)?(?:unlocked|obtained|received|looted|earned|claimed)\\s+(.+?)\\s*!?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_STYLE = Pattern.compile(".*\u00a7[0-9a-fA-FlLrR].*", Pattern.DOTALL);
    private static final Pattern TRAILING_COUNT = Pattern.compile("(?:\\s*\\(\\d+\\))+\\s*$");
    private static final Pattern QTY_WRAP = Pattern.compile("^\\(\\s*(?:\\d+\\s*[xX]\\s*)?(.+?)\\s*\\)$");
    private static final Pattern QTY_PREFIX = Pattern.compile("^\\d+\\s*[xX]\\s+");
    private static final Pattern LEAD_IN = Pattern.compile(
        "^(?:(?:you\\s+)?(?:dug\\s+out|found|got|obtained|received|caught|fished\\s+up|looted|unearthed)\\s+)?(?:an?\\s+|the\\s+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern QTY_SUFFIX = Pattern.compile("\\s*\\(?\\s*(?:[xX]\\s*\\d[\\d,]*|\\d[\\d,]*\\s*[xX])\\s*\\)?\\s*$");
    private static final Pattern END_PUNCT = Pattern.compile("[\\s.!]+$");

    private final Map<String, Long> recent = new HashMap<String, Long>();

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || e.message == null) return;
        try {
            handle(e.message.getFormattedText());
        } catch (Throwable t) {
            AIAConfig.log("ERROR in ChatDetector: " + t);
        }
    }

    private void handle(String formatted) {
        String plain = CODES.matcher(formatted).replaceAll("").trim();
        if (AIAConfig.debug) AIAConfig.log("LINE  " + formatted.replace('\u00a7', '&'));
        if (!AIAConfig.master || !serverAllowed()) return;

        Minecraft mc = Minecraft.getMinecraft();
        String me = mc.thePlayer != null ? mc.thePlayer.getName() : "";

        String header = null;
        for (String h : AIAConfig.headers()) {
            if (plain.startsWith(h)) { header = h; break; }
        }
        if (header == null) {
            Matcher hm = DROP_HEADER.matcher(plain);
            if (hm.find()) header = hm.group(1);
        }

        String rest;
        if (header != null) {
            if (AIAConfig.strict) {
                int idx = formatted.indexOf(header);
                if (idx < 0) { reject("header is split by formatting codes"); return; }
                String prefix = formatted.substring(0, idx);
                if (!STYLE_ONLY_PREFIX.matcher(prefix).matches()) { reject("text before header"); return; }
                if (!HAS_STYLE.matcher(prefix).matches()) { reject("header carries no colour/bold (not a server line)"); return; }
            }
            rest = LEADING_JUNK.matcher(plain.substring(header.length())).replaceFirst("").trim();
            Matcher om = OTHER_PLAYER.matcher(rest);
            if (om.find()) {
                String who = om.group(1);
                if (!who.equalsIgnoreCase("you") && !who.equalsIgnoreCase(me)) { reject("line is about another player: " + who); return; }
                rest = LEADING_JUNK.matcher(rest.substring(om.end())).replaceFirst("").trim();
            }
        } else {
            Matcher um = UNLOCK.matcher(plain);
            if (!um.find()) return;
            if (AIAConfig.strict && !ANY_STYLE.matcher(formatted).matches()) { reject("unlock line has no server formatting"); return; }
            if (!um.group(1).equalsIgnoreCase(me)) { reject("someone else unlocked it: " + um.group(1)); return; }
            header = "unlocked";
            rest = um.group(2).trim();
        }

        long now = System.currentTimeMillis();
        Long last = recent.get(plain);
        if (last != null && now - last.longValue() < 2500) { reject("duplicate within 2.5s"); return; }
        recent.put(plain, now);
        if (recent.size() > 64) {
            for (Iterator<Map.Entry<String, Long>> it = recent.entrySet().iterator(); it.hasNext(); ) {
                if (now - it.next().getValue().longValue() > 10000) it.remove();
            }
        }

        String body = cleanBody(rest);

        DropEntry match = null;
        for (DropEntry d : DropCatalog.ALL) {
            if (d.matches(header, body)) { match = d; break; }
        }
        if (match == null) {
            if (AIAConfig.debug) AIAConfig.log("  -> drop line, but no catalog entry matches: " + body);
            return;
        }
        if (!match.enabled) {
            if (AIAConfig.debug) AIAConfig.log("  -> matched " + match.id + " but it is switched off");
            return;
        }

        String name = match.name;
        if (match.category == Category.PETS) {
            name = match.name;
        } else if (!match.book && match.pattern != null) {
            Matcher nm = match.pattern.matcher(body);
            if (nm.find() && !nm.group().trim().isEmpty() && nm.group().trim().length() <= 40) name = nm.group().trim();
        } else if (!match.book && !body.isEmpty() && body.length() <= 40) {
            name = body;
        }

        Rarity rarity = match.override;
        if (rarity == null && !match.fixedRarity) {
            rarity = detectRarity(formatted, header, name);
        }
        if (rarity == null) rarity = match.defaultRarity;

        if (AIAConfig.debug) AIAConfig.log("  -> TRIGGER " + match.id + " as \"" + name + "\" (" + rarity + ")");
        AnimationManager.INSTANCE.submit(match, name, rarity, false);
    }

    private static String cleanBody(String rest) {
        String b = rest.trim();
        for (int i = 0; i < 4; i++) {
            String before = b;
            b = TRAILING_COUNT.matcher(b).replaceFirst("");
            b = MF_TAIL.matcher(b).replaceFirst("");
            b = b.trim();
            if (b.equals(before)) break;
        }
        Matcher q = QTY_WRAP.matcher(b);
        if (q.find()) b = q.group(1).trim();
        b = QTY_PREFIX.matcher(b).replaceFirst("").trim();
        b = LEAD_IN.matcher(b).replaceFirst("").trim();
        b = QTY_SUFFIX.matcher(b).replaceFirst("").trim();
        return END_PUNCT.matcher(b).replaceFirst("").trim();
    }

    private static void reject(String why) {
        if (AIAConfig.debug) AIAConfig.log("  -> rejected: " + why);
    }

    private static Rarity detectRarity(String formatted, String header, String itemName) {
        int at = itemName == null ? -1 : formatted.lastIndexOf(itemName);
        if (at < 0) {
            int idx = formatted.indexOf(header);
            if (idx < 0) return null;
            at = formatted.length();
            Matcher m = NAME_COLOUR.matcher(formatted.substring(idx + header.length()));
            while (m.find()) {
                Rarity r = Rarity.fromCode(m.group(1).charAt(0));
                if (r != null && r != Rarity.COMMON) return r;
            }
            return null;
        }
        char colour = 0;
        String before = formatted.substring(0, at);
        for (int i = 0; i + 1 < before.length(); i++) {
            if (before.charAt(i) != '\u00a7') continue;
            char c = Character.toLowerCase(before.charAt(i + 1));
            if (c == 'r') colour = 0;
            else if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) colour = c;
            i++;
        }
        if (colour == 0) return null;
        Rarity r = Rarity.fromCode(colour);
        return (r == null || r == Rarity.COMMON) ? null : r;
    }

    private static boolean serverAllowed() {
        String f = AIAConfig.serverFilter == null ? "" : AIAConfig.serverFilter.trim().toLowerCase();
        if (f.isEmpty()) return true;
        ServerData sd = Minecraft.getMinecraft().getCurrentServerData();
        return sd != null && sd.serverIP != null && sd.serverIP.toLowerCase().contains(f);
    }
}
