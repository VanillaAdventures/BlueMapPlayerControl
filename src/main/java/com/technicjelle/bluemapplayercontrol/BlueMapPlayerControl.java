package com.technicjelle.bluemapplayercontrol;

import com.technicjelle.UpdateChecker;
import com.technicjelle.bluemapplayercontrol.commands.BMPC;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

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
	
	private void registerCommand() {
		String commandName = configManager.getCommandName();
		PluginCommand command = getCommand(commandName);
		
		if (command == null) {
			getLogger().warning("Command '" + commandName + "' not found in plugin.yml");
			return;
		}
		
		// Create executor
		executor = new BMPC(this, configManager);
		
		// Set executor and tab completer
		command.setExecutor(executor);
		command.setTabCompleter(executor);
		
		// Log command registration
		String aliases = String.join(", ", configManager.getCommandAliases());
		getLogger().info("[BlueMapPlayerControl] Command '" + commandName + "' registered with aliases: " + aliases);
	}
	
	/**
	 * Get the configuration manager
	 * @return ConfigManager instance
	 */
	public ConfigManager getConfigManager() {
		return configManager;
	}
}
