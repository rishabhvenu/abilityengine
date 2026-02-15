# Creating Abilities with YAML

## Overview

AbilityEngine allows server owners to create abilities using YAML configuration files. This guide covers the YAML schema and all available options.

## File Location

Place ability configuration files in:

```
plugins/AbilityEngine/abilities/
```

Files must have `.yml` or `.yaml` extension.

## Basic Structure

Each YAML file can contain multiple abilities:

```yaml
abilities:
  ability_id:
    display-name: "Display Name"
    triggers:
      - TRIGGER_TYPE
    conditions:
      - condition_type: value
    cooldown: "3s"
    actions:
      - type: ACTION_TYPE
        param1: value1
        param2: value2
```

## Complete Example

```yaml
abilities:
  fireball:
    display-name: "&cFireball"
    triggers:
      - RIGHT_CLICK
    conditions:
      - sneaking: true
      - health-above: 5.0
    cooldown: 3s
    actions:
      - type: LAUNCH_PROJECTILE
        projectile: FIREBALL
        speed: 2.0
      - type: SEND_MESSAGE
        message: "&cFireball launched!"
      - type: PLAY_SOUND
        sound: ENTITY_BLAZE_SHOOT
        volume: 1.0
        pitch: 1.0

  healing_touch:
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

  lightning_strike:
    display-name: "&eLightning Strike"
    triggers:
      - SHIFT_RIGHT_CLICK
    conditions:
      - has-target: true
    cooldown: 15s
    actions:
      - type: COMMAND
        command: "execute at {player} run summon lightning_bolt"
      - type: PLAY_SOUND
        sound: ENTITY_LIGHTNING_BOLT_THUNDER
```

## Triggers

List of trigger types that activate the ability. Multiple triggers can be specified.

### Available Triggers

**Basic Interactions:**
- `RIGHT_CLICK` - Right-click with the item
- `LEFT_CLICK` - Left-click with the item

**Shift Interactions:**
- `SHIFT_RIGHT_CLICK` - Shift + right-click
- `SHIFT_LEFT_CLICK` - Shift + left-click

**Entity Interactions:**
- `RIGHT_CLICK_ENTITY` - Right-click on an entity
- `LEFT_CLICK_ENTITY` - Left-click/attack an entity
- `SHIFT_RIGHT_CLICK_ENTITY` - Shift + right-click on entity
- `SHIFT_LEFT_CLICK_ENTITY` - Shift + left-click on entity

**Combat:**
- `DAMAGE_DEALT` - When player damages an entity
- `DAMAGE_TAKEN` - When player takes damage

**Movement:**
- `MOVE` - When player moves (fires frequently, use with caution)

**Special:**
- `TICK` - Fires every tick (only for session-based abilities)

## Conditions

Conditions must ALL be met (AND logic) for the ability to execute.

### Available Conditions

**Player State:**

```yaml
conditions:
  - sneaking: true          # Player must be sneaking
  - sneaking: false         # Player must NOT be sneaking
```

**Health:**

```yaml
conditions:
  - health-above: 10.0      # Health > 10 (exclusive)
  - health-below: 15.0      # Health < 15 (exclusive)
```

**Position:**

```yaml
conditions:
  - y-above: 64.0           # Y coordinate > 64
  - y-below: 100.0          # Y coordinate < 100
```

**Target:**

```yaml
conditions:
  - has-target: true        # Must have a target entity (for entity-click triggers)
```

### Multiple Conditions

All conditions are combined with AND logic:

```yaml
conditions:
  - sneaking: true
  - health-above: 5.0
  - y-above: 0
```

This means: player is sneaking AND health > 5 AND y > 0.

## Cooldown

Cooldown duration before the ability can be used again.

**Format:** `<number><unit>`

**Units:**
- `s` - Seconds
- `m` - Minutes
- `h` - Hours
- `d` - Days

**Examples:**

```yaml
cooldown: 3s      # 3 seconds
cooldown: 1m      # 1 minute
cooldown: 30s     # 30 seconds
cooldown: 0s      # No cooldown
```

