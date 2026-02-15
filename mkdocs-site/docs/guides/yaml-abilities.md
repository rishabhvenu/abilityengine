# YAML Abilities Guide

Create custom abilities using YAML configuration files. This guide covers everything you need to know about the YAML ability system.

---

## Overview

YAML abilities are the easiest way to create custom abilities in AbilityEngine. They're perfect for server owners who want to add abilities without writing code.

**Key Features**:

- Simple, human-readable syntax
- 11+ built-in action types
- Composable conditions
- Hot-reloadable with `/ability reload`
- No programming experience required

---

## File Location

Place YAML files in:

```
plugins/AbilityEngine/abilities/
```

Files must have `.yml` or `.yaml` extension. All files in this directory are automatically loaded on server start.

---

## Basic Structure

Each YAML file can contain multiple abilities:

```yaml
abilities:
  ability_id_1:
    display-name: "Ability Name 1"
    triggers:
      - TRIGGER_TYPE
    conditions:
      - condition_type: value
    cooldown: "3s"
    actions:
      - type: ACTION_TYPE
        param: value
  
  ability_id_2:
    display-name: "Ability Name 2"
    # ... ability configuration
```

---

## Configuration Options

### Ability ID (Required)

The unique identifier for the ability. Must be unique across all abilities (YAML, scripts, and modules).

```yaml
abilities:
  fireball:     # This is the ability ID
    # ... configuration
```

**Rules**:

- Must be unique
- Case-sensitive
- Used in commands: `/ability give player fireball`
- Alphanumeric, underscores, and hyphens recommended

---

### Display Name (Required)

The name shown on ability items. Supports legacy color codes with `&`.

```yaml
display-name: "&cFireball"
```

**Color Codes**:

| Code | Color |
|------|-------|
| `&0` | Black |
| `&1` | Dark Blue |
| `&2` | Dark Green |
| `&3` | Dark Aqua |
| `&4` | Dark Red |
| `&5` | Dark Purple |
| `&6` | Gold |
| `&7` | Gray |
| `&8` | Dark Gray |
| `&9` | Blue |
| `&a` | Green |
| `&b` | Aqua |
| `&c` | Red |
| `&d` | Light Purple |
| `&e` | Yellow |
| `&f` | White |

**Formatting Codes**:

| Code | Format |
|------|--------|
| `&l` | Bold |
| `&m` | Strikethrough |
| `&n` | Underline |
| `&o` | Italic |
| `&r` | Reset |

**Example**:

```yaml
display-name: "&c&lFireball &r&7(Right Click)"
```

---

### Triggers (Optional)

List of trigger types that activate the ability. If omitted, defaults to `[RIGHT_CLICK]`.

```yaml
triggers:
  - RIGHT_CLICK
  - SHIFT_RIGHT_CLICK
```

**Available Triggers**:

#### Basic Interactions

| Trigger | Description |
|---------|-------------|
| `RIGHT_CLICK` | Right-click with the item |
| `LEFT_CLICK` | Left-click (or left-click air) |

#### Shift Interactions

| Trigger | Description |
|---------|-------------|
| `SHIFT_RIGHT_CLICK` | Sneak + right-click |
| `SHIFT_LEFT_CLICK` | Sneak + left-click |

#### Entity Interactions

| Trigger | Description |
|---------|-------------|
| `RIGHT_CLICK_ENTITY` | Right-click on an entity |
| `LEFT_CLICK_ENTITY` | Left-click/attack an entity |
| `SHIFT_RIGHT_CLICK_ENTITY` | Sneak + right-click entity |
| `SHIFT_LEFT_CLICK_ENTITY` | Sneak + left-click entity |

#### Combat Triggers

| Trigger | Description |
|---------|-------------|
| `DAMAGE_DEALT` | When player damages an entity |
| `DAMAGE_TAKEN` | When player takes damage |

#### Movement Triggers

| Trigger | Description |
|---------|-------------|
| `MOVE` | When player moves (fires frequently!) |

#### Special Triggers

| Trigger | Description |
|---------|-------------|
| `TICK` | Fires every tick (only for session-based abilities) |

!!! warning "Performance Warning"
    Be careful with `MOVE` and `TICK` triggers as they fire very frequently and can impact performance if not optimized.

---

### Conditions (Optional)

Conditions that must ALL be met (AND logic) for the ability to execute. If omitted, ability always executes (subject to cooldown).

```yaml
conditions:
  - sneaking: true
  - health-above: 5.0
  - y-above: 64.0
```

**Available Conditions**:

#### Player State

```yaml
conditions:
  - sneaking: true        # Player must be sneaking
  - sneaking: false       # Player must NOT be sneaking
```

#### Health Conditions

