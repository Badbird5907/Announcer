package com.semivanilla.announcer;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.semivanilla.announcer.listener.MainListener;
import com.semivanilla.announcer.manager.ConfigManager;
import com.semivanilla.announcer.manager.TitleManager;
import com.semivanilla.announcer.object.JoinConfig;
import com.semivanilla.announcer.object.TitleInfo;
import com.semivanilla.announcer.object.TitleUpdateRunnable;
import lombok.Getter;
import net.badbird5907.blib.bLib;
import net.badbird5907.blib.util.Tasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.UUID;

public final class Announcer extends JavaPlugin {
    @Getter
    private static Announcer instance;
    @Getter
    private static ConfigManager configManager;
    private FileConfiguration config = null;

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        bLib.create(this);
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        configManager = new ConfigManager();
        configManager.init();
        Bukkit.getLogger().info("Loaded (" + ConfigManager.getMessages().size() + ") messages!");
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        Tasks.runAsyncTimer(() -> {
            String s = ConfigManager.getNextMessage();
            if (s != null) {
                Component component = miniMessage.deserialize(s.trim());
                Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(component));
            }
        }, ConfigManager.getTicks(), ConfigManager.getTicks());
        new TitleUpdateRunnable().runTaskTimer(this, 10, 1);
        if (ConfigManager.isEnableBungee()) {
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "bungeejoin:title", (s, player, bytes) -> {
                ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
                String subchannel = in.readUTF();
                if (!subchannel.equalsIgnoreCase("title")) return;
                String uuidString = in.readUTF();
                boolean firstJoin = in.readBoolean();
                UUID uuid = UUID.fromString(uuidString);
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) return;

                JoinConfig config = firstJoin ? ConfigManager.getNewPlayer() : ConfigManager.getReturning();
                if (Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId())) {
                    if (config.isEnableBedrockTitle()) {
                        Tasks.runLater(() -> {
                            TitleManager.showTitle(p, config.getBedrockTitle(), config.getBedrockSubtitle(), config.getFadeInBedrock(), config.getBedrockDuration(), config.getFadeOutBedrock(), false);
                        }, 50l);
                        return;
                    }
                }

                TitleManager.showTitle(player, config.getTitle(), config.getSubtitle(), config.getFadeIn(), config.getTitleDuration(), config.getFadeOut(), true);
                if (config.isEnableSound()) {
                    Sound sound = null;
                    try {
                        sound = Sound.valueOf(config.getSoundName());
                    } catch (IllegalArgumentException e) {
                        for (Sound value : Sound.values()) {
                            if (value.getKey().getKey().equalsIgnoreCase(config.getSoundName().replace("minecraft:", ""))) {
                                sound = value;
                            }
                        }
                        if (sound == null) {
                            Bukkit.getLogger().severe("Invalid sound name: " + config.getSoundName());
                            return;
                        }
                    }
                    player.playSound(player.getLocation(), sound, (float) config.getVolume(), (float) config.getPitch());
                }
            });
        }
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(new File(getDataFolder() + "/config.yml"));
        }
        return config;
    }
}
