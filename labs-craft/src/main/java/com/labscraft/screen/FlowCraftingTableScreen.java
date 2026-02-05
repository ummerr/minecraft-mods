package com.labscraft.screen;

import com.labscraft.LabsCraft;
import com.labscraft.block.entity.FlowCraftingTableBlockEntity;
import com.labscraft.network.FlowCraftingTablePackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FlowCraftingTableScreen extends HandledScreen<FlowCraftingTableScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of(LabsCraft.MOD_ID, "textures/gui/flow_crafting_table.png");

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

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Craft Nano Banana button
        craftNanoBananaButton = ButtonWidget.builder(Text.literal("Craft Nano (5)"), button -> {
            FlowCraftingTablePackets.sendCraftNanoBananaPacket();
        }).dimensions(x + 98, y + 20, 70, 20).build();

        // Craft Veo button
        craftVeoButton = ButtonWidget.builder(Text.literal("Craft Veo (10)"), button -> {
            FlowCraftingTablePackets.sendCraftVeoPacket();
        }).dimensions(x + 98, y + 44, 70, 20).build();

        this.addDrawableChild(craftNanoBananaButton);
        this.addDrawableChild(craftVeoButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        // Update button states based on TPU count
        craftNanoBananaButton.active = handler.canCraftNanoBanana();
        craftVeoButton.active = handler.canCraftVeo();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw background - blue/tech theme
        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFFC6C6C6);
        context.drawBorder(x, y, this.backgroundWidth, this.backgroundHeight, 0xFF373737);

        // Draw title area
        context.fill(x + 1, y + 1, x + this.backgroundWidth - 1, y + 18, 0xFF8B8B8B);

        // Draw TPU input area background
        context.fill(x + 23, y + 23, x + 117, y + 63, 0xFF373737);
        context.fill(x + 24, y + 24, x + 116, y + 62, 0xFF8B8B8B);

        // Draw output slot background
        context.fill(x + 131, y + 32, x + 153, y + 54, 0xFF373737);
        context.fill(x + 132, y + 33, x + 152, y + 53, 0xFF8B8B8B);

        // Draw arrow
        context.fill(x + 118, y + 38, x + 130, y + 48, 0xFF4285F4);

        // Draw player inventory background
        context.fill(x + 7, y + 83, x + 169, y + 161, 0xFF8B8B8B);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Draw title centered
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);

        // Draw TPU count
        int tpuCount = handler.getTPUCount();
        String countText = "TPUs: " + tpuCount;
        context.drawText(this.textRenderer, countText, 26, 67, 0x404040, false);

        // Draw player inventory title
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
