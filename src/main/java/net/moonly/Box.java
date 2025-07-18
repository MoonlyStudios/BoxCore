package net.moonly;

import net.moonly.commands.player.BaltopCommand;
import net.moonly.commands.player.PayCommand;
import net.moonly.commands.player.SpawnCommand;
import net.moonly.commands.staff.*;
import net.moonly.handlers.AutoRespawnListener;
import net.moonly.modules.Claims.ClaimListener;
import net.moonly.modules.Claims.ClaimManager;
import net.moonly.modules.Claims.ClaimSelectionListener;
import net.moonly.modules.Economy.placeholder.EconomyPlaceholder;
import net.moonly.modules.Kit.KitListener;
import net.moonly.modules.Kit.KitManager;
import net.moonly.modules.Spawn.SpawnListener;
import net.moonly.modules.Spawn.SpawnManager;
import net.moonly.modules.TemporaryBlocks.TemporaryBlockListener;
import net.moonly.modules.TemporaryBlocks.TemporaryBlockManager;
import net.moonly.modules.scoreboard.ScoreboardService;
import net.moonly.modules.deathmessage.DeathMessageManager;
import net.moonly.modules.deathmessage.GUIManager;
import net.moonly.modules.deathmessage.DeathListener;
import net.moonly.commands.player.DeathMessageCommand;

