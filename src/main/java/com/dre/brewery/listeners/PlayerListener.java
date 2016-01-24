package com.dre.brewery.listeners;

import com.dre.brewery.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.filedata.UpdateChecker;


public class PlayerListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();

		if (clickedBlock != null) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Player player = event.getPlayer();
				if (!player.isSneaking()) {
					Material type = clickedBlock.getType();

					// Interacting with a Cauldron
					if (type == Material.CAULDRON) {
						Material materialInHand = event.getMaterial();
						ItemStack item = event.getItem();

						if (materialInHand == Material.WATCH) {
							CauldronWrapper.printTime(player, clickedBlock);
							return;

							// fill a glass bottle with potion
						} else if (materialInHand == Material.GLASS_BOTTLE) {
							if (player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
								if (CauldronWrapper.fill(player, clickedBlock)) {
									event.setCancelled(true);
									if (player.hasPermission("brewery.cauldron.fill")) {
										if (item.getAmount() > 1) {
											item.setAmount(item.getAmount() - 1);
										} else {
											player.setItemInHand(new ItemStack(Material.AIR));
										}
									}
								}
							} else {
								event.setCancelled(true);
							}
							return;

							// reset cauldron when refilling to prevent
							// unlimited source of potions
						} else if (materialInHand == Material.WATER_BUCKET) {
							if (CauldronWrapper.getFillLevel(clickedBlock) != 0 && CauldronWrapper.getFillLevel(clickedBlock) < 2) {
								// will only remove when existing
								CauldronWrapper.remove(clickedBlock);
							}
							return;
						}

						// Check if fire alive below cauldron when adding ingredients
						Block down = clickedBlock.getRelative(BlockFace.DOWN);
						if (down.getType() == Material.FIRE || down.getType() == Material.STATIONARY_LAVA || down.getType() == Material.LAVA) {

							// add ingredient to cauldron that meet the previous conditions
							if (BreweryPlugin.possibleIngredients.contains(materialInHand)) {

								if (player.hasPermission("brewery.cauldron.insert")) {
									if (CauldronWrapper.ingredientAdd(clickedBlock, item)) {
										boolean isBucket = item.getType().equals(Material.WATER_BUCKET)
												|| item.getType().equals(Material.LAVA_BUCKET)
												|| item.getType().equals(Material.MILK_BUCKET);
										if (item.getAmount() > 1) {
											item.setAmount(item.getAmount() - 1);

											if (isBucket) {
												CauldronWrapper.giveItem(player, new ItemStack(Material.BUCKET));
											}
										} else {
											if (isBucket) {
												player.setItemInHand(new ItemStack(Material.BUCKET));
											} else {
												player.setItemInHand(new ItemStack(Material.AIR));
											}
										}
									}
								} else {
									BreweryPlugin.instance.msg(player, BreweryPlugin.instance.languageReader.get("Perms_NoCauldronInsert"));
								}
								event.setCancelled(true);
							} else {
								event.setCancelled(true);
							}
						}
						return;
					}

					// Access a Barrel
					Barrel barrel = null;
					if (type == Material.WOOD) {
						if (BreweryPlugin.openEverywhere) {
							barrel = Barrel.get(clickedBlock);
						}
					} else if (Barrel.isStairs(type)) {
						for (Barrel barrel2 : Barrel.barrels) {
							if (barrel2.hasStairsBlock(clickedBlock)) {
								if (BreweryPlugin.openEverywhere || !barrel2.isLarge()) {
									barrel = barrel2;
								}
								break;
							}
						}
					} else if (Barrel.isFence(type) || type == Material.SIGN_POST || type == Material.WALL_SIGN) {
						barrel = Barrel.getBySpigot(clickedBlock);
					}

					if (barrel != null) {
						event.setCancelled(true);

						if (!barrel.hasPermsOpen(player, event)) {
							return;
						}

						barrel.open(player);
					}
				}
			}
		}

		if (event.getAction() == Action.LEFT_CLICK_AIR) {
			if (!event.hasItem()) {
				if (Wakeup.checkPlayer != null) {
					if (event.getPlayer() == Wakeup.checkPlayer) {
						Wakeup.tpNext();
					}
				}
			}
		}

	}

	// player drinks a custom potion
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item != null) {
			if (item.getType() == Material.POTION) {
				Brew brew = Brew.get(item);
				if (brew != null) {
					PlayerWrapper.drink(brew, player);
					if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
						brew.remove(item);
					}
				}
			} else if (BreweryPlugin.drainItems.containsKey(item.getType())) {
				PlayerWrapper bplayer = PlayerWrapper.get(player);
				if (bplayer != null) {
					bplayer.drainByItem(player, item.getType());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSplash(org.bukkit.event.entity.PotionSplashEvent event) {
		ItemStack item = event.getPotion().getItem();

		if (item != null) {
			if (item.getType() == Material.POTION && item.hasItemMeta()) {
				Brew brew = Brew.get(item);
				if (brew != null) {
					for (LivingEntity entity: event.getAffectedEntities()) {
						if (entity instanceof Player) {
							PlayerWrapper.drink(brew, (Player) entity, event.getIntensity(entity));
						}
					}
				}
			}
		}
	}

	// Player has died! Decrease Drunkeness by 20
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		PlayerWrapper playerWrapper = PlayerWrapper.get(event.getPlayer());
		if (playerWrapper != null) {
			if (playerWrapper.getDrunkeness() > 20) {
				playerWrapper.setData(playerWrapper.getDrunkeness() - 20, 0);
			} else {
				PlayerWrapper.remove(event.getPlayer());
			}
		}
	}

	// player walks while drunk, push him around!
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (PlayerWrapper.hasPlayer(event.getPlayer())) {
			PlayerWrapper.playerMove(event);
		}
	}

	// player talks while drunk, but he cant speak very well
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		PlayerWrapper playerWrapper = PlayerWrapper.get(event.getPlayer());
		if (playerWrapper != null) {
			String message = event.getMessage();
			if (BreweryPlugin.logMessages) {
				BreweryPlugin.instance.log(BreweryPlugin.instance.languageReader.get("Player_TriedToSay", event.getPlayer().getName(), message));
			}
			event.setMessage(DrunkTextEffect.distortMessage(message, playerWrapper.getDrunkeness()));
		}
	}
	
	// player distortCommands while drunk, distort chat distortCommands
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
		PlayerWrapper playerWrapper = PlayerWrapper.get(event.getPlayer());
		if (playerWrapper != null) {
			if (!BreweryPlugin.distortCommands.isEmpty()) {
				String name = event.getPlayer().getName();
				String chat = event.getMessage();
				for (String command : BreweryPlugin.distortCommands) {
					if (command.length() + 1 < chat.length()) {
						if (Character.isSpaceChar(chat.charAt(command.length()))) {
							if (chat.toLowerCase().startsWith(command.toLowerCase())) {
								if (BreweryPlugin.logMessages) {
									BreweryPlugin.instance.log(BreweryPlugin.instance.languageReader.get("Player_TriedToSay", name, chat));
								}
								String message = chat.substring(command.length() + 1);
								message = DrunkTextEffect.distortMessage(message, playerWrapper.getDrunkeness());

								event.setMessage(chat.substring(0, command.length() + 1) + message);
								return;
							}
						}
					}
				}
			}
		}
	}

	// player joins while passed out
	@EventHandler()
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
			final Player player = event.getPlayer();
			PlayerWrapper bplayer = PlayerWrapper.get(player);
			if (bplayer != null) {
				if (player.hasPermission("brewery.bypass.logindeny")) {
					if (bplayer.getDrunkeness() > 100) {
						bplayer.setData(100, 0);
					}
					bplayer.join(player);
					return;
				}
				switch (bplayer.canJoin()) {
					case 0:
						bplayer.join(player);
						return;
					case 2:
						event.disallow(PlayerLoginEvent.Result.KICK_OTHER, BreweryPlugin.instance.languageReader.get("Player_LoginDeny"));
						return;
					case 3:
						event.disallow(PlayerLoginEvent.Result.KICK_OTHER, BreweryPlugin.instance.languageReader.get("Player_LoginDenyLong"));
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		UpdateChecker.notify(event.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		PlayerWrapper bplayer = PlayerWrapper.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		PlayerWrapper bplayer = PlayerWrapper.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
	}
}