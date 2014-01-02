package me.eccentric_nz.plugins.discoverwarps;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class DiscoverWarpsPlateListener implements Listener {

    final DiscoverWarps plugin;
    final DiscoverWarpsDatabase service = DiscoverWarpsDatabase.getInstance();
    final List<Material> validBlocks = new ArrayList<Material>();

    public DiscoverWarpsPlateListener(DiscoverWarps plugin) {
        this.plugin = plugin;
        validBlocks.add(Material.WOOD_PLATE);
        validBlocks.add(Material.STONE_PLATE);
    }

    @EventHandler
    public void onPlateStep(PlayerInteractEvent event) {
        final Action a = event.getAction();
        final Block b = event.getClickedBlock();
        if (a.equals(Action.PHYSICAL) && validBlocks.contains(b.getType())) {
            final Player p = event.getPlayer();
            final String name = p.getName();
            if (p.hasPermission("discoverwarps.use")) {
                final Location l = b.getLocation();
                final String w = l.getWorld().getName();
                final int x = l.getBlockX();
                final int y = l.getBlockY();
                final int z = l.getBlockZ();
                boolean discovered = false;
                boolean firstplate = true;
                Statement statement = null;
                ResultSet rsPlate = null;
                ResultSet rsPlayer = null;
                try {
                    final Connection connection = service.getConnection();
                    statement = connection.createStatement();
                    // get their current gamemode inventory from database
                    final String getQuery = "SELECT * FROM discoverwarps WHERE world = '" + w + "' AND x = " + x + " AND y = " + y + " AND z = " + z;
                    rsPlate = statement.executeQuery(getQuery);
                    if (rsPlate.next()) {
                        // is a discoverplate
                        final String id = rsPlate.getString("id");
                        final String warp = rsPlate.getString("name");
                        String queryDiscover = "";
                        // check whether they have visited this plate before
                        final String queryPlayer = "SELECT * FROM players WHERE player = '" + name + "'";
                        rsPlayer = statement.executeQuery(queryPlayer);
                        if (rsPlayer.next()) {
                            firstplate = false;
                            final String data = rsPlayer.getString("visited");
                            final String[] visited = data.split(",");
                            if (Arrays.asList(visited).contains(id)) {
                                discovered = true;
                            }
                            if (discovered == false) {
                                queryDiscover = "UPDATE players SET visited = '" + data + "," + id + "' WHERE player = '" + name + "'";
                            }
                        }
                        if (!discovered && firstplate) {
                            queryDiscover = "INSERT INTO players (player, visited) VALUES ('" + name + "','" + id + "')";
                        }
                        statement.executeUpdate(queryDiscover);
                        if (!discovered) {
                            p.sendMessage(ChatColor.GOLD + "[" + plugin.getConfig().getString("localisation.plugin_name") + "] " + ChatColor.RESET + String.format(plugin.getConfig().getString("localisation.discovered"), warp));
                        }
                        rsPlayer.close();
                        rsPlate.close();
                        statement.close();
                    }
                } catch (SQLException e) {
                    plugin.debug("Could not update player's visited data, " + e);
                } finally {
                    if (rsPlayer != null) {
                        try {
                            rsPlayer.close();
                        } catch (Exception e) {
                        }
                    }
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
}