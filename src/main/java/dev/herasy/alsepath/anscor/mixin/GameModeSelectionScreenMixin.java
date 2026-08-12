package dev.herasy.alsepath.anscor.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.herasy.alsepath.anscor.client.gui.CustomGameModeButton;
import dev.herasy.alsepath.anscor.client.gui.GameModeSwitcherReloadable;
import dev.herasy.alsepath.anscor.client.gui.SwitcherLayout;
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
public abstract class GameModeSelectionScreenMixin
        extends Screen
        implements GameModeSwitcherReloadable {

    @Shadow
    @Final
    private List<ClickableWidget> gameModeButtons;

    @Shadow
    @Final
    private static Text SELECT_NEXT_TEXT;

    @Shadow
    private int lastMouseX;

    @Shadow
    private int lastMouseY;

    @Shadow
    private boolean mouseUsedForSelection;

    @Unique
    private static final Logger ANSCOR_LOGGER =
            LoggerFactory.getLogger("anscor-mixin");

    @Unique
    private static final Identifier TEX_LEFT =
            Identifier.of(
                    "anscor",
                    "textures/gui/gamemode_switcher/left_cap.png"
            );

    @Unique
    private static final Identifier TEX_MID =
            Identifier.of(
                    "anscor",
                    "textures/gui/gamemode_switcher/center_fill.png"
            );

    @Unique
    private static final Identifier TEX_RIGHT =
            Identifier.of(
                    "anscor",
                    "textures/gui/gamemode_switcher/right_cap.png"
            );

    // ------------------------------------------------------------
    // Layout constants
    // ------------------------------------------------------------

    @Unique
    private static final int ANSCOR_BUTTON_SIZE = 26;

    @Unique
    private static final int ANSCOR_SPACING = 5;

    @Unique
    private static final int ANSCOR_ROW_Y_OFFSET = -31;

    @Unique
    private static final int ANSCOR_BOX_Y_OFFSET = -27;

    @Unique
    private static final int ANSCOR_TITLE_Y_OFFSET = -20;

    @Unique
    private static final int ANSCOR_HINT_Y_OFFSET = 36;

    // ------------------------------------------------------------
    // Runtime GUI state
    // ------------------------------------------------------------

    @Unique
    private final List<ClickableWidget> anscor$vanillaButtons =
            new ArrayList<>();

    /**
     * The unified ordered selection list used by the F4 cycle logic.
     * This is rebuilt every init(), but it contains references to
     * existing widgets rather than creating new widgets.
     */
    @Unique
    private final List<ClickableWidget> anscor$unifiedButtonList =
            new ArrayList<>();

    /**
     * Custom buttons belong to this screen instance.
     * These objects are created only during BUILD and reused on resize.
     */
    @Unique
    private final List<CustomGameModeButton> anscor$customButtons =
            new ArrayList<>();

    @Unique
    private int anscor$currentCycleIndex = 0;

    /**
     * Whether the intrinsic GUI layout has already been built.
     * IMPORTANT:
     * This does NOT mean "don't do anything in init()".
     * It only means "don't discover/create the layout again".
     */
    @Unique
    private boolean anscor$layoutBuilt = false;

    /**
     * Cached intrinsic layout geometry.
     */
    @Unique
    private SwitcherLayout anscor$layout;

    // ------------------------------------------------------------
    // Reflection
    // ------------------------------------------------------------

    @Unique
    private static Method anscor$methodSetSelected;

    @Unique
    private static Field anscor$fieldButtonGameMode;

    @Unique
    private static Field anscor$fieldScreenGameMode;

    @Unique
    private static boolean anscor$reflectionInitialized = false;

    // ------------------------------------------------------------

    protected GameModeSelectionScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private void anscor$setButtonSelected(
            ClickableWidget btn,
            boolean selected
    ) {
        if (btn instanceof SelectableButton selectableButton) {
            selectableButton.anscor$setSelected(selected);
            return;
        }

        if (anscor$methodSetSelected != null) {
            try {
                anscor$methodSetSelected.invoke(btn, selected);
            } catch (Exception ignored) {
            }
        }
    }

    @Unique
    private void anscor$applySelectionVisuals() {

        if (anscor$unifiedButtonList.isEmpty()) {
            return;
        }

        int safeIndex = Math.max(
                0,
                Math.min(
                        anscor$currentCycleIndex,
                        anscor$unifiedButtonList.size() - 1
                )
        );

        anscor$currentCycleIndex = safeIndex;

        for (int i = 0; i < anscor$unifiedButtonList.size(); i++) {

            ClickableWidget btn =
                    anscor$unifiedButtonList.get(i);

            boolean selected =
                    i == anscor$currentCycleIndex;

            anscor$setButtonSelected(btn, selected);
            btn.setFocused(selected);
        }
    }

    // ============================================================
    // INIT / BUILD / POSITION
    // ============================================================

    @Inject(method = "init", at = @At("TAIL"))
    private void anscor$initLogic(CallbackInfo ci) {

        anscor$initializeReflection();

        /*
         * BUILD:
         *
         * Only create custom widgets and calculate intrinsic geometry
         * the first time this screen instance is initialized, or after
         * SwitcherReload explicitly invalidates the cache.
         */
        if (!anscor$layoutBuilt) {
            anscor$buildLayout();
        }

        /*
         * RE-ATTACH:
         *
         * Screen re-initialization clears its child collections.
         * Re-add the SAME custom widget objects instead of creating new ones.
         */
        for (CustomGameModeButton customButton : anscor$customButtons) {
            this.addDrawableChild(customButton);
        }

        /*
         * The vanilla widgets are recreated/reinitialized by Minecraft,
         * so rebuild the unified reference list around the existing widgets.
         */
        anscor$rebuildUnifiedButtonList();

        /*
         * POSITION:
         *
         * This happens every init(), including resize, but it uses
         * cached intrinsic geometry rather than rebuilding the GUI.
         */
        anscor$positionLayout();

        /*
         * Only determine the initial selection during BUILD.
         * A resize should not unexpectedly change the selected entry.
         */
        if (anscor$layout != null && anscor$layout.justBuilt) {
            anscor$syncInitialSelection();
            anscor$layout.justBuilt = false;
        }

        anscor$applySelectionVisuals();
    }

    @Unique
    private void anscor$buildLayout() {
        List<CustomGameMode> customModes =
                new ArrayList<>(CustomGameModeRegistry.getF3VisibleModes());

        anscor$vanillaButtons.clear();
        anscor$vanillaButtons.addAll(this.gameModeButtons);

        int vanillaCount = anscor$vanillaButtons.size();
        int customCount = customModes.size();
        int totalItems = vanillaCount + customCount;

        int contentWidth = anscor$calculateContentWidth(totalItems);
        int totalBoxWidth = contentWidth + 6;

        anscor$layout = new SwitcherLayout(
                vanillaCount,
                customCount,
                totalItems,
                contentWidth,
                totalBoxWidth
        );

        /*
         * Create custom widgets ONCE for this screen instance.
         *
         * Their actual screen coordinates are assigned by
         * anscor$positionLayout().
         */
        for (CustomGameMode mode : customModes) {
            anscor$customButtons.add(
                    new CustomGameModeButton(
                            0,
                            0,
                            mode
                    )
            );
        }

        anscor$layoutBuilt = true;
        anscor$layout.justBuilt = true;

        ANSCOR_LOGGER.debug(
                "Anscor: Built switcher layout: vanilla={}, custom={}, total={}, width={}",
                vanillaCount,
                customCount,
                totalItems,
                contentWidth
        );
    }

    @Unique
    private void anscor$rebuildUnifiedButtonList() {

        anscor$unifiedButtonList.clear();

        /*
         * Vanilla buttons first, preserving the game's normal order.
         */
        anscor$unifiedButtonList.addAll(anscor$vanillaButtons);

        /*
         * Then our cached custom buttons.
         */
        anscor$unifiedButtonList.addAll(anscor$customButtons);
    }

    @Unique
    private void anscor$positionLayout() {

        if (anscor$layout == null) {
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int startX =
                centerX - (anscor$layout.contentWidth / 2);

        int rowY =
                centerY + ANSCOR_ROW_Y_OFFSET;

        /*
         * Position vanilla buttons.
         */
        for (int i = 0; i < anscor$vanillaButtons.size(); i++) {
            ClickableWidget widget =
                    anscor$vanillaButtons.get(i);

            widget.setX(
                    startX
                            + i * (ANSCOR_BUTTON_SIZE + ANSCOR_SPACING)
            );

            widget.setY(rowY);
        }

        /*
         * Position cached custom buttons directly after vanilla buttons.
         */
        for (int i = 0; i < anscor$customButtons.size(); i++) {
            CustomGameModeButton button =
                    anscor$customButtons.get(i);

            int absoluteIndex =
                    anscor$vanillaButtons.size() + i;

            button.setX(
                    startX
                            + absoluteIndex
                            * (ANSCOR_BUTTON_SIZE + ANSCOR_SPACING)
            );

            button.setY(rowY);
        }
    }

    @Unique
    private static int anscor$calculateContentWidth(int totalItems) {

        if (totalItems <= 0) {
            return 0;
        }

        return totalItems * ANSCOR_BUTTON_SIZE
                + (totalItems - 1) * ANSCOR_SPACING;
    }

    // ============================================================
    // EXPLICIT RELOAD API
    // ============================================================

    /**
     * Called by SwitcherReload.reload().
     * This invalidates the intrinsic layout and tells Minecraft to
     * perform a normal screen reinitialization.
     */
    @Override
    public void anscor$reloadSwitcherLayout() {

        ANSCOR_LOGGER.info(
                "Anscor: Reloading Game Mode Switcher layout."
        );

        anscor$layoutBuilt = false;
        anscor$layout = null;
        anscor$customButtons.clear();
        anscor$unifiedButtonList.clear();
        anscor$currentCycleIndex = 0;

        /*
         * This is intentional:
         *
         * clearAndInit() lets Minecraft clear the current screen
         * children and run GameModeSelectionScreen.init() again.
         *
         * Our @Inject at TAIL then performs a fresh BUILD.
         */
        this.clearAndInit();
    }

    // ============================================================
    // RENDER
    // ============================================================

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anscor$renderDynamic(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {

        if (!this.anscor$checkForClose()) {

            if (anscor$layout != null) {

                int centerX = this.width / 2;

                int centerY = this.height / 2;

                int rowY =
                        centerY + ANSCOR_ROW_Y_OFFSET;

                int totalBoxWidth =
                        anscor$layout.totalBoxWidth;

                int boxLeft =
                        centerX - (totalBoxWidth / 2);

                int boxTop =
                        rowY + ANSCOR_BOX_Y_OFFSET;

                // ------------------------------------------------
                // Background
                // ------------------------------------------------

                context.getMatrices().push();

                RenderSystem.enableBlend();

                context.drawTexture(
                        TEX_LEFT,
                        boxLeft,
                        boxTop,
                        0,
                        0,
                        5,
                        75,
                        5,
                        75
                );

                context.drawTexture(
                        TEX_MID,
                        boxLeft + 5,
                        boxTop,
                        totalBoxWidth - 10,
                        75,
                        0,
                        0,
                        1,
                        75,
                        1,
                        75
                );

                context.drawTexture(
                        TEX_RIGHT,
                        boxLeft + totalBoxWidth - 5,
                        boxTop,
                        0,
                        0,
                        5,
                        75,
                        5,
                        75
                );

                context.getMatrices().pop();

                // ------------------------------------------------
                // Mouse tracking
                // ------------------------------------------------

                if (!this.mouseUsedForSelection) {
                    this.lastMouseX = mouseX;
                    this.lastMouseY = mouseY;
                    this.mouseUsedForSelection = true;
                }

                boolean mouseMoved =
                        this.lastMouseX != mouseX
                                || this.lastMouseY != mouseY;

                // ------------------------------------------------
                // Selection + button rendering
                // ------------------------------------------------

                for (
                        int i = 0;
                        i < anscor$unifiedButtonList.size();
                        i++
                ) {

                    ClickableWidget btn =
                            anscor$unifiedButtonList.get(i);

                    if (
                            mouseMoved
                                    && btn.isMouseOver(mouseX, mouseY)
                    ) {

                        anscor$currentCycleIndex = i;

                        this.lastMouseX = mouseX;
                        this.lastMouseY = mouseY;
                    }

                    boolean isSelected =
                            i == anscor$currentCycleIndex;

                    anscor$setButtonSelected(btn, isSelected);
                    btn.setFocused(isSelected);

                    btn.render(context, mouseX, mouseY, delta);
                }

                // ------------------------------------------------
                // Text
                // ------------------------------------------------

                if (!anscor$unifiedButtonList.isEmpty()) {

                    int safeIndex = Math.max(
                            0,
                            Math.min(
                                    anscor$currentCycleIndex,
                                    anscor$unifiedButtonList.size() - 1
                            )
                    );

                    anscor$currentCycleIndex = safeIndex;

                    ClickableWidget selectedBtn =
                            anscor$unifiedButtonList.get(
                                    safeIndex
                            );

                    context.drawCenteredTextWithShadow(
                            this.textRenderer,
                            selectedBtn.getMessage(),
                            centerX,
                            rowY + ANSCOR_TITLE_Y_OFFSET,
                            0xFFFFFF
                    );

                    context.drawCenteredTextWithShadow(
                            this.textRenderer,
                            SELECT_NEXT_TEXT,
                            centerX,
                            rowY + ANSCOR_HINT_Y_OFFSET,
                            0xFFFFFF
                    );
                }
            }
        }

        /*
         * We intentionally replace vanilla GameModeSelectionScreen.render().
         */
        ci.cancel();
    }

    // ============================================================
    // F4
    // ============================================================

    @Inject(
            method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    public void anscor$handleF4(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {

        if (
                keyCode == GLFW.GLFW_KEY_F4
                        && !anscor$unifiedButtonList.isEmpty()
        ) {

            this.mouseUsedForSelection = false;

            anscor$currentCycleIndex =
                    (
                            anscor$currentCycleIndex + 1
                    ) % anscor$unifiedButtonList.size();

            cir.setReturnValue(true);
        }
    }

    // ============================================================
    // INITIAL SELECTION
    // ============================================================

    @Unique
    private void anscor$syncInitialSelection() {

        if (anscor$unifiedButtonList.isEmpty()) {
            anscor$currentCycleIndex = 0;
            return;
        }

        if (
                anscor$fieldScreenGameMode != null
                        && anscor$fieldButtonGameMode != null
        ) {

            try {

                Object currentMode =
                        anscor$fieldScreenGameMode.get(this);

                if (currentMode != null) {

                    for (
                            int i = 0;
                            i < anscor$unifiedButtonList.size();
                            i++
                    ) {

                        ClickableWidget btn =
                                anscor$unifiedButtonList.get(i);

                        if (
                                btn instanceof CustomGameModeButton
                        ) {
                            continue;
                        }

                        Object buttonMode =
                                anscor$fieldButtonGameMode.get(btn);

                        if (buttonMode == currentMode) {

                            anscor$currentCycleIndex = i;
                            return;
                        }
                    }
                }

            } catch (Exception e) {

                ANSCOR_LOGGER.debug(
                        "Anscor: Failed to determine initial gamemode selection.",
                        e
                );
            }
        }

        anscor$currentCycleIndex = 0;
    }

    // ============================================================
    // REFLECTION
    // ============================================================

    @Unique
    private void anscor$initializeReflection() {

        if (anscor$reflectionInitialized) {
            return;
        }

        try {

            anscor$fieldScreenGameMode =
                    GameModeSelectionScreen.class
                            .getDeclaredField("gameMode");

            anscor$fieldScreenGameMode.setAccessible(true);

            if (!this.gameModeButtons.isEmpty()) {

                Class<?> btnClass =
                        this.gameModeButtons
                                .getFirst()
                                .getClass();

                ANSCOR_LOGGER.info(
                        "Anscor: Detected Vanilla Button Class: {}",
                        btnClass.getName()
                );

                try {

                    anscor$methodSetSelected =
                            btnClass.getMethod(
                                    "setSelected",
                                    boolean.class
                            );

                } catch (NoSuchMethodException e) {

                    anscor$methodSetSelected =
                            btnClass.getDeclaredMethod(
                                    "setSelected",
                                    boolean.class
                            );

                    anscor$methodSetSelected.setAccessible(true);
                }

                anscor$fieldButtonGameMode =
                        btnClass.getDeclaredField("gameMode");

                anscor$fieldButtonGameMode.setAccessible(true);

                anscor$reflectionInitialized = true;
            }

        } catch (Exception e) {

            ANSCOR_LOGGER.error(
                    "Anscor: Failed to initialize reflection handles for GameModeSwitcher!",
                    e
            );
        }
    }

    // ============================================================
    // CLOSE / APPLY
    // ============================================================

    @Unique
    private boolean anscor$checkForClose() {

        if (
                this.client == null
                        || this.client.getWindow() == null
        ) {
            return false;
        }

        if (
                !InputUtil.isKeyPressed(
                        this.client.getWindow().getHandle(),
                        GLFW.GLFW_KEY_F3
                )
        ) {

            this.anscor$apply();

            this.client.setScreen(null);

            return true;
        }

        return false;
    }

    @Unique
    private void anscor$apply() {

        if (
                this.client == null
                        || this.client.player == null
                        || anscor$unifiedButtonList.isEmpty()
        ) {
            return;
        }

        int safeIndex = Math.max(
                0,
                Math.min(
                        anscor$currentCycleIndex,
                        anscor$unifiedButtonList.size() - 1
                )
        );

        ClickableWidget selected =
                anscor$unifiedButtonList.get(safeIndex);

        if (selected instanceof CustomGameModeButton customButton) {

            String flatId =
                    customButton
                            .getMode()
                            .getId()
                            .getPath();

            this.client.player.networkHandler.sendChatCommand(
                    "gamemode " + flatId
            );

        } else {

            try {

                if (anscor$fieldButtonGameMode != null) {

                    Object vanillaModeObj =
                            anscor$fieldButtonGameMode.get(
                                    selected
                            );

                    if (vanillaModeObj != null) {

                        String vanillaName =
                                vanillaModeObj
                                        .toString()
                                        .toLowerCase();

                        this.client.player.networkHandler.sendChatCommand(
                                "gamemode " + vanillaName
                        );
                    }
                }

            } catch (Exception e) {

                this.client.player.networkHandler.sendChatCommand(
                        "gamemode survival"
                );
            }
        }
    }
}