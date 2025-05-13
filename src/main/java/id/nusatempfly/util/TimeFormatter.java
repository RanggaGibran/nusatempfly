package id.nusatempfly.util;

import id.nusatempfly.Plugin;
import org.bukkit.ChatColor;

public class TimeFormatter {
    private final Plugin plugin;
    
    public TimeFormatter(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Format time in seconds to human-readable string based on config settings
     * @param seconds Time in seconds
     * @return Formatted time string
     */
    public String format(long seconds) {
        if (seconds == 0) {
            return "0 seconds";
        }
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        String format = plugin.getConfig().getString("time-format.format", "%days% %hours% %minutes% %seconds%");
        
        // Replace days
        if (days > 0) {
            String dayFormat = days == 1 ? 
                plugin.getConfig().getString("time-format.day", "%d day") : 
                plugin.getConfig().getString("time-format.days", "%d days");
            format = format.replace("%days%", dayFormat.replace("%d", String.valueOf(days)));
        } else {
            format = format.replace("%days%", "");
        }
        
        // Replace hours
        if (hours > 0) {
            String hourFormat = hours == 1 ? 
                plugin.getConfig().getString("time-format.hour", "%d hour") : 
                plugin.getConfig().getString("time-format.hours", "%d hours");
            format = format.replace("%hours%", hourFormat.replace("%d", String.valueOf(hours)));
        } else {
            format = format.replace("%hours%", "");
        }
        
        // Replace minutes
        if (minutes > 0) {
            String minuteFormat = minutes == 1 ? 
                plugin.getConfig().getString("time-format.minute", "%d minute") : 
                plugin.getConfig().getString("time-format.minutes", "%d minutes");
            format = format.replace("%minutes%", minuteFormat.replace("%d", String.valueOf(minutes)));
        } else {
            format = format.replace("%minutes%", "");
        }
        
        // Replace seconds
        if (remainingSeconds > 0 || (days == 0 && hours == 0 && minutes == 0)) {
            String secondFormat = remainingSeconds == 1 ? 
                plugin.getConfig().getString("time-format.second", "%d second") : 
                plugin.getConfig().getString("time-format.seconds", "%d seconds");
            format = format.replace("%seconds%", secondFormat.replace("%d", String.valueOf(remainingSeconds)));
        } else {
            format = format.replace("%seconds%", "");
        }
        
        // Cleanup extra spaces
        return format.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * Format time in compact format (1d2h3m4s)
     * @param seconds Time in seconds
     * @return Compact formatted time
     */
    public String formatCompact(long seconds) {
        if (seconds == 0) {
            return "0s";
        }
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) sb.append(days).append("d");
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (remainingSeconds > 0 || sb.length() == 0) sb.append(remainingSeconds).append("s");
        
        return sb.toString();
    }
    
    /**
     * Parse time string in format 1d2h3m4s to seconds
     * @param timeString Time string
     * @return Time in seconds, -1 if invalid format
     */
    public long parse(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return -1;
        }
        
        // Remove all whitespaces
        timeString = timeString.replaceAll("\\s+", "").toLowerCase();
        
        long totalSeconds = 0;
        StringBuilder currentNumber = new StringBuilder();
        
        for (char c : timeString.toCharArray()) {
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else {
                // If we have accumulated some digits
                if (currentNumber.length() > 0) {
                    long value = Long.parseLong(currentNumber.toString());
                    
                    switch (c) {
                        case 'd':
                            totalSeconds += value * 86400; // Days to seconds
                            break;
                        case 'h':
                            totalSeconds += value * 3600; // Hours to seconds
                            break;
                        case 'm':
                            totalSeconds += value * 60; // Minutes to seconds
                            break;
                        case 's':
                            totalSeconds += value; // Seconds
                            break;
                        default:
                            return -1; // Invalid character
                    }
                    
                    // Reset for next number
                    currentNumber.setLength(0);
                } else {
                    // If there's no number before the unit character
                    return -1;
                }
            }
        }
        
        // If there are trailing digits without a unit
        if (currentNumber.length() > 0) {
            // Assume seconds for bare numbers
            totalSeconds += Long.parseLong(currentNumber.toString());
        }
        
        return totalSeconds;
    }
}