```yaml
conditions:
  - health-above: 10.0    # Health > 10 (exclusive)
  - health-below: 15.0    # Health < 15 (exclusive)
```

#### Position Conditions

```yaml
conditions:
  - y-above: 64.0         # Y coordinate > 64
  - y-below: 100.0        # Y coordinate < 100
```

#### Target Conditions

```yaml
conditions:
  - has-target: true      # Must have a target entity
```

Used with entity-click triggers to ensure an entity was targeted.

**Multiple Conditions Example**:

```yaml
conditions:
  - sneaking: true
  - health-above: 5.0
  - y-above: 0
  - has-target: false
```

All conditions must pass for the ability to execute.

---

### Cooldown (Optional)

Cooldown duration before the ability can be used again. Defaults to no cooldown if omitted.

**Format**: `<number><unit>`

**Units**:

| Unit | Meaning |
|------|---------|
| `s` | Seconds |
| `m` | Minutes |
| `h` | Hours |
| `d` | Days |

**Examples**:

```yaml
cooldown: 3s       # 3 seconds
cooldown: 1m       # 1 minute
cooldown: 30s      # 30 seconds
cooldown: 1.5m     # 1.5 minutes (90 seconds)
cooldown: 2h       # 2 hours
cooldown: 0s       # No cooldown
```

---

### Actions (Required)

List of actions to execute when the ability triggers. Actions are executed in order.

```yaml
actions:
  - type: LAUNCH_PROJECTILE
    projectile: FIREBALL
    speed: 2.0
  - type: SEND_MESSAGE
    message: "&cFireball launched!"
```

