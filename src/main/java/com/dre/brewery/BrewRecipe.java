package com.dre.brewery;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

public class BrewRecipe {
	public final ImmutableList<String> names;
	public final ImmutableList<ItemStack> ingredients;// material and amount
	public final int cookingTime;// time to cook in cauldron
	public final int distillRuns;// runs through the brewer
	public final byte wood;// type of wood the barrel has to consist of
	public final int age;// time in minecraft days for the potions to age in barrels
	public final String color;// color of the destilled/finished potion
	public final int difficulty;// difficulty to brew the potion, how exact the instruction has to be followed
	public final int alcohol;// Alcohol in perfect potion
	public final boolean splashable;
	public final ImmutableList<BrewEffect> effects; // Special Effects when drinking

	public BrewRecipe(ImmutableList<String> names, ImmutableList<ItemStack> ingredients,
					  int cookingTime, int distillRuns, byte wood,
					  int age, String color, int difficulty, int alcohol, boolean splashable,
					  ImmutableList<BrewEffect> effects) {
		this.names = names;
		this.ingredients = ingredients;
		this.cookingTime = cookingTime;
		this.distillRuns = distillRuns;
		this.wood = wood;
		this.age = age;
		this.color = color;
		this.difficulty = difficulty;
		this.alcohol = alcohol;
		this.splashable = splashable;
		this.effects = effects;
	}

	public static BrewRecipe read(ConfigurationSection configSectionRecipes, String recipeId) {
		String nameList = configSectionRecipes.getString(recipeId + ".name");
		final ImmutableList<String> names;
		if (nameList != null) {
			String[] name = nameList.split("/");
			if (name.length > 2) {
				names = ImmutableList.copyOf(name);
			} else {
				names = ImmutableList.of(name[0]);
			}
		} else {
			return null;
		}

		List<String> ingredientsList = configSectionRecipes.getStringList(recipeId + ".ingredients");
		ArrayList<ItemStack> ingredients = new ArrayList<>();
		if (ingredientsList != null) {
			for (String item : ingredientsList) {
				String[] ingredParts = item.split("/");
				if (ingredParts.length == 2) {
					String[] matParts;
					if (ingredParts[0].contains(",")) {
						matParts = ingredParts[0].split(",");
					} else if (ingredParts[0].contains(":")) {
						matParts = ingredParts[0].split(":");
					} else if (ingredParts[0].contains(";")) {
						matParts = ingredParts[0].split(";");
					} else {
						matParts = ingredParts[0].split("\\.");
					}
					Material mat = Material.matchMaterial(matParts[0]);
					short durability = -1;
					if (matParts.length == 2) {
						durability = (short) BreweryPlugin.instance.parseInt(matParts[1]);
					}
					if (mat == null && BreweryPlugin.instance.hasVault) {
						try {
							net.milkbowl.vault.item.ItemInfo vaultItem = net.milkbowl.vault.item.Items.itemByString(matParts[0]);
							if (vaultItem != null) {
								mat = vaultItem.getType();
								if (durability == -1 && vaultItem.getSubTypeId() != 0) {
									durability = vaultItem.getSubTypeId();
								}
							}
						} catch (Exception e) {
							BreweryPlugin.instance.errorLog("Could not check vault for Item Name");
							e.printStackTrace();
						}
					}
					if (mat != null) {
						ItemStack stack = new ItemStack(mat, BreweryPlugin.instance.parseInt(ingredParts[1]), durability);
						ingredients.add(stack);
						BreweryPlugin.possibleIngredients.add(mat);
					} else {
						BreweryPlugin.instance.errorLog("Unknown Material: " + ingredParts[0]);
						return null;
					}
				} else {
					return null;
				}
			}
		}
		int cookingTime = configSectionRecipes.getInt(recipeId + ".cookingtime");
		int distillruns = configSectionRecipes.getInt(recipeId + ".distillruns");
		byte wood = (byte) configSectionRecipes.getInt(recipeId + ".wood");
		int age = configSectionRecipes.getInt(recipeId + ".age");
		String color = configSectionRecipes.getString(recipeId + ".color");
		int difficulty = configSectionRecipes.getInt(recipeId + ".difficulty");
		int alcohol = configSectionRecipes.getInt(recipeId + ".alcohol");
		boolean splashable = configSectionRecipes.getBoolean(recipeId + ".splashable", false);

		List<String> effectStringList = configSectionRecipes.getStringList(recipeId + ".effects");
		ArrayList<BrewEffect> effects = new ArrayList<>();
		if (effectStringList != null) {
			for (String effectString : effectStringList) {
				BrewEffect effect = BrewEffect.parse(effectString);
				if (effect != null && effect.isValid()) {
					effects.add(effect);
				} else {
					BreweryPlugin.instance.errorLog("Error adding Effect to Recipe: " + names.get(0));
				}
			}
		}

		return new BrewRecipe(names, ImmutableList.copyOf(ingredients), cookingTime, distillruns, wood,
				age, color, difficulty, alcohol, splashable, ImmutableList.copyOf(effects));
	}

