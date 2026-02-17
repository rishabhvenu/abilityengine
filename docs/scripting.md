# JavaScript Scripting Guide

## Overview

AbilityEngine includes a powerful JavaScript scripting system built on GraalVM. Scripts can register abilities, listen to events, schedule tasks, and access the full Bukkit API with a high-level, declarative framework that abstracts common patterns.

## Script Location

Place scripts in:

```
plugins/AbilityEngine/scripts/
```

All files with `.js` extension will be automatically loaded on server start.

## The `engine` Global Object

Every script has access to a global `engine` object that provides the scripting API. This is the primary way to interact with AbilityEngine.

### Quick Example

```javascript
engine.ability({
  id: "dash",
  trigger: "DOUBLE_SHIFT",
  cooldown: {
    seconds: 5,
    showBossBar: true,
    bossBarColor: "BLUE"
  },
  execute: function(ctx) {
    var dir = ctx.player().getLocation().getDirection();
    ctx.player().setVelocity(dir.multiply(2.5));
    ctx.player().sendMessage("§bDashed!");
  }
});
```

## Core Concepts

### Triggers

AbilityEngine now supports a wide range of built-in triggers beyond simple clicks:

**Basic Click Triggers:**
- `RIGHT_CLICK`, `LEFT_CLICK`
- `SHIFT_RIGHT_CLICK`, `SHIFT_LEFT_CLICK`
- `RIGHT_CLICK_ENTITY`, `LEFT_CLICK_ENTITY`
- `SHIFT_RIGHT_CLICK_ENTITY`, `SHIFT_LEFT_CLICK_ENTITY`

**Movement Triggers:**
- `DOUBLE_SHIFT` - Double-tap sneak within 400ms
- `HOLD_SHIFT` - Holding the sneak key
- `JUMP` - Player jumps (was on ground, now airborne with upward velocity)
- `LAND` - Player lands (was airborne, now on ground)
- `MOVE` - Any player movement

**Combat Triggers:**
- `DAMAGE_DEALT` - When player damages an entity
- `DAMAGE_TAKEN` - When player takes damage
- `KILL_ENTITY` - When player kills an entity

**Projectile Triggers:**
- `PROJECTILE_HIT` - When player's projectile hits entity or block

**Lifecycle Triggers:**
- `ON_JOIN` - When player joins the server
- `ON_QUIT` - When player leaves the server
- `TICK` - Fires every tick for active sessions

**Custom Triggers:**
- `CUSTOM` - For use with `engine.listen()` to create custom event triggers

### The Context Object (`ctx`)

The `ctx` object passed to ability functions is now `AbilityExecContext` with enhanced features:

**Player & Event Access:**
- `ctx.player()` - The player (method call, not property!)
- `ctx.trigger()` - The trigger type
- `ctx.targetEntity()` - Target entity (nullable)
- `ctx.targetBlock()` - Target block (nullable)
- `ctx.item()` - The item used (nullable)
- `ctx.event()` - Raw Bukkit event (nullable)

**Ability-Scoped State:**
- `ctx.state.get(key)` - Get state value for this ability + player
- `ctx.state.set(key, value)` - Set state value
- `ctx.state.clear()` - Clear all state for this ability + player

**Ability-Scoped Scheduling:**
- `ctx.scheduleRepeating(func, delayTicks, periodTicks)` - Returns task ID
- `ctx.scheduleDelayed(func, delayTicks)` - Returns task ID
- `ctx.cancelTask(taskId)` - Cancel a task

**Important:** Tasks scheduled via `ctx` are automatically tracked and cancelled when the ability is unloaded or the player quits. This prevents memory leaks.

### Cooldowns

Cooldowns can be specified as a simple number or a rich object:

**Simple form:**
```javascript
cooldown: 5  // 5 seconds
```

**Extended form (auto-triggers boss bar):**
```javascript
cooldown: {
  seconds: 8,
  showBossBar: true,
  bossBarColor: "GREEN",  // BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
  bossBarLabel: "Recoil"  // Optional, defaults to ability ID
}
```

## API Reference

### `engine.ability(config)`

Registers an ability with a declarative configuration.

