package uk.org.whoami.authme;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import me.muizers.Notifications.Notification;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import uk.org.whoami.authme.api.API;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.cache.limbo.LimboCache;
import uk.org.whoami.authme.cache.limbo.LimboPlayer;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.events.AuthMeTeleportEvent;
import uk.org.whoami.authme.events.LoginEvent;
import uk.org.whoami.authme.events.RestoreInventoryEvent;
import uk.org.whoami.authme.events.SpawnTeleportEvent;
import uk.org.whoami.authme.listener.AuthMePlayerListener;
import uk.org.whoami.authme.security.PasswordSecurity;
import uk.org.whoami.authme.security.RandomString;
import uk.org.whoami.authme.settings.Messages;
import uk.org.whoami.authme.settings.PlayersLogs;
import uk.org.whoami.authme.settings.Settings;
import uk.org.whoami.authme.settings.Spawn;

public class Management {

	private Messages m = Messages.getInstance();
	private PlayersLogs pllog = PlayersLogs.getInstance();
	private Utils utils = Utils.getInstance();
	private FileCache playerCache = new FileCache();
	private DataSource database;
	public AuthMe plugin;
	public static RandomString rdm = new RandomString(Settings.captchaLength);
	public PluginManager pm;

	public Management(DataSource database, AuthMe plugin) {
		this.database = database;
		this.plugin = plugin;
		this.pm = plugin.getServer().getPluginManager();
	}

