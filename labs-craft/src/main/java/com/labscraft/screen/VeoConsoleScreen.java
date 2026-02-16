package com.labscraft.screen;

import com.labscraft.LabsCraft;
import com.labscraft.network.VeoConsolePackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VeoConsoleScreen extends HandledScreen<VeoConsoleScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of(LabsCraft.MOD_ID, "textures/gui/veo_console.png");

    private static final int THEME_COLOR = 0xFF9966FF;
    private static final int THEME_DARK = 0xFF1a1a2e;
    private static final int THEME_HEADER = 0xFF2a2a4e;

    private ButtonWidget generateButton;
    private boolean wasGenerating = false;
    private int completionFlashTicks = 0;
    private long animationTick = 0;

    public VeoConsoleScreen(VeoConsoleScreenHandler handler, PlayerInventory inventory, Text title) {
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
            VeoConsolePackets.sendGeneratePacket();
        }).dimensions(centerX - 50, centerY + 20, 100, 20).build();

        this.addDrawableChild(generateButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        animationTick++;

        boolean isGen = handler.isGenerating();
        if (wasGenerating && !isGen) {
            completionFlashTicks = 6;
        }
        wasGenerating = isGen;

        if (completionFlashTicks > 0) {
            completionFlashTicks--;
        }

        generateButton.active = !isGen;
        if (isGen) {
            generateButton.setMessage(Text.literal("Generating..."));
        } else {
            generateButton.setMessage(Text.literal("Generate Video"));
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw background - purple/violet theme for Veo
        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, THEME_DARK);
        context.drawBorder(x, y, this.backgroundWidth, this.backgroundHeight, THEME_COLOR);

        // Draw title area
        context.fill(x + 1, y + 1, x + this.backgroundWidth - 1, y + 20, THEME_HEADER);

        // Draw decorative line under title
        context.fill(x + 10, y + 19, x + this.backgroundWidth - 10, y + 20, THEME_COLOR);

        // Draw progress bar background
        int barX = x + 28;
        int barY = y + 50;
        int barWidth = 120;
        int barHeight = 16;

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF0f0f23);
        context.drawBorder(barX, barY, barWidth, barHeight, THEME_COLOR);

        // Draw progress bar fill with shimmer
        if (handler.isGenerating()) {
            float progress = handler.getGenerationProgressPercent();
            int fillWidth = (int) ((barWidth - 2) * progress);
            if (fillWidth > 0) {
                context.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + barHeight - 1, THEME_COLOR);

                // Shimmer highlight
                float shimmer = (float) Math.sin((animationTick + delta) * 0.12) * 0.5f + 0.5f;
                int shimmerX = barX + 1 + (int) (fillWidth * shimmer);
                int shimmerWidth = Math.min(8, fillWidth);
                if (shimmerX + shimmerWidth <= barX + 1 + fillWidth) {
                    context.fill(shimmerX, barY + 1, shimmerX + shimmerWidth, barY + barHeight - 1, 0x40FFFFFF);
                }
            }
        }

        // Completion flash overlay
        if (completionFlashTicks > 0) {
            int alpha = (int) (completionFlashTicks * 25);
            context.fill(x + 1, y + 1, x + this.backgroundWidth - 1, y + this.backgroundHeight - 1,
                (alpha << 24) | 0xEECCFF);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Draw title centered
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xCC99FF, false);

        // Draw status text with pulse effect when generating
        String statusText;
        int statusColor;
        if (handler.isGenerating()) {
            int percent = (int) (handler.getGenerationProgressPercent() * 100);
            statusText = "Generating: " + percent + "%";
            float pulse = (float) Math.sin(animationTick * 0.15) * 0.3f + 0.7f;
            int brightness = (int) (pulse * 255);
            statusColor = (brightness << 16) | (brightness << 8) | brightness;
        } else {
            statusText = "Ready to generate";
            statusColor = 0xCCCCCC;
        }
        int statusX = (this.backgroundWidth - this.textRenderer.getWidth(statusText)) / 2;
        context.drawText(this.textRenderer, statusText, statusX, 35, statusColor, false);

        // Draw generation count with themed color
        int count = handler.getTotalGenerations();
        String countText = "Total videos: " + count;
        int countX = (this.backgroundWidth - this.textRenderer.getWidth(countText)) / 2;
        int countColor = count > 0 ? 0xCC99FF : 0x888888;
        context.drawText(this.textRenderer, countText, countX, 75, countColor, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
