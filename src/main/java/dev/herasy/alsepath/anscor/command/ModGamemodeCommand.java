package dev.herasy.alsepath.anscor.command;

import net.minecraft.command.argument.EntityArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.herasy.alsepath.anscor.AnscorComponents;
import dev.herasy.alsepath.anscor.gamemode.CustomGameMode;
import dev.herasy.alsepath.anscor.gamemode.CustomGameModeComponent;
import dev.herasy.alsepath.anscor.gamemode.CustomGameModeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class ModGamemodeCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // Get the EXISTING vanilla /gamemode node.
            // We extend it instead of creating another generic <string> branch.
            var gamemodeNode = dispatcher.getRoot().getChild("gamemode");

            if (gamemodeNode == null) {
                return;
            }

            for (CustomGameMode mode : CustomGameModeRegistry.getAll()) {

                String commandName = mode.getId().getPath();

                // /gamemode anscor
                // /gamemode rpg_adventure
                var modeNode = CommandManager.literal(commandName)
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(context -> executeCustomGamemode(context, mode, null))

                        // /gamemode anscor <targets>
                        // /gamemode rpg_adventure <targets>
                        .then(CommandManager.argument(
                                "targets",
                                EntityArgumentType.players()
                        ).executes(context ->
                                executeCustomGamemode(
                                        context,
                                        mode,
                                        EntityArgumentType.getPlayers(context, "targets")
                                )
                        ))
                        .build();

                // Add the literal directly to vanilla /gamemode.
                gamemodeNode.addChild(modeNode);
            }
        });
    }

    private static int executeCustomGamemode(
            CommandContext<ServerCommandSource> context,
            CustomGameMode selectedMode,
            Collection<ServerPlayerEntity> targets
    ) {
        ServerCommandSource source = context.getSource();

        // No explicit target = executing player
        if (targets == null) {
            ServerPlayerEntity player = source.getPlayer();

            if (player == null) {
                source.sendError(
                        Text.literal("Only players can be assigned custom gameplay profiles.")
                );
                return 0;
            }

            applyCustomGamemode(player, selectedMode);

            source.sendFeedback(
                    () -> Text.literal("Gamemode updated to: ")
                            .append(selectedMode.getDisplayName()),
                    true
            );

            return 1;
        }

        // Explicit targets
        for (ServerPlayerEntity player : targets) {
            applyCustomGamemode(player, selectedMode);
        }

        int targetCount = targets.size();

        source.sendFeedback(
                () -> Text.literal(
                        "Set " + targetCount + " player(s) to "
                ).append(selectedMode.getDisplayName()),
                true
        );

        return targetCount;
    }

    private static void applyCustomGamemode(
            ServerPlayerEntity player,
            CustomGameMode selectedMode
    ) {
        // Apply vanilla underlying gameplay rules.
        player.changeGameMode(selectedMode.getVanillaFallback());

        // Store custom mode in Cardinal Components.
        CustomGameModeComponent component =
                AnscorComponents.CUSTOM_GAMEMODE.get(player);

        component.setCustomGameMode(selectedMode.getId());

        AnscorComponents.CUSTOM_GAMEMODE.sync(player);
    }
}