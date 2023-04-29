package io.grasscutter.packets;

import emu.grasscutter.server.http.Router;
import io.javalin.Javalin;

import java.io.IOException;

public final class PacketsRouter implements Router {
    private final byte[] packetViewer, shiconFont;

    public PacketsRouter() {
        this.packetViewer = this.readResource("/index.html");
        this.shiconFont = this.readResource("/shicon.ttf");
    }

    /**
     * Attempts to read a resource.
     *
     * @param path The path to the resource.
     * @return The resource's bytes.
     */
    private byte[] readResource(String path) {
        try (var stream = Packets.class
                .getResourceAsStream(path)) {
            if (stream == null)
                throw new IOException("Unable to load packet viewer.");

            return stream.readAllBytes();
        } catch (IOException ignored) {
            throw new RuntimeException("Unable to load packet viewer.");
        }
    }

    @Override public void applyRoutes(Javalin javalin) {
        javalin.get("/packets/viewer", ctx ->
                ctx
                        .contentType("text/html")
                        .result(this.packetViewer));
        javalin.get("/fonts/shicon.ttf", ctx ->
                ctx
                        .result(this.shiconFont));
    }
}
