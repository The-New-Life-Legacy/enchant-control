package com.testnalpaca.enchantcontrol.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import com.testnalpaca.enchantcontrol.managers.PluginManager;

public class EnchantControlCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("block", "unblock", "list", "reload", "enable", "disable");
    private static final Map<String, String> SUBCOMMAND_PERMISSIONS = Map.of(
            "block", "enchantcontrol.block",
            "unblock", "enchantcontrol.unblock",
            "list", "enchantcontrol.list",
            "reload", "enchantcontrol.reload",
            "enable", "enchantcontrol.enable",
            "disable", "enchantcontrol.disable"
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PluginManager manager = PluginManager.getInstance();

        if (args.length == 0) {
            List<String> allowed = getAllowedSubcommands(sender);
            if (allowed.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use any Enchant Control subcommands.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /enchantcontrol " + String.join("|", allowed));
            }
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (!hasPermission(sender, subcommand)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use that subcommand.");
            return true;
        }

        switch (subcommand) {
            case "block" -> handleBlock(sender, manager, args);
            case "unblock" -> handleUnblock(sender, manager, args);
            case "list" -> handleList(sender, manager);
            case "reload" -> handleReload(sender, manager);
            case "enable" -> handleToggle(sender, manager, true);
            case "disable" -> handleToggle(sender, manager, false);
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use block, unblock, list, reload, enable, or disable.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (String subcommand : SUBCOMMANDS) {
                if (hasPermission(sender, subcommand) && subcommand.startsWith(prefix)) {
                    suggestions.add(subcommand);
                }
            }
            return suggestions;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("block") || args[0].equalsIgnoreCase("unblock"))) {
            if (!hasPermission(sender, args[0].toLowerCase(Locale.ROOT))) {
                return Collections.emptyList();
            }
            return PluginManager.getInstance().getEnchantmentSuggestions(args[1]);
        }

        return Collections.emptyList();
    }

    private boolean hasPermission(CommandSender sender, String subcommand) {
        if (sender.isOp()) {
            return true;
        }
        String permission = SUBCOMMAND_PERMISSIONS.get(subcommand);
        return permission != null && sender.hasPermission(permission);
    }

    private List<String> getAllowedSubcommands(CommandSender sender) {
        List<String> allowed = new ArrayList<>();
        for (String subcommand : SUBCOMMANDS) {
            if (hasPermission(sender, subcommand)) {
                allowed.add(subcommand);
            }
        }
        return allowed;
    }

    private void handleBlock(CommandSender sender, PluginManager manager, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /enchantcontrol block <enchantment>");
            return;
        }

        String enchantName = args[1];
        Enchantment enchantment = manager.getEnchantment(enchantName);
        if (enchantment == null) {
            sender.sendMessage(ChatColor.RED + "Could not block enchantment: " + enchantName + ". The enchantment name is invalid.");
            return;
        }

        if (manager.isEnchantmentBlocked(enchantment)) {
            sender.sendMessage(ChatColor.RED + "Could not block enchantment: " + enchantName + ". That enchantment is already blocked.");
            return;
        }

        manager.addBlockedEnchantment(enchantment);
        sender.sendMessage(ChatColor.GREEN + "Blocked enchantment: " + enchantment.getKey());
    }

    private void handleUnblock(CommandSender sender, PluginManager manager, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /enchantcontrol unblock <enchantment>");
            return;
        }

        String enchantName = args[1];
        Enchantment enchantment = manager.getEnchantment(enchantName);
        if (enchantment == null) {
            sender.sendMessage(ChatColor.RED + "Could not unblock enchantment: " + enchantName + ". The enchantment name is invalid.");
            return;
        }

        if (!manager.isEnchantmentBlocked(enchantment)) {
            sender.sendMessage(ChatColor.RED + "Could not unblock enchantment: " + enchantName + ". That enchantment is not currently blocked.");
            return;
        }

        manager.removeBlockedEnchantment(enchantment);
        sender.sendMessage(ChatColor.GREEN + "Unblocked enchantment: " + enchantment.getKey());
    }

    private void handleList(CommandSender sender, PluginManager manager) {
        List<String> blocked = manager.getBlockedEnchantments();
        if (blocked.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No enchantments are currently blocked.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Blocked Enchantments:");
        blocked.forEach(entry -> sender.sendMessage(ChatColor.GRAY + " - " + entry));
    }

    private void handleReload(CommandSender sender, PluginManager manager) {
        manager.reloadConfiguration();
        sender.sendMessage(ChatColor.GREEN + "Enchant Control configuration reloaded.");
    }

    private void handleToggle(CommandSender sender, PluginManager manager, boolean enabled) {
        manager.setBlockingEnabled(enabled);
        sender.sendMessage(ChatColor.GREEN + "Blocking is now " + (enabled ? "enabled" : "disabled") + ".");
    }
}
