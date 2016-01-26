package com.dre.brewery;

import com.google.common.collect.ImmutableList;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class Lore {
    // Converts to/from qualitycolored Lore
    public static void convertLore(Brew brew, PotionMeta meta, Boolean toQuality) {
        if (brew.currentRecipe == null) {
            return;
        }
        meta.setLore(null);
        int quality;
        String prefix = BreweryPlugin.instance.color("&7");
        String lore;

        // Ingredients
        if (toQuality && !brew.unlabeled) {
            quality = brew.ingredients.getIngredientQuality(brew.currentRecipe);
            prefix = getQualityColor(quality);
            lore = BreweryPlugin.instance.languageReader.get("Brew_Ingredients");
            addOrReplaceLore(meta, prefix, lore);
        }

        // Cooking
        if (toQuality && !brew.unlabeled) {
            if (brew.distillRuns > 0 == brew.currentRecipe.needsDistilling()) {
                quality = brew.ingredients.getCookingQuality(brew.currentRecipe, brew.distillRuns > 0);
                prefix = getQualityColor(quality) + brew.ingredients.getCookedTime() + " " + BreweryPlugin.instance.languageReader.get("Brew_minute");
                if (brew.ingredients.getCookedTime() > 1) {
                    prefix = prefix + BreweryPlugin.instance.languageReader.get("Brew_MinutePluralPostfix");
                }
                lore = " " + BreweryPlugin.instance.languageReader.get("Brew_fermented");
                addOrReplaceLore(meta, prefix, lore);
            }
        }

        // Distilling
        if (brew.distillRuns > 0) {
            if (toQuality) {
                quality = brew.ingredients.getDistillQuality(brew.currentRecipe, brew.distillRuns);
                prefix = getQualityColor(quality);
            }
            updateDistillLore(brew, prefix, meta);
        }

        // Ageing
        if (brew.ageTime >= 1) {
            if (toQuality) {
                quality = brew.ingredients.getAgeQuality(brew.currentRecipe, brew.ageTime);
                prefix = getQualityColor(quality);
            }
            updateAgeLore(brew, prefix, meta);
        }

        // WoodType
        if (toQuality && !brew.unlabeled) {
            if (brew.ageTime > 0.5) {
                updateWoodLore(brew, meta);
            }
        }
    }

    // sets the DistillLore. Prefix is the color to be used
    public static void updateDistillLore(Brew brew, String prefix, PotionMeta meta) {
        if (!brew.unlabeled) {
            if (brew.distillRuns > 1) {
                prefix = prefix + brew.distillRuns + BreweryPlugin.instance.languageReader.get("Brew_-times") + " ";
            }
        }
        addOrReplaceLore(meta, prefix, BreweryPlugin.instance.languageReader.get("Brew_Distilled"));
    }

    // sets the AgeLore. Prefix is the color to be used
    public static void updateAgeLore(Brew brew, String prefix, PotionMeta meta) {
        if (!brew.unlabeled) {
            if (brew.ageTime >= 1 && brew.ageTime < 2) {
                prefix = prefix + BreweryPlugin.instance.languageReader.get("Brew_OneYear") + " ";
            } else if (brew.ageTime < 201) {
                prefix = prefix + (int) Math.floor(brew.ageTime) + " " + BreweryPlugin.instance.languageReader.get("Brew_Years") + " ";
            } else {
                prefix = prefix + BreweryPlugin.instance.languageReader.get("Brew_HundredsOfYears") + " ";
            }
        }
        addOrReplaceLore(meta, prefix, BreweryPlugin.instance.languageReader.get("Brew_BarrelRiped"));
    }

    // updates/sets the color on WoodLore
    public static void updateWoodLore(Brew brew, PotionMeta meta) {
        if (brew.currentRecipe.wood > 0) {
            int quality = brew.ingredients.getWoodQuality(brew.currentRecipe, brew.wood);
            addOrReplaceLore(meta, getQualityColor(quality), BreweryPlugin.instance.languageReader.get("Brew_Woodtype"));
        } else {
            if (meta.hasLore()) {
                List<String> existingLore = meta.getLore();
                int index = indexOfSubstring(existingLore, BreweryPlugin.instance.languageReader.get("Brew_Woodtype"));
                if (index > -1) {
                    existingLore.remove(index);
                    meta.setLore(existingLore);
                }
            }
        }
    }

    // Adds or replaces a line of Lore. Searches for Substring lore and replaces it
    public static void addOrReplaceLore(PotionMeta meta, String prefix, String lore) {
        if (meta.hasLore()) {
            List<String> existingLore = meta.getLore();
            int index = indexOfSubstring(existingLore, lore);
            if (index > -1) {
                existingLore.set(index, prefix + lore);
            } else {
                existingLore.add(prefix + lore);
            }
            meta.setLore(existingLore);
            return;
        }
        List<String> newLore = new ArrayList<String>();
        newLore.add("");
        newLore.add(prefix + lore);
        meta.setLore(newLore);
    }

    // True if the PotionMeta has colored Lore
    public  static Boolean hasColorLore(PotionMeta meta) {
        return meta.hasLore() && !meta.getLore().get(1).startsWith(BreweryPlugin.instance.color("&7"));
    }

    // gets the Color that represents a quality in Lore
    public static String getQualityColor(int quality) {
        String color;
        if (quality > 8) {
            color = "&a";
        } else if (quality > 6) {
            color = "&e";
        } else if (quality > 4) {
            color = "&6";
        } else if (quality > 2) {
            color = "&c";
        } else {
            color = "&4";
        }
        return BreweryPlugin.instance.color(color);
    }

    // Adds the Effect names to the Items description
    public static void addOrReplaceEffects(PotionMeta meta, ImmutableList<BrewEffect> effects, int quality) {
        if (effects != null) {
            for (BrewEffect effect : effects) {
                if (!effect.hidden) {
                    BrewEffect.updateMeta(effect, meta, quality);
                }
            }
        }
    }

    // Removes all effects except regeneration which stores data
    public static void removeEffects(PotionMeta meta) {
        if (meta.hasCustomEffects()) {
            for (PotionEffect effect : meta.getCustomEffects()) {
                PotionEffectType type = effect.getType();
                if (!type.equals(PotionEffectType.REGENERATION)) {
                    meta.removeCustomEffect(type);
                }
            }
        }
    }

    // Returns the Index of a String from the list that contains this substring
    private static int indexOfSubstring(List<String> list, String substring) {
        for (int index = 0; index < list.size(); index++) {
            String string = list.get(index);
            if (string.contains(substring)) {
                return index;
            }
        }
        return -1;
    }
}
