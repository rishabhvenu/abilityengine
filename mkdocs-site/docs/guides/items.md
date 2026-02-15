# Ability Items Guide

Learn how to create, manage, and use ability items.

---

## Overview

Ability items are Minecraft items that trigger abilities when used. They're created using Persistent Data Container (PDC) tags to bind abilities to items.

**Key Features**:

- **Multi-ability items** - Multiple abilities on one item with different triggers
- **PDC-based** - No NBT hacking, uses native Bukkit API
- **Persistent** - Survives server restarts and inventories
- **Flexible** - Works with any item type

---

## Creating Ability Items

### Via Commands

The easiest way to create ability items:

```
/ability give <player> <ability_id>
```

**Examples**:

```
/ability give Steve fireball
/ability give @a dash
/ability give @p healing_touch
```

### Via JavaScript

```javascript
var item = engine.items.create("fireball");
if (item !== null) {
  ctx.player.getInventory().addItem(item);
}
```

### Via Java API

```java
AbilityItemService itemService = getItemService();
ItemStack item = itemService.createAbilityItem("fireball");
if (item != null) {
    player.getInventory().addItem(item);
}
```

---

## Item Structure

### Single-Ability Items

Basic item with one ability:

```yaml
abilities:
  fireball:
    display-name: "&cFireball"
    triggers:
      - RIGHT_CLICK
    # ... rest of config
```

When created via `/ability give player fireball`, the item:

- Uses STICK as the base item (default)
- Has display name from config: "§cFireball"
- Contains PDC data linking to the ability
- Triggers on RIGHT_CLICK

### Multi-Ability Items

Items can have multiple abilities with different triggers. This is configured in the ability item PDC data, not in YAML.

**Example** (conceptual - would need custom item creation):

An item that has:

- `fireball` on RIGHT_CLICK
- `shield` on SHIFT_RIGHT_CLICK
- `dash` on LEFT_CLICK

---

## Working with Items

### Checking if Item is an Ability Item

**JavaScript**:

```javascript
if (engine.items.isAbilityItem(ctx.item)) {
  ctx.player.sendMessage("This is an ability item!");
}
```

**Java**:

```java
if (itemService.isAbilityItem(itemStack)) {
    player.sendMessage("This is an ability item!");
}
```

### Getting Ability ID

**JavaScript**:

```javascript
var abilityId = engine.items.getAbilityId(ctx.item);
if (abilityId !== null) {
  ctx.player.sendMessage("Ability: " + abilityId);
}
```

**Java**:

```java
String abilityId = itemService.getAbilityId(itemStack);
if (abilityId != null) {
    player.sendMessage("Ability: " + abilityId);
}
```

### Getting All Abilities on Item

For multi-ability items:

**Java**:

```java
List<String> abilities = itemService.getAbilities(itemStack);
for (String abilityId : abilities) {
    System.out.println("Item has ability: " + abilityId);
}
```

---

## Item Data Format

Items use Persistent Data Container (PDC) with namespaced keys:

### Legacy Format (Single Ability)

```
ability_engine:ability_id = "fireball"
```

### Modern Format (Multi-Ability)

```
ability_engine:abilities = [
  {"id": "fireball", "trigger": "RIGHT_CLICK"},
  {"id": "shield", "trigger": "SHIFT_RIGHT_CLICK"}
]
ability_engine:item_version = "1"
```

---

## Custom Item Creation

### Creating Custom Items (Java)

```java
public ItemStack createCustomAbilityItem(String abilityId, Material material, String displayName) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    
    // Set display name
    meta.displayName(Component.text(displayName)
        .decoration(TextDecoration.ITALIC, false));
    
    // Add lore
    meta.lore(List.of(
        Component.text("Right-click to use")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
    ));
    
    // Add PDC data
    var container = meta.getPersistentDataContainer();
    var key = new NamespacedKey("ability_engine", "ability_id");
    container.set(key, PersistentDataType.STRING, abilityId);
    
    item.setItemMeta(meta);
    return item;
}
```

### Creating Custom Items (JavaScript)

