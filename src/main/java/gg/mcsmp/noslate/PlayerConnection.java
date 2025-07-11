package gg.mcsmp.noslate;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;

public class PlayerConnection extends ServerGamePacketListenerImpl {

    private final ServerPlayer player;

    public PlayerConnection(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
        this.player = player;
    }

    @Override
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
            ServerLevel level = (ServerLevel) player.level();
            LevelChunk chunk = level.getChunk(chunkPacket.getX(), chunkPacket.getZ());
            Packet<?> filtered = FilteredChunkSender.createFilteredChunkPacket(player, chunk);
            super.send(filtered);
        } else {
            super.send(packet);
        }
    }
}

