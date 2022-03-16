package com.semivanilla.announcer.listener;

import com.semivanilla.announcer.manager.ConfigManager;
import com.semivanilla.announcer.manager.TitleManager;
import com.semivanilla.announcer.object.JoinConfig;
import com.semivanilla.announcer.util.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MainListener implements Listener {
    private static final Set<UUID> newPlayers = ConcurrentHashMap.newKeySet();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        File playerData = new File(Bukkit.getWorlds().get(0).getWorldFolder() + "/playerdata/" + event.getUniqueId() + ".dat");
        if (!playerData.exists()) {
            newPlayers.add(event.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        JoinConfig config;
        if (UUIDUtil.contains(newPlayers, event.getPlayer().getUniqueId())) {
            UUIDUtil.remove(newPlayers, event.getPlayer().getUniqueId());
            config = ConfigManager.getNewPlayer();
        } else {
            config = ConfigManager.getReturning();
        }
        if (Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateApi.getInstance().isFloodgatePlayer(event.getPlayer().getUniqueId()))
            return;
        boolean showTitle = config.isEnableTitle();
        if (showTitle) {
            TitleManager.showTitle(event.getPlayer(), config.getTitle(), config.getSubtitle(), config.getFadeIn(), config.getTitleDuration(), config.getFadeOut());
        }
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
            event.getPlayer().playSound(event.getPlayer().getLocation(), sound, (float) config.getVolume(), (float) config.getPitch());
        }
    }
}
