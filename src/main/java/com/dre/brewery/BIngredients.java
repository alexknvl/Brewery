package com.dre.brewery;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.PotionMeta;

public class BIngredients {
	private static int lastId = 0;

	private int id;
	private ArrayList<ItemStack> ingredients = new ArrayList<ItemStack>();
	private Map<Material, Integer> materials = new HashMap<Material, Integer>(); // Merged List Of ingredients that doesnt consider Durability
	private int cookedTime;

	// Represents ingredients in Cauldron, Brew
	// Init a new BIngredients
	public BIngredients() {
		this.id = lastId;
		lastId++;
	}

	// Load from File
	public BIngredients(ArrayList<ItemStack> ingredients, int cookedTime) {
		this.ingredients = ingredients;
		this.cookedTime = cookedTime;
		this.id = lastId;
		lastId++;

		for (ItemStack item : ingredients) {
			addMaterial(item);
		}
	}

	// Add an ingredient to this
	public void add(ItemStack ingredient) {
		addMaterial(ingredient);
		for (ItemStack item : ingredients) {
			if (item.isSimilar(ingredient)) {
				item.setAmount(item.getAmount() + ingredient.getAmount());
				return;
			}
		}
		ingredients.add(ingredient);
	}

	private void addMaterial(ItemStack ingredient) {
		if (materials.containsKey(ingredient.getType())) {
			int newAmount = materials.get(ingredient.getType()) + ingredient.getAmount();
			materials.put(ingredient.getType(), newAmount);
		} else {
			materials.put(ingredient.getType(), ingredient.getAmount());
		}
	}

	// returns an Potion item with cooked ingredients
	public ItemStack cook(int state) {

		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		// cookedTime is always time in minutes, state may differ with number of ticks
		cookedTime = state;
		String cookedName = null;
		BrewRecipe cookRecipe = getCookRecipe();

		int uid = Brew.generateUID();

		if (cookRecipe != null) {
			// Potion is best with cooking only
			int quality = (int) Math.round((getIngredientQuality(cookRecipe) + getCookingQuality(cookRecipe, false)) / 2.0);
			BreweryPlugin.instance.debugLog("cooked potion has Quality: " + quality);
			Brew brew = new Brew(quality, cookRecipe, this);
			Brew.potions.put(uid, brew);
			Lore.addOrReplaceEffects(potionMeta, brew.getEffects(), brew.getQuality());

			cookedName = cookRecipe.getName(quality);
			potion.setDurability(PotionColor.valueOf(cookRecipe.getColor()).getColorId(false));

		} else {
			// new base potion
			Brew.potions.put(uid, new Brew(this));

			if (state <= 1) {
				cookedName = BreweryPlugin.instance.languageReader.get("Brew_ThickBrew");
				potion.setDurability(PotionColor.BLUE.getColorId(false));
			} else {
				for (Material ingredient : materials.keySet()) {
					if (BreweryPlugin.cookedNames.containsKey(ingredient)) {
						// if more than half of the ingredients is of one kind
						if (materials.get(ingredient) > (getIngredientsCount() / 2)) {
							cookedName = BreweryPlugin.cookedNames.get(ingredient);
							potion.setDurability(PotionColor.CYAN.getColorId(true));
						}
					}
				}
			}
		}
		if (cookedName == null) {
			// if no name could be found
			cookedName = BreweryPlugin.instance.languageReader.get("Brew_Undefined");
			potion.setDurability(PotionColor.CYAN.getColorId(true));
		}

		potionMeta.setDisplayName(BreweryPlugin.instance.color("&f" + cookedName));
		// This effect stores the UID in its Duration
		potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);
		potion.setItemMeta(potionMeta);

