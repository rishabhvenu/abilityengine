# Commands & Permissions

Complete reference for all commands and permissions.

---

## Commands

### /ability give <player> <ability>

Gives an ability item to a player.

**Usage**: `/ability give <player> <ability_id>`

**Permission**: `abilityengine.command.give`

**Examples**:

```
/ability give Steve fireball
/ability give @a dash
/ability give @p healing_touch
```

---

### /ability reload

Reloads all abilities (YAML + scripts).

**Usage**: `/ability reload`

**Permission**: `abilityengine.command.reload`

**What it reloads**:

- YAML ability files
- JavaScript scripts
- Config abilities

**Note**: Does not reload external JAR modules (requires restart)

---

### /ability list

Lists all registered abilities.

**Usage**: `/ability list`

**Permission**: `abilityengine.command.list`

**Output**:

```
Registered abilities:
- fireball
- heal
- dash
(Total: 3)
```

---

### /ability info <ability>

Shows detailed information about an ability.

**Usage**: `/ability info <ability_id>`

**Permission**: `abilityengine.command.info`

**Example**:

```
/ability info fireball
```

**Output**:

```
Ability: fireball
Triggers: RIGHT_CLICK
Cooldown: 3 seconds
Conditions: sneaking
```

---

### /ability script reload [filename]

Reloads JavaScript scripts.

**Usage**:

- `/ability script reload` - Reloads all scripts
- `/ability script reload <filename>` - Reloads specific script

**Permission**: `abilityengine.command.script`

**Examples**:

```
/ability script reload
/ability script reload custom-abilities.js
```

---

### /ability script list

Lists all loaded JavaScript scripts.

**Usage**: `/ability script list`

**Permission**: `abilityengine.command.script`

**Output**:

```
Loaded scripts:
- basic-abilities.js (2 abilities)
- custom.js (5 abilities)
(Total: 2 scripts, 7 abilities)
```

---

### /ability module list

Lists all loaded external modules.

**Usage**: `/ability module list`

**Permission**: `abilityengine.command.module`

**Output**:

```
Loaded modules:
- my-abilities v1.0.0 (5 abilities)
- combat-pack v2.1.0 (8 abilities)
(Total: 2 modules, 13 abilities)
```

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `abilityengine.command` | Access to all commands | op |
| `abilityengine.command.give` | Give ability items | op |
| `abilityengine.command.reload` | Reload abilities and scripts | op |
| `abilityengine.command.list` | List abilities | op |
| `abilityengine.command.info` | View ability info | op |
| `abilityengine.command.script` | Manage scripts | op |
| `abilityengine.command.module` | View modules | op |

---

## Tab Completion

All commands support tab completion:

- `/ability <tab>` - Lists subcommands
- `/ability give <tab>` - Lists online players
- `/ability give <player> <tab>` - Lists ability IDs
- `/ability info <tab>` - Lists ability IDs
- `/ability script reload <tab>` - Lists script filenames

---

## Permission Configuration

### Using LuckPerms

```
luckperms user <username> permission set abilityengine.command.give true
luckperms group admin permission set abilityengine.command true
```

### Using Permission Plugin

```yaml
groups:
  admin:
    permissions:
      - abilityengine.command
  moderator:
    permissions:
      - abilityengine.command.give
      - abilityengine.command.reload
```

---

## Command Aliases

Currently, no aliases are configured. To add aliases, edit `plugin.yml`:

```yaml
commands:
  ability:
    aliases: [ab, abilities]
```

---

## See Also

- [Quick Start](../getting-started/quick-start.md) - Using commands
- [YAML Guide](../guides/yaml-abilities.md) - Creating abilities
- [Scripting Guide](../guides/scripting.md) - Script hot reload