**Available action types**: See [Action Types](#action-types) below.

---

## Action Types

AbilityEngine includes 11 built-in action types:

### 1. LAUNCH_PROJECTILE

Launches a projectile from the player.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `projectile` | String | Yes | - | Projectile entity type |
| `speed` | Number | No | 1.0 | Launch speed multiplier |

**Example**:

```yaml
- type: LAUNCH_PROJECTILE
  projectile: FIREBALL
  speed: 2.0
```

**Common Projectiles**:

- `ARROW`
- `SNOWBALL`
- `EGG`
- `FIREBALL`
- `SMALL_FIREBALL`
- `ENDER_PEARL`
- `TRIDENT`
- `SPECTRAL_ARROW`

---

### 2. SEND_MESSAGE

Sends a message to the player.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `message` | String | Yes | - | Message text (supports `&` color codes) |

**Example**:

```yaml
- type: SEND_MESSAGE
  message: "&aYou used an ability!"
```

---

### 3. PLAY_SOUND

Plays a sound at the player's location.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `sound` | String | Yes | - | Bukkit Sound enum value |
| `volume` | Number | No | 1.0 | Volume (0.0 to 2.0+) |
| `pitch` | Number | No | 1.0 | Pitch (0.5 to 2.0) |

**Example**:

```yaml
- type: PLAY_SOUND
  sound: ENTITY_PLAYER_LEVELUP
  volume: 1.0
  pitch: 1.2
```

**Common Sounds**:

- `ENTITY_PLAYER_LEVELUP`
- `ENTITY_BLAZE_SHOOT`
- `ENTITY_LIGHTNING_BOLT_THUNDER`
- `ENTITY_WITHER_SHOOT`
- `ENTITY_ENDERMAN_TELEPORT`
- `BLOCK_ANVIL_LAND`

For a full list, see [Bukkit Sound enum](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html).

---

### 4. PLAY_EFFECT

Spawns particle effects at the player's location.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `particle` | String | Yes | - | Bukkit Particle enum value |
| `count` | Integer | No | 1 | Number of particles |

**Example**:

```yaml
- type: PLAY_EFFECT
  particle: FLAME
  count: 20
```

**Common Particles**:

- `FLAME`
- `CLOUD`
- `HEART`
- `EXPLOSION_LARGE`
- `SMOKE_LARGE`
- `PORTAL`
- `ENCHANTMENT_TABLE`
- `VILLAGER_HAPPY`

For a full list, see [Bukkit Particle enum](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Particle.html).

---

### 5. DAMAGE

Damages the target entity. Requires a target from the trigger (e.g., entity-click triggers).

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `damage` | Number | Yes | - | Damage amount (hearts) |

**Example**:

```yaml
- type: DAMAGE
  damage: 5.0
```

!!! info "Target Required"
    This action only works if the ability was triggered by an entity interaction (`RIGHT_CLICK_ENTITY`, `LEFT_CLICK_ENTITY`, etc.) or if a target entity exists in the context.

---

### 6. HEAL

Heals the player. Health is capped at max health.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `amount` | Number | Yes | - | Health to restore (hearts) |

**Example**:

```yaml
- type: HEAL
  amount: 5.0
```

---

### 7. VELOCITY

Applies velocity to the player or target entity.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `x` | Number | No | 0.0 | X velocity component |
| `y` | Number | No | 0.0 | Y velocity component (positive = up) |
| `z` | Number | No | 0.0 | Z velocity component |

**Example**:

```yaml
# Launch player upward
- type: VELOCITY
  x: 0.0
  y: 1.5
  z: 0.0
```

**Tips**:

- Positive Y launches upward
- Combine with TELEPORT for dash effects
- Values above 2.0 can be dangerous (fall damage)

---

### 8. POTION_EFFECT

Applies a potion effect to the player or target entity.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `effect` | String | Yes | - | Potion effect type |
| `duration` | Integer | No | 100 | Duration in ticks (20 ticks = 1 second) |
| `amplifier` | Integer | No | 0 | Effect level (0 = level I, 1 = level II, etc.) |

**Example**:

```yaml
- type: POTION_EFFECT
  effect: SPEED
  duration: 200     # 10 seconds
  amplifier: 1      # Speed II
```

**Common Effects**:

- `SPEED`
- `SLOW`
- `JUMP`
- `REGENERATION`
- `STRENGTH`
- `INVISIBILITY`
- `NIGHT_VISION`
- `WEAKNESS`
- `POISON`

For a full list, see [Bukkit PotionEffectType](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html).

---

### 9. TELEPORT

Teleports the player.

**Parameters (Absolute)**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `x` | Number | Yes* | - | Target X coordinate |
| `y` | Number | Yes* | - | Target Y coordinate |
| `z` | Number | Yes* | - | Target Z coordinate |

**Parameters (Relative)**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `forward` | Number | Yes* | - | Distance to teleport forward |

*Either absolute coordinates (x, y, z) OR relative (forward) is required.

**Examples**:

```yaml
# Absolute teleport
- type: TELEPORT
  x: 100.0
  y: 64.0
  z: 200.0

# Relative teleport (forward)
- type: TELEPORT
  forward: 10.0
```

---

### 10. SPAWN_ENTITY

Spawns an entity at the player's location.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `entity` | String | Yes | - | Entity type |

**Example**:

```yaml
- type: SPAWN_ENTITY
  entity: ZOMBIE
```

**Common Entities**:

- `ZOMBIE`
- `SKELETON`
- `CREEPER`
- `ENDERMAN`
- `IRON_GOLEM`
- `VILLAGER`
- `COW`
- `PIG`

For a full list, see [Bukkit EntityType](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html).

---

### 11. COMMAND

Executes a console command. Use `{player}` placeholder for the player's name.

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `command` | String | Yes | - | Command to execute |

**Example**:

```yaml
- type: COMMAND
  command: "give {player} diamond 1"
```

**Placeholders**:

- `{player}` - Replaced with player's name

**Advanced Example**:

```yaml
- type: COMMAND
  command: "execute at {player} run summon lightning_bolt"
```

!!! warning "Security"
    Commands run as console (bypass permissions). Use with caution in public configurations.

---

## Complete Examples

### Basic Fireball

```yaml
abilities:
  fireball:
    display-name: "&cFireball"
    triggers:
      - RIGHT_CLICK
    conditions:
      - sneaking: true
    cooldown: 3s
    actions:
      - type: LAUNCH_PROJECTILE
        projectile: FIREBALL
        speed: 2.0
      - type: SEND_MESSAGE
        message: "&cFireball launched!"
      - type: PLAY_SOUND
        sound: ENTITY_BLAZE_SHOOT
```

### Healing Touch

```yaml
abilities:
  heal:
    display-name: "&aHealing Touch"
    triggers:
      - RIGHT_CLICK
    conditions:
      - health-below: 15.0
    cooldown: 10s
    actions:
      - type: HEAL
        amount: 5.0
      - type: SEND_MESSAGE
        message: "&aYou have been healed!"
      - type: PLAY_EFFECT
        particle: HEART
        count: 10
      - type: PLAY_SOUND
        sound: ENTITY_PLAYER_LEVELUP
```

### Dash

```yaml
abilities:
  dash:
    display-name: "&bDash"
    triggers:
      - SHIFT_RIGHT_CLICK
    cooldown: 5s
    actions:
      - type: VELOCITY
        x: 0.0
        y: 0.5
        z: 0.0
      - type: TELEPORT
        forward: 5.0
      - type: PLAY_EFFECT
        particle: CLOUD
        count: 20
      - type: SEND_MESSAGE
        message: "&bDashed!"
```

### Lightning Strike (Entity Target)

```yaml
abilities:
  lightning:
    display-name: "&eLightning Strike"
    triggers:
      - RIGHT_CLICK_ENTITY
    conditions:
      - has-target: true
    cooldown: 15s
    actions:
      - type: COMMAND
        command: "execute at {player} run summon lightning_bolt"
      - type: PLAY_SOUND
        sound: ENTITY_LIGHTNING_BOLT_THUNDER
        volume: 2.0
      - type: SEND_MESSAGE
        message: "&eLightning summoned!"
```

### Life Steal

```yaml
abilities:
  lifesteal:
    display-name: "&4Life Steal"
    triggers:
      - DAMAGE_DEALT
    conditions:
      - health-below: 15.0
    cooldown: 8s
    actions:
      - type: HEAL
        amount: 2.0
      - type: SEND_MESSAGE
        message: "&4Life stolen!"
      - type: PLAY_EFFECT
        particle: HEART
        count: 5
```

### Speed Boost

```yaml
abilities:
  speed_boost:
    display-name: "&eSpeed Boost"
    triggers:
      - RIGHT_CLICK
    cooldown: 30s
    actions:
      - type: POTION_EFFECT
        effect: SPEED
        duration: 200        # 10 seconds
        amplifier: 2         # Speed III
      - type: POTION_EFFECT
        effect: JUMP
        duration: 200
        amplifier: 1         # Jump II
      - type: SEND_MESSAGE
        message: "&eSpeed boost activated!"
```

---

## Tips & Best Practices

### Organization

!!! tip "Multiple Files"
    Split abilities across multiple YAML files for better organization:
    ```
    abilities/
    ├── combat.yml        # Combat abilities
    ├── movement.yml      # Movement abilities
    ├── utility.yml       # Utility abilities
    └── custom.yml        # Custom server-specific abilities
    ```

### Testing

!!! tip "Quick Testing"
    Use `/ability reload` to test changes without restarting:
    ```
    1. Edit YAML file
    2. Run /ability reload
    3. Test immediately
    ```

### Naming

!!! tip "Descriptive IDs"
    Use clear, descriptive ability IDs:
    - **Good**: `fireball`, `healing_touch`, `lightning_strike`
    - **Bad**: `ability1`, `test`, `a`

### Comments

!!! tip "Document Your Abilities"
    Use YAML comments to document complex abilities:
    ```yaml
    abilities:
      # Ultimate fireball with knockback and speed boost
      ultimate_fireball:
        display-name: "&c&lUltimate Fireball"
        triggers:
          - SHIFT_RIGHT_CLICK  # Requires sneak for safety
        cooldown: 30s          # Long cooldown due to power
        actions:
          # Launch fireball at high speed
          - type: LAUNCH_PROJECTILE
            projectile: FIREBALL
            speed: 3.0
          # Give player speed to escape
          - type: POTION_EFFECT
            effect: SPEED
            duration: 100
            amplifier: 1
    ```

### Performance

!!! warning "Avoid Frequent Triggers"
    Be careful with `MOVE` and `TICK` triggers - they fire very frequently and can cause lag if not optimized.

### Cooldowns

!!! tip "Balance Cooldowns"
    - Powerful abilities: 15-30 seconds
    - Medium abilities: 5-10 seconds
    - Weak abilities: 2-5 seconds
    - No cooldown: Use sparingly

### Safety

!!! warning "Test Before Production"
    Always test abilities in a development environment before deploying to a production server, especially when using:
    - COMMAND actions
    - High velocity values
    - Entity spawning

---

## Troubleshooting

### YAML not loading

**Common causes**:

1. **Indentation errors** - Use spaces, not tabs. Each level is 2 spaces.
2. **Missing colons** - Every key needs a colon: `key: value`
3. **File extension** - Must be `.yml` or `.yaml`
4. **Syntax errors** - Check console for error messages

**Solution**: Use an online YAML validator or check server logs for specific errors.

### Ability not triggering

**Check**:

1. Are you holding the ability item?
2. Do you meet all conditions (sneaking, health, etc.)?
3. Is the ability on cooldown? (`/ability info <ability_id>`)
4. Is the trigger type correct for your action?

### Actions not working

**Common issues**:

- **DAMAGE action**: Requires target entity (use with entity-click triggers)
- **TELEPORT forward**: Player must be facing a valid direction
- **COMMAND**: Check console for command execution errors

---

## Next Steps

- [Action Types Reference](../reference/actions.md) - Complete action parameter reference
- [YAML Schema Reference](../reference/yaml-schema.md) - Full schema documentation
- [Trigger Reference](../reference/trigger-reference.md) - All trigger types explained
- [YAML Examples](../examples/yaml-examples.md) - More complete examples
- [JavaScript Scripting](scripting.md) - More powerful ability creation
