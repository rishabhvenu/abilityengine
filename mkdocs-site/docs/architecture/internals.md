# Architecture Internals

Deep dive into AbilityEngine's internal implementation.

---

## Registry Implementation

### AbilityRegistryImpl

**Storage**:

```java
private final ConcurrentHashMap<String, Ability> abilities = new ConcurrentHashMap<>();
```

**Thread Safety**: ConcurrentHashMap provides lock-free reads and fine-grained locking for writes.

**Operations**:

- `register()`: `abilities.put(id, ability)` - O(1)
- `get()`: `abilities.get(id)` - O(1)
- `getAll()`: `Collections.unmodifiableCollection(abilities.values())` - O(1)
- `unregister()`: `abilities.remove(id)` - O(1)

---

## Cooldown Manager Implementation

### CooldownManagerImpl

**Storage**:

```java
private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Instant>> cooldowns;
```

**Structure**: `Map<PlayerUUID, Map<AbilityID, ExpiryTime>>`

**isReady() Logic**:

```java
public boolean isReady(UUID playerId, String abilityId) {
    var playerCooldowns = cooldowns.get(playerId);
    if (playerCooldowns == null) return true;
    
    Instant expiry = playerCooldowns.get(abilityId);
    if (expiry == null) return true;
    
    if (Instant.now().isAfter(expiry)) {
        playerCooldowns.remove(abilityId);  // Lazy cleanup
        return true;
    }
    
    return false;
}
```

**Cleanup**: Lazy - entries removed on next access after expiry.

---

## Trigger Dispatcher

### Event Listening

Registers listeners for:

- `PlayerInteractEvent`
- `PlayerInteractEntityEvent`
- `EntityDamageByEntityEvent`
- `EntityDamageEvent`
- `PlayerMoveEvent`

### Trigger Resolution

```java
private TriggerType resolveTrigger(PlayerInteractEvent event) {
    boolean sneaking = event.getPlayer().isSneaking();
    Action action = event.getAction();
    
    if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
        return sneaking ? TriggerType.SHIFT_RIGHT_CLICK : TriggerType.RIGHT_CLICK;
    }
    
    if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
        return sneaking ? TriggerType.SHIFT_LEFT_CLICK : TriggerType.LEFT_CLICK;
    }
    
    return null;
}
```

### Ability Dispatch

```java
private void dispatchAbility(Player player, TriggerType trigger, Event event) {
    ItemStack item = player.getInventory().getItemInMainHand();
    
    // Check if ability item
    List<String> abilityIds = itemService.getAbilities(item);
    if (abilityIds.isEmpty()) return;
    
    // Build context
    AbilityContext context = buildContext(player, trigger, event, item);
    
    // Execute each ability
    for (String abilityId : abilityIds) {
        Ability ability = registry.get(abilityId);
        if (ability == null) continue;
        
        // Check trigger matches
        if (!ability.triggers().contains(trigger)) continue;
        
        // Check conditions
        if (!evaluateConditions(ability, context)) continue;
        
        // Check cooldown
        if (!cooldownManager.isReady(player, abilityId)) continue;
        
        // Execute
        try {
            ability.execute(context);
            cooldownManager.setCooldown(player, abilityId, ability.cooldown());
        } catch (Exception e) {
            logger.error("Error executing ability " + abilityId, e);
        }
    }
}
```

---

## Session Manager

### Storage

```java
private final Map<UUID, List<AbilitySession>> activeSessions = new ConcurrentHashMap<>();
```

**Structure**: `Map<PlayerUUID, List<Session>>`

### Tick Loop

Single repeating task runs every tick:

```java
private void tickSessions() {
    for (var entry : activeSessions.entrySet()) {
        List<AbilitySession> sessions = entry.getValue();
        
        // Tick each session
        for (int i = sessions.size() - 1; i >= 0; i--) {
            AbilitySession session = sessions.get(i);
            
            try {
                session.tick();
            } catch (Exception e) {
                logger.error("Error ticking session", e);
                session.end();
                sessions.remove(i);
            }
            
            // Remove if no longer active
            if (!session.isActive()) {
                sessions.remove(i);
            }
        }
    }
}
```

**Performance**: O(n) where n = total active sessions across all players.

---

## Item Service Implementation

### PDC Keys

```java
private static final NamespacedKey ABILITY_ID_KEY = 
    new NamespacedKey("ability_engine", "ability_id");

private static final NamespacedKey ABILITIES_KEY = 
    new NamespacedKey("ability_engine", "abilities");

private static final NamespacedKey VERSION_KEY = 
    new NamespacedKey("ability_engine", "item_version");
```

### Creating Items

