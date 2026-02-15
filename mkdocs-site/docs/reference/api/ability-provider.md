# AbilityProvider Interface

SPI interface for external modules to provide abilities.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Interface Declaration

```java
public interface AbilityProvider
```

---

## Overview

`AbilityProvider` is the base interface for external modules that provide abilities. Implementations are discovered using Java's ServiceLoader SPI.

**Discovery**: ServiceLoader SPI via `META-INF/services`

**Extended by**: `AbilityModule` (adds lifecycle hooks)

---

## Methods

### `getAbilities()`

```java
Collection<Ability> getAbilities()
```

Returns the collection of abilities provided by this provider. Called once during plugin initialization.

**Returns**: Collection of abilities to register

**Example**:

```java
@Override
public Collection<Ability> getAbilities() {
    return List.of(
        new FireballAbility(),
        new HealingAbility(),
        new DashAbility()
    );
}
```

---

### `getProviderId()`

```java
String getProviderId()
```

Returns a unique identifier for this provider.

**Returns**: Provider ID (must be unique)

**Requirements**:

- Must be unique across all providers
- Alphanumeric with underscores/hyphens recommended
- Used for logging and identification

**Example**:

```java
@Override
public String getProviderId() {
    return "my-abilities";
}
```

---

## Implementation Example

### Basic Provider

```java
package com.example.abilities;

import xyz.rishabhvenu.abilityengine.api.*;
import java.util.Collection;
import java.util.List;

public class MyAbilityProvider implements AbilityProvider {
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
}
```

---

## ServiceLoader Registration

### Creating the Service File

Create `src/main/resources/META-INF/services/xyz.rishabhvenu.abilityengine.api.AbilityProvider`:

```
com.example.abilities.MyAbilityProvider
```

### Multiple Providers

One file can list multiple providers:

```
com.example.abilities.CombatProvider
com.example.abilities.MovementProvider
com.example.abilities.UtilityProvider
```

---

## Loading Process

1. Plugin scans `plugins/AbilityEngine/modules/` for JAR files
2. Creates URLClassLoader for each JAR
3. ServiceLoader discovers `AbilityProvider` implementations
4. Calls `getAbilities()` for each provider
5. Registers all returned abilities

---

## Module Packaging

### Directory Structure

```
src/main/
├── java/
│   └── com/example/abilities/
│       ├── MyAbilityProvider.java
│       ├── FireballAbility.java
│       └── HealingAbility.java
└── resources/
    └── META-INF/
        └── services/
            └── xyz.rishabhvenu.abilityengine.api.AbilityProvider
```

### Build Configuration

```groovy
plugins {
    id 'java'
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT'
    compileOnly files('libs/ability-engine-api.jar')
}
```

### Deployment

```bash
# Build
./gradlew build

# Deploy
cp build/libs/my-abilities-1.0.0.jar /path/to/server/plugins/AbilityEngine/modules/
```

---

## Comparison with AbilityModule

### AbilityProvider (Simple)

**Use when**:

- Just providing abilities
- No lifecycle management needed
- No listeners or tasks

**Example**:

```java
public class SimpleProvider implements AbilityProvider {
    @Override
    public Collection<Ability> getAbilities() {
        return List.of(new FireballAbility());
    }
    
    @Override
    public String getProviderId() {
        return "simple";
    }
}
```

### AbilityModule (Advanced)

**Use when**:

- Need `onEnable`/`onDisable` hooks
- Registering listeners or tasks
- Dynamic ability registration
- Resource management

**Example**:

```java
public class AdvancedModule implements AbilityModule {
    @Override
    public void onEnable(AbilityRegistry registry,
                         CooldownManager cooldowns,
                         AbilityItemService items) {
        // Lifecycle management
    }
    
    @Override
    public void onDisable() {
        // Cleanup
    }
    
    @Override
    public Collection<Ability> getAbilities() {
        return List.of(new FireballAbility());
    }
    
    @Override
    public String getProviderId() {
        return "advanced";
    }
}
```

---

## Best Practices

### Unique Provider IDs

Use namespaced IDs to avoid conflicts:

```java
@Override
public String getProviderId() {
    return "myusername:my-abilities";
}
```

### Immutable Abilities

Return immutable abilities from `getAbilities()`:

```java
@Override
public Collection<Ability> getAbilities() {
    return List.of(  // Immutable list
        new FireballAbility(),
        new HealingAbility()
    );
}
```

### Error Handling

Handle errors gracefully:

```java
@Override
public Collection<Ability> getAbilities() {
    try {
        return loadAbilitiesFromConfig();
    } catch (Exception e) {
        System.err.println("Failed to load abilities: " + e);
        return List.of();  // Return empty, don't crash
    }
}
```

---

## Verification

### Check Module Loaded

```
/ability module list
```

Console output:

```
[AbilityEngine] Loaded modules:
[AbilityEngine] - my-abilities (5 abilities)
```

### Check Abilities Registered

```
/ability list
```

---

## Troubleshooting

### Provider Not Discovered

**Check**:

1. ServiceLoader file exists: `META-INF/services/xyz.rishabhvenu.abilityengine.api.AbilityProvider`
2. File contains correct fully-qualified class name
3. Provider class implements `AbilityProvider`
4. JAR is in `plugins/AbilityEngine/modules/`

### Abilities Not Registering

**Check**:

1. `getAbilities()` returns non-empty collection
2. Ability IDs are unique
3. Check console for errors

---

## See Also

- [AbilityModule](ability-module.md) - Extended interface with lifecycle
- [Ability](ability.md) - Ability interface
- [Module Development Guide](../../guides/module-development.md) - Complete guide
