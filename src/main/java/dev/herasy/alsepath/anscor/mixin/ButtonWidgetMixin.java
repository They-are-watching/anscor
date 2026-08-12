// src/main/java/dev/herasy/alsepath/anscor/mixin/ButtonWidgetMixin.java
package dev.herasy.alsepath.anscor.mixin;

import dev.herasy.alsepath.anscor.client.gui.SelectableButton; // FIX: Import from GUI package
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.screen.GameModeSelectionScreen$ButtonWidget")
public abstract class ButtonWidgetMixin implements SelectableButton {

    @Shadow
    public abstract void setSelected(boolean selected);

    @Override
    public void anscor$setSelected(boolean selected) {
        this.setSelected(selected);
    }
}
