package gavrh.noslate;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;

public class ChunkFilterHandler extends ChannelOutboundHandlerAdapter {
    private final ServerPlayer player;

    public ChunkFilterHandler(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            if (msg instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
                try {
                    ServerLevel level = (ServerLevel) player.level();
                    LevelChunk chunk = level.getChunk(chunkPacket.getX(), chunkPacket.getZ());
                    Object filtered = ChunkSender.createFilteredChunkPacket(player, chunk);
                    if (filtered != null) {
                        super.write(ctx, filtered, promise);
                    } else {
                        super.write(ctx, msg, promise);
                    }
                } catch (UnsupportedOperationException e) {
                    Bukkit.getLogger().warning("UnsupportedOperationException in chunk filtering for player " + 
                        player.getName().getString() + " at chunk " + chunkPacket.getX() + "," + chunkPacket.getZ());
                    super.write(ctx, msg, promise);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Error filtering chunk packet for player " + 
                        player.getName().getString());
                    super.write(ctx, msg, promise);
                }
            } else {
                super.write(ctx, msg, promise);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Critical error in ChunkFilterHandler for player " + 
                player.getName().getString());
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Bukkit.getLogger().warning("Exception caught in ChunkFilterHandler: " + cause.getMessage());
        super.exceptionCaught(ctx, cause);
    }
}
