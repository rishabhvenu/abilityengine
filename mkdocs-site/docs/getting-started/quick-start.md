# Quick Start

Create your first ability in 5 minutes.

---

## What You'll Build

A simple **Fireball** ability that:

- Triggers when you right-click while sneaking
- Launches a fireball projectile
- Has a 3-second cooldown
- Plays a sound and sends a message

---

## Step 1: Create the Ability File

Navigate to your server's AbilityEngine abilities folder:

```
plugins/AbilityEngine/abilities/
```

Create a new file called `fireball.yml` with the following content:

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
        volume: 1.0
        pitch: 1.0
```

!!! tip "YAML Syntax"
    Pay attention to indentation! YAML uses spaces (not tabs) for indentation. Each level should be indented by 2 spaces.

---

## Step 2: Reload the Plugin

In-game or from the console, run:

```
/ability reload
```

You should see a confirmation message that abilities were reloaded.

---

## Step 3: Get the Ability Item

Give yourself the fireball ability item:

```
/ability give <your_username> fireball
```

A stick with the name "Fireball" should appear in your inventory.

!!! info "Item Type"
    By default, ability items are sticks. The item type is determined by the ability's configuration or can be customized.

---

## Step 4: Use the Ability

1. Hold the Fireball item in your hand
2. **Sneak** (hold Shift)
3. **Right-click**
4. A fireball should launch in the direction you're facing!

You should also:

- See the message: "Fireball launched!"
- Hear the blaze shoot sound
- Need to wait 3 seconds before using it again

---

## Understanding the Configuration

Let's break down what each part does:

### Ability ID

```yaml
fireball:
```

The unique identifier for this ability. Used in commands and internally.

### Display Name

```yaml
display-name: "&cFireball"
```

The name shown on the item. `&c` is a color code for red.

### Triggers

```yaml
triggers:
  - RIGHT_CLICK
```

When the ability activates. In this case, when you right-click.

### Conditions

```yaml
conditions:
  - sneaking: true
```

Requirements that must be met. Here, the player must be sneaking.

### Cooldown

```yaml
cooldown: 3s
```

Time before the ability can be used again. Format: `<number><unit>` where unit is `s` (seconds), `m` (minutes), `h` (hours), or `d` (days).

### Actions

```yaml
actions:
  - type: LAUNCH_PROJECTILE
    projectile: FIREBALL
    speed: 2.0
  - type: SEND_MESSAGE
    message: "&cFireball launched!"
  - type: PLAY_SOUND
    sound: ENTITY_BLAZE_SHOOT
```

What happens when the ability triggers. Multiple actions are executed in order.

---

## Next Steps

### Try More Triggers

Change the trigger to see different behaviors:

```yaml
triggers:
  - LEFT_CLICK              # Left-click
  - SHIFT_RIGHT_CLICK       # Shift + right-click
  - RIGHT_CLICK_ENTITY      # Right-click on an entity
  - DAMAGE_DEALT            # When you damage an entity
```

### Add More Conditions

Require specific conditions:

```yaml
conditions:
  - sneaking: true
  - health-above: 5.0       # Only works above 5 hearts
  - y-above: 64.0           # Only works above Y=64
```

### Combine Multiple Actions

Create more complex abilities:

```yaml
actions:
  - type: LAUNCH_PROJECTILE
    projectile: FIREBALL
    speed: 2.0
  - type: VELOCITY
    x: 0.0
    y: 1.0                  # Launch yourself upward
    z: 0.0
  - type: PLAY_EFFECT
    particle: FLAME
    count: 20
  - type: POTION_EFFECT
    effect: SPEED
    duration: 100           # 5 seconds (20 ticks = 1 second)
    amplifier: 1            # Speed II
```

---

## More Examples

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
        forward: 5.0          # Teleport 5 blocks forward
      - type: PLAY_EFFECT
        particle: CLOUD
        count: 20
```

### Lightning Strike (on entity)

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
```

---

## Learn More

You've created your first ability! Now dive deeper:

- **[YAML Abilities Guide](../guides/yaml-abilities.md)** - Full YAML reference with all actions and conditions
- **[Action Types Reference](../reference/actions.md)** - Complete list of all 11 action types
- **[Trigger Reference](../reference/trigger-reference.md)** - All available trigger types
- **[Commands](../reference/commands.md)** - All available commands

Want more power? Try scripting:

- **[JavaScript Scripting Guide](../guides/scripting.md)** - Write abilities with JavaScript

---

## Troubleshooting

### Ability not loading

**Check**:

1. YAML syntax is correct (proper indentation, no tabs)
2. File ends with `.yml` or `.yaml`
3. File is in `plugins/AbilityEngine/abilities/`
4. Run `/ability reload` after creating the file
5. Check server console for error messages

### Item not working

**Check**:

1. You're holding the ability item (check name and lore)
2. You meet all conditions (sneaking, health, etc.)
3. Ability is not on cooldown
4. Run `/ability info fireball` to see ability details

### "Unknown ability" error

**Check**:

1. Ability ID is correct (case-sensitive)
2. Ability was loaded successfully (check `/ability list`)
3. Run `/ability reload` to reload abilities

---

## Tips

!!! tip "Color Codes"
    Use `&` for legacy color codes: `&a` = green, `&c` = red, `&b` = aqua, `&e` = yellow, `&f` = white

!!! tip "Testing"
    Use `/ability reload` to test changes without restarting the server

!!! tip "Multiple Abilities"
    You can define multiple abilities in one YAML file or split them across multiple files

!!! tip "Comments"
    Use `#` for comments in YAML files:
    ```yaml
    # This is a comment
    cooldown: 3s  # Comment after a value
    ```
