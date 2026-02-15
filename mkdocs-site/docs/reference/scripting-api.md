# Scripting API Reference

Complete JavaScript API reference for the `engine` global object.

---

## engine.ability(config)

Registers an ability from a JavaScript object.

```javascript
engine.ability({
  id: "ability_id",
  triggers: ["RIGHT_CLICK"],
  conditions: [engine.condition.sneaking()],
  cooldown: 5,
  execute: function(ctx) {
    // Ability logic
  }
});
```

**Config Object**:

- `id` (String, required) - Unique ability identifier
- `triggers` (Array, optional) - Trigger types (default: `["RIGHT_CLICK"]`)
- `conditions` (Array, optional) - Conditions (default: `[]`)
- `cooldown` (Number, optional) - Cooldown in seconds (default: `0`)
- `execute` (Function, required) - Execution function

**Context Properties** (`ctx`):

- `ctx.player` - Player object
- `ctx.trigger` - TriggerType
- `ctx.targetEntity` - Entity or null
- `ctx.targetBlock` - Block or null
- `ctx.item` - ItemStack or null
- `ctx.event` - Event or null

---

## Triggers

### engine.trigger.*

Access trigger constants:

- `engine.trigger.RIGHT_CLICK`
- `engine.trigger.LEFT_CLICK`
- `engine.trigger.SHIFT_RIGHT_CLICK`
- `engine.trigger.SHIFT_LEFT_CLICK`
- `engine.trigger.RIGHT_CLICK_ENTITY`
- `engine.trigger.LEFT_CLICK_ENTITY`
- `engine.trigger.DAMAGE_DEALT`
- `engine.trigger.DAMAGE_TAKEN`
- `engine.trigger.MOVE`
- `engine.trigger.TICK`

---

## Conditions

### engine.condition.sneaking()

Checks if player is sneaking.

### engine.condition.notSneaking()

Checks if player is NOT sneaking.

### engine.condition.healthAbove(threshold)

Checks if health > threshold.

### engine.condition.healthBelow(threshold)

Checks if health < threshold.

### engine.condition.yAbove(y)

Checks if Y coordinate > y.

### engine.condition.yBelow(y)

Checks if Y coordinate < y.

### engine.condition.hasTarget()

Checks if target entity exists.

### engine.condition.custom(function)

Creates custom condition.

```javascript
engine.condition.custom(function(ctx) {
  return ctx.player.getWorld().getName() === "world_nether";
})
```

---

## Event Listening

### engine.listen(eventClass, handler)

Registers a listener for any Bukkit event.

```javascript
engine.listen("PlayerJoinEvent", function(event) {
  event.getPlayer().sendMessage("Welcome!");
});
```

**Short Names**: `"PlayerJoinEvent"`, `"PlayerMoveEvent"`, `"EntityDamageEvent"`, etc.

**Full Names**: `"org.bukkit.event.player.PlayerJoinEvent"`

---

## Sessions

### engine.sessions.start(player, ability, handlers)

Starts a new session.

```javascript
engine.sessions.start(ctx.player, {id: "ability_id"}, {
  onStart: function() {
    // Called once when session starts
  },
  onTick: function(tickCount) {
    // Called every tick
    if (tickCount > 200) {
      engine.sessions.end(ctx.player, "ability_id");
    }
  },
  onEnd: function() {
    // Called once when session ends
  }
});
```

### engine.sessions.end(player, abilityId)

Ends all sessions for player running this ability.

### engine.sessions.getActive(player)

Returns array of ability IDs for active sessions.

```javascript
var active = engine.sessions.getActive(ctx.player);
```

---

## Cooldowns

### engine.cooldowns.isReady(player, abilityId)

Returns `true` if ability is ready (not on cooldown).

### engine.cooldowns.set(player, abilityId, seconds)

Sets a cooldown manually.

### engine.cooldowns.remaining(player, abilityId)

Returns remaining cooldown Duration object.

```javascript
var remaining = engine.cooldowns.remaining(ctx.player, "fireball");
if (remaining.toSeconds() > 0) {
  ctx.player.sendMessage("Wait " + remaining.toSeconds() + " seconds");
}
```

---

## Items

### engine.items.create(abilityId)

Creates an ability item.

```javascript
var item = engine.items.create("fireball");
if (item !== null) {
  ctx.player.getInventory().addItem(item);
}
```

### engine.items.isAbilityItem(item)

Returns `true` if item is an ability item.

### engine.items.getAbilityId(item)

Returns ability ID for item (or `null`).

---

## Scheduling

### engine.scheduleDelayed(function, delayTicks)

Schedules a one-time delayed task.

```javascript
engine.scheduleDelayed(function() {
  engine.log("Delayed task");
}, 20 * 5);  // 5 seconds
```

### engine.scheduleRepeating(function, delayTicks, periodTicks)

Schedules a repeating task.

```javascript
var taskId = engine.scheduleRepeating(function() {
  engine.log("Every second");
}, 20, 20);
```

### engine.cancelTask(taskId)

Cancels a scheduled task.

---

## Logging

### engine.log(message)

Logs info message to console.

### engine.warn(message)

Logs warning message.

### engine.error(message)

Logs error message.

---

## Java Interop

Access Java classes via `Java.type()`:

```javascript
const Bukkit = Java.type("org.bukkit.Bukkit");
const Location = Java.type("org.bukkit.Location");
const Particle = Java.type("org.bukkit.Particle");

Bukkit.broadcastMessage("Hello!");
```

---

## See Also

- [Scripting Guide](../guides/scripting.md)
- [Script Examples](../examples/script-examples.md)
- [Sessions Guide](../guides/sessions.md)
