// src/main/java/dev/herasy/alsepath/anscor/client/gui/CustomGameModeButton.java
package dev.herasy.alsepath.anscor.client.gui;

import dev.herasy.alsepath.anscor.gamemode.CustomGameMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.util.Identifier;

public class CustomGameModeButton extends ClickableWidget {
    private final CustomGameMode mode;
    private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/container/gamemode_switcher.png");

    public CustomGameModeButton(int x, int y, CustomGameMode mode) {
        super(x, y, 26, 26, mode.getDisplayName());
        this.mode = mode;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().push();
        if (this.isFocused()) {
            // Draw the "Selected" white outline box
            context.drawTexture(BACKGROUND_TEXTURE, this.getX(), this.getY(), 0, 166, 26, 26);
        } else {
            // Draw the standard grey box
            context.drawTexture(BACKGROUND_TEXTURE, this.getX(), this.getY(), 0, 192, 26, 26);
        }

        context.drawTexture(mode.getIconTexture(), this.getX() + 5, this.getY() + 5, 0, 0, 16, 16, 16, 16);
        context.getMatrices().pop();
    }

    // Triggered when user releases click on this icon slot
    @Override
    public void onClick(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Log for verification
            client.player.sendMessage(net.minecraft.text.Text.of("Selected Custom Mode: " + mode.getId().toString()), false);

            // Force the fallback base vanilla loop to prevent game breaks right now
            if (client.interactionManager != null) {
                client.interactionManager.setGameMode(mode.getVanillaFallback());
            }
        }
        // Close screen layout menu
        client.setScreen(null);
    }

    public CustomGameMode getMode() { return mode; }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
