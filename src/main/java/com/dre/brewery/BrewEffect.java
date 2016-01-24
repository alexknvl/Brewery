package com.dre.brewery;

import org.apache.commons.lang.math.IntRange;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

public class BrewEffect {
	private final PotionEffectType type;
	private final IntRange levelRange;
	private final IntRange durationRange;
	private final boolean hidden;

	public BrewEffect(PotionEffectType type, IntRange levelRange, IntRange durationRange, boolean hidden) {
		this.type = type;
		this.levelRange = levelRange;
		this.durationRange = durationRange;
		this.hidden = hidden;
	}

	public static BrewEffect parse(String effectString) {
		try {
			boolean hidden;
			String[] effectSplit = effectString.split("/");
			String effect = effectSplit[0];
			if (effect.equalsIgnoreCase("WEAKNESS") ||
					effect.equalsIgnoreCase("INCREASE_DAMAGE") ||
					effect.equalsIgnoreCase("SLOW") ||
					effect.equalsIgnoreCase("SPEED") ||
					effect.equalsIgnoreCase("REGENERATION")) {
				// hide these effects as they put crap into lore
				// Dont write Regeneration into Lore, its already there storing data!
				hidden = true;
			} else if (effect.endsWith("X")) {
				hidden = true;
				effect = effect.substring(0, effect.length() - 1);
			} else hidden = false;

			PotionEffectType type = PotionEffectType.getByName(effect);
			if (type == null) {
				BreweryPlugin.instance.errorLog("Effect: " + effect + " does not exist!");
				return null;
			}

			IntRange levelRange, durationRange;
			if (effectSplit.length == 3) {
				if (type.isInstant()) {
					levelRange = parseLevel(effectSplit[1].split("-"));
					durationRange = new IntRange(0, 0);
				} else {
					levelRange = parseLevel(effectSplit[1].split("-"));
					durationRange = parseDuration(effectSplit[2].split("-"));
				}
			} else if (effectSplit.length == 2) {
				if (type.isInstant()) {
					levelRange = parseLevel(effectSplit[1].split("-"));
					durationRange = new IntRange(0, 0);
				} else {
					durationRange = parseDuration(effectSplit[1].split("-"));
					levelRange = new IntRange(1, 3);
				}
			} else {
				levelRange = new IntRange(1, 3);
				durationRange = new IntRange(10, 20);
			}

			return new BrewEffect(type, levelRange, durationRange, hidden);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static IntRange parseLevel(String[] range) {
		if (range.length == 1)
			return new IntRange(1, BreweryPlugin.instance.parseInt(range[0]));
		else
			return new IntRange(BreweryPlugin.instance.parseInt(range[0]), BreweryPlugin.instance.parseInt(range[1]));
	}

	private static IntRange parseDuration(String[] range) {
		if (range.length == 1) {
			int value = BreweryPlugin.instance.parseInt(range[0]);
			return new IntRange(value / 8, value);
		} else {
			return new IntRange(BreweryPlugin.instance.parseInt(range[0]),
					BreweryPlugin.instance.parseInt(range[1]));
		}
	}

	public static void apply(BrewEffect effect, int quality, Player player, double intensity) {
		int duration = effect.getDuration(quality, intensity);
		int lvl = effect.getLevel(quality);

		if (lvl <= 0 || (duration <= 0 && !effect.type.isInstant())) {
			return;
		}

		duration *= 20;
		duration /= effect.type.getDurationModifier();
		effect.type.createEffect(duration, lvl - 1).apply(player);
	}

	public int getDuration(float quality, double intensity) {
		int minDuration = durationRange.getMinimumInteger();
		int maxDuration = durationRange.getMaximumInteger();
		return (int) Math.round((minDuration + ((maxDuration - minDuration) * (quality / 10.0))) * intensity);
	}

	public int getLevel(float quality) {
		int minLevel = levelRange.getMinimumInteger();
		int maxLevel = levelRange.getMaximumInteger();
		return (int) Math.round(minLevel + ((maxLevel - minLevel) * (quality / 10.0)));
	}

	public static void updateMeta(BrewEffect effect, PotionMeta meta, int quality) {
		int duration = effect.getDuration(quality, 1.0D);
		int level = effect.getLevel(quality);
		if ((duration > 0 || effect.type.isInstant()) && level > 0) {
			meta.addCustomEffect(effect.type.createEffect(0, 0), true);
		} else {
			meta.removeCustomEffect(effect.type);
		}
	}

	public boolean isValid() {
		int minLevel = levelRange.getMinimumInteger();
		int maxLevel = levelRange.getMaximumInteger();
		int minDuration = durationRange.getMinimumInteger();
		int maxDuration = durationRange.getMaximumInteger();
		return type != null && minLevel >= 0 && maxLevel >= 0 && minDuration >= 0 && maxDuration >= 0;
	}

	public boolean isHidden() {
		return hidden;
	}
}
