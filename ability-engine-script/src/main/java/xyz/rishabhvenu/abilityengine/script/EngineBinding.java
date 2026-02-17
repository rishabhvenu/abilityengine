package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.api.*;
import xyz.rishabhvenu.abilityengine.core.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The global 'engine' object exposed to all scripts.
 * Provides the primary scripting API.
 */
public final class EngineBinding {
    
    private final Plugin plugin;
    private final AbilityRegistry registry;
    private final CooldownManager cooldownManager;
    private final AbilityItemService itemService;
    private final SessionManager sessionManager;
    private final EventTriggerRegistry eventTriggerRegistry;
    private final AbilityStateStore stateStore;
    private final BossBarManager bossBarManager;
    private final ScriptContext scriptContext;
    private final ExecutionTracker executionTracker;
    private final PhaseBindings phaseBindings;
    private final EntityControlManager entityControlManager;
    private final InterruptManager interruptManager;
    
    // Sub-APIs
    public final TriggerConstants trigger = new TriggerConstants();
    public final ConditionBindings condition = new ConditionBindings();
    public final SessionBindings sessions;
    public final CooldownBindings cooldowns;
    public final ItemBindings items;
    public final StateBindings state;
    public final UIBindings ui;
    public final EffectsBindings effects;
    public final ProjectileBindings projectile;
    public final AreaEffectBindings areaEffect;
    public final RaycastBindings raycastBindings;
    public final MovementBindings movement;
    public final ControlBindings control;
    
    public EngineBinding(
            Plugin plugin,
            AbilityRegistry registry,
            CooldownManager cooldownManager,
            AbilityItemService itemService,
            SessionManager sessionManager,
            EventTriggerRegistry eventTriggerRegistry,
            AbilityStateStore stateStore,
            BossBarManager bossBarManager,
            ScriptContext scriptContext,
            ExecutionTracker executionTracker,
            EntityControlManager entityControlManager,
            InterruptManager interruptManager) {
        this.plugin = plugin;
        this.registry = registry;
        this.cooldownManager = cooldownManager;
        this.itemService = itemService;
        this.sessionManager = sessionManager;
        this.eventTriggerRegistry = eventTriggerRegistry;
        this.stateStore = stateStore;
        this.bossBarManager = bossBarManager;
        this.scriptContext = scriptContext;
        this.executionTracker = executionTracker;
        this.phaseBindings = new PhaseBindings(plugin);
        this.entityControlManager = entityControlManager;
        this.interruptManager = interruptManager;
        
        this.sessions = new SessionBindings(sessionManager);
        this.cooldowns = new CooldownBindings(cooldownManager);
        this.items = new ItemBindings(itemService, registry, plugin, scriptContext);
        this.state = new StateBindings(stateStore);
        this.ui = new UIBindings(bossBarManager, plugin);
        this.effects = new EffectsBindings();
        this.projectile = new ProjectileBindings(plugin, scriptContext);
        this.areaEffect = new AreaEffectBindings(plugin);
        this.raycastBindings = new RaycastBindings();
        this.movement = new MovementBindings(plugin);
        this.control = new ControlBindings(entityControlManager);
    }
    
    /**
     * Performs a raycast with block and entity detection.
     * Convenience method that delegates to raycastBindings.
     */
    public Value raycast(Value config) {
        return raycastBindings.raycast(config, scriptContext.getGraalContext());
    }
    
    /**
     * Creates and registers a named item template with auto-wired abilities.
     */
    public void item(Value config) {
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("item() requires a config object");
        }
        
