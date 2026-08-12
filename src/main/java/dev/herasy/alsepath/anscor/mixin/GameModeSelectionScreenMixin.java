// src/main/java/dev/herasy/alsepath/anscor/mixin/GameModeSelectionScreenMixin.java
package dev.herasy.alsepath.anscor.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.herasy.alsepath.anscor.client.gui.CustomGameModeButton;
import dev.herasy.alsepath.anscor.client.gui.SelectableButton;
import dev.herasy.alsepath.anscor.gamemode.CustomGameMode;
import dev.herasy.alsepath.anscor.gamemode.CustomGameModeRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Mixin(GameModeSelectionScreen.class)
public abstract class GameModeSelectionScreenMixin extends Screen {

    @Shadow @Final private List<ClickableWidget> gameModeButtons;
    @Shadow @Final private static Text SELECT_NEXT_TEXT;
    @Shadow private int lastMouseX;
    @Shadow private int lastMouseY;
    @Shadow private boolean mouseUsedForSelection;

    @Unique private static final Logger ANSCOR_LOGGER = LoggerFactory.getLogger("anscor-mixin");
    @Unique private static final Identifier TEX_LEFT = Identifier.of("anscor", "textures/gui/gamemode_switcher/left_cap.png");
    @Unique private static final Identifier TEX_MID = Identifier.of("anscor", "textures/gui/gamemode_switcher/center_fill.png");
    @Unique private static final Identifier TEX_RIGHT = Identifier.of("anscor", "textures/gui/gamemode_switcher/right_cap.png");

    @Unique private final List<ClickableWidget> anscor$unifiedButtonList = new ArrayList<>();
    @Unique private int anscor$currentCycleIndex = 0;

    // Reflection Caches
    @Unique private static boolean anscor$reflectionInit = false;
    @Unique private static Method anscor$methodSetSelected;
    @Unique private static Field anscor$fieldButtonGameMode;
    @Unique private static Field anscor$fieldScreenGameMode;

