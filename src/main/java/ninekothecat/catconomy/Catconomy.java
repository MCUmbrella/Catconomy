package ninekothecat.catconomy;

import net.milkbowl.vault.economy.Economy;
import ninekothecat.catconomy.commands.balance.BalanceCommandExecutor;
import ninekothecat.catconomy.commands.balance.BalanceTabAutocomplete;
import ninekothecat.catconomy.commands.catconomycommand.CatEconomyCommand;
import ninekothecat.catconomy.commands.catconomycommand.CatEconomyCommandHandler;
import ninekothecat.catconomy.commands.catconomycommand.CatEconomyCommandHandlerAutoCompleter;
import ninekothecat.catconomy.commands.deposit.DepositCommandExecutor;
import ninekothecat.catconomy.commands.give.GiveCommandExecutor;
import ninekothecat.catconomy.commands.take.TakeCommandExecutor;
import ninekothecat.catconomy.defaultImplementations.CatBalanceHandler;
import ninekothecat.catconomy.defaultImplementations.CatPermissionGuard;
import ninekothecat.catconomy.defaultImplementations.CatPrefix;
import ninekothecat.catconomy.defaultImplementations.database.CatMapDBDatabase;
import ninekothecat.catconomy.defaultImplementations.database.SQL.CatSQLDatabase;
import ninekothecat.catconomy.enums.DefaultDatabaseType;
import ninekothecat.catconomy.eventlisteners.CatPlayerJoinHandler;
import ninekothecat.catconomy.integrations.CatVaultIntegration;
import ninekothecat.catconomy.interfaces.*;
import ninekothecat.catconomy.logging.CatLogger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

