# AbilitySession Interface

Interface for stateful abilities that run over time.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Interface Declaration

```java
public interface AbilitySession
```

---

## Overview

`AbilitySession` represents an active ability session for a player. Sessions support abilities with continuous effects or tick-based logic.

**Use cases**: Grappling hooks, auras, channels, active buffs

**Base implementation**: `BaseAbilitySession` (in core module)

---

## Lifecycle

```
start() → tick() → tick() → tick() → ... → end()
```

1. **start()** - Called once when session begins
2. **tick()** - Called every tick (50ms) while active
3. **end()** - Called once when session ends

---

## Methods

### `sessionId()`

```java
UUID sessionId()
```

Returns the unique identifier for this session.

**Returns**: Session ID (unique per session)

---

### `player()`

```java
Player player()
```

Returns the player this session is bound to.

**Returns**: The player

---

### `ability()`

```java
Ability ability()
```

Returns the ability this session is running.

**Returns**: The ability

---

### `start()`

```java
void start()
```

Called when the session starts.

**Override**: Implement initialization logic here

---

### `tick()`

```java
void tick()
```

Called every tick while the session is active.

**Override**: Implement per-tick logic here

**Frequency**: 20 times per second (every 50ms)

---

### `end()`

```java
void end()
```

Called when the session ends.

**Override**: Implement cleanup logic here

**Always called**: Even on player disconnect or plugin disable

---

### `isActive()`

```java
boolean isActive()
```

Checks if this session is currently active.

**Returns**: `true` if active

---

### `getTickCount()`

```java
int getTickCount()
```

Gets the number of ticks this session has been active.

**Returns**: Tick count (starts at 0)

---

## Implementation Example

### Basic Session

```java
public class FireAuraSession extends BaseAbilitySession {
    private int duration;
    
    public FireAuraSession(Player player, Ability ability, int durationTicks) {
        super(player, ability);
        this.duration = durationTicks;
    }
    
    @Override
    public void start() {
        super.start();
        player().sendMessage("§cFire Aura activated!");
    }
    
    @Override
    public void tick() {
        super.tick();  // Important: increments tick count
        
        // Check duration
        if (getTickCount() > duration) {
            end();
            return;
        }
        
        // Execute every second
        if (getTickCount() % 20 == 0) {
            damageNearbyEntities();
            spawnParticles();
        }
    }
    
    @Override
    public void end() {
        super.end();
        player().sendMessage("§cAura ended!");
    }
    
    private void damageNearbyEntities() {
        var location = player().getLocation();
        player().getWorld().getNearbyEntities(location, 3, 3, 3).stream()
            .filter(e -> e instanceof LivingEntity)
            .filter(e -> e != player())
            .map(e -> (LivingEntity) e)
            .forEach(e -> e.damage(2.0, player()));
    }
    
    private void spawnParticles() {
        player().getWorld().spawnParticle(
            Particle.FLAME,
            player().getLocation(),
            20, 1.0, 1.0, 1.0, 0.1
        );
    }
}
```

### Grappling Hook Session

```java
public class GrappleSession extends BaseAbilitySession {
    private final Vector direction;
    private final int duration;
    
    public GrappleSession(Player player, Ability ability, Vector direction, int duration) {
        super(player, ability);
        this.direction = direction.normalize();
        this.duration = duration;
    }
    
    @Override
    public void start() {
        super.start();
        player().sendMessage("§bGrappling!");
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (getTickCount() > duration) {
            end();
            return;
        }
        
        // Apply velocity
        player().setVelocity(direction.multiply(0.8));
        
        // Particles every 2 ticks
        if (getTickCount() % 2 == 0) {
            player().getWorld().spawnParticle(
                Particle.CLOUD,
                player().getLocation(),
                3
            );
        }
    }
    
    @Override
    public void end() {
        super.end();
        player().sendMessage("§bGrapple complete!");
    }
}
```

---

## Usage with SessionManager

### Starting Sessions

```java
SessionManager sessionManager = getSessionManager();
AbilitySession session = new FireAuraSession(player, ability, 200);
sessionManager.startSession(player, ability, session);
```

### Ending Sessions

```java
sessionManager.endSession(player, "fire_aura");
```

### Getting Active Sessions

```java
Collection<AbilitySession> sessions = sessionManager.getActiveSessions(player);
for (AbilitySession session : sessions) {
    System.out.println(session.ability().id() + " - " + session.getTickCount() + " ticks");
}
```

---

## JavaScript Sessions

JavaScript has a simplified session API:

```javascript
engine.sessions.start(player, {id: "fire_aura"}, {
  onStart: function() {
    player.sendMessage("§cStarted!");
  },
  
  onTick: function(tickCount) {
    if (tickCount > 200) {
      engine.sessions.end(player, "fire_aura");
      return;
    }
    // Tick logic
  },
  
  onEnd: function() {
    player.sendMessage("§cEnded!");
  }
});
```

---

## Best Practices

### Always Call super()

```java
@Override
public void start() {
    super.start();  // Important!
    // Your code
}

@Override
public void tick() {
    super.tick();  // Increments tick count
    // Your code
}

@Override
public void end() {
    super.end();  // Cleanup
    // Your code
}
```

### Set Maximum Duration

```java
@Override
public void tick() {
    super.tick();
    
    if (getTickCount() > MAX_DURATION) {
        end();
        return;
    }
    
    // Rest of logic
}
```

### Optimize Tick Logic

```java
@Override
public void tick() {
    super.tick();
    
    // Run every second instead of every tick
    if (getTickCount() % 20 == 0) {
        expensiveOperation();
    }
}
```

### Clean Up Resources

```java
public class TaskSession extends BaseAbilitySession {
    private BukkitTask task;
    
    @Override
    public void start() {
        super.start();
        task = Bukkit.getScheduler().runTaskTimer(...);
    }
    
    @Override
    public void end() {
        super.end();
        if (task != null) {
            task.cancel();  // Important!
        }
    }
}
```

---

## Automatic Cleanup

Sessions are automatically ended when:

- Player disconnects
- Plugin disables
- `end()` is called manually

The `end()` method is always called, so use it for cleanup.

---

## See Also

- [Ability](ability.md) - Creating abilities
- [Sessions Guide](../../guides/sessions.md) - Complete guide
- [Script Examples](../../examples/script-examples.md) - JavaScript session examples