## Actions

Actions are executed in order when the ability triggers. Multiple actions can be specified.

### LAUNCH_PROJECTILE

Launches a projectile from the player.

```yaml
- type: LAUNCH_PROJECTILE
  projectile: FIREBALL      # Entity type (ARROW, SNOWBALL, FIREBALL, etc.)
  speed: 2.0                # Optional, default: 1.0
```

### SEND_MESSAGE

Sends a message to the player. Supports legacy color codes (&).

```yaml
- type: SEND_MESSAGE
  message: "&cYou used an ability!"
```

### PLAY_SOUND

Plays a sound at the player's location.

```yaml
- type: PLAY_SOUND
  sound: ENTITY_PLAYER_LEVELUP    # Bukkit Sound enum
  volume: 1.0                      # Optional, default: 1.0
  pitch: 1.0                       # Optional, default: 1.0
```

### PLAY_EFFECT

Spawns particle effects at the player's location.

```yaml
- type: PLAY_EFFECT
  particle: FLAME      # Bukkit Particle enum
  count: 10            # Optional, default: 1
```

### DAMAGE

Damages the target entity (requires target from trigger).

```yaml
- type: DAMAGE
  damage: 5.0          # Damage amount
```

### HEAL

Heals the player.

```yaml
- type: HEAL
  amount: 4.0          # Health to restore (capped at max health)
```

### VELOCITY

Applies velocity to the player or target entity.

```yaml
- type: VELOCITY
  x: 0.0               # X component
  y: 1.0               # Y component (positive = up)
  z: 0.0               # Z component
```

### POTION_EFFECT

Applies a potion effect to the player or target entity.

```yaml
- type: POTION_EFFECT
  effect: SPEED              # Potion effect type
  duration: 100              # Duration in ticks (20 ticks = 1 second)
  amplifier: 1               # Effect level (0 = level 1, 1 = level 2, etc.)
```

### TELEPORT

Teleports the player.

**Absolute teleport:**

```yaml
- type: TELEPORT
  x: 100.0
  y: 64.0
  z: 200.0
```

**Relative teleport (forward):**

```yaml
- type: TELEPORT
  forward: 10.0        # Teleport 10 blocks in the direction player is facing
```

### SPAWN_ENTITY

Spawns an entity at the player's location.

```yaml
- type: SPAWN_ENTITY
  entity: ZOMBIE       # Entity type
```

### COMMAND

Executes a console command. Use `{player}` placeholder for player name.

```yaml
- type: COMMAND
  command: "give {player} diamond 1"
```

## Creating Ability Items

Once abilities are defined in YAML and the plugin is loaded, use commands to create items:

```
/ability give <player> <ability_id>
```

Example:

```
/ability give Steve fireball
```

## Reloading Abilities

To reload abilities after editing YAML files:

```
/ability reload
```

## Viewing Registered Abilities

List all abilities:

```
/ability list
```

View ability details:

```
/ability info <ability_id>
```

## Tips & Best Practices

1. **Start Simple** - Create basic abilities first, then add complexity
2. **Test Cooldowns** - Balance cooldowns to prevent spam
3. **Use Conditions** - Add safety conditions (health checks, sneaking requirements)
4. **Combine Actions** - Multiple actions create more interesting abilities
5. **Name Clearly** - Use descriptive ability IDs and display names
6. **Comment Your Files** - Use YAML comments (#) to document complex abilities
7. **Avoid MOVE Trigger** - Fires very frequently, can cause lag if not optimized

## Example: Complete Ability Set

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

  life_steal:
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

  meteor_rain:
    display-name: "&6Meteor Rain"
    triggers:
      - RIGHT_CLICK
    conditions:
      - sneaking: true
      - y-above: 60.0
    cooldown: 30s
    actions:
      - type: COMMAND
        command: "execute at {player} run summon fireball ~ ~10 ~"
      - type: PLAY_SOUND
        sound: ENTITY_WITHER_SHOOT
        volume: 2.0
      - type: SEND_MESSAGE
        message: "&6Meteor rain summoned!"
```
