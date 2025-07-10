package gg.mcsmp.deepslatehider;

import java.lang.reflect.Field;
import java.util.WeakHashMap;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@DefaultQualifier(NonNull.class)
public final class DeepslateHider extends JavaPlugin implements Listener {

    private final WeakHashMap<ServerPlayer, ServerGamePacketListenerImpl> originalConnections = new WeakHashMap<>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new PlayerYLevelListener(), this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ServerPlayer nms = ((CraftPlayer) player).getHandle();

        // backup original connection
        if (!originalConnections.containsKey(nms)) {
            originalConnections.put(nms, nms.connection);
        }

        try {
            // grab server instance
            Field serverField = ServerPlayer.class.getDeclaredField("server");
            serverField.setAccessible(true);
            MinecraftServer server = (MinecraftServer) serverField.get(nms);

            Connection conn = nms.connection.connection;
            GameProfile profile = nms.getGameProfile();
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);

            CustomPlayerConnection custom = new CustomPlayerConnection(server, conn, nms, cookie);
            nms.connection = custom;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ServerPlayer nms = ((CraftPlayer) event.getPlayer()).getHandle();

        // restore the original connection to avoid crashes
        ServerGamePacketListenerImpl original = originalConnections.remove(nms);
        if (original != null) {
            nms.connection = original;
        }
    }
}

