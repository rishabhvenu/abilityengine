# YAML Schema Reference

Complete reference for YAML ability configuration.

---

## File Structure

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
        param: value
```

---

## Top-Level

### `abilities`

Root object containing all ability definitions.

**Type**: Object (map)

**Required**: Yes

---

## Ability Properties

### `ability_id`

Unique identifier for the ability.

**Type**: String (key)

**Required**: Yes

**Rules**:

- Must be unique across all abilities
- Alphanumeric, underscores, hyphens
- Case-sensitive

---

### `display-name`

Display name for the ability item.

**Type**: String

**Required**: Yes

**Supports**: Legacy color codes (`&`)

**Example**:

```yaml
display-name: "&cFireball"
```

---

### `triggers`

List of trigger types that activate the ability.

**Type**: Array of strings

**Required**: No (defaults to `[RIGHT_CLICK]`)

**Values**: See [Trigger Reference](trigger-reference.md)

**Example**:

```yaml
triggers:
  - RIGHT_CLICK
  - SHIFT_RIGHT_CLICK
```

---

### `conditions`

List of conditions that must be met.

**Type**: Array of objects

**Required**: No (defaults to no conditions)

**Logic**: AND (all must pass)

**Example**:

```yaml
conditions:
  - sneaking: true
  - health-above: 5.0
```

---

### `cooldown`

Cooldown duration before ability can be used again.

**Type**: String (duration format)

**Required**: No (defaults to no cooldown)

**Format**: `<number><unit>`

**Units**: `s` (seconds), `m` (minutes), `h` (hours), `d` (days)

**Examples**:

```yaml
cooldown: 3s
cooldown: 1.5m
cooldown: 2h
```

---

### `actions`

List of actions to execute when ability triggers.

**Type**: Array of objects

**Required**: Yes

**Execution**: Sequential (in order)

**Example**:

```yaml
actions:
  - type: LAUNCH_PROJECTILE
    projectile: FIREBALL
  - type: SEND_MESSAGE
    message: "&cFired!"
```

---

## Condition Types

### `sneaking`

**Type**: Boolean

**Values**: `true`, `false`

```yaml
- sneaking: true
```

### `health-above`

**Type**: Number

**Unit**: Hearts (2.0 = 1 heart)

```yaml
- health-above: 5.0
```

### `health-below`

**Type**: Number

```yaml
- health-below: 15.0
```

### `y-above`

**Type**: Number

```yaml
- y-above: 64.0
```

### `y-below`

**Type**: Number

```yaml
- y-below: 100.0
```

### `has-target`

**Type**: Boolean

```yaml
- has-target: true
```

---

## Complete Example

```yaml
abilities:
  # Fireball ability
  fireball:
    display-name: "&c&lFireball"
    triggers:
      - RIGHT_CLICK
      - SHIFT_RIGHT_CLICK
    conditions:
      - sneaking: true
      - health-above: 5.0
      - y-above: 0
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
      - type: PLAY_EFFECT
        particle: FLAME
        count: 10
  
  # Healing ability
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
        message: "&aHealed!"
      - type: PLAY_EFFECT
        particle: HEART
        count: 10
```

---

## Validation

AbilityEngine validates YAML on load:

- **Syntax errors**: Logged to console
- **Missing required fields**: Ability skipped
- **Invalid values**: Defaults used or ability skipped
- **Unknown action types**: Action skipped

---

## See Also

- [YAML Abilities Guide](../guides/yaml-abilities.md) - Complete guide
- [Action Types](actions.md) - All action types
- [Trigger Reference](trigger-reference.md) - All triggers
