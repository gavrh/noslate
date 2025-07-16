package gavrh.noslate;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

public class YLevelListener implements Listener {
    private static final int RADIUS = 2;
    private static final int REVEAL_THRESHOLD_Y = 1;
    private final Plugin plugin;
    private final RegionScheduler scheduler;
    private final WeakHashMap<ServerPlayer, Set<ChunkPos>> revealedChunks = new WeakHashMap<>();
    private final WeakHashMap<ServerPlayer, Boolean> isBelowReveal = new WeakHashMap<>();

    public YLevelListener(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getRegionScheduler();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player bukkitPlayer = event.getPlayer();
        ServerPlayer player = ((CraftPlayer) bukkitPlayer).getHandle();
        double y = player.getBoundingBox().minY;

        boolean currentlyBelow = y <= REVEAL_THRESHOLD_Y;
        boolean wasBelow = isBelowReveal.getOrDefault(player, false);

        if (currentlyBelow && !wasBelow) {
            revealChunksAroundPlayer(player);
            isBelowReveal.put(player, true);
        } else if (!currentlyBelow && wasBelow) {
            hideAllBelowY0Chunks(player);
            isBelowReveal.put(player, false);
        } else if (currentlyBelow) {
            updateChunksForPlayerMovement(player);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        Consumer<ScheduledTask> task = (ScheduledTask scheduledTask) -> {
            ServerPlayer nms = ((CraftPlayer) player).getHandle();
            double y = nms.getBoundingBox().minY;
            if (y <= REVEAL_THRESHOLD_Y) {
                revealChunksAroundPlayer(nms);
                isBelowReveal.put(nms, true);
            } else {
                hideAllBelowY0Chunks(nms);
                isBelowReveal.put(nms, false);
            }
        };
        // longer delay to ensure anti-xray has processed chunks
        scheduler.runDelayed(plugin, to, task, 10L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();

        Consumer<ScheduledTask> task = (ScheduledTask scheduledTask) -> {
            ServerPlayer nms = ((CraftPlayer) player).getHandle();
            double y = nms.getBoundingBox().minY;
            if (y <= REVEAL_THRESHOLD_Y) {
                revealChunksAroundPlayer(nms);
                isBelowReveal.put(nms, true);
            } else {
                hideAllBelowY0Chunks(nms);
                isBelowReveal.put(nms, false);
            }
        };
        scheduler.runDelayed(plugin, loc, task, 10L);
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

        if (!toReveal.isEmpty() || !toHide.isEmpty()) {
            for (ChunkPos pos : toReveal) {
                resendFilteredChunk(player, pos);
            }
            for (ChunkPos pos : toHide) {
                resendFilteredChunk(player, pos);
            }
        }

        revealedChunks.put(player, newRevealed);
    }

    private void resendFilteredChunk(ServerPlayer player, ChunkPos pos) {
        try {
            ServerLevel level = (ServerLevel) player.level();
            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            ClientboundLevelChunkWithLightPacket packet = ChunkSender.createFilteredChunkPacket(player, chunk);
            player.connection.send(packet);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to resend filtered chunk at " + pos + " for player " + player.getName().getString());
        }
    }
}
