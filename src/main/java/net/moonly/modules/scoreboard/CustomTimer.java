package net.moonly.modules.scoreboard;

public class CustomTimer {
    private final String name;
    private final String prefix;
    private final long startTime;
    private final long durationMillis;
    private boolean finished;
    private boolean showOnScoreboard;

    public CustomTimer(String name, String prefix, long durationMillis) {
        this(name, prefix, durationMillis, true);
    }

    public CustomTimer(String name, String prefix, long durationMillis, boolean showOnScoreboard) {
        this.name = name;
        this.prefix = prefix;
        this.startTime = System.currentTimeMillis();
        this.durationMillis = durationMillis;
        this.finished = false;
        this.showOnScoreboard = showOnScoreboard;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public long getRemainingTimeMillis() {
        if (finished) {
            return 0;
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        long remaining = durationMillis - elapsedTime;
        if (remaining <= 0) {
            finished = true;
            return 0;
        }
        return remaining;
    }

    public String getFormattedRemainingTime() {
        long remainingMillis = getRemainingTimeMillis();
        if (remainingMillis <= 0) {
            return "00:00";
        }

        long totalSeconds = remainingMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isShowOnScoreboard() {
        return showOnScoreboard;
    }

    public void setShowOnScoreboard(boolean showOnScoreboard) {
        this.showOnScoreboard = showOnScoreboard;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}