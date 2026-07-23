package com.labscraft.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Client screen shared by the three generation consoles. Slots and progress
 * come from {@link ConsoleScreenHandler}; the Generate button uses the vanilla
 * button-click channel (no custom packets).
 */
public class ConsoleScreen extends HandledScreen<ConsoleScreenHandler> {
    private static final int THEME_COLOR = 0xFF4285F4;
    private static final int THEME_DARK = 0xFF1A1A2E;
    private static final int THEME_HEADER = 0xFF16213E;
    private static final int SLOT_BG = 0xFF0F0F23;

    private ButtonWidget generateButton;
    private boolean wasGenerating = false;
    private int completionFlashTicks = 0;
    private long animationTick = 0;

    public ConsoleScreen(ConsoleScreenHandler handler, PlayerInventory inventory, Text title) {
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

        generateButton = ButtonWidget.builder(generateLabel(), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.interactionManager != null) {
                client.interactionManager.clickButton(handler.syncId, ConsoleScreenHandler.GENERATE_BUTTON_ID);
            }
        }).dimensions(this.x + 53, this.y + 55, 70, 18).build();

        this.addDrawableChild(generateButton);
    }

    private Text generateLabel() {
        int cost = Math.max(1, handler.getTpuCost());
        return Text.translatable("screen.labscraft.generate", cost);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        animationTick++;

        boolean generating = handler.isGenerating();
        if (wasGenerating && !generating) {
            completionFlashTicks = 6;
        }
        wasGenerating = generating;
        if (completionFlashTicks > 0) {
            completionFlashTicks--;
        }

        generateButton.active = handler.canStart();
        generateButton.setMessage(generating
            ? Text.translatable("screen.labscraft.generating")
            : generateLabel());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, THEME_DARK);
        context.drawBorder(x, y, this.backgroundWidth, this.backgroundHeight, THEME_COLOR);
        context.fill(x + 1, y + 1, x + this.backgroundWidth - 1, y + 17, THEME_HEADER);
        context.fill(x + 10, y + 16, x + this.backgroundWidth - 10, y + 17, THEME_COLOR);

        // Slot frames (input at 35,35; output at 125,35 in handler coordinates)
        drawSlotFrame(context, x + 34, y + 34);
        drawSlotFrame(context, x + 124, y + 34);

        // Progress bar between the slots
        int barX = x + 58;
        int barY = y + 38;
        int barWidth = 60;
        int barHeight = 10;
        context.fill(barX, barY, barX + barWidth, barY + barHeight, SLOT_BG);
        context.drawBorder(barX, barY, barWidth, barHeight, THEME_COLOR);
        if (handler.isGenerating()) {
            int fillWidth = (int) ((barWidth - 2) * handler.getProgressFraction());
            if (fillWidth > 0) {
                context.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + barHeight - 1, THEME_COLOR);
            }
        }

        // Player inventory backdrop
        context.fill(x + 7, y + 83, x + 169, y + 161, 0xFF26263A);

        if (completionFlashTicks > 0) {
            int alpha = completionFlashTicks * 25;
            context.fill(x + 1, y + 1, x + this.backgroundWidth - 1, y + this.backgroundHeight - 1,
                (alpha << 24) | 0xFFFFFF);
        }
    }

    private void drawSlotFrame(DrawContext context, int slotX, int slotY) {
        context.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, SLOT_BG);
        context.drawBorder(slotX - 1, slotY - 1, 18, 18, THEME_COLOR);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFFFFFF, false);

        Text status;
        int statusColor;
        if (handler.isGenerating()) {
            int percent = (int) (handler.getProgressFraction() * 100);
            status = Text.translatable("screen.labscraft.status.generating", percent);
            float pulse = (float) Math.sin(animationTick * 0.15) * 0.3f + 0.7f;
            int brightness = (int) (pulse * 255);
            statusColor = (brightness << 16) | (brightness << 8) | brightness;
        } else {
            status = Text.translatable("screen.labscraft.status.ready");
            statusColor = 0xCCCCCC;
        }
        int statusX = (this.backgroundWidth - this.textRenderer.getWidth(status)) / 2;
        context.drawText(this.textRenderer, status, statusX, 22, statusColor, false);

        Text count = Text.translatable("screen.labscraft.total_generations", handler.getCompletedCount());
        context.drawText(this.textRenderer, count,
            (this.backgroundWidth - this.textRenderer.getWidth(count)) / 2, 74,
            handler.getCompletedCount() > 0 ? 0x6699FF : 0x888888, false);

        context.drawText(this.textRenderer, this.playerInventoryTitle,
            this.playerInventoryTitleX, this.playerInventoryTitleY, 0xAAAAAA, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
