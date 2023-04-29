package io.grasscutter.packets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.GeneratedMessageV3;
import emu.grasscutter.net.packet.PacketOpcodes;
import emu.grasscutter.net.packet.PacketOpcodesUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public interface PacketUtils {
    String PACKAGE = "emu.grasscutter.net.proto";
    Map<Integer, Class<GeneratedMessageV3>> PACKET_MAP
            = new HashMap<>();
    Gson JSON = new Gson();

    /**
     * Binds all packet IDs to message classes.
     */
    @SuppressWarnings("unchecked")
    static void loadPacketMessages()
            throws IllegalAccessException, ClassNotFoundException {
        var fields = PacketOpcodes.class.getFields();
        for (var field : fields) {
            try {
                var name = field.getName();
                if (name.equals("NONE"))
                    continue;

                var packetId = field.get(null);
                var packetDomain = PACKAGE + "." + name + "OuterClass$" + name;
                var packetClass = (Class<GeneratedMessageV3>) Class.forName(packetDomain);

                PACKET_MAP.put((int) packetId,  packetClass);
            } catch (ClassNotFoundException ignored) {
                // If a packet doesn't have a definition attached, ignore it.
                Packets.getPluginLogger().trace("Packet handler not found for" + field.getName());
            } catch (NoClassDefFoundError ignored) {
                // If a packet has a mis-named definition, ignore it.
                Packets.getPluginLogger().trace("Packet " + field.getName() + " has a different definition name.");
            }
        }
    }

    /**
     * Checks if the packet name is valid.
     *
     * @param packetName The packet name.
     * @return Whether the packet name is valid.
     */
    static boolean isPacket(String packetName) {
        try {
            PacketOpcodes.class.getField(packetName);
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }

    /**
     * Converts an object to a primitive.
     *
     * @param obj The object to convert.
     * @return The primitive.
     */
    static JsonPrimitive toPrimitive(Object obj) {
        if (obj instanceof Integer)
            return new JsonPrimitive((int) obj);
        if (obj instanceof Long)
            return new JsonPrimitive((long) obj);
        if (obj instanceof Float)
            return new JsonPrimitive((float) obj);
        if (obj instanceof Double)
            return new JsonPrimitive((double) obj);
        if (obj instanceof Boolean)
            return new JsonPrimitive((boolean) obj);
        if (obj instanceof String)
            return new JsonPrimitive((String) obj);
        return null;
    }

    /**
     * Decodes a packet from a byte array.
     * Re-encodes the packet into JSON.
     *
     * @param packetId The packet ID.
     * @param packetData The packet data.
     * @return The encoded packet.
     */
    @SuppressWarnings("RedundantCast")
    static String serialize(int packetId, byte[] packetData)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var packetClass = PACKET_MAP.get(packetId);
        if (packetClass == null)
            return null;

        var json = new JsonObject();
        // Check if the packet has data.
        if (packetData != null && packetData.length > 0) {
            // Get the packet's fields.
            var fields = packetClass.getDeclaredFields();
            // Parse the packet's data.
            var packet = packetClass.getMethod("parseFrom", byte[].class)
                    .invoke(null, (Object) packetData);

            for (var field : fields) {
                // Get the packet's fields.
                var name = field.getName();
                if (name.endsWith("_")) {
                    // Enable access to private fields.
                    field.setAccessible(true);

                    // Get the name of the field.
                    name = name.substring(0, name.length() - 1);
                    // Encode the field and write its name + value to the JSON object.
                    json.add(name, PacketUtils.toPrimitive(field.get(packet)));

                    // Disable access to private fields.
                    field.setAccessible(false);
                }
            }
        }

        return JSON.toJson(json);
    }

    /**
     * Creates a message for Packet-Visualizer.
     *
     * @param packetId The packet's ID.
     * @param packetData The serialized packet.
     * @param messageType The message type.
     * @return A JSON message.
     */
    static JsonObject message(int packetId, String packetData, Type messageType) {
        var packetMessage = new JsonObject();
        packetMessage.addProperty("time", System.currentTimeMillis());
        packetMessage.addProperty("source", messageType.getType());
        packetMessage.addProperty("packetId", packetId);
        packetMessage.addProperty("packetName", PacketOpcodesUtils.getOpcodeName(packetId));
        packetMessage.addProperty("length", (long) packetData.length());
        packetMessage.addProperty("data", packetData);

        var visualizerMessage = new JsonObject();
        visualizerMessage.addProperty("packetId", 1);
        visualizerMessage.add("data", packetMessage);

        return visualizerMessage;
    }

    /**
     * Crafts a handshake message.
     *
     * @return A JSON message.
     */
    static JsonObject handshake() {
        var message = new JsonObject();
        message.addProperty("packetId", 0);
        message.addProperty("data", Long.toString(System.currentTimeMillis()));

        return message;
    }

    @AllArgsConstructor
    @Getter enum Type {
        CLIENT("client"), SERVER("server");

        private final String type;
    }
}
