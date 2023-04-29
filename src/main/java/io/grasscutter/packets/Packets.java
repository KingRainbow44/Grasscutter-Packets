package io.grasscutter.packets;

import emu.grasscutter.net.packet.PacketOpcodesUtils;
import emu.grasscutter.plugin.Plugin;
import emu.grasscutter.server.event.EventHandler;
import emu.grasscutter.server.event.HandlerPriority;
import emu.grasscutter.server.event.game.ReceivePacketEvent;
import emu.grasscutter.server.event.game.SendPacketEvent;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.Objects;

public final class Packets extends Plugin {
    @Getter private static Packets instance;

    /**
     * @return The plugin's logger.
     */
    public static Logger getPluginLogger() {
        return Packets.getInstance().getLogger();
    }

    @Getter private final PacketServer packetServer
            = new PacketServer();

    @Override public void onLoad() {
        Packets.instance = this;
        PacketsConfig.load();

        try {
            PacketUtils.loadPacketMessages();
        } catch (IllegalAccessException | ClassNotFoundException exception) {
            Packets.getPluginLogger().error("Unable to load packet messages.", exception);
        }
    }

    @Override public void onEnable() {
        this.getPacketServer().start(); // Start the server.

        // Register packet handlers.
        new EventHandler<>(ReceivePacketEvent.class)
                .listener(this::onPacketReceived)
                .priority(HandlerPriority.HIGH)
                .ignore(true).register(this);
        new EventHandler<>(SendPacketEvent.class)
                .listener(this::onPacketSend)
                .priority(HandlerPriority.HIGH)
                .ignore(true).register(this);

        // Register the packet command.
        this.getHandle().registerCommand(new PacketsCommand());

        // Check if the frontend should be served.
        if (PacketsConfig.get().serveClient) {
            // Register the frontend.
            this.getHandle().getHttpServer().addRouter(PacketsRouter.class);
        }

        this.getLogger().info("Packet logger enabled.");
    }

    @Override public void onDisable() {
        try {
            this.getPacketServer().stop();
        } catch (Exception ignored) {
            this.getLogger().error("Unable to stop packet server.");
            return;
        }

        this.getLogger().info("Packet logger disabled.");
    }

    private void onPacketReceived(ReceivePacketEvent event) {
        var packetId = event.getPacketId();
        var packetData = event.getPacketData();

        // Check if the packet shouldn't be logged.
        var packetName = PacketOpcodesUtils.getOpcodeName(packetId);
        if (PacketsConfig.ignore(packetName)) return;

        this.broadcastPacket(packetId, packetData, PacketUtils.Type.CLIENT);
    }

    private void onPacketSend(SendPacketEvent event) {
        var packet = event.getPacket();
        var packetId = packet.getOpcode();
        var packetData = packet.getData();

        // Check if the packet shouldn't be logged.
        var packetName = PacketOpcodesUtils.getOpcodeName(packetId);
        if (PacketsConfig.ignore(packetName)) return;

        this.broadcastPacket(packetId, packetData, PacketUtils.Type.SERVER);
    }

    /**
     * Serializes and broadcasts a packet.
     *
     * @param packetId The packet's ID.
     * @param packetData The packet's data.
     */
    private void broadcastPacket(int packetId, byte[] packetData, PacketUtils.Type type) {
        try {
            // Encode the packet into JSON.
            var encoded = PacketUtils.serialize(packetId, packetData);
            if (encoded == null) return;

            // Create the message.
            var message = PacketUtils.message(packetId, encoded, type);
            // Broadcast the message to all connected clients.
            this.getPacketServer().broadcast(message);
        } catch (Exception exception) {
            Packets.getPluginLogger().warn("Unable to serialize packet.", exception);
        }
    }
}