// Nuevas importaciones para el módulo de economía
import net.moonly.modules.Economy.EconomyManager;
import net.moonly.modules.Economy.EconomyListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class Box extends JavaPlugin implements Listener {

    public static Box instance;

    private static final int bstatsId = 20215;

    private KitManager kitManager;
    private boolean enableKits;

    private TemporaryBlockManager temporaryBlockManager;
    private boolean enableTemporaryBlocks;

    private SpawnManager spawnManager;
    private boolean enableSpawn;

    private ClaimManager claimManager;
    private boolean enableClaims;

    private ScoreboardService scoreboardService;
    private boolean enableScoreboard;
    private CustomTimerCommand customTimerCommand;
    private RebootService rebootService;

    private DeathMessageManager deathMessageManager;
    private GUIManager guiManager;
    private boolean enableDeathMessages;

    private AutoRespawnListener autoRespawnListener;
    private boolean enableAutoRespawn;
    // private int autoRespawnDelayTicks; // Removido si el listener lo gestiona internamente

    // >>>>> INICIO: Nuevas propiedades para el módulo de Economía <<<<<
    private EconomyManager economyManager;
    private boolean enableEconomy;
    // >>>>> FIN: Nuevas propiedades para el módulo de Economía <<<<<

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();
        loadConfigSettings();

        getLogger().info("BoxCore loaded successfully.");

        this.customTimerCommand = new CustomTimerCommand(this);
        if (getCommand("customtimer") != null) {
            getCommand("customtimer").setExecutor(this.customTimerCommand);
            getLogger().info("'/customtimer' command enabled.");
        } else {
            getLogger().severe("Command 'customtimer' not defined in plugin.yml. CustomTimer module command could not be enabled.");
        }

        if (getCommand("reboot") != null) {
            this.rebootService = new RebootService(this, this.customTimerCommand);
            getCommand("reboot").setExecutor(this.rebootService);
            getLogger().info("'/reboot' command enabled.");
        } else {
            getLogger().severe("Command 'reboot' not defined in plugin.yml. Reboot module command could not be enabled.");
        }

        if (enableKits) {
            this.kitManager = new KitManager(this);
            if (getCommand("kit") != null) {
                getCommand("kit").setExecutor(new KitCommand(this, kitManager));
                getLogger().info("Kits module enabled and '/kit' command registered.");
            } else {
                getLogger().severe("Command 'kit' not defined in plugin.yml. Kits module could not be enabled.");
            }
            getServer().getPluginManager().registerEvents(new KitListener(kitManager), this);
            getLogger().info("Kits Listener registered for Kits module.");
        } else {
            getLogger().info("Kits module disabled in config.yml.");
        }

        if (enableTemporaryBlocks) {
            this.temporaryBlockManager = new TemporaryBlockManager(this);
            getServer().getPluginManager().registerEvents(new TemporaryBlockListener(temporaryBlockManager), this);
            getLogger().info("Temporary Blocks module enabled.");
        } else {
            getLogger().info("Temporary Blocks module disabled in config.yml.");
        }

        // 5. Spawn Module (Initialize BEFORE Auto-Respawn)
        if (enableSpawn) {
            try {
                this.spawnManager = new SpawnManager(this);
                if (getCommand("setspawn") != null) {
                    getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager));
                    getLogger().info("'/setspawn' command enabled.");
                } else {
                    getLogger().severe("Command 'setspawn' not defined in plugin.yml. Spawn module could not be enabled.");
                }
                if (getCommand("spawn") != null) {
                    getCommand("spawn").setExecutor(new SpawnCommand(spawnManager));
                    getLogger().info("'/spawn' command enabled.");
                } else {
                    getLogger().severe("Command 'spawn' not defined in plugin.yml. Spawn module could not be enabled.");
                }
                getServer().getPluginManager().registerEvents(new SpawnListener(spawnManager), this);
                getLogger().info("Spawn Listener registered for Spawn module.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error enabling Spawn module:", e);
                this.enableSpawn = false;
            }
        } else {
            getLogger().info("Spawn module disabled in config.yml.");
        }

        if (enableClaims) {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null || !(Bukkit.getPluginManager().getPlugin("WorldGuard") instanceof com.sk89q.worldguard.bukkit.WorldGuardPlugin)) {
                getLogger().severe("WorldGuard plugin not found or not enabled. Claims module will be disabled.");
                this.enableClaims = false;
            } else {
                this.claimManager = new ClaimManager(this);
                if (getCommand("claim") != null) {
                    getCommand("claim").setExecutor(new ClaimCommand(this, claimManager));
                    getLogger().info("'/claim' command enabled.");
                } else {
                    getLogger().severe("Command 'claim' not defined in plugin.yml. Claims module could not be enabled.");
                }
                getServer().getPluginManager().registerEvents(new ClaimSelectionListener(claimManager), this);
                getServer().getPluginManager().registerEvents(new ClaimListener(claimManager), this);
                getLogger().info("Claims module enabled.");
            }
        } else {
            getLogger().info("Claims module disabled in config.yml.");
        }

        if (enableScoreboard) {
            try {
                Class.forName("fr.mrmicky.fastboard.FastBoard");
                this.scoreboardService = new ScoreboardService(this);
                this.scoreboardService.setCustomTimerCommand(this.customTimerCommand);
                getLogger().info("Scoreboard module enabled.");
            } catch (ClassNotFoundException e) {
                getLogger().severe("FastBoard library not found. Scoreboard module will be disabled.");
                this.enableScoreboard = false;
            }
        } else {
            getLogger().info("Scoreboard module disabled in config.yml.");
        }

        // 8. Death Messages Module
        if (enableDeathMessages) {
            try {
                this.deathMessageManager = new DeathMessageManager(this);
                this.guiManager = new GUIManager(this);

                getServer().getPluginManager().registerEvents(new DeathListener(deathMessageManager), this);
                getLogger().info("DeathListener registered for Death Messages module.");

                if (getCommand("deathmessage") != null) {
                    getCommand("deathmessage").setExecutor(new DeathMessageCommand(deathMessageManager, guiManager));
                    getLogger().info("'/deathmessage' command enabled.");
                } else {
                    getLogger().severe("Command 'deathmessage' not defined in plugin.yml. Death Messages module command could not be enabled.");
                }
                getLogger().info("Death Messages module enabled.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error enabling Death Messages module:", e);
                this.enableDeathMessages = false;
            }
        } else {
            getLogger().info("Death Messages module disabled in config.yml.");
        }

        // 9. Auto-Respawn Module
        if (enableAutoRespawn) {
            if (spawnManager != null) {
                try {
                    this.autoRespawnListener = new AutoRespawnListener(this, spawnManager, enableAutoRespawn);
                    getServer().getPluginManager().registerEvents(autoRespawnListener, this);
                    getLogger().info("Auto-Respawn module enabled.");
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Error enabling Auto-Respawn module:", e);
                    this.enableAutoRespawn = false;
                }
            } else {
                getLogger().severe("Spawn module is not enabled or initialized. Auto-Respawn module will be disabled.");
                this.enableAutoRespawn = false;
            }
        } else {
            getLogger().info("Auto-Respawn module disabled in config.yml.");
        }

        // >>>>> INICIO: Inicialización del módulo de Economía <<<<<
        if (enableEconomy) {
            try {
                this.economyManager = new EconomyManager(this);
                // Registrar comandos
                if (getCommand("eco") != null) {
                    getCommand("eco").setExecutor(new EconomyAdminCommand(economyManager));
                    getLogger().info("'/eco' command enabled.");
                } else {
                    getLogger().severe("Command 'eco' not defined in plugin.yml. Economy module command could not be enabled.");
                }
                if (getCommand("pay") != null) {
                    getCommand("pay").setExecutor(new PayCommand(economyManager));
                    getLogger().info("'/pay' command enabled.");
                } else {
                    getLogger().severe("Command 'pay' not defined in plugin.yml. Pay command could not be enabled.");
                }
                // REGISTRAR EL NUEVO COMANDO /baltop
                if (getCommand("baltop") != null) { // <--- NUEVA LÍNEA
                    getCommand("baltop").setExecutor(new BaltopCommand(economyManager)); // <--- NUEVA LÍNEA
                    getLogger().info("'/baltop' command enabled."); // <--- NUEVA LÍNEA
                } else { // <--- NUEVA LÍNEA
                    getLogger().severe("Command 'baltop' not defined in plugin.yml. Baltop command could not be enabled."); // <--- NUEVA LÍNEA
                } // <--- NUEVA LÍNEA

                // Registrar Listener para inicializar el dinero de nuevos jugadores
                getServer().getPluginManager().registerEvents(new EconomyListener(economyManager), this);
                getLogger().info("Economy Listener registered for Economy module.");

                // Registrar PlaceholderAPI hook si PlaceholderAPI está presente
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    new EconomyPlaceholder(this, economyManager).register();
                    getLogger().info("PlaceholderAPI hook for Economy registered.");
                } else {
                    getLogger().warning("PlaceholderAPI not found. Economy placeholders will not be available.");
                }

                getLogger().info("Economy module enabled.");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error enabling Economy module:", e);
                this.enableEconomy = false;
            }
        } else {
            getLogger().info("Economy module disabled in config.yml.");
        }
        // >>>>> FIN: Inicialización del módulo de Economía <<<<<
    }

    @Override
    public void onDisable() {
        getLogger().info("BoxCore disabling...");

        if (enableKits && kitManager != null) {
            getLogger().info("Cleaning up Kits module.");
        }

        if (enableTemporaryBlocks && temporaryBlockManager != null) {
            temporaryBlockManager.shutdown();
            getLogger().info("Cleaning up Temporary Blocks module.");
        }

        if (enableSpawn && spawnManager != null) {
            spawnManager.shutdown();
            getLogger().info("Cleaning up Spawn module.");
        }

        if (enableClaims && claimManager != null) {
            getLogger().info("Cleaning up Claims module.");
        }

        if (enableScoreboard && scoreboardService != null) {
            scoreboardService.unload();
            getLogger().info("Cleaning up Scoreboard module.");
        }

        if (rebootService != null) {
            rebootService.unload();
            getLogger().info("Cleaning up Reboot module.");
        }

        if (enableDeathMessages && deathMessageManager != null) {
            deathMessageManager.saveCustomMessages();
            getLogger().info("Cleaning up Death Messages module.");
        }

        if (enableAutoRespawn && autoRespawnListener != null) {
            getLogger().info("Cleaning up Auto-Respawn module.");
        }

        // >>>>> INICIO: Limpieza del módulo de Economía <<<<<
        if (enableEconomy && economyManager != null) {
            economyManager.shutdown();
            getLogger().info("Cleaning up Economy module.");
        }
        // >>>>> FIN: Limpieza del módulo de Economía <<<<<
    }

    public static Box getInstance() {
        return instance;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public TemporaryBlockManager getTemporaryBlockManager() {
        return temporaryBlockManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public ScoreboardService getScoreboardService() {
        return scoreboardService;
    }

    public CustomTimerCommand getCustomTimerCommand() {
        return customTimerCommand;
    }

    public RebootService getRebootService() {
        return rebootService;
    }

    public DeathMessageManager getDeathMessageManager() {
        return deathMessageManager;
    }

    // >>>>> INICIO: Getter para EconomyManager <<<<<
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    // >>>>> FIN: Getter para EconomyManager <<<<<

    private void loadConfigSettings() {
        this.enableKits = getConfig().getBoolean("modules.kits.enabled", true);
        this.enableTemporaryBlocks = getConfig().getBoolean("modules.temporaryblocks.enabled", true);
        this.enableSpawn = getConfig().getBoolean("modules.spawn.enabled", true);
        this.enableClaims = getConfig().getBoolean("modules.claims.enabled", true);
        this.enableScoreboard = getConfig().getBoolean("modules.scoreboard.enabled", true);
        this.enableDeathMessages = getConfig().getBoolean("modules.deathmessages.enabled", true);
        this.enableAutoRespawn = getConfig().getBoolean("modules.autorespawn.enabled", true);

        // >>>>> INICIO: Cargar configuración de Economía <<<<<
        this.enableEconomy = getConfig().getBoolean("modules.economy.enabled", true);
        // >>>>> FIN: Cargar configuración de Economía <<<<<
    }

    public void reloadModules() {
        getLogger().info("Reloading BoxCore modules...");

        if (scoreboardService != null) {
            scoreboardService.reload();
        }
        if (rebootService != null) {
            rebootService.reload();
        }
        if (deathMessageManager != null) {
            deathMessageManager.reload();
        }

        this.enableAutoRespawn = getConfig().getBoolean("modules.autorespawn.enabled", true);
        if (autoRespawnListener != null) {
            autoRespawnListener.setEnableAutoRespawn(this.enableAutoRespawn);
        }

        // >>>>> INICIO: Recarga del módulo de Economía <<<<<
        if (economyManager != null) {
            economyManager.reloadConfig();
        }
        // >>>>> FIN: Recarga del módulo de Economía <<<<<

        getLogger().info("BoxCore modules reloaded.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("boxcore") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("boxcore.reload")) {
                reloadModules();
                sender.sendMessage(ChatColor.GREEN + "BoxCore modules reloaded.");
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have permission to reload BoxCore.");
            }
            return true;
        }
        return false;
    }
}