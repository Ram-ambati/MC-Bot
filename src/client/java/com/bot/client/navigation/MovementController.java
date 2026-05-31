package com.bot.client.navigation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Very small Stage 1 movement controller.
 * - Accepts a target position (x,y,z).
 * - Each client tick it rotates the player to face the target and simulates forward key input.
 * - Stops when within a small distance of the target.
 *
 * Notes / limitations:
 * - This stage purposefully ignores obstacles or terrain; it simply walks in a straight line toward the goal.
 * - It uses the game's forward KeyBinding to simulate movement; this may interact with normal player input.
 */
public class MovementController {
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean active = false;
    private static final double STOP_DISTANCE = 1.0D;

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.active = true;
    }

    public void clearTarget() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public Vec3d getTarget() {
        return active ? new Vec3d(targetX, targetY, targetZ) : null;
    }

    public void tick(MinecraftClient client) {
        if (!active) return;
        if (client.player == null) return;

        PlayerEntity player = client.player;
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double dx = targetX - px;
        double dy = targetY - py;
        double dz = targetZ - pz;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq <= STOP_DISTANCE * STOP_DISTANCE) {
            // arrived
            active = false;
            // release forward key if we set it
            try {
                KeyBinding forward = client.options.forwardKey;
                forward.setPressed(false);
            } catch (Throwable ignored) {
            }

            if (client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(Text.literal("Arrived at target."));
            }
            return;
        }

        // Compute yaw and pitch to face the target (immediate snap)
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));

        // Apply rotation directly to the player (client-only). This will visually rotate the view.
        // Use setter methods instead of direct field access.
        try {
            player.setYaw(yaw);
            player.setPitch(pitch);
        } catch (Throwable ignored) {
            // Fallback: if setters are not available on this mapping set, ignore rotation.
        }

        // Simulate forward key being pressed so the player moves ahead.
        try {
            KeyBinding forward = client.options.forwardKey;
            forward.setPressed(true);
        } catch (Throwable ignored) {
        }
    }
}

