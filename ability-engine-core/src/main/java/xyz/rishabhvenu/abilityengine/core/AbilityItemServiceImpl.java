package xyz.rishabhvenu.abilityengine.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import xyz.rishabhvenu.abilityengine.api.Ability;
import xyz.rishabhvenu.abilityengine.api.AbilityItemService;
import xyz.rishabhvenu.abilityengine.api.AbilityRegistry;
import xyz.rishabhvenu.abilityengine.api.TriggerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of AbilityItemService using PersistentDataContainer.
 */
public final class AbilityItemServiceImpl implements AbilityItemService {
    
    private static final String KEY_ABILITY_ID = "ability_id";
    private static final String KEY_ABILITIES = "abilities";
    private static final String KEY_ITEM_VERSION = "item_version";
    private static final int CURRENT_VERSION = 1;
    
    private final Plugin plugin;
    private final AbilityRegistry registry;
    private final Gson gson = new Gson();
    
    private final NamespacedKey abilityIdKey;
    private final NamespacedKey abilitiesKey;
    private final NamespacedKey itemVersionKey;
    
    public AbilityItemServiceImpl(Plugin plugin, AbilityRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.abilityIdKey = new NamespacedKey(plugin, KEY_ABILITY_ID);
        this.abilitiesKey = new NamespacedKey(plugin, KEY_ABILITIES);
        this.itemVersionKey = new NamespacedKey(plugin, KEY_ITEM_VERSION);
    }
    
    @Override
    public ItemStack createAbilityItem(String abilityId) {
        Ability ability = registry.get(abilityId);
        if (ability == null) {
            return null;
        }
        
        // Create a default item (can be customized later)
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        // Set display name
        meta.displayName(net.kyori.adventure.text.Component.text(abilityId));
        
        // Set PDC data
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(abilityIdKey, PersistentDataType.STRING, abilityId);
        pdc.set(itemVersionKey, PersistentDataType.INTEGER, CURRENT_VERSION);
        
        // For single-ability items, store the primary trigger
        if (!ability.triggers().isEmpty()) {
            TriggerType primaryTrigger = ability.triggers().iterator().next();
            JsonArray abilitiesArray = new JsonArray();
            JsonObject abilityEntry = new JsonObject();
            abilityEntry.addProperty("id", abilityId);
            abilityEntry.addProperty("trigger", primaryTrigger.name());
            abilitiesArray.add(abilityEntry);
            pdc.set(abilitiesKey, PersistentDataType.STRING, gson.toJson(abilitiesArray));
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    @Override
    public boolean isAbilityItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(abilityIdKey, PersistentDataType.STRING) 
            || pdc.has(abilitiesKey, PersistentDataType.STRING);
    }
    
    @Override
    public String getAbilityId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(abilityIdKey, PersistentDataType.STRING);
    }
    
    @Override
    public List<String> getAbilities(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Collections.emptyList();
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // Check for multi-ability format
        String abilitiesJson = pdc.get(abilitiesKey, PersistentDataType.STRING);
        if (abilitiesJson != null) {
            try {
                JsonArray array = gson.fromJson(abilitiesJson, JsonArray.class);
                List<String> abilityIds = new ArrayList<>();
                for (int i = 0; i < array.size(); i++) {
                    JsonObject entry = array.get(i).getAsJsonObject();
                    abilityIds.add(entry.get("id").getAsString());
                }
                return abilityIds;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse abilities JSON: " + e.getMessage());
            }
        }
        
        // Fallback to single ability format
        String abilityId = pdc.get(abilityIdKey, PersistentDataType.STRING);
        if (abilityId != null) {
            return Collections.singletonList(abilityId);
        }
        
        return Collections.emptyList();
    }
    
    @Override
    public boolean isAbilityItem(ItemStack item, String abilityId) {
        return getAbilities(item).contains(abilityId);
    }
    
    @Override
    public TriggerType getAbilityTrigger(ItemStack item, String abilityId) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        String abilitiesJson = pdc.get(abilitiesKey, PersistentDataType.STRING);
        if (abilitiesJson != null) {
            try {
                JsonArray array = gson.fromJson(abilitiesJson, JsonArray.class);
                for (int i = 0; i < array.size(); i++) {
                    JsonObject entry = array.get(i).getAsJsonObject();
                    if (entry.get("id").getAsString().equals(abilityId)) {
                        String triggerName = entry.get("trigger").getAsString();
                        return TriggerType.valueOf(triggerName);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse ability trigger: " + e.getMessage());
            }
        }
        
        return null;
    }
}
