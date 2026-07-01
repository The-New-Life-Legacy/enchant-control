package com.testnalpaca.enchantcontrol.managers;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginManager {
    private static final Map<String, String> ENCHANTMENT_ALIASES = Map.ofEntries(
            Map.entry("curse_of_binding", "binding_curse"),
            Map.entry("curse_of_vanish", "vanishing_curse"),
            Map.entry("curse_of_vanishing", "vanishing_curse")
    );

    private static PluginManager instance;
    private JavaPlugin plugin;
    private boolean blockingEnabled = true;
    private boolean deleteItems = false;
    private boolean switchEnchantments = true;
    private boolean debugEnabled = false;
    private final Set<Enchantment> blockedEnchantments = new LinkedHashSet<>();
    private final Random random = new Random();
    
    private PluginManager() {}
    
    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reloadConfiguration();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public boolean isBlockingEnabled() {
        return blockingEnabled;
    }

    public boolean shouldDeleteItems() {
        return deleteItems;
    }

    public boolean shouldSwitchEnchantments() {
        return switchEnchantments;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    private void logDebug(String message) {
        if (debugEnabled && plugin != null) {
            plugin.getLogger().info("[EnchantControl DEBUG] " + message);
        }
    }

    public void reloadConfiguration() {
        if (plugin == null) {
            return;
        }

        plugin.reloadConfig();
        plugin.saveDefaultConfig();

        blockingEnabled = plugin.getConfig().getBoolean("blocking-enabled", true);
        deleteItems = plugin.getConfig().getBoolean("delete-items", false);
        switchEnchantments = plugin.getConfig().getBoolean("switch-enchantments", true);
        debugEnabled = plugin.getConfig().getBoolean("debug-enabled", false);

        blockedEnchantments.clear();
        List<String> blocked = plugin.getConfig().getStringList("blocked-enchantments");
        for (String raw : blocked) {
            Enchantment enchantment = parseEnchantment(raw);
            if (enchantment != null) {
                blockedEnchantments.add(enchantment);
            } else {
                plugin.getLogger().warning("Invalid blocked enchantment in config: " + raw);
            }
        }
    }

    public boolean isEnchantmentBlocked(Enchantment enchantment) {
        return enchantment != null && blockedEnchantments.contains(enchantment);
    }

    public Enchantment getEnchantment(String raw) {
        return parseEnchantment(raw);
    }

    public List<String> getEnchantmentSuggestions(String prefix) {
        String normalized = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(Enchantment.values())
                .flatMap(enchantment -> Stream.of(
                        enchantment.getKey().toString(),
                        enchantment.getKey().getKey(),
                        enchantment.getName().toLowerCase(Locale.ROOT)))
                .filter(name -> normalized.isEmpty() || name.startsWith(normalized))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public boolean addBlockedEnchantment(String raw) {
        Enchantment enchantment = parseEnchantment(raw);
        if (enchantment == null) {
            return false;
        }

        return addBlockedEnchantment(enchantment);
    }

    public boolean addBlockedEnchantment(Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }

        if (!blockedEnchantments.add(enchantment)) {
            return false;
        }

        saveBlockedEnchantments();
        return true;
    }

    public boolean removeBlockedEnchantment(String raw) {
        Enchantment enchantment = parseEnchantment(raw);
        if (enchantment == null) {
            return false;
        }

        return removeBlockedEnchantment(enchantment);
    }

    public boolean removeBlockedEnchantment(Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }

        if (!blockedEnchantments.remove(enchantment)) {
            return false;
        }

        saveBlockedEnchantments();
        return true;
    }

    public List<String> getBlockedEnchantments() {
        return blockedEnchantments.stream()
                .map(enchantment -> enchantment.getKey().toString())
                .collect(Collectors.toList());
    }

    public void setBlockingEnabled(boolean enabled) {
        if (plugin == null) {
            return;
        }

        blockingEnabled = enabled;
        plugin.getConfig().set("blocking-enabled", enabled);
        plugin.saveConfig();
    }

    public void sanitizeOnlinePlayers() {
        if (plugin == null) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sanitizePlayer(player);
        }
    }

    public void sanitizePlayer(Player player) {
        if (player == null || player.getInventory() == null) {
            return;
        }

        sanitizeInventory(player.getInventory());
    }

    public void sanitizeInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        ItemStack[] contents = inventory.getContents();
        for (int index = 0; index < contents.length; index++) {
            ItemStack item = contents[index];
            ItemStack sanitized = sanitizeItem(item);
            if (sanitized == null) {
                inventory.setItem(index, null);
            } else if (sanitized != item) {
                inventory.setItem(index, sanitized);
            }
        }
    }

    public ItemStack sanitizeItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return item;
        }

        Set<Enchantment> blockedOnItem = new HashSet<>();
        clone.getEnchantments().keySet().stream()
                .filter(this::isEnchantmentBlocked)
                .forEach(blockedOnItem::add);

        EnchantmentStorageMeta storageMeta = null;
        if (meta instanceof EnchantmentStorageMeta storage) {
            storageMeta = storage;
            storageMeta.getStoredEnchants().keySet().stream()
                    .filter(this::isEnchantmentBlocked)
                    .forEach(blockedOnItem::add);
        }

        if (blockedOnItem.isEmpty()) {
            return item;
        }

        if (deleteItems) {
            return null;
        }

        boolean changed = false;
        for (Enchantment blocked : blockedOnItem) {
            int blockedLevel = 1;
            if (clone.containsEnchantment(blocked)) {
                blockedLevel = clone.getEnchantmentLevel(blocked);
            } else if (storageMeta != null && storageMeta.hasStoredEnchant(blocked)) {
                blockedLevel = storageMeta.getStoredEnchantLevel(blocked);
            }

            if (clone.containsEnchantment(blocked)) {
                clone.removeEnchantment(blocked);
                changed = true;
            }

            if (storageMeta != null && storageMeta.hasStoredEnchant(blocked)) {
                storageMeta.removeStoredEnchant(blocked);
                changed = true;
            }

            if (switchEnchantments) {
                Enchantment replacement = getRandomAllowedEnchantment(clone, blocked);
                if (replacement != null) {
                    int replacementLevel = Math.min(blockedLevel, replacement.getMaxLevel());
                    if (storageMeta != null && clone.getType() == Material.ENCHANTED_BOOK) {
                        storageMeta.addStoredEnchant(replacement, replacementLevel, true);
                    } else {
                        clone.addUnsafeEnchantment(replacement, replacementLevel);
                    }
                    changed = true;
                }
            }
        }

        if (changed) {
            if (storageMeta != null) {
                clone.setItemMeta(storageMeta);
            }
            return clone;
        }

        return item;
    }

    public Enchantment getRandomAllowedEnchantment(ItemStack item, Enchantment blocked) {
        Set<Enchantment> ignored = new HashSet<>(item.getEnchantments().keySet());
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            ignored.addAll(storageMeta.getStoredEnchants().keySet());
        }
        ignored.add(blocked);

        List<Enchantment> allowed = new ArrayList<>();
        boolean isBook = item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK;

        for (Enchantment enchantment : Enchantment.values()) {
            if (isEnchantmentBlocked(enchantment) || ignored.contains(enchantment)) {
                continue;
            }

            if (isBook || enchantment.canEnchantItem(item)) {
                allowed.add(enchantment);
            }
        }

        if (allowed.isEmpty()) {
            return null;
        }

        return allowed.get(random.nextInt(allowed.size()));
    }

    private Enchantment parseEnchantment(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String original = raw.trim();
        String normalized = original.toLowerCase(Locale.ROOT).replace(' ', '_');

        int colonIndex = normalized.indexOf(':');
        String namespace = "minecraft";
        String keyPart = normalized;
        boolean hasSlashInKey = false;

        if (colonIndex >= 0) {
            namespace = normalized.substring(0, colonIndex);
            keyPart = normalized.substring(colonIndex + 1);
            hasSlashInKey = keyPart.contains("/");
        }

        keyPart = ENCHANTMENT_ALIASES.getOrDefault(keyPart, keyPart);
        String fullKey = namespace + ":" + keyPart;

        Enchantment enchantment = null;
        try {
            NamespacedKey key = NamespacedKey.fromString(fullKey);
            enchantment = Enchantment.getByKey(key);
        } catch (IllegalArgumentException ex) {
            logDebug("NamespacedKey.fromString failed for fullKey='" + fullKey + "': " + ex.getMessage());
        }

        if (enchantment == null && !hasSlashInKey) {
            try {
                NamespacedKey key = new NamespacedKey(namespace, keyPart);
                enchantment = Enchantment.getByKey(key);
            } catch (IllegalArgumentException ex) {
                logDebug("NamespacedKey constructor failed for namespace='" + namespace + "' keyPart='" + keyPart + "': " + ex.getMessage());
            }
        }

        if (enchantment == null && "minecraft".equals(namespace)) {
            try {
                enchantment = Enchantment.getByKey(NamespacedKey.minecraft(keyPart));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid key format
            }
        }

        if (enchantment == null) {
            enchantment = Enchantment.getByName(keyPart.toUpperCase(Locale.ROOT));
        }

        if (enchantment == null && normalized.contains(":")) {
            enchantment = Enchantment.getByName(normalized.toUpperCase(Locale.ROOT));
        }

        if (enchantment == null) {
            enchantment = findEnchantmentByKeyOrName(normalized, keyPart);
        }

        if (enchantment == null) {
            logDebug("Failed to resolve enchantment. raw='" + raw + "' normalized='" + normalized + "' namespace='" + namespace + "' keyPart='" + keyPart + "'");
            logDebug("Registered enchantments count=" + Enchantment.values().length);
            logDebug("Attempted key='" + fullKey + "' and keyPart='" + keyPart + "'");
        } else {
            logDebug("Resolved enchantment raw='" + raw + "' -> " + enchantment.getKey());
        }

        return enchantment;
    }

    private Enchantment findEnchantmentByKeyOrName(String normalized, String keyPart) {
        for (Enchantment enchantment : Enchantment.values()) {
            if (enchantment == null) {
                continue;
            }

            if (enchantment.getKey().toString().equals(normalized)
                    || enchantment.getKey().getKey().equals(keyPart)
                    || enchantment.getKey().getKey().equals(normalized)
                    || enchantment.getName().equalsIgnoreCase(keyPart)
                    || enchantment.getName().equalsIgnoreCase(normalized)) {
                return enchantment;
            }
        }
        return null;
    }

    private void saveBlockedEnchantments() {
        if (plugin == null) {
            return;
        }

        plugin.getConfig().set("blocked-enchantments", getBlockedEnchantments());
        plugin.saveConfig();
    }
}
