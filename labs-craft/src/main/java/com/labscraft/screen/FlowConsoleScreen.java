package com.labscraft.screen;

import com.labscraft.LabsCraft;
import com.labscraft.network.FlowConsolePackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FlowConsoleScreen extends HandledScreen<FlowConsoleScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of(LabsCraft.MOD_ID, "textures/gui/flow_console.png");

    private ButtonWidget generateButton;

    public FlowConsoleScreen(FlowConsoleScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleY = this.backgroundHeight - 94;

        // Hide player inventory title since we don't show inventory
        this.playerInventoryTitleX = -1000;

        int centerX = this.x + this.backgroundWidth / 2;
        int centerY = this.y + this.backgroundHeight / 2;

        generateButton = ButtonWidget.builder(Text.literal("Generate Video"), button -> {
            FlowConsolePackets.sendGeneratePacket();
        }).dimensions(centerX - 50, centerY + 20, 100, 20).build();

        this.addDrawableChild(generateButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        // Disable button while generating
        generateButton.active = !handler.isGenerating();
        if (handler.isGenerating()) {
            generateButton.setMessage(Text.literal("Generating..."));
        } else {
            generateButton.setMessage(Text.literal("Generate Video"));
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw background
        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFF1a1a2e);
        context.drawBorder(x, y, this.backgroundWidth, this.backgroundHeight, 0xFF4285F4);

        // Draw title area
        context.fill(x + 1, y + 1, x + this.backgroundWidth - 1, y + 20, 0xFF16213e);

        // Draw progress bar background
        int barX = x + 28;
        int barY = y + 50;
        int barWidth = 120;
        int barHeight = 16;

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF0f0f23);
        context.drawBorder(barX, barY, barWidth, barHeight, 0xFF4285F4);

        // Draw progress bar fill
        if (handler.isGenerating()) {
            float progress = handler.getGenerationProgressPercent();
            int fillWidth = (int) ((barWidth - 2) * progress);
            if (fillWidth > 0) {
                context.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + barHeight - 1, 0xFF4285F4);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Draw title centered
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFFFFFF, false);

        // Draw status text
        String statusText;
        if (handler.isGenerating()) {
            int percent = (int) (handler.getGenerationProgressPercent() * 100);
            statusText = "Generating: " + percent + "%";
        } else {
            statusText = "Ready to generate";
        }
        int statusX = (this.backgroundWidth - this.textRenderer.getWidth(statusText)) / 2;
        context.drawText(this.textRenderer, statusText, statusX, 35, 0xCCCCCC, false);

        // Draw generation count
        String countText = "Total generations: " + handler.getTotalGenerations();
        int countX = (this.backgroundWidth - this.textRenderer.getWidth(countText)) / 2;
        context.drawText(this.textRenderer, countText, countX, 75, 0x888888, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