public final class Catconomy extends JavaPlugin {
    public static IPermissionGuard permissionGuard;
    public static IDatabase database;
    public static Logger logger;
    public static ICurrencyPrefix prefix;
    public static CatEconomyCommandHandler catEconomyCommandHandler;
    static IBalanceHandler balanceHandler;
    public static ICatLogger iCatLogger;

    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            this.getLogger().info("Found vault plugin! Enabling");
            enableVaultIntegration();
        }
        setDatabase();
        setPrefix();
        final ServicesManager servicesManager = this.getServer().getServicesManager();
        servicesManager.register(IBalanceHandler.class,new CatBalanceHandler(this.getConfig().getBoolean("do_logs",true)),this,ServicePriority.Low );
        servicesManager.register(IPermissionGuard.class,new CatPermissionGuard(),this,ServicePriority.Low);
        servicesManager.register(ICatLogger.class, new CatLogger(),this,ServicePriority.Low);


    }

    public static IBalanceHandler getBalanceHandler() {
        return balanceHandler;
    }

    public static void setBalanceHandler(IBalanceHandler balanceHandler) {
        Catconomy.balanceHandler = balanceHandler;
    }

    private static void makeCatConomyCommand(String name, Permission permission) {
        final CatEconomyCommand catEconomyCommand = new CatEconomyCommand(name);
        catEconomyCommand.setPermission(permission);
        catEconomyCommandHandler.put(name, catEconomyCommand);
    }

    @Nullable
    public static Player getPlayerFromName(String playerName) {
        Player player = null;
        for (Player player1 : Bukkit.getServer().getOnlinePlayers())
            if (player1.getDisplayName().toUpperCase(Locale.ROOT).equals(playerName.toUpperCase(Locale.ROOT)))
                player = player1;
        if (player == null) {
            for (OfflinePlayer player1 : Bukkit.getServer().getOfflinePlayers())
                if (Objects.requireNonNull(player1.getName()).toUpperCase(Locale.ROOT).equals(playerName.toUpperCase(Locale.ROOT)))
                    player = player1.getPlayer();
        }
        return player;
    }

    @Override
    public void onEnable() {
        //Bstats
        int pluginID = 13186;
        Metrics metrics = new Metrics(this,pluginID);
        // Plugin startup logic
        logger = this.getLogger();
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            this.saveDefaultConfig();
        }

        logger.info("Loading services...");
        if (loadServices()) return;

        catEconomyCommandHandler = new CatEconomyCommandHandler();
        final CatPlayerJoinHandler catPlayerJoinHandler = new CatPlayerJoinHandler(this.getConfig().getDouble("starting_amount", 1000));
        registerBukkitCommands();
        makeCatConomyCommand("give",Bukkit.getPluginManager().getPermission("catconomy.give"));
        makeCatConomyCommand("take", Bukkit.getPluginManager().getPermission("catconomy.subtract"));
        catEconomyCommandHandler.get("give").setExecutor(new GiveCommandExecutor());
        catEconomyCommandHandler.get("take").setExecutor(new TakeCommandExecutor());

        this.getServer().getPluginManager().registerEvents(catPlayerJoinHandler, this);
    }

    private void registerBukkitCommands() {
        Objects.requireNonNull(this.getCommand("balance")).setTabCompleter(new BalanceTabAutocomplete());
        Objects.requireNonNull(this.getCommand("balance")).setExecutor(new BalanceCommandExecutor());
        Objects.requireNonNull(this.getCommand("deposit")).setExecutor(new DepositCommandExecutor());
        Objects.requireNonNull(this.getCommand("catconomy")).setExecutor(catEconomyCommandHandler);
        Objects.requireNonNull(this.getCommand("catconomy")).setTabCompleter(new CatEconomyCommandHandlerAutoCompleter());
    }

    private boolean loadServices() {
        final ServicesManager servicesManager = this.getServer().getServicesManager();

        balanceHandler = servicesManager.load(IBalanceHandler.class);
        if (balanceHandler == null){
            logger.severe("Balance handler couldn't be loaded");
            this.getServer().getPluginManager().disablePlugin(this);
            return true;
        }
        iCatLogger = servicesManager.load(ICatLogger.class);
        if (iCatLogger == null){
            logger.severe("Logger could not be loaded");
            this.getServer().getPluginManager().disablePlugin(this);
            return true;
        }
        permissionGuard = servicesManager.load(IPermissionGuard.class);
        if (permissionGuard == null){
            logger.severe("permission guard could not be loaded");
            this.getServer().getPluginManager().disablePlugin(this);
            return true;
        }
        database = servicesManager.load(IDatabase.class);
        if (database == null){
            logger.severe("database could not be loaded");
            this.getServer().getPluginManager().disablePlugin(this);
            return true;
        }
        prefix = servicesManager.load(ICurrencyPrefix.class);
        if (prefix == null){
            logger.severe("prefix could not be loaded");
            this.getServer().getPluginManager().disablePlugin(this);
            return true;
        }
        logger.info("Loaded all services!");
        return false;
    }

    private void enableVaultIntegration() {
        this.getServer().getServicesManager().register(Economy.class,
                new CatVaultIntegration(),
                this, ServicePriority.High);
        this.getLogger().info("Registered Vault Integration");
    }

    private void setPrefix() {
        final ServicesManager servicesManager = this.getServer().getServicesManager();

        if (this.getConfig().contains("Sprefix") && this.getConfig().contains("Lprefix")) {
            servicesManager.register(ICurrencyPrefix.class,new CatPrefix(this.getConfig().getString("Sprefix"), this.getConfig().getString("Lprefix"))  ,this, ServicePriority.Low);
        } else {
            servicesManager.register(ICurrencyPrefix.class, new CatPrefix(this.getConfig().getString("Sprefix")),this,ServicePriority.Low);
        }
    }

    private void setDatabase() {
        try {
            switch (DefaultDatabaseType.valueOf(this.getConfig().getString("database"))) {
                case MapDBFile:
                    this.getServer().getServicesManager().register(IDatabase.class,new CatMapDBDatabase(),this,ServicePriority.Low);
                    break;
                case SQL:
                    YamlConfiguration yamlConfiguration = loadConfiguration("Sql.yml");
                    final CatSQLDatabase catSQLDatabase = new CatSQLDatabase(yamlConfiguration.getString("user"),
                            yamlConfiguration.getString("password"),
                            yamlConfiguration.getString("host"),
                            yamlConfiguration.getString("database_name"),
                            yamlConfiguration.getString("port"));
                    this.getServer().getServicesManager().register(IDatabase.class, catSQLDatabase,this,ServicePriority.Low);
                    break;
            }
        } catch (IllegalArgumentException | SQLException ignored) {}
    }
    private YamlConfiguration loadConfiguration(String file) {
        File file1 = new File(this.getDataFolder(), file);
        if (!file1.exists()) {
            this.saveResource(file, false);
        }
        return YamlConfiguration.loadConfiguration(file1);
    }
}