**Config Object:**
- `id` (string, required) - Unique ability identifier
- `trigger` or `triggers` (string or array, optional) - Trigger type(s) (default: `"RIGHT_CLICK"`)
- `conditions` (array, optional) - Conditions that must pass
- `cooldown` (number or object, optional) - Cooldown config
- `permission` (string, optional) - Permission node required to use ability
- `execute` or `onTrigger` (function, required) - Main execution function
- `onProjectileHit` (function, optional) - Called when ability's projectile hits
- `onProjectileTick` (function, optional) - Called every tick for projectiles spawned by this ability
- `onExpire` (function, optional) - Called when session expires
- `onCancel` (function, optional) - Called when session is cancelled externally

**Example:**
```javascript
engine.ability({
  id: "poison_arrow",
  trigger: "LEFT_CLICK",
  permission: "ability.poisonbow",
  cooldown: {
    seconds: 3,
    showBossBar: true,
    bossBarColor: "GREEN"
  },
  onTrigger: function(ctx) {
    var arrow = engine.projectile.spawn({
      type: "ARROW",
      shooter: ctx.player(),
      speed: 2.5,
      damage: 8,
      critical: true,
      potion: {type: "POISON", duration: 100, amplifier: 1},
      trail: {particle: "DUST", color: "#00AA00", count: 3}
    });
  }
});
```

### `engine.item(config)`

Creates and registers a named item template with auto-wired abilities.

**Config Object:**
- `id` (string, required) - Unique item template ID
- `type` (string, required) - Material type (e.g., `"BOW"`, `"DIAMOND_SWORD"`)
- `name` (string, optional) - Display name with `&` color codes
- `lore` (array of strings, optional) - Lore lines with `&` color codes
- `abilities` (array of ability IDs, required) - Abilities to attach (triggers auto-detected from registry)
- `unbreakable` (boolean, optional) - Make item unbreakable
- `enchantments` (object, optional) - Enchantments with levels

**Example:**
```javascript
// First register abilities
engine.ability({
  id: "poison_cloud",
  trigger: "DOUBLE_SHIFT",
  execute: function(ctx) { /* ... */ }
});

engine.ability({
  id: "poison_arrow",
  trigger: "LEFT_CLICK",
  execute: function(ctx) { /* ... */ }
});

// Then create item that uses them
engine.item({
  id: "legendary_poison_bow",
  type: "BOW",
  name: "&5&lLegendary Poison Bow",
  lore: [
    "&aPoison Cloud &7- Double Shift",
    "&aPoisonous Arrow &7- Left Click"
  ],
  abilities: ["poison_cloud", "poison_arrow"],
  unbreakable: true,
  enchantments: {
    "ARROW_INFINITE": 1
  }
});

// Give to player
engine.items.give(player, "legendary_poison_bow");
```

### `engine.trigger` Constants

Access trigger type constants:

```javascript
// Clicks
engine.trigger.RIGHT_CLICK
engine.trigger.LEFT_CLICK
engine.trigger.SHIFT_RIGHT_CLICK
engine.trigger.SHIFT_LEFT_CLICK
engine.trigger.RIGHT_CLICK_ENTITY
engine.trigger.LEFT_CLICK_ENTITY

// Movement
engine.trigger.DOUBLE_SHIFT
engine.trigger.HOLD_SHIFT
engine.trigger.JUMP
engine.trigger.LAND
engine.trigger.MOVE

// Combat
engine.trigger.DAMAGE_DEALT
engine.trigger.DAMAGE_TAKEN
engine.trigger.KILL_ENTITY

// Projectile
engine.trigger.PROJECTILE_HIT

// Lifecycle
engine.trigger.ON_JOIN
engine.trigger.ON_QUIT
engine.trigger.TICK

// Custom
engine.trigger.CUSTOM
```

### `engine.condition` Builders

**Built-in Conditions:**

```javascript
engine.condition.sneaking()
engine.condition.notSneaking()
engine.condition.healthAbove(threshold)
engine.condition.healthBelow(threshold)
engine.condition.yAbove(y)
engine.condition.yBelow(y)
engine.condition.hasTarget()
engine.condition.custom(function(ctx) { return true; })
```

### `engine.state` - Global State Management

For state not tied to a specific ability execution:

```javascript
engine.state.set(player, abilityId, key, value)
engine.state.get(player, abilityId, key)
engine.state.clear(player, abilityId)
```

**Note:** Prefer `ctx.state` inside ability functions for automatic scoping.

### `engine.ui` - UI Utilities

#### `engine.ui.cooldownBar(player, abilityId, durationSeconds, label, color)`

Shows a cooldown progress bar.

