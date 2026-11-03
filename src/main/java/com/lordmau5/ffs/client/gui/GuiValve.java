package com.lordmau5.ffs.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.lordmau5.ffs.client.FluidHelper;
import com.lordmau5.ffs.network.FFSPacket;
import com.lordmau5.ffs.network.NetworkHandler;
import com.lordmau5.ffs.tile.TileEntityValve;
import com.lordmau5.ffs.util.GenericUtil;
import com.lordmau5.ffs.util.ModNames;

public class GuiValve extends GuiScreen {

    protected static final ResourceLocation tex = new ResourceLocation(ModNames.resource("textures/gui/gui_tank.png"));
    protected static final int AUTO_FLUID_OUTPUT_BTN_ID = 23442;
    protected static final int SAVE_BTN_ID = 23443;
    TileEntityValve valve;
    boolean isFrame;
    GuiTextField valveName;
    String originalValveName = "";
    int xSize = 256;
    int ySize = 121;
    int left = 0;
    int top = 0;
    int mouseX;
    int mouseY;

    public GuiValve(TileEntityValve valve, boolean isFrame) {
        this.valve = valve;
        this.isFrame = isFrame;
    }

    public void initGui() {
        super.initGui();
        if (this.valve == null) {
            return;
        }
        this.left = (this.width - this.xSize) / 2;
        this.top = (this.height - this.ySize) / 2;
        if (!this.isFrame) {
            this.buttonList.add(
                new GuiToggle(
                    AUTO_FLUID_OUTPUT_BTN_ID,
                    this.left + 80,
                    this.top + 20,
                    translate("gui.ffs.auto_output"),
                    this.valve.getAutoOutput(),
                    16777215));
            this.originalValveName = this.valve.getValveName();
            this.valveName = new GuiTextField(this.fontRendererObj, this.left + 80, this.top + 100, 120, 10);
            this.valveName.setText(this.originalValveName);
            this.valveName.setMaxStringLength(18);
            this.buttonList
                .add(new GuiButton(SAVE_BTN_ID, this.left + 204, this.top + 97, 44, 16, translate("gui.ffs.save")));
        }
    }

    public void onGuiClosed() {
        super.onGuiClosed();
        this.saveValveName();
    }

    private void saveValveName() {
        if (this.valve == null || this.isFrame || this.valveName == null) {
            return;
        }

        String name = this.valveName.getText()
            .trim();
        if (name.isEmpty() || name.equals(this.originalValveName)) {
            return;
        }

        this.valveName.setText(name);
        this.valve.setValveName(name);
        this.originalValveName = name;
        NetworkHandler.sendPacketToServer(new FFSPacket.Server.UpdateValveName(this.valve, name));
    }

    protected void keyTyped(char keyChar, int keyCode) {
        if (this.valve == null) {
            super.keyTyped(keyChar, keyCode);
            return;
        }

        if (keyCode == 1) {
            this.mc.thePlayer.closeScreen();
            this.mc.setIngameFocus();
            return;
        }

        if (!this.isFrame && this.valveName.isFocused()) {
            if (keyCode == 28) {
                this.saveValveName();
                return;
            }
            this.valveName.textboxKeyTyped(keyChar, keyCode);
            return;
        }

        if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.mc.thePlayer.closeScreen();
            this.mc.setIngameFocus();
            return;
        }

        super.keyTyped(keyChar, keyCode);
    }

    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (this.valve == null) {
            return;
        }

        if (!this.isFrame) {
            this.valveName.mouseClicked(mouseX, mouseY, button);
        }
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.valve == null) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(tex);
        this.drawTexturedModalRect(this.left, this.top, 0, 0, this.xSize, this.ySize);

        FluidStack fluid = this.valve.getFluid();
        int capacity = this.valve.getCapacity();
        int amount = fluid == null ? 0 : fluid.amount;
        String fluidName = fluid == null ? translate("gui.ffs.empty") : fluid.getLocalizedName();
        String title = this.isFrame ? translate("gui.ffs.frame") : translate("gui.ffs.tank", fluidName);
        this.drawCenteredString(this.fontRendererObj, title, this.left + 163, this.top + 6, 16777215);

        if (fluid != null && capacity > 0) {
            this.drawFluid(this.left, this.top, amount, capacity, fluid);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!this.isFrame) {
            this.drawValveName();
        }

        if (fluid != null) {
            this.fluidHoveringText(fluidName, fluid, amount, capacity);
        }
    }

    private void drawValveName() {
        this.drawString(this.fontRendererObj, translate("gui.ffs.valve_name"), this.left + 80, this.top + 88, 16777215);
        this.valveName.drawTextBox();
    }

    private void fluidHoveringText(String fluidName, FluidStack fluid, int amount, int capacity) {
        if (this.mouseX >= this.left + 10 && this.mouseX < this.left + 10 + 64
            && this.mouseY >= this.top + 10
            && this.mouseY < this.top + 10 + 101) {
            List<String> texts = new ArrayList<>();
            texts.add(fluidName);
            texts.add(
                EnumChatFormatting.GRAY.toString() + GenericUtil.intToFancyNumber(amount)
                    + " / "
                    + GenericUtil.intToFancyNumber(capacity)
                    + " mB");
            if (fluid.getFluid() != null) {
                texts.add(
                    EnumChatFormatting.GRAY.toString() + translate(
                        "gui.ffs.temperature",
                        fluid.getFluid()
                            .getTemperature(fluid)));
            }

            GL11.glPushMatrix();
            GL11.glPushAttrib(64);
            this.drawHoveringText(texts, this.mouseX, this.mouseY, this.fontRendererObj);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    public void actionPerformed(GuiButton button) {
        if (this.valve == null) {
            return;
        }

        if (button.id == SAVE_BTN_ID) {
            this.saveValveName();
            return;
        }

        if (button.id != AUTO_FLUID_OUTPUT_BTN_ID || !(button instanceof GuiToggle)) {
            return;
        }

        GuiToggle toggle = (GuiToggle) button;
        this.valve.setAutoOutput(toggle.getState());
        NetworkHandler
            .sendPacketToServer(new FFSPacket.Server.UpdateAutoOutput(this.valve, this.valve.getAutoOutput()));
    }

    private void drawFluid(int x, int y, int amount, int capacity, FluidStack fluid) {
        if (capacity <= 0 || fluid == null) {
            return;
        }

        IIcon fluidIcon = FluidHelper.getFluidTexture(fluid.getFluid(), false);
        if (fluidIcon == null) {
            return;
        }

        this.mc.getTextureManager()
            .bindTexture(FluidHelper.BLOCK_TEXTURE);
        int height = (int) Math.ceil((float) amount / capacity * 101.0F);
        height = Math.max(0, Math.min(101, height));
        if (height == 0) {
            return;
        }

        ScaledResolution resolution = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
        int scale = resolution.getScaleFactor();
        int fluidTop = y + 10 + 101 - height;
        int[] scissor = GuiScissor.scissorFor(x + 10, fluidTop, 64, height, scale, this.mc.displayHeight);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissor[0], scissor[1], scissor[2], scissor[3]);

        for (int iX = 0; iX < 4; iX++) {
            for (int iY = 7; iY > 0; iY--) {
                this.drawTexturedModelRectFromIcon(x + 10 + iX * 16, y - 1 + (iY - 1) * 16, fluidIcon, 16, 16);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static String translate(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }
}
