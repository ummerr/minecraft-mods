package com.sportscraft.item;

import com.sportscraft.SportsHooks;
import com.sportscraft.core.physics.BallSpec;
import com.sportscraft.core.physics.Vec3c;
import com.sportscraft.core.swing.ClubSpec;
import com.sportscraft.core.swing.LaunchSolver;
import com.sportscraft.core.swing.SwingPower;
import com.sportscraft.entity.SportsBallEntity;
import java.util.Comparator;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A golf club. Charge and release, exactly like a bow and with zero custom
 * packets: {@code use()} starts the vanilla "using an item" state, the server
 * counts the held ticks, and {@code onStoppedUsing} does all the work. The
 * actionbar power meter is a plain server-to-client message, so there is no
 * networking code in this mod at all.
 *
 * <p>One class, three registrations — the clubs differ only by their
 * {@link ClubSpec}.</p>
 */
public class ClubItem extends Item {

    /** How far from a ball you can stand and still play it. */
    private static final double REACH = 3.0;

    /** Meter refresh rate; every tick is needless message spam. */
    private static final int METER_INTERVAL = 2;

    private final ClubSpec club;

    public ClubItem(Settings settings, ClubSpec club) {
        super(settings);
        this.club = club;
    }

    public ClubSpec club() {
        return club;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    /** BOW gives the free draw pose without any custom animation work. */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return;
        }
        int ticksHeld = getMaxUseTime(stack, user) - remainingUseTicks;
        if (ticksHeld % METER_INTERVAL == 0) {
            player.sendMessage(Text.literal(SwingPower.meter(club, ticksHeld, 10)), true);
        }
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)
                || !(world instanceof ServerWorld serverWorld)) {
            return false;
        }

        int ticksHeld = getMaxUseTime(stack, user) - remainingUseTicks;
        double power = SwingPower.forTicks(ticksHeld);

        // Aim comes from the player, elevation from the club — never look pitch.
        Vec3c velocity = LaunchSolver.solve(player.getYaw(), club, power);

        SportsBallEntity ball = findPlayableBall(serverWorld, player);
        if (ball != null) {
            ball.launch(velocity);
        } else {
            ball = spawnDrivingRangeBall(serverWorld, player, velocity);
            if (ball == null) {
                return false;
            }
        }

        playSwingFeedback(serverWorld, player, ball.getPos(), power);
        SportsHooks.fireStrokeTaken(player, club.id(), power);
        player.sendMessage(Text.literal(club.displayName() + " — "
                + (int) Math.round(power * 100) + "%"), true);
        return true;
    }

    /**
     * The player's own ball, at rest, within reach — nearest first so two balls
     * lying close together resolve predictably.
     */
    private SportsBallEntity findPlayableBall(ServerWorld world, ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(REACH);
        List<SportsBallEntity> nearby = world.getEntitiesByClass(SportsBallEntity.class, box,
                ball -> ball.isAtRest() && player.getUuid().equals(ball.getOwnerUuid()));
        return nearby.stream()
                .min(Comparator.comparingDouble(ball -> ball.squaredDistanceTo(player)))
                .orElse(null);
    }

    /** No ball in reach: driving-range mode drops a fresh one and hits it. */
    private SportsBallEntity spawnDrivingRangeBall(ServerWorld world, ServerPlayerEntity player,
                                                   Vec3c velocity) {
        Vec3c heading = LaunchSolver.headingFromYaw(player.getYaw());
        Vec3d spawnPos = player.getPos().add(heading.x(), 0.25, heading.z());
        return SportsBallEntity.spawn(world, spawnPos, velocity, player.getUuid(), BallSpec.GOLF);
    }

    private void playSwingFeedback(ServerWorld world, ServerPlayerEntity player, Vec3d at, double power) {
        world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_WOOD_BREAK,
                SoundCategory.PLAYERS, (float) (0.4 + power * 0.6), (float) (1.4 - power * 0.4));
        world.spawnParticles(ParticleTypes.CRIT, at.x, at.y + 0.1, at.z,
                (int) Math.round(2 + power * 10), 0.1, 0.1, 0.1, 0.02);
    }
}
