package com.visualspearhitbox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public final class SpearDetection {
	private SpearDetection() {
	}

	/**
	 * True if the stack is a spear. The item id always reaches the client, so the vanilla tag is the
	 * signal to trust; attack_range is only a fallback for modded spears, because a vanilla server
	 * never sends it.
	 */
	public static boolean isSpear(ItemStack stack) {
		return stack != null && !stack.isEmpty() && (stack.is(ItemTags.SPEARS) || stack.has(DataComponents.ATTACK_RANGE));
	}

	/**
	 * Compares contents rather than stack identity. A held slot can be rewritten in place, leaving
	 * the same ItemStack object holding something else, and an identity check calls that unchanged.
	 */
	public static boolean hasHeldItemChanged(ItemStack previous, ItemStack current) {
		return previous == null
			|| current == null
			|| previous.isEmpty()
			|| current.isEmpty()
			|| !ItemStack.matches(previous, current);
	}
}
