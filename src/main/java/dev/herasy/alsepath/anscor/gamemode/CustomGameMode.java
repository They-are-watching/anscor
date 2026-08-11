// src/main/java/dev/herasy/alsepath/anscor/gamemode/CustomGameMode.java
package dev.herasy.alsepath.anscor.gamemode;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public class CustomGameMode {
    private final Identifier id;
    private final Text displayName;
    private final Identifier iconTexture;
    private final boolean revealInF3;
    private final GameMode vanillaFallback;

    public CustomGameMode(Identifier id, Text displayName, Identifier iconTexture, boolean revealInF3, GameMode vanillaFallback) {
        this.id = id;
        this.displayName = displayName;
        this.iconTexture = iconTexture;
        this.revealInF3 = revealInF3;
        this.vanillaFallback = vanillaFallback;
    }

    public Identifier getId() { return id; }
    public Text getDisplayName() { return displayName; }
    public Identifier getIconTexture() { return iconTexture; }
    public boolean shouldRevealInF3() { return revealInF3; }
    public GameMode getVanillaFallback() { return vanillaFallback; }
}
