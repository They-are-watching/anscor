// src/main/java/dev/herasy/alsepath/anscor/gamemode/PlayerGameModeComponent.java
package dev.herasy.alsepath.anscor.gamemode;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

public class PlayerGameModeComponent implements CustomGameModeComponent {
    private Identifier currentMode = null;

    @Override
    public Identifier getCustomGameMode() { return currentMode; }

    @Override
    public void setCustomGameMode(Identifier id) { this.currentMode = id; }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        if (tag.contains("CustomGameMode")) {
            this.currentMode = Identifier.of(tag.getString("CustomGameMode"));
        } else {
            this.currentMode = null;
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        if (currentMode != null) {
            tag.putString("CustomGameMode", currentMode.toString());
        }
    }
}
