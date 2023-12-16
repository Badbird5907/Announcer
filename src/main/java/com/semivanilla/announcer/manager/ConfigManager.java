package com.semivanilla.announcer.manager;

import com.semivanilla.announcer.Announcer;
import com.semivanilla.announcer.object.JoinConfig;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    @Getter
    private static final List<String> messages = new ArrayList<>();
    @Getter
    public static boolean randomOrder;
    @Getter
    private static TimeUnit timeUnit;
    @Getter
    private static int interval;
    @Getter
    private static long ticks;
    @Getter
    private static double gradientSpeed;

    @Getter
    private static JoinConfig returning, newPlayer;

    @Getter
    private static String color1, color2, color3;
    private static int index = -1;

    public static String getNextMessage() {
        if (messages.isEmpty()) return null;
        if (!randomOrder) {
            index++;
            if (index >= messages.size()) {
                index = 0;
            }
            return messages.get(index);
        }
        return messages.get(new Random().nextInt(messages.size()));
    }

    public void init() {
        Announcer plugin = Announcer.getInstance();
        if (!new File(plugin.getDataFolder() + "/config.yml").exists()) {
            plugin.saveDefaultConfig();
        }
        loadConfig();
    }

    public void loadConfig() {
        /*
        for (String key : getConfig().getKeys(true)) {
            if (key.startsWith("messages.")) {
                StringBuilder sb = new StringBuilder();
                for (String s : getConfig().getStringList(key)) {
                    sb.append(s).append("\n");
                }
                messages.add(sb.toString());
            }
        }
         */
        List<?> messagesList = getConfig().getList("messages");
        for (Object obj : messagesList) {
            if (obj instanceof List<?> list) {
                StringBuilder sb = new StringBuilder();
                for (Object o : list) {
                    if (o instanceof String str) {
                        sb.append(str).append("\n");
                    }
                }
                messages.add(sb.toString());
            }
        }

        timeUnit = TimeUnit.valueOf(getConfig().getString("interval-time-unit"));
        interval = getConfig().getInt("interval");
        randomOrder = getConfig().getBoolean("random-message-order");

        ticks = timeUnit.toSeconds(interval) * 20;
        newPlayer = new JoinConfig(
                getConfig().getBoolean("new-player.join-sounds.enabled"),
                getConfig().getBoolean("new-player.title-settings.enabled"),
                getConfig().getInt("new-player.title-settings.duration-seconds"),
                getConfig().getInt("new-player.title-settings.bedrock.duration-seconds"),
                getConfig().getLong("new-player.title-settings.fade-in"),
                getConfig().getLong("new-player.title-settings.fade-out"),
                getConfig().getLong("new-player.title-settings.bedrock.fade-in"),
                getConfig().getLong("new-player.title-settings.bedrock.fade-out"),
                getConfig().getString("new-player.title-settings.title"),
                getConfig().getString("new-player.title-settings.subtitle"),
                getConfig().getString("new-player.join-sounds.name"),
                getConfig().getString("new-player.join-sounds.source"),
                getConfig().getDouble("new-player.join-sounds.pitch"),
                getConfig().getDouble("new-player.join-sounds.volume"),
                getConfig().getBoolean("new-player.title-settings.bedrock.enable"),
                getConfig().getString("new-player.title-settings.bedrock.title"),
                getConfig().getString("new-player.title-settings.bedrock.subtitle")
                );
        returning = new JoinConfig(
                getConfig().getBoolean("returning-player.join-sounds.enabled"),
                getConfig().getBoolean("returning-player.title-settings.enabled"),
                getConfig().getInt("returning-player.title-settings.duration-seconds"),
                getConfig().getInt("returning-player.title-settings.bedrock.duration-seconds"),
                getConfig().getLong("returning-player.title-settings.fade-in"),
                getConfig().getLong("returning-player.title-settings.fade-out"),
                getConfig().getLong("returning-player.title-settings.bedrock.fade-in"),
                getConfig().getLong("returning-player.title-settings.bedrock.fade-out"),
                getConfig().getString("returning-player.title-settings.title"),
                getConfig().getString("returning-player.title-settings.subtitle"),
                getConfig().getString("returning-player.join-sounds.name"),
                getConfig().getString("returning-player.join-sounds.source"),
                getConfig().getDouble("returning-player.join-sounds.pitch"),
                getConfig().getDouble("returning-player.join-sounds.volume"),
                getConfig().getBoolean("returning-player.title-settings.bedrock.enable"),
                getConfig().getString("returning-player.title-settings.bedrock.title"),
                getConfig().getString("returning-player.title-settings.bedrock.subtitle")
                );
        gradientSpeed = getConfig().getDouble("gradient.speed");
        color1 = getConfig().getString("gradient.color-1");
        color2 = getConfig().getString("gradient.color-2");
        color3 = getConfig().getString("gradient.color-3");
    }

    public FileConfiguration getConfig() {
        return Announcer.getInstance().getConfig();
    }
}
