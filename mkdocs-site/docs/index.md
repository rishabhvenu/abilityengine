# AbilityEngine

**A modular Minecraft plugin framework for Paper 1.21+ that enables creation of custom abilities through Java API, JavaScript scripting, or YAML configuration.**

---

## What is AbilityEngine?

AbilityEngine is a flexible, high-performance plugin framework that allows server owners, scripters, and developers to create custom abilities for Minecraft. Whether you're a server owner who wants to add abilities through simple YAML files, a scripter who prefers JavaScript, or a Java developer building reusable modules, AbilityEngine provides the tools you need.

## Key Features

### Multiple Creation Methods

- **YAML Configuration** - Simple config files for server owners
- **JavaScript Scripting** - Powered by GraalVM with hot reload support
- **Java Modules** - External JAR modules with full lifecycle management

### Powerful Trigger System

- **14 built-in triggers** including clicks, shift-clicks, entity interactions, combat, and movement
- **Event-based triggers** for advanced custom event listening
- **Multi-ability items** with different triggers on a single item

### Rich Condition System

- **Composable conditions** (sneaking, health, position, targets)
- **AND/OR/NOT logic** for complex condition chains
- **Custom conditions** via scripting or Java

### Performance & Safety

- **O(1) ability lookup** with concurrent data structures
- **Per-player cooldowns** with automatic cleanup
- **Thread-safe** throughout the codebase
- **Minimal event overhead** - only processes ability items

### Stateful Sessions

- **Continuous abilities** like grappling hooks, auras, channels
- **Tick-based execution** with automatic lifecycle management
- **Resource cleanup** on player quit or plugin disable

### Developer-Friendly API

- **Clean interfaces** with comprehensive Javadoc
- **Modular architecture** separating API, core, and implementations
- **ServiceLoader SPI** for external module discovery
- **Hot reload** for scripts during development

---

## Quick Example

### YAML Ability (Server Owners)

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
```

### JavaScript Ability (Scripters)

```javascript
engine.ability({
  id: "dash",
  triggers: ["SHIFT_RIGHT_CLICK"],
  cooldown: 5,
  execute: function(ctx) {
    var dir = ctx.player.getLocation().getDirection();
    ctx.player.setVelocity(dir.multiply(2.5));
    ctx.player.sendMessage("§bDashed!");
  }
});
```

### Java Module (Developers)

```java
public class MyAbilityModule implements AbilityModule {
    @Override
    public void onEnable(AbilityRegistry registry, 
                         CooldownManager cooldowns, 
                         AbilityItemService items) {
        // Register abilities, listeners, tasks
    }
    
    @Override
    public Collection<Ability> getAbilities() {
        return List.of(new CustomAbility());
    }
    
    @Override
    public String getProviderId() {
        return "my-abilities";
    }
}
```

---

## Getting Started

<div class="grid cards" markdown>

-   :material-download:{ .lg .middle } __Installation__

    ---

    Install AbilityEngine on your Paper 1.21+ server

    [:octicons-arrow-right-24: Installation Guide](getting-started/installation.md)

-   :material-rocket-launch:{ .lg .middle } __Quick Start__

    ---

    Create your first ability in 5 minutes

    [:octicons-arrow-right-24: Quick Start Tutorial](getting-started/quick-start.md)

-   :material-file-document:{ .lg .middle } __YAML Abilities__

    ---

    Create abilities with YAML configuration

    [:octicons-arrow-right-24: YAML Guide](guides/yaml-abilities.md)

-   :material-language-javascript:{ .lg .middle } __JavaScript Scripting__

    ---

    Write abilities with JavaScript and hot reload

    [:octicons-arrow-right-24: Scripting Guide](guides/scripting.md)

-   :material-code-braces:{ .lg .middle } __Module Development__

    ---

    Build reusable JAR modules with Java

    [:octicons-arrow-right-24: Module Guide](guides/module-development.md)

-   :material-api:{ .lg .middle } __API Reference__

    ---

    Complete API documentation for developers

    [:octicons-arrow-right-24: API Reference](reference/api/ability.md)

</div>

---

## Use Cases

### Server Owners
Create custom abilities through YAML configuration without writing code. Perfect for adding unique gameplay mechanics to your server.

### Scripters & Power Users
Leverage JavaScript scripting for rapid prototyping and server-specific customization. Hot reload scripts without restarting the server.

### Plugin Developers
Build reusable ability modules in Java with type safety and full IDE support. Distribute modules as JARs that work on any AbilityEngine server.

---

## Architecture

AbilityEngine uses a clean modular architecture:

- **ability-engine-api** - Public API with all interfaces and types
- **ability-engine-core** - Internal implementation with registry, triggers, sessions
- **ability-engine-config** - YAML ability loader with 11+ built-in actions
- **ability-engine-script** - GraalVM JavaScript scripting engine
- **ability-engine-module-loader** - External JAR module loading via ServiceLoader
- **ability-engine-plugin** - Paper plugin entrypoint

All three ability sources (YAML, scripts, modules) register into the same unified registry.

[:octicons-arrow-right-24: Learn more about the architecture](architecture/overview.md)

---

## Commands

- `/ability give <player> <ability>` - Give an ability item
- `/ability reload` - Reload abilities and scripts
- `/ability list` - List all registered abilities
- `/ability info <ability>` - Show ability details
- `/ability script reload [filename]` - Reload scripts
- `/ability script list` - List loaded scripts
- `/ability module list` - List external modules

[:octicons-arrow-right-24: Full command reference](reference/commands.md)

---

## Support & Contributing

For issues, questions, or feature requests, please open an issue on the GitHub repository.

---

## License

[Add your license information here]
