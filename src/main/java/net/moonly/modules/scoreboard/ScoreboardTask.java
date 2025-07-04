package net.moonly.modules.scoreboard;

import net.moonly.commands.staff.CustomTimerCommand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class ScoreboardTask extends BukkitRunnable {

    private final ScoreboardService service;
    private int titleFrameIndex = 0;
    private long titleTickCounter = 0;

    private int customFooterFrameIndex = 0;
    private long customFooterTickCounter = 0;

    private List<String> titleFrames;
    private int titleTicks;
    private List<String> customFooterFrames;
    private int customFooterTicks;
    private List<String> scoreboardLines;

    private CustomTimerCommand customTimerCommand;

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    private static final Map<String, String> CUSTOM_TAG_MAP = new HashMap<>();
    static {
        CUSTOM_TAG_MAP.put("<gray>", "&7");
        CUSTOM_TAG_MAP.put("</gray>", "&r");
        CUSTOM_TAG_MAP.put("<dark_gray>", "&8");
        CUSTOM_TAG_MAP.put("</dark_gray>", "&r");
        CUSTOM_TAG_MAP.put("<red>", "&c");
        CUSTOM_TAG_MAP.put("</red>", "&r");
        CUSTOM_TAG_MAP.put("<bold>", "&l");
        CUSTOM_TAG_MAP.put("</bold>", "&r");
    }

    public ScoreboardTask(ScoreboardService service) {
        this.service = service;
        this.titleFrames = Collections.emptyList();
        this.titleTicks = 0;
        this.customFooterFrames = Collections.emptyList();
        this.customFooterTicks = 0;
        this.scoreboardLines = Collections.emptyList();

        this.runTaskTimer(service.getServices(), 0L, 1L);
    }

    public void setCustomTimerCommand(CustomTimerCommand customTimerCommand) {
        this.customTimerCommand = customTimerCommand;
    }

    @Override
    public void run() {
        if (!service.getServices().getConfig().getBoolean("modules.scoreboard.enabled", true)) {
            service.unload();
            this.cancel();
            return;
        }

        boolean hasActiveTimers = (customTimerCommand != null && !customTimerCommand.getActiveTimers().isEmpty());
        ConfigurationSection currentScoreboardConfig;

        if (hasActiveTimers && service.getCustomTimerScoreboardConfig() != null) {
            currentScoreboardConfig = service.getCustomTimerScoreboardConfig();
        } else if (service.getDefaultScoreboardConfig() != null) {
            currentScoreboardConfig = service.getDefaultScoreboardConfig();
        } else {
            service.getScoreboards().forEach((uuid, profile) -> {
                if (!profile.getFastBoard().isDeleted()) {
                    profile.getFastBoard().updateLines(Collections.emptyList());
                    profile.getFastBoard().updateTitle("");
                }
            });
            return;
        }

        this.titleFrames = currentScoreboardConfig.getStringList("title.frames");
        this.titleTicks = currentScoreboardConfig.getInt("title.ticks", 2);
        this.customFooterFrames = currentScoreboardConfig.getStringList("custom-footer-frames");
        this.customFooterTicks = currentScoreboardConfig.getInt("custom-footer-ticks", 10);
        this.scoreboardLines = currentScoreboardConfig.getStringList("scoreboard");

        if (!this.titleFrames.isEmpty()) {
            if (this.titleTicks > 0) {
                titleTickCounter++;
                if (titleTickCounter >= this.titleTicks) {
                    titleFrameIndex = (titleFrameIndex + 1) % this.titleFrames.size();
                    titleTickCounter = 0;
                }
            } else {
                titleFrameIndex = 0;
            }
        } else {
            titleFrameIndex = 0;
        }

        if (!this.customFooterFrames.isEmpty()) {
            if (this.customFooterTicks > 0) {
                customFooterTickCounter++;
                if (customFooterTickCounter >= this.customFooterTicks) {
                    customFooterFrameIndex = (customFooterFrameIndex + 1) % this.customFooterFrames.size();
                    customFooterTickCounter = 0;
                }
            } else {
                customFooterFrameIndex = 0;
            }
        } else {
            customFooterFrameIndex = 0;
        }

        service.getScoreboards().forEach((uuid, profile) -> {
            Player player = Bukkit.getPlayer(uuid);

            if (profile.getFastBoard().isDeleted() || player == null) {
                service.getScoreboards().remove(uuid);
                return;
            }

            if (!this.titleFrames.isEmpty()) {
                String title = this.titleFrames.get(titleFrameIndex);
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    title = PlaceholderAPI.setPlaceholders(player, title);
                }
                title = translateCustomTags(title);
                title = translateHexColorCodes(title);
                profile.getFastBoard().updateTitle(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', title));
            } else {
                profile.getFastBoard().updateTitle("");
            }

            List<String> lines = new ArrayList<>();

            String currentCustomFooterFrame = this.customFooterFrames.isEmpty() ? "" : this.customFooterFrames.get(customFooterFrameIndex);

            for (String line : this.scoreboardLines) {
                String processedLine = line;

                if (processedLine.contains("%footer%")) {
                    processedLine = processedLine.replace("%footer%", currentCustomFooterFrame);
                }

                if (processedLine.contains("%customtimer_list%")) {
                    StringBuilder timerLinesBuilder = new StringBuilder();
                    if (customTimerCommand != null && !customTimerCommand.getActiveTimers().isEmpty()) {
                        for (CustomTimer timer : customTimerCommand.getActiveTimers().values()) {
                            // NUEVA COMPROBACIÓN: Solo añadir si showOnScoreboard es true
                            if (!timer.isFinished() && timer.isShowOnScoreboard()) {
                                String timerLine = " " + timer.getPrefix() + " " + timer.getFormattedRemainingTime();
                                timerLine = translateCustomTags(timerLine);
                                timerLine = translateHexColorCodes(timerLine);
                                timerLinesBuilder.append(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', timerLine)).append("\n");
                            }
                        }
                    }
                    processedLine = processedLine.replace("%customtimer_list%", timerLinesBuilder.toString().trim());
                }

                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    processedLine = PlaceholderAPI.setPlaceholders(player, processedLine);
                }
                processedLine = translateCustomTags(processedLine);
                processedLine = translateHexColorCodes(processedLine);

                String[] splitLines = processedLine.split("\n");
                for (String splitLine : splitLines) {
                    if (!splitLine.isEmpty()) {
                        lines.add(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', splitLine));
                    }
                }
            }

            profile.getFastBoard().updateLines(lines);
        });
    }

    private String translateHexColorCodes(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String hex = matcher.group(1);
            try {
                String bungeecordColorCode = net.md_5.bungee.api.ChatColor.of("#" + hex).toString();
                matcher.appendReplacement(buffer, bungeecordColorCode);
            } catch (IllegalArgumentException e) {
                matcher.appendReplacement(buffer, matcher.group(0));
                service.getServices().getLogger().warning("Invalid hex color code detected: #" + hex + " in scoreboard. Message: " + message);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String translateCustomTags(String message) {
        String processedMessage = message;
        for (Map.Entry<String, String> entry : CUSTOM_TAG_MAP.entrySet()) {
            processedMessage = processedMessage.replace(entry.getKey(), entry.getValue());
        }
        return processedMessage;
    }
}