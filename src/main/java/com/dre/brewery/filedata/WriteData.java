package com.dre.brewery.filedata;


import java.io.File;
import java.io.IOException;

import com.dre.brewery.BreweryPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class WriteData implements Runnable {

	private FileConfiguration data;

	public WriteData(FileConfiguration data) {
		this.data = data;
	}

	@Override
	public void run() {
		File datafile = new File(BreweryPlugin.instance.getDataFolder(), "data.yml");

		try {
			data.save(datafile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		DataSave.lastSave = 1;
		DataSave.running = null;
	}
}