**Parameters:**
- `player` (Player)
- `abilityId` (String) - Unique ID for this bar
- `durationSeconds` (int) - Duration
- `label` (String, optional) - Display text (defaults to ability ID)
- `color` (String, optional) - Bar color (defaults to "GREEN")

#### `engine.ui.removeBar(player, abilityId)`

Removes a boss bar.

### `engine.effects` - Effect Library

High-level effect utilities that abstract away GraalVM type issues:

#### `engine.effects.particle(location, particleType, colorHex, count, spreadX, spreadY, spreadZ)`

Spawn particles. For colored particles (`ENTITY_EFFECT`), pass a hex color string. Pass `null` for non-colored types.

```javascript
engine.effects.particle(
  ctx.player().getLocation(),
  "ENTITY_EFFECT",
  "#FF0000",
  50,
  0.5, 0.5, 0.5
);
```

#### `engine.effects.sound(location, sound, volume, pitch)`

Play sound effect.

```javascript
engine.effects.sound(
  ctx.player().getLocation(),
  "ENTITY_ARROW_SHOOT",
  1.0,
  1.2
);
```

#### `engine.effects.potion(target, effectType, durationTicks, amplifier)`

Apply potion effect.

```javascript
engine.effects.potion(
  ctx.targetEntity(),
  "POISON",
  100,
  1
);
```

#### `engine.effects.knockback(entity, direction, strength)`

Apply knockback.

```javascript
var dir = ctx.player().getLocation().getDirection();
engine.effects.knockback(ctx.targetEntity(), dir, 2.0);
```

#### `engine.effects.explosion(location, power, setFire, breakBlocks)`

Create explosion.

```javascript
engine.effects.explosion(
  ctx.targetBlock().getLocation(),
  3.0,
  false,
  false
);
```

#### `engine.effects.decayTerrain(center, radius, rules)`

Decay terrain safely.

**Rules:**
- `"NATURE_ONLY"` - Grass/mycelium/podzol → dirt; leaves break naturally; tall grass, ferns, dead bush → air
- `"STONE_DECAY"` - Stone → cobblestone; deepslate → cobbled deepslate
- `"ICE_MELT"` - Ice types → water; snow → air

```javascript
engine.effects.decayTerrain(
  ctx.player().getLocation(),
  5,
  "NATURE_ONLY"
);
```

#### `engine.effects.decayTerrainCustom(center, radius, ruleFunction)`

Decay terrain using a custom JavaScript rule function. Return a material name to replace the block, or nothing to skip it.

```javascript
engine.effects.decayTerrainCustom(
  ctx.player().getLocation(), 3,
  function(block) {
    if (block.getType().name() === "SAND") return "GLASS";
  }
);
```

### `engine.projectile` - Projectile Utilities

#### `engine.projectile.spawn(config)`

Spawns a projectile with advanced features.

**Config Object:**
- `type` (string, required) - Projectile type (e.g., `"ARROW"`, `"FIREBALL"`)
- `shooter` (LivingEntity, required) - Who shot it
- `speed` (number, optional) - Launch speed
- `damage` (number, optional) - Damage on hit
- `critical` (boolean, optional) - Critical hit (arrows only)
- `potion` (object, optional) - `{type, duration, amplifier}` - Potion effect on hit
- `trail` (object, optional) - `{particle, color, count, spread}` - Particle trail
- `onTick` (function, optional) - Called every tick: `function(projectile, tickCount)`
- `onHit` (function, optional) - Called on hit: `function(projectile, hitResult)`
- `maxTicks` (number, optional) - Auto-remove after this many ticks

**Example:**
```javascript
engine.projectile.spawn({
  type: "ARROW",
  shooter: ctx.player(),
  speed: 2.5,
  damage: 8,
  critical: true,
  potion: {type: "POISON", duration: 100, amplifier: 1},
  trail: {
    particle: "DUST",
    color: "#00AA00",
    count: 3,
    spread: 0.1
  },
  onHit: function(projectile, hit) {
    if (hit.hitEntity) {
      engine.effects.decayTerrain(hit.hitEntity.getLocation(), 2, "NATURE_ONLY");
    }
  },
  maxTicks: 200
});
```

### `engine.areaEffect` - Area Effect Cloud Utilities

#### `engine.areaEffect.spawn(config)`

Spawns an area effect cloud with auto-exclusion.

