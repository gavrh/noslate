package gg.mcsmp.noslate;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

public class YLevelListener implements Listener {

    private static final int RADIUS = 2;
    private static final int REVEAL_THRESHOLD_Y = 1; // Y level threshold for revealing

    private final WeakHashMap<ServerPlayer, Set<ChunkPos>> revealedChunks = new WeakHashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player bukkitPlayer = event.getPlayer();
        ServerPlayer player = ((CraftPlayer) bukkitPlayer).getHandle();

        double oldY = event.getFrom().getY();
        double newY = event.getTo().getY();

        boolean crossedDown = oldY >= REVEAL_THRESHOLD_Y && newY < REVEAL_THRESHOLD_Y;
        boolean crossedUp = oldY < REVEAL_THRESHOLD_Y && newY >= REVEAL_THRESHOLD_Y;

        if (crossedDown) {
            revealChunksAroundPlayer(player);
        } else if (crossedUp) {
            hideAllBelowY0Chunks(player);
        } else if (newY <= REVEAL_THRESHOLD_Y) {
            updateChunksForPlayerMovement(player);
        }
    }

    private void revealChunksAroundPlayer(ServerPlayer player) {
        ChunkPos playerChunkPos = player.chunkPosition();
        Set<ChunkPos> newRevealed = new HashSet<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                ChunkPos pos = new ChunkPos(playerChunkPos.x + dx, playerChunkPos.z + dz);
                resendFilteredChunk(player, pos);
                newRevealed.add(pos);
            }
        }
        revealedChunks.put(player, newRevealed);
    }

    private void hideAllBelowY0Chunks(ServerPlayer player) {
        Set<ChunkPos> currentlyRevealed = revealedChunks.remove(player);
        if (currentlyRevealed != null) {
            for (ChunkPos pos : currentlyRevealed) {
                resendFilteredChunk(player, pos);
            }
        }
    }

    private void updateChunksForPlayerMovement(ServerPlayer player) {
        ChunkPos playerChunkPos = player.chunkPosition();
        Set<ChunkPos> currentlyRevealed = revealedChunks.getOrDefault(player, new HashSet<>());
        Set<ChunkPos> newRevealed = new HashSet<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                newRevealed.add(new ChunkPos(playerChunkPos.x + dx, playerChunkPos.z + dz));
            }
        }

        Set<ChunkPos> toReveal = new HashSet<>(newRevealed);
        toReveal.removeAll(currentlyRevealed);

        Set<ChunkPos> toHide = new HashSet<>(currentlyRevealed);
        toHide.removeAll(newRevealed);

        for (ChunkPos pos : toReveal) {
            resendFilteredChunk(player, pos);
        }
        for (ChunkPos pos : toHide) {
            resendFilteredChunk(player, pos);
        }

        revealedChunks.put(player, newRevealed);
    }

    private void resendFilteredChunk(ServerPlayer player, ChunkPos pos) {
        ServerLevel level = (ServerLevel) player.level();
        LevelChunk chunk = level.getChunk(pos.x, pos.z);
        ClientboundLevelChunkWithLightPacket packet = ChunkSender.createFilteredChunkPacket(player, chunk);
        player.connection.send(packet);
    }
}
