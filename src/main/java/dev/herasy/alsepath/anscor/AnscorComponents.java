// src/main/java/dev/herasy/alsepath/anscor/AnscorComponents.java
package dev.herasy.alsepath.anscor;

import dev.herasy.alsepath.anscor.gamemode.CustomGameModeComponent;
import dev.herasy.alsepath.anscor.gamemode.PlayerGameModeComponent;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

public class AnscorComponents implements EntityComponentInitializer {
    public static final ComponentKey<CustomGameModeComponent> CUSTOM_GAMEMODE =
            ComponentRegistry.getOrCreate(Identifier.of("anscor", "custom_gamemode"), CustomGameModeComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Automatically hook data component tracking to all player instances
        registry.registerForPlayers(CUSTOM_GAMEMODE, player -> new PlayerGameModeComponent(), RespawnCopyStrategy.CHARACTER);
    }
}
