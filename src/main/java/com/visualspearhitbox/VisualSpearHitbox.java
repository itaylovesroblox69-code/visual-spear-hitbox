package com.visualspearhitbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualSpearHitbox implements ClientModInitializer {
	public static final String MOD_ID = "visualspearhitbox";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("visualspearhitbox.json");

	public static boolean enabled = true;

	@Override
	public void onInitializeClient() {
		load();
		LOGGER.info("[{}] active: F3+B draws a visual-only spear hitbox (enabled={})", MOD_ID, enabled);
	}

	/** Falls back to defaults if the config is missing or broken. */
	public static void load() {
		if (!Files.exists(CONFIG)) {
			save();
			return;
		}

		try {
			Config loaded = GSON.fromJson(Files.readString(CONFIG, StandardCharsets.UTF_8), Config.class);

			if (loaded != null) {
				enabled = loaded.enabled;
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("[{}] config unreadable ({}); keeping enabled={}", MOD_ID, e.toString(), enabled);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG.getParent());
			Files.writeString(CONFIG, GSON.toJson(new Config()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("[{}] could not write config: {}", MOD_ID, e.toString());
		}
	}

	private static final class Config {
		boolean enabled = VisualSpearHitbox.enabled;
	}
}