        String itemId = config.getMember("id").asString();
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item must have an id");
        }
        
        // Extract abilities array
        if (!config.hasMember("abilities")) {
            throw new IllegalArgumentException("Item must have an abilities array");
        }
        
        Value abilitiesValue = config.getMember("abilities");
        if (!abilitiesValue.hasArrayElements()) {
            throw new IllegalArgumentException("abilities must be an array");
        }
        
        // Build enriched abilities array with trigger info from registry
        com.google.gson.JsonArray enrichedAbilities = new com.google.gson.JsonArray();
        long size = abilitiesValue.getArraySize();
        
        for (long i = 0; i < size; i++) {
            String abilityId = abilitiesValue.getArrayElement(i).asString();
            Ability ability = registry.get(abilityId);
            
            if (ability == null) {
                warn("Item " + itemId + " references unknown ability: " + abilityId);
                continue;
            }
            
            // Get primary trigger from ability
            Collection<TriggerType> triggers = ability.triggers();
            String primaryTrigger = triggers.isEmpty() ? "RIGHT_CLICK" : triggers.iterator().next().name();
            
            com.google.gson.JsonObject entry = new com.google.gson.JsonObject();
            entry.addProperty("id", abilityId);
            entry.addProperty("trigger", primaryTrigger);
            enrichedAbilities.add(entry);
        }
        
        // Create modified config with enriched abilities
        org.graalvm.polyglot.Context polyglotCtx = scriptContext.getGraalContext();
        Value modifiedConfig = polyglotCtx.eval("js", "({})");
        
        // Copy all fields from original config
        for (String key : config.getMemberKeys()) {
            if (!key.equals("abilities")) {
                modifiedConfig.putMember(key, config.getMember(key));
            }
        }
        
        // Add enriched abilities as array of objects
        Value abilitiesArray = polyglotCtx.eval("js", "[]");
        for (int i = 0; i < enrichedAbilities.size(); i++) {
            com.google.gson.JsonObject entry = enrichedAbilities.get(i).getAsJsonObject();
            Value abilityObj = polyglotCtx.eval("js", "({})");
            abilityObj.putMember("id", entry.get("id").getAsString());
            abilityObj.putMember("trigger", entry.get("trigger").getAsString());
            abilitiesArray.setArrayElement(i, abilityObj);
        }
        modifiedConfig.putMember("abilities", abilitiesArray);
        
        // Create the item using the extended item builder
        ItemStack template = items.create(modifiedConfig);
        
        // Store template in script context
        scriptContext.registerItemTemplate(itemId, template);
        
        log("Registered item template: " + itemId);
    }
    
    /**
     * Registers an ability from a JS config object.
     * 
     * @param config JS object with id, triggers, conditions, cooldown, execute
     */
    public void ability(Value config) {
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("ability() requires a config object");
        }
        
        // Extract ID
        String id = config.getMember("id").asString();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Ability must have an id");
        }
        
        // Extract triggers (or single trigger)
        Value triggersValue = config.getMember("triggers");
        
        // Support singular "trigger" as alias
        if ((triggersValue == null || triggersValue.isNull()) && config.hasMember("trigger")) {
            Value singleTrigger = config.getMember("trigger");
            if (singleTrigger.isString()) {
                // Wrap in array handling
                triggersValue = singleTrigger;
            }
        }
        
        Collection<TriggerType> triggers = parseTriggers(triggersValue);
        
        // Extract conditions
        Value conditionsValue = config.getMember("conditions");
        List<Condition> conditions = parseConditions(conditionsValue);
        
        // Extract cooldown (in seconds or as object)
        Value cooldownValue = config.getMember("cooldown");
        Duration cooldown = Duration.ZERO;
        boolean showBossBar = false;
        String bossBarColor = "GREEN";
        String bossBarLabel = null;
        
        if (cooldownValue != null && !cooldownValue.isNull()) {
            if (cooldownValue.isNumber()) {
                // Simple form: cooldown: 5
                cooldown = Duration.ofSeconds(cooldownValue.asLong());
            } else if (cooldownValue.hasMembers()) {
                // Extended form: cooldown: { seconds: 5, showBossBar: true, ... }
                Value secondsVal = cooldownValue.getMember("seconds");
                if (secondsVal != null && secondsVal.isNumber()) {
                    cooldown = Duration.ofSeconds(secondsVal.asLong());
                }
                
                Value showBarVal = cooldownValue.getMember("showBossBar");
                if (showBarVal != null && showBarVal.isBoolean()) {
                    showBossBar = showBarVal.asBoolean();
                }
                
                Value colorVal = cooldownValue.getMember("bossBarColor");
                if (colorVal != null && colorVal.isString()) {
                    bossBarColor = colorVal.asString();
                }
                
                Value labelVal = cooldownValue.getMember("bossBarLabel");
                if (labelVal != null && labelVal.isString()) {
                    bossBarLabel = labelVal.asString();
                }
            }
        }
        
        // Extract permission
        Value permissionValue = config.getMember("permission");
        String permission = null;
        if (permissionValue != null && !permissionValue.isNull() && permissionValue.isString()) {
            permission = permissionValue.asString();
        }
        
        // Extract execution function - support onTrigger alias
        Value executeFunc = config.getMember("execute");
        Value onTriggerFunc = config.getMember("onTrigger");
        
        // onTrigger takes priority, execute is fallback for backward compat
        Value primaryFunc = (onTriggerFunc != null && onTriggerFunc.canExecute()) 
            ? onTriggerFunc 
            : executeFunc;
            
        if (primaryFunc == null || !primaryFunc.canExecute()) {
            throw new IllegalArgumentException("Ability must have an execute or onTrigger function");
        }
        
        // Extract lifecycle hooks
        Value onProjectileHit = config.hasMember("onProjectileHit") ? config.getMember("onProjectileHit") : null;
        Value onProjectileTick = config.hasMember("onProjectileTick") ? config.getMember("onProjectileTick") : null;
        Value onExpire = config.hasMember("onExpire") ? config.getMember("onExpire") : null;
        Value onCancel = config.hasMember("onCancel") ? config.getMember("onCancel") : null;
        
        // Extract phases
        Value phasesValue = config.hasMember("phases") ? config.getMember("phases") : null;
        
        // Extract onInterrupt callback
        Value onInterrupt = config.hasMember("onInterrupt") ? config.getMember("onInterrupt") : null;
        
        // Extract interrupts
        java.util.Set<InterruptType> interruptTypes = new java.util.HashSet<>();
        if (config.hasMember("interrupts") && config.getMember("interrupts").hasArrayElements()) {
            Value interruptsArray = config.getMember("interrupts");
            long size = interruptsArray.getArraySize();
            for (long i = 0; i < size; i++) {
                String interruptName = interruptsArray.getArrayElement(i).asString();
                try {
                    interruptTypes.add(InterruptType.valueOf(interruptName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    warn("Unknown interrupt type: " + interruptName);
                }
            }
        }
        
        // Create and register the ability
        ScriptAbility ability = new ScriptAbility(
            id, 
            triggers, 
            conditions, 
            cooldown,
            permission,
            showBossBar,
            bossBarColor,
            bossBarLabel != null ? bossBarLabel : id,
            primaryFunc,
            onProjectileHit,
            onProjectileTick,
            onExpire,
            onCancel,
            phasesValue,
            onInterrupt,
            interruptTypes,
            scriptContext,
            stateStore,
            plugin,
            bossBarManager,
            cooldownManager,
            executionTracker,
            phaseBindings,
            scriptContext.getGraalContext(),
            entityControlManager,
            interruptManager
        );
        registry.register(ability);
        scriptContext.trackAbility(id);
        
        log("Registered ability: " + id);
    }
    
    /**
     * Registers a listener for a Bukkit event.
     * 
     * @param eventClassName Event class name (short or fully qualified)
     * @param handler JS function to handle the event
     */
    public void listen(String eventClassName, Value handler) {
        if (!handler.canExecute()) {
            throw new IllegalArgumentException("listen() requires a function as the second parameter");
        }
        
        // Resolve event class
        Class<? extends Event> eventClass = resolveEventClass(eventClassName);
        if (eventClass == null) {
            throw new IllegalArgumentException("Unknown event class: " + eventClassName);
        }
        
        // Create event executor
        EventExecutor executor = (listener, event) -> {
            try {
                handler.execute(event);
            } catch (Exception e) {
                plugin.getLogger().warning("Error in script event listener for " + eventClassName + ": " + e.getMessage());
            }
        };
        
        // Register the listener
        RegisteredListener registeredListener = new RegisteredListener(
            new org.bukkit.event.Listener() {},
            executor,
            EventPriority.NORMAL,
            plugin,
            false
        );
        
        Bukkit.getPluginManager().registerEvent(
            eventClass,
            registeredListener.getListener(),
            EventPriority.NORMAL,
            executor,
            plugin,
            false
        );
        
        scriptContext.trackEventListener(registeredListener);
        log("Registered event listener: " + eventClassName);
    }
    
    /**
     * Schedules a delayed task.
     * 
     * @param func JS function to execute
     * @param delayTicks Delay in ticks
     * @return Task ID
     */
    public int scheduleDelayed(Value func, long delayTicks) {
        if (!func.canExecute()) {
            throw new IllegalArgumentException("scheduleDelayed() requires a function");
        }
        
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                func.execute();
            } catch (Exception e) {
                plugin.getLogger().warning("Error in scheduled task: " + e.getMessage());
            }
        }, delayTicks).getTaskId();
        
        scriptContext.trackScheduledTask(taskId);
        return taskId;
    }
    
    /**
     * Schedules a repeating task.
     * 
     * @param func JS function to execute
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     * @return Task ID
     */
    public int scheduleRepeating(Value func, long delayTicks, long periodTicks) {
        if (!func.canExecute()) {
            throw new IllegalArgumentException("scheduleRepeating() requires a function");
        }
        
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                func.execute();
            } catch (Exception e) {
                plugin.getLogger().warning("Error in repeating task: " + e.getMessage());
            }
        }, delayTicks, periodTicks).getTaskId();
        
        scriptContext.trackScheduledTask(taskId);
        return taskId;
    }
    
    /**
     * Cancels a scheduled task.
     * 
     * @param taskId Task ID
     */
    public void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }
    
    /**
     * Logs an info message.
     */
    public void log(String message) {
        plugin.getLogger().info("[Script] " + message);
    }
    
    /**
     * Logs a warning message.
     */
    public void warn(String message) {
        plugin.getLogger().warning("[Script] " + message);
    }
    
    /**
     * Logs an error message.
     */
    public void error(String message) {
        plugin.getLogger().severe("[Script] " + message);
    }
    
    // Helper methods
    
    private Collection<TriggerType> parseTriggers(Value triggersValue) {
        List<TriggerType> triggers = new ArrayList<>();
        
        if (triggersValue == null || triggersValue.isNull()) {
            triggers.add(TriggerType.RIGHT_CLICK);
            return triggers;
        }
        
        // Support single string value
        if (triggersValue.isString()) {
            String triggerName = triggersValue.asString();
            try {
                triggers.add(TriggerType.valueOf(triggerName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                warn("Unknown trigger type: " + triggerName);
            }
            return triggers.isEmpty() ? List.of(TriggerType.RIGHT_CLICK) : triggers;
        }
        
        if (triggersValue.hasArrayElements()) {
            long size = triggersValue.getArraySize();
            for (long i = 0; i < size; i++) {
                Value element = triggersValue.getArrayElement(i);
                String triggerName = element.asString();
                try {
                    triggers.add(TriggerType.valueOf(triggerName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    warn("Unknown trigger type: " + triggerName);
                }
            }
        }
        
        if (triggers.isEmpty()) {
            triggers.add(TriggerType.RIGHT_CLICK);
        }
        
        return triggers;
    }
    
    private List<Condition> parseConditions(Value conditionsValue) {
        List<Condition> conditions = new ArrayList<>();
        
        if (conditionsValue == null || conditionsValue.isNull() || !conditionsValue.hasArrayElements()) {
            return conditions;
        }
        
        long size = conditionsValue.getArraySize();
        for (long i = 0; i < size; i++) {
            Value element = conditionsValue.getArrayElement(i);
            
            // Each element should be a Condition returned by engine.condition.*
            // Or a JS function
            if (element.canExecute()) {
                // Custom JS function
                conditions.add(ctx -> {
                    try {
                        Object result = element.execute(ctx);
                        return result instanceof Boolean && (Boolean) result;
                    } catch (Exception e) {
                        return false;
                    }
                });
            } else if (element.isHostObject()) {
                // Java Condition object
                conditions.add(element.asHostObject());
            }
        }
        
        return conditions;
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends Event> resolveEventClass(String className) {
        // Try short name first (e.g., "PlayerJoinEvent")
        if (!className.contains(".")) {
            // Check common packages
            String[] packages = {
                "org.bukkit.event.player.",
                "org.bukkit.event.entity.",
                "org.bukkit.event.block.",
                "org.bukkit.event.inventory.",
                "org.bukkit.event.server.",
                "org.bukkit.event.world."
            };
            
            for (String pkg : packages) {
                try {
                    return (Class<? extends Event>) Class.forName(pkg + className);
                } catch (ClassNotFoundException e) {
                    // Try next package
                }
            }
        }
        
        // Try fully qualified name
        try {
            return (Class<? extends Event>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    // Inner binding classes
    
    public static final class CooldownBindings {
        private final CooldownManager cooldownManager;
        
        CooldownBindings(CooldownManager cooldownManager) {
            this.cooldownManager = cooldownManager;
        }
        
        public boolean isReady(Player player, String abilityId) {
            return cooldownManager.isReady(player, abilityId);
        }
        
        public void set(Player player, String abilityId, double seconds) {
            cooldownManager.setCooldown(player, abilityId, Duration.ofMillis((long) (seconds * 1000)));
        }
        
        public double remaining(Player player, String abilityId) {
            return cooldownManager.getRemainingCooldown(player.getUniqueId(), abilityId).toMillis() / 1000.0;
        }
    }
    
    public static final class ItemBindings {
        private final AbilityItemService itemService;
        private final AbilityRegistry registry;
        private final Plugin plugin;
        private final ScriptContext scriptContext;
        
        ItemBindings(AbilityItemService itemService, AbilityRegistry registry, Plugin plugin, ScriptContext scriptContext) {
            this.itemService = itemService;
            this.registry = registry;
            this.plugin = plugin;
            this.scriptContext = scriptContext;
        }
        
        /**
         * Creates an ability item (simple string form).
         */
        public ItemStack create(String abilityId) {
            return itemService.createAbilityItem(abilityId);
        }
        
        /**
         * Creates an ability item from a rich config object.
         */
        public ItemStack create(Value config) {
            if (config.isString()) {
                // String form - delegate to simple create
                return create(config.asString());
            }
            
            if (!config.hasMembers()) {
                throw new IllegalArgumentException("create() requires a string or config object");
            }
            
            // Parse material type
            String typeStr = getString(config, "type", "STICK");
            org.bukkit.Material material;
            try {
                material = org.bukkit.Material.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = org.bukkit.Material.STICK;
            }
            
            ItemStack item = new ItemStack(material);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return item;
            }
            
            // Parse display name with & color codes
            String name = getString(config, "name", null);
            if (name != null) {
                meta.displayName(parseColoredText(name));
            }
            
            // Parse lore with & color codes
            if (config.hasMember("lore")) {
                Value loreValue = config.getMember("lore");
                if (loreValue.hasArrayElements()) {
                    java.util.List<net.kyori.adventure.text.Component> loreComponents = new java.util.ArrayList<>();
                    long size = loreValue.getArraySize();
                    for (long i = 0; i < size; i++) {
                        String line = loreValue.getArrayElement(i).asString();
                        loreComponents.add(parseColoredText(line));
                    }
                    meta.lore(loreComponents);
                }
            }
            
            // Set unbreakable
            if (getBool(config, "unbreakable", false)) {
                meta.setUnbreakable(true);
            }
            
            // Apply enchantments
            if (config.hasMember("enchantments")) {
                Value enchants = config.getMember("enchantments");
                if (enchants.hasMembers()) {
                    for (String enchantKey : enchants.getMemberKeys()) {
                        try {
                            org.bukkit.enchantments.Enchantment ench = 
                                org.bukkit.enchantments.Enchantment.getByName(enchantKey.toUpperCase());
                            if (ench != null) {
                                int level = enchants.getMember(enchantKey).asInt();
                                meta.addEnchant(ench, level, true);
                            }
                        } catch (Exception e) {
                            // Skip invalid enchantments
                        }
                    }
                }
            }
            
            // Set PDC data
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey abilityIdKey = new org.bukkit.NamespacedKey(plugin, "ability_id");
            org.bukkit.NamespacedKey abilitiesKey = new org.bukkit.NamespacedKey(plugin, "abilities");
            org.bukkit.NamespacedKey versionKey = new org.bukkit.NamespacedKey(plugin, "item_version");
            
            // Handle abilities array
            if (config.hasMember("abilities")) {
                Value abilitiesValue = config.getMember("abilities");
                if (abilitiesValue.hasArrayElements()) {
                    com.google.gson.JsonArray abilitiesArray = new com.google.gson.JsonArray();
                    long size = abilitiesValue.getArraySize();
                    String primaryAbilityId = null;
                    
                    for (long i = 0; i < size; i++) {
                        Value abilityEntry = abilitiesValue.getArrayElement(i);
                        if (abilityEntry.hasMembers()) {
                            String abilityId = abilityEntry.getMember("id").asString();
                            String trigger = abilityEntry.getMember("trigger").asString();
                            
                            com.google.gson.JsonObject entry = new com.google.gson.JsonObject();
                            entry.addProperty("id", abilityId);
                            entry.addProperty("trigger", trigger.toUpperCase());
                            abilitiesArray.add(entry);
                            
                            if (primaryAbilityId == null) {
                                primaryAbilityId = abilityId;
                            }
                        }
                    }
                    
                    if (primaryAbilityId != null) {
                        pdc.set(abilityIdKey, org.bukkit.persistence.PersistentDataType.STRING, primaryAbilityId);
                    }
                    pdc.set(abilitiesKey, org.bukkit.persistence.PersistentDataType.STRING, 
                            new com.google.gson.Gson().toJson(abilitiesArray));
                    pdc.set(versionKey, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
                }
            } else if (config.hasMember("abilityId")) {
                // Single ability form
                String abilityId = config.getMember("abilityId").asString();
                pdc.set(abilityIdKey, org.bukkit.persistence.PersistentDataType.STRING, abilityId);
                pdc.set(versionKey, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
            }
            
            item.setItemMeta(meta);
            return item;
        }
        
        public boolean isAbilityItem(ItemStack item) {
            return itemService.isAbilityItem(item);
        }
        
        public String getAbilityId(ItemStack item) {
            return itemService.getAbilityId(item);
        }
        
        /**
         * Gives an item to a player. Supports both ability IDs and item template IDs.
         */
        public void give(org.bukkit.entity.Player player, String itemIdOrAbilityId) {
            ItemStack item = null;
            
            // First try to find an item template
            ItemStack template = scriptContext.getItemTemplate(itemIdOrAbilityId);
            if (template != null) {
                item = template.clone();
            } else {
                // Fall back to creating ability item directly
                item = create(itemIdOrAbilityId);
            }
            
            if (item != null) {
                player.getInventory().addItem(item);
            }
        }
        
        private String getString(Value config, String key, String defaultValue) {
            if (!config.hasMember(key)) return defaultValue;
            Value v = config.getMember(key);
            return v.isString() ? v.asString() : defaultValue;
        }
        
        private boolean getBool(Value config, String key, boolean defaultValue) {
            if (!config.hasMember(key)) return defaultValue;
            Value v = config.getMember(key);
            return v.isBoolean() ? v.asBoolean() : defaultValue;
        }
        
        private net.kyori.adventure.text.Component parseColoredText(String text) {
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(text);
        }
    }
}
