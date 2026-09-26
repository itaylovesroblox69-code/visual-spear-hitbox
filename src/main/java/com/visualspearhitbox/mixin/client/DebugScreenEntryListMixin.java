package com.visualspearhitbox.mixin.client;

import com.visualspearhitbox.VisualSpearHitbox;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla drops every debug entry while reducedDebugInfo is on, which hides F3+B on most
 * multiplayer servers. This puts entity_hitboxes back when the player asked for it, and leaves every
 * other entry to the server.
 */
@Mixin(DebugScreenEntryList.class)
public class DebugScreenEntryListMixin {
	@Shadow
	private Map<Identifier, DebugScreenEntryStatus> allStatuses;

	@Shadow
	private boolean isOverlayVisible;

	@Shadow
	@Final
	private List<Identifier> currentlyEnabled;

	@Shadow
	private long currentlyEnabledVersion;

	@Inject(method = "rebuildCurrentList()V", at = @At("RETURN"))
	private void visualspearhitbox$restoreHitboxEntry(CallbackInfo ci) {
		if (!VisualSpearHitbox.enabled) {
			return;
		}

		DebugScreenEntryStatus status = this.allStatuses.get(DebugScreenEntries.ENTITY_HITBOXES);

		// The same condition vanilla uses a few lines above us.
		boolean userWantsIt = status == DebugScreenEntryStatus.ALWAYS_ON
			|| this.isOverlayVisible && status == DebugScreenEntryStatus.IN_OVERLAY;

		if (userWantsIt && !this.currentlyEnabled.contains(DebugScreenEntries.ENTITY_HITBOXES)) {
			this.currentlyEnabled.add(DebugScreenEntries.ENTITY_HITBOXES);
			this.currentlyEnabled.sort(Identifier::compareTo);

			// Vanilla already bumped this before RETURN. Bump it again or DebugRenderer does not
			// notice the entry and never builds EntityHitboxDebugRenderer.
			this.currentlyEnabledVersion++;
		}
	}
}
