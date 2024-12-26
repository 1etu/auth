package net.hywave.auth.commands;

import net.hywave.auth.Auth;
import net.hywave.auth.utils.TwoFactorManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Setup2FACommand implements CommandExecutor {
    private final Auth plugin;

    public Setup2FACommand(Auth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        
        if (args.length > 0 && args[0].equals("verify")) {
            open2FASetupGUI(player);
            return true;
        }

        String secret = plugin.getManagerFactory().getDatabaseManager().get2FASecret(player.getUniqueId());
        if (secret != null) {
            player.sendMessage("§cYou have already enabled 2FA!");
            return true;
        }

        TwoFactorManager.initiate2FASetup(player);
        
        TextComponent verifyButton = new TextComponent("§a§l[Verify]");
        verifyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/setup2fa verify"));
        verifyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder("§7Click here to enter your 2FA verification code").create()));
        player.spigot().sendMessage(verifyButton);
        
        return true;
    }

    private void open2FASetupGUI(Player player) {
        new net.wesjd.anvilgui.AnvilGUI.Builder()
            .onClose(stateSnapshot -> {
                if (TwoFactorManager.isPendingSetup(player.getUniqueId()) && 
                    plugin.getManagerFactory().getDatabaseManager().get2FASecret(player.getUniqueId()) == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> open2FASetupGUI(player));
                }
            })
            .onClick((slot, stateSnapshot) -> {
                String code = stateSnapshot.getText();
                if (TwoFactorManager.verifyAndEnable2FA(player, code)) {
                    player.sendMessage("§a2FA has been successfully enabled!");
                    player.getInventory().setItem(1, null);
                    player.updateInventory();
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