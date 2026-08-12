package dev.herasy.alsepath.anscor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class SwitcherReload {

    private SwitcherReload() {
    }

    /**
     * Rebuilds the F3 game-mode switcher if it is currently open.
     *
     * Safe to call from another thread; the actual screen operation
     * is scheduled onto Minecraft's client thread.
     *
     * If the switcher is not currently open, this does nothing.
     * The newly registered modes will be picked up the next time
     * the switcher is opened normally.
     */
    public static void reload() {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            Screen currentScreen = client.currentScreen;

            if (currentScreen instanceof GameModeSwitcherReloadable reloadable) {
                reloadable.anscor$reloadSwitcherLayout();
            }
        });
    }
}