```java
public ItemStack createAbilityItem(String abilityId) {
    Ability ability = registry.get(abilityId);
    if (ability == null) return null;
    
    ItemStack item = new ItemStack(Material.STICK);
    ItemMeta meta = item.getItemMeta();
    
    // Set display name
    meta.displayName(Component.text(getDisplayName(ability)));
    
    // Add PDC data
    meta.getPersistentDataContainer().set(
        ABILITY_ID_KEY,
        PersistentDataType.STRING,
        abilityId
    );
    
    item.setItemMeta(meta);
    return item;
}
```

### Reading Items

```java
public String getAbilityId(ItemStack item) {
    if (item == null) return null;
    
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return null;
    
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    
    // Check modern format first
    if (pdc.has(ABILITIES_KEY, PersistentDataType.STRING)) {
        String json = pdc.get(ABILITIES_KEY, PersistentDataType.STRING);
        return parseAbilitiesJson(json).get(0);  // Return first
    }
    
    // Check legacy format
    return pdc.get(ABILITY_ID_KEY, PersistentDataType.STRING);
}
```

---

## Script Engine

### Context Management

Each script has its own `ScriptContext` tracking resources:

```java
class ScriptContext {
    private final List<String> registeredAbilityIds = new ArrayList<>();
    private final List<HandlerList> registeredListeners = new ArrayList<>();
    private final List<Integer> scheduledTasks = new ArrayList<>();
    private final org.graalvm.polyglot.Context graalContext;
}
```

### Loading Scripts

```java
public void loadScript(File scriptFile) {
    // Create GraalVM context
    Context graalContext = Context.newBuilder("js")
        .allowAllAccess(true)  // Full Java access
        .build();
    
    // Create resource tracking
    ScriptContext scriptContext = new ScriptContext(graalContext);
    
    // Inject engine global
    EngineBinding binding = new EngineBinding(
        registry,
        cooldownManager,
        sessionManager,
        itemService,
        scriptContext
    );
    graalContext.getBindings("js").putMember("engine", binding);
    
    // Execute script
    String code = Files.readString(scriptFile.toPath());
    graalContext.eval("js", code);
    
    // Store context
    loadedScripts.put(scriptFile.getName(), scriptContext);
}
```

### Unloading Scripts

```java
public void unloadScript(String filename) {
    ScriptContext ctx = loadedScripts.remove(filename);
    if (ctx == null) return;
    
    // Unregister abilities
    for (String abilityId : ctx.getRegisteredAbilityIds()) {
        registry.unregister(abilityId);
    }
    
    // Unregister listeners
    for (HandlerList handler : ctx.getRegisteredListeners()) {
        HandlerList.unregisterAll(handler);
    }
    
    // Cancel tasks
    for (int taskId : ctx.getScheduledTasks()) {
        Bukkit.getScheduler().cancelTask(taskId);
    }
    
    // Close GraalVM context
    ctx.getGraalContext().close();
}
```

---

## Module Loader

### JAR Loading

```java
public void loadModules(File modulesDir) {
    File[] jars = modulesDir.listFiles((dir, name) -> name.endsWith(".jar"));
    if (jars == null) return;
    
    for (File jar : jars) {
        try {
            loadModule(jar);
        } catch (Exception e) {
            logger.error("Failed to load module: " + jar.getName(), e);
        }
    }
}

private void loadModule(File jar) throws Exception {
    // Create URLClassLoader
    URL[] urls = new URL[] { jar.toURI().toURL() };
    ClassLoader parent = getClass().getClassLoader();
    URLClassLoader loader = new URLClassLoader(urls, parent);
    
    // ServiceLoader discovery
    ServiceLoader<AbilityProvider> services = 
        ServiceLoader.load(AbilityProvider.class, loader);
    
    for (AbilityProvider provider : services) {
        // Check if AbilityModule
        if (provider instanceof AbilityModule module) {
            module.onEnable(registry, cooldownManager, itemService);
        }
        
        // Register abilities
        for (Ability ability : provider.getAbilities()) {
            registry.register(ability);
        }
        
        // Track loaded module
        loadedModules.put(provider.getProviderId(), 
            new LoadedModule(provider, loader, getAbilityIds(provider)));
    }
}
```

---

## Performance Optimizations

### Lazy Cleanup

Cooldowns and expired entries are cleaned up lazily on next access rather than with a scheduled task.

### Concurrent Collections

All shared data structures use concurrent collections to minimize locking.

### Event Filtering

Trigger dispatcher only processes events for players holding ability items, skipping irrelevant events.

### Single Tick Task

Session manager uses one repeating task for all sessions rather than one per session.

---

## Error Handling

### Graceful Degradation

Errors in one ability don't affect others:

```java
try {
    ability.execute(context);
} catch (Exception e) {
    logger.error("Error in ability " + abilityId, e);
    // Continue to next ability
}
```

### Resource Cleanup

Always-executed cleanup in `finally` blocks and shutdown hooks.

---

## See Also

- [Overview](overview.md) - High-level architecture
- [API Reference](../reference/api/ability.md) - API documentation
