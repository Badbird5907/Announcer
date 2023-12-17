package com.semivanilla.announcer.object;

import com.semivanilla.announcer.animation.Animation;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class TitleInfo {
    private String title, subtitle, rawTitle, rawSubtitle;
    private Animation animation;
    private long ticksLeft;
    private UUID uuid;
    private int stay;
    private long fadeIn, fadeOut;
}
