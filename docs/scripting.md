# JavaScript Scripting Guide

## Overview

AbilityEngine Phase 2 includes a powerful JavaScript scripting system built on GraalVM. Scripts can register abilities, listen to events, schedule tasks, and access the full Bukkit API.

## Script Location

Place scripts in:

```
plugins/AbilityEngine/scripts/
```

All files with `.js` extension will be automatically loaded on server start.

## The `engine` Global Object

Every script has access to a global `engine` object that provides the scripting API. This is the primary (and recommended) way to interact with AbilityEngine.

### Quick Example

```javascript
engine.ability({
  id: "dash",
  triggers: ["SHIFT_RIGHT_CLICK"],
  cooldown: 5,
  execute: function(ctx) {
    var dir = ctx.player.getLocation().getDirection();
    ctx.player.setVelocity(dir.multiply(2.5));
    ctx.player.sendMessage("§bDashed!");
  }
});
```

## API Reference

### `engine.ability(config)`

Registers an ability from a JavaScript object.

**Config Object:**
- `id` (string, required) - Unique ability identifier
- `triggers` (array of strings, optional) - Trigger types (default: `["RIGHT_CLICK"]`)
- `conditions` (array of Condition, optional) - Conditions that must pass
- `cooldown` (number, optional) - Cooldown in seconds (default: 0)
- `execute` (function, required) - Function called when ability fires

**Execute Function Signature:**
```javascript
function(ctx) {
  // ctx is an AbilityContext with:
  // - ctx.player (Player)
  // - ctx.trigger (TriggerType)
  // - ctx.targetEntity (Entity, nullable)
  // - ctx.targetBlock (Block, nullable)
  // - ctx.item (ItemStack, nullable)
  // - ctx.event (Event, nullable - raw Bukkit event)
}
```

**Example:**
```javascript
engine.ability({
  id: "fireball",
  triggers: ["RIGHT_CLICK"],
  conditions: [engine.condition.sneaking()],
  cooldown: 3,
  execute: function(ctx) {
    ctx.player.launchProjectile(Java.type("org.bukkit.entity.Fireball"));
  }
});
```

### `engine.trigger` Constants

Access trigger type constants:

- `engine.trigger.RIGHT_CLICK`
- `engine.trigger.LEFT_CLICK`
- `engine.trigger.SHIFT_RIGHT_CLICK`
- `engine.trigger.SHIFT_LEFT_CLICK`
- `engine.trigger.RIGHT_CLICK_ENTITY`
- `engine.trigger.LEFT_CLICK_ENTITY`
- `engine.trigger.SHIFT_RIGHT_CLICK_ENTITY`
- `engine.trigger.SHIFT_LEFT_CLICK_ENTITY`
- `engine.trigger.DAMAGE_DEALT`
- `engine.trigger.DAMAGE_TAKEN`
- `engine.trigger.MOVE`
- `engine.trigger.TICK`

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

**Example:**
```javascript
conditions: [
  engine.condition.sneaking(),
  engine.condition.healthAbove(5.0)
]
```

### `engine.listen(eventClass, handler)`

Registers a listener for any Bukkit event.

**Parameters:**
- `eventClass` (string) - Event class name (short or fully qualified)
- `handler` (function) - Function that receives the event

**Supported Short Names:**
- `"PlayerJoinEvent"`, `"PlayerQuitEvent"`, `"PlayerMoveEvent"`
- `"PlayerInteractEvent"`, `"PlayerInteractEntityEvent"`
- `"EntityDamageEvent"`, `"EntityDamageByEntityEvent"`
- `"BlockBreakEvent"`, `"BlockPlaceEvent"`
- Any event in `org.bukkit.event.**` packages

**Example:**
```javascript
engine.listen("PlayerJoinEvent", function(event) {
  event.getPlayer().sendMessage("§aWelcome!");
});

// Or with full class path
engine.listen("org.bukkit.event.player.PlayerMoveEvent", function(event) {
  // Handle move
});
```

### `engine.sessions` - Session Management

Manage stateful abilities that run over time.

#### `engine.sessions.start(player, ability, handlers)`

Starts a new session.

**Parameters:**
- `player` (Player) - The player
- `ability` (Ability or object with id) - The ability
- `handlers` (object) - Object with lifecycle functions:
  - `onStart()` - Called when session starts
  - `onTick(tickCount)` - Called every tick
  - `onEnd()` - Called when session ends

**Example:**
```javascript
engine.sessions.start(ctx.player, {id: "fire_aura"}, {
  onStart: function() {
    ctx.player.sendMessage("Aura activated!");
  },
  onTick: function(tickCount) {
    if (tickCount > 200) { // 10 seconds
      engine.sessions.end(ctx.player, "fire_aura");
      return;
    }
    // Do something every tick
  },
  onEnd: function() {
    ctx.player.sendMessage("Aura ended!");
  }
});
```

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

### `engine.items` - Item Management

#### `engine.items.create(abilityId)`

Creates an ability item for the given ability ID.

#### `engine.items.isAbilityItem(item)`

Returns `true` if the item is an ability item.

#### `engine.items.getAbilityId(item)`

Returns the ability ID for an item (or `null`).

### Scheduling

#### `engine.scheduleDelayed(function, delayTicks)`

Schedules a one-time delayed task.

**Returns:** Task ID

**Example:**
```javascript
engine.scheduleDelayed(function() {
  engine.log("This runs after 5 seconds");
}, 20 * 5);
```

#### `engine.scheduleRepeating(function, delayTicks, periodTicks)`

Schedules a repeating task.

**Returns:** Task ID

**Example:**
```javascript
var taskId = engine.scheduleRepeating(function() {
  engine.log("Every second");
}, 20, 20);
```

#### `engine.cancelTask(taskId)`

Cancels a scheduled task.

### Logging

```javascript
engine.log("Info message")
engine.warn("Warning message")
engine.error("Error message")
```

## Raw Java Interop (Power Mode)

For advanced use cases, scripts have full access to Java classes via `Java.type()`:

```javascript
const Bukkit = Java.type("org.bukkit.Bukkit");
const Location = Java.type("org.bukkit.Location");
const Particle = Java.type("org.bukkit.Particle");

// Use any Bukkit/Java API directly
Bukkit.broadcastMessage("Hello from script!");

var world = Bukkit.getWorld("world");
world.spawnParticle(Particle.FLAME, new Location(world, 0, 64, 0), 100);
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
4. The GraalVM context is closed
5. The script is re-executed

## Memory Safety

The engine automatically cleans up resources:

- **On script unload/reload:** All abilities, listeners, and tasks are removed
- **On plugin disable:** All scripts are unloaded cleanly
- **On player quit:** Sessions for that player are ended

Scripts cannot cause memory leaks if they follow the engine API.

## Example Scripts

### Basic Ability

```javascript
engine.ability({
  id: "dash",
  triggers: ["SHIFT_RIGHT_CLICK"],
  cooldown: 5,
  execute: function(ctx) {
    var dir = ctx.player.getLocation().getDirection();
    ctx.player.setVelocity(dir.multiply(2.5));
    ctx.player.sendMessage("§bDash!");
  }
});
```

### With Conditions

```javascript
engine.ability({
  id: "heal",
  triggers: ["RIGHT_CLICK"],
  conditions: [
    engine.condition.sneaking(),
    engine.condition.healthBelow(15.0)
  ],
  cooldown: 10,
  execute: function(ctx) {
    var newHealth = Math.min(ctx.player.getHealth() + 6, ctx.player.getMaxHealth());
    ctx.player.setHealth(newHealth);
  }
});
```

### Event Listener

```javascript
engine.listen("PlayerJoinEvent", function(event) {
  event.getPlayer().sendMessage("§aAbilityEngine scripts active!");
});
```

### Session-Based Ability

```javascript
engine.ability({
  id: "fire_aura",
  triggers: ["RIGHT_CLICK"],
  cooldown: 30,
  execute: function(ctx) {
    engine.sessions.start(ctx.player, {id: "fire_aura"}, {
      onStart: function() {
        ctx.player.sendMessage("§cFire Aura!");
      },
      onTick: function(tickCount) {
        if (tickCount > 200) {
          engine.sessions.end(ctx.player, "fire_aura");
          return;
        }
        // Damage nearby entities every second
        if (tickCount % 20 === 0) {
          var loc = ctx.player.getLocation();
          var entities = ctx.player.getWorld().getNearbyEntities(loc, 3, 3, 3);
          entities.forEach(function(e) {
            if (e !== ctx.player) e.damage(2.0);
          });
        }
      },
      onEnd: function() {
        ctx.player.sendMessage("§cAura ended!");
      }
    });
  }
});
```

## Best Practices

1. **Use the engine API** - It's safer and cleaner than raw Java
2. **Handle errors** - Wrap risky code in try/catch
3. **Test with `/ability script reload`** - Fast iteration
4. **Use descriptive IDs** - Ability IDs must be unique across all sources
5. **Log important events** - Use `engine.log()` for debugging
6. **Clean up in onEnd** - Cancel tasks, clear state when sessions end
7. **Avoid blocking operations** - No `Thread.sleep()`, no blocking I/O

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

**Memory leaks**
- Always use `engine.scheduleRepeating()` instead of `setInterval()`
- Always use `engine.listen()` instead of manual listener registration
- Sessions auto-cleanup, but call `engine.sessions.end()` when done
