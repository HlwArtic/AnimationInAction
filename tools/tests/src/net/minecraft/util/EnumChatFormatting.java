package net.minecraft.util; public class EnumChatFormatting { public static String getTextWithoutFormattingCodes(String s){ return s==null?null:s.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]",""); } }
