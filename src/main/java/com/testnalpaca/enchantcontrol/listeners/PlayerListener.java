package com.testnalpaca.enchantcontrol.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.testnalpaca.enchantcontrol.managers.PluginManager;

public class PlayerListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!PluginManager.getInstance().isBlockingEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(PluginManager.getInstance().getPlugin(), () -> {
            PluginManager.getInstance().sanitizePlayer(event.getPlayer());
        });
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!PluginManager.getInstance().isBlockingEnabled()) {
            return;
        }

        if (event.getInventory() == null) {
            return;
        }

        PluginManager.getInstance().sanitizeInventory(event.getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!PluginManager.getInstance().isBlockingEnabled()) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        boolean changed = false;

        ItemStack sanitizedCurrent = PluginManager.getInstance().sanitizeItem(currentItem);
        if (sanitizedCurrent != currentItem) {
            event.setCurrentItem(sanitizedCurrent);
            changed = true;
        }

        ItemStack sanitizedCursor = PluginManager.getInstance().sanitizeItem(cursorItem);
        if (sanitizedCursor != cursorItem) {
            event.setCursor(sanitizedCursor);
            changed = true;
        }

        if (changed) {
            Bukkit.getScheduler().runTask(PluginManager.getInstance().getPlugin(), () -> {
                Inventory top = event.getView().getTopInventory();
                Inventory bottom = event.getView().getBottomInventory();
                PluginManager.getInstance().sanitizeInventory(top);
                PluginManager.getInstance().sanitizeInventory(bottom);
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!PluginManager.getInstance().isBlockingEnabled()) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        ItemStack sanitized = PluginManager.getInstance().sanitizeItem(currentItem);
        if (sanitized != currentItem) {
            event.setCurrentItem(sanitized);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!PluginManager.getInstance().isBlockingEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(PluginManager.getInstance().getPlugin(), () -> {
            PluginManager.getInstance().sanitizeInventory(event.getInventory());
        });
    }
}
