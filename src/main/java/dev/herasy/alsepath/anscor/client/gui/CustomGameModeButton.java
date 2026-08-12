// src/main/java/dev/herasy/alsepath/anscor/client/gui/CustomGameModeButton.java
package dev.herasy.alsepath.anscor.client.gui;

import dev.herasy.alsepath.anscor.gamemode.CustomGameMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.util.Identifier;

public class CustomGameModeButton extends ClickableWidget implements SelectableButton {
    private final CustomGameMode mode;
    private boolean selected;

    // Vanilla Atlas Keys
    private static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("gamemode_switcher/slot");
    private static final Identifier SELECTION_TEXTURE = Identifier.ofVanilla("gamemode_switcher/selection");

    public CustomGameModeButton(int x, int y, CustomGameMode mode) {
        super(x, y, 26, 26, mode.getDisplayName());
        this.mode = mode;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Draw Slot
        context.drawGuiTexture(SLOT_TEXTURE, this.getX(), this.getY(), 26, 26);

        // 2. Draw Icon (16x16 centered in 26x26)
        context.drawTexture(mode.getIconTexture(), this.getX() + 5, this.getY() + 5, 0, 0, 16, 16, 16, 16);

        // 3. Draw Selection
        if (this.selected) {
            context.drawGuiTexture(SELECTION_TEXTURE, this.getX(), this.getY(), 26, 26);
        }
    }

    @Override
    public void anscor$setSelected(boolean selected) {
        this.selected = selected;
        this.setFocused(selected);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            if (client.interactionManager != null) {
                client.interactionManager.setGameMode(mode.getVanillaFallback());
            }
            client.player.networkHandler.sendChatCommand("gamemode " + mode.getId().toString());
        }
        if (client != null) client.setScreen(null);
    }

    public CustomGameMode getMode() { return mode; }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
