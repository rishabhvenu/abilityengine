# Frequently Asked Questions

Common questions about AbilityEngine.

---

## General Questions

### What Minecraft versions are supported?

AbilityEngine requires **Paper 1.21 or later**. It will not work on Spigot or CraftBukkit due to Paper-specific API usage.

### Can I use this on Spigot?

No. AbilityEngine uses Paper-specific APIs that are not available in Spigot. You must use Paper or Paper-based forks (Purpur, etc.).

### How many abilities can I create?

There's no hard limit. The system is designed to handle thousands of abilities with minimal performance impact due to O(1) lookups.

### Can I have multiple abilities on one item?

Yes! Items can have multiple abilities, each bound to different triggers. For example, one item could have:
- RIGHT_CLICK → Fireball
- SHIFT_RIGHT_CLICK → Shield
- LEFT_CLICK → Dash

---

## YAML Abilities

### Why isn't my YAML ability loading?

Check these common issues:

1. **File location**: Must be in `plugins/AbilityEngine/abilities/`
2. **File extension**: Must be `.yml` or `.yaml`
3. **YAML syntax**: Use spaces (not tabs) for indentation
4. **Required fields**: Must have `display-name`, `triggers`, and `actions`
5. **Console errors**: Check server logs for specific errors

### Can I use custom materials for ability items?

By default, ability items use STICK. To use custom materials, you'll need to create items programmatically via JavaScript or Java.

### How do I make an ability work only in specific worlds?

Use a custom condition in JavaScript:

```javascript
engine.ability({
  id: "world_specific",
  conditions: [
    engine.condition.custom(function(ctx) {
      return ctx.player.getWorld().getName() === "world";
    })
  ],
  execute: function(ctx) {
    // ...
  }
});
```

### Can I reload abilities without restarting?

Yes! Use `/ability reload` to reload YAML abilities and scripts without restarting the server.

---

## JavaScript Scripting

### Do I need to know JavaScript to use AbilityEngine?

No! YAML abilities work great for most use cases. JavaScript is only needed for complex custom logic.

### Can scripts access the file system?

Yes. Scripts have unrestricted access (no sandboxing). Only load scripts from trusted sources.

### Why did my script fail to load?

Common causes:

1. **Syntax errors**: Check console for JavaScript error messages
2. **File extension**: Must be `.js`
3. **File location**: Must be in `plugins/AbilityEngine/scripts/`
4. **Missing dependencies**: Ensure you're using valid Bukkit API calls

### Can I use npm packages?

No. GraalVM JavaScript doesn't support npm or Node.js modules. You can only use Java interop via `Java.type()`.

### How do I debug scripts?

Use `engine.log()`, `engine.warn()`, and `engine.error()` for logging. Check server console for output.

---

## Module Development

### What's the difference between AbilityProvider and AbilityModule?

- **AbilityProvider**: Simple interface, just provides abilities
- **AbilityModule**: Extends AbilityProvider, adds lifecycle hooks (`onEnable`/`onDisable`)

Use AbilityModule when you need to register listeners, schedule tasks, or manage resources.

### Do I need to include Paper API in my module JAR?

No. Mark Paper API and ability-engine-api as `compileOnly` dependencies. They're provided by the server.

### Can modules depend on other plugins?

Yes, but you'll need to ensure those plugins are loaded before your module. Use Bukkit's plugin dependency system.

### How do I distribute my module?

Build your JAR and distribute it. Users place it in `plugins/AbilityEngine/modules/` and restart the server.

---

## Performance

### Will this lag my server?

AbilityEngine is designed for performance:

- O(1) ability lookups
- Minimal event overhead (only processes ability items)
- Automatic cleanup
- Thread-safe concurrent collections

Most servers see negligible performance impact.

### Can I have too many active sessions?

Sessions are efficient, but hundreds of concurrent sessions may impact performance. Each session ticks 20 times per second.

**Best practices**:
- Set maximum durations
- Optimize tick logic
- Run expensive operations less frequently

### Does this work with other ability plugins?

Yes! AbilityEngine doesn't interfere with other plugins. However, PDC keys are unique to AbilityEngine.

---

## Items & Triggers

### Why isn't my ability item working?

Check:

1. **Holding the item**: Must hold the ability item when triggering
2. **Conditions**: All conditions must pass
3. **Cooldown**: Ability must not be on cooldown
4. **Trigger type**: Action must match configured trigger

### Can I change the item type?

Yes, via JavaScript or Java. Create custom items with `ItemStack` and add PDC data:

```javascript
const NamespacedKey = Java.type("org.bukkit.NamespacedKey");
const PersistentDataType = Java.type("org.bukkit.persistence.PersistentDataType");

var key = new NamespacedKey("ability_engine", "ability_id");
meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "ability_id");
```

### Can items have lore/enchantments?

Yes! The item creation is flexible. Add lore and enchantments via ItemMeta after creating the ability item.

