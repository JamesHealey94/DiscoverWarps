package me.eccentric_nz.plugins.discoverwarps;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class DiscoverWarpsSignListener implements Listener {

    private final DiscoverWarps plugin;
    private final DiscoverWarpsDatabase service = DiscoverWarpsDatabase.getInstance();
    private final String plugin_name;

    public DiscoverWarpsSignListener(DiscoverWarps plugin) {
        this.plugin = plugin;
        plugin_name = ChatColor.GOLD + "[" + this.plugin.getConfig().getString("localisation.plugin_name") + "] " + ChatColor.RESET;
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        final Action a = event.getAction();
        final Block b = event.getClickedBlock();
        if (a.equals(Action.RIGHT_CLICK_BLOCK) && (b.getType().equals(Material.WALL_SIGN) || b.getType().equals(Material.SIGN_POST))) {
            final Sign s = (Sign) b.getState();
            if (s.getLine(0).equalsIgnoreCase("[" + plugin.getConfig().getString("sign") + "]")) {
                final Player p = event.getPlayer();
                final String name = p.getName();
                if (p.hasPermission("discoverwarps.use")) {
                    final String plate = s.getLine(1);
                    Statement statement = null;
                    ResultSet rsPlate = null;
                    ResultSet rsPlayer = null;
                    try {
                        final Connection connection = service.getConnection();
                        statement = connection.createStatement();
                        // get their current gamemode inventory from database
                        final String getQuery = "SELECT * FROM discoverwarps WHERE name = '" + plate + "'";
                        rsPlate = statement.executeQuery(getQuery);
                        if (rsPlate.next()) {
                            // is a discoverplate
                            final boolean enabled = rsPlate.getBoolean("enabled");
                            if (enabled) {
                                final String id = rsPlate.getString("id");
                                final String warp = rsPlate.getString("name");
                                final World w = plugin.getServer().getWorld(rsPlate.getString("world"));
                                final int x = rsPlate.getInt("x");
                                final int y = rsPlate.getInt("y");
                                final int z = rsPlate.getInt("z");
                                final double cost = rsPlate.getDouble("cost");
                                String queryDiscover = "";
                                // check whether they have visited this plate before
                                final String queryPlayer = "SELECT * FROM players WHERE player = '" + name + "'";
                                rsPlayer = statement.executeQuery(queryPlayer);
                                boolean firstplate = true;
                                boolean discovered = false;
                                if (rsPlayer.next()) {
                                    firstplate = false;
                                    final String data = rsPlayer.getString("visited");
                                    final String[] visited = data.split(",");
                                    if (Arrays.asList(visited).contains(id)) {
                                        discovered = true;
                                    }
                                    if (discovered == false) {
                                        // check if there is a cost
                                        if (cost > 0 && plugin.getConfig().getBoolean("allow_buying")) {
                                            // check if they have sufficient balance
                                            double bal = plugin.economy.getBalance(name);
                                            if (cost > bal) {
                                                p.sendMessage(plugin_name + plugin.getConfig().getString("localisation.signs.no_money"));
                                                return;
                                            }
                                            plugin.economy.withdrawPlayer(name, cost);
                                            queryDiscover = "UPDATE players SET visited = '" + data + "," + id + "' WHERE player = '" + name + "'";
                                        } else {
                                            p.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.signs.needs_discover"), warp));
                                            return;
                                        }
                                    }
                                }
                                if (discovered == false && firstplate == true) {
                                    queryDiscover = "INSERT INTO players (player, visited) VALUES ('" + name + "','" + id + "')";
                                }
                                statement.executeUpdate(queryDiscover);
                                // warp to location
                                final Location l = new Location(w, x, y, z);
                                l.setPitch(p.getLocation().getPitch());
                                l.setYaw(p.getLocation().getYaw());
                                final DiscoverWarpsCommands dwc = new DiscoverWarpsCommands(plugin);
                                dwc.movePlayer(p, l, p.getLocation().getWorld());
                                if (discovered == false) {
                                    p.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.discovered"), warp));
                                }
                                rsPlayer.close();
                                rsPlate.close();
                                statement.close();
                            }
                        }
                    } catch (SQLException e) {
                        plugin.debug("Could not update player's visited data from sign, " + e);
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

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        final String line1 = event.getLine(0);
        final String firstline = "[" + plugin.getConfig().getString("sign") + "]";
        if (line1.equalsIgnoreCase(firstline)) {
            final Player player = event.getPlayer();
            if (player.hasPermission("discoverwarps.admin")) {
                final String line2 = event.getLine(1);
                Statement statement = null;
                ResultSet rsPlate = null;
                try {
                    final Connection connection = service.getConnection();
                    statement = connection.createStatement();
                    // get their current gamemode inventory from database
                    final String getQuery = "SELECT * FROM discoverwarps WHERE name = '" + line2 + "'";
                    rsPlate = statement.executeQuery(getQuery);
                    if (!rsPlate.next()) {
                        player.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_plate_name"));
                        event.setCancelled(true);
                        return;
                    }
                    final double cost = rsPlate.getDouble("cost");
                    if (cost > 0) {
                        event.setLine(2, "" + cost);
                    }
                    player.sendMessage(plugin_name + plugin.getConfig().getString("localisation.signs.sign_made"));
                } catch (SQLException e) {
                    plugin.debug("Could not get data for sign, " + e);
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
}