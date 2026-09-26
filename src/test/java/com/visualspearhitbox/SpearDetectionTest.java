package com.visualspearhitbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Item tags are bound during datapack loading, which a plain JVM never does. So the tag itself is
 * checked by reading the data file the game binds ItemTags.SPEARS from, and the rest runs against the
 * real 1.21.11 registries.
 */
class SpearDetectionTest {
	private static final List<String> VANILLA_SPEAR_IDS = List.of(
		"minecraft:wooden_spear",
		"minecraft:stone_spear",
		"minecraft:copper_spear",
		"minecraft:iron_spear",
		"minecraft:golden_spear",
		"minecraft:diamond_spear",
		"minecraft:netherite_spear"
	);

	private static final AttackRange MODDED_SPEAR_RANGE = new AttackRange(1.0F, 3.0F, 1.0F, 3.0F, 0.125F, 1.0F);

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	@DisplayName("The vanilla #minecraft:spears tag data lists exactly the seven spears")
	void theVanillaTagDataListsExactlyTheSevenSpears() throws Exception {
		try (InputStream in = SpearDetectionTest.class.getResourceAsStream("/data/minecraft/tags/item/spears.json")) {
			assertTrue(in != null, "the Minecraft jar must contain data/minecraft/tags/item/spears.json");

			List<String> values = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
				.getAsJsonObject().getAsJsonArray("values").asList().stream()
				.map(e -> e.getAsString())
				.sorted()
				.toList();

			assertEquals(VANILLA_SPEAR_IDS.stream().sorted().toList(), values, "#minecraft:spears changed upstream");
		}
	}

	@Test
	@DisplayName("Every id in the tag data resolves to a distinct real item")
	void everyTaggedIdIsARealDistinctItem() {
		assertEquals(7, VANILLA_SPEAR_IDS.size());

		for (String id : VANILLA_SPEAR_IDS) {
			Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id.substring("minecraft:".length())));
			assertTrue(item != null && item != Items.AIR, id + " must be a real item");

			// No components on the stack, which is what a vanilla server sends.
			assertTrue(SpearDetection.isSpear(new ItemStack(item)), id + " must be detected as a spear");
		}
	}

	@Test
	@DisplayName("A modded spear outside the tag is caught by the attack_range fallback")
	void aSpearOutsideTheTagIsStillDetectedViaItsComponent() {
		ItemStack moddedSpear = new ItemStack(Items.DIAMOND_SWORD);
		moddedSpear.set(DataComponents.ATTACK_RANGE, MODDED_SPEAR_RANGE);

		assertTrue(moddedSpear.has(DataComponents.ATTACK_RANGE), "the component must be settable on a stack");
		assertTrue(SpearDetection.isSpear(moddedSpear), "attack_range is the fallback spear signal");
	}

	@Test
	@DisplayName("Empty and null stacks are not spears")
	void emptyAndNullAreNotSpears() {
		assertFalse(SpearDetection.isSpear(ItemStack.EMPTY));
		assertFalse(SpearDetection.isSpear(null));
	}

	@Test
	@DisplayName("Ordinary items are not spears")
	void ordinaryItemsAreNotSpears() {
		assertFalse(SpearDetection.isSpear(new ItemStack(Items.DIAMOND_SWORD)));
		assertFalse(SpearDetection.isSpear(new ItemStack(Items.COOKED_BEEF)));
		assertFalse(SpearDetection.isSpear(new ItemStack(Items.DIRT)));
	}

	@Test
	@DisplayName("The trident is not a spear")
	void theTridentIsNotASpear() {
		// Tag side is unbound here, so this only covers the fallback. The tag is covered by the
		// data file test above.
		ItemStack trident = new ItemStack(Items.TRIDENT);
		assertFalse(trident.has(DataComponents.ATTACK_RANGE), "the trident has no attack_range in 1.21.11");
		assertFalse(SpearDetection.isSpear(trident), "the trident must not draw the spear box");
	}

	@Test
	@DisplayName("A held slot that becomes a spear in place is seen, with no swap")
	void aHeldSlotChangedInPlaceIntoASpearIsStillDetected() {
		// The stack object the player holds stays the same, so an identity check would call this
		// unchanged and keep reporting the old item.
		ItemStack held = new ItemStack(Items.DIAMOND_SWORD);
		ItemStack snapshot = held.copy();

		assertFalse(SpearDetection.isSpear(held), "a plain sword is not a spear");

		held.set(DataComponents.ATTACK_RANGE, MODDED_SPEAR_RANGE);

		assertTrue(SpearDetection.isSpear(held), "the re-check must read the new contents");
		assertSame(held.getItem(), snapshot.getItem(), "only the components changed, not the item");
		assertTrue(SpearDetection.hasHeldItemChanged(snapshot, held), "so contents are what has to be compared");
	}

	@Test
	@DisplayName("A new stack object with the same contents is not a change")
	void equalContentsInADifferentStackObjectIsNotAChange() {
		// A plain swap builds a new object, which an identity check calls a change.
		ItemStack before = new ItemStack(Items.IRON_SPEAR);
		ItemStack after = before.copy();

		assertNotSame(before, after, "premise: the swap produced a different stack object");
		assertFalse(SpearDetection.hasHeldItemChanged(before, after), "identical contents are not a change");
	}

	@Test
	@DisplayName("An empty hand, or no snapshot to compare against, counts as changed")
	void emptyOrAbsentHeldStackCountsAsChanged() {
		ItemStack spear = new ItemStack(Items.IRON_SPEAR);

		assertTrue(SpearDetection.hasHeldItemChanged(null, spear), "nothing recorded yet");
		assertTrue(SpearDetection.hasHeldItemChanged(ItemStack.EMPTY, spear), "empty hand, then a spear");
		assertTrue(SpearDetection.hasHeldItemChanged(spear, ItemStack.EMPTY), "a spear, then an empty hand");
		assertTrue(SpearDetection.hasHeldItemChanged(spear, null), "null in, changed out");
		assertTrue(SpearDetection.hasHeldItemChanged(null, null));
	}
}
