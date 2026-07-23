package com.labscraft.screen;

import com.labscraft.logic.FlowCraftingLogic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** Client screen for the Flow Crafting Table (10 TPU slots + output + craft buttons). */
public class FlowCraftingTableScreen extends HandledScreen<FlowCraftingTableScreenHandler> {
    private static final int ACCENT = 0xFF4285F4;

    private ButtonWidget craftNanoBananaButton;
    private ButtonWidget craftVeoButton;

    public FlowCraftingTableScreen(FlowCraftingTableScreenHandler handler, PlayerInventory inventory, Text title) {
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

        craftNanoBananaButton = ButtonWidget.builder(
            Text.translatable("screen.labscraft.craft_nano_banana", FlowCraftingLogic.NANO_BANANA_COST),
            button -> clickButton(FlowCraftingTableScreenHandler.CRAFT_NANO_BANANA_BUTTON_ID)
        ).dimensions(this.x + 8, this.y + 61, 78, 18).build();

        craftVeoButton = ButtonWidget.builder(
            Text.translatable("screen.labscraft.craft_veo", FlowCraftingLogic.VEO_COST),
            button -> clickButton(FlowCraftingTableScreenHandler.CRAFT_VEO_BUTTON_ID)
        ).dimensions(this.x + 90, this.y + 61, 78, 18).build();

        this.addDrawableChild(craftNanoBananaButton);
        this.addDrawableChild(craftVeoButton);
    }

    private void clickButton(int id) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, id);
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        craftNanoBananaButton.active = handler.canCraftNanoBanana();
        craftVeoButton.active = handler.canCraftVeo();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFFC6C6C6);
        context.drawBorder(x, y, this.backgroundWidth, this.backgroundHeight, 0xFF373737);
        context.fill(x + 1, y + 1, x + this.backgroundWidth - 1, y + 17, 0xFF8B8B8B);

        // TPU input grid backdrop (slots at 26..115, 22..57)
        context.fill(x + 24, y + 20, x + 117, y + 59, 0xFF373737);
        context.fill(x + 25, y + 21, x + 116, y + 58, 0xFF8B8B8B);

        // Output slot backdrop (slot at 134,31)
        context.fill(x + 131, y + 28, x + 153, y + 50, 0xFF373737);
        context.fill(x + 132, y + 29, x + 152, y + 49, 0xFF8B8B8B);

        // Arrow
        context.fill(x + 119, y + 36, x + 129, y + 42, ACCENT);

        // Player inventory backdrop
        context.fill(x + 7, y + 83, x + 169, y + 161, 0xFF8B8B8B);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);

        Text count = Text.translatable("screen.labscraft.tpu_count", handler.getTpuCount());
        context.drawText(this.textRenderer, count, 121, 20, 0x404040, false);

        context.drawText(this.textRenderer, this.playerInventoryTitle,
            this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
