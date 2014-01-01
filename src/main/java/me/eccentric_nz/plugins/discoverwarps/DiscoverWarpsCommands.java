package me.eccentric_nz.plugins.discoverwarps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscoverWarpsCommands implements CommandExecutor {

    private final DiscoverWarps plugin;
    final List<String> admincmds;
    final List<String> usercmds;
    final DiscoverWarpsDatabase service = DiscoverWarpsDatabase.getInstance();
    final String plugin_name;
    final List<Material> validBlocks = new ArrayList<Material>();

    public DiscoverWarpsCommands(DiscoverWarps plugin) {
        this.plugin = plugin;

        this.admincmds = new ArrayList<String>();
        admincmds.add("set");
        admincmds.add("delete");
        admincmds.add("enable");
        admincmds.add("disable");
        admincmds.add("cost");
        admincmds.add("allow_buying");

        this.usercmds = new ArrayList<String>();
        usercmds.add("tp");
        usercmds.add("list");
        usercmds.add("buy");

        plugin_name = ChatColor.GOLD + "[" + this.plugin.getConfig().getString("localisation.plugin_name") + "] " + ChatColor.RESET;
        validBlocks.add(Material.WOOD_PLATE);
        validBlocks.add(Material.STONE_PLATE);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("discoverwarps")) {
            if (args.length == 0) {
                final String HELP
                        = plugin.getConfig().getString("localisation.help.set") + ":\n" + ChatColor.GREEN + "/dw set [name]" + ChatColor.RESET
                        + "\n" + plugin.getConfig().getString("localisation.help.delete") + ":\n" + ChatColor.GREEN + "/dw delete [name]" + ChatColor.RESET
                        + "\n" + plugin.getConfig().getString("localisation.help.disable") + ":\n" + ChatColor.GREEN + "/dw disable [name]" + ChatColor.RESET
                        + "\n" + plugin.getConfig().getString("localisation.help.enable") + ":\n" + ChatColor.GREEN + "/dw enable [name]" + ChatColor.RESET
                        + "\n" + plugin.getConfig().getString("localisation.help.cost") + ":\n" + ChatColor.GREEN + "/dw cost [name] [amount]" + ChatColor.RESET
                        + "\n" + plugin.getConfig().getString("localisation.help.list") + ":\n" + ChatColor.GREEN + "/dw list" + ChatColor.RESET
                        + "\n" + plugin.getConfig().getString("localisation.help.warp") + ":\n" + ChatColor.GREEN + "/dw tp [name]" + ChatColor.RESET
                        + "\n" + plugin.getConfig().getString("localisation.help.buy") + ":\n" + ChatColor.GREEN + "/dw buy [name]" + ChatColor.RESET
                        + "\n" + plugin.getConfig().getString("localisation.help.config") + ":\n" + ChatColor.GREEN + "/dw [config setting name]" + ChatColor.RESET + " e.g. /dw allow_buying";
                sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.help"));
                sender.sendMessage("------------");
                sender.sendMessage(HELP.split("\n"));
                return true;
            }
            if (admincmds.contains(args[0])) {
                if (!sender.hasPermission("discoverwarps.admin")) {
                    sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.permission"));
                    return true;
                }
                if (args[0].equalsIgnoreCase("allow_buying")) {
                    final boolean bool = !plugin.getConfig().getBoolean("allow_buying");
                    plugin.getConfig().set("allow_buying", bool);
                    final String boolString = (bool) ? plugin.getConfig().getString("localisation.commands.str_true") : plugin.getConfig().getString("localisation.commands.str_false");
                    sender.sendMessage(plugin_name + "allow_buying " + String.format(plugin.getConfig().getString("localisation.config"), boolString));
                    if (bool) {
                        sender.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.commands.restart"), plugin_name));
                    }
                    plugin.saveConfig();
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.arguments"));
                    return false;
                }
                if (args[0].equalsIgnoreCase("set")) {
                    if (sender instanceof Player) {
                        final Player p = (Player) sender;
                        final Location l = p.getLocation();
                        //l.setY(l.getY() - .2);
                        final Block b = l.getBlock();
                        // check player is standing on pressure plate
                        final Material m = b.getType();
                        if (!validBlocks.contains(m)) {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.not_plate"));
                            return true;
                        }
                        try {
                            final Connection connection = service.getConnection();
                            final Statement statement = connection.createStatement();
                            final String queryName = "SELECT name FROM discoverwarps WHERE name = '" + args[1] + "'";
                            final ResultSet rsName = statement.executeQuery(queryName);
                            // check name is not in use
                            if (rsName.isBeforeFirst()) {
                                sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.name_in_use"));
                                return true;
                            }
                            final String w = b.getLocation().getWorld().getName();
                            final int x = b.getLocation().getBlockX();
                            final int y = b.getLocation().getBlockY();
                            final int z = b.getLocation().getBlockZ();
                            final PreparedStatement ps = connection.prepareStatement("INSERT INTO discoverwarps (name, world, x, y, z, enabled) VALUES (?, ?, ?, ?, ?, 1)");
                            ps.setString(1, args[1]);
                            ps.setString(2, w);
                            ps.setInt(3, x);
                            ps.setInt(4, y);
                            ps.setInt(5, z);
                            ps.executeUpdate();
                            sender.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.commands.added"), args[1]));
                        } catch (SQLException e) {
                            plugin.debug("Could not insert new discover plate, " + e);
                        }
                    } else {
                        sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.only_player"));
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("delete")) {
                    try {
                        final Connection connection = service.getConnection();
                        final Statement statement = connection.createStatement();
                        final String queryName = "SELECT * FROM discoverwarps WHERE name = '" + args[1] + "'";
                        final ResultSet rsName = statement.executeQuery(queryName);
                        // check name is valid
                        if (rsName.next()) {
                            final World w = plugin.getServer().getWorld(rsName.getString("world"));
                            final int x = rsName.getInt("x");
                            final int y = rsName.getInt("y");
                            final int z = rsName.getInt("z");
                            final String queryDel = "DELETE FROM discoverwarps WHERE name = '" + args[1] + "'";
                            statement.executeUpdate(queryDel);
                            final Block b = w.getBlockAt(x, y, z);
                            b.setTypeId(0);
                            sender.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.commands.deleted"), args[1]));
                            return true;
                        } else {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_plate_name"));
                            return true;
                        }
                    } catch (SQLException e) {
                        plugin.debug("Could not delete discover plate, " + e);
                    }
                }
                if (args[0].equalsIgnoreCase("enable")) {
                    try {
                        final Connection connection = service.getConnection();
                        final Statement statement = connection.createStatement();
                        final String queryName = "SELECT name FROM discoverwarps WHERE name = '" + args[1] + "'";
                        final ResultSet rsName = statement.executeQuery(queryName);
                        // check name is valid
                        if (rsName.isBeforeFirst()) {
                            final String queryDel = "UPDATE discoverwarps SET enabled = 1 WHERE name = '" + args[1] + "'";
                            statement.executeUpdate(queryDel);
                            sender.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.commands.enabled"), args[1]));
                            return true;
                        } else {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_plate_name"));
                            return true;
                        }
                    } catch (SQLException e) {
                        plugin.debug("Could not enable discover plate, " + e);
                    }
                }
                if (args[0].equalsIgnoreCase("disable")) {
                    try {
                        final Connection connection = service.getConnection();
                        final Statement statement = connection.createStatement();
                        final String queryName = "SELECT name FROM discoverwarps WHERE name = '" + args[1] + "'";
                        final ResultSet rsName = statement.executeQuery(queryName);
                        // check name is valid
                        if (rsName.isBeforeFirst()) {
                            final String queryDel = "UPDATE discoverwarps SET enabled = 0 WHERE name = '" + args[1] + "'";
                            statement.executeUpdate(queryDel);
                            sender.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.commands.disabled"), args[1]));
                            return true;
                        } else {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_plate_name"));
                            return true;
                        }
                    } catch (SQLException e) {
                        plugin.debug("Could not disable discover plate, " + e);
                    }
                }
                if (args[0].equalsIgnoreCase("cost")) {
                    try {
                        final Connection connection = service.getConnection();
                        final Statement statement = connection.createStatement();
                        final String queryCost = "SELECT name FROM discoverwarps WHERE name = '" + args[1] + "'";
                        final ResultSet rsCost = statement.executeQuery(queryCost);
                        // check name is valid
                        if (rsCost.isBeforeFirst()) {
                            int cost;
                            try {
                                cost = Integer.parseInt(args[2]);
                            } catch (NumberFormatException nfe) {
                                sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.cost"));
                                return true;
                            }
                            final String queryDel = "UPDATE discoverwarps SET cost = " + cost + " WHERE name = '" + args[1] + "'";
                            statement.executeUpdate(queryDel);
                            sender.sendMessage(plugin_name + "DiscoverPlate " + args[1] + " now costs " + cost + " to buy!");
                            return true;
                        } else {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_plate_name"));
                            return true;
                        }
                    } catch (SQLException e) {
                        plugin.debug("Could not set discover plate cost, " + e);
                    }
                }
            }
            if (usercmds.contains(args[0])) {
                if (!sender.hasPermission("discoverwarps.use")) {
                    sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.permission"));
                    return true;
                }
                if (args[0].equalsIgnoreCase("list")) {
                    try {
                        final Connection connection = service.getConnection();
                        final Statement statement = connection.createStatement();
                        List<String> visited = new ArrayList<String>();
                        if (sender instanceof Player) {
                            final Player player = (Player) sender;
                            final String p = player.getName();
                            // get players visited plates
                            final String queryVisited = "SELECT visited FROM players WHERE player = '" + p + "'";
                            final ResultSet rsVisited = statement.executeQuery(queryVisited);
                            if (rsVisited.isBeforeFirst()) {
                                visited = Arrays.asList(rsVisited.getString("visited").split(","));
                            }
                        }
                        final String queryList = "SELECT id, name, cost FROM discoverwarps WHERE enabled = 1";
                        final ResultSet rsList = statement.executeQuery(queryList);
                        // check name is valid
                        if (rsList.isBeforeFirst()) {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.list"));
                            sender.sendMessage("------------");
                            String discovered;
                            for (int i = 1; rsList.next(); i++) {
                                discovered = (visited.contains(rsList.getString("id"))) ? ChatColor.GREEN + plugin.getConfig().getString("localisation.visited") : ChatColor.RED + plugin.getConfig().getString("localisation.not_visited");
                                final String warp = rsList.getString("name");
                                String cost = "";
                                if (plugin.getConfig().getBoolean("allow_buying") && !visited.contains(rsList.getString("id"))) {
                                    final int amount = rsList.getInt("cost");
                                    if (amount > 0) {
                                        cost = ChatColor.RESET + " [" + plugin.economy.format(amount) + "]";
                                    }
                                }
                                sender.sendMessage(i + ". " + warp + " " + discovered + cost);
                            }
                            sender.sendMessage("------------");
                            return true;
                        } else {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.none_set"));
                            return true;
                        }
                    } catch (SQLException e) {
                        plugin.debug("Could not list discover plates, " + e);
                    }
                }
                if (args[0].equalsIgnoreCase("tp")) {
                    Player player;
                    if (sender instanceof Player) {
                        player = (Player) sender;
                    } else {
                        sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.only_player"));
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_warp_name"));
                        return false;
                    }
                    try {
                        final Connection connection = service.getConnection();
                        final Statement statement = connection.createStatement();
                        final String queryName = "SELECT * FROM discoverwarps WHERE name = '" + args[1] + "' COLLATE NOCASE";
                        final ResultSet rsName = statement.executeQuery(queryName);
                        // check name is valid
                        if (rsName.next()) {
                            final String id = rsName.getString("id");
                            final String warp = rsName.getString("name");
                            final World w = plugin.getServer().getWorld(rsName.getString("world"));
                            final int x = rsName.getInt("x");
                            final int y = rsName.getInt("y");
                            final int z = rsName.getInt("z");
                            List<String> visited = new ArrayList<String>();
                            // can the player tp to here?
                            final String p = player.getName();
                            // get players visited plates
                            final String queryVisited = "SELECT visited FROM players WHERE player = '" + p + "'";
                            final ResultSet rsVisited = statement.executeQuery(queryVisited);
                            if (rsVisited.isBeforeFirst()) {
                                visited = Arrays.asList(rsVisited.getString("visited").split(","));
                            }
                            if (!visited.contains(id)) {
                                sender.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.commands.needs_discover"), warp));
                                return true;
                            }
                            final World from = player.getLocation().getWorld();
                            final Location l = new Location(w, x, y, z);
                            l.setPitch(player.getLocation().getPitch());
                            l.setYaw(player.getLocation().getYaw());
                            movePlayer(player, l, from);
                            return true;
                        } else {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_plate_name"));
                            return true;
                        }
                    } catch (SQLException e) {
                        plugin.debug("Could not find discover plate record, " + e);
                    }
                }
                if (args[0].equalsIgnoreCase("buy")) {
                    if (!plugin.getConfig().getBoolean("allow_buying")) {
                        sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.buying.no_buying"));
                        return true;
                    }
                    Player player;
                    if (sender instanceof Player) {
                        player = (Player) sender;
                    } else {
                        sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.only_player"));
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_warp_name"));
                        return false;
                    }
                    try {
                        final Connection connection = service.getConnection();
                        final Statement statement = connection.createStatement();
                        final String queryBuy = "SELECT * FROM discoverwarps WHERE name = '" + args[1] + "'";
                        final ResultSet rsBuy = statement.executeQuery(queryBuy);
                        // check name is valid
                        if (rsBuy.next()) {
                            boolean firstplate = true;
                            final double cost = rsBuy.getDouble("cost");
                            if (cost <= 0) {
                                sender.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.buying.cannot_buy"), args[1]));
                                return true;
                            }
                            final String p = player.getName();
                            // check they have sufficient balance
                            final double bal = plugin.economy.getBalance(p);
                            if (cost > bal) {
                                player.sendMessage(plugin_name + plugin.getConfig().getString("localisation.buying.no_money"));
                                return true;
                            }
                            final String id = rsBuy.getString("id");
                            String queryDiscover = "";
                            // check whether they have visited this plate before
                            final String queryPlayer = "SELECT * FROM players WHERE player = '" + p + "'";
                            final ResultSet rsPlayer = statement.executeQuery(queryPlayer);
                            if (rsPlayer.next()) {
                                firstplate = false;
                                final String data = rsPlayer.getString("visited");
                                final String[] visited = data.split(",");
                                if (Arrays.asList(visited).contains(id)) {
                                    sender.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.buying.no_need"), args[1]));
                                    return true;
                                }
                                queryDiscover = "UPDATE players SET visited = '" + data + "," + id + "' WHERE player = '" + p + "'";
                            }
                            if (firstplate) {
                                queryDiscover = "INSERT INTO players (player, visited) VALUES ('" + p + "','" + id + "')";
                            }
                            statement.executeUpdate(queryDiscover);
                            plugin.economy.withdrawPlayer(p, cost);
                            player.sendMessage(plugin_name + String.format(plugin.getConfig().getString("localisation.buying.bought"), args[1]) + " " + cost);
                            return true;
                        } else {
                            sender.sendMessage(plugin_name + plugin.getConfig().getString("localisation.commands.no_plate_name"));
                            return true;
                        }
                    } catch (SQLException e) {
                        plugin.debug("Could not buy discover plate, " + e);
                    }
                }
            }
        }
        return false;
    }

    public void movePlayer(Player player, Location loc, World from) {

        player.sendMessage(plugin_name + plugin.getConfig().getString("localisation.teleport") + "...");

        final Player thePlayer = player;
        final Location theLocation = loc;

        // adjust location to centre of plate
        theLocation.setX(loc.getX() + 0.5);
        theLocation.setZ(loc.getZ() + 0.5);

        // try loading chunk
        final World world = loc.getWorld();
        final Chunk chunk = world.getChunkAt(loc);
        if (!world.isChunkLoaded(chunk)) {
            world.loadChunk(chunk);
        }

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                thePlayer.teleport(theLocation);
                thePlayer.getWorld().playSound(theLocation, Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            }
        }, 5L);
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                thePlayer.teleport(theLocation);
                if (plugin.getConfig().getBoolean("no_damage")) {
                    thePlayer.setNoDamageTicks(plugin.getConfig().getInt("no_damage_time") * 20);
                }
            }
        }, 10L);
    }
}