package com.labscraft.entity;

import com.google.gson.JsonObject;
import com.labscraft.agent.AgentBridge;
import com.labscraft.agent.RecentEventsTracker;
import com.labscraft.agent.WorldStateCollector;
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
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JoshWoodwardEntity extends PathAwareEntity {

    // ── Static dialogue (fallback when agent server is unavailable) ──

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

    // ── Agent bridge state ──

    private int agentTickCounter = 0;
    private int ticksSinceLastSpoke = 0;
    private final ConcurrentMap<String, RecentEventsTracker> playerEvents = new ConcurrentHashMap<>();

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

    public int getTicksSinceLastSpoke() {
        return ticksSinceLastSpoke;
    }

    public RecentEventsTracker getEventsTracker(ServerPlayerEntity player) {
        return playerEvents.computeIfAbsent(
                player.getUuidAsString(),
                k -> new RecentEventsTracker()
        );
    }

    // ── Tick ──

    @Override
    public void tick() {
        super.tick();

        ticksSinceLastSpoke++;

        if (this.getWorld().isClient) return;

        if (dialogueCooldown > 0) {
            dialogueCooldown--;
        }

        agentTickCounter++;

        AgentBridge bridge = AgentBridge.getInstance();

        // Process pending agent actions for nearby players
        if (bridge != null) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (this.distanceTo(player) < 32.0) {
                    bridge.getExecutor(player).tick(this, player, serverWorld);
                }
            }
        }

        // Send periodic state updates to agent server
        if (bridge != null && bridge.isAvailable()) {
            // Only tick at the configured rate (default: every 20 ticks = 1 second)
            // But skip if no player is nearby
            PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, 16.0D);
            if (nearestPlayer instanceof ServerPlayerEntity serverPlayer && agentTickCounter % 20 == 0) {
                RecentEventsTracker tracker = getEventsTracker(serverPlayer);

                // Only send to agent if there's something interesting happening
                if (tracker.hasEvents() || this.distanceTo(serverPlayer) < 6.0) {
                    JsonObject worldState = WorldStateCollector.collect(serverPlayer, this);
                    bridge.sendWorldState(serverPlayer, this, worldState, tracker);
                }
            }
            return; // Agent is handling dialogue, skip static path
        }

        // ── Fallback: static idle dialogue when agent server is down ──
        if (dialogueCooldown == 0) {
            PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, 5.0D);
            if (nearestPlayer != null && random.nextInt(100) < 5) {
                sayIdleDialogue(nearestPlayer);
                dialogueCooldown = DIALOGUE_COOLDOWN_TICKS;
            }
        }
    }

    private void sayIdleDialogue(PlayerEntity player) {
        String dialogue = IDLE_DIALOGUES[random.nextInt(IDLE_DIALOGUES.length)];
        player.sendMessage(Text.literal("<Josh Woodward> " + dialogue), false);
        ticksSinceLastSpoke = 0;
    }

    // ── Player interaction (right-click) ──

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
            AgentBridge bridge = AgentBridge.getInstance();

            if (bridge != null && bridge.isAvailable()) {
                // Agent path: send interaction event to agent server
                RecentEventsTracker tracker = getEventsTracker(serverPlayer);
                tracker.addInteraction(serverPlayer.getName().getString());

                JsonObject worldState = WorldStateCollector.collect(serverPlayer, this);
                bridge.sendWorldState(serverPlayer, this, worldState, tracker);

                ticksSinceLastSpoke = 0;
                return ActionResult.SUCCESS;
            }

            // Fallback: static dialogue
            return handleStaticInteraction(serverPlayer);
        }
        return ActionResult.PASS;
    }

    private ActionResult handleStaticInteraction(ServerPlayerEntity serverPlayer) {
        ServerWorld serverWorld = (ServerWorld) this.getWorld();
        QuestManager questManager = QuestManager.get(serverWorld);
        QuestStage stage = questManager.getStage(serverPlayer);

        String dialogue = getDialogueForStage(stage);
        serverPlayer.sendMessage(Text.literal("<Josh Woodward> " + dialogue), false);

        if (stage == QuestStage.NOT_STARTED) {
            giveTPUs(serverPlayer, 5);
            serverPlayer.sendMessage(Text.literal("<Josh Woodward> Here's 5 TPUs to get you started. Use the Flow Crafting Table to build a console."), false);
            questManager.advanceStage(serverPlayer);
        } else if (stage == QuestStage.FIRST_GENERATION) {
            giveTPUs(serverPlayer, 5);
            serverPlayer.sendMessage(Text.literal("<Josh Woodward> Great job! Here's 5 more TPUs. You can upgrade to a Veo Console now."), false);
            questManager.advanceStage(serverPlayer);
        }

        ticksSinceLastSpoke = 0;
        return ActionResult.SUCCESS;
    }

    private void giveTPUs(ServerPlayerEntity player, int count) {
        ItemStack tpuStack = new ItemStack(ModItems.TPU, count);
        if (!player.getInventory().insertStack(tpuStack)) {
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

    // ── Damage handling ──

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
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
