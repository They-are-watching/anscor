// src/main/java/dev/herasy/alsepath/anscor/Anscor.java
package dev.herasy.alsepath.anscor;

import dev.herasy.alsepath.anscor.command.ModGamemodeCommand; // Add import
import dev.herasy.alsepath.anscor.gamemode.CustomGameMode;
import dev.herasy.alsepath.anscor.gamemode.CustomGameModeRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Anscor implements ModInitializer {
	public static final String MOD_ID = "anscor";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Custom Game Mode Library!");

		// 1. Create a Hardcore Creative mode that shows up in F3 menu
		CustomGameModeRegistry.register(new CustomGameMode(
				Identifier.of(MOD_ID,"anscor"),
				Text.literal("Anscor"),
				Identifier.of(MOD_ID, "textures/gui/icons/heart.png"),
				true,
				GameMode.CREATIVE
		));

		// 2. Create a Hidden RPG Quest Mode (F3 hidden!)
		CustomGameModeRegistry.register(new CustomGameMode(
				Identifier.of(MOD_ID,"rpg_adventure"),
				Text.literal("Story Mode"),
				Identifier.of(MOD_ID, "textures/gui/icons/hunger.png"),
				false,
				GameMode.ADVENTURE
		));

		// Register new custom sub-commands
		ModGamemodeCommand.register();
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
