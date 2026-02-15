# AbilityEngine

A modular Minecraft plugin framework for Paper 1.21+ that enables creation of custom abilities through Java API, JavaScript scripting, or YAML configuration.

## Features

- **Multiple Creation Methods**: Java API, JavaScript (Phase 2), or YAML configuration
- **Modular Architecture**: Clean separation between API, core, config, and plugin modules
- **Rich Trigger System**: Support for clicks, shift+clicks, entity interactions, combat, movement
- **Flexible Conditions**: Composable conditions (sneaking, health, position, targets)
- **Per-Player Cooldowns**: Thread-safe cooldown management
- **Stateful Sessions**: Support for continuous abilities (grappling hooks, channels, auras)
- **Multi-Ability Items**: Bind multiple abilities to different triggers on a single item
- **Built-in Actions**: 11+ config-driven actions (projectiles, effects, commands, etc.)
- **Event-Based Triggers**: Advanced path for custom event listening

## Project Structure

```
abilityengine/
├── ability-engine-api/          # Public API
├── ability-engine-core/         # Core implementation
├── ability-engine-config/       # YAML ability loader
├── ability-engine-plugin/       # Paper plugin entrypoint
└── docs/                        # Documentation
    ├── architecture.md          # Architecture overview
    └── abilities.md             # YAML ability guide
```

## Building

### Prerequisites

- Java 21+
- Gradle 8.5+

### Build Commands

```bash
# Clean and build all modules
./gradlew clean build

# Build only
./gradlew build

# Build and skip tests
./gradlew build -x test
```

The final plugin JAR will be located at:
```
ability-engine-plugin/build/libs/AbilityEngine-1.0.0-SNAPSHOT.jar
```

### Installing Gradle (Windows)

If Gradle is not installed:

**Option 1: Using Chocolatey**
```powershell
choco install gradle
```

**Option 2: Manual Installation**
1. Download Gradle 8.5+ from https://gradle.org/releases/
2. Extract to a folder (e.g., `C:\Gradle`)
3. Add `C:\Gradle\bin` to your PATH environment variable
4. Verify: `gradle --version`

**Option 3: Use Gradle Wrapper (Recommended)**

Initialize the Gradle wrapper:
```bash
gradle wrapper --gradle-version 8.5
```

Then use `./gradlew` instead of `gradle` for all commands.

## Installation

1. Build the plugin (see above)
2. Copy `AbilityEngine-1.0.0-SNAPSHOT.jar` to your Paper server's `plugins/` folder
3. Start or reload your server
4. Abilities directory will be created at `plugins/AbilityEngine/abilities/`

## Quick Start

### Creating Your First Ability

1. Create `plugins/AbilityEngine/abilities/fireball.yml`:

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

2. Reload the plugin: `/ability reload`
3. Give yourself the item: `/ability give <your_name> fireball`
4. Right-click to use!

## Commands

- `/ability give <player> <ability>` - Give an ability item
- `/ability reload` - Reload ability configurations
- `/ability list` - List all registered abilities
- `/ability info <ability>` - Show ability details

## Permissions

- `abilityengine.command` - Access to all commands (default: op)
- `abilityengine.command.give` - Give ability items (default: op)
- `abilityengine.command.reload` - Reload abilities (default: op)
- `abilityengine.command.list` - List abilities (default: op)
- `abilityengine.command.info` - View ability info (default: op)

## Documentation

- [Architecture Guide](docs/architecture.md) - Technical architecture and design
- [Ability YAML Guide](docs/abilities.md) - Complete YAML reference with examples

## Module Overview

### ability-engine-api

Public API module for external integration. Contains:
- Core interfaces (`Ability`, `AbilityRegistry`, `CooldownManager`)
- Data types (`AbilityContext`, `TriggerType`, `Condition`)
- Service interfaces (`AbilityItemService`, `AbilitySession`)

### ability-engine-core

Internal implementation:
- Registry and cooldown management
- Trigger dispatcher and condition evaluator
- Session manager with tick loop
- PDC-based item service
- Event trigger registry (advanced)

### ability-engine-config

YAML-based ability system:
- Config loader with validation
- 11+ built-in action types
- Duration parsing
- Condition mapping

### ability-engine-plugin

Paper plugin entrypoint:
- Plugin lifecycle management
- Command handlers
- Component wiring

## Performance

- **O(1) ability lookup** via ConcurrentHashMap
- **Minimal event overhead** - only processes ability items
- **Automatic cleanup** - expired cooldowns and ended sessions
- **Thread-safe** - concurrent data structures throughout

## Development Roadmap

### Phase 1 (Current)
- ✅ Core framework
- ✅ YAML configuration
- ✅ Built-in actions
- ✅ Multi-ability items
- ✅ Session system

### Phase 2 (Planned)
- ⬜ GraalVM JavaScript scripting
- ⬜ Hot reload for scripts
- ⬜ External JAR module loading
- ⬜ AbilityProvider SPI
- ⬜ Full Bukkit event access for scripts

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]

## Support

For issues, questions, or feature requests, please [open an issue](link-to-your-repo/issues).
