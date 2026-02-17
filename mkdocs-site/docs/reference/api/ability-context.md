p# AbilityContext Record

Immutable context passed to abilities when they execute.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Record Declaration

```java
public record AbilityContext(
    Player player,
    TriggerType trigger,
    @Nullable Entity targetEntity,
    @Nullable Block targetBlock,
    @Nullable ItemStack item,
    @Nullable Event event
)
```

---

## Overview

`AbilityContext` is an immutable record that contains all relevant information about an ability trigger event. It's passed to the `Ability.execute()` method.

**Design**: Uses Java 16+ records for immutability and automatic getters.

---

## Components

### `player`

```java
Player player()
```

The player who triggered the ability.

**Type**: `org.bukkit.entity.Player`

**Always present**: Yes

---

### `trigger`

```java
TriggerType trigger()
```

The type of trigger that fired.

**Type**: [`TriggerType`](trigger-type.md)

**Always present**: Yes

---

### `targetEntity`

```java
@Nullable Entity targetEntity()
```

The target entity if applicable (e.g., entity clicks).

**Type**: `org.bukkit.entity.Entity`

**Nullable**: Yes

**When present**:

- `RIGHT_CLICK_ENTITY`
- `LEFT_CLICK_ENTITY`
- `SHIFT_RIGHT_CLICK_ENTITY`
- `SHIFT_LEFT_CLICK_ENTITY`
- `DAMAGE_DEALT`

---

### `targetBlock`

```java
@Nullable Block targetBlock()
```

The target block if applicable (e.g., block clicks).

**Type**: `org.bukkit.block.Block`

**Nullable**: Yes

**When present**: Block interaction triggers (if implemented)

---

### `item`

```java
@Nullable ItemStack item()
```

The item held when the ability was triggered.

**Type**: `org.bukkit.inventory.ItemStack`

**Nullable**: Yes

**When present**: When player is holding an ability item

---

### `event`

```java
@Nullable Event event()
```

The raw Bukkit event for advanced use.

**Type**: `org.bukkit.event.Event`

**Nullable**: Yes

**Usage**: Access underlying event data not exposed by other fields

---

## Static Factory Methods

### `of(Player, TriggerType)`

```java
public static AbilityContext of(Player player, TriggerType trigger)
```

Creates a basic context with just player and trigger.

**Parameters**:

- `player` - The player
- `trigger` - The trigger type

**Returns**: AbilityContext with null targets, item, and event

**Example**:

```java
var context = AbilityContext.of(player, TriggerType.RIGHT_CLICK);
```

---

## Usage Examples

### Basic Usage

```java
@Override
public void execute(AbilityContext context) {
    var player = context.player();
    var trigger = context.trigger();
    
    player.sendMessage("Triggered by: " + trigger);
}
```

### With Target Entity

```java
@Override
public void execute(AbilityContext context) {
    if (context.targetEntity() instanceof LivingEntity target) {
        target.damage(5.0, context.player());
    } else {
        context.player().sendMessage("§cNo valid target!");
    }
}
```

### Checking Item

```java
@Override
public void execute(AbilityContext context) {
    var item = context.item();
    if (item != null && item.getType() == Material.DIAMOND_SWORD) {
        // Bonus damage with diamond sword
        context.player().sendMessage("§bBonus activated!");
    }
}
```

### Accessing Raw Event

```java
@Override
public void execute(AbilityContext context) {
    if (context.event() instanceof PlayerInteractEvent event) {
        // Access event-specific data
        var clickedBlock = event.getClickedBlock();
        // ...
    }
}
```

---

## JavaScript Access

```javascript
engine.ability({
  id: "example",
  execute: function(ctx) {
    // Access fields directly
    var player = ctx.player;
    var trigger = ctx.trigger;
    var target = ctx.targetEntity;
    var block = ctx.targetBlock;
    var item = ctx.item;
    var event = ctx.event;
    
    // Use them
    if (target !== null) {
      player.sendMessage("Target: " + target.getName());
    }
  }
});
```

---

## Immutability

`AbilityContext` is immutable - all fields are final and cannot be changed after construction.

**Thread-safe**: Yes, can be safely shared across threads.

---

## See Also

- [Ability](ability.md) - Core ability interface
- [TriggerType](trigger-type.md) - Trigger types
- [Condition](conditions.md) - Using context in conditions
