package com.bot.client.navigation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Very small Stage 1 movement controller.
 * - Accepts a target position (x,y,z).
 * - Each client tick it moves the actual player entity toward the target using a direction vector.
 * - Stops when within a small distance of the target.
 *
 * Notes / limitations:
 * - This stage purposefully ignores obstacles or terrain; it simply walks in a straight line toward the goal.
 * - It does not use camera entities, player rotation, or keybindings, which keeps it compatible with Freecam mods.
 */
public class MovementController {
    private static final double STOP_DISTANCE = 1.0D;
    private static final double WALK_SPEED = 0.215D;
    private static final double SLOWDOWN_DISTANCE = 2.0D;

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean active = false;
    private boolean appliedMovement = false;

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.active = true;
    }

    public void clearTarget() {
        this.active = false;
        stopControlledMovement();
    }

    public boolean isActive() {
        return active;
    }

    public Vec3d getTarget() {
        return active ? new Vec3d(targetX, targetY, targetZ) : null;
    }

    public void tick() {
        if (!active) return;

        /*
         * Freecam compatibility:
         * Always resolve and control the real client player entity here.
         * Do not use MinecraftClient#getCameraEntity(), camera rotation, or
         * keybinding state for navigation. Freecam mods often redirect those
         * concepts to a detached camera, which would make the bot move the
         * camera instead of the actual player.
         */
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double dx = targetX - px;
        double dy = targetY - py;
        double dz = targetZ - pz;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq <= STOP_DISTANCE * STOP_DISTANCE) {
            active = false;
            stopControlledMovement(player);

            if (client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(Text.literal("Arrived at target."));
            }
            return;
        }

        movePlayerTowardTarget(player, dx, dz, Math.sqrt(distSq));
    }

    private void movePlayerTowardTarget(ClientPlayerEntity player, double dx, double dz, double distance) {
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance <= 0.0001D) {
            stopControlledMovement(player);
            return;
        }

        /*
         * Movement is intentionally derived from the target delta, not from the
         * player's yaw/pitch. Rotating the player would also rotate the attached
         * camera in normal play and can fight Freecam/debug cameras. Applying a
         * horizontal velocity vector lets navigation continue while the camera
         * remains free for observation.
         */
        Vec3d velocity = player.getVelocity();
        double speed = WALK_SPEED * Math.min(1.0D, distance / SLOWDOWN_DISTANCE);
        double velocityX = dx / horizontalDistance * speed;
        double velocityZ = dz / horizontalDistance * speed;

        player.setVelocity(velocityX, velocity.y, velocityZ);
        appliedMovement = true;
    }

    private void stopControlledMovement() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            stopControlledMovement(client.player);
        }
    }

    private void stopControlledMovement(ClientPlayerEntity player) {
        if (!appliedMovement) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        player.setVelocity(0.0D, velocity.y, 0.0D);
        appliedMovement = false;
    }
}
