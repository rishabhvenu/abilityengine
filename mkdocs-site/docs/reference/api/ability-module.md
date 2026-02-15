# AbilityModule Interface

Extended interface for external modules with lifecycle management.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Interface Declaration

```java
public interface AbilityModule extends AbilityProvider
```

---

## Overview

`AbilityModule` extends `AbilityProvider` with lifecycle hooks (`onEnable`/`onDisable`). Use this for modules that need to register listeners, schedule tasks, or perform setup/cleanup.

**Extends**: `AbilityProvider`

**Discovery**: ServiceLoader SPI (same as AbilityProvider)

---

## Methods

### Lifecycle Methods

#### `onEnable(...)`

```java
void onEnable(AbilityRegistry registry, 
              CooldownManager cooldowns, 
              AbilityItemService items)
```

Called when the module is loaded and enabled.

**Parameters**:

- `registry` - The ability registry for dynamic registration
- `cooldowns` - The cooldown manager
- `items` - The ability item service

**Use for**:

- Initializing resources
- Registering listeners
- Scheduling tasks
- Loading configuration
- Dynamic ability registration

**Example**:

```java
@Override
public void onEnable(AbilityRegistry registry,
                     CooldownManager cooldowns,
                     AbilityItemService items) {
    // Register listeners
    Bukkit.getPluginManager().registerEvents(
        new MyListener(),
        getPlugin()
    );
    
    // Schedule tasks
    Bukkit.getScheduler().runTaskTimer(
        getPlugin(),
        this::tick,
        0L, 20L
    );
    
    // Dynamic registration
    registry.register(new DynamicAbility());
    
    getLogger().info(getModuleName() + " enabled");
}
```

---

#### `onDisable()`

```java
void onDisable()
```

Called when the module is disabled (plugin shutdown or reload).

**Use for**:

- Cancelling tasks
- Unregistering listeners
- Closing resources
- Saving data
- Cleanup

**Example**:

```java
@Override
public void onDisable() {
    // Cancel tasks
    Bukkit.getScheduler().cancelTasks(getPlugin());
    
    // Unregister listeners
    HandlerList.unregisterAll(getPlugin());
    
    // Close resources
    if (database != null) {
        database.close();
    }
    
    getLogger().info(getModuleName() + " disabled");
}
```

---

### Metadata Methods

#### `getModuleName()`

```java
default String getModuleName()
```

Returns the human-readable module name. Defaults to the provider ID.

**Returns**: Module name

**Example**:

```java
@Override
public String getModuleName() {
    return "My Abilities Pack";
}
```

---

#### `getModuleVersion()`

```java
default String getModuleVersion()
```

Returns the module version.

**Returns**: Module version string

**Default**: `"1.0.0"`

**Example**:

```java
@Override
public String getModuleVersion() {
    return "2.1.0";
}
```

---

### Inherited from AbilityProvider

#### `getAbilities()`

```java
Collection<Ability> getAbilities()
```

Returns the collection of abilities provided by this module.

---

#### `getProviderId()`

```java
String getProviderId()
```

Returns a unique identifier for this module.

---

## Complete Implementation Example

```java
package com.example.abilities;

import xyz.rishabhvenu.abilityengine.api.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class MyAbilityModule implements AbilityModule {
    private static final Logger LOGGER = Logger.getLogger("MyAbilities");
    private BukkitTask task;
    private MyListener listener;
    
    @Override
    public void onEnable(AbilityRegistry registry,
                         CooldownManager cooldowns,
                         AbilityItemService items) {
        LOGGER.info("Enabling " + getModuleName() + " v" + getModuleVersion());
        
        // Register event listeners
        listener = new MyListener(registry, cooldowns);
        Bukkit.getPluginManager().registerEvents(
            listener,
            getAbilityEnginePlugin()
        );
        
        // Schedule repeating task
        task = Bukkit.getScheduler().runTaskTimer(
            getAbilityEnginePlugin(),
            this::tick,
            0L,
            20L  // Every second
        );
        
        // Load config and dynamically register abilities
        loadConfigAbilities(registry);
        
        LOGGER.info("Loaded " + getAbilities().size() + " abilities");
    }
    
    @Override
    public void onDisable() {
        LOGGER.info("Disabling " + getModuleName());
        
        // Cancel tasks
        if (task != null) {
            task.cancel();
        }
        
        // Unregister listeners
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        
        // Clean up resources
        cleanup();
        
        LOGGER.info(getModuleName() + " disabled");
    }
    
    @Override
    public Collection<Ability> getAbilities() {
        return List.of(
            new FireballAbility(),
            new HealingAbility(),
            new DashAbility()
        );
    }
    
    @Override
    public String getProviderId() {
        return "my-abilities";
    }
    
    @Override
    public String getModuleName() {
        return "My Abilities Pack";
    }
    
    @Override
    public String getModuleVersion() {
        return "1.0.0";
    }
    
    private void tick() {
        // Repeating task logic
    }
    
    private void loadConfigAbilities(AbilityRegistry registry) {
        // Load from config
    }
    
    private void cleanup() {
        // Cleanup logic
    }
    
    private Plugin getAbilityEnginePlugin() {
        return Bukkit.getPluginManager().getPlugin("AbilityEngine");
    }
}
```

