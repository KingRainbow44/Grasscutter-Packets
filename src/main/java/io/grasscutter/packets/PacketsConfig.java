package io.grasscutter.packets;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class PacketsConfig {
    private static PacketsConfig instance
            = new PacketsConfig();

    /**
     * @return The plugin's configuration.
     */
    public static PacketsConfig get() {
        return PacketsConfig.instance;
    }

    /**
     * Loads the plugin configuration.
     */
    public static void load() {
        var baseFolder = Packets.getInstance().getDataFolder();
        var configFile = new File(baseFolder, "config.json");

        if (!configFile.exists()) {
            // Save this configuration.
            PacketsConfig.save();
        } else try {
            // Load the configuration.
            PacketsConfig.instance = PacketUtils.JSON.fromJson(
                    new FileReader(configFile), PacketsConfig.class);

            // Check if the configuration is null.
            if (PacketsConfig.instance == null) {
                PacketsConfig.instance = new PacketsConfig();
            }
        } catch (IOException ignored) {
            Packets.getPluginLogger().error("Unable to load configuration.");
        }
    }

    /**
     * Saves the plugin configuration.
     */
    public static void save() {
        var baseFolder = Packets.getInstance().getDataFolder();
        var configFile = new File(baseFolder, "config.json");

        try {
            // Save the configuration.
            var json = PacketUtils.JSON.toJson(PacketsConfig.instance);
            Files.write(configFile.toPath(), json.getBytes());
        } catch (IOException ignored) {
            Packets.getPluginLogger().error("Unable to save configuration.");
        }
    }

    /** Enable serving the HTTP frontend. */
    public boolean serveClient = true;

    /**
     * These packets are ignored from being logged.
     * This setting is ignored if highlighted packets is enabled.
     */
    public List<String> ignoredPackets = new ArrayList<>();

    /**
     * These packets are the only ones which should be logged.
     * If this list is empty, all packets will be shown.
     */
    public List<String> highlightedPackets = new ArrayList<>();

    /**
     * Checks if the packet should be ignored.
     *
     * @param packetName The packet name.
     * @return {@code true} if the packet should be ignored.
     */
    public static boolean ignore(String packetName) {
        var config = PacketsConfig.get();

        if (config.highlightedPackets.isEmpty()) {
            return config.ignoredPackets.contains(packetName);
        } else {
            return !config.highlightedPackets.contains(packetName);
        }
    }
}
