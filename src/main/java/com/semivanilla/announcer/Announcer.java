package com.semivanilla.announcer;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.semivanilla.announcer.listener.MainListener;
import com.semivanilla.announcer.manager.ConfigManager;
import com.semivanilla.announcer.manager.TitleManager;
import com.semivanilla.announcer.object.JoinConfig;
import com.semivanilla.announcer.object.TitleUpdateRunnable;
import lombok.Getter;
import net.badbird5907.blib.bLib;
import net.badbird5907.blib.util.Tasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
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
    private static MiniMessage miniMessage;
    private NamespacedKey key;

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
        MiniMessage.Builder miniMessageBuilder = MiniMessage.builder()
                .tags(TagResolver.builder().resolvers(TagResolver.standard()).resolvers(configManager.getResolvers()).build());
        miniMessage = miniMessageBuilder.build();
        Bukkit.getLogger().info("Loaded (" + ConfigManager.getMessages().size() + ") messages!");
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        key = NamespacedKey.fromString("announcements_disable", this);
        Tasks.runAsyncTimer(() -> {
            String s = ConfigManager.getNextMessage();
            if (s != null) {
                Component component = miniMessage.deserialize(s.trim());
                Bukkit.getOnlinePlayers().stream().filter(
                        player -> !player.getPersistentDataContainer().has(key, PersistentDataType.BYTE)
                ).forEach(player -> player.sendMessage(component));
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equalsIgnoreCase("announcer")) {
            if (args.length == 0) {
                sender.sendMessage(miniMessage.deserialize("<aqua>Announcer v" + getDescription().getVersion() + " by Badbird5907"));
                sender.sendMessage(miniMessage.deserialize("<aqua>Use /announcer reload to reload the config"));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("announcer.reload")) {
                    sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to use this command!"));
                    return true;
                }
                long start = System.currentTimeMillis();
                reloadConfig();
                configManager.loadConfig();
                long end = System.currentTimeMillis();
                sender.sendMessage(miniMessage.deserialize("<green>Loaded " + ConfigManager.getMessages().size() + " messages!"));
                sender.sendMessage(miniMessage.deserialize("<green>Reloaded announcer config in " + (end - start) + "ms!"));
                return true;
            }
        } else if (label.equalsIgnoreCase("toggleannouncements")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(miniMessage.deserialize("<red>Only players can use this command!"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("announcer.toggle")) {
                player.sendMessage(miniMessage.deserialize("<red>You do not have permission to use this command!"));
                return true;
            }
            // use persistent data container to store this
            if (key == null) throw new RuntimeException("Failed to create namespaced key");
            if (player.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                player.getPersistentDataContainer().remove(key);
                player.sendMessage(miniMessage.deserialize("<green>Announcements enabled!"));
            } else {
                player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
                player.sendMessage(miniMessage.deserialize("<red>Announcements disabled!"));
            }
        }
        return true;
    }
}
