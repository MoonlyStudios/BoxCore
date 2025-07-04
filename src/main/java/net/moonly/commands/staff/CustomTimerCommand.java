package net.moonly.commands.staff;

import net.moonly.modules.scoreboard.CustomTimer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomTimerCommand implements CommandExecutor {

    private final Map<String, CustomTimer> activeTimers = new HashMap<>();
    private final JavaPlugin plugin; // Necesitamos el plugin para acceder a su logger

    public CustomTimerCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Método para que ScoreboardTask pueda acceder a los temporizadores
    public Map<String, CustomTimer> getActiveTimers() {
        // Limpiar temporizadores terminados antes de devolver la lista
        activeTimers.entrySet().removeIf(entry -> entry.getValue().isFinished());
        return activeTimers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("boxcore.customtimer")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUso: /customtimer <start|stop>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("start")) {
            if (args.length < 4) {
                sender.sendMessage("§cUso: /customtimer start <nombre> <prefix> <tiempo>");
                sender.sendMessage("§cEjemplo: /customtimer start evento \"Evento: \" 1h30m");
                return true;
            }

            String name = args[1];
            String prefix = args[2].replace("_", " "); // Permite prefijos con espacios usando guiones bajos
            prefix += "§f:"; // NUEVO: Añadir ': ' en color blanco al final del prefijo
            String timeString = args[3];

            if (activeTimers.containsKey(name)) {
                sender.sendMessage("§cYa existe un temporizador con el nombre '" + name + "'.");
                return true;
            }

            long durationMillis = parseTimeString(timeString);
            if (durationMillis <= 0) {
                sender.sendMessage("§cTiempo inválido. Usa formatos como 1h, 30m, 5s, 1h30m.");
                return true;
            }

            CustomTimer timer = new CustomTimer(name, prefix, durationMillis);
            activeTimers.put(name, timer);
            sender.sendMessage("§aTemporizador '" + name + "' iniciado con prefijo '" + prefix + "' por " + timer.getFormattedRemainingTime() + ".");
            plugin.getLogger().info("Temporizador '" + name + "' iniciado por " + sender.getName() + ".");

        } else if (subCommand.equals("stop")) {
            if (args.length < 2) {
                sender.sendMessage("§cUso: /customtimer stop <nombre>");
                return true;
            }

            String name = args[1];
            if (activeTimers.remove(name) != null) {
                sender.sendMessage("§aTemporizador '" + name + "' detenido.");
                plugin.getLogger().info("Temporizador '" + name + "' detenido por " + sender.getName() + ".");
            } else {
                sender.sendMessage("§cNo se encontró ningún temporizador con el nombre '" + name + "'.");
            }
        } else {
            sender.sendMessage("§cUso: /customtimer <start|stop>");
        }

        return true;
    }

    private long parseTimeString(String timeString) {
        long totalMillis = 0;
        Pattern pattern = Pattern.compile("(\\d+)([hms])");
        Matcher matcher = pattern.matcher(timeString);

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "h":
                    totalMillis += TimeUnit.HOURS.toMillis(value);
                    break;
                case "m":
                    totalMillis += TimeUnit.MINUTES.toMillis(value);
                    break;
                case "s":
                    totalMillis += TimeUnit.SECONDS.toMillis(value);
                    break;
            }
        }
        return totalMillis;
    }
}