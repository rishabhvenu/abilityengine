# Condition & Conditions

Condition interface and factory class for ability conditions.

---

## Condition Interface

### Package

```java
xyz.rishabhvenu.abilityengine.api
```

### Interface Declaration

```java
@FunctionalInterface
public interface Condition
```

### Overview

`Condition` is a functional interface for evaluating whether an ability can execute. It tests against an `AbilityContext`.

### Method

```java
boolean test(AbilityContext context)
```

Tests this condition against the given context.

**Parameters**: `context` - The ability execution context

**Returns**: `true` if the condition passes, `false` otherwise

### Implementation Example

```java
public class CustomCondition implements Condition {
    @Override
    public boolean test(AbilityContext context) {
        return context.player().getWorld().getName().equals("world_nether");
    }
}
```

### Lambda Usage

```java
Condition isInNether = context -> 
    context.player().getWorld().getName().equals("world_nether");
```

---

## Conditions Factory Class

### Package

```java
xyz.rishabhvenu.abilityengine.api
```

### Class Declaration

```java
public final class Conditions
```

Static factory class for creating common conditions.

---

## Built-in Conditions

### Player State

#### `sneaking()`

```java
public static Condition sneaking()
```

Checks if the player is sneaking.

**Example**:

```java
conditions: List.of(Conditions.sneaking())
```

---

#### `notSneaking()`

```java
public static Condition notSneaking()
```

Checks if the player is NOT sneaking.

---

#### `holdingAbilityItem()`

```java
public static Condition holdingAbilityItem()
```

Checks if the player is holding an ability item.

---

### Health Conditions

#### `healthAbove(double)`

```java
public static Condition healthAbove(double threshold)
```

Checks if the player's health is above a threshold (exclusive).

**Parameters**: `threshold` - Minimum health (exclusive)

**Example**:

```java
Conditions.healthAbove(5.0)  // Health > 5
```

---

#### `healthBelow(double)`

```java
public static Condition healthBelow(double threshold)
```

Checks if the player's health is below a threshold (exclusive).

**Parameters**: `threshold` - Maximum health (exclusive)

**Example**:

```java
Conditions.healthBelow(15.0)  // Health < 15
```

---

### Position Conditions

#### `yAbove(double)`

```java
public static Condition yAbove(double y)
```

Checks if the player's Y coordinate is above a threshold (exclusive).

**Parameters**: `y` - Minimum Y coordinate (exclusive)

---

#### `yBelow(double)`

```java
public static Condition yBelow(double y)
```

Checks if the player's Y coordinate is below a threshold (exclusive).

**Parameters**: `y` - Maximum Y coordinate (exclusive)

---

### Target Conditions

#### `hasTarget()`

```java
public static Condition hasTarget()
```

Checks if the context has a target entity.

**Usage**: With entity-click triggers

---

### Material Conditions

#### `holdingMaterial(Material)`

```java
public static Condition holdingMaterial(org.bukkit.Material material)
```

Checks if the player is holding a specific item type.

**Parameters**: `material` - The material to check

**Example**:

```java
Conditions.holdingMaterial(Material.DIAMOND_SWORD)
```

---

## Logic Operators

### `and(Condition...)`

```java
public static Condition and(Condition... conditions)
```

Combines multiple conditions with AND logic. All conditions must pass.

**Example**:

```java
Condition combined = Conditions.and(
    Conditions.sneaking(),
    Conditions.healthAbove(5.0),
    Conditions.yAbove(64.0)
);
```

---

### `or(Condition...)`

```java
public static Condition or(Condition... conditions)
```

Combines multiple conditions with OR logic. At least one condition must pass.

**Example**:

```java
Condition combined = Conditions.or(
    Conditions.healthBelow(5.0),
    Conditions.yBelow(0)
);
```

---

### `not(Condition)`

```java
public static Condition not(Condition condition)
```

Negates a condition.

**Example**:

```java
Condition notSneaking = Conditions.not(Conditions.sneaking());
```

---

## Advanced Usage

### Custom Conditions

```java
// World check
Condition inNether = context ->
    context.player().getWorld().getName().equals("world_nether");

// Time check
Condition isDay = context ->
    context.player().getWorld().getTime() < 12000;

// Permission check
Condition hasPermission = context ->
    context.player().hasPermission("ability.special");

// Combine
List<Condition> conditions = List.of(
    Conditions.sneaking(),
    inNether,
    isDay,
    hasPermission
);
```

### Reusable Conditions

```java
public class CustomConditions {
    public static Condition worldIs(String worldName) {
        return context ->
            context.player().getWorld().getName().equals(worldName);
    }
    
    public static Condition hasPermission(String permission) {
        return context ->
            context.player().hasPermission(permission);
    }
    
    public static Condition foodLevelAbove(int threshold) {
        return context ->
            context.player().getFoodLevel() > threshold;
    }
}
```

---

## YAML Usage

```yaml
conditions:
  - sneaking: true
  - health-above: 5.0
  - y-above: 64.0
```

---

## JavaScript Usage

```javascript
engine.ability({
  id: "example",
  conditions: [
    engine.condition.sneaking(),
    engine.condition.healthAbove(5.0),
    engine.condition.custom(function(ctx) {
      return ctx.player.getWorld().getName() === "world_nether";
    })
  ],
  execute: function(ctx) {
    // All conditions passed
  }
});
```

---

## Evaluation Order

Conditions are evaluated in order and use **short-circuit evaluation**:

1. First condition fails → Stop, don't execute ability
2. All conditions pass → Execute ability

**Performance tip**: Put fast/cheap conditions first:

```java
List.of(
    Conditions.sneaking(),              // Fast
    Conditions.healthAbove(5.0),        // Fast
    expensiveCustomCondition            // Slow, evaluated last
)
```

---

## See Also

- [Ability](ability.md) - Using conditions in abilities
- [AbilityContext](ability-context.md) - Context passed to conditions
- [YAML Schema](../yaml-schema.md) - YAML condition syntax
