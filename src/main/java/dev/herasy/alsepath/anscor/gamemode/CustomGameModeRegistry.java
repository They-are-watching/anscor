// src/main/java/dev/herasy/alsepath/anscor/gamemode/CustomGameModeRegistry.java
package dev.herasy.alsepath.anscor.gamemode;

import net.minecraft.util.Identifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomGameModeRegistry {
    private static final Map<Identifier, CustomGameMode> REGISTRY = new LinkedHashMap<>();

    public static void register(CustomGameMode mode) {
        REGISTRY.put(mode.getId(), mode);
    }

    public static CustomGameMode get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static Collection<CustomGameMode> getAll() {
        return REGISTRY.values();
    }

    public static Collection<CustomGameMode> getF3VisibleModes() {
        return REGISTRY.values().stream().filter(CustomGameMode::shouldRevealInF3).toList();
    }
}
