package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.LegacyOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

import java.time.LocalTime;

/** Client-side-only world tweaks: fixed or moving time of day, and weather. The server is never told. */
public class WorldTweaks {
    private static double running = -1;

    public static void tick(MinecraftClient c) {
        ClientWorld w = c.world;
        if (w == null) return;
        if (ModConfig.timeChangerEnabled) {
            double base = LegacyOptions.s("time_opts", "time").n;
            double speed = LegacyOptions.s("time_opts", "speed").n;
            long t;
            if (LegacyOptions.s("time_opts", "real_time").b) {
                double hours = LocalTime.now().toSecondOfDay() / 3600.0;
                t = (long) (((hours - 6.0) * 1000.0 % 24000.0 + 24000.0) % 24000.0);
            } else if (speed <= 0) {
                running = -1;
                t = (long) base;
            } else {
                running = running < 0 ? base : (running + speed) % 24000.0;
                t = (long) running;
            }
            w.setTime(w.getTime(), t, false);
        } else {
            running = -1;
        }
        if (ModConfig.clearWeatherEnabled) {
            float rain = (float) LegacyOptions.s("weather_opts", "rain").n;
            w.setRainGradient(rain);
            w.setThunderGradient(LegacyOptions.s("weather_opts", "thunder").b ? rain : 0.0F);
        }
    }
}