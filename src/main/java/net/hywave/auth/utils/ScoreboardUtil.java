package net.hywave.auth.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitRunnable;
import net.hywave.auth.Auth;

public class ScoreboardUtil {
    private static final String[] TITLE_COLORS = {
        "§b§l", "§6§l", "§e§l", "§f§l"
    };
    
    public static void setAuthScoreboard(Player player, boolean isRegistered) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("auth", "dummy");
        objective.setDisplayName(TITLE_COLORS[0] + "AUTHENTICATION");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (!isRegistered) {
            objective.getScore("§r ").setScore(8);
            objective.getScore("§f§lWelcome to Hywave!").setScore(7);
            objective.getScore("§7Register to play:").setScore(6);
            objective.getScore("§b/register <password>").setScore(5);
            objective.getScore("§r  ").setScore(4);
            objective.getScore("     §bWWW.HYWAVE.NET     ").setScore(1);
        } else {
            objective.getScore("§r ").setScore(8);
            objective.getScore("§f§lWelcome back, " + player.getName() + "!").setScore(7);
            objective.getScore("§7Login to play:").setScore(6);
            objective.getScore("§b/login <password>").setScore(5);
            objective.getScore("§r   ").setScore(2);
            objective.getScore("     §bWWW.HYWAVE.NET     ").setScore(1);
        }

        player.setScoreboard(scoreboard);
        startPulsingEffect(player, objective, isRegistered);
    }

    private static void startPulsingEffect(Player player, Objective objective, boolean isRegistered) {
        new BukkitRunnable() {
            int phase = 0;
            long lastChange = System.currentTimeMillis();
            boolean bright = true;
            
            @Override
            public void run() {
                if (player == null || !player.isOnline() || 
                    player.getScoreboard() == null || 
                    player.getScoreboard().getObjective("auth") == null) {
                    this.cancel();
                    return;
                }

                long now = System.currentTimeMillis();
                long delay = 1000 + (phase * 500);
                
                if (now - lastChange >= delay) {
                    objective.setDisplayName((bright ? "§b§l" : "§f§l") + "AUTHENTICATION");
                    bright = !bright;
                    lastChange = now;
                    
                    if (!bright) {
                        phase = (phase + 1) % 3;
                    }
                }
            }
        }.runTaskTimer(Auth.getInstance(), 0L, 2L);
    }

    public static void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public static void setQueueScoreboard(Player player, int position, int totalQueue) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("queue", "dummy");
        objective.setDisplayName("§6§lQUEUE STATUS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore("§7§m------------------").setScore(6);
        objective.getScore("§r ").setScore(5);
        objective.getScore("§fPosition: §e#" + position).setScore(4);
        objective.getScore("§fIn Queue: §a" + totalQueue).setScore(3);
        objective.getScore("§r  ").setScore(2);
        objective.getScore("§7Please wait...").setScore(1);
        objective.getScore("§7§m------------------§r").setScore(0);

        player.setScoreboard(scoreboard);
    }
} 