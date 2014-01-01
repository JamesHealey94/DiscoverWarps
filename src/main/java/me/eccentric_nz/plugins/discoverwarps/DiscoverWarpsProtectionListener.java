package me.eccentric_nz.plugins.discoverwarps;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class DiscoverWarpsProtectionListener implements Listener {

    private final DiscoverWarps plugin;
    private final DiscoverWarpsDatabase service = DiscoverWarpsDatabase.getInstance();
    private final List<Material> validBlocks = new ArrayList<Material>();

    public DiscoverWarpsProtectionListener(DiscoverWarps plugin) {
        this.plugin = plugin;
        validBlocks.add(Material.WOOD_PLATE);
        validBlocks.add(Material.STONE_PLATE);
    }

    @EventHandler
    public void onPlateBreak(BlockBreakEvent event) {
        final Block b = event.getBlock();
        final Material m = b.getType();
        if (validBlocks.contains(m) || validBlocks.contains(b.getRelative(BlockFace.UP).getType())) {
            final Location l = b.getLocation();
            final String w = l.getWorld().getName();
            final int x = l.getBlockX();
            int y = l.getBlockY();
            if (validBlocks.contains(b.getRelative(BlockFace.UP).getType())) {
                y += 1;
            }
            final int z = l.getBlockZ();
            Statement statement = null;
            ResultSet rsPlate = null;
            try {
                final Connection connection = service.getConnection();
                statement = connection.createStatement();
                final String getQuery = "SELECT name FROM discoverwarps WHERE world = '" + w + "' AND x = " + x + " AND y = " + y + " AND z = " + z;
                rsPlate = statement.executeQuery(getQuery);
                if (rsPlate.isBeforeFirst()) {
                    final Player p = event.getPlayer();
                    p.sendMessage(ChatColor.GOLD + "[" + plugin.getConfig().getString("localisation.plugin_name") + "] " + ChatColor.RESET + String.format(plugin.getConfig().getString("localisation.no_break"), ChatColor.GREEN + "/dw delete [name]" + ChatColor.RESET));
                    event.setCancelled(true);
                }
            } catch (SQLException e) {
                plugin.debug("Could not find discover plate to protect, " + e);
            } finally {
                if (rsPlate != null) {
                    try {
                        rsPlate.close();
                    } catch (Exception e) {
                    }
                }
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}