		return potion;
	}

	// returns amount of ingredients
	private int getIngredientsCount() {
		int count = 0;
		for (ItemStack item : ingredients) {
			count += item.getAmount();
		}
		return count;
	}

	/*public Map<Material, Integer> getIngredients() {
		return ingredients;
	}*/

	public int getCookedTime() {
		return cookedTime;
	}

	// best recipe for current state of potion, STILL not always returns the
	// correct one...
	public BrewRecipe getBestRecipe(float wood, float time, boolean distilled) {
		float quality = 0;
		int ingredientQuality;
		int cookingQuality;
		int woodQuality;
		int ageQuality;
		BrewRecipe bestRecipe = null;
		for (BrewRecipe recipe : BreweryPlugin.recipes) {
			ingredientQuality = getIngredientQuality(recipe);
			cookingQuality = getCookingQuality(recipe, distilled);

			if (ingredientQuality > -1 && cookingQuality > -1) {
				if (recipe.needsToAge() || time > 0.5) {
					// needs riping in barrel
					ageQuality = getAgeQuality(recipe, time);
					woodQuality = getWoodQuality(recipe, wood);
					BreweryPlugin.instance.debugLog("Ingredient Quality: " + ingredientQuality + " Cooking Quality: " + cookingQuality +
						" Wood Quality: " + woodQuality + " age Quality: " + ageQuality + " for " + recipe.getName(5));

					// is this recipe better than the previous best?
					if ((((float) ingredientQuality + cookingQuality + woodQuality + ageQuality) / 4) > quality) {
						quality = ((float) ingredientQuality + cookingQuality + woodQuality + ageQuality) / 4;
						bestRecipe = recipe;
					}
				} else {
					BreweryPlugin.instance.debugLog("Ingredient Quality: " + ingredientQuality + " Cooking Quality: " + cookingQuality + " for " + recipe.getName(5));
					// calculate quality without age and barrel
					if ((((float) ingredientQuality + cookingQuality) / 2) > quality) {
						quality = ((float) ingredientQuality + cookingQuality) / 2;
						bestRecipe = recipe;
					}
				}
			}
		}
		if (bestRecipe != null) {
			BreweryPlugin.instance.debugLog("best recipe: " + bestRecipe.getName(5) + " has Quality= " + quality);
		}
		return bestRecipe;
	}

	// returns recipe that is cooking only and matches the ingredients and
	// cooking time
	public BrewRecipe getCookRecipe() {
		BrewRecipe bestRecipe = getBestRecipe(0, 0, false);

		// Check if best recipe is cooking only
		if (bestRecipe != null) {
			if (bestRecipe.isCookingOnly()) {
				return bestRecipe;
			}
		}
		return null;
	}

	// returns the currently best matching recipe for distilling for the
	// ingredients and cooking time
	public BrewRecipe getdistillRecipe(float wood, float time) {
		BrewRecipe bestRecipe = getBestRecipe(wood, time, true);

		// Check if best recipe needs to be destilled
		if (bestRecipe != null) {
			if (bestRecipe.needsDistilling()) {
				return bestRecipe;
			}
		}
		return null;
	}

	// returns currently best matching recipe for ingredients, cooking- and
	// ageingtime
	public BrewRecipe getAgeRecipe(float wood, float time, boolean distilled) {
		BrewRecipe bestRecipe = getBestRecipe(wood, time, distilled);

		if (bestRecipe != null) {
			if (bestRecipe.needsToAge()) {
				return bestRecipe;
			}
		}
		return null;
	}

	// returns the quality of the ingredients conditioning given recipe, -1 if
	// no recipe is near them
	public int getIngredientQuality(BrewRecipe recipe) {
		float quality = 10;
		int count;
		int badStuff = 0;
		if (recipe.isMissingIngredients(ingredients)) {
			// when ingredients are not complete
			return -1;
		}
		ArrayList<Material> mergedChecked = new ArrayList<Material>();
		for (ItemStack ingredient : ingredients) {
			if (mergedChecked.contains(ingredient.getType())) {
				// This ingredient type was already checked as part of a merged material
				continue;
			}
			int amountInRecipe = recipe.amountOf(ingredient);
			// If we dont consider durability for this ingredient, check the merged material
			if (recipe.hasExactData(ingredient)) {
				count = ingredient.getAmount();
			} else {
				mergedChecked.add(ingredient.getType());
				count = materials.get(ingredient.getType());
			}
			if (amountInRecipe == 0) {
				// this ingredient doesnt belong into the recipe
				if (count > (getIngredientsCount() / 2)) {
					// when more than half of the ingredients dont fit into the
					// recipe
					return -1;
				}
				badStuff++;
				if (badStuff < ingredients.size()) {
					// when there are other ingredients
					quality -= count * (recipe.difficulty / 2);
					continue;
				} else {
					// ingredients dont fit at all
					return -1;
				}
			}
			// calculate the quality
			quality -= ((float) Math.abs(count - amountInRecipe) / recipe.allowedCountDiff(amountInRecipe)) * 10.0;
		}
		if (quality >= 0) {
			return Math.round(quality);
		}
		return -1;
	}

	// returns the quality regarding the cooking-time conditioning given Recipe
	public int getCookingQuality(BrewRecipe recipe, boolean distilled) {
		if (!recipe.needsDistilling() == distilled) {
			return -1;
		}
		int quality = 10 - (int) Math.round(((float) Math.abs(cookedTime - recipe.cookingTime) / recipe.allowedTimeDiff(recipe.cookingTime)) * 10.0);

		if (quality >= 0) {
			if (cookedTime <= 1) {
				return 0;
			}
			return quality;
		}
		return -1;
	}

	// returns pseudo quality of distilling. 0 if doesnt match the need of the recipes distilling
	public int getDistillQuality(BrewRecipe recipe, int distillRuns) {
		if (recipe.needsDistilling() != distillRuns > 0) {
			return 0;
		}
		return 10 - Math.abs(recipe.distillRuns - distillRuns);
	}

	// returns the quality regarding the barrel wood conditioning given Recipe
	public int getWoodQuality(BrewRecipe recipe, float wood) {
		if (recipe.wood == 0) {
			// type of wood doesnt matter
			return 10;
		}
		int quality = 10 - Math.round(recipe.getWoodDiff(wood) * recipe.difficulty);

		if (quality > 0) {
			return quality;
		}
		return 0;
	}

	// returns the quality regarding the ageing time conditioning given Recipe
	public int getAgeQuality(BrewRecipe recipe, float time) {
		int quality = 10 - Math.round(Math.abs(time - (float) recipe.age) * ((float) recipe.difficulty / 2));

		if (quality > 0) {
			return quality;
		}
		return 0;
	}

	// Creates a copy ingredients
	public BIngredients clone() {
		BIngredients copy = new BIngredients();
		copy.ingredients.addAll(ingredients);
		copy.materials.putAll(materials);
		copy.cookedTime = cookedTime;
		return copy;
	}

	// saves data into main Ingredient section. Returns the save id
	public int save(ConfigurationSection config) {
		String path = "Ingredients." + id;
		if (cookedTime != 0) {
			config.set(path + ".cookedTime", cookedTime);
		}
		config.set(path + ".mats", serializeIngredients());
		return id;
	}

	//convert the ingredient Material to String
	public Map<String, Integer> serializeIngredients() {
		Map<String, Integer> mats = new HashMap<String, Integer>();
		for (ItemStack item : ingredients) {
			String mat = item.getType().name() + "," + item.getDurability();
			mats.put(mat, item.getAmount());
		}
		return mats;
	}

}