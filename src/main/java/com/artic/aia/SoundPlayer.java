package com.artic.aia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.audio.SoundCategory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public final class SoundPlayer {
    private SoundPlayer() {}

    private static final String RESOURCE = "/assets/animationinaction/sounds/rareDropSound.wav";
    private static byte[] data;

    public static void init(File base) {
        InputStream in = null;
        try {
            in = SoundPlayer.class.getResourceAsStream(RESOURCE);
            if (in == null) { AIAConfig.log("bundled sound is missing from the jar: " + RESOURCE); return; }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            data = out.toByteArray();
        } catch (Exception e) {
            AIAConfig.log("could not load the bundled sound: " + e);
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }

    public static void play(Rarity rarity) {
        if (!AIAConfig.sound || AIAConfig.volume <= 0f || data == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        float master = 1f;
        try {
            GameSettings gs = mc.gameSettings;
            if (gs != null) master = gs.getSoundLevel(SoundCategory.MASTER);
        } catch (Throwable ignored) {}
        final float v = AIAConfig.volume * master;
        Thread t = new Thread(new Runnable() {
            @Override public void run() { playBytes(data, v); }
        }, "AIA-sound");
        t.setDaemon(true);
        t.start();
    }

    private static void playBytes(byte[] bytes, float volume) {
        AudioInputStream in = null;
        AudioInputStream use = null;
        try {
            in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
            AudioFormat base = in.getFormat();
            use = in;
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(base.getEncoding())) {
                AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), 16,
                    base.getChannels(), base.getChannels() * 2, base.getSampleRate(), false);
                use = AudioSystem.getAudioInputStream(pcm, in);
            }
            final Clip clip = AudioSystem.getClip();
            clip.open(use);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float db = (float) (20.0 * Math.log10(Math.max(0.0001f, Math.min(1f, volume))));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }
            clip.addLineListener(new LineListener() {
                @Override public void update(LineEvent e) {
                    if (e.getType() == LineEvent.Type.STOP) clip.close();
                }
            });
            clip.start();
        } catch (Throwable t) {
            AIAConfig.log("sound failed: " + t);
        } finally {
            try { if (use != null && use != in) use.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }
}
