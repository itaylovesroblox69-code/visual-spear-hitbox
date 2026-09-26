package com.visualspearhitbox.mixin.client;

import com.mojang.logging.LogUtils;
import com.visualspearhitbox.HitboxLogic;
import com.visualspearhitbox.SpearDetection;
import com.visualspearhitbox.VisualSpearHitbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the spear's box over the vanilla one. Nothing here writes to the entity. */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
	private static final int SPEAR_BOX_COLOR = 0xFF00FFFF;

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Last spear written to the log, and the margin the game reported for it. */
	private static ItemStack lastLoggedSpear;
	private static float lastLoggedMargin = Float.NaN;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "showHitboxes(Lnet/minecraft/world/entity/Entity;FZ)V", at = @At("TAIL"))
	private void visualspearhitbox$addSpearBox(Entity entity, float partialTick, boolean serverEntity, CallbackInfo ci) {
		LocalPlayer player = this.minecraft.player;

		// Vanilla already drew the player's own box, and this runs before a world has loaded.
		if (player == null || entity == player) {
			return;
		}

		// Read every frame instead of reacting to a swap. The held slot can become a spear without
		// one happening, and it can stay the same ItemStack object while its contents change.
		ItemStack activeItem = player.getActiveItem();
		boolean holdingSpear = SpearDetection.isSpear(activeItem);
		float hitboxMargin = player.entityAttackRange().hitboxMargin();

		// One line per held spear, so "F3+B shows nothing" can be read off the log. Holding
		// something else is the normal case and stays quiet.
		if (holdingSpear && (lastLoggedSpear == null
			|| lastLoggedMargin != hitboxMargin
			|| SpearDetection.hasHeldItemChanged(lastLoggedSpear, activeItem))) {
			lastLoggedSpear = activeItem.copy();
			lastLoggedMargin = hitboxMargin;

			LOGGER.info(
				"[{}] F3+B spear box: held={} margin={}",
				VisualSpearHitbox.MOD_ID,
				activeItem.getItem(),
				hitboxMargin
			);
		}

		AABB vanillaBox = entity.getBoundingBox();
		AABB spearBox = HitboxLogic.visualBox(
			vanillaBox,
			hitboxMargin,
			holdingSpear,
			entity instanceof LivingEntity,
			VisualSpearHitbox.enabled
		);

		// Same instance back means draw nothing.
		if (spearBox == vanillaBox) {
			return;
		}

		Vec3 offset = entity.getPosition(partialTick).subtract(entity.position());
		Gizmos.cuboid(spearBox.move(offset), GizmoStyle.stroke(SPEAR_BOX_COLOR));
	}
}