---

## ServiceLoader Registration

Create `src/main/resources/META-INF/services/xyz.rishabhvenu.abilityengine.api.AbilityProvider`:

```
com.example.abilities.MyAbilityModule
```

**Note**: Register as `AbilityProvider`, not `AbilityModule`. The loader checks for `AbilityModule` at runtime.

---

## Usage Patterns

### With Event Listeners

```java
public class ListenerModule implements AbilityModule {
    private MyListener listener;
    
    @Override
    public void onEnable(AbilityRegistry registry, ...) {
        listener = new MyListener();
        Bukkit.getPluginManager().registerEvents(listener, getPlugin());
    }
    
    @Override
    public void onDisable() {
        HandlerList.unregisterAll(listener);
    }
    
    // ... other methods
}
```

### With Scheduled Tasks

```java
public class TaskModule implements AbilityModule {
    private BukkitTask task;
    
    @Override
    public void onEnable(AbilityRegistry registry, ...) {
        task = Bukkit.getScheduler().runTaskTimer(
            getPlugin(),
            this::update,
            0L, 20L
        );
    }
    
    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
        }
    }
    
    private void update() {
        // Task logic
    }
    
    // ... other methods
}
```

### With Configuration

```java
public class ConfigModule implements AbilityModule {
    private FileConfiguration config;
    
    @Override
    public void onEnable(AbilityRegistry registry, ...) {
        loadConfig();
        
        // Register abilities from config
        for (String key : config.getKeys(false)) {
            registry.register(createAbilityFromConfig(key));
        }
    }
    
    @Override
    public void onDisable() {
        saveConfig();
    }
    
    private void loadConfig() {
        // Load config logic
    }
    
    private void saveConfig() {
        // Save config logic
    }
    
    // ... other methods
}
```

---

## Lifecycle Guarantee

**onEnable** is called:

- After plugin enable
- Before abilities are registered
- In order of module discovery

**onDisable** is called:

- Before plugin disable
- After abilities are unregistered
- In reverse order of enable

---

## Best Practices

### Resource Management

Always clean up in `onDisable()`:

```java
@Override
public void onDisable() {
    // Cancel ALL tasks
    Bukkit.getScheduler().cancelTasks(plugin);
    
    // Unregister ALL listeners
    HandlerList.unregisterAll(plugin);
    
    // Close ALL resources
    closeDatabase();
    closeConnections();
}
```

### Error Handling

Handle errors gracefully:

```java
@Override
public void onEnable(AbilityRegistry registry, ...) {
    try {
        // Setup logic
    } catch (Exception e) {
        LOGGER.severe("Failed to enable module: " + e.getMessage());
        e.printStackTrace();
        // Module still loads but with reduced functionality
    }
}
```

### Logging

Use consistent logging:

```java
private static final Logger LOGGER = Logger.getLogger("MyModule");

@Override
public void onEnable(...) {
    LOGGER.info("Enabling " + getModuleName() + " v" + getModuleVersion());
    // ...
    LOGGER.info("Successfully enabled " + getAbilities().size() + " abilities");
}
```

---

## See Also

- [AbilityProvider](ability-provider.md) - Base interface
- [AbilityRegistry](ability-registry.md) - Registry interface
- [Module Development Guide](../../guides/module-development.md) - Complete guide
