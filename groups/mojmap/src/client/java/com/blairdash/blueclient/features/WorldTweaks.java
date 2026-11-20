package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.LegacyOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.time.LocalTime;

/** Client-side time and weather settings; TimeChangerMixin supplies the time to the client clock. */
public class WorldTweaks {
    private static double running = -1;
    private static long timeOfDay = 6000;

    public static long timeOfDay() { return timeOfDay; }

    public static void tick(Minecraft c) {
        ClientLevel w = c.level;
        if (w == null) return;
        if (ModConfig.timeChangerEnabled) {
            double base = LegacyOptions.s("time_opts", "time").n;
            double speed = LegacyOptions.s("time_opts", "speed").n;
            if (LegacyOptions.s("time_opts", "real_time").b) {
                double hours = LocalTime.now().toSecondOfDay() / 3600.0;
                timeOfDay = (long) (((hours - 6.0) * 1000.0 % 24000.0 + 24000.0) % 24000.0);
            } else if (speed <= 0) {
                running = -1;
                timeOfDay = (long) base;
            } else {
                running = running < 0 ? base : (running + speed) % 24000.0;
                timeOfDay = (long) running;
            }
        } else {
            running = -1;
        }
        if (ModConfig.clearWeatherEnabled) {
            float rain = (float) LegacyOptions.s("weather_opts", "rain").n;
            w.setRainLevel(rain);
            w.setThunderLevel(LegacyOptions.s("weather_opts", "thunder").b ? rain : 0.0F);
        }
    }
}
