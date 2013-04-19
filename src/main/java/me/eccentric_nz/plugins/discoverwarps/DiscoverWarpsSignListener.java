package me.eccentric_nz.plugins.discoverwarps;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
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

    DiscoverWarps plugin;
    DiscoverWarpsDatabase service = DiscoverWarpsDatabase.getInstance();

    public DiscoverWarpsSignListener(DiscoverWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        Action a = event.getAction();
        Block b = event.getClickedBlock();
        if (a.equals(Action.RIGHT_CLICK_BLOCK) && (b.getType().equals(Material.WALL_SIGN) || b.getType().equals(Material.SIGN_POST))) {
            plugin.debug("Sign clicked");
            Sign s = (Sign) b.getState();
            if (s.getLine(0).equalsIgnoreCase("[" + plugin.getConfig().getString("sign") + "]")) {
                plugin.debug("It's a DiscoverWarps sign");
                Player p = event.getPlayer();
                String name = p.getName();
                if (p.hasPermission("discoverwarps.use")) {
                    plugin.debug("Player has permission");
                    String plate = s.getLine(1);
                    Statement statement = null;
                    ResultSet rsPlate = null;
                    ResultSet rsPlayer = null;
                    try {
                        Connection connection = service.getConnection();
                        statement = connection.createStatement();
                        // get their current gamemode inventory from database
                        String getQuery = "SELECT * FROM discoverwarps WHERE name = '" + plate + "'";
                        rsPlate = statement.executeQuery(getQuery);
                        if (rsPlate.next()) {
                            plugin.debug("Found the plate");
                            // is a discoverplate
                            boolean enabled = rsPlate.getBoolean("enabled");
                            if (enabled) {
                                String id = rsPlate.getString("id");
                                String warp = rsPlate.getString("name");
                                World w = plugin.getServer().getWorld(rsPlate.getString("world"));
                                int x = rsPlate.getInt("x");
                                int y = rsPlate.getInt("y");
                                int z = rsPlate.getInt("z");
                                double cost = rsPlate.getDouble("cost");
                                String queryDiscover = "";
                                // check whether they have visited this plate before
                                String queryPlayer = "SELECT * FROM players WHERE player = '" + name + "'";
                                rsPlayer = statement.executeQuery(queryPlayer);
                                boolean firstplate = true;
                                boolean discovered = false;
                                if (rsPlayer.next()) {
                                    firstplate = false;
                                    String data = rsPlayer.getString("visited");
                                    String[] visited = data.split(",");
                                    if (Arrays.asList(visited).contains(id)) {
                                        discovered = true;
                                    }
                                    if (discovered == false) {
                                        // check if there is a cost
                                        if (cost > 0 && plugin.getConfig().getBoolean("allow_buying")) {
                                            // check if they have sufficient balance
                                            double bal = plugin.economy.getBalance(name);
                                            if (cost > bal) {
                                                p.sendMessage(DiscoverWarpsConstants.MY_PLUGIN_NAME + "You don't have enough maney to use this sign!");
                                                return;
                                            }
                                            plugin.economy.withdrawPlayer(name, cost);
                                            queryDiscover = "UPDATE players SET visited = '" + data + "," + id + "' WHERE player = '" + name + "'";
                                        }
                                    }
                                }
                                if (discovered == false && firstplate == true) {
                                    queryDiscover = "INSERT INTO players (player, visited) VALUES ('" + name + "','" + id + "')";
                                }
                                statement.executeUpdate(queryDiscover);
                                // warp to location
                                Location l = new Location(w, x, y, z);
                                l.setPitch(p.getLocation().getPitch());
                                l.setYaw(p.getLocation().getYaw());
                                DiscoverWarpsCommands dwc = new DiscoverWarpsCommands(plugin);
                                dwc.movePlayer(p, l, p.getLocation().getWorld());
                                plugin.debug("Warped!");
                                if (discovered == false) {
                                    p.sendMessage(DiscoverWarpsConstants.MY_PLUGIN_NAME + "You have discovered " + warp);
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
        String line1 = event.getLine(0);
        String firstline = "[" + plugin.getConfig().getString("sign") + "]";
        if (line1.equalsIgnoreCase(firstline)) {
            Player player = event.getPlayer();
            if (player.hasPermission("discoverwarps.admin")) {
                String line2 = event.getLine(1);
                Statement statement = null;
                ResultSet rsPlate = null;
                try {
                    Connection connection = service.getConnection();
                    statement = connection.createStatement();
                    // get their current gamemode inventory from database
                    String getQuery = "SELECT * FROM discoverwarps WHERE name = '" + line2 + "'";
                    rsPlate = statement.executeQuery(getQuery);
                    if (!rsPlate.next()) {
                        player.sendMessage(DiscoverWarpsConstants.MY_PLUGIN_NAME + "There is no DiscoverWrap with that name!");
                        event.setCancelled(true);
                        return;
                    }
                    double cost = rsPlate.getDouble("cost");
                    if (cost > 0) {
                        event.setLine(2, "" + cost);
                    }
                    player.sendMessage(DiscoverWarpsConstants.MY_PLUGIN_NAME + "Sign set successfully!");
                } catch (SQLException e) {
                    plugin.debug("Could not update player's visited data from sign, " + e);
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