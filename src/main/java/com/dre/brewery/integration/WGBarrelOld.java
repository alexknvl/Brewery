package com.dre.brewery.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.dre.brewery.BreweryPlugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;


public class WGBarrelOld implements WGBarrel {

	private Method allows;
	private Method canBuild;

	public WGBarrelOld() {
		try {
			allows = ApplicableRegionSet.class.getMethod("allows", StateFlag.class, LocalPlayer.class);
			canBuild = ApplicableRegionSet.class.getMethod("canBuild", LocalPlayer.class);
		} catch (NoSuchMethodException e) {
			BreweryPlugin.instance.errorLog("Failed to Hook WorldGuard for Barrel Open Permissions! Opening Barrels will NOT work!");
			BreweryPlugin.instance.errorLog("Brewery was tested with version 5.8 to 6.0 of WorldGuard!");
			BreweryPlugin.instance.errorLog("Disable the WorldGuard support in the config and do /brew reload");
			e.printStackTrace();
		}
	}

	public boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;
		if (!wg.getGlobalRegionManager().hasBypass(player, player.getWorld())) {

			Object region = wg.getRegionManager(player.getWorld()).getApplicableRegions(spigot.getLocation());

			if (region != null) {
				LocalPlayer localPlayer = wg.wrapPlayer(player);
				try {

					if (!(Boolean) allows.invoke(region, DefaultFlag.CHEST_ACCESS, localPlayer)) {
						if (!(Boolean) canBuild.invoke(region, localPlayer)) {
							BreweryPlugin.instance.msg(player, BreweryPlugin.instance.languageReader.get("Error_NoBarrelAccess"));
							return false;
						}
					}

				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return false;
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return true;
	}
}
