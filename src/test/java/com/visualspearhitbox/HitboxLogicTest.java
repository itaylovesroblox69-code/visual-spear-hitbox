package com.visualspearhitbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HitboxLogicTest {
	private static final AABB VANILLA = new AABB(0.0, 0.0, 0.0, 0.6, 1.8, 0.6);

	private static final float SPEAR_MARGIN = 0.125F;
	private static final float MISSING_MARGIN = 0.0F;
	private static final boolean HOLDING_SPEAR = true;
	private static final boolean HOLDING_ORDINARY_ITEM = false;
	private static final boolean LIVING = true;
	private static final boolean NOT_LIVING = false;
	private static final boolean ON = true;
	private static final boolean OFF = false;

	@Test
	@DisplayName("Holding a spear grows the box by the spear's own margin on every side")
	void heldSpearInflatesTheBox() {
		AABB drawn = HitboxLogic.visualBox(VANILLA, SPEAR_MARGIN, HOLDING_SPEAR, LIVING, ON);

		assertNotSame(VANILLA, drawn, "a spear must produce a new, larger box");
		assertEquals(-0.125, drawn.minX, 1e-9);
		assertEquals(-0.125, drawn.minY, 1e-9);
		assertEquals(-0.125, drawn.minZ, 1e-9);
		assertEquals(0.725, drawn.maxX, 1e-9);
		assertEquals(1.925, drawn.maxY, 1e-9);
		assertEquals(0.725, drawn.maxZ, 1e-9);
	}

	@Test
	@DisplayName("A held spear with no synced margin (vanilla server) still draws, using 0.125")
	void zeroMarginOnAHeldSpearFallsBackToTheVanillaSpearMargin() {
		AABB drawn = HitboxLogic.visualBox(VANILLA, MISSING_MARGIN, HOLDING_SPEAR, LIVING, ON);

		assertNotSame(VANILLA, drawn, "a held spear must draw even when the margin was never synced");
		assertEquals(-0.125, drawn.minX, 1e-9);
		assertEquals(0.725, drawn.maxX, 1e-9);
		assertEquals(1.925, drawn.maxY, 1e-9);
	}

	@Test
	@DisplayName("A NaN or negative margin on a held spear falls back instead of breaking the draw")
	void nonsenseMarginOnAHeldSpearFallsBackToo() {
		AABB nan = HitboxLogic.visualBox(VANILLA, Float.NaN, HOLDING_SPEAR, LIVING, ON);
		assertEquals(VANILLA.inflate(HitboxLogic.FALLBACK_MARGIN), nan);

		AABB negative = HitboxLogic.visualBox(VANILLA, -1.0F, HOLDING_SPEAR, LIVING, ON);
		assertEquals(VANILLA.inflate(HitboxLogic.FALLBACK_MARGIN), negative);
	}

	@Test
	@DisplayName("A sword or an empty hand returns the very same box instance, untouched")
	void ordinaryItemKeepsTheVanillaBox() {
		// A margin on its own means nothing, the spear is the gate.
		assertSame(VANILLA, HitboxLogic.visualBox(VANILLA, 0.5F, HOLDING_ORDINARY_ITEM, LIVING, ON));
		assertSame(VANILLA, HitboxLogic.visualBox(VANILLA, MISSING_MARGIN, HOLDING_ORDINARY_ITEM, LIVING, ON));
	}

	@Test
	@DisplayName("Non-living targets (dropped items, arrows, boats) keep the vanilla box")
	void nonLivingTargetsKeepTheVanillaBox() {
		assertSame(VANILLA, HitboxLogic.visualBox(VANILLA, SPEAR_MARGIN, HOLDING_SPEAR, NOT_LIVING, ON));
	}

	@Test
	@DisplayName("Disabled means vanilla behaviour")
	void disabledNeverInflates() {
		assertSame(VANILLA, HitboxLogic.visualBox(VANILLA, SPEAR_MARGIN, HOLDING_SPEAR, LIVING, OFF));
	}

	@Test
	@DisplayName("A custom attack_range margin is applied verbatim")
	void customMarginIsApplied() {
		AABB drawn = HitboxLogic.visualBox(VANILLA, 0.8F, HOLDING_SPEAR, LIVING, ON);

		assertEquals(-0.8, drawn.minX, 1e-6);
		assertEquals(2.6, drawn.maxY, 1e-6);
	}

	@Test
	@DisplayName("Inflating never mutates the box it was given")
	void neverMutatesTheRealBox() {
		AABB realBox = new AABB(0.0, 0.0, 0.0, 0.6, 1.8, 0.6);
		AABB before = new AABB(0.0, 0.0, 0.0, 0.6, 1.8, 0.6);

		AABB drawn = HitboxLogic.visualBox(realBox, SPEAR_MARGIN, HOLDING_SPEAR, LIVING, ON);

		assertNotSame(realBox, drawn);
		assertEquals(before, realBox, "the entity's real box must be untouched");
	}

	@Test
	@DisplayName("A zero-volume box or null is passed straight through")
	void invalidInputIsPassedThrough() {
		AABB marker = new AABB(2.0, 2.0, 2.0, 2.0, 2.0, 2.0);
		assertSame(marker, HitboxLogic.visualBox(marker, SPEAR_MARGIN, HOLDING_SPEAR, LIVING, ON));
		assertNull(HitboxLogic.visualBox(null, SPEAR_MARGIN, HOLDING_SPEAR, LIVING, ON), "null in, null out");
	}
}
