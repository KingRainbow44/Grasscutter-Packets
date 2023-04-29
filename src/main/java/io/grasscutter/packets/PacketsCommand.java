package io.grasscutter.packets;

import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.net.packet.PacketOpcodesUtils;

import java.util.List;

@Command(label = "packets", aliases = {"pk", "pks", "packet"},
        usage = "/packets <reload|(un)ignore|(un)highlight> [packet name]",
        permission = "grasscutter.packets",
        targetRequirement = Command.TargetRequirement.NONE)
public final class PacketsCommand implements CommandHandler {
    private static final String USAGE_MESSAGE = "Usage: /packets <reload|(un)ignore|(un)highlight> [packet name]";
    private static final String RELOAD_MESSAGE = "Reloaded packet messages.";
    private static final String IGNORE_MESSAGE = "Ignored packet: %s";
    private static final String UNIGNORE_MESSAGE = "Un-ignored packet: %s";
    private static final String HIGHLIGHT_MESSAGE = "Highlighted packet: %s";
    private static final String UNHIGHLIGHT_MESSAGE = "Un-highlighted packet: %s";
    private static final String UNKNOWN_PACKET_MESSAGE = "Unknown packet: %s";

    @Override public void execute(Player sender, Player targetPlayer, List<String> args) {
        if (args.size() == 0) {
            CommandHandler.sendMessage(sender, USAGE_MESSAGE);
            return;
        }

        var subCommand = args.get(0).toLowerCase();
        switch (subCommand) {
            default -> CommandHandler.sendMessage(sender, USAGE_MESSAGE);
            case "reload", "reset" -> {
                PacketsConfig.load();
                CommandHandler.sendMessage(sender, RELOAD_MESSAGE);
            }
            case "ignore", "i" -> {
                var packet = this.toPacket(args.get(1));

                if (!PacketUtils.isPacket(packet)) {
                    CommandHandler.sendMessage(sender, String.format(UNKNOWN_PACKET_MESSAGE, packet));
                    return;
                }

                PacketsConfig.get().ignoredPackets.add(packet);
                CommandHandler.sendMessage(sender, String.format(IGNORE_MESSAGE, packet));
            }
            case "unignore", "ui" -> {
                var packet = this.toPacket(args.get(1));

                if (!PacketUtils.isPacket(packet)) {
                    CommandHandler.sendMessage(sender, String.format(UNKNOWN_PACKET_MESSAGE, packet));
                    return;
                }

                PacketsConfig.get().ignoredPackets.remove(packet);
                CommandHandler.sendMessage(sender, String.format(UNIGNORE_MESSAGE, packet));
            }
            case "highlight", "h" -> {
                var packet = this.toPacket(args.get(1));

                if (!PacketUtils.isPacket(packet)) {
                    CommandHandler.sendMessage(sender, String.format(UNKNOWN_PACKET_MESSAGE, packet));
                    return;
                }

                PacketsConfig.get().highlightedPackets.add(packet);
                CommandHandler.sendMessage(sender, String.format(HIGHLIGHT_MESSAGE, packet));
            }
            case "unhighlight", "uh" -> {
                var packet = this.toPacket(args.get(1));

                if (!PacketUtils.isPacket(packet)) {
                    CommandHandler.sendMessage(sender, String.format(UNKNOWN_PACKET_MESSAGE, packet));
                    return;
                }

                PacketsConfig.get().highlightedPackets.remove(packet);
                CommandHandler.sendMessage(sender, String.format(UNHIGHLIGHT_MESSAGE, packet));
            }
        }
    }

    /**
     * Attempts to get the packet by its ID.
     * If the packet is not found, the packet name is returned.
     *
     * @param packet The packet ID or name.
     * @return The packet name.
     */
    private String toPacket(String packet) {
        try {
            var packetId = Integer.parseInt(packet);
            return PacketOpcodesUtils.getOpcodeName(packetId);
        } catch (NumberFormatException ignored) {
            return packet;
        }
    }
}
