package io.grasscutter.packets;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketServer extends WebSocketServer {
    private final Map<String, WebSocket> connections =
            new ConcurrentHashMap<>();

    /**
     * Constructor for the PacketServer class.
     * Defaults the port to 8080.
     * @see <a href="https://github.com/KingRainbow44/Packet-Visualizer">...</a>
     */
    public PacketServer() {
        super(new InetSocketAddress(8080));
    }

    /**
     * Broadcasts a JSON object to all connected clients.
     *
     * @param object The JSON object to broadcast.
     */
    public void broadcast(JsonObject object) {
        var encoded = PacketUtils.JSON.toJson(object);
        this.connections.forEach((key, conn) ->
                conn.send(encoded));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        this.connections.put(conn.getLocalSocketAddress().toString(), conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        this.connections.remove(conn.getLocalSocketAddress().toString());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        var object = PacketUtils.JSON.fromJson(message, JsonObject.class);
        if (object == null) {
            Packets.getPluginLogger().error("Failed to decode JSON object.");
            conn.close();
            return;
        }

        if (object.get("packetId").getAsInt() == 0) {
            conn.send(PacketUtils.JSON.toJson(PacketUtils.handshake()));
            Packets.getPluginLogger().info("Now broadcasting to " + conn.getLocalSocketAddress().toString());
        } else {
            conn.close();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Packets.getPluginLogger().error("Error in WebSocket connection.", ex);
    }

    @Override
    public void onStart() {
        Packets.getPluginLogger().info("Packet server started on ws://localhost:8080.");
    }
}
