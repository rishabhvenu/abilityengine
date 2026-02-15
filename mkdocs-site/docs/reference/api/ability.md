# Ability Interface

The core interface that all abilities must implement.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Interface Declaration

```java
public interface Ability
```

---

## Overview

`Ability` is the fundamental interface in AbilityEngine. Every ability, whether created via YAML, JavaScript, or Java, is represented as an implementation of this interface.

**Key Responsibilities**:

- Define unique ability identifier
- Specify trigger types
- Define execution conditions
- Implement ability logic
- Set cooldown duration

---

## Methods

### `id()`

```java
String id()
```

Returns the unique identifier for this ability.

**Returns**: The ability ID (must be unique across all abilities)

**Requirements**:

- Must be unique
- Case-sensitive
- Should be alphanumeric with underscores/hyphens
- Used in commands and internal lookups

**Example**:

```java
@Override
public String id() {
    return "fireball";
}
```

---

### `triggers()`

```java
Collection<TriggerType> triggers()
```

Returns the trigger types that can activate this ability.

**Returns**: Collection of trigger types (must not be empty)

**Example**:

```java
@Override
public Collection<TriggerType> triggers() {
    return List.of(
        TriggerType.RIGHT_CLICK,
        TriggerType.SHIFT_RIGHT_CLICK
    );
}
```

**See Also**: [TriggerType](trigger-type.md)

---

### `conditions()`

```java
List<Condition> conditions()
```

Returns the conditions that must be met for this ability to execute. All conditions are evaluated with AND logic.

**Returns**: List of conditions (empty list means no conditions)

**Example**:

```java
@Override
public List<Condition> conditions() {
    return List.of(
        Conditions.sneaking(),
        Conditions.healthAbove(5.0)
    );
}
```

**See Also**: [Condition](conditions.md)

---

### `execute(AbilityContext)`

```java
void execute(AbilityContext context)
```

Executes the ability logic.

**Parameters**:

- `context` - The execution context containing player, trigger, targets, etc.

**Example**:

```java
@Override
public void execute(AbilityContext context) {
    var player = context.player();
    player.launchProjectile(Fireball.class);
    player.sendMessage("§cFireball launched!");
}
```

**See Also**: [AbilityContext](ability-context.md)

---

### `cooldown()`

```java
Duration cooldown()
```

Returns the cooldown duration for this ability.

**Returns**: Cooldown duration, or `Duration.ZERO` for no cooldown

**Example**:

```java
@Override
public Duration cooldown() {
    return Duration.ofSeconds(3);
}

// No cooldown
@Override
public Duration cooldown() {
    return Duration.ZERO;
}
```

---

## Implementation Example

### Basic Implementation

```java
public class FireballAbility implements Ability {
    @Override
    public String id() {
        return "fireball";
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
        var fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(2.0f);
        player.sendMessage("§cFireball!");
    }
    
    @Override
    public Duration cooldown() {
        return Duration.ofSeconds(3);
    }
}
```

### Configurable Implementation

```java
public class ConfigurableAbility implements Ability {
    private final String id;
    private final List<TriggerType> triggers;
    private final List<Condition> conditions;
    private final Duration cooldown;
    private final Consumer<AbilityContext> executor;
    
    public ConfigurableAbility(String id,
                               List<TriggerType> triggers,
                               List<Condition> conditions,
                               Duration cooldown,
                               Consumer<AbilityContext> executor) {
        this.id = id;
        this.triggers = triggers;
        this.conditions = conditions;
        this.cooldown = cooldown;
        this.executor = executor;
    }
    
    @Override
    public String id() {
        return id;
    }
    
    @Override
    public Collection<TriggerType> triggers() {
        return triggers;
    }
    
    @Override
    public List<Condition> conditions() {
        return conditions;
    }
    
    @Override
    public void execute(AbilityContext context) {
        executor.accept(context);
    }
    
    @Override
    public Duration cooldown() {
        return cooldown;
    }
}
```

---

## Usage

### Registering Abilities

```java
AbilityRegistry registry = getRegistry();
Ability ability = new FireballAbility();
registry.register(ability);
```

### Manual Execution

```java
Ability ability = registry.get("fireball");
if (ability != null) {
    var context = AbilityContext.of(player, TriggerType.RIGHT_CLICK);
    
    // Check conditions
    boolean conditionsMet = ability.conditions().stream()
        .allMatch(condition -> condition.test(context));
    
    if (conditionsMet) {
        ability.execute(context);
    }
}
```

---

## Design Notes

### Immutability

Ability implementations should be immutable or thread-safe. The same instance may be used concurrently for multiple players.

**Good**:

```java
public class ThreadSafeAbility implements Ability {
    private final String id;  // Immutable
    private final Duration cooldown;  // Immutable
    
    // ...
}
```

**Bad**:

```java
public class UnsafeAbility implements Ability {
    private int useCount;  // Mutable shared state!
    
    @Override
    public void execute(AbilityContext context) {
        useCount++;  // Race condition!
    }
}
```

### Performance

The `execute()` method should complete quickly. For long-running operations, use sessions or async tasks.

---

## See Also

- [AbilityContext](ability-context.md) - Execution context
- [TriggerType](trigger-type.md) - Trigger types enum
- [Condition](conditions.md) - Condition system
- [AbilityRegistry](ability-registry.md) - Ability registration
- [AbilitySession](ability-session.md) - Stateful abilities
