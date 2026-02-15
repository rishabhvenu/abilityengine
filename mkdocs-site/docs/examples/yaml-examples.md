# YAML Ability Examples

Curated examples of YAML abilities from simple to advanced.

---

## Basic Abilities

### Simple Fireball

```yaml
abilities:
  fireball:
    display-name: "&cFireball"
    triggers:
      - RIGHT_CLICK
    cooldown: 3s
    actions:
      - type: LAUNCH_PROJECTILE
        projectile: FIREBALL
        speed: 2.0
      - type: SEND_MESSAGE
        message: "&cFireball launched!"
```

**Use case**: Basic projectile ability

---

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

**Use case**: Conditional healing ability

---

## Movement Abilities

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

**Use case**: Quick movement ability

---

### Super Jump

```yaml
abilities:
  super_jump:
    display-name: "&eSuper Jump"
    triggers:
      - SHIFT_LEFT_CLICK
    cooldown: 8s
    actions:
      - type: VELOCITY
        x: 0.0
        y: 2.0
        z: 0.0
      - type: PLAY_SOUND
        sound: ENTITY_ENDERMAN_TELEPORT
        pitch: 1.5
      - type: PLAY_EFFECT
        particle: EXPLOSION_LARGE
        count: 3
```

**Use case**: Vertical movement

---

## Combat Abilities

### Lightning Strike

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

**Use case**: Entity-targeted damage

---

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

**Use case**: Passive combat ability

---

### Counter Strike

```yaml
abilities:
  counter:
    display-name: "&6Counter Strike"
    triggers:
      - DAMAGE_TAKEN
    conditions:
      - health-above: 3.0
    cooldown: 10s
    actions:
      - type: TELEPORT
        forward: -5.0  # Teleport backwards
      - type: DAMAGE
        damage: 3.0
      - type: SEND_MESSAGE
        message: "&6Counter!"
```

**Use case**: Defensive counter-attack

---

## Utility Abilities

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
        duration: 200  # 10 seconds
        amplifier: 2   # Speed III
      - type: POTION_EFFECT
        effect: JUMP
        duration: 200
        amplifier: 1   # Jump II
      - type: SEND_MESSAGE
        message: "&eSpeed boost activated!"
      - type: PLAY_SOUND
        sound: ENTITY_PLAYER_LEVELUP
```

**Use case**: Buff ability

---

### Invisibility Cloak

```yaml
abilities:
  invis:
    display-name: "&7Invisibility"
    triggers:
      - SHIFT_RIGHT_CLICK
    cooldown: 60s
    actions:
      - type: POTION_EFFECT
        effect: INVISIBILITY
        duration: 200  # 10 seconds
        amplifier: 0
      - type: SEND_MESSAGE
        message: "&7You are now invisible!"
      - type: PLAY_EFFECT
        particle: SMOKE_LARGE
        count: 30
```

**Use case**: Stealth ability

---

## Advanced Combinations

### Meteor Rain

```yaml
abilities:
  meteor:
    display-name: "&6Meteor Rain"
    triggers:
      - RIGHT_CLICK
    conditions:
      - sneaking: true
      - y-above: 60.0
    cooldown: 30s
    actions:
      - type: COMMAND
        command: "execute at {player} run summon fireball ~ ~10 ~ {direction:[0.0,-1.0,0.0]}"
      - type: COMMAND
        command: "execute at {player} run summon fireball ~3 ~10 ~ {direction:[0.0,-1.0,0.0]}"
      - type: COMMAND
        command: "execute at {player} run summon fireball ~-3 ~10 ~ {direction:[0.0,-1.0,0.0]}"
      - type: PLAY_SOUND
        sound: ENTITY_WITHER_SHOOT
        volume: 2.0
      - type: SEND_MESSAGE
        message: "&6Meteor rain summoned!"
