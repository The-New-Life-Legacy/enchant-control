package com.testnalpaca.enchantcontrol.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import com.testnalpaca.enchantcontrol.managers.PluginManager;

public class EnchantEventListener implements Listener {

    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        if (!PluginManager.getInstance().isBlockingEnabled()) {
            return;
        }

        EnchantmentOffer[] offers = event.getOffers();
        if (offers == null || offers.length == 0) {
            return;
        }

        for (int i = 0; i < offers.length; i++) {
            EnchantmentOffer offer = offers[i];
            if (offer == null) {
                continue;
            }

            Enchantment enchantment = offer.getEnchantment();
            if (!PluginManager.getInstance().isEnchantmentBlocked(enchantment)) {
                continue;
            }

            if (PluginManager.getInstance().shouldSwitchEnchantments()) {
                ItemStack item = event.getItem();
                Enchantment replacement = PluginManager.getInstance().getRandomAllowedEnchantment(item, enchantment);
                if (replacement != null) {
                    offer.setEnchantment(replacement);
                    offer.setEnchantmentLevel(Math.min(offer.getEnchantmentLevel(), replacement.getMaxLevel()));
                } else {
                    offers[i] = null;
                }
            } else {
                offers[i] = null;
            }
        }
    }

    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!PluginManager.getInstance().isBlockingEnabled()) {
            return;
        }

        MerchantRecipe recipe = event.getRecipe();
        if (recipe == null) {
            return;
        }

        ItemStack result = recipe.getResult();
        ItemStack sanitized = PluginManager.getInstance().sanitizeItem(result);
        if (sanitized == null) {
            sanitized = new ItemStack(result.getType(), result.getAmount());
        }

        if (sanitized != result) {
            MerchantRecipe newRecipe = new MerchantRecipe(sanitized, recipe.getMaxUses());
            newRecipe.setIngredients(recipe.getIngredients());
            newRecipe.setDemand(recipe.getDemand());
            newRecipe.setPriceMultiplier(recipe.getPriceMultiplier());
            newRecipe.setVillagerExperience(recipe.getVillagerExperience());
            newRecipe.setExperienceReward(recipe.hasExperienceReward());
            newRecipe.setSpecialPrice(recipe.getSpecialPrice());
            newRecipe.setIgnoreDiscounts(recipe.shouldIgnoreDiscounts());
            newRecipe.setUses(recipe.getUses());
            event.setRecipe(newRecipe);
        }
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (!PluginManager.getInstance().isBlockingEnabled()) {
            return;
        }

        List<ItemStack> loot = event.getLoot();
        if (loot == null || loot.isEmpty()) {
            return;
        }

        List<ItemStack> sanitizedLoot = new ArrayList<>(loot.size());
        for (ItemStack item : loot) {
            ItemStack sanitized = PluginManager.getInstance().sanitizeItem(item);
            if (sanitized != null) {
                sanitizedLoot.add(sanitized);
            }
        }

        event.setLoot(sanitizedLoot);
    }
}
