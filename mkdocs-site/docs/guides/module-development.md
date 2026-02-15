# Module Development Guide

Build reusable ability modules in Java that can be distributed as JAR files.

---

## Overview

Ability modules are external JAR files that provide abilities through the `AbilityProvider` or `AbilityModule` interface. They're discovered automatically using Java's ServiceLoader SPI.

**Benefits**:

- **Type safety** - Full compile-time checking
- **IDE support** - Autocomplete, refactoring, debugging
- **Reusability** - Distribute modules across servers
- **Performance** - Compiled Java code
- **Lifecycle hooks** - `onEnable`/`onDisable` for resource management

---

## Quick Start

### 1. Create a Gradle Project

```groovy
plugins {
    id 'java'
}

group = 'com.example'
version = '1.0.0'

repositories {
    mavenCentral()
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT'
    compileOnly files('/path/to/ability-engine-api-1.0.0-SNAPSHOT.jar')
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

### 2. Implement AbilityModule

```java
package com.example.abilities;

import xyz.rishabhvenu.abilityengine.api.*;
import java.util.Collection;
import java.util.List;

public class MyAbilityModule implements AbilityModule {
    
    @Override
    public void onEnable(AbilityRegistry registry, 
                         CooldownManager cooldowns, 
                         AbilityItemService items) {
        // Called when module loads
        System.out.println("MyAbilityModule enabled!");
        
        // Register custom listeners, tasks, etc.
    }
    
    @Override
    public void onDisable() {
        // Called when module unloads
        System.out.println("MyAbilityModule disabled!");
        
        // Clean up resources
    }
    
    @Override
    public Collection<Ability> getAbilities() {
        return List.of(
            new FireballAbility(),
            new HealingAbility()
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
}
```

### 3. Create Ability Implementations

```java
package com.example.abilities;

import xyz.rishabhvenu.abilityengine.api.*;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

public class FireballAbility implements Ability {
    
    @Override
    public String id() {
        return "custom_fireball";
    }
    
    @Override
    public Collection<TriggerType> triggers() {
        return List.of(TriggerType.RIGHT_CLICK);
    }
    
    @Override
    public List<Condition> conditions() {
        return List.of(Conditions.sneaking());
    }
    
    @Override
    public void execute(AbilityContext context) {
        var player = context.player();
        player.launchProjectile(org.bukkit.entity.Fireball.class);
        player.sendMessage("§cCustom Fireball!");
    }
    
    @Override
    public Duration cooldown() {
        return Duration.ofSeconds(3);
    }
}
```

### 4. Register with ServiceLoader

Create `src/main/resources/META-INF/services/xyz.rishabhvenu.abilityengine.api.AbilityProvider`:

```
com.example.abilities.MyAbilityModule
```

### 5. Build and Deploy

```bash
./gradlew build

# Copy to server
cp build/libs/my-abilities-1.0.0.jar /path/to/server/plugins/AbilityEngine/modules/
```

---

## Interface Choices

### AbilityProvider (Simple)

Use for basic modules that just provide abilities:

```java
public class SimpleProvider implements AbilityProvider {
    @Override
    public Collection<Ability> getAbilities() {
        return List.of(new MyAbility());
    }
    
    @Override
    public String getProviderId() {
        return "simple-provider";
    }
}
```

### AbilityModule (Advanced)

Use for modules that need lifecycle management:

```java
public class AdvancedModule implements AbilityModule {
    private BukkitTask task;
    
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
        task = Bukkit.getScheduler().runTaskTimer(
            getPlugin(),
            () -> { /* ... */ },
            0L, 20L
        );
        
        // Dynamically register abilities
        registry.register(new DynamicAbility());
    }
    
    @Override
    public void onDisable() {
        // Cancel tasks
        if (task != null) {
            task.cancel();
        }
        
        // Clean up resources
    }
    
    @Override
    public Collection<Ability> getAbilities() {
        return List.of(new MyAbility());
    }
    
    @Override
    public String getProviderId() {
        return "advanced-module";
    }
}
```

---

## Ability Implementation Patterns

### Basic Ability

```java
public class BasicAbility implements Ability {
    @Override
    public String id() {
        return "basic_ability";
    }
    
