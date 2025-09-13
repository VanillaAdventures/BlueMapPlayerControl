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
		// Check if BlueMap is available
		if (!BlueMapAPI.getInstance().isPresent()) {
			sendMessage(sender, "errors.bluemap_not_loaded");
			return true;
		}
		
		BlueMapAPI api = BlueMapAPI.getInstance().get();
		
		// Handle help command
		if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
			sendHelpMessage(sender);
			return true;
		}
		
		String subCommand = args[0].toLowerCase();
		
		// Handle self commands (only for players)
		if (sender instanceof Player player) {
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
		} else {
			// Console can't use self commands
			if (args.length == 1) {
				sendMessage(sender, "errors.player_only");
				return true;
			}
		}
		
		// Handle other player commands
		if (args.length < 2) {
			sendMessage(sender, "errors.invalid_usage");
			return true;
		}
		
		String targetName = args[1];
		List<Entity> targets = Bukkit.selectEntities(sender, targetName);
		
		if (targets.isEmpty()) {
			sendMessage(sender, "errors.player_not_found", "player", targetName);
			return true;
		}
		
		for (Entity target : targets) {
			if (!(target instanceof Player targetPlayer)) continue;
			
			// Check if trying to target self
			if (sender instanceof Player && targetPlayer.equals(sender)) {
				sendMessage(sender, "errors.cannot_target_self");
				continue;
			}
			
			switch (subCommand) {
				case "toggle":
					if (!sender.hasPermission("bmpc.others.toggle")) {
						sendMessage(sender, "errors.no_permission");
						continue;
					}
					toggleOther(api, sender, targetPlayer);
					break;
					
				case "show":
					if (!sender.hasPermission("bmpc.others.show")) {
						sendMessage(sender, "errors.no_permission");
						continue;
					}
					showOther(api, sender, targetPlayer);
					break;
					
				case "hide":
					if (!sender.hasPermission("bmpc.others.hide")) {
						sendMessage(sender, "errors.no_permission");
						continue;
					}
					hideOther(api, sender, targetPlayer);
					break;
					
				default:
					sendMessage(sender, "errors.invalid_usage");
					break;
			}
		}
		
		return true;
	}

	private void toggleSelf(BlueMapAPI api, CommandSender sender, UUID senderUUID) {
		boolean currentVisibility = api.getWebApp().getPlayerVisibility(senderUUID);
		api.getWebApp().setPlayerVisibility(senderUUID, !currentVisibility);
		
		if (!currentVisibility) {
			sendMessage(sender, "status.visible");
		} else {
			sendMessage(sender, "status.invisible");
		}
	}
	
	private void showSelf(BlueMapAPI api, CommandSender sender, UUID senderUUID) {
		api.getWebApp().setPlayerVisibility(senderUUID, true);
		sendMessage(sender, "status.visible");
	}

	private void hideSelf(BlueMapAPI api, CommandSender sender, UUID senderUUID) {
		api.getWebApp().setPlayerVisibility(senderUUID, false);
		sendMessage(sender, "status.invisible");
	}
	
	private void toggleOther(BlueMapAPI api, CommandSender sender, Player targetPlayer) {
		boolean currentVisibility = api.getWebApp().getPlayerVisibility(targetPlayer.getUniqueId());
		api.getWebApp().setPlayerVisibility(targetPlayer.getUniqueId(), !currentVisibility);
		
		if (!currentVisibility) {
			sendMessage(sender, "status.other_visible", "player", targetPlayer.getDisplayName());
		} else {
			sendMessage(sender, "status.other_invisible", "player", targetPlayer.getDisplayName());
		}
	}

	private void showOther(BlueMapAPI api, CommandSender sender, Player targetPlayer) {
		api.getWebApp().setPlayerVisibility(targetPlayer.getUniqueId(), true);
		sendMessage(sender, "status.other_visible", "player", targetPlayer.getDisplayName());
	}

	private void hideOther(BlueMapAPI api, CommandSender sender, Player targetPlayer) {
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
		
		// Send footer
		sender.sendMessage("");
		String footer = configManager.getMessageFormatted("help.footer",
			"command", commandName);
		sender.sendMessage(footer);
	}
}
