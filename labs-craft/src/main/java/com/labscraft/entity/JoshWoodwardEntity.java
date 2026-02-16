package com.labscraft.entity;

import com.labscraft.item.ModItems;
import com.labscraft.quest.QuestManager;
import com.labscraft.quest.QuestStage;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Random;

public class JoshWoodwardEntity extends PathAwareEntity {

    private static final String[] IDLE_DIALOGUES = {
        "Let me check my calendar...",
        "We should sync on this later.",
        "This is a P0 for Q1.",
        "Can you add that to the PRD?",
        "Let's take this offline.",
        "I'll ping you on Slack.",
        "We need to align on OKRs.",
        "That's definitely in scope for H2."
    };

    private static final int DIALOGUE_COOLDOWN_TICKS = 200; // 10 seconds
    private int dialogueCooldown = 0;
    private final Random random = new Random();

    public JoshWoodwardEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setCustomName(Text.literal("Josh Woodward"));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.5D));
        this.goalSelector.add(3, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (dialogueCooldown > 0) {
            dialogueCooldown--;
        }

        // Idle dialogue when player is nearby
        if (!this.getWorld().isClient && dialogueCooldown == 0) {
            PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, 5.0D);
            if (nearestPlayer != null && random.nextInt(100) < 5) { // 5% chance per tick when player nearby
                sayIdleDialogue(nearestPlayer);
                dialogueCooldown = DIALOGUE_COOLDOWN_TICKS;
            }
        }
    }

    private void sayIdleDialogue(PlayerEntity player) {
        String dialogue = IDLE_DIALOGUES[random.nextInt(IDLE_DIALOGUES.length)];
        player.sendMessage(Text.literal("<Josh Woodward> " + dialogue), false);
        this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.NEUTRAL, 0.6f, 1.0f);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            QuestManager questManager = QuestManager.get(serverWorld);
            QuestStage stage = questManager.getStage(serverPlayer);

            String dialogue = getDialogueForStage(stage);
            player.sendMessage(Text.literal("<Josh Woodward> " + dialogue), false);
            serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.NEUTRAL, 0.8f, 1.1f);

            // Give TPU rewards and advance quest based on stage
            if (stage == QuestStage.NOT_STARTED) {
                // Welcome gift: 5 TPUs to build a Nano Banana Console
                giveTPUs(serverPlayer, 5);
                player.sendMessage(Text.literal("<Josh Woodward> Here's 5 TPUs to get you started. Use the Flow Crafting Table to build a console."), false);
                serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.8f);
                questManager.advanceStage(serverPlayer);
            } else if (stage == QuestStage.FIRST_GENERATION) {
                // Reward for first generation: 5 more TPUs to upgrade to Veo
                giveTPUs(serverPlayer, 5);
                player.sendMessage(Text.literal("<Josh Woodward> Great job! Here's 5 more TPUs. You can upgrade to a Veo Console now."), false);
                serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.8f);
                questManager.advanceStage(serverPlayer);
            }

            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private void giveTPUs(ServerPlayerEntity player, int count) {
        ItemStack tpuStack = new ItemStack(ModItems.TPU, count);
        if (!player.getInventory().insertStack(tpuStack)) {
            // If inventory is full, drop the items
            ItemEntity itemEntity = new ItemEntity(
                player.getWorld(),
                player.getX(), player.getY(), player.getZ(),
                tpuStack
            );
            player.getWorld().spawnEntity(itemEntity);
        }
    }

    private String getDialogueForStage(QuestStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "Oh hey, you're the new APM intern? Welcome to Labs. Let me get you set up with some TPUs.";
            case FLOW_INTRO -> "Use the Flow Crafting Table to combine TPUs into consoles. 5 TPUs for Nano Banana (images), 10 for Veo (videos).";
            case LEARNING_PIPELINE -> "Build a console and generate something. You can mine more TPUs underground or craft them.";
            case FIRST_GENERATION -> "Nice! You got the image gen working. Talk to me again for some bonus TPUs.";
            case COMPLETED -> "You've got both consoles unlocked. Not bad for an intern. Keep mining TPUs if you need more.";
        };
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Josh can't be damaged - he's got meetings to attend
        if (source.getAttacker() instanceof PlayerEntity player) {
            player.sendMessage(Text.literal("<Josh Woodward> Hey, I've got a meeting. Can we not do this?"), false);
        }
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
