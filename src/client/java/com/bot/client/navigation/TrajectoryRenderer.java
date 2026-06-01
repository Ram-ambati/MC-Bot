package com.bot.client.navigation;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;

/**
 * Renders a simple debug trajectory from player to active /go target.
 * Stage 1 visualization:
 * - cyan line from player to target
 * - yellow marker box at destination
 */
public class TrajectoryRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PATH_COLOR = 0xFF00FFFF;
    private static final int TARGET_COLOR = 0xFFFFFF00;
    private static final float PATH_LINE_WIDTH = 4.0F;
    private static final float MARKER_LINE_WIDTH = 2.0F;

    private final MovementController movementController;
    private boolean disabledDueToRenderError;

    public TrajectoryRenderer(MovementController movementController) {
        this.movementController = movementController;
    }

    public void register() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::onBeforeDebugRender);
    }

    private void onBeforeDebugRender(WorldRenderContext context) {
        if (disabledDueToRenderError) {
            return;
        }

        try {
            renderTrajectory(context);
        } catch (Throwable t) {
            disabledDueToRenderError = true;
            LOGGER.error("Disabling trajectory overlay after render exception", t);
        }
    }

    private void renderTrajectory(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        Vec3d target = movementController.getTarget();

        if (target == null) {
            return;
        }

        VertexConsumer consumer = context.consumers().getBuffer(RenderLayers.lines());
        Vec3d cameraPos = context.worldState().cameraRenderState.pos;

        /*
         * Rendering is the only place where camera position should be used.
         * Fabric's world render consumers expect coordinates relative to the
         * active camera, so the overlay subtracts cameraPos for drawing.
         *
         * The trajectory itself still starts at MinecraftClient#player, not the
         * camera entity. This keeps the visual path anchored to the actual
         * player while Freecam is active.
         */
        double startWorldX = client.player.getX();
        double startWorldY = client.player.getY() + 0.05D;
        double startWorldZ = client.player.getZ();

        float startX = (float) (startWorldX - cameraPos.x);
        float startY = (float) (startWorldY - cameraPos.y);
        float startZ = (float) (startWorldZ - cameraPos.z);
        float targetX = (float) (target.x - cameraPos.x);
        float targetY = (float) (target.y + 0.05D - cameraPos.y);
        float targetZ = (float) (target.z - cameraPos.z);

        drawLine(
                consumer,
                startX,
                startY,
                startZ,
                targetX,
                targetY,
                targetZ,
                PATH_COLOR,
                PATH_LINE_WIDTH
        );

        float minX = targetX - 0.25F;
        float minY = targetY;
        float minZ = targetZ - 0.25F;
        float maxX = targetX + 0.25F;
        float maxY = targetY + 0.5F;
        float maxZ = targetZ + 0.25F;

        // Bottom square
        drawLine(consumer, minX, minY, minZ, maxX, minY, minZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, maxX, minY, minZ, maxX, minY, maxZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, maxX, minY, maxZ, minX, minY, maxZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, minX, minY, maxZ, minX, minY, minZ, TARGET_COLOR, MARKER_LINE_WIDTH);

        // Top square
        drawLine(consumer, minX, maxY, minZ, maxX, maxY, minZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, maxX, maxY, minZ, maxX, maxY, maxZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, maxX, maxY, maxZ, minX, maxY, maxZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, minX, maxY, maxZ, minX, maxY, minZ, TARGET_COLOR, MARKER_LINE_WIDTH);

        // Vertical edges
        drawLine(consumer, minX, minY, minZ, minX, maxY, minZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, maxX, minY, minZ, maxX, maxY, minZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, maxX, minY, maxZ, maxX, maxY, maxZ, TARGET_COLOR, MARKER_LINE_WIDTH);
        drawLine(consumer, minX, minY, maxZ, minX, maxY, maxZ, TARGET_COLOR, MARKER_LINE_WIDTH);
    }

    private static void drawLine(
            VertexConsumer consumer,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            int color,
            float lineWidth
    ) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        float nx = 0.0F;
        float ny = 1.0F;
        float nz = 0.0F;

        if (length > 0.0001F) {
            nx = dx / length;
            ny = dy / length;
            nz = dz / length;
        }

        consumer.vertex(x1, y1, z1)
                .color(color)
                .normal(nx, ny, nz)
                .lineWidth(lineWidth);

        consumer.vertex(x2, y2, z2)
                .color(color)
                .normal(nx, ny, nz)
                .lineWidth(lineWidth);
    }
}

