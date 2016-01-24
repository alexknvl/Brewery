package com.dre.brewery.integration;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.dre.brewery.BreweryPlugin;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.Claim;

public class GriefPreventionBarrel {
	public static boolean checkAccess(Player player, Block sign) {

		GriefPrevention gp = GriefPrevention.instance;

		if (!gp.claimsEnabledForWorld(player.getWorld()))
			return true;

		PlayerData playerData = gp.dataStore.getPlayerData(player.getUniqueId());

		if (playerData.ignoreClaims)
			return true;

		if (gp.config_claims_preventTheft) {
			// block container use during pvp combat
			if (playerData.inPvpCombat()) {
				BreweryPlugin.instance.msg(player, BreweryPlugin.instance.languageReader.get("Error_NoBarrelAccess"));
				return false;
			}

			// check permissions for the claim the Barrel is in
			Claim claim = gp.dataStore.getClaimAt(sign.getLocation(), false, playerData.lastClaim);
			if (claim != null) {
				playerData.lastClaim = claim;
				String noContainersReason = claim.allowContainers(player);
				if(noContainersReason != null)
				{
					BreweryPlugin.instance.msg(player, BreweryPlugin.instance.languageReader.get("Error_NoBarrelAccess"));
					return false;
				}
			}

			// drop any pvp protection, as the player opens a barrel
			if (playerData.pvpImmune) {
				playerData.pvpImmune = false;
				// GriefPrevention.sendMessage(player, ChatColor.GOLD, Messages.PvPImmunityEnd, "", "");
			}
			return true;

		} else return true;
	}
}
