package com.dre.brewery;

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

    private final short colorId;

    private PotionColor(int colorId) {
        this.colorId = (short) colorId;
    }

    // gets the Damage Value, that sets a color on the potion
    // offset +32 is not accepted by brewer, so not further destillable
    public short getColorId(boolean destillable) {
//			if (destillable) {
//				return (short) (colorId + 64);
//			}
        //return (short) (colorId + 32);
        return (short) (colorId + 8192);
    }
}
