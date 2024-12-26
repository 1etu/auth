package net.hywave.auth.commands;

import net.hywave.auth.Auth;
import net.hywave.auth.managers.DatabaseManager;
import net.hywave.auth.models.AuthPlayer;
import net.hywave.auth.utils.ScoreboardUtil;
import net.hywave.auth.utils.TimeoutBar;
import net.hywave.auth.utils.TwoFactorManager;
import net.hywave.auth.utils.RateLimiter;
import net.hywave.auth.utils.CaptchaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.Arrays;

public class LoginCommand implements CommandExecutor {
    private final Auth plugin;

    public LoginCommand(Auth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        
        AuthPlayer authPlayer = AuthPlayer.getAuthPlayer(player.getUniqueId());
        if (authPlayer != null && authPlayer.isAuthenticated()) {
            player.sendMessage("§cYou are already logged in!");
            return true;
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        UUID uuid = player.getUniqueId();

        if (RateLimiter.isIpBlocked(ip)) {
            long remainingTime = RateLimiter.getRemainingCooldown(ip) / 60000;
            player.sendMessage("§cToo many failed attempts. Please try again in " + remainingTime + " minutes.");
            return true;
        }

        if (RateLimiter.isCaptchaRequired(ip)) {
            CaptchaManager.generateCaptcha(player);
            player.sendMessage("§ePlease complete the captcha verification first!");
            return true;
        }

        if (RateLimiter.isAccountLocked(uuid)) {
            long remainingTime = RateLimiter.getRemainingLockTime(uuid) / 60000;
            player.sendMessage("§cThis account is temporarily locked. Please try again in " + remainingTime + " minutes.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /login <password>");
            return true;
        }

        String password = args[0];
        DatabaseManager dbManager = plugin.getManagerFactory().getDatabaseManager();

        if (dbManager.verifyPassword(uuid, password)) {
            String secret = dbManager.get2FASecret(uuid);
            if (secret != null) {
                open2FAGUI(player);
                return true;
            }

            completeLogin(player);
        } else {
            handleFailedLogin(player);
        }

        return true;
    }

    private void completeLogin(Player player) {
        AuthPlayer authPlayer = AuthPlayer.getAuthPlayer(player.getUniqueId());
        if (authPlayer != null) {
            authPlayer.setAuthenticated(true);
            
            if (!plugin.getSessionManager().createSession(player)) {
                player.kickPlayer("§cFailed to create session. Please try again.");
                return;
            }
            
            plugin.getSecurityListener().authenticatePlayer(player);
            ScoreboardUtil.removeScoreboard(player);
            
            TimeoutBar timeoutBar = plugin.getAuthListener().getTimeoutBar(player.getUniqueId());
            if (timeoutBar != null) {
                timeoutBar.cancel();
            }

            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.setPlayerListName("§r" + player.getName());
            player.sendMessage("§aLogin successful!");
        }
    }

    private void handleFailedLogin(Player player) {
        double currentMaxHealth = player.getMaxHealth();
        if (currentMaxHealth > 1.0) {
            player.setMaxHealth(currentMaxHealth - 1.0);
            player.sendMessage("§cIncorrect password! Attempts remaining: " + (int)(currentMaxHealth - 1.0));
        } else {
            player.kickPlayer("§cToo many failed attempts!");
        }
    }

    private void open2FAGUI(Player player) {
        new net.wesjd.anvilgui.AnvilGUI.Builder()
            .onClose(stateSnapshot -> {
                if (!AuthPlayer.getAuthPlayer(player.getUniqueId()).isAuthenticated()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> open2FAGUI(player));
                }
            })
            .onClick((slot, stateSnapshot) -> {
                String code = stateSnapshot.getText();
                if (TwoFactorManager.verify2FACode(player.getUniqueId(), code)) {
                    completeLogin(player);
                    return Arrays.asList(net.wesjd.anvilgui.AnvilGUI.ResponseAction.close());
                } else {
                    return Arrays.asList(net.wesjd.anvilgui.AnvilGUI.ResponseAction.replaceInputText("Wrong Code!"));
                }
            })
            .text("Verification Code")
            .itemLeft(new ItemStack(Material.PAPER))
            .title("Verification Code")
            .plugin(plugin)
            .open(player);
    }
}
