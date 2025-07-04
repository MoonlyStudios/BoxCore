package net.moonly.commands.staff;

import net.moonly.Box;
import net.moonly.modules.scoreboard.CustomTimer;
import net.moonly.utils.ConfigFile;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set; // Importar Set
import java.util.HashSet; // Importar HashSet
import java.util.Map; // Importar Map
import java.util.HashMap; // Importar HashMap

public class RebootService implements CommandExecutor {

    private final Box plugin;
    private final CustomTimerCommand customTimerCommand;
    private ConfigFile config;
    private CustomTimer rebootTimer;
    private BukkitTask rebootTask;

    private final Set<Long> sentThresholds = new HashSet<>();
    private boolean halfwayMessageSent = false;

    private boolean showOnScoreboard;
    private boolean messagesEnabled;
    private String messageStart;
    private String messageHalfway;
    private String messageEnd;
    private boolean titleEnabled;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private String titleStartTitle;
    private String titleStartSubtitle;
    private String titleHalfwayTitle;
    private String titleHalfwaySubtitle;
    private String titleEndTitle;
    private String titleEndSubtitle;
    private boolean actionbarEnabled;
    private String actionbarFormat;
    private String actionbarHalfway;
    private String actionbarEnd;

    private Map<Long, String> chatThresholdMessages;
    private Map<Long, String[]> titleThresholdMessages;
    private Map<Long, String> actionbarThresholdMessages;


    public RebootService(Box plugin, CustomTimerCommand customTimerCommand) {
        this.plugin = plugin;
        this.customTimerCommand = customTimerCommand;
        this.config = new ConfigFile(plugin, "modules/reboot/reboot.yml");
        loadConfig();
    }

    public void loadConfig() {
        this.config.reloadConfig();
        ConfigurationSection settings = config.getConfig().getConfigurationSection("settings");
        if (settings != null) {
            showOnScoreboard = settings.getBoolean("show_on_scoreboard", true);
        } else {
            showOnScoreboard = true;
        }

        ConfigurationSection messages = config.getConfig().getConfigurationSection("messages");
        if (messages != null) {
            messagesEnabled = messages.getBoolean("enabled", true);
            messageStart = messages.getString("start", "&cThe server will reboot in %time%!");
            messageHalfway = messages.getString("halfway", "&eHalf the time remaining until reboot: %time%!");
            messageEnd = messages.getString("end", "&4The server is rebooting NOW!");
        } else { messagesEnabled = true; messageStart = "&cThe server will reboot in %time%!"; messageHalfway = "&eHalf the time remaining until reboot: %time%!"; messageEnd = "&4The server is rebooting NOW!"; }

        ConfigurationSection title = config.getConfig().getConfigurationSection("title");
        if (title != null) {
            titleEnabled = title.getBoolean("enabled", true);
            titleFadeIn = title.getInt("fade_in", 10);
            titleStay = title.getInt("stay", 70);
            titleFadeOut = title.getInt("fade_out", 20);

            ConfigurationSection titleStart = title.getConfigurationSection("start");
            titleStartTitle = titleStart != null ? titleStart.getString("title", "&c&lREBOOT") : "&c&lREBOOT";
            titleStartSubtitle = titleStart != null ? titleStart.getString("subtitle", "&fIn %time%") : "&fIn %time%";

            ConfigurationSection titleHalfway = title.getConfigurationSection("halfway");
            titleHalfwayTitle = titleHalfway != null ? titleHalfway.getString("title", "&e&lHALF TIME") : "&e&lHALF TIME";
            titleHalfwaySubtitle = titleHalfway != null ? titleHalfway.getString("subtitle", "&f%time% remaining") : "&f%time% remaining";

            ConfigurationSection titleEnd = title.getConfigurationSection("end");
            titleEndTitle = titleEnd != null ? titleEnd.getString("title", "&4&lREBOOTING") : "&4&lREBOOTING";
            titleEndSubtitle = titleEnd != null ? titleEnd.getString("subtitle", "&fGoodbye!") : "&fGoodbye!";
        } else { titleEnabled = true; titleFadeIn = 10; titleStay = 70; titleFadeOut = 20; titleStartTitle = "&c&lREBOOT"; titleStartSubtitle = "&fIn %time%"; titleHalfwayTitle = "&e&lMITAD DE TIEMPO"; titleHalfwaySubtitle = "&fQuedan %time%"; titleEndTitle = "&4&lREINICIANDO"; titleEndSubtitle = "&f¡Adiós!"; }

        ConfigurationSection actionbar = config.getConfig().getConfigurationSection("actionbar");
        if (actionbar != null) {
            actionbarEnabled = actionbar.getBoolean("enabled", true);
            actionbarFormat = actionbar.getString("format", "&6Reboot: &f%time%");
            actionbarHalfway = actionbar.getString("halfway", "&eHalf the time remaining until reboot: %time%!");
            actionbarEnd = actionbar.getString("end", "&4¡The server is rebooting NOW!");
        } else { actionbarEnabled = true; actionbarFormat = "&6Reboot: &f%time%"; actionbarHalfway = "&eHalf the time remaining until reboot: %time%!"; actionbarEnd = "&4The server is rebooting NOW!"; }

        chatThresholdMessages = new HashMap<>();
        titleThresholdMessages = new HashMap<>();
        actionbarThresholdMessages = new HashMap<>();

        ConfigurationSection msgThresholds = config.getConfig().getConfigurationSection("messages.thresholds");
        if (msgThresholds != null) {
            msgThresholds.getKeys(false).forEach(key -> {
                try {
                    long seconds = parseTimeThresholdKey(key);
                    chatThresholdMessages.put(TimeUnit.SECONDS.toMillis(seconds), msgThresholds.getString(key));
                } catch (NumberFormatException e) { plugin.getLogger().warning("Invalid threshold key in reboot.yml -> messages.thresholds: " + key + ": " + e.getMessage()); }
            });
        }
        ConfigurationSection titleThresholds = config.getConfig().getConfigurationSection("title.thresholds");
        if (titleThresholds != null) {
            titleThresholds.getKeys(false).forEach(key -> {
                try {
                    long seconds = parseTimeThresholdKey(key);
                    titleThresholdMessages.put(TimeUnit.SECONDS.toMillis(seconds), new String[]{
                            titleThresholds.getString(key + ".title", ""),
                            titleThresholds.getString(key + ".subtitle", "")
                    });
                } catch (NumberFormatException e) { plugin.getLogger().warning("Invalid threshold key in reboot.yml -> title.thresholds: " + key + ": " + e.getMessage()); }
            });
        }
        ConfigurationSection actionbarThresholds = config.getConfig().getConfigurationSection("actionbar.thresholds");
        if (actionbarThresholds != null) {
            actionbarThresholds.getKeys(false).forEach(key -> {
                try {
                    long seconds = parseTimeThresholdKey(key);
                    actionbarThresholdMessages.put(TimeUnit.SECONDS.toMillis(seconds), actionbarThresholds.getString(key));
                } catch (NumberFormatException e) { plugin.getLogger().warning("Invalid threshold key in reboot.yml -> actionbar.thresholds: " + key + ": " + e.getMessage()); }
            });
        }
    }

