package com.technicjelle.bluemapplayercontrol;

import com.technicjelle.UpdateChecker;
import com.technicjelle.bluemapplayercontrol.commands.BMPC;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.List;

public final class BlueMapPlayerControl extends JavaPlugin {
	UpdateChecker updateChecker;
	BMPC executor;
	ConfigManager configManager;

	@Override
	public void onEnable() {
		// Initialize configuration manager
		configManager = new ConfigManager(this);
		
		// Log enabled message
		getLogger().info("[BlueMapPlayerControl] Plugin enabled successfully");

		// Initialize metrics
		new Metrics(this, 18378);

		// Initialize update checker
		updateChecker = new UpdateChecker("TechnicJelle", "BlueMapPlayerControl", getDescription().getVersion());
		updateChecker.checkAsync();

		// Register BlueMap callback
		BlueMapAPI.onEnable(api -> updateChecker.logUpdateMessage(getLogger()));

		// Register command
		registerCommand();
	}

	@Override
	public void onDisable() {
		getLogger().info("[BlueMapPlayerControl] Plugin disabled");
	}
	
	public void registerCommand() {
		String commandName = configManager.getCommandName();
		
		// Create executor first
		executor = new BMPC(this, configManager);
		
		// Always create commands dynamically since they're not in plugin.yml
		createDynamicCommand(commandName);
		// Register aliases for dynamic command
		registerAliasesForDynamicCommand();
		
		// Log command registration
		String aliases = String.join(", ", configManager.getCommandAliases());
		getLogger().info("[BlueMapPlayerControl] Command '" + commandName + "' registered with aliases: " + aliases);
	}
	
	private PluginCommand createDynamicCommand(String commandName) {
		try {
			// Get CommandMap through reflection
			Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			commandMapField.setAccessible(true);
			CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
			
			// Create a custom command that delegates to our executor
			Command customCommand = new Command(commandName) {
				@Override
				public boolean execute(CommandSender sender, String commandLabel, String[] args) {
					if (executor != null) {
						return executor.onCommand(sender, this, commandLabel, args);
					}
					return false;
				}
				
				@Override
				public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
					if (executor != null) {
						return executor.onTabComplete(sender, this, alias, args);
					}
					return null;
				}
			};
			
			customCommand.setDescription("Control player visibility on BlueMap");
			customCommand.setPermission("bmpc");
			customCommand.setUsage("/<command> [help | toggle | show | hide] [player]");
			
			// Register the command
			commandMap.register(getName().toLowerCase(), customCommand);
			
			// Return null since we're using a custom Command instead of PluginCommand
			// The executor will be set on the custom command directly
			return null;
		} catch (Exception e) {
			getLogger().warning("Failed to create dynamic command: " + e.getMessage());
			return null;
		}
	}
	
	private void registerAliasesForDynamicCommand() {
		try {
			// Get CommandMap through reflection
			Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			commandMapField.setAccessible(true);
			CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
			
			// Register each alias
			for (String alias : configManager.getCommandAliases()) {
				// Create a new command for each alias
				Command aliasCommand = new Command(alias) {
					@Override
					public boolean execute(CommandSender sender, String commandLabel, String[] args) {
						if (executor != null) {
							return executor.onCommand(sender, this, commandLabel, args);
						}
						return false;
					}
					
					@Override
					public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
						if (executor != null) {
							return executor.onTabComplete(sender, this, alias, args);
						}
						return null;
					}
				};
				
				// Set the same permission and description
				aliasCommand.setPermission("bmpc");
				aliasCommand.setDescription("Control player visibility on BlueMap");
				aliasCommand.setUsage("/<command> [help | toggle | show | hide] [player]");
				
				// Register the alias
				commandMap.register(getName().toLowerCase(), aliasCommand);
			}
		} catch (Exception e) {
			getLogger().warning("Failed to register aliases: " + e.getMessage());
		}
	}
	
	/**
	 * Get the configuration manager
	 * @return ConfigManager instance
	 */
	public ConfigManager getConfigManager() {
		return configManager;
	}
}