	public void performLogin(final Player player, final String password,
			final boolean passpartu) {
		final String name = player.getName().toLowerCase();

		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				String ip = player.getAddress().getAddress().getHostAddress();
				if (Settings.bungee) {
					if (plugin.realIp.containsKey(name))
						ip = plugin.realIp.get(name);
				}
				World world = player.getWorld();
				Location spawnLoc = world.getSpawnLocation();
				if (plugin.mv != null) {
					try {
						spawnLoc = plugin.mv.getMVWorldManager()
								.getMVWorld(world).getSpawnLocation();
					} catch (NullPointerException npe) {
					} catch (ClassCastException cce) {
					} catch (NoClassDefFoundError ncdfe) {
					}
				}
		        if (plugin.essentialsSpawn != null) {
		        	spawnLoc = plugin.essentialsSpawn;
		        }
				if (Spawn.getInstance().getLocation() != null)
					spawnLoc = Spawn.getInstance().getLocation();
				if (PlayerCache.getInstance().isAuthenticated(name)) {
					player.sendMessage(m._("logged_in"));
					return;
				}
				if (!database.isAuthAvailable(player.getName().toLowerCase())) {
					player.sendMessage(m._("user_unknown"));
					return;
				}
				PlayerAuth pAuth = database.getAuth(name);
				if (pAuth == null) {
					player.sendMessage(m._("user_unknown"));
					return;
				}
				if (!Settings.getMySQLColumnGroup.isEmpty()
						&& pAuth.getGroupId() == Settings.getNonActivatedGroup) {
					player.sendMessage(m._("vb_nonActiv"));
					return;
				}
				String hash = pAuth.getHash();
				String email = pAuth.getEmail();
				try {
					if (!passpartu) {
						if (Settings.useCaptcha) {
							if (!plugin.captcha.containsKey(name)) {
								plugin.captcha.put(name, 1);
							} else {
								int i = plugin.captcha.get(name) + 1;
								plugin.captcha.remove(name);
								plugin.captcha.put(name, i);
							}
							if (plugin.captcha.containsKey(name)
									&& plugin.captcha.get(name) > Settings.maxLoginTry) {
								player.sendMessage(m._("need_captcha"));
								plugin.cap.put(name, rdm.nextString());
								player.sendMessage("Type : /captcha "
										+ plugin.cap.get(name));
								return;
							} else if (plugin.captcha.containsKey(name)
									&& plugin.captcha.get(name) > Settings.maxLoginTry) {
								try {
									plugin.captcha.remove(name);
									plugin.cap.remove(name);
								} catch (NullPointerException npe) {
								}
							}
						}
						if (PasswordSecurity.comparePasswordWithHash(password,
								hash, name)) {
							PlayerAuth auth = new PlayerAuth(name, hash, ip,
									new Date().getTime(), email);
							database.updateSession(auth);
							PlayerCache.getInstance().addPlayer(auth);
							final LimboPlayer limbo = LimboCache.getInstance()
									.getLimboPlayer(name);
							final PlayerAuth getAuth = database.getAuth(name);
							if (limbo != null) {
								Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
										new Runnable() {
											@Override
											public void run() {
												player.setOp(limbo
														.getOperator());
											}
										});

								utils.addNormal(player, limbo.getGroup());

								if ((Settings.isTeleportToSpawnEnabled)
										&& (!Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds
												.contains(player.getWorld()
														.getName()))) {
									if ((Settings.isSaveQuitLocationEnabled)
											&& (getAuth.getQuitLocY() != 0)) {
										Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
											@Override
											public void run() {
												utils.packCoords(getAuth.getQuitLocX(),
														getAuth.getQuitLocY(),
														getAuth.getQuitLocZ(),
														getAuth.getWorld(), player);
											}
										});
									} else {
										Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
												new Runnable() {
													@Override
													public void run() {
														AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(
																player,
																limbo.getLoc());
														pm.callEvent(tpEvent);
														Location fLoc = tpEvent
																.getTo();
														if (!tpEvent
																.isCancelled()) {
															if (!fLoc
																	.getChunk()
																	.isLoaded()) {
																fLoc.getChunk()
																		.load();
															}
															player.teleport(fLoc);
														}
													}
												});
									}
								} else if (Settings.isForceSpawnLocOnJoinEnabled
										&& Settings.getForcedWorlds
												.contains(player.getWorld()
														.getName())) {
									final Location spawnL = spawnLoc;
									Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
											new Runnable() {
												@Override
												public void run() {
													SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(
															player,
															player.getLocation(),
															spawnL, true);
													pm.callEvent(tpEvent);
													if (!tpEvent.isCancelled()) {
														Location fLoc = tpEvent
																.getTo();
														if (!fLoc.getChunk()
																.isLoaded()) {
															fLoc.getChunk()
																	.load();
														}
														player.teleport(fLoc);
													}
												}
											});
								} else if ((Settings.isSaveQuitLocationEnabled)
										&& (getAuth.getQuitLocY() != 0)) {
									Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
										@Override
										public void run() {
											utils.packCoords(getAuth.getQuitLocX(),
													getAuth.getQuitLocY(),
													getAuth.getQuitLocZ(),
													getAuth.getWorld(), player);
										}
									});
								} else {
									Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
											new Runnable() {
												@Override
												public void run() {
													AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(
															player, limbo
																	.getLoc());
													pm.callEvent(tpEvent);
													Location fLoc = tpEvent
															.getTo();
													if (!tpEvent.isCancelled()) {
														if (!fLoc.getChunk()
																.isLoaded()) {
															fLoc.getChunk()
																	.load();
														}
														player.teleport(fLoc);
													}
												}
											});
								}

								Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
										new Runnable() {
											@Override
											public void run() {
												player.setGameMode(GameMode
														.getByValue(limbo
																.getGameMode()));
											}
										});

								if (Settings.protectInventoryBeforeLogInEnabled
										&& player.hasPlayedBefore()) {
									Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
											new Runnable() {
												@Override
												public void run() {
													RestoreInventoryEvent event = new RestoreInventoryEvent(
															player,
															limbo.getInventory(),
															limbo.getArmour());
													Bukkit.getServer()
															.getPluginManager()
															.callEvent(event);
													if (!event.isCancelled()) {
														API.setPlayerInventory(
																player,
																limbo.getInventory(),
																limbo.getArmour());
													}
												}
											});
								}

								player.getServer().getScheduler()
										.cancelTask(limbo.getTimeoutTaskId());
								player.getServer().getScheduler()
										.cancelTask(limbo.getMessageTaskId());
								LimboCache.getInstance()
										.deleteLimboPlayer(name);
								if (playerCache.doesCacheExist(name)) {
									playerCache.removeCache(name);
								}
							}

							/*
							 * Little Work Around under Registration Group
							 * Switching for admins that add Registration thru a
							 * web Scripts.
							 */
							if (Settings.isPermissionCheckEnabled
									&& AuthMe.permission.playerInGroup(player,
											Settings.unRegisteredGroup)
									&& !Settings.unRegisteredGroup.isEmpty()) {
								AuthMe.permission.playerRemoveGroup(
										player.getWorld(), player.getName(),
										Settings.unRegisteredGroup);
								AuthMe.permission.playerAddGroup(
										player.getWorld(), player.getName(),
										Settings.getRegisteredGroup);
							}

							try {
								if (!PlayersLogs.players.contains(player
										.getName()))
									PlayersLogs.players.add(player.getName());
								pllog.save();
							} catch (NullPointerException ex) {
							}
							Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
									new Runnable() {
										@Override
										public void run() {
											Bukkit.getServer()
													.getPluginManager()
													.callEvent(
															new LoginEvent(
																	player,
																	true));
										}
									});
							if (Settings.useCaptcha) {
								if (plugin.captcha.containsKey(name)) {
									plugin.captcha.remove(name);
								}
								if (plugin.cap.containsKey(name)) {
									plugin.cap.containsKey(name);
								}
							}
							player.sendMessage(m._("login"));
							displayOtherAccounts(auth);
							if (!Settings.noConsoleSpam)
								ConsoleLogger.info(player.getName()
										+ " logged in!");
							if (plugin.notifications != null) {
								plugin.notifications
										.showNotification(new Notification(
												"[AuthMe] " + player.getName()
														+ " logged in!"));
							}
							Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
									new Runnable() {
										@Override
										public void run() {
											player.saveData();
										}
									});

						} else {
							if (!Settings.noConsoleSpam)
								ConsoleLogger.info(player.getName()
										+ " used the wrong password");
							if (Settings.isKickOnWrongPasswordEnabled) {
								try {
									final int gm = AuthMePlayerListener.gameMode
											.get(name);
									Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
											new Runnable() {
												@Override
												public void run() {
													player.setGameMode(GameMode
															.getByValue(gm));
												}
											});
								} catch (NullPointerException npe) {
								}
								Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
										new Runnable() {
											@Override
											public void run() {
												player.kickPlayer(m
														._("wrong_pwd"));
											}
										});
							} else {
								player.sendMessage(m._("wrong_pwd"));
								return;
							}
						}
					} else {
						// need for bypass password check if passpartu command
						// is enabled
						PlayerAuth auth = new PlayerAuth(name, hash, ip,
								new Date().getTime(), email);
						database.updateSession(auth);
						PlayerCache.getInstance().addPlayer(auth);
						final LimboPlayer limbo = LimboCache.getInstance()
								.getLimboPlayer(name);
						if (limbo != null) {

							Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
									new Runnable() {
										@Override
										public void run() {
											player.setOp(limbo.getOperator());
										}
									});

							utils.addNormal(player, limbo.getGroup());

							if ((Settings.isTeleportToSpawnEnabled)
									&& (!Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds
											.contains(player.getWorld()
													.getName()))) {
								if ((Settings.isSaveQuitLocationEnabled)
										&& (database.getAuth(name)
												.getQuitLocY() != 0)) {
									String worldname = database.getAuth(name)
											.getWorld();
									World theWorld;
									if (worldname.equals("unavailableworld")) {
										theWorld = player.getWorld();
									} else {
										theWorld = Bukkit.getWorld(worldname);
									}
									if (theWorld == null)
										theWorld = player.getWorld();
									final Location quitLoc = new Location(
											theWorld, database.getAuth(name)
													.getQuitLocX() + 0.5D,
											database.getAuth(name)
													.getQuitLocY() + 0.5D,
											database.getAuth(name)
													.getQuitLocZ() + 0.5D);

									Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
											new Runnable() {
												@Override
												public void run() {
													AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(
															player, quitLoc);
													pm.callEvent(tpEvent);
													Location fLoc = tpEvent
															.getTo();
													if (!tpEvent.isCancelled()) {
														if (!fLoc.getChunk()
																.isLoaded()) {
															fLoc.getChunk()
																	.load();
														}
														player.teleport(fLoc);
													}
												}
											});
								} else {
									Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
											new Runnable() {
												@Override
												public void run() {
													AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(
															player, limbo
																	.getLoc());
													pm.callEvent(tpEvent);
													Location fLoc = tpEvent
															.getTo();
													if (!tpEvent.isCancelled()) {
														if (!fLoc.getChunk()
																.isLoaded()) {
															fLoc.getChunk()
																	.load();
														}
														player.teleport(fLoc);
													}
												}
											});
								}
							} else if (Settings.isForceSpawnLocOnJoinEnabled
									&& Settings.getForcedWorlds.contains(player
											.getWorld().getName())) {
								final Location spawnL = spawnLoc;
								Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
										new Runnable() {
											@Override
											public void run() {
												SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(
														player, player
																.getLocation(),
														spawnL, true);
												pm.callEvent(tpEvent);
												if (!tpEvent.isCancelled()) {
													Location fLoc = tpEvent
															.getTo();
													if (!fLoc.getChunk()
															.isLoaded()) {
														fLoc.getChunk().load();
													}
													player.teleport(fLoc);
												}
											}
										});
							} else if ((Settings.isSaveQuitLocationEnabled)
									&& (database.getAuth(name).getQuitLocY() != 0)) {
								String worldname = database.getAuth(name)
										.getWorld();
								World theWorld;
								if (worldname.equals("unavailableworld")) {
									theWorld = player.getWorld();
								} else {
									theWorld = Bukkit.getWorld(worldname);
								}
								if (theWorld == null)
									theWorld = player.getWorld();
								final Location quitLoc = new Location(
										theWorld,
										database.getAuth(name).getQuitLocX() + 0.5D,
										database.getAuth(name).getQuitLocY() + 0.5D,
										database.getAuth(name).getQuitLocZ() + 0.5D);
								Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
										new Runnable() {
											@Override
											public void run() {
												AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(
														player, quitLoc);
												pm.callEvent(tpEvent);
												Location fLoc = tpEvent.getTo();
												if (!tpEvent.isCancelled()) {
													if (!fLoc.getChunk()
															.isLoaded()) {
														fLoc.getChunk().load();
													}
													player.teleport(fLoc);
												}
											}
										});
							} else {
								Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
										new Runnable() {
											@Override
											public void run() {
												AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(
														player, limbo.getLoc());
												pm.callEvent(tpEvent);
												Location fLoc = tpEvent.getTo();
												if (!tpEvent.isCancelled()) {
													if (!fLoc.getChunk()
															.isLoaded()) {
														fLoc.getChunk().load();
													}
													player.teleport(fLoc);
												}
											}
										});
							}

							Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
									new Runnable() {
										@Override
										public void run() {
											player.setGameMode(GameMode
													.getByValue(limbo
															.getGameMode()));
										}
									});

							if (Settings.protectInventoryBeforeLogInEnabled
									&& player.hasPlayedBefore()) {
								Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
										new Runnable() {
											@Override
											public void run() {
												RestoreInventoryEvent event = new RestoreInventoryEvent(
														player,
														limbo.getInventory(),
														limbo.getArmour());
												Bukkit.getServer()
														.getPluginManager()
														.callEvent(event);
												if (!event.isCancelled()) {
													API.setPlayerInventory(
															player,
															limbo.getInventory(),
															limbo.getArmour());
												}
											}
										});
							}

							player.getServer().getScheduler()
									.cancelTask(limbo.getTimeoutTaskId());
							player.getServer().getScheduler()
									.cancelTask(limbo.getMessageTaskId());
							LimboCache.getInstance().deleteLimboPlayer(name);
							if (playerCache.doesCacheExist(name)) {
								playerCache.removeCache(name);
							}
						}

						/*
						 * Little Work Around under Registration Group Switching
						 * for admins that add Registration thru a web Scripts.
						 */
						if (Settings.isPermissionCheckEnabled
								&& AuthMe.permission.playerInGroup(player,
										Settings.unRegisteredGroup)
								&& !Settings.unRegisteredGroup.isEmpty()) {
							AuthMe.permission.playerRemoveGroup(
									player.getWorld(), player.getName(),
									Settings.unRegisteredGroup);
							AuthMe.permission.playerAddGroup(player.getWorld(),
									player.getName(),
									Settings.getRegisteredGroup);
						}

						try {
							if (!PlayersLogs.players.contains(player.getName()))
								PlayersLogs.players.add(player.getName());
							pllog.save();
						} catch (NullPointerException ex) {
						}
						Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
							@Override
							public void run() {
								Bukkit.getServer()
										.getPluginManager()
										.callEvent(new LoginEvent(player, true));
							}
						});
						if (Settings.useCaptcha) {
							if (plugin.captcha.containsKey(name)) {
								plugin.captcha.remove(name);
							}
							if (plugin.cap.containsKey(name)) {
								plugin.cap.containsKey(name);
							}
						}
						player.sendMessage(m._("login"));
						displayOtherAccounts(auth);
						if (!Settings.noConsoleSpam)
							ConsoleLogger.info(player.getName() + " logged in!");
						if (plugin.notifications != null) {
							plugin.notifications
									.showNotification(new Notification(
											"[AuthMe] " + player.getName()
													+ " logged in!"));
						}
						Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
							@Override
							public void run() {
								player.saveData();
							}
						});
					}
				} catch (NoSuchAlgorithmException ex) {
					ConsoleLogger.showError(ex.getMessage());
					player.sendMessage(m._("error"));
					return;
				}
				return;
			}

		});

	}

	private void displayOtherAccounts(PlayerAuth auth) {
		if (!Settings.displayOtherAccounts) {
			return;
		}
		if (auth == null) {
			return;
		}
		if (this.database.getAllAuthsByName(auth).isEmpty()
				|| this.database.getAllAuthsByName(auth) == null) {
			return;
		}
		if (this.database.getAllAuthsByName(auth).size() == 1) {
			return;
		}
		List<String> accountList = this.database.getAllAuthsByName(auth);
		String message = "[AuthMe] ";
		int i = 0;
		for (String account : accountList) {
			i++;
			message = message + account;
			if (i != accountList.size()) {
				message = message + ", ";
			} else {
				message = message + ".";
			}

		}
		for (Player player : AuthMe.getInstance().getServer()
				.getOnlinePlayers()) {
			if (plugin.authmePermissible(player, "authme.seeOtherAccounts")) {
				player.sendMessage("[AuthMe] The player " + auth.getNickname()
						+ " has " + String.valueOf(accountList.size())
						+ " accounts");
				player.sendMessage(message);
			}
		}
	}

}
