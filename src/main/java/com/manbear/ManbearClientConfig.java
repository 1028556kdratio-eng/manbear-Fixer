package com.manbear;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings that only exist on your own computer. */
public class ManbearClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue MIC_SAMPLING;
    public static final ForgeConfigSpec.BooleanValue SHARE_VOICE;
    public static final ForgeConfigSpec.BooleanValue SPEECH_RECOGNITION;
    public static final ForgeConfigSpec.IntValue MIC_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue VOICE_PITCH;
    public static final ForgeConfigSpec.DoubleValue VOICE_VOLUME;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        MIC_SAMPLING = b.comment("Listen to your microphone and keep short clips of your voice IN MEMORY ONLY (nothing is saved or sent anywhere) so the Manbear can play them back. Set to false to turn it off completely.").define("micSampling", true);
        SHARE_VOICE = b.comment("ON by default (this mod is meant for friends who all know their voice gets mimicked). If true, your recorded phrases are also sent to the server you are playing on (RAM only there) so the Manbear can play YOUR voice to OTHER players. Toggle in game with /manbearvoice share on|off").define("shareVoiceWithServer", true);
        SPEECH_RECOGNITION = b.comment("Windows only: uses the speech recognition built into Windows (nothing is downloaded, nothing leaves your PC) to hear you say 'man bear' or 'bear'. Saying it enrages him and may trigger a chase, fake chase or despawn.").define("speechRecognition", true);
        MIC_THRESHOLD = b.comment("How loud you must be (0-32767) before a phrase is recorded. Raise it if background noise gets recorded.").defineInRange("micThreshold", 450, 50, 20000);
        VOICE_PITCH = b.comment("Playback pitch. Below 1 = deeper and creepier.").defineInRange("voicePitch", 0.88, 0.5, 1.5);
        VOICE_VOLUME = b.defineInRange("voiceVolume", 1.0, 0.0, 4.0);
        SPEC = b.build();
    }
}
