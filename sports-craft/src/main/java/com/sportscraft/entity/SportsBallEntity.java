package com.sportscraft.entity;

import com.sportscraft.SportsCraft;
import com.sportscraft.SportsHooks;
import com.sportscraft.core.physics.BallPhysics;
import com.sportscraft.core.physics.BallSpec;
import com.sportscraft.core.physics.SurfaceType;
import com.sportscraft.core.physics.Vec3c;
import com.sportscraft.course.SurfaceClassifier;
import com.sportscraft.item.ModItems;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The ball. Extends {@link Entity} directly and implements
 * {@link FlyingItemEntity} rather than building on {@code ThrownItemEntity} or
 * {@code ProjectileEntity}: those despawn on their first hit and run client-side
 * prediction, both of which fight bounce-and-roll physics. Owner is a plain UUID
 * field in NBT instead of the projectile owner machinery.
 *
 * <p>Everything here is server-authoritative. There are no custom packets and no
 * client prediction — the client ticks no physics at all and simply renders the
 * position the server sends each tick.</p>
 *
 * <p>The class is deliberately thin. Every decision about what velocity becomes
 * next lives in {@link BallPhysics}; this class only supplies the facts the pure
 * layer cannot know: what the world collided with, and what block is underneath.</p>
 */
public class SportsBallEntity extends Entity implements FlyingItemEntity {

    private static final String NBT_OWNER = "ball_owner";
    private static final String NBT_SPORT = "sport";
    private static final String NBT_REST_TICKS = "rest_ticks";
    private static final String NBT_AT_REST = "at_rest";

    /** Below this Y the ball is gone for good, whatever the dimension floor is. */
    private static final int VOID_MARGIN = 8;

    @Nullable
    private UUID ownerUuid;

    private BallSpec spec = BallSpec.GOLF;
    private int restTicks;
    private boolean atRest;

    /** Guards against firing at-rest / water events repeatedly for one event. */
    private boolean restAnnounced;
    private boolean removalAnnounced;

    public SportsBallEntity(EntityType<? extends SportsBallEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // v1 tracks nothing: the ball's appearance never changes and position is
        // already synced by the entity tracker. A sport-id byte slot belongs here
        // when tennis needs the renderer to pick a different item stack.
    }

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setSpec(BallSpec spec) {
        this.spec = spec;
    }

    public BallSpec getSpec() {
        return spec;
    }

    public boolean isAtRest() {
        return atRest;
    }

    /** Launches the ball, waking it if it had settled. */
    public void launch(Vec3c velocity) {
        setVelocity(velocity.x(), velocity.y(), velocity.z());
        atRest = false;
        restTicks = 0;
        restAnnounced = false;
        velocityModified = true;
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(ModItems.GOLF_BALL);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();

        if (!(getWorld() instanceof ServerWorld serverWorld)) {
            // Server-authoritative: the client renders, it does not simulate.
            return;
        }

        if (checkOutOfPlay(serverWorld)) {
            return;
        }

        if (atRest) {
            // A settled ball costs nothing per tick until something disturbs it.
            if (!BallPhysics.shouldWake(toVec3c(getVelocity()), spec, isSupported())) {
                return;
            }
            atRest = false;
            restTicks = 0;
            restAnnounced = false;
        }

        stepPhysics(serverWorld);
    }

    private void stepPhysics(ServerWorld world) {
        Vec3c velocity = BallPhysics.applyFlight(toVec3c(getVelocity()), spec);

        // Capture before move(): move() zeroes any component that collided, so
        // reading velocity afterwards would always bounce off zero and the ball
        // would die on first contact.
        Vec3c preMove = velocity;
        setVelocity(preMove.x(), preMove.y(), preMove.z());
        move(MovementType.SELF, getVelocity());
        Vec3c postMove = toVec3c(getVelocity());

        SurfaceType surface = SurfaceClassifier.below(world, getBlockPos());
        Vec3c next = postMove;

        if (horizontalCollision) {
            boolean xBlocked = preMove.x() != 0.0 && postMove.x() == 0.0;
            boolean zBlocked = preMove.z() != 0.0 && postMove.z() == 0.0;
            if (xBlocked || zBlocked) {
                next = BallPhysics.bounceHorizontal(preMove, xBlocked, zBlocked, spec, surface);
            }
        }

        if (verticalCollision) {
            // Keep whatever the horizontal bounce decided, restore the vertical
            // speed the ball actually struck the ground with.
            next = BallPhysics.bounceVertical(next.withY(preMove.y()), spec, surface);
        }

        if (isOnGround() && next.y() == 0.0) {
            next = BallPhysics.roll(next, spec, surface);
        }

        setVelocity(next.x(), next.y(), next.z());
        velocityModified = true;

        restTicks = BallPhysics.nextRestTicks(restTicks, next, spec, isOnGround());
        if (BallPhysics.isAtRest(restTicks, spec)) {
            comeToRest(world);
        }
    }

