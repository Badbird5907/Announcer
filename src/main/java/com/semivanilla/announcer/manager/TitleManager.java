package com.semivanilla.announcer.manager;

import com.semivanilla.announcer.Announcer;
import com.semivanilla.announcer.animation.Animation;
import com.semivanilla.announcer.animation.impl.GradientAnimation;
import com.semivanilla.announcer.object.TitleInfo;
import lombok.Getter;
import net.badbird5907.blib.util.Logger;
import net.badbird5907.blib.util.Tasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TitleManager {
    @Getter
    private static final Map<UUID, TitleInfo> titles = new ConcurrentHashMap<>();

    public static void showTitle(Player player, String title, String subtitle, long fadeIn, int stay, long fadeOut, boolean animate) {
        Logger.debug("Showing title to " + player.getName() + " title: " + title + " subtitle: " + subtitle + " fadeIn: " + fadeIn + " stay: " + stay + " fadeOut: " + fadeOut);
        if (animate) {
            Animation animation = new GradientAnimation();
            Component title1 = parseTitle(title, animation);
            Component subtitle1 = parseTitle(subtitle, animation);
            showTitle(player, title1, subtitle1, fadeIn, stay, fadeOut);
            long ticksLeft = stay * 20L;
            TitleInfo info = new TitleInfo(title, subtitle, title, subtitle, animation, ticksLeft, player.getUniqueId(), stay, fadeIn, fadeOut);
            titles.put(player.getUniqueId(), info);
        } else {
            Component title1 = parseTitle(title);
            Component subtitle1 = parseTitle(subtitle);
            showTitle(player, title1, subtitle1, fadeIn, stay, fadeOut);
            Tasks.runLater(() -> { // We need to send the title twice because bedrock or geyser bug
                showTitle(player, title1, subtitle1, fadeIn, stay, fadeOut);
            }, 5L);
        }
    }

    public static void update(TitleInfo info) {
        if (Bukkit.getPlayer(info.getUuid()) == null) {
            titles.remove(info.getUuid());
            return;
        }
        Player player = Bukkit.getPlayer(info.getUuid());
        if (info.getTicksLeft() <= 0) {
            titles.remove(info.getUuid());
            return;
        }
        info.setTicksLeft(info.getTicksLeft() - 1);
        Component title = parseTitle(info.getRawTitle(), info.getAnimation());
        Component subtitle = parseTitle(info.getRawSubtitle(), info.getAnimation());
        boolean last = info.getTicksLeft() <= 0;
        if (last) {
            showTitle(player, title, subtitle, 0, 0, info.getFadeOut());
        } else {
            showTitle(player, title, subtitle, 0, 1, 0);
        }
    }

    public static Component parseTitle(String title, Animation animation) {
        if (title.contains("<animate>")) {
            if (title.contains("</animate>"))
                title = title.replace("<animate>", "<gradient:" + ConfigManager.getColor1() + ":" + ConfigManager.getColor2() + ":" + ConfigManager.getColor3() + ":" + animation.nextValue() + ">").replace("</animate>", "</gradient>");
            else throw new RuntimeException("Missing closing animate tag (</animate> expected)");
        }
        return parseTitle(title);
    }

    private static final Map<String, Component> titleCache = new HashMap<>();
    public static Component parseTitle(String title) {
        if (titleCache.containsKey(title)) return titleCache.get(title);
        // minimessage may be expensive, so we'll cache the result
        Component deserialize = Announcer.getMiniMessage().deserialize(title);
        titleCache.put(title, deserialize);
        return deserialize;
    }

    public static void showTitle(Player player, Component title, Component subtitle, long fadeIn, int stay, long fadeOut) {
        Title.Times times = Title.Times.times(Duration.ofMillis(fadeIn), Duration.ofSeconds(stay), Duration.ofMillis(fadeOut));
        Title t = Title.title(title, subtitle, times);
        player.showTitle(t);
    }
}
