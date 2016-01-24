package com.dre.brewery.filedata;


import java.io.File;

import com.dre.brewery.*;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.dre.brewery.PlayerWrapper;

public class DataSave extends BukkitRunnable {

	public static int lastBackup = 0;
	public static int lastSave = 1;
	public static int autosave = 3;
	final public static String dataVersion = "1.1";
	public static DataSave running;

	public ReadOldData read;
	private long time;
	public boolean collected = false;

	// Not Thread-Safe! Needs to be run in main thread but uses async Read/Write
	public DataSave(ReadOldData read) {
		this.read = read;
		time = System.currentTimeMillis();
	}


	@Override
	public void run() {
		FileConfiguration oldData;
		if (read != null) {
			if (!read.done) {
				// Wait for async thread to load old data
				if (System.currentTimeMillis() - time > 30000) {
					BreweryPlugin.instance.errorLog("Old Data took too long to load!");
					cancel();
					return;
				}
				return;
			}
			oldData = read.getData();
		} else {
			oldData = new YamlConfiguration();
		}
		try {
			cancel();
		} catch (IllegalStateException e) {
		}

		FileConfiguration configFile = new YamlConfiguration();

		if (!Brew.potions.isEmpty()) {
			Brew.save(configFile.createSection("Brew"));
		}

		if (!CauldronWrapper.bcauldrons.isEmpty() || oldData.contains("BCauldron")) {
			CauldronWrapper.save(configFile.createSection("BCauldron"), oldData.getConfigurationSection("BCauldron"));
		}

		if (!Barrel.barrels.isEmpty() || oldData.contains("Barrel")) {
			Barrel.save(configFile.createSection("Barrel"), oldData.getConfigurationSection("Barrel"));
		}

		if (!PlayerWrapper.isEmpty()) {
			PlayerWrapper.save(configFile.createSection("Player"));
		}

		if (!Wakeup.wakeups.isEmpty() || oldData.contains("Wakeup")) {
			Wakeup.save(configFile.createSection("Wakeup"), oldData.getConfigurationSection("Wakeup"));
		}

		saveWorldNames(configFile, oldData.getConfigurationSection("Worlds"));
		configFile.set("Version", dataVersion);

		collected = true;
		if (BreweryPlugin.instance.isEnabled()) {
			BreweryPlugin.instance.getServer().getScheduler().runTaskAsynchronously(BreweryPlugin.instance, new WriteData(configFile));
		} else {
			new WriteData(configFile).run();
		}
	}

	// Finish the collection of data immediately
	public void now() {
		if (!read.done) {
			read.cancel();
			read.run();
		}
		if (!collected) {
			cancel();
			run();
		}
	}



	// Save all data. Takes a boolean whether all data should be collected in instantly
	public static void save(boolean collectInstant) {
		long time = System.nanoTime();
		if (running != null) {
			BreweryPlugin.instance.log("Another Save was started while a Save was in Progress");
			if (collectInstant) {
				running.now();
			}
			return;
		}
		File datafile = new File(BreweryPlugin.instance.getDataFolder(), "data.yml");

		if (datafile.exists()) {
			ReadOldData read = new ReadOldData();
			if (collectInstant) {
				read.run();
				running = new DataSave(read);
				running.run();
			} else {
				read.runTaskAsynchronously(BreweryPlugin.instance);
				running = new DataSave(read);
				running.runTaskTimer(BreweryPlugin.instance, 1, 2);
			}
		} else {
			running = new DataSave(null);
			running.run();
		}
		BreweryPlugin.instance.debugLog("saving: " + ((System.nanoTime() - time) / 1000000.0) + "ms");
	}

	public static void autoSave() {
		if (lastSave >= autosave) {
			save(false);// save all data
		} else {
			lastSave++;
		}
	}

	public static void saveWorldNames(FileConfiguration root, ConfigurationSection old) {
		if (old != null) {
			root.set("Worlds", old);
		}
		for (World world : BreweryPlugin.instance.getServer().getWorlds()) {
			String worldName = world.getName();
			if (worldName.startsWith("DXL_")) {
				worldName = BreweryPlugin.instance.getDxlName(worldName);
				root.set("Worlds." + worldName, 0);
			} else {
				worldName = world.getUID().toString();
				root.set("Worlds." + worldName, world.getName());
			}
		}
	}
}