    @Override
    public Collection<TriggerType> triggers() {
        return List.of(TriggerType.RIGHT_CLICK);
    }
    
    @Override
    public List<Condition> conditions() {
        return List.of();
    }
    
    @Override
    public void execute(AbilityContext context) {
        context.player().sendMessage("Basic ability executed!");
    }
    
    @Override
    public Duration cooldown() {
        return Duration.ZERO;
    }
}
```

### With Custom Conditions

```java
public class ConditionalAbility implements Ability {
    @Override
    public String id() {
        return "conditional_ability";
    }
    
    @Override
    public Collection<TriggerType> triggers() {
        return List.of(TriggerType.RIGHT_CLICK);
    }
    
    @Override
    public List<Condition> conditions() {
        return List.of(
            Conditions.sneaking(),
            Conditions.healthAbove(5.0),
            // Custom condition
            context -> context.player().getWorld().getName().equals("world_nether")
        );
    }
    
    @Override
    public void execute(AbilityContext context) {
        context.player().sendMessage("Conditions met!");
    }
    
    @Override
    public Duration cooldown() {
        return Duration.ofSeconds(10);
    }
}
```

### With Configuration

```java
public class ConfigurableAbility implements Ability {
    private final String id;
    private final int cooldownSeconds;
    private final double damage;
    
    public ConfigurableAbility(String id, int cooldown, double damage) {
        this.id = id;
        this.cooldownSeconds = cooldown;
        this.damage = damage;
    }
    
    @Override
    public String id() {
        return id;
    }
    
    @Override
    public Collection<TriggerType> triggers() {
        return List.of(TriggerType.RIGHT_CLICK_ENTITY);
    }
    
    @Override
    public List<Condition> conditions() {
        return List.of(Conditions.hasTarget());
    }
    
    @Override
    public void execute(AbilityContext context) {
        if (context.targetEntity() instanceof LivingEntity target) {
            target.damage(damage, context.player());
        }
    }
    
    @Override
    public Duration cooldown() {
        return Duration.ofSeconds(cooldownSeconds);
    }
}
```

---

## Advanced Features

### Dynamic Registration

Register abilities dynamically in `onEnable`:

```java
@Override
public void onEnable(AbilityRegistry registry,
                     CooldownManager cooldowns,
                     AbilityItemService items) {
    // Load from config
    var config = loadConfig();
    
    for (var abilityConfig : config.getAbilities()) {
        registry.register(new ConfigurableAbility(
            abilityConfig.id(),
            abilityConfig.cooldown(),
            abilityConfig.damage()
        ));
    }
}
```

### Event Listening

Register Bukkit event listeners:

```java
public class MyModule implements AbilityModule {
    private Listener listener;
    
    @Override
    public void onEnable(AbilityRegistry registry,
                         CooldownManager cooldowns,
                         AbilityItemService items) {
        listener = new MyListener(registry, cooldowns);
        Bukkit.getPluginManager().registerEvents(
            listener,
            getPluginInstance()
        );
    }
    
    @Override
    public void onDisable() {
        HandlerList.unregisterAll(listener);
    }
    
    // ... other methods
}
```

### Session-Based Abilities

Create abilities that use sessions:

```java
public class AuraAbility implements Ability {
    @Override
    public String id() {
        return "fire_aura";
    }
    
    @Override
    public Collection<TriggerType> triggers() {
        return List.of(TriggerType.RIGHT_CLICK);
    }
    
    @Override
    public List<Condition> conditions() {
        return List.of();
    }
    
