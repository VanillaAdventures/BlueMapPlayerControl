package com.technicjelle.bluemapplayercontrol.commands;

import com.technicjelle.bluemapplayercontrol.BlueMapPlayerControl;
import com.technicjelle.bluemapplayercontrol.ConfigManager;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class BMPC implements CommandExecutor, TabCompleter {
	
	private final BlueMapPlayerControl plugin;
	private final ConfigManager configManager;
	
	public BMPC(BlueMapPlayerControl plugin, ConfigManager configManager) {
		this.plugin = plugin;
		this.configManager = configManager;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		// Debug logging
		configManager.debugLog("Command executed by %s with args: %s", sender.getName(), String.join(" ", args));
		
		// Check if BlueMap is available
		if (!BlueMapAPI.getInstance().isPresent()) {
			configManager.debugLog("BlueMap API not available");
			sendMessage(sender, "errors.bluemap_not_loaded");
			return true;
		}
		
		BlueMapAPI api = BlueMapAPI.getInstance().get();
		configManager.debugLog("BlueMap API loaded successfully");
		
		// Handle help command
		if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
			sendHelpMessage(sender);
			return true;
		}
		
		// Handle reload command
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("bmpc.reload")) {
				configManager.debugLog("Sender %s lacks permission bmpc.reload", sender.getName());
				sendMessage(sender, "errors.no_permission");
				return true;
			}
			
			configManager.debugLog("Reloading configuration...");
			configManager.reload();
			
			// Re-register command with new aliases
			plugin.registerCommand();
			
			sendMessage(sender, "status.config_reloaded");
			return true;
		}
		
		String subCommand = args[0].toLowerCase();
		
		// Handle self commands (only for players, only when no second argument)
		if (sender instanceof Player player && args.length == 1) {
			UUID senderUUID = player.getUniqueId();
			
			switch (subCommand) {
				case "toggle":
					if (!sender.hasPermission("bmpc.self.toggle")) {
						sendMessage(sender, "errors.no_permission");
						return true;
					}
					toggleSelf(api, sender, senderUUID);
					return true;
					
				case "show":
					if (!sender.hasPermission("bmpc.self.show")) {
						sendMessage(sender, "errors.no_permission");
						return true;
					}
					showSelf(api, sender, senderUUID);
					return true;
					
				case "hide":
					if (!sender.hasPermission("bmpc.self.hide")) {
						sendMessage(sender, "errors.no_permission");
						return true;
					}
					hideSelf(api, sender, senderUUID);
					return true;
			}
		}
		
		// Handle other player commands (requires second argument)
		if (args.length < 2) {
			configManager.debugLog("Not enough arguments for other player command");
			sendMessage(sender, "errors.invalid_usage");
			return true;
		}
		
		String targetName = args[1];
		configManager.debugLog("Looking for target player: %s", targetName);
		
		List<Entity> targets = Bukkit.selectEntities(sender, targetName);
		configManager.debugLog("Found %d entities matching '%s'", targets.size(), targetName);
		
		// Filter to only players
		List<Player> playerTargets = new ArrayList<>();
		for (Entity target : targets) {
			if (target instanceof Player player) {
				playerTargets.add(player);
				configManager.debugLog("Added player target: %s (UUID: %s)", player.getName(), player.getUniqueId());
			}
		}
		
		if (playerTargets.isEmpty()) {
			configManager.debugLog("No players found matching '%s'", targetName);
			sendMessage(sender, "errors.player_not_found", "player", targetName);
			return true;
		}
		
		for (Player targetPlayer : playerTargets) {
			configManager.debugLog("Processing target player: %s (UUID: %s)", targetPlayer.getName(), targetPlayer.getUniqueId());
			
			// Check if trying to target self
			if (sender instanceof Player && targetPlayer.equals(sender)) {
				configManager.debugLog("Player %s tried to target themselves, skipping", sender.getName());
				sendMessage(sender, "errors.cannot_target_self");
				continue;
			}
			
			configManager.debugLog("Executing command '%s' on player %s", subCommand, targetPlayer.getName());
			
			switch (subCommand) {
				case "toggle":
					if (!sender.hasPermission("bmpc.others.toggle")) {
						configManager.debugLog("Sender %s lacks permission bmpc.others.toggle", sender.getName());
						sendMessage(sender, "errors.no_permission");
						continue;
					}
					toggleOther(api, sender, targetPlayer);
					break;
					
				case "show":
					if (!sender.hasPermission("bmpc.others.show")) {
						configManager.debugLog("Sender %s lacks permission bmpc.others.show", sender.getName());
						sendMessage(sender, "errors.no_permission");
						continue;
					}
					showOther(api, sender, targetPlayer);
					break;
					
				case "hide":
					if (!sender.hasPermission("bmpc.others.hide")) {
						configManager.debugLog("Sender %s lacks permission bmpc.others.hide", sender.getName());
						sendMessage(sender, "errors.no_permission");
						continue;
					}
					hideOther(api, sender, targetPlayer);
					break;
					
				default:
					configManager.debugLog("Unknown subcommand: %s", subCommand);
					sendMessage(sender, "errors.invalid_usage");
					break;
			}
		}
		
		return true;
	}

	private void toggleSelf(BlueMapAPI api, CommandSender sender, UUID senderUUID) {
		boolean currentVisibility = api.getWebApp().getPlayerVisibility(senderUUID);
		configManager.debugLog("Player %s current visibility: %s", sender.getName(), currentVisibility);
		
		api.getWebApp().setPlayerVisibility(senderUUID, !currentVisibility);
		configManager.debugLog("Set player %s visibility to: %s", sender.getName(), !currentVisibility);
		
		if (!currentVisibility) {
			sendMessage(sender, "status.visible");
		} else {
			sendMessage(sender, "status.invisible");
		}
	}
	
	private void showSelf(BlueMapAPI api, CommandSender sender, UUID senderUUID) {
		configManager.debugLog("Showing player %s on map", sender.getName());
		api.getWebApp().setPlayerVisibility(senderUUID, true);
		sendMessage(sender, "status.visible");
	}

	private void hideSelf(BlueMapAPI api, CommandSender sender, UUID senderUUID) {
		configManager.debugLog("Hiding player %s from map", sender.getName());
		api.getWebApp().setPlayerVisibility(senderUUID, false);
		sendMessage(sender, "status.invisible");
	}
	
	private void toggleOther(BlueMapAPI api, CommandSender sender, Player targetPlayer) {
		boolean currentVisibility = api.getWebApp().getPlayerVisibility(targetPlayer.getUniqueId());
		configManager.debugLog("Player %s current visibility: %s", targetPlayer.getName(), currentVisibility);
		
		api.getWebApp().setPlayerVisibility(targetPlayer.getUniqueId(), !currentVisibility);
		configManager.debugLog("Set player %s visibility to: %s", targetPlayer.getName(), !currentVisibility);
		
		if (!currentVisibility) {
			sendMessage(sender, "status.other_visible", "player", targetPlayer.getDisplayName());
		} else {
			sendMessage(sender, "status.other_invisible", "player", targetPlayer.getDisplayName());
		}
	}

	private void showOther(BlueMapAPI api, CommandSender sender, Player targetPlayer) {
		configManager.debugLog("Showing player %s on map", targetPlayer.getName());
		api.getWebApp().setPlayerVisibility(targetPlayer.getUniqueId(), true);
		sendMessage(sender, "status.other_visible", "player", targetPlayer.getDisplayName());
	}

	private void hideOther(BlueMapAPI api, CommandSender sender, Player targetPlayer) {
		configManager.debugLog("Hiding player %s from map", targetPlayer.getName());
		api.getWebApp().setPlayerVisibility(targetPlayer.getUniqueId(), false);
		sendMessage(sender, "status.other_invisible", "player", targetPlayer.getDisplayName());
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
		List<String> completions = new ArrayList<>();
		
		if (args.length == 1) {
			// First argument: subcommands
			completions.add("help");
			completions.add("toggle");
			completions.add("show");
			completions.add("hide");
			
			// Add reload command if player has permission
			if (sender.hasPermission("bmpc.reload")) {
				completions.add("reload");
			}
			
			// Add player names if they have permission for others
			if (sender.hasPermission("bmpc.others")) {
				for (Player player : sender.getServer().getOnlinePlayers()) {
					completions.add(player.getName());
				}
			}
		} else if (args.length == 2) {
			// Second argument: player names (only for other player commands)
			String subCommand = args[0].toLowerCase();
			if (subCommand.equals("toggle") || subCommand.equals("show") || subCommand.equals("hide")) {
				if (sender.hasPermission("bmpc.others")) {
					for (Player player : sender.getServer().getOnlinePlayers()) {
						completions.add(player.getName());
					}
					completions.add("@a");
					completions.add("@p");
					completions.add("@r");
					completions.add("@s");
				}
			}
		}
		
		return completions;
	}
	
	private void sendMessage(CommandSender sender, String messageKey, String... placeholders) {
		String message = configManager.getMessageFormatted(messageKey, placeholders);
		sender.sendMessage(message);
	}
	
	private void sendHelpMessage(CommandSender sender) {
		String commandName = configManager.getCommandName();
		String version = plugin.getDescription().getVersion();
		
		// Send header
		String header = configManager.getMessageFormatted("help.header", 
			"version", version);
		sender.sendMessage(header);
		
		// Send description
		String description = configManager.getMessageFormatted("help.description");
		sender.sendMessage(description);
		
		// Send commands
		sender.sendMessage("");
		
		// Self commands (only for players)
		if (sender instanceof Player) {
			if (sender.hasPermission("bmpc.self.toggle")) {
				String toggleCmd = configManager.getMessageFormatted("help.commands.toggle",
					"command", commandName);
				sender.sendMessage(toggleCmd);
			}
			
			if (sender.hasPermission("bmpc.self.show")) {
				String showCmd = configManager.getMessageFormatted("help.commands.show",
					"command", commandName);
				sender.sendMessage(showCmd);
			}
			
			if (sender.hasPermission("bmpc.self.hide")) {
				String hideCmd = configManager.getMessageFormatted("help.commands.hide",
					"command", commandName);
				sender.sendMessage(hideCmd);
			}
		}
		
		// Other player commands
		if (sender.hasPermission("bmpc.others")) {
			sender.sendMessage("");
			
			if (sender.hasPermission("bmpc.others.toggle")) {
				String toggleOtherCmd = configManager.getMessageFormatted("help.commands.toggle_other",
					"command", commandName);
				sender.sendMessage(toggleOtherCmd);
			}
			
			if (sender.hasPermission("bmpc.others.show")) {
				String showOtherCmd = configManager.getMessageFormatted("help.commands.show_other",
					"command", commandName);
				sender.sendMessage(showOtherCmd);
			}
			
			if (sender.hasPermission("bmpc.others.hide")) {
				String hideOtherCmd = configManager.getMessageFormatted("help.commands.hide_other",
					"command", commandName);
				sender.sendMessage(hideOtherCmd);
			}
		}
		
		// Admin commands
		if (sender.hasPermission("bmpc.reload")) {
			sender.sendMessage("");
			
			String reloadCmd = configManager.getMessageFormatted("help.commands.reload",
				"command", commandName);
			sender.sendMessage(reloadCmd);
		}
		
		// Send footer
		sender.sendMessage("");
		String footer = configManager.getMessageFormatted("help.footer",
			"command", commandName);
		sender.sendMessage(footer);
	}
}
