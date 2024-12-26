package net.hywave.auth.utils;

import net.hywave.auth.Auth;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TimeoutBar {
    private final Auth plugin;
    private final Player player;
    private final int timeoutSeconds;
    private BukkitRunnable task;

    public TimeoutBar(Auth plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.timeoutSeconds = plugin.getConfig().getInt("settings.session-timeout", 60);
        startCountdown();
    }

    private void startCountdown() {
        player.setLevel(timeoutSeconds);
        player.setExp(1.0f);

        task = new BukkitRunnable() {
            int secondsLeft = timeoutSeconds;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    player.kickPlayer("§cLogin timeout exceeded");
                    cancel();
                    return;
                }

                secondsLeft--;
                player.setLevel(secondsLeft);
                player.setExp((float) secondsLeft / timeoutSeconds);
            }
        };

        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            player.setLevel(0);
            player.setExp(0f);
        }
    }

    public void stopCountdown() {
        if (task != null) {
            task.cancel();
            player.setLevel(0);
            player.setExp(0f);
        }
    }
} 