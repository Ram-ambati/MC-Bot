package com.bot.client;

import com.bot.client.navigation.MovementController;
import com.bot.client.navigation.TrajectoryRenderer;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Client entrypoint: minimal Stage 1 movement control.
 * Behavior:
 * - Registers a client-side /go <x> <y> <z> command to set a movement target.
 * - Ticks the movement controller every client tick so the player can walk toward the active target.
 */
public class BotClient implements ClientModInitializer {
	private static final Logger LOGGER = LogUtils.getLogger();
	private final MovementController movementController = new MovementController();
	private final TrajectoryRenderer trajectoryRenderer = new TrajectoryRenderer(movementController);

	private static String formatCoordinate(double value) {
		return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
	}

	@Override
	public void onInitializeClient() {
		trajectoryRenderer.register();

		// Register a client-side command /go <x> <y> <z>
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
					literal("go")
							.then(argument("x", DoubleArgumentType.doubleArg())
									.then(argument("y", DoubleArgumentType.doubleArg())
											.then(argument("z", DoubleArgumentType.doubleArg())
													.executes(ctx -> {
														double x = DoubleArgumentType.getDouble(ctx, "x");
														double y = DoubleArgumentType.getDouble(ctx, "y");
														double z = DoubleArgumentType.getDouble(ctx, "z");
														movementController.setTarget(x, y, z);
														MinecraftClient client = MinecraftClient.getInstance();
														if (client != null && client.inGameHud != null) {
																	client.inGameHud.getChatHud().addMessage(Text.literal("Going to " + formatCoordinate(x) + " " + formatCoordinate(y) + " " + formatCoordinate(z)));
														}
														return 1;
														}))))));

		// Register a tick listener to update movement each client tick.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			try {
				movementController.tick();
			} catch (Throwable t) {
				// Prevent exceptions from crashing the client tick loop.
				LOGGER.error("Unhandled error in client tick", t);
			}
		});
	}
}
