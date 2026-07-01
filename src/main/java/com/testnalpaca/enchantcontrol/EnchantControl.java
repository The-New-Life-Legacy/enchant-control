package com.testnalpaca.enchantcontrol;

import org.bukkit.plugin.java.JavaPlugin;
import com.testnalpaca.enchantcontrol.managers.PluginManager;
import com.testnalpaca.enchantcontrol.listeners.PlayerListener;
import com.testnalpaca.enchantcontrol.listeners.EnchantEventListener;
import com.testnalpaca.enchantcontrol.commands.EnchantControlCommand;

public class EnchantControl extends JavaPlugin {
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        PluginManager.getInstance().initialize(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new EnchantEventListener(), this);
        EnchantControlCommand command = new EnchantControlCommand();
        getCommand("enchantcontrol").setExecutor(command);
        getCommand("enchantcontrol").setTabCompleter(command);

        getLogger().info(getDescription().getName() + " has been enabled!");

        if (PluginManager.getInstance().isBlockingEnabled()) {
            getServer().getScheduler().runTask(this, () -> PluginManager.getInstance().sanitizeOnlinePlayers());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(getDescription().getName() + " has been disabled!");
    }
    
}
