package com.artic.aia;

public enum Rarity {
    COMMON("COMMON", 'f', 0xFFFFFF, 0xD0D0D0),
    UNCOMMON("UNCOMMON", 'a', 0x55FF55, 0xB5FFB5),
    RARE("RARE", '9', 0x5577FF, 0xA0B8FF),
    EPIC("EPIC", '5', 0xB02CFF, 0xE0A0FF),
    LEGENDARY("LEGENDARY", '6', 0xFFAA00, 0xFFE27A),
    MYTHIC("MYTHIC", 'd', 0xE24BFF, 0xB070FF),
    DIVINE("DIVINE", 'b', 0x55FFFF, 0xC8FFFF),
    SPECIAL("SPECIAL", 'c', 0xFF5555, 0xFFB0B0);

    public final String label;
    public final char code;
    public final int primary;
    public final int secondary;

    Rarity(String label, char code, int primary, int secondary) {
        this.label = label;
        this.code = code;
        this.primary = primary;
        this.secondary = secondary;
    }

    public static Rarity fromCode(char c) {
        c = Character.toLowerCase(c);
        for (Rarity r : values()) {
            if (r.code == c) return r;
        }
        return null;
    }
}
