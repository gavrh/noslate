package gavrh.noslate;

import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.server.level.ServerPlayer;

@DefaultQualifier(NonNull.class)
public final class Noslate extends JavaPlugin implements Listener {
    private final WeakHashMap<ServerPlayer, ChannelHandler> injectedHandlers = new WeakHashMap<>();
    private final RegionScheduler scheduler = Bukkit.getRegionScheduler();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new YLevelListener(this), this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ServerPlayer nms = ((CraftPlayer) player).getHandle();
        
        // longer delay to ensure anti-xray has set up its handlers first
        scheduler.runDelayed(this, player.getLocation(), (ScheduledTask task) -> {
            try {
                Channel channel = nms.connection.connection.channel;
                if (channel.pipeline().get("noslate_chunk_filter") != null) {
                    channel.pipeline().remove("noslate_chunk_filter");
                }
                
                ChunkFilterHandler handler = new ChunkFilterHandler(nms);
                
                // inject after anti-xray if it exists, otherwise before packet_handler
                if (channel.pipeline().get("anti-xray") != null) {
                    channel.pipeline().addAfter("anti-xray", "noslate_chunk_filter", handler);
                } else {
                    channel.pipeline().addBefore("packet_handler", "noslate_chunk_filter", handler);
                }
                
                injectedHandlers.put(nms, handler);
            } catch (Exception e) {
                getLogger().severe("Failed to inject packet handler for player " + player.getName());
                e.printStackTrace();
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ServerPlayer nms = ((CraftPlayer) event.getPlayer()).getHandle();
        ChannelHandler handler = injectedHandlers.remove(nms);
        if (handler != null) {
            try {
                Channel channel = nms.connection.connection.channel;
                if (channel.pipeline().get("noslate_chunk_filter") != null) {
                    channel.pipeline().remove("noslate_chunk_filter");
                }
            } catch (Exception e) {
                getLogger().warning("Failed to remove packet handler for player " + event.getPlayer().getName());
            }
        }
    }

    @Override
    public void onDisable() {
        for (ServerPlayer player : injectedHandlers.keySet()) {
            try {
                Channel channel = player.connection.connection.channel;
                if (channel.pipeline().get("noslate_chunk_filter") != null) {
                    channel.pipeline().remove("noslate_chunk_filter");
                }
            } catch (Exception e) {
                // ignore errors during shutdown
            }
        }
        injectedHandlers.clear();
    }
}