```javascript
function createCustomItem(abilityId, materialName, displayName) {
  const ItemStack = Java.type("org.bukkit.ItemStack");
  const Material = Java.type("org.bukkit.Material");
  const Component = Java.type("net.kyori.adventure.text.Component");
  const NamedTextColor = Java.type("net.kyori.adventure.text.format.NamedTextColor");
  const NamespacedKey = Java.type("org.bukkit.NamespacedKey");
  const PersistentDataType = Java.type("org.bukkit.persistence.PersistentDataType");
  
  var material = Material[materialName];
  var item = new ItemStack(material);
  var meta = item.getItemMeta();
  
  // Set display name
  meta.displayName(Component.text(displayName));
  
  // Add PDC data
  var container = meta.getPersistentDataContainer();
  var key = new NamespacedKey("ability_engine", "ability_id");
  container.set(key, PersistentDataType.STRING, abilityId);
  
  item.setItemMeta(meta);
  return item;
}

// Usage
var customItem = createCustomItem("fireball", "BLAZE_ROD", "§cCustom Fireball");
ctx.player.getInventory().addItem(customItem);
```

---

## Item Triggers

How triggers work with items:

### Standard Item Use

When a player uses an ability item:

1. Player right-clicks (or other trigger action)
2. Engine checks held item for PDC tags
3. Engine finds abilities matching the trigger
4. Engine evaluates conditions
5. Engine checks cooldowns
6. Engine executes abilities

### Multi-Ability Resolution

For items with multiple abilities:

```json
[
  {"id": "fireball", "trigger": "RIGHT_CLICK"},
  {"id": "shield", "trigger": "SHIFT_RIGHT_CLICK"},
  {"id": "dash", "trigger": "LEFT_CLICK"}
]
```

- RIGHT_CLICK → only `fireball` executes
- SHIFT_RIGHT_CLICK → only `shield` executes
- LEFT_CLICK → only `dash` executes

---

## Item Persistence

### Across Restarts

Ability items persist through:

- Server restarts
- Item drops/pickups
- Chest storage
- Player inventories
- Trading/giving

The PDC data is saved with the item data.

### Item Duplication

!!! warning "Items Can Be Duplicated"
    Ability items are normal items and can be duplicated through:
    
    - `/give` commands
    - Creative mode
    - Item duplication glitches
    
    Consider adding additional validation if this is a concern.

---

## Best Practices

### Item Type Selection

Choose appropriate item types:

```yaml
# Good choices:
- STICK          # Default, generic
- BLAZE_ROD      # Magic-looking
- NETHER_STAR    # Rare, valuable-looking
- DIAMOND        # Premium abilities
- BOOK           # Scroll/spell abilities

# Avoid:
- DIAMOND_SWORD  # Has its own use
- FOOD           # Gets consumed
- BLOCKS         # Can be placed
```

### Display Names

Use clear, descriptive names:

```yaml
display-name: "&c&lFireball &r&7(Right Click)"
```

**Good Patterns**:

- `"&cFireball"` - Color + name
- `"&c&lUltimate Fireball"` - Color + bold + name
- `"&cFireball &7(3s cooldown)"` - Name + info
- `"&c⚡ Fireball"` - Unicode symbols

### Lore Text

Add usage instructions:

```java
meta.lore(List.of(
    Component.text(""),
    Component.text("Right-click to launch fireball")
        .color(NamedTextColor.GRAY),
    Component.text("3 second cooldown")
        .color(NamedTextColor.DARK_GRAY),
    Component.text("")
));
```

---

## Advanced Patterns

### Custom Item Giver

Create a system to give custom items:

```javascript
// Item definitions
var customItems = {
  "starter_pack": [
    {ability: "fireball", material: "BLAZE_ROD", name: "§cStarter Fireball"},
    {ability: "heal", material: "GOLDEN_APPLE", name: "§aHealing Touch"},
    {ability: "dash", material: "FEATHER", name: "§bQuick Dash"}
  ]
};

// Command to give pack
engine.listen("PlayerJoinEvent", function(event) {
  var player = event.getPlayer();
  
  if (!player.hasPlayedBefore()) {
    customItems.starter_pack.forEach(function(itemDef) {
      var item = createCustomItem(
        itemDef.ability,
        itemDef.material,
        itemDef.name
      );
      player.getInventory().addItem(item);
    });
    
    player.sendMessage("§aReceived starter ability pack!");
  }
});
```

