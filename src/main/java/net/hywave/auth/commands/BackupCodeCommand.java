package net.hywave.auth.commands;

import net.hywave.auth.Auth;
import net.hywave.auth.utils.BackupCodeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackupCodeCommand implements CommandExecutor {
    @SuppressWarnings("unused")
    private final Auth plugin;
    private final BackupCodeGUI backupCodeGUI;

    public BackupCodeCommand(Auth plugin) {
        this.plugin = plugin;
        this.backupCodeGUI = new BackupCodeGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        backupCodeGUI.openBackupCodeGUI(player);
        return true;
    }
} 