package dev.bobodado.acquiredutils;

import dev.bobodado.acquiredutils.config.AcquiredUtilsConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (client + server safe) entrypoint for AcquiredUtils.
 * <p>
 * Everything in this class must be safe to run on a dedicated server, since
 * "environment": "*" in fabric.mod.json means this mod loads on both sides.
 * All GUI code lives under the client-only source set (src/client/java) and
 * must never be referenced from here.
 */
public class AcquiredUtils implements ModInitializer {

	public static final String MOD_ID = "acquiredutils";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[AcquiredUtils] Initializing common entrypoint");

		// Load (or create-with-defaults) the config on startup so both the
		// client GUI and any server-relevant logic can read it immediately.
		AcquiredUtilsConfig.load();
	}
}
