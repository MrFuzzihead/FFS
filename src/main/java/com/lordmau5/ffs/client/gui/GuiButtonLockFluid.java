package com.lordmau5.ffs.client.gui;

import com.lordmau5.ffs.FancyFluidStorage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public class GuiButtonLockFluid extends Button {
    private static final ResourceLocation TEXTURE_FLUID_LOCKED = ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "textures/gui/valve/fluid_locked.png");
    private static final ResourceLocation TEXTURE_FLUID_UNLOCKED = ResourceLocation.fromNamespaceAndPath(FancyFluidStorage.MOD_ID, "textures/gui/valve/fluid_unlocked.png");

    private boolean state;

    public GuiButtonLockFluid(int x, int y, boolean state, Button.OnPress onPress) {
        super(x, y, 7, 8, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.state = state;
    }

    public boolean getState() {
        return this.state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public void toggleState() {
        this.state = !this.state;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.disableDepthTest();

        ResourceLocation resourcelocation = this.getState() ? TEXTURE_FLUID_LOCKED : TEXTURE_FLUID_UNLOCKED;
        guiGraphics.blit(resourcelocation, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);

        RenderSystem.enableDepthTest();
    }
}
