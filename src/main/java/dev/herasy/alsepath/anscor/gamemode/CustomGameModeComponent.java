// src/main/java/dev/herasy/alsepath/anscor/gamemode/CustomGameModeComponent.java
package dev.herasy.alsepath.anscor.gamemode;

import org.ladysnake.cca.api.v3.component.Component;
import net.minecraft.util.Identifier;

public interface CustomGameModeComponent extends Component {
    Identifier getCustomGameMode();
    void setCustomGameMode(Identifier id);
}
