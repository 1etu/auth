package net.hywave.auth.commands;

import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthPlayer;
import net.hywave.auth.utils.BackupCodeGenerator;
import net.hywave.auth.utils.BackupCodesMap;
import net.hywave.auth.utils.PasswordUtils;
import net.hywave.auth.utils.ScoreboardUtil;
import net.hywave.auth.utils.TimeoutBar;
import net.hywave.auth.utils.WebServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class RegisterCommand implements CommandExecutor {
    private final Auth plugin;

    public RegisterCommand(Auth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getManagerFactory().getDatabaseManager().loadPlayer(player.getUniqueId()) != null) {
            player.sendMessage("§c§lERROR! §7An account with this username already exists!");
            player.sendMessage("§7Please login using §b/login <password>");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§c§lERROR! §7Usage: §f/register <password>");
            return true;
        }

        String password = args[0];
        if (password.length() < 6) {
            player.sendMessage("§c§lERROR! §7Password must be at least 6 characters long!");
            return true;
        }

        List<String> backupCodes = BackupCodeGenerator.generateCodes(8);
        
        AuthPlayer authPlayer = new AuthPlayer(
            player.getUniqueId(),
            player.getName(),
            PasswordUtils.hashPassword(password)
        );
        authPlayer.setBackupCodes(backupCodes);

        plugin.getManagerFactory().getDatabaseManager().savePlayer(
            authPlayer,
            player.getAddress().getAddress().getHostAddress()
        );

        BackupCodesMap.showBackupCodes(player, backupCodes);
        
        TextComponent enable2FA = new TextComponent("§c§l[Two-Factor Authentication]");
        enable2FA.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/setup2fa"));
        enable2FA.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§7Click here to enable two-factor authentication").create()));
        player.spigot().sendMessage(enable2FA);
        
        AuthPlayer.addAuthPlayer(authPlayer);
        authPlayer.setAuthenticated(true);
        plugin.getSecurityListener().authenticatePlayer(player);
        ScoreboardUtil.removeScoreboard(player);
        
        TimeoutBar timeoutBar = plugin.getAuthListener().getTimeoutBar(player.getUniqueId());
        if (timeoutBar != null) {
            timeoutBar.cancel();
        }

        player.removePotionEffect(PotionEffectType.BLINDNESS);

        player.setPlayerListName("§r" + player.getName());

        String backupCodesUrl = WebServer.generateBackupCodesUrl(player.getUniqueId(), backupCodes);

        TextComponent downloadButton = new TextComponent("§6§l[Download Backup Codes]");
        downloadButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§7Click here to download your backup codes").create()));
        downloadButton.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, backupCodesUrl));

        for (int i = 0; i < 100; i++) {
            player.sendMessage("");
        }

        player.sendMessage("§a§lSuccessfully registered!\n§7You can download your backup codes or enable two-factor authentication using the buttons below. No staff member will ever ask for your password!");
        player.sendMessage("");
        TextComponent spacer = new TextComponent("  ");
        
        TextComponent[] buttons = {downloadButton, spacer, enable2FA};
        player.spigot().sendMessage(buttons);
        return true;
    }
} 