    @Override
    public void execute(AbilityContext context) {
        var player = context.player();
        var sessionManager = getSessionManager(); // Get from module
        
        sessionManager.startSession(player, this, new BaseAbilitySession(player, this) {
            @Override
            public void tick() {
                if (getTickCount() > 200) { // 10 seconds
                    end();
                    return;
                }
                
                if (getTickCount() % 20 == 0) { // Every second
                    damageNearbyEntities(player);
                }
            }
            
            @Override
            public void end() {
                super.end();
                player.sendMessage("§cAura ended!");
            }
        });
    }
    
    @Override
    public Duration cooldown() {
        return Duration.ofSeconds(30);
    }
}
```

---

## ServiceLoader Registration

### Creating the Service File

Create `src/main/resources/META-INF/services/xyz.rishabhvenu.abilityengine.api.AbilityProvider`:

```
com.example.abilities.MyAbilityModule
```

**Multiple Providers**:

```
com.example.abilities.CombatModule
com.example.abilities.MovementModule
com.example.abilities.UtilityModule
```

### Directory Structure

```
src/main/
├── java/
│   └── com/example/abilities/
│       ├── MyAbilityModule.java
│       ├── FireballAbility.java
│       └── HealingAbility.java
└── resources/
    └── META-INF/
        └── services/
            └── xyz.rishabhvenu.abilityengine.api.AbilityProvider
```

---

## Building and Packaging

### Gradle Build Script

```groovy
plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

group = 'com.example'
version = '1.0.0'

repositories {
    mavenCentral()
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT'
    compileOnly files('libs/ability-engine-api-1.0.0-SNAPSHOT.jar')
    
    // Include dependencies in JAR if needed
    implementation 'com.google.code.gson:gson:2.10.1'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

shadowJar {
    archiveClassifier.set('')
    
    // Relocate dependencies to avoid conflicts
    relocate 'com.google.gson', 'com.example.abilities.libs.gson'
}
```

### Build Commands

```bash
# Build JAR
./gradlew shadowJar

# Output: build/libs/my-abilities-1.0.0.jar
```

---

## Deployment

### Installation

1. Build the module JAR
2. Copy to `plugins/AbilityEngine/modules/`
3. Restart the server (or use hot reload if implemented)

```bash
cp build/libs/my-abilities-1.0.0.jar /path/to/server/plugins/AbilityEngine/modules/
```

### Verification

Check server console for load message:

```
[AbilityEngine] Loading module: My Abilities Pack v1.0.0
[AbilityEngine] Loaded 5 abilities from module 'my-abilities'
```

Or use command:

```
/ability module list
```

---

## Best Practices

### Package Structure

```
com.example.abilities/
├── MyAbilityModule.java        # Main module class
├── abilities/                  # Ability implementations
│   ├── FireballAbility.java
│   ├── HealingAbility.java
│   └── DashAbility.java
├── conditions/                 # Custom conditions
│   └── CustomConditions.java
└── utils/                      # Utilities
    └── AbilityUtils.java
```

### Error Handling

```java
@Override
public void execute(AbilityContext context) {
    try {
        // Your code
        context.player().sendMessage("Success!");
    } catch (Exception e) {
        context.player().sendMessage("§cAbility failed!");
        e.printStackTrace();
    }
}
```

### Logging

```java
public class MyModule implements AbilityModule {
    private static final Logger LOGGER = Logger.getLogger("MyAbilities");
    
    @Override
    public void onEnable(...) {
        LOGGER.info("MyAbilities enabled");
    }
    
    @Override
    public void onDisable() {
        LOGGER.info("MyAbilities disabled");
    }
}
```

### Resource Cleanup

```java
@Override
public void onDisable() {
    // Cancel all tasks
    Bukkit.getScheduler().cancelTasks(plugin);
    
    // Unregister listeners
    HandlerList.unregisterAll(plugin);
    
    // Close resources
    if (connection != null) {
        connection.close();
    }
}
```

---

## Testing

### Unit Testing

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FireballAbilityTest {
    @Test
    void testAbilityId() {
        var ability = new FireballAbility();
        assertEquals("fireball", ability.id());
    }
    
    @Test
    void testCooldown() {
        var ability = new FireballAbility();
        assertEquals(Duration.ofSeconds(3), ability.cooldown());
    }
}
```

### Integration Testing

Test on a local server:

1. Build module JAR
2. Copy to test server's `modules/` folder
3. Start server and test abilities
4. Use `/ability list` to verify registration
5. Use `/ability info <ability_id>` to check details

---

## Common Patterns

### Ability Factory

```java
public class AbilityFactory {
    public static Ability createProjectile(String id, Class<? extends Projectile> type, int cooldown) {
        return new Ability() {
            @Override
            public String id() { return id; }
            
            @Override
            public Collection<TriggerType> triggers() {
                return List.of(TriggerType.RIGHT_CLICK);
            }
            
            @Override
            public List<Condition> conditions() {
                return List.of(Conditions.sneaking());
            }
            
            @Override
            public void execute(AbilityContext context) {
                context.player().launchProjectile(type);
            }
            
            @Override
            public Duration cooldown() {
                return Duration.ofSeconds(cooldown);
            }
        };
    }
}

// Usage
@Override
public Collection<Ability> getAbilities() {
    return List.of(
        AbilityFactory.createProjectile("fireball", Fireball.class, 3),
        AbilityFactory.createProjectile("arrow", Arrow.class, 1),
        AbilityFactory.createProjectile("snowball", Snowball.class, 0)
    );
}
```

### Builder Pattern

```java
public class AbilityBuilder {
    private String id;
    private List<TriggerType> triggers = new ArrayList<>();
    private List<Condition> conditions = new ArrayList<>();
    private Duration cooldown = Duration.ZERO;
    private Consumer<AbilityContext> executor;
    
    public AbilityBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    public AbilityBuilder trigger(TriggerType... triggers) {
        this.triggers.addAll(Arrays.asList(triggers));
        return this;
    }
    
    public AbilityBuilder condition(Condition... conditions) {
        this.conditions.addAll(Arrays.asList(conditions));
        return this;
    }
    
    public AbilityBuilder cooldown(Duration cooldown) {
        this.cooldown = cooldown;
        return this;
    }
    
    public AbilityBuilder execute(Consumer<AbilityContext> executor) {
        this.executor = executor;
        return this;
    }
    
    public Ability build() {
        return new Ability() {
            @Override
            public String id() { return id; }
            
            @Override
            public Collection<TriggerType> triggers() { return triggers; }
            
            @Override
            public List<Condition> conditions() { return conditions; }
            
            @Override
            public void execute(AbilityContext context) {
                executor.accept(context);
            }
            
            @Override
            public Duration cooldown() { return cooldown; }
        };
    }
}

// Usage
var ability = new AbilityBuilder()
    .id("custom_fireball")
    .trigger(TriggerType.RIGHT_CLICK)
    .condition(Conditions.sneaking())
    .cooldown(Duration.ofSeconds(3))
    .execute(ctx -> ctx.player().launchProjectile(Fireball.class))
    .build();
```

---

## Troubleshooting

### Module not loading

**Check**:

1. JAR is in `plugins/AbilityEngine/modules/`
2. ServiceLoader file exists and has correct path
3. Module implements `AbilityProvider` or `AbilityModule`
4. Check console for error messages

### Abilities not registering

**Check**:

1. `getAbilities()` returns non-empty collection
2. Ability IDs are unique
3. No exceptions in `onEnable()`

### ClassNotFoundException

**Solution**: Shade dependencies into your JAR or mark as `compileOnly` if provided by server

---

## Next Steps

- [API Reference](../reference/api/ability.md) - Complete API documentation
- [Sessions Guide](sessions.md) - Deep dive into session-based abilities
- [Items Guide](items.md) - Working with ability items

---

## Example Module

See the complete example module in the AbilityEngine repository:

`examples/example-module/` - Full working module with multiple abilities, conditions, and lifecycle management.