    private long parseTimeThresholdKey(String key) throws NumberFormatException {
        Pattern pattern = Pattern.compile("(\\d+)([msh])");
        Matcher matcher = pattern.matcher(key.toLowerCase()); // Corrected this line
        if (matcher.matches()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "h": return TimeUnit.HOURS.toSeconds(value);
                case "m": return TimeUnit.MINUTES.toSeconds(value);
                case "s": return value;
                default: throw new NumberFormatException("Unknown unit: " + unit);
            }
        }
        throw new NumberFormatException("Invalid time threshold format: " + key);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("boxcore.reboot")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /reboot <start|stop|reload> [tiempo]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("start")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /reboot start <tiempo>");
                sender.sendMessage(ChatColor.RED + "Ejemplo: /reboot start 30m");
                return true;
            }

            if (rebootTimer != null && !rebootTimer.isFinished()) {
                sender.sendMessage(ChatColor.RED + "Ya hay un reinicio programado.");
                return true;
            }

            String timeString = args[1];
            long durationMillis = parseTimeString(timeString);

            if (durationMillis <= 0) {
                sender.sendMessage(ChatColor.RED + "Tiempo inválido. Usa formatos como 1h, 30m, 5s, 1h30m.");
                return true;
            }

            rebootTimer = new CustomTimer("Reboot", "&cReboot", durationMillis, showOnScoreboard);
            customTimerCommand.getActiveTimers().put("Reboot", rebootTimer);

            sentThresholds.clear(); // Limpiar todas las banderas de umbrales enviadas para el nuevo timer
            halfwayMessageSent = false; // Resetear la bandera de punto medio

            // Enviar mensaje de inicio de reinicio
            sendMessageAndTitles(messageStart, titleStartTitle, titleStartSubtitle, null, rebootTimer.getFormattedRemainingTime()); // Pasa null para actionbar si no es de inicio

            startRebootTask(); // Iniciar la tarea de cuenta regresiva y visualización

        } else if (subCommand.equals("stop")) {
            if (rebootTimer != null && !rebootTimer.isFinished()) {
                rebootTimer.setFinished(true); // Marcar como terminado para detener la lógica
                customTimerCommand.getActiveTimers().remove("Reboot"); // Remover del CustomTimerCommand
                if (rebootTask != null) {
                    rebootTask.cancel();
                }
                sender.sendMessage(ChatColor.GREEN + "Reinicio cancelado.");
                Bukkit.broadcastMessage(ChatColor.GREEN + "El reinicio ha sido cancelado.");
                // Limpiar action bar y title si es necesario
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (actionbarEnabled) p.spigot().sendMessage(new TextComponent("")); // Limpiar action bar
                    if (titleEnabled) p.sendTitle("", "", 0, 1, 0); // Limpiar title
                }
            } else {
                sender.sendMessage(ChatColor.RED + "No hay ningún reinicio activo para detener.");
            }
        } else if (subCommand.equals("reload")) {
            if (!sender.hasPermission("boxcore.reboot.reload")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para recargar el módulo de reinicio.");
                return true;
            }
            reload();
            sender.sendMessage(ChatColor.GREEN + "Módulo de reinicio recargado.");
        }
        else {
            sender.sendMessage(ChatColor.RED + "Uso: /reboot <start|stop|reload> [tiempo]");
        }

        return true;
    }

    private void startRebootTask() {
        if (rebootTask != null) {
            rebootTask.cancel();
        }

        rebootTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (rebootTimer == null || rebootTimer.isFinished()) {
                    cancel();
                    return;
                }

                long remainingMillis = rebootTimer.getRemainingTimeMillis();
                String formattedTime = rebootTimer.getFormattedRemainingTime();
                long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis);

                // Lógica de punto medio
                // Se usa 'remainingSeconds' para ser más preciso con los umbrales
                if (!halfwayMessageSent && remainingMillis <= rebootTimer.getDurationMillis() / 2 && rebootTimer.getDurationMillis() / 2 > 0) {
                    sendMessageAndTitles(messageHalfway, titleHalfwayTitle, titleHalfwaySubtitle, actionbarHalfway, formattedTime);
                    halfwayMessageSent = true; // Marcar la bandera
                }

                // Lógica para umbrales de tiempo específicos
                Long currentThresholdMillis = remainingSeconds * 1000L;

                // Bandera para saber si se envió algún mensaje (chat, título, actionbar) de umbral específico en este tick
                boolean specificThresholdMessageSentThisTick = false;

                if (!sentThresholds.contains(currentThresholdMillis)) { // Si este umbral NO ha sido enviado antes
                    String chatMsgForThreshold = chatThresholdMessages.get(currentThresholdMillis);
                    String[] titleAndSubtitleForThreshold = titleThresholdMessages.get(currentThresholdMillis);
                    String actionbarMsgForThreshold = actionbarThresholdMessages.get(currentThresholdMillis);

                    if (chatMsgForThreshold != null || titleAndSubtitleForThreshold != null || actionbarMsgForThreshold != null) {
                        sendMessageAndTitles(chatMsgForThreshold, titleAndSubtitleForThreshold != null ? titleAndSubtitleForThreshold[0] : null, titleAndSubtitleForThreshold != null ? titleAndSubtitleForThreshold[1] : null, actionbarMsgForThreshold, formattedTime);
                        sentThresholds.add(currentThresholdMillis); // Marcar este umbral como enviado
                        specificThresholdMessageSentThisTick = true; // Un mensaje de umbral específico fue enviado
                    }
                }

                // Lógica del Action Bar Constante (se envía si NO se activó un umbral específico en este tick)
                if (actionbarEnabled && !specificThresholdMessageSentThisTick) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.spigot().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', actionbarFormat.replace("%time%", formattedTime))));
                    }
                }

                if (remainingMillis <= 0) {
                    sendMessageAndTitles(messageEnd, titleEndTitle, titleEndSubtitle, actionbarEnd, formattedTime);

                    cancel();
                    customTimerCommand.getActiveTimers().remove("Reboot");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Ejecutar cada segundo (20 ticks)
    }

    private void sendMessageAndTitles(String chatMessage, String titleText, String subtitleText, String actionbarText, String time) {
        if (messagesEnabled && chatMessage != null) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', chatMessage.replace("%time%", time)));
        }
        if (titleEnabled && titleText != null && subtitleText != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', titleText.replace("%time%", time)),
                        ChatColor.translateAlternateColorCodes('&', subtitleText.replace("%time%", time)),
                        titleFadeIn, titleStay, titleFadeOut
                );
            }
        }
        if (actionbarEnabled && actionbarText != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', actionbarText.replace("%time%", time))));
            }
        }
    }

    private long parseTimeString(String timeString) {
        long totalMillis = 0;
        Pattern pattern = Pattern.compile("(\\d+)([hms])");
        Matcher matcher = pattern.matcher(timeString);

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "h": totalMillis += TimeUnit.HOURS.toMillis(value); break;
                case "m": totalMillis += TimeUnit.MINUTES.toMillis(value); break;
                case "s": totalMillis += TimeUnit.SECONDS.toMillis(value); break;
            }
        }
        return totalMillis;
    }

    public void unload() {
        if (rebootTask != null) {
            rebootTask.cancel();
        }
        if (rebootTimer != null) {
            rebootTimer.setFinished(true);
            customTimerCommand.getActiveTimers().remove("Reboot");
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (actionbarEnabled) p.spigot().sendMessage(new TextComponent(""));
            if (titleEnabled) p.sendTitle("", "", 0, 1, 0);
        }
    }

    public void reload() {
        unload();
        loadConfig();
    }
}