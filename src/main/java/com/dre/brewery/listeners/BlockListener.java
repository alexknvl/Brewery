package com.dre.brewery.listeners;

import com.dre.brewery.BreweryPlugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.BlockBreakEvent;

import com.dre.brewery.Barrel;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Words;

public class BlockListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		String[] lines = event.getLines();

		if (lines[0].equalsIgnoreCase(BreweryPlugin.instance.languageReader.get("Etc_Barrel"))) {
			Player player = event.getPlayer();
			if (!player.hasPermission("brewery.createbarrel.small") && !player.hasPermission("brewery.createbarrel.big")) {
				BreweryPlugin.instance.msg(player, BreweryPlugin.instance.languageReader.get("Perms_NoBarrelCreate"));
				return;
			}
			if (Barrel.create(event.getBlock(), player)) {
				BreweryPlugin.instance.msg(player, BreweryPlugin.instance.languageReader.get("Player_BarrelCreated"));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onSignChangeLow(SignChangeEvent event) {
		if (Words.doSigns) {
			BPlayer bPlayer = BPlayer.get(event.getPlayer());
			if (bPlayer != null) {
				int index = 0;
				for (String message : event.getLines()) {
					if (message.length() > 1) {
						message = Words.distortMessage(message, bPlayer.getDrunkeness());

						if (message.length() > 15) {
							message = message.substring(0, 14);
						}
						event.setLine(index, message);
					}
					index++;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!BreweryPlugin.instance.blockDestroy(event.getBlock(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		BreweryPlugin.instance.blockDestroy(event.getBlock(), null);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (event.isSticky()) {
			Block block = event.getRetractLocation().getBlock();

			if (Barrel.get(block) != null) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		for (Block block : event.getBlocks()) {
			if (Barrel.get(block) != null) {
				event.setCancelled(true);
				return;
			}
		}
	}
}
