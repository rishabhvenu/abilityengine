# Action Types Reference

Complete reference for all 11 action types in YAML abilities.

---

## 1. LAUNCH_PROJECTILE

Launches a projectile from the player.

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

**Common Projectiles**: `ARROW`, `FIREBALL`, `SNOWBALL`, `EGG`, `ENDER_PEARL`, `TRIDENT`

---

## 2. SEND_MESSAGE

Sends a message to the player.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `message` | String | Yes | - | Message text (supports `&` color codes) |

**Example**:

```yaml
- type: SEND_MESSAGE
  message: "&aAbility used!"
```

---

## 3. PLAY_SOUND

Plays a sound at the player's location.

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

**Common Sounds**: `ENTITY_PLAYER_LEVELUP`, `ENTITY_BLAZE_SHOOT`, `ENTITY_LIGHTNING_BOLT_THUNDER`

---

## 4. PLAY_EFFECT

Spawns particle effects.

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

**Common Particles**: `FLAME`, `CLOUD`, `HEART`, `EXPLOSION_LARGE`, `PORTAL`

---

## 5. DAMAGE

Damages the target entity.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `damage` | Number | Yes | - | Damage amount (hearts) |

**Example**:

```yaml
- type: DAMAGE
  damage: 5.0
```

**Note**: Requires target entity (use with entity-click triggers)

---

## 6. HEAL

Heals the player.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `amount` | Number | Yes | - | Health to restore (hearts) |

**Example**:

```yaml
- type: HEAL
  amount: 5.0
```

---

## 7. VELOCITY

Applies velocity to player or target.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `x` | Number | No | 0.0 | X velocity component |
| `y` | Number | No | 0.0 | Y velocity component |
| `z` | Number | No | 0.0 | Z velocity component |

**Example**:

```yaml
- type: VELOCITY
  x: 0.0
  y: 1.5
  z: 0.0
```

---

## 8. POTION_EFFECT

Applies a potion effect.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `effect` | String | Yes | - | Potion effect type |
| `duration` | Integer | No | 100 | Duration in ticks (20 ticks = 1 second) |
| `amplifier` | Integer | No | 0 | Effect level (0 = level I, 1 = level II) |

**Example**:

```yaml
- type: POTION_EFFECT
  effect: SPEED
  duration: 200
  amplifier: 1
```

**Common Effects**: `SPEED`, `JUMP`, `REGENERATION`, `STRENGTH`, `INVISIBILITY`

---

## 9. TELEPORT

Teleports the player.

**Absolute**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `x` | Number | Yes* | - | Target X coordinate |
| `y` | Number | Yes* | - | Target Y coordinate |
| `z` | Number | Yes* | - | Target Z coordinate |

**Relative**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `forward` | Number | Yes* | - | Distance to teleport forward |

**Examples**:

```yaml
# Absolute
- type: TELEPORT
  x: 100.0
  y: 64.0
  z: 200.0

# Relative
- type: TELEPORT
  forward: 10.0
```

---

## 10. SPAWN_ENTITY

Spawns an entity at the player's location.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `entity` | String | Yes | - | Entity type |

**Example**:

```yaml
- type: SPAWN_ENTITY
  entity: ZOMBIE
```

**Common Entities**: `ZOMBIE`, `SKELETON`, `CREEPER`, `IRON_GOLEM`, `VILLAGER`

---

## 11. COMMAND

Executes a console command.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `command` | String | Yes | - | Command to execute |

**Placeholders**: `{player}` - Player name

**Example**:

```yaml
- type: COMMAND
  command: "give {player} diamond 1"
```

**Warning**: Commands run as console (bypass permissions)

---

## See Also

- [YAML Abilities Guide](../guides/yaml-abilities.md)
- [YAML Schema](yaml-schema.md)
- [YAML Examples](../examples/yaml-examples.md)
