package com.dre.brewery.listeners;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.CauldronWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.World;

import com.dre.brewery.Barrel;
import com.dre.brewery.filedata.DataSave;

public class WorldListener implements Listener {

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		World world = event.getWorld();

		if (world.getName().startsWith("DXL_")) {
			BreweryPlugin.instance.loadWorldData(BreweryPlugin.instance.getDxlName(world.getName()), world);
		} else {
			BreweryPlugin.instance.loadWorldData(world.getUID().toString(), world);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		DataSave.save(true);
		Barrel.onUnload(event.getWorld().getName());
		CauldronWrapper.onUnload(event.getWorld().getName());
	}

}
