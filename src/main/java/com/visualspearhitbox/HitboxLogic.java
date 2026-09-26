package com.visualspearhitbox;

import net.minecraft.world.phys.AABB;

/**
 * Decides whether F3+B gets an extra box and how big it is. Holds no client classes so the tests can
 * run on a plain JVM.
 */
public final class HitboxLogic {
	/**
	 * What every vanilla spear uses. A server that never sent attack_range reports a margin of 0, and
	 * without this fallback we would draw nothing there.
	 */
	public static final float FALLBACK_MARGIN = 0.125F;

	private HitboxLogic() {
	}

	/** Returns the inflated box, or {@code vanillaBox} itself when there is nothing extra to draw. */
	public static AABB visualBox(
		AABB vanillaBox,
		float hitboxMargin,
		boolean holdingSpear,
		boolean targetIsLiving,
		boolean enabled
	) {
		if (vanillaBox == null || vanillaBox.getXsize() <= 0.0 || vanillaBox.getYsize() <= 0.0 || vanillaBox.getZsize() <= 0.0) {
			return vanillaBox;
		}

		if (!enabled || !holdingSpear || !targetIsLiving) {
			return vanillaBox;
		}

		// Holding the spear is the gate, the margin is only the size.
		float margin = (Float.isNaN(hitboxMargin) || hitboxMargin <= 0.0F) ? FALLBACK_MARGIN : hitboxMargin;

		// inflate returns a new box, the entity keeps its own.
		return vanillaBox.inflate(margin);
	}
}
