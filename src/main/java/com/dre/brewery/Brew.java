package com.dre.brewery;

import java.util.Map;
import java.util.HashMap;

import com.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Brew {

	// represents the liquid in the brewed Potions

	public static Map<Integer, Brew> potions = new HashMap<Integer, Brew>();

	public BIngredients ingredients;
	public int quality;
	public int distillRuns;
	public float ageTime;
	public float wood;
	public BrewRecipe currentRecipe;

	public boolean unlabeled;
	public boolean persistent;
	// static potions should not be changed
	public boolean immutable;

	public Brew(int uid, BIngredients ingredients) {
		this.ingredients = ingredients;
		potions.put(uid, this);
	}

	// quality already set
	public Brew(int uid, int quality, BrewRecipe recipe, BIngredients ingredients) {
		this.ingredients = ingredients;
		this.quality = quality;
		this.currentRecipe = recipe;
		potions.put(uid, this);
	}

	// loading from file
	public Brew(int uid, BIngredients ingredients, int quality, int distillRuns, float ageTime, float wood, String recipe, boolean unlabeled, boolean persistent, boolean immutable) {
		potions.put(uid, this);
		this.ingredients = ingredients;
		this.quality = quality;
		this.distillRuns = distillRuns;
		this.ageTime = ageTime;
		this.wood = wood;
		this.unlabeled = unlabeled;
		this.persistent = persistent;
		this.immutable = immutable;
		setRecipeFromString(recipe);
	}

	// returns a Brew by its UID
	public static Brew get(int uid) {
		if (uid < -1) {
			if (!potions.containsKey(uid)) {
				BreweryPlugin.instance.errorLog("Database failure! unable to find UID " + uid + " of a custom Potion!");
				return null;// throw some exception?
			}
		} else {
			return null;
		}
		return potions.get(uid);
	}

	// returns a Brew by PotionMeta
	public static Brew get(PotionMeta meta) {
		return get(getUID(meta));
	}

	// returns a Brew by ItemStack
	public static Brew get(ItemStack item) {
		if (item.getType() == Material.POTION) {
			if (item.hasItemMeta()) {
				PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
				return get(potionMeta);
			}
		}
		return null;
	}


	// returns UID of custom Potion item
	public static int getUID(ItemStack item) {
		return getUID((PotionMeta) item.getItemMeta());
	}

	// returns UID of custom Potion meta
	public static int getUID(PotionMeta potionMeta) {
		if (potionMeta.hasCustomEffect(PotionEffectType.REGENERATION)) {
			for (PotionEffect effect : potionMeta.getCustomEffects()) {
				if (effect.getType().equals(PotionEffectType.REGENERATION)) {
					if (effect.getDuration() < -1) {
						return effect.getDuration();
					}
				}
			}
		}
		return 0;
	}

	// generate an UID
	public static int generateUID() {
		int uid = -2;
		while (potions.containsKey(uid)) {
			uid -= 1;
		}
		return uid;
	}

	//returns the recipe with the given name, recalculates if not found
	public boolean setRecipeFromString(String name) {
		currentRecipe = null;
		if (name != null && !name.equals("")) {
			for (BrewRecipe recipe : BreweryPlugin.recipes) {
				if (recipe.getName(5).equalsIgnoreCase(name)) {
					currentRecipe = recipe;
					return true;
				}
			}

			if (quality > 0) {
				currentRecipe = ingredients.getBestRecipe(wood, ageTime, distillRuns > 0);
				if (currentRecipe != null) {
					if (!immutable) {
						this.quality = calcQuality();
					}
					BreweryPlugin.instance.log("Brew was made from Recipe: '" + name + "' which could not be found. '" + currentRecipe.getName(5) + "' used instead!");
					return true;
				} else {
					BreweryPlugin.instance.errorLog("Brew was made from Recipe: '" + name + "' which could not be found!");
				}
			}
		}
		return false;
	}

	public boolean reloadRecipe() {
		if (currentRecipe != null) {
			return setRecipeFromString(currentRecipe.getName(5));
		}
		return true;
	}

	// Copy a Brew with a new unique ID and return its item
	public ItemStack copy(ItemStack item) {
		ItemStack copy = item.clone();
		int uid = generateUID();
		clone(uid);
		PotionMeta meta = (PotionMeta) copy.getItemMeta();
		meta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);
		copy.setItemMeta(meta);
		return copy;
	}

	// Clones this instance with a new unique ID
	public Brew clone(int uid) {
		Brew brew = new Brew(uid, quality, currentRecipe, ingredients);
		brew.distillRuns = distillRuns;
		brew.ageTime = ageTime;
		brew.unlabeled = unlabeled;
		if (!brew.persistent) {
			brew.immutable = immutable;
		}
		return brew;
	}

	// remove potion from file (drinking, despawning, combusting, cmdDeleting, should be more!)
	public void remove(ItemStack item) {
		if (!persistent) {
			potions.remove(getUID(item));
		}
	}

	// calculate alcohol from recipe
	public int calcAlcohol() {
		if (quality == 0) {
			// Give bad potions some alc
			int badAlc = 0;
			if (distillRuns > 1) {
				badAlc = distillRuns;
			}
			if (ageTime > 10) {
				badAlc += 5;
			} else if (ageTime > 2) {
				badAlc += 3;
			}
			if (currentRecipe != null) {
				return badAlc;
			} else {
				return badAlc / 2;
			}
		}

		if (currentRecipe != null) {
			int alc = currentRecipe.alcohol;
			if (currentRecipe.needsDistilling()) {
				if (distillRuns == 0) {
					return 0;
				}
				// bad quality can decrease alc by up to 40%
				alc *= 1 - ((float) (10 - quality) * 0.04);
				// distillable Potions should have half alc after one and full alc after all needed distills
				alc /= 2;
				alc *= 1.0F + ((float) distillRuns / currentRecipe.distillRuns) ;
			} else {
				// quality decides 10% - 100%
				alc *= ((float) quality / 10.0);
			}
			if (alc > 0) {
				return alc;
			}
		}
		return 0;
	}

	// calculating quality
	public int calcQuality() {
		// calculate quality from all of the factors
		float quality = ingredients.getIngredientQuality(currentRecipe) + ingredients.getCookingQuality(currentRecipe, distillRuns > 0);
		if (currentRecipe.needsToAge() || ageTime > 0.5) {
			quality += ingredients.getWoodQuality(currentRecipe, wood) + ingredients.getAgeQuality(currentRecipe, ageTime);
			quality /= 4;
		} else {
			quality /= 2;
		}
		return Math.round(quality);
	}

	public int getQuality() {
		return quality;
	}

	public boolean isSplashable() {
		return currentRecipe.splashable;
	}

	public boolean canDistill() {
		if (currentRecipe != null) {
			return currentRecipe.distillRuns > distillRuns;
		} else if (distillRuns >= 6) {
			return false;
		}
		return true;
	}

	// return special effect
	public ImmutableList<BrewEffect> getEffects() {
		if (currentRecipe != null && quality > 0) {
			return currentRecipe.effects;
		}
		return null;
	}

	// Set unlabeled to true to hide the numbers in Lore
	public void unLabel(ItemStack item) {
		PotionMeta meta = (PotionMeta) item.getItemMeta();
		if (meta.hasLore()) {
			if (distillRuns > 0) {
				Lore.addOrReplaceLore(meta, BreweryPlugin.instance.color("&7"), BreweryPlugin.instance.languageReader.get("Brew_Distilled"));
			}
			if (ageTime >= 1) {
				Lore.addOrReplaceLore(meta, BreweryPlugin.instance.color("&7"), BreweryPlugin.instance.languageReader.get("Brew_BarrelRiped"));
			}
			item.setItemMeta(meta);
		}
		unlabeled = true;
	}

	public boolean isPersistent() {
		return persistent;
	}

	// Make a potion persistent to not delete it when drinking it
	public void makePersistent() {
		persistent = true;
	}

	// Remove the Persistence Flag from a brew, so it will be normally deleted when drinking it
	public void removePersistence() {
		persistent = false;
	}

	public boolean isStatic() {
		return immutable;
	}

	// Set the Static flag, so potion is unchangeable
	public void setStatic(boolean stat, ItemStack potion) {
		this.immutable = stat;
		if (currentRecipe != null && canDistill()) {
			if (stat) {
				potion.setDurability(PotionColor.valueOf(currentRecipe.getColor()).getColorId(false));
			} else {
				potion.setDurability(PotionColor.valueOf(currentRecipe.getColor()).getColorId(true));
			}
		}
	}

	// Distilling section ---------------

	// distill custom potion in given slot
	public void distillSlot(ItemStack slotItem, PotionMeta potionMeta) {
		if (immutable) {
			return;
		}

		distillRuns += 1;
		BrewRecipe recipe = ingredients.getdistillRecipe(wood, ageTime);
		if (recipe != null) {
			// distillRuns will have an effect on the amount of alcohol, not the quality
			currentRecipe = recipe;
			quality = calcQuality();

			Lore.addOrReplaceEffects(potionMeta, getEffects(), quality);
			potionMeta.setDisplayName(BreweryPlugin.instance.color("&f" + recipe.getName(quality)));
			slotItem.setDurability(PotionColor.valueOf(recipe.getColor()).getColorId(canDistill()));
		} else {
			quality = 0;
			Lore.removeEffects(potionMeta);
			potionMeta.setDisplayName(BreweryPlugin.instance.color("&f" + BreweryPlugin.instance.languageReader.get("Brew_DistillUndefined")));
			slotItem.setDurability(PotionColor.GREY.getColorId(canDistill()));
		}

		// Distill Lore
		if (currentRecipe != null) {
			if (BreweryPlugin.colorInBrewer != Lore.hasColorLore(potionMeta)) {
				Lore.convertLore(this, potionMeta, BreweryPlugin.colorInBrewer);
			}
		}
		String prefix = BreweryPlugin.instance.color("&7");
		if (BreweryPlugin.colorInBrewer && currentRecipe != null) {
			prefix = Lore.getQualityColor(ingredients.getDistillQuality(recipe, distillRuns));
		}
		Lore.updateDistillLore(this, prefix, potionMeta);

		slotItem.setItemMeta(potionMeta);
	}

	// Ageing Section ------------------

	public void age(ItemStack item, float time, byte woodType) {
		if (immutable) {
			return;
		}

		PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
		ageTime += time;
		
		// if younger than half a day, it shouldnt get aged form
		if (ageTime > 0.5) {
			if (wood == 0) {
				wood = woodType;
			} else {
				if (wood != woodType) {
					woodShift(time, woodType);
				}
			}
			BrewRecipe recipe = ingredients.getAgeRecipe(wood, ageTime, distillRuns > 0);
			if (recipe != null) {
				currentRecipe = recipe;
				quality = calcQuality();

				Lore.addOrReplaceEffects(potionMeta, getEffects(), quality);
				potionMeta.setDisplayName(BreweryPlugin.instance.color("&f" + recipe.getName(quality)));
				item.setDurability(PotionColor.valueOf(recipe.getColor()).getColorId(canDistill()));
			} else {
				quality = 0;
				Lore.removeEffects(potionMeta);
				potionMeta.setDisplayName(BreweryPlugin.instance.color("&f" + BreweryPlugin.instance.languageReader.get("Brew_BadPotion")));
				item.setDurability(PotionColor.GREY.getColorId(canDistill()));
			}
		}

		// Lore
		if (currentRecipe != null) {
			if (BreweryPlugin.colorInBarrels != Lore.hasColorLore(potionMeta)) {
				Lore.convertLore(this, potionMeta, BreweryPlugin.colorInBarrels);
			}
		}
		if (ageTime >= 1) {
			String prefix = BreweryPlugin.instance.color("&7");
			if (BreweryPlugin.colorInBarrels && currentRecipe != null) {
				prefix = Lore.getQualityColor(ingredients.getAgeQuality(currentRecipe, ageTime));
			}
			Lore.updateAgeLore(this, prefix, potionMeta);
		}
		if (ageTime > 0.5) {
			if (BreweryPlugin.colorInBarrels && !unlabeled && currentRecipe != null) {
				Lore.updateWoodLore(this, potionMeta);
			}
		}
		item.setItemMeta(potionMeta);
	}

	// Slowly shift the wood of the Brew to the new Type
	public void woodShift(float time, byte to) {
		byte factor = 1;
		if (ageTime > 5) {
			factor = 2;
		} else if (ageTime > 10) {
			factor = 2;
			factor += Math.round(ageTime / 10);
		}
		if (wood > to) {
			wood -= time / factor;
			if (wood < to) {
				wood = to;
			}
		} else {
			wood += time / factor;
			if (wood > to) {
				wood = to;
			}
		}
	}

	// Saves all data
	public static void save(ConfigurationSection config) {
		for (Map.Entry<Integer, Brew> entry : potions.entrySet()) {
			int uid = entry.getKey();
			Brew brew = entry.getValue();
			ConfigurationSection idConfig = config.createSection("" + uid);
			// not saving unneccessary data
			if (brew.quality != 0) {
				idConfig.set("quality", brew.quality);
			}
			if (brew.distillRuns != 0) {
				idConfig.set("distillRuns", brew.distillRuns);
			}
			if (brew.ageTime != 0) {
				idConfig.set("ageTime", brew.ageTime);
			}
			if (brew.wood != -1) {
				idConfig.set("wood", brew.wood);
			}
			if (brew.currentRecipe != null) {
				idConfig.set("recipe", brew.currentRecipe.getName(5));
			}
			if (brew.unlabeled) {
				idConfig.set("unlabeled", true);
			}
			if (brew.persistent) {
				idConfig.set("persist", true);
			}
			if (brew.immutable) {
				idConfig.set("stat", true);
			}
			// save the ingredients
			idConfig.set("ingId", brew.ingredients.save(config.getParent()));
		}
	}

}