**Config Object:**
- `location` (Location, required)
- `source` (LivingEntity, optional) - Source entity
- `radius` (number, optional) - Cloud radius
- `duration` (number, optional) - Duration in ticks
- `color` (string, optional) - Hex color (e.g., `"#00AA00"`)
- `potion` (object, optional) - `{type, duration, amplifier}`
- `excludeCaster` (boolean, optional) - Don't affect the caster
- `radiusShrink` (number, optional) - Radius shrink per tick

**Example:**
```javascript
engine.areaEffect.spawn({
  location: ctx.player().getLocation(),
  source: ctx.player(),
  radius: 4,
  duration: 200,
  color: "#00AA00",
  potion: {type: "POISON", duration: 60, amplifier: 0},
  excludeCaster: true,
  radiusShrink: 0.005
});
```

### `engine.items` - Item Management

#### `engine.items.create(abilityIdOrConfig)`

Creates an ability item. Accepts either a string (ability ID) or a config object.

**Config Object (extended form):**
- `type` (string, required) - Material
- `name` (string, optional) - Display name with `&` codes
- `lore` (array, optional) - Lore lines with `&` codes
- `abilityId` or `abilities` (string/array, optional) - Ability attachment
- `unbreakable` (boolean, optional)
- `enchantments` (object, optional)

**Example:**
```javascript
// Simple form
var item = engine.items.create("dash");

// Extended form
var item = engine.items.create({
  type: "DIAMOND_SWORD",
  name: "&cFire Blade",
  lore: ["&7Ignites enemies"],
  abilityId: "fire_strike",
  unbreakable: true,
  enchantments: {"FIRE_ASPECT": 2}
});
```

#### `engine.items.give(player, itemIdOrAbilityId)`

Gives an item to a player. Supports both item template IDs (from `engine.item()`) and ability IDs.

```javascript
engine.items.give(ctx.player(), "legendary_poison_bow");
```

#### `engine.items.isAbilityItem(item)`

Returns `true` if the item is an ability item.

#### `engine.items.getAbilityId(item)`

Returns the ability ID for an item (or `null`).

### `engine.sessions` - Session Management

Manage stateful abilities that run over time.

#### `engine.sessions.start(player, ability, handlers)`

Starts a new session with lifecycle handlers.

#### `engine.sessions.end(player, abilityId)`

Ends all sessions for a player running a specific ability.

#### `engine.sessions.getActive(player)`

Returns an array of ability IDs for active sessions.

### `engine.cooldowns` - Cooldown Management

#### `engine.cooldowns.isReady(player, abilityId)`

Returns `true` if the ability is ready (not on cooldown).

#### `engine.cooldowns.set(player, abilityId, seconds)`

Sets a cooldown manually.

#### `engine.cooldowns.remaining(player, abilityId)`

Returns remaining cooldown time in seconds.

### `engine.listen(eventClass, handler)`

Registers a listener for any Bukkit event.

**Example:**
```javascript
engine.listen("PlayerJoinEvent", function(event) {
  event.getPlayer().sendMessage("§aWelcome!");
});
```

### Scheduling

#### `engine.scheduleDelayed(function, delayTicks)`

Schedules a one-time delayed task (global, not ability-scoped).

#### `engine.scheduleRepeating(function, delayTicks, periodTicks)`

Schedules a repeating task (global, not ability-scoped).

#### `engine.cancelTask(taskId)`

Cancels a scheduled task.

**Note:** Prefer `ctx.scheduleDelayed()` and `ctx.scheduleRepeating()` inside abilities for automatic cleanup.

### Logging

```javascript
engine.log("Info message")
engine.warn("Warning message")
engine.error("Error message")
```

## Example: What the Poison Bow Becomes

**Before (364 lines of boilerplate):**

The original `legendary-poison-bow.js` manually implemented:
- Double-tap detection logic
- Boss bar recoil UI
- Metadata tagging
- Cooldown boss bar syncing
- Arrow trail scheduling
- Block decay logic
- Cloud self-immunity filtering
- Cleanup on quit
- PDC wiring
- Float casting hacks

**After (with new APIs):**

```javascript
// Poison Cloud Ability
engine.ability({
  id: "poison_cloud",
  trigger: "DOUBLE_SHIFT",
  cooldown: {
    seconds: 8,
    showBossBar: true,
    bossBarColor: "GREEN",
    bossBarLabel: "Recoil"
  },
  onTrigger: function(ctx) {
    engine.areaEffect.spawn({
      location: ctx.player().getLocation(),
      source: ctx.player(),
      radius: 4,
      duration: 200,
      color: "#00AA00",
      potion: {type: "POISON", duration: 60, amplifier: 0},
      excludeCaster: true,
      radiusShrink: 0.005
    });
  }
});

// Poisonous Arrow Ability
engine.ability({
  id: "poison_arrow",
  trigger: "LEFT_CLICK",
  cooldown: 3,
  onTrigger: function(ctx) {
    engine.projectile.spawn({
      type: "ARROW",
      shooter: ctx.player(),
      speed: 2.5,
      damage: 8,
      critical: true,
      potion: {type: "POISON", duration: 100, amplifier: 1},
      trail: {particle: "DUST", color: "#00AA00", count: 3},
      onHit: function(projectile, hit) {
        if (hit.hitBlock) {
          engine.effects.decayTerrain(hit.hitBlock.getLocation(), 2, "NATURE_ONLY");
        }
      }
    });
  }
});

// Create the Item
engine.item({
  id: "legendary_poison_bow",
  type: "BOW",
  name: "&5&lLegendary Poison Bow",
  lore: [
    "&aPoison Cloud &7- Double Shift",
    "&aPoisonous Arrow &7- Left Click"
  ],
  abilities: ["poison_cloud", "poison_arrow"],
  unbreakable: true,
  enchantments: {"ARROW_INFINITE": 1}
});
```

**~50 lines vs 364. Clean, declarative, safe.**

## Raw Java Interop (Power Mode)

For advanced use cases, scripts have full access to Java classes via `Java.type()`:

```javascript
const Bukkit = Java.type("org.bukkit.Bukkit");
const Location = Java.type("org.bukkit.Location");

Bukkit.broadcastMessage("Hello from script!");
```

**Note:** Scripts are trusted and have unrestricted access. There is no sandboxing.

## Hot Reload

Scripts can be reloaded without restarting the server.

**Commands:**
- `/ability reload` - Reloads config abilities AND all scripts
- `/ability script reload` - Reloads all scripts
- `/ability script reload <filename>` - Reloads a specific script
- `/ability script list` - Lists loaded scripts

**What Happens on Reload:**
1. All abilities registered by the script are unregistered
2. All event listeners are unregistered
3. All scheduled tasks are cancelled
4. All boss bars are removed
5. All ability state is cleared
6. The GraalVM context is closed
7. The script is re-executed

## Memory Safety

The engine automatically cleans up resources:

- **On script unload/reload:** All abilities, listeners, tasks, boss bars, and state are removed
- **On plugin disable:** All scripts are unloaded cleanly
- **On player quit:** Sessions, cooldowns, boss bars, and ability state for that player are cleared
- **Ability-scoped tasks:** Tasks scheduled via `ctx.scheduleRepeating()` are automatically cancelled when the ability is unloaded

Scripts following the engine API cannot cause memory leaks.

## Best Practices

1. **Use the high-level APIs** - `engine.effects`, `engine.projectile`, `engine.areaEffect` abstract away complexity
2. **Use `ctx.state` for ability state** - Automatically scoped and cleaned
3. **Use `ctx.scheduleRepeating()` for ability tasks** - Automatically tracked and cancelled
4. **Use `engine.item()` for multi-ability items** - Triggers are auto-wired from registry
5. **Use extended cooldown config** - Automatic boss bar UI for cooldowns
6. **Handle errors** - Wrap risky code in try/catch
7. **Test with `/ability script reload`** - Fast iteration
8. **Log important events** - Use `engine.log()` for debugging
9. **Avoid blocking operations** - No `Thread.sleep()`, no blocking I/O

## Troubleshooting

**"Unknown ability: X"**
- Check that your ability ID matches exactly
- Reload scripts: `/ability script reload`

**"Script failed to load"**
- Check server logs for JavaScript syntax errors
- Verify your script is in `plugins/AbilityEngine/scripts/`
- Make sure it has `.js` extension

**Ability not firing**
- Check conditions are passing
- Check cooldown: `/ability info <ability_id>`
- Verify trigger type matches your action
- Check logs: `engine.log("Debug message")`

**Boss bar not showing**
- Ensure `showBossBar: true` in cooldown config
- Check that cooldown duration > 0
- Verify player has permissions if `permission` is set
