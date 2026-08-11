// src/main/java/dev/herasy/alsepath/anscor/mixin/GameModeSelectionScreenMixin.java
package dev.herasy.alsepath.anscor.mixin;

import dev.herasy.alsepath.anscor.client.gui.CustomGameModeButton;
import dev.herasy.alsepath.anscor.gamemode.CustomGameMode;
import dev.herasy.alsepath.anscor.gamemode.CustomGameModeRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GameModeSelectionScreen.class)
public abstract class GameModeSelectionScreenMixin extends Screen {

    @Shadow private List<ClickableWidget> gameModeButtons;

    @Unique
    private final List<CustomGameModeButton> anscor$customButtons = new ArrayList<>();
    @Unique
    private CustomGameModeButton anscor$currentlySelected = null;

    protected GameModeSelectionScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addCustomGameModesToLayout(CallbackInfo ci) {
        List<CustomGameMode> customModes = new ArrayList<>(CustomGameModeRegistry.getF3VisibleModes());
        if (customModes.isEmpty()) return;

        anscor$customButtons.clear();

        int vanillaCount = this.gameModeButtons.size();
        int totalCount = vanillaCount + customModes.size();

        int buttonWidth = 26;
        int padding = 5;
        int totalWidth = (totalCount * buttonWidth) + ((totalCount - 1) * padding);

        int startX = (this.width / 2) - (totalWidth / 2);
        int y = (this.height / 2) - 31;

        // 1. Reposition Vanilla Buttons
        for (int i = 0; i < vanillaCount; i++) {
            ClickableWidget widget = this.gameModeButtons.get(i);
            widget.setX(startX + (i * (buttonWidth + padding)));
        }

        // 2. Add custom buttons to OWN safe tracking lists
        for (int i = 0; i < customModes.size(); i++) {
            CustomGameMode mode = customModes.get(i);
            int xPos = startX + (vanillaCount * (buttonWidth + padding)) + (i * (buttonWidth + padding));

            CustomGameModeButton customBtn = new CustomGameModeButton(xPos, y, mode);

            this.addDrawableChild(customBtn);
            this.anscor$customButtons.add(customBtn); // DO NOT add to vanilla's gameModeButtons list
        }
    }

    // Intercept mouse hovers to update tooltips and selections cleanly
    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustomTooltips(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean hoveringAnyCustom = false;

        for (CustomGameModeButton btn : anscor$customButtons) {
            if (btn.isMouseOver(mouseX, mouseY)) {
                // Deselect vanilla buttons dynamically by altering focus state
                for (ClickableWidget vanillaBtn : gameModeButtons) {
                    vanillaBtn.setFocused(false);
                }

                // Clear any other custom button selections
                for (CustomGameModeButton other : anscor$customButtons) {
                    other.setFocused(other == btn);
                }

                anscor$currentlySelected = btn;
                hoveringAnyCustom = true;

                context.drawCenteredTextWithShadow(this.textRenderer, btn.getMode().getDisplayName(), this.width / 2, this.height / 2 + 5, 0xFFFFFF);
                break;
            }
        }

        // If mouse left our custom zone, clean up our tracking
        if (!hoveringAnyCustom && anscor$currentlySelected != null) {
            anscor$currentlySelected.setFocused(false);
            anscor$currentlySelected = null;
        }
    }
}