    private void comeToRest(ServerWorld world) {
        atRest = true;
        setVelocity(Vec3d.ZERO);
        velocityModified = true;

        if (!restAnnounced) {
            restAnnounced = true;
            SportsHooks.fireBallAtRest(world, getUuid(), ownerUuid, getPos());
        }
    }

    /**
     * Water and the void take the ball out of play. Both are penalty situations
     * in golf, but this class does not know that — it fires the event and lets
     * the rules layer decide.
     */
    private boolean checkOutOfPlay(ServerWorld world) {
        if (removalAnnounced) {
            return true;
        }

        if (isTouchingWater() || SurfaceClassifier.classify(world, getBlockPos()) == SurfaceType.WATER) {
            removalAnnounced = true;
            SportsHooks.fireBallInWater(world, getUuid(), ownerUuid, getPos());
            discard();
            return true;
        }

        if (getY() < world.getBottomY() - VOID_MARGIN) {
            removalAnnounced = true;
            SportsHooks.fireBallOutOfBounds(world, getUuid(), ownerUuid, getPos());
            discard();
            return true;
        }
        return false;
    }

    /** True when there is something solid holding the ball up. */
    private boolean isSupported() {
        if (isOnGround()) {
            return true;
        }
        BlockPos below = getBlockPos().down();
        return getWorld().getBlockState(below).isSolidBlock(getWorld(), below);
    }

    // ------------------------------------------------------------------
    // Entity plumbing
    // ------------------------------------------------------------------

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        ownerUuid = nbt.containsUuid(NBT_OWNER) ? nbt.getUuid(NBT_OWNER) : null;
        spec = BallSpec.byId(nbt.getString(NBT_SPORT));
        restTicks = nbt.getInt(NBT_REST_TICKS);
        atRest = nbt.getBoolean(NBT_AT_REST);
        restAnnounced = atRest;
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (ownerUuid != null) {
            nbt.putUuid(NBT_OWNER, ownerUuid);
        }
        nbt.putString(NBT_SPORT, spec.id());
        nbt.putInt(NBT_REST_TICKS, restTicks);
        nbt.putBoolean(NBT_AT_REST, atRest);
    }

    /**
     * A ball cannot be damaged. It leaves play by being holed, drowned, lost to
     * the void, or picked up — never by taking a hit, and never by a stray arrow
     * or a splash of lava mid-round.
     */
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    /** Balls are scenery to the physics engine — players walk through them. */
    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Rendered small; no shadow-casting hitbox games needed. */
    @Override
    public boolean shouldRender(double distance) {
        return distance < 4096.0;
    }

    private static Vec3c toVec3c(Vec3d vec) {
        return new Vec3c(vec.x, vec.y, vec.z);
    }

    /** Spawns a ball at a position with an initial velocity, already in the world. */
    public static SportsBallEntity spawn(ServerWorld world, Vec3d pos, Vec3c velocity,
                                         @Nullable UUID owner, BallSpec spec) {
        SportsBallEntity ball = ModEntities.SPORTS_BALL.create(world, null);
        if (ball == null) {
            SportsCraft.LOGGER.error("Could not create a sports ball entity");
            return null;
        }
        ball.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0f, 0.0f);
        ball.setOwnerUuid(owner);
        ball.setSpec(spec);
        world.spawnEntity(ball);
        ball.launch(velocity);
        return ball;
    }
}