### Item Validation

Prevent invalid ability items:

```java
public boolean isValidAbilityItem(ItemStack item, AbilityRegistry registry) {
    String abilityId = itemService.getAbilityId(item);
    if (abilityId == null) {
        return false;
    }
    
    // Check if ability exists
    return registry.isRegistered(abilityId);
}
```

### Item Upgrading

System to upgrade ability items:

```javascript
engine.ability({
  id: "upgrade_item",
  triggers: ["SHIFT_RIGHT_CLICK"],
  execute: function(ctx) {
    var item = ctx.item;
    if (!engine.items.isAbilityItem(item)) {
      ctx.player.sendMessage("§cNot an ability item!");
      return;
    }
    
    var abilityId = engine.items.getAbilityId(item);
    var upgradedId = abilityId + "_upgraded";
    
    // Check if upgraded version exists
    if (isAbilityRegistered(upgradedId)) {
      var newItem = engine.items.create(upgradedId);
      ctx.player.getInventory().setItemInMainHand(newItem);
      ctx.player.sendMessage("§aItem upgraded!");
    } else {
      ctx.player.sendMessage("§cNo upgrade available!");
    }
  }
});
```

---

## Troubleshooting

### Item Not Triggering Ability

**Check**:

1. Item has PDC data (use `/data get entity @s SelectedItem`)
2. Ability is registered (`/ability list`)
3. Trigger type matches action
4. Conditions are met
5. Not on cooldown

### Lost PDC Data

**Causes**:

- Item was modified by another plugin
- Item was created before plugin was installed
- Item was duplicated incorrectly

**Solution**: Re-create the item with `/ability give`

### Wrong Ability Triggers

**Problem**: Right-click triggers wrong ability

**Solution**: Check multi-ability item configuration. Each trigger should map to exactly one ability.

---

## Examples

### Complete Custom Item System

```javascript
// Custom item creator
function createMagicWand(abilityId, power) {
  const ItemStack = Java.type("org.bukkit.ItemStack");
  const Material = Java.type("org.bukkit.Material");
  const Component = Java.type("net.kyori.adventure.text.Component");
  const NamespacedKey = Java.type("org.bukkit.NamespacedKey");
  const PersistentDataType = Java.type("org.bukkit.persistence.PersistentDataType");
  
  var item = new ItemStack(Material.BLAZE_ROD);
  var meta = item.getItemMeta();
  
  // Name based on power
  var colors = ["§7", "§a", "§b", "§d", "§6"];
  var tierNames = ["Common", "Uncommon", "Rare", "Epic", "Legendary"];
  var color = colors[power - 1] || "§7";
  var tier = tierNames[power - 1] || "Common";
  
  meta.displayName(Component.text(color + "Magic Wand (" + tier + ")"));
  
  // Lore
  var lore = [
    Component.text(""),
    Component.text("§7Power: " + power),
    Component.text("§7Ability: §f" + abilityId),
    Component.text(""),
    Component.text("§eRight-click to cast")
  ];
  meta.lore(lore);
  
  // PDC
  var container = meta.getPersistentDataContainer();
  var abilityKey = new NamespacedKey("ability_engine", "ability_id");
  var powerKey = new NamespacedKey("custom", "power");
  
  container.set(abilityKey, PersistentDataType.STRING, abilityId);
  container.set(powerKey, PersistentDataType.INTEGER, power);
  
  item.setItemMeta(meta);
  return item;
}

// Give player a wand
engine.ability({
  id: "summon_wand",
  triggers: ["RIGHT_CLICK"],
  execute: function(ctx) {
    var power = Math.floor(Math.random() * 5) + 1;
    var wand = createMagicWand("fireball", power);
    ctx.player.getInventory().addItem(wand);
    ctx.player.sendMessage("§aReceived magic wand!");
  }
});
```

---

## Next Steps

- [Scripting Guide](scripting.md) - Create abilities with JavaScript
- [Module Development](module-development.md) - Build Java modules
- [API Reference](../reference/api/ability-item-service.md) - AbilityItemService API
