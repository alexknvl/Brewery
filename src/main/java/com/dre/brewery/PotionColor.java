package com.dre.brewery;

import com.google.common.base.Optional;

/**
 * Created by alex on 1/24/16.
 */
public enum PotionColor {
    PINK(1),
    CYAN(2),
    ORANGE(3),
    GREEN(4),
    BRIGHT_RED(5),
    BLUE(6),
    BLACK(8),
    RED(9),
    GREY(10),
    WATER(11),
    DARK_RED(12),
    BRIGHT_GREY(14);

    private final int colorId;

    PotionColor(int colorId) {
        this.colorId = colorId;
    }

	private static final int COLOR_MASK = (1 << 4) - 1;
	private static final int MAX_COLOR_VALUE = 14;
	private static final int NAME_MASK = (1 << 6) - 1;

	private static final int STRONG_BIT = 1 << 5;
	private static final int LONG_BIT = 1 << 6;
	private static final int DRINKABLE_BIT = 1 << 13;
	private static final int SPLASH_BIT = 1 << 14;

	private static final PotionColor[] lookupArray;
	static {
		lookupArray = new PotionColor[MAX_COLOR_VALUE + 1];
		for (PotionColor color : values()) {
			lookupArray[color.colorId] = color;
		}
	}

	/**
	 * Returns the durability value that sets potion color.
	 * This method will break in 1.9.
	 */
    public short getColorId(boolean distillable) {
        return (short) (colorId | DRINKABLE_BIT);
    }

	public static Optional<PotionColor> getColor(short durabilityValue) {
		int value = durabilityValue & COLOR_MASK;
		if (value > MAX_COLOR_VALUE) return Optional.absent();
		else return Optional.of(lookupArray[value]);
	}

	public static short toSplashPotion(short durabilityValue) {
		int value = durabilityValue & ((1 << 7) - 1);
		return (short) (value | SPLASH_BIT);
	}
}