### Do ability items persist through restarts?

Yes. PDC data is saved with the item and persists through:
- Server restarts
- Item drops/pickups
- Chest storage
- Player inventories

---

## Cooldowns

### Are cooldowns per-player or global?

Per-player. Each player has their own cooldown timers.

### Can I clear a player's cooldowns?

Yes, via API:

```java
cooldownManager.clearCooldowns(player.getUniqueId());
```

Or in JavaScript:

```javascript
// Clear specific cooldown
engine.cooldowns.set(player, "ability_id", 0);
```

### Do cooldowns persist through restarts?

No. Cooldowns are in-memory only and reset on server restart.

---

## Sessions

### What's the difference between abilities and sessions?

- **Abilities**: Instant effects that execute once
- **Sessions**: Continuous effects that run over time (tick-based)

Use sessions for grappling hooks, auras, channels, etc.

### Can a player have multiple sessions?

Yes! A player can have multiple sessions running simultaneously (e.g., speed boost + shield).

### Do sessions end when players disconnect?

Yes. Sessions are automatically ended and cleaned up when players disconnect.

---

## Compatibility

### Does this work with ProtocolLib?

Yes. AbilityEngine doesn't interact with ProtocolLib.

### Does this work with PlaceholderAPI?

Not directly, but you can integrate it in your custom scripts or modules using Java interop.

### Can I use this with custom items from other plugins?

Yes! You can add ability PDC data to items from other plugins via scripting or module code.

---

## Troubleshooting

### My abilities aren't registering

1. Check `/ability list` to see registered abilities
2. Check console for errors during plugin enable
3. Verify YAML syntax with an online validator
4. Ensure file is in correct location

### Ability triggers but nothing happens

1. Check conditions are passing
2. Check cooldown isn't active
3. Check console for execution errors
4. Add debug logging to your execute function

### Server crashes when using abilities

This usually indicates:

1. Invalid Bukkit API usage in scripts
2. Null pointer exceptions (missing null checks)
3. Infinite loops in session tick logic

Check server crash logs for stack traces.

---

## Migration & Compatibility

### Can I migrate from another ability plugin?

You'll need to:

1. Recreate abilities using YAML, scripts, or Java modules
2. Give players new ability items
3. There's no automatic migration tool

### How do I backup my abilities?

Backup these directories:

- `plugins/AbilityEngine/abilities/` - YAML files
- `plugins/AbilityEngine/scripts/` - JavaScript files
- `plugins/AbilityEngine/modules/` - External modules

---

## Advanced Topics

### Can I add custom conditions in YAML?

No. YAML supports built-in conditions only. For custom conditions, use JavaScript or Java modules.

### Can I create GUI-based abilities?

Yes! Use JavaScript with Bukkit's inventory API:

```javascript
const Bukkit = Java.type("org.bukkit.Bukkit");
const InventoryHolder = Java.type("org.bukkit.inventory.InventoryHolder");

engine.ability({
  id: "open_gui",
  execute: function(ctx) {
    var inv = Bukkit.createInventory(null, 27, "My GUI");
    // Add items...
    ctx.player.openInventory(inv);
  }
});
```

### Can abilities cost items/money?

Yes! Check and remove items in the execute function:

```javascript
execute: function(ctx) {
  var inv = ctx.player.getInventory();
  const Material = Java.type("org.bukkit.Material");
  
  if (inv.contains(Material.DIAMOND, 1)) {
    inv.removeItem(new ItemStack(Material.DIAMOND, 1));
    // Execute ability
  } else {
    ctx.player.sendMessage("§cNot enough diamonds!");
  }
}
```

### Can I integrate with economy plugins?

Yes, via Java interop. Example with Vault:

```javascript
const Bukkit = Java.type("org.bukkit.Bukkit");

var economy = Bukkit.getServer().getServicesManager()
  .getRegistration(Java.type("net.milkbowl.vault.economy.Economy")).getProvider();

if (economy.has(ctx.player, 100)) {
  economy.withdrawPlayer(ctx.player, 100);
  // Execute ability
}
```

---

## Getting Help

### Where can I get support?

1. Check this documentation thoroughly
2. Review example abilities and scripts
3. Check console logs for error messages
4. Open an issue on GitHub (if available)

### How do I report a bug?

Include:

1. Server version (Paper version)
2. AbilityEngine version
3. Full error message from console
4. Steps to reproduce
5. Relevant configuration files

---

## Contributing

### Can I contribute abilities?

Yes! Share your abilities:

1. YAML files can be shared directly
2. Scripts can be shared as `.js` files
3. Modules can be distributed as JARs

### Can I contribute to the plugin?

Check the project's GitHub repository for contribution guidelines.

---

## See Also

- [Troubleshooting Guide](troubleshooting.md) - Detailed troubleshooting
- [Best Practices](best-practices.md) - Tips and patterns
- [Quick Start](getting-started/quick-start.md) - Getting started guide
