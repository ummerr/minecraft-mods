package com.labscraft.entity;

import com.labscraft.quest.PlayerQuestState;
import com.labscraft.quest.QuestManager;
import com.labscraft.quest.QuestStage;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Josh Woodward — deadpan Labs PM, onboarding NPC, and the body the agent
 * bridge (G5) drives. A {@link PathAwareEntity} with real navigation so
 * WALK_TO / LOOK_AT actions work.
 *
 * <p>Invulnerable (except /kill and the void), unpushable, never despawns.
 * Right-clicking fires {@link QuestManager#onTalkedToJosh} and then speaks a
 * stage-appropriate {@link JoshDialogue} line — the full quest line is
 * playable with no agent server running.</p>
 *
 * <h2>G5 agent-bridge surface</h2>
 * <ul>
 *   <li>{@link #say(String)} — broadcast a chat line to players within
 *       {@link #CHAT_RANGE} blocks (SAY action / defect #6 fix)</li>
 *   <li>{@link #playEmote(String)} — real synced animation + particles +
 *       complementary text (EMOTE action)</li>
 *   <li>{@link #walkTo(double, double, double, double)} and
 *       {@link #lookAtEntity(net.minecraft.entity.Entity)} — WALK_TO / LOOK_AT</li>
 *   <li>{@link #getCurrentActivity()} — {@code idle | walking | talking} for
 *       the /tick payload's {@code josh.current_activity}</li>
 *   <li>{@link #secondsSinceLastSpoke()} — {@code josh.last_spoke_seconds_ago}
 *       (-1 if he has never spoken)</li>
 * </ul>
 */
public class JoshWoodwardEntity extends PathAwareEntity {

    /** Chat broadcast radius in blocks (PROTOCOL-V2 SAY semantics). */
    public static final double CHAT_RANGE = 24.0;

    /** How long one emote plays, in ticks. */
    public static final int EMOTE_DURATION_TICKS = 40;

    /** Spoken within this many seconds => activity is "talking". */
    private static final long TALKING_WINDOW_SECONDS = 4;

    /** 0 = no emote; otherwise {@link JoshEmote#trackedValue()}. Synced to clients. */
    private static final TrackedData<Integer> EMOTE =
            DataTracker.registerData(JoshWoodwardEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private int emoteTicksLeft;
    private long lastSpokeGameTime = Long.MIN_VALUE;
    private int dialogueVariant;

    public JoshWoodwardEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setPersistent();
        setInvulnerable(true);
        setCustomName(Text.literal("Josh Woodward"));
        setCustomNameVisible(true);
    }

    public static DefaultAttributeContainer.Builder createJoshAttributes() {
        return createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 40.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.45)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(3, new WanderAroundFarGoal(this, 0.5));
        goalSelector.add(4, new LookAroundGoal(this));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(EMOTE, 0);
    }

    // ------------------------------------------------------------------
    // Indestructibility
    // ------------------------------------------------------------------

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Only /kill and falling out of the world get through. PMs are forever.
        if (source.isOf(DamageTypes.GENERIC_KILL) || source.isOf(DamageTypes.OUT_OF_WORLD)) {
            return super.damage(world, source, amount);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        // Unmoved, like his opinion of your sprint estimate.
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    // ------------------------------------------------------------------
    // Interaction: quest hook + static dialogue fallback
    // ------------------------------------------------------------------

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.SUCCESS;
        }

        QuestStage before = QuestManager.getState(serverPlayer).stage();
        QuestManager.onTalkedToJosh(serverPlayer);

        PlayerQuestState state = QuestManager.getState(serverPlayer);
        getLookControl().lookAt(serverPlayer);
        say(JoshDialogue.lineFor(state.stage(), state.objectives(), dialogueVariant++));

        if (state.stage() != before) {
            // Talking just moved the quest along — acknowledge it physically.
            playEmote(state.stage().isTerminal() ? JoshEmote.CLAP.id() : JoshEmote.NOD.id());
        }
        return ActionResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // Speech (SAY) — broadcast, not a whisper (v1 defect #6 fix)
    // ------------------------------------------------------------------

    /** Broadcasts a chat line from Josh to all players within {@link #CHAT_RANGE} blocks. */
    public void say(String text) {
        if (!(getWorld() instanceof ServerWorld world) || text == null || text.isBlank()) {
            return;
        }
        lastSpokeGameTime = world.getTime();
        com.labscraft.LabsCraft.LOGGER.info("[Josh] {}", text);
        Text message = Text.literal("<Josh Woodward> ").formatted(Formatting.GOLD)
                .append(Text.literal(text).formatted(Formatting.WHITE));
        for (ServerPlayerEntity nearby : world.getPlayers(p -> p.squaredDistanceTo(this) <= CHAT_RANGE * CHAT_RANGE)) {
            nearby.sendMessage(message, false);
        }
    }

    // ------------------------------------------------------------------
    // Emotes — synced animation + particles + complementary text
    // ------------------------------------------------------------------

    /**
     * Plays an emote by wire id ({@code nod | shake_head | shrug | point |
     * facepalm | clap}). Head emotes are driven server-side through real
     * pitch/head-yaw motion (synced natively); arm emotes are posed client-side
     * by the renderer from the tracked emote value. Both get particles and an
     * italic action line for nearby players. Returns false for unknown ids.
     */
    public boolean playEmote(String emoteId) {
        JoshEmote emote = JoshEmote.fromId(emoteId);
        if (emote == null || !(getWorld() instanceof ServerWorld world)) {
            return false;
        }
        dataTracker.set(EMOTE, emote.trackedValue());
        emoteTicksLeft = EMOTE_DURATION_TICKS;
        getNavigation().stop();

        Text action = Text.literal("* Josh Woodward " + emote.verb())
                .formatted(Formatting.GRAY, Formatting.ITALIC);
        for (ServerPlayerEntity nearby : world.getPlayers(p -> p.squaredDistanceTo(this) <= CHAT_RANGE * CHAT_RANGE)) {
            nearby.sendMessage(action, false);
        }
        world.spawnParticles(particleFor(emote), getX(), getEyeY() + 0.4, getZ(), 6, 0.3, 0.2, 0.3, 0.01);
        return true;
    }

    /** The currently playing emote, or null. Readable on both sides (tracked data). */
    @Nullable
    public JoshEmote getActiveEmote() {
        return JoshEmote.fromTrackedValue(dataTracker.get(EMOTE));
    }

    /** Raw tracked emote value for the client renderer (0 = none). */
    public int getEmoteTrackedValue() {
        return dataTracker.get(EMOTE);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            return;
        }
        if (emoteTicksLeft > 0) {
            applyEmoteMotion(EMOTE_DURATION_TICKS - emoteTicksLeft);
            emoteTicksLeft--;
            if (emoteTicksLeft == 0) {
                dataTracker.set(EMOTE, 0);
                setPitch(0.0f);
            }
        }
    }

    /** Server-side head motion for the head emotes; runs after AI so it wins the tick. */
    private void applyEmoteMotion(int t) {
        JoshEmote emote = getActiveEmote();
        if (emote == null) {
            return;
        }
        switch (emote) {
            case NOD -> setPitch(28.0f * MathHelper.sin(t * 0.55f));
            case SHAKE_HEAD -> {
                float swing = 35.0f * MathHelper.sin(t * 0.55f);
                setHeadYaw(bodyYaw + swing);
                setYaw(bodyYaw);
            }
            case FACEPALM -> setPitch(32.0f); // looking down into the palm
            default -> {
                // Arm emotes (shrug/point/clap) are posed by the client model.
            }
        }
    }

    private static ParticleEffect particleFor(JoshEmote emote) {
        return switch (emote) {
            case NOD, CLAP -> ParticleTypes.HAPPY_VILLAGER;
            case SHAKE_HEAD -> ParticleTypes.SMOKE;
            case SHRUG -> ParticleTypes.CLOUD;
            case POINT -> ParticleTypes.END_ROD;
            case FACEPALM -> ParticleTypes.CRIT;
        };
    }

    // ------------------------------------------------------------------
    // Agent-bridge accessors (G5)
    // ------------------------------------------------------------------

    /** {@code idle | walking | talking} for the /tick payload. */
    public String getCurrentActivity() {
        long spoke = secondsSinceLastSpoke();
        if (spoke >= 0 && spoke < TALKING_WINDOW_SECONDS) {
            return "talking";
        }
        if (!getNavigation().isIdle()) {
            return "walking";
        }
        return "idle";
    }

    /** Seconds since Josh last said anything, or -1 if he never has. */
    public long secondsSinceLastSpoke() {
        if (lastSpokeGameTime == Long.MIN_VALUE || !(getWorld() instanceof ServerWorld world)) {
            return -1;
        }
        return Math.max(0, (world.getTime() - lastSpokeGameTime) / 20);
    }

    /** Starts real pathfinding toward a position. Speed is a modifier around 0.1–1.0. */
    public boolean walkTo(double x, double y, double z, double speed) {
        return getNavigation().startMovingTo(x, y, z, speed);
    }

    /** Turns Josh's head toward an entity. */
    public void lookAtEntity(net.minecraft.entity.Entity target) {
        getLookControl().lookAt(target);
    }

    /** Turns Josh's head toward a position. */
    public void lookAtPosition(double x, double y, double z) {
        getLookControl().lookAt(x, y, z);
    }
}
