package net.hywave.auth.commands;

import net.hywave.auth.Auth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public class EmergencyLockdownCommand implements CommandExecutor {
    private final Auth plugin;

    public EmergencyLockdownCommand(Auth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("auth.admin.lockdown")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /lockdown <activate|deactivate> [reason]");
            return true;
        }

        UUID executorUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;

        switch (args[0].toLowerCase()) {
            case "activate":
                String reason = args.length > 1 ? 
                    String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : 
                    "No reason specified";
                plugin.getEmergencyLockdown().activateLockdown(reason, executorUUID);
                sender.sendMessage("§aEmergency lockdown activated!");
                break;

            case "deactivate":
                plugin.getEmergencyLockdown().deactivateLockdown(executorUUID);
                sender.sendMessage("§aEmergency lockdown deactivated!");
                break;

            default:
                sender.sendMessage("§cInvalid option. Use 'activate' or 'deactivate'");
        }

        return true;
    }
} 