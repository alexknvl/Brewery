package com.dre.brewery.listeners;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.PotionColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import com.dre.brewery.integration.LogBlockBarrel;

public class InventoryListener implements Listener {

	private int getCustomPotionID(ItemStack item) {
		if (item == null) return 0;
		if (item.getType() != Material.POTION) return 0;
		if (!item.hasItemMeta()) return 0;
		return Brew.getUID(item);
	}

	private boolean distill(BrewerInventory brewerContents) {
		boolean success = false;

		for (int slot = 0; slot < 3; slot++) {
			ItemStack item = brewerContents.getItem(slot);
			int id = getCustomPotionID(item);
			if (id < -1) {
				Brew brew = Brew.get(id);
				if (brew == null) continue;

				if (brew.canDistill()) {
					brew.distillSlot(item, (PotionMeta) item.getItemMeta());
					success = true;
				}
			}
		}

		return success;
	}

	private boolean addGunpowder(BrewerInventory brewerContents) {
		boolean success = false;

		for (int slot = 0; slot < 3; slot++) {
			ItemStack item = brewerContents.getItem(slot);
			int id = getCustomPotionID(item);
			if (id < -1) {
				Brew brew = Brew.get(id);
				if (brew == null) continue;

				if (brew.isSplashable()) {
					item.setDurability(PotionColor.toSplashPotion(item.getDurability()));
					success = true;
				}
			}
		}

		return success;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBrew(BrewEvent event) {
		BrewerInventory brewerContents = event.getContents();

		// Check if there are any custom potions.
		boolean customBrewing = false;
		for (int slot = 0; slot < 3; slot++) {
			int id = getCustomPotionID(brewerContents.getItem(slot));
			if (id < -1) customBrewing = true;
		}

		// If there are no custom potions, use the default event handler.
		if (customBrewing) {
			Material ingredient = brewerContents.getIngredient().getType();

			if (ingredient == Material.SULPHUR) {
				if (addGunpowder(brewerContents))
					brewerContents.setIngredient(null);
			} else if (ingredient == Material.GLOWSTONE_DUST) {
				distill(brewerContents);
			}

			event.setCancelled(true);
		}

	}

	// convert to non colored Lore when taking out of Barrel/Brewer
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getType() == InventoryType.BREWING) {
			if (event.getSlot() > 2) {
				return;
			}
		} else if (!(event.getInventory().getHolder() instanceof Barrel)) {
			return;
		}

		ItemStack item = event.getCurrentItem();
		if (item != null) {
			if (item.getType() == Material.POTION) {
				if (item.hasItemMeta()) {
					PotionMeta meta = (PotionMeta) item.getItemMeta();
					Brew brew = Brew.get(meta);
					if (brew != null) {
						if (Brew.hasColorLore(meta)) {
							Brew.convertLore(brew, meta, false);
							item.setItemMeta(meta);
						}
					}
				}
			}
		}
	}
	
	// block the pickup of items where getPickupDelay is > 1000 (puke)
	@EventHandler(ignoreCancelled = true)
	public void onInventoryPickupItem(InventoryPickupItemEvent event){
		if (event.getItem().getPickupDelay() > 1000) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (BreweryPlugin.instance.useLB) {
			if (event.getInventory().getHolder() instanceof Barrel) {
				try {
					LogBlockBarrel.closeBarrel(event.getPlayer(), event.getInventory());
				} catch (Exception e) {
					BreweryPlugin.instance.errorLog("Failed to Log Barrel to LogBlock!");
					BreweryPlugin.instance.errorLog("Brewery was tested with version 1.80 of LogBlock!");
					e.printStackTrace();
				}
			}
		}
	}
}
