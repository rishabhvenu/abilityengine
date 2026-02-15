# AbilityItemService Interface

Interface for creating and managing ability-backed items.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Interface Declaration

```java
public interface AbilityItemService
```

---

## Overview

`AbilityItemService` handles the creation and validation of items bound to abilities using Persistent Data Container (PDC).

**Implementation**: `AbilityItemServiceImpl` (internal)

---

## Methods

### `createAbilityItem(String)`

```java
@Nullable
ItemStack createAbilityItem(String abilityId)
```

Creates an item bound to a specific ability.

**Parameters**: `abilityId` - The ability ID

**Returns**: The ability item, or `null` if the ability doesn't exist

**Example**:

```java
AbilityItemService items = getItemService();
ItemStack fireballItem = items.createAbilityItem("fireball");
if (fireballItem != null) {
    player.getInventory().addItem(fireballItem);
}
```

---

### `isAbilityItem(ItemStack)`

```java
boolean isAbilityItem(@Nullable ItemStack item)
```

Checks if an item is an ability item.

**Parameters**: `item` - The item to check

**Returns**: `true` if the item is bound to any ability

**Example**:

```java
if (items.isAbilityItem(heldItem)) {
    player.sendMessage("This is an ability item!");
}
```

---

### `getAbilityId(ItemStack)`

```java
@Nullable
String getAbilityId(@Nullable ItemStack item)
```

Gets the primary ability ID from an item.

**Parameters**: `item` - The item

**Returns**: The ability ID, or `null` if not an ability item

**Example**:

```java
String abilityId = items.getAbilityId(heldItem);
if (abilityId != null) {
    player.sendMessage("Ability: " + abilityId);
}
```

---

### `getAbilities(ItemStack)`

```java
List<String> getAbilities(@Nullable ItemStack item)
```

Gets all abilities bound to an item (includes primary and additional abilities).

**Parameters**: `item` - The item

**Returns**: List of ability IDs (empty if not an ability item)

**Example**:

```java
List<String> abilities = items.getAbilities(heldItem);
for (String abilityId : abilities) {
    System.out.println("Item has ability: " + abilityId);
}
```

---

### `isAbilityItem(ItemStack, String)`

```java
boolean isAbilityItem(@Nullable ItemStack item, String abilityId)
```

Checks if an item is bound to a specific ability.

**Parameters**:

- `item` - The item
- `abilityId` - The ability ID to check

**Returns**: `true` if the item contains this ability

**Example**:

```java
if (items.isAbilityItem(heldItem, "fireball")) {
    player.sendMessage("This is a fireball item!");
}
```

---

### `getAbilityTrigger(ItemStack, String)`

```java
@Nullable
TriggerType getAbilityTrigger(@Nullable ItemStack item, String abilityId)
```

Gets the trigger type for a specific ability on an item. For multi-ability items, this returns which trigger activates this ability.

**Parameters**:

- `item` - The item
- `abilityId` - The ability ID

**Returns**: The trigger type, or `null` if not found

**Example**:

```java
TriggerType trigger = items.getAbilityTrigger(heldItem, "fireball");
if (trigger != null) {
    System.out.println("Fireball triggers on: " + trigger);
}
```

---

## Usage Examples

### Basic Item Creation

```java
AbilityItemService items = getItemService();

// Create item
ItemStack item = items.createAbilityItem("fireball");
if (item != null) {
    player.getInventory().addItem(item);
    player.sendMessage("§aReceived fireball ability!");
}
```

### Validation

```java
@EventHandler
public void onInteract(PlayerInteractEvent event) {
    ItemStack item = event.getItem();
    
    if (!items.isAbilityItem(item)) {
        return; // Not an ability item
    }
    
    String abilityId = items.getAbilityId(item);
    if (abilityId != null) {
        // Handle ability trigger
        handleAbilityTrigger(event.getPlayer(), abilityId);
    }
}
```

### Multi-Ability Items

```java
// Check all abilities on an item
List<String> abilities = items.getAbilities(heldItem);
player.sendMessage("§eThis item has " + abilities.size() + " abilities:");
for (String abilityId : abilities) {
    TriggerType trigger = items.getAbilityTrigger(heldItem, abilityId);
    player.sendMessage("§7- " + abilityId + " (" + trigger + ")");
}
```

---

## JavaScript Access

```javascript
// Create item
var item = engine.items.create("fireball");
if (item !== null) {
  ctx.player.getInventory().addItem(item);
}

// Check if ability item
if (engine.items.isAbilityItem(ctx.item)) {
  var abilityId = engine.items.getAbilityId(ctx.item);
  ctx.player.sendMessage("Ability: " + abilityId);
}
```

---

## Item Data Format

Items store data using Persistent Data Container with these keys:

### Legacy Format (Single Ability)

```
ability_engine:ability_id = "fireball"
```

### Modern Format (Multi-Ability)

```
ability_engine:abilities = JSON array [
  {"id": "fireball", "trigger": "RIGHT_CLICK"},
  {"id": "shield", "trigger": "SHIFT_RIGHT_CLICK"}
]
ability_engine:item_version = "1"
```

---

## Implementation Details

### PDC Keys

- Namespace: `ability_engine`
- Keys: `ability_id`, `abilities`, `item_version`

### Item Generation

Default item properties:

- Material: STICK
- Display name: From ability configuration
- Lore: Optional usage instructions
- Unbreakable: False (configurable)

---

## See Also

- [Items Guide](../../guides/items.md) - Complete items guide
- [Ability](ability.md) - Ability interface
- [Commands Reference](../commands.md) - `/ability give` command