    protected GameModeSelectionScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void anscor$initLogic(CallbackInfo ci) {
        // --- 1. Robust Reflection Setup ---
        if (!anscor$reflectionInit) {
            anscor$reflectionInit = true;
            try {
                // A. Find the Screen's 'gameMode' field
                // Note: If running in non-dev, you might need "field_32316" or similar intermediate names
                anscor$fieldScreenGameMode = GameModeSelectionScreen.class.getDeclaredField("gameMode");
                anscor$fieldScreenGameMode.setAccessible(true);

                // B. Find the Inner Button Class & Fields
                // We assume the first button in the vanilla list is the correct class
                if (!this.gameModeButtons.isEmpty()) {
                    Class<?> btnClass = this.gameModeButtons.get(0).getClass();
                    ANSCOR_LOGGER.info("Anscor: Detected Vanilla Button Class: " + btnClass.getName());

                    // Method: setSelected(boolean) - Forces the visual highlight
                    try {
                        anscor$methodSetSelected = btnClass.getMethod("setSelected", boolean.class);
                    } catch (NoSuchMethodException e) {
                        // Fallback: try getDeclaredMethod if it's not public
                        anscor$methodSetSelected = btnClass.getDeclaredMethod("setSelected", boolean.class);
                        anscor$methodSetSelected.setAccessible(true);
                    }

                    // Field: gameMode - For logic syncing
                    anscor$fieldButtonGameMode = btnClass.getDeclaredField("gameMode");
                    anscor$fieldButtonGameMode.setAccessible(true);
                }
            } catch (Exception e) {
                ANSCOR_LOGGER.error("Anscor: Failed to initialize reflection handles for GameModeSwitcher!", e);
            }
        }

        // --- 2. Build Unified List ---
        List<CustomGameMode> customModes = new ArrayList<>(CustomGameModeRegistry.getF3VisibleModes());
        this.anscor$unifiedButtonList.clear();

        int vanillaCount = this.gameModeButtons.size();
        int totalItems = vanillaCount + customModes.size();
        int buttonSize = 26;
        int spacing = 5;

        int contentWidth = (totalItems * buttonSize) + ((totalItems - 1) * spacing);
        int startX = (this.width / 2) - (contentWidth / 2);
        int y = (this.height / 2) - 31;

        // Vanilla
        for (int i = 0; i < vanillaCount; i++) {
            ClickableWidget widget = this.gameModeButtons.get(i);
            widget.setX(startX + (i * (buttonSize + spacing)));
            anscor$unifiedButtonList.add(widget);
        }

        // Custom
        for (int i = 0; i < customModes.size(); i++) {
            CustomGameModeButton customBtn = new CustomGameModeButton(
                    startX + (vanillaCount * (buttonSize + spacing)) + (i * (buttonSize + spacing)),
                    y,
                    customModes.get(i)
            );
            this.addDrawableChild(customBtn);
            anscor$unifiedButtonList.add(customBtn);
        }

        // this.anscor$syncCycleIndex(); // Uncomment if you have this method
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void anscor$renderDynamic(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!this.anscor$checkForClose()) {

            // Background Drawing
            int totalItems = anscor$unifiedButtonList.size();
            int contentWidth = (totalItems * 26) + ((totalItems - 1) * 5);
            int totalBoxWidth = contentWidth + 6;
            int boxLeft = (this.width / 2) - (totalBoxWidth / 2);
            int boxTop = (this.height / 2) - 31 - 27;

            context.getMatrices().push();
            RenderSystem.enableBlend();
            context.drawTexture(TEX_LEFT, boxLeft, boxTop, 0, 0, 5, 75, 5, 75);
            context.drawTexture(TEX_MID, boxLeft + 5, boxTop, totalBoxWidth - 10, 75, 0, 0, 1, 75, 1, 75);
            context.drawTexture(TEX_RIGHT, boxLeft + totalBoxWidth - 5, boxTop, 0, 0, 5, 75, 5, 75);
            context.getMatrices().pop();

            if (!this.mouseUsedForSelection) {
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                this.mouseUsedForSelection = true;
            }
            boolean mouseMoved = this.lastMouseX != mouseX || this.lastMouseY != mouseY;

            // --- THE LOOP ---
            for (int i = 0; i < anscor$unifiedButtonList.size(); i++) {
                ClickableWidget btn = anscor$unifiedButtonList.get(i);

                if (mouseMoved && btn.isMouseOver(mouseX, mouseY)) {
                    anscor$currentCycleIndex = i;
                    this.lastMouseX = mouseX;
                    this.lastMouseY = mouseY;
                }

                boolean isSelected = (i == anscor$currentCycleIndex);

                // --- PATH A: Custom Button (Safe) ---
                if (btn instanceof SelectableButton sb) {
                    sb.anscor$setSelected(isSelected);
                }
                // --- PATH B: Vanilla Button (Reflection) ---
                else {
                    // 1. Force Visual Highlight
                    if (anscor$methodSetSelected != null) {
                        try {
                            anscor$methodSetSelected.invoke(btn, isSelected);
                        } catch (Exception e) {
                            // Suppress per-frame errors after logging once ideally, but this is safe for now
                        }
                    }

                    // 2. Sync Logic (Only if selected)
                    if (isSelected && anscor$fieldButtonGameMode != null && anscor$fieldScreenGameMode != null) {
                        try {
                            Object mode = anscor$fieldButtonGameMode.get(btn);
                            anscor$fieldScreenGameMode.set(this, mode);
                        } catch (Exception e) {
                            // Suppress
                        }
                    }
                }

                btn.setFocused(isSelected);
                btn.render(context, mouseX, mouseY, delta);
            }

            if (!anscor$unifiedButtonList.isEmpty()) {
                ClickableWidget selectedBtn = anscor$unifiedButtonList.get(anscor$currentCycleIndex);
                context.drawCenteredTextWithShadow(this.textRenderer, selectedBtn.getMessage(), this.width / 2, this.height / 2 - 31 - 20, 0xFFFFFF);
                context.drawCenteredTextWithShadow(this.textRenderer, SELECT_NEXT_TEXT, this.width / 2, this.height / 2 + 5, 0xFFFFFF);
            }
        }
        ci.cancel();
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void anscor$handleF4(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == GLFW.GLFW_KEY_F4 && !anscor$unifiedButtonList.isEmpty()) {
            this.mouseUsedForSelection = false;
            anscor$currentCycleIndex = (anscor$currentCycleIndex + 1) % anscor$unifiedButtonList.size();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean anscor$checkForClose() {
        if (this.client == null || this.client.getWindow() == null) return false;

        if (!InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_F3)) {
            this.anscor$apply();
            this.client.setScreen(null);
            return true;
        }
        return false;
    }

    @Unique
    private void anscor$apply() {
        if (this.client == null || this.client.player == null) return;

        ClickableWidget selected = anscor$unifiedButtonList.get(anscor$currentCycleIndex);

        if (selected instanceof CustomGameModeButton cBtn) {
            this.client.player.networkHandler.sendChatCommand("gamemode " + cBtn.getMode().getId().toString());
        } else {
            // Vanilla Fallback Order: Creative(0), Survival(1), Adventure(2), Spectator(3)
            String command = "gamemode survival";
            int index = anscor$unifiedButtonList.indexOf(selected);
            switch (index) {
                case 0 -> command = "gamemode creative";
                case 1 -> command = "gamemode survival";
                case 2 -> command = "gamemode adventure";
                case 3 -> command = "gamemode spectator";
            }
            if (this.client.player.hasPermissionLevel(2)) {
                this.client.player.networkHandler.sendChatCommand(command);
            }
        }
    }

    @Unique
    private void anscor$syncCycleIndex() {
        for (int i = 0; i < anscor$unifiedButtonList.size(); i++) {
            if (anscor$unifiedButtonList.get(i).isFocused()) {
                anscor$currentCycleIndex = i;
                return;
            }
        }
    }
}
