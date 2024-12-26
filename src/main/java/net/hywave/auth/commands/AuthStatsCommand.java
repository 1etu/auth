package net.hywave.auth.commands;

import net.hywave.auth.Auth;
import net.hywave.auth.gui.AuthStatsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuthStatsCommand implements CommandExecutor {
    private final Auth plugin;

    public AuthStatsCommand(Auth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!sender.hasPermission("auth.admin.stats")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        new AuthStatsGUI(plugin).show((Player) sender);
        return true;
    }
} 