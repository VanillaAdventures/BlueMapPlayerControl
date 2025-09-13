package com.technicjelle.bluemapplayercontrol;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;
    
    private FileConfiguration config;
    private FileConfiguration messages;
    private String commandName;
    private List<String> commandAliases;
    private boolean minimessageEnabled;
    private boolean debugEnabled;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacySection();
        
        loadConfig();
        loadMessages();
    }
    
    private void loadConfig() {
        // Save default config if not exists
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Load command settings
        commandName = config.getString("command.name", "bmpc");
        commandAliases = config.getStringList("command.aliases");
        minimessageEnabled = config.getBoolean("language.minimessage", true);
        
        // Load plugin settings
        debugEnabled = config.getBoolean("settings.debug", false);
        
        logger.info("Configuration loaded");
    }
    
    private void loadMessages() {
        String languageFile = config.getString("language.file", "en");
        String fileName = "messages_" + languageFile + ".yml";
        
        File messagesFile = new File(plugin.getDataFolder(), fileName);
        
        // Create messages file if it doesn't exist
        if (!messagesFile.exists()) {
            try (InputStream inputStream = plugin.getResource(fileName)) {
                if (inputStream != null) {
                    Files.copy(inputStream, messagesFile.toPath());
                } else {
                    // Fallback to English if language file doesn't exist
                    fileName = "messages_en.yml";
                    try (InputStream fallbackStream = plugin.getResource(fileName)) {
                        if (fallbackStream != null) {
                            Files.copy(fallbackStream, messagesFile.toPath());
                        }
                    }
                }
            } catch (IOException e) {
                logger.warning("Could not create messages file: " + e.getMessage());
                return;
            }
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        logger.info("Messages loaded from " + fileName);
    }
    
    public String getCommandName() {
        return commandName;
    }
    
    public List<String> getCommandAliases() {
        return commandAliases;
    }
    
    public boolean isMinimessageEnabled() {
        return minimessageEnabled;
    }
    
    public String getMessage(String path) {
        return messages.getString(path, "Message not found: " + path);
    }
    
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        
        // Replace placeholders in format {key}
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        
        return message;
    }
    
    public Component getMessageComponent(String path) {
        return getMessageComponent(path, new String[0]);
    }
    
    public Component getMessageComponent(String path, String... placeholders) {
        String message = getMessage(path, placeholders);
        
        if (minimessageEnabled) {
            // Use real MiniMessage
            return miniMessage.deserialize(message);
        } else {
            // Use legacy color codes
            return legacySerializer.deserialize(message);
        }
    }
    
    // Legacy method for backward compatibility
    public String getMessageFormatted(String path, String... placeholders) {
        Component component = getMessageComponent(path, placeholders);
        return legacySerializer.serialize(component);
    }
    
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    public void debugLog(String message) {
        if (debugEnabled) {
            logger.info("[DEBUG] " + message);
        }
    }
    
    public void debugLog(String message, Object... args) {
        if (debugEnabled) {
            logger.info("[DEBUG] " + String.format(message, args));
        }
    }
    
    public void reload() {
        plugin.reloadConfig();
        loadConfig();
        loadMessages();
    }
}
