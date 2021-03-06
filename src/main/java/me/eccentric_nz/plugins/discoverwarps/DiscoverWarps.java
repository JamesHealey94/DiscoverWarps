package me.eccentric_nz.plugins.discoverwarps;

import java.io.File;
import net.milkbowl.vault.Vault;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscoverWarps extends JavaPlugin {

    private final DiscoverWarpsDatabase service = DiscoverWarpsDatabase.getInstance();
    private DiscoverWarpsCommands commands;
    private final PluginManager pm = Bukkit.getServer().getPluginManager();
    private DiscoverWarpsPlateListener plateListener;
    private DiscoverWarpsProtectionListener protectionListener;
    private DiscoverWarpsExplodeListener explodeListener;
    private Vault vault;
//    public Economy economy;
    private FileConfiguration config = null;
    final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
    final String MY_PLUGIN_NAME = ChatColor.GOLD + "[DiscoverWarps] " + ChatColor.RESET;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                console.sendMessage(MY_PLUGIN_NAME + " Could not create directory!");
                console.sendMessage(MY_PLUGIN_NAME + " Requires you to manually make the DiscoverWarps/ directory!");
            }
            getDataFolder().setWritable(true);
            getDataFolder().setExecutable(true);
        }
        this.saveDefaultConfig();
        try {
            String path = getDataFolder() + File.separator + "DiscoverWarps.db";
            service.setConnection(path);
            service.createTables();
        } catch (Exception e) {
            console.sendMessage(MY_PLUGIN_NAME + " Connection and Tables Error: " + e);
        }
        registerListeners();
        commands = new DiscoverWarpsCommands(this);
        getCommand("discoverwarps").setExecutor(commands);

        // check config
        new DiscoverWarpsConfig(this).checkConfig();

        if (!setupVault()) {
            pm.disablePlugin(this);
            return;
        }
        //setupEconomy();
    }

    @Override
    public void onDisable() {
        this.saveConfig();
        try {
            service.connection.close();
        } catch (Exception e) {
            debug("Could not close database connection: " + e);
        }
    }

    private boolean setupVault() {
        Plugin x = pm.getPlugin("Vault");
        if (x != null && x instanceof Vault) {
            vault = (Vault) x;
            return true;
        } else {
            // console.sendMessage("Vault is required for economy, but wasn't found!"); TODO update
            console.sendMessage("Download it from http://dev.bukkit.org/server-mods/vault/");
            console.sendMessage("Disabling plugin.");
            return false;
        }
    }
//
//    //Loading economy API from Vault
//    private boolean setupEconomy() {
//        if (getServer().getPluginManager().getPlugin("Vault") == null) {
//            return false;
//        }
//        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
//        if (rsp == null) {
//            return false;
//        }
//        economy = rsp.getProvider();
//        return economy != null;
//    }

    public void debug(Object o) {
        console.sendMessage("[DiscoverWarps Debug] " + o);
    }

    private void registerListeners() {
        plateListener = new DiscoverWarpsPlateListener(this);
        protectionListener = new DiscoverWarpsProtectionListener(this);
        explodeListener = new DiscoverWarpsExplodeListener(this);
        pm.registerEvents(plateListener, this);
        pm.registerEvents(protectionListener, this);
        pm.registerEvents(explodeListener, this);
    }
}