```

**Use case**: Complex AoE ability

---

### Berserker Mode

```yaml
abilities:
  berserker:
    display-name: "&4Berserker Mode"
    triggers:
      - RIGHT_CLICK
    conditions:
      - health-below: 10.0
    cooldown: 45s
    actions:
      - type: POTION_EFFECT
        effect: STRENGTH
        duration: 200  # 10 seconds
        amplifier: 2   # Strength III
      - type: POTION_EFFECT
        effect: SPEED
        duration: 200
        amplifier: 1
      - type: POTION_EFFECT
        effect: RESISTANCE
        duration: 200
        amplifier: 1
      - type: SEND_MESSAGE
        message: "&4BERSERKER MODE!"
      - type: PLAY_EFFECT
        particle: EXPLOSION_LARGE
        count: 10
      - type: PLAY_SOUND
        sound: ENTITY_ENDER_DRAGON_GROWL
```

**Use case**: Emergency power-up

---

### Teleport Home

```yaml
abilities:
  tp_home:
    display-name: "&bTeleport Home"
    triggers:
      - SHIFT_RIGHT_CLICK
    cooldown: 120s
    actions:
      - type: COMMAND
        command: "spawn {player}"
      - type: SEND_MESSAGE
        message: "&bTeleported to spawn!"
      - type: PLAY_EFFECT
        particle: PORTAL
        count: 50
      - type: PLAY_SOUND
        sound: ENTITY_ENDERMAN_TELEPORT
```

**Use case**: Utility teleport

---

## Multi-Ability Item Concept

While not directly configurable in YAML, here's how you'd conceptually design a multi-ability item:

```yaml
abilities:
  # Primary ability (RIGHT_CLICK)
  staff_fireball:
    display-name: "&cMagic Staff - Fireball"
    triggers:
      - RIGHT_CLICK
    cooldown: 3s
    actions:
      - type: LAUNCH_PROJECTILE
        projectile: FIREBALL
  
  # Secondary ability (SHIFT_RIGHT_CLICK)
  staff_heal:
    display-name: "&cMagic Staff - Heal"
    triggers:
      - SHIFT_RIGHT_CLICK
    cooldown: 10s
    actions:
      - type: HEAL
        amount: 5.0
      - type: PLAY_EFFECT
        particle: HEART
        count: 10
```

Then create a custom item that binds both abilities.

---

## Complete Ability Pack

```yaml
abilities:
  fireball:
    display-name: "&cFireball"
    triggers: [RIGHT_CLICK]
    cooldown: 3s
    actions:
      - type: LAUNCH_PROJECTILE
        projectile: FIREBALL
        speed: 2.0
      - type: SEND_MESSAGE
        message: "&cFireball!"
  
  heal:
    display-name: "&aHeal"
    triggers: [RIGHT_CLICK]
    conditions: [{health-below: 15.0}]
    cooldown: 10s
    actions:
      - type: HEAL
        amount: 5.0
      - type: PLAY_EFFECT
        particle: HEART
        count: 10
  
  dash:
    display-name: "&bDash"
    triggers: [SHIFT_RIGHT_CLICK]
    cooldown: 5s
    actions:
      - type: VELOCITY
        y: 0.5
      - type: TELEPORT
        forward: 5.0
  
  lightning:
    display-name: "&eLightning"
    triggers: [RIGHT_CLICK_ENTITY]
    conditions: [{has-target: true}]
    cooldown: 15s
    actions:
      - type: COMMAND
        command: "execute at {player} run summon lightning_bolt"
  
  speed:
    display-name: "&eSpeed"
    triggers: [RIGHT_CLICK]
    cooldown: 30s
    actions:
      - type: POTION_EFFECT
        effect: SPEED
        duration: 200
        amplifier: 2
```

---

## See Also

- [YAML Abilities Guide](../guides/yaml-abilities.md) - Complete guide
- [Action Types](../reference/actions.md) - All action types
- [Script Examples](script-examples.md) - JavaScript examples
