package com.visualspearhitbox;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Mod Menu screen. Both Mod Menu and Cloth Config are optional, and the setting is read from the
 * json config when either is missing. Without Cloth Config the factory returns null, which is how
 * Mod Menu is told there is no screen instead of throwing when the button is clicked.
 */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
			return (ConfigScreenFactory<Screen>) parent -> null;
		}

		return (ConfigScreenFactory<Screen>) this::configScreen;
	}

	private Screen configScreen(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.literal("Visual Spear Hitbox"))
			.setSavingRunnable(VisualSpearHitbox::save);

		builder.getOrCreateCategory(Component.literal("General")).addEntry(builder.entryBuilder()
			.startBooleanToggle(Component.literal("Enabled"), VisualSpearHitbox.enabled)
			.setDefaultValue(true)
			.setTooltip(Component.literal("Draw the spear's visual hitbox with F3+B while a spear is held. Visual only: it never changes the real hitbox or any gameplay."))
			.setSaveConsumer(value -> VisualSpearHitbox.enabled = value)
			.build());

		return builder.build();
	}
}