	// allowed deviation to the recipes count of ingredients at the given difficulty
	public int allowedCountDiff(int count) {
		if (count < 8) {
			count = 8;
		}
		int allowedCountDiff = Math.round((float) ((11.0 - difficulty) * (count / 10.0)));

		if (allowedCountDiff == 0) {
			return 1;
		}
		return allowedCountDiff;
	}

	// allowed deviation to the recipes cooking-time at the given difficulty
	public int allowedTimeDiff(int time) {
		if (time < 8) {
			time = 8;
		}
		int allowedTimeDiff = Math.round((float) ((11.0 - difficulty) * (time / 10.0)));

		if (allowedTimeDiff == 0) {
			return 1;
		}
		return allowedTimeDiff;
	}

	// difference between given and recipe-wanted woodtype
	public float getWoodDiff(float wood) {
		return Math.abs(wood - this.wood);
	}

	public boolean isCookingOnly() {
		return age == 0 && distillRuns == 0;
	}

	public boolean needsDistilling() {
		return distillRuns != 0;
	}

	public boolean needsToAge() {
		return age != 0;
	}

	// true if given list misses an ingredient
	public boolean isMissingIngredients(List<ItemStack> list) {
		if (list.size() < ingredients.size()) {
			return true;
		}
		for (ItemStack ingredient : ingredients) {
			boolean matches = false;
			for (ItemStack used : list) {
				if (ingredientsMatch(used, ingredient)) {
					matches = true;
					break;
				}
			}
			if (!matches) {
				return true;
			}
		}
		return false;
	}

	// Returns true if this ingredient cares about durability
	public boolean hasExactData(ItemStack item) {
		for (ItemStack ingredient : ingredients) {
			if (ingredient.getType().equals(item.getType())) {
				return ingredient.getDurability() != -1;
			}
		}
		return true;
	}

	// Returns true if this item matches the item from a recipe
	public static boolean ingredientsMatch(ItemStack usedItem, ItemStack recipeItem) {
		if (!recipeItem.getType().equals(usedItem.getType())) {
			return false;
		}
		return recipeItem.getDurability() == -1 || recipeItem.getDurability() == usedItem.getDurability();
	}

	// Create a Potion from this Recipe with best values. Quality can be set, but will reset to 10 if put in a barrel
	public ItemStack create(int quality) {
		ItemStack potion = new ItemStack(Material.POTION);
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

		int uid = Brew.generateUID();

		ArrayList<ItemStack> list = new ArrayList<ItemStack>(ingredients.size());
		for (ItemStack item : ingredients) {
			if (item.getDurability() == -1) {
				list.add(new ItemStack(item.getType(), item.getAmount()));
			} else {
				list.add(item.clone());
			}
		}

		BIngredients bIngredients = new BIngredients(list, cookingTime);

		Brew brew = new Brew(uid, bIngredients, quality, distillRuns, (float) age, wood, getName(5), false, false, true);

		potion.setDurability(PotionColor.valueOf(getColor()).getColorId(false));
		potionMeta.setDisplayName(BreweryPlugin.instance.color("&f" + getName(quality)));
		// This effect stores the UID in its Duration
		potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);

		Lore.convertLore(brew, potionMeta, false);
		Lore.addOrReplaceEffects(potionMeta, effects, quality);

		potion.setItemMeta(potionMeta);
		return potion;
	}

	// true if name and ingredients are correct
	public boolean isValid() {
		return (ingredients != null && !ingredients.isEmpty());
	}


	// Getter

	// how many of a specific ingredient in the recipe
	public int amountOf(ItemStack item) {
		for (ItemStack ingredient : ingredients) {
			if (ingredientsMatch(item, ingredient)) {
				return ingredient.getAmount();
			}
		}
		return 0;
	}

	// name that fits the quality
	public String getName(int quality) {
		if (names.size() > 2) {
			if (quality <= 3) {
				return names.get(0);
			} else if (quality <= 7) {
				return names.get(1);
			} else {
				return names.get(2);
			}
		} else {
			return names.get(0);
		}
	}

	// If one of the quality names equalIgnoreCase given name
	public boolean hasName(String name) {
		for (String test : this.names) {
			if (test.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public String getColor() {
		if (color != null) {
			return color.toUpperCase();
		}
		return "BLUE";
	}

}