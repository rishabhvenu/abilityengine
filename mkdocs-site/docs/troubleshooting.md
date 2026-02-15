# Troubleshooting Guide

Comprehensive troubleshooting for common issues.

---

## Installation Issues

### Plugin Not Loading

**Symptoms**: Plugin doesn't appear in `/plugins` list

**Causes & Solutions**:

1. **Wrong server type**
   - Problem: Using Spigot or CraftBukkit
   - Solution: Use Paper 1.21+

2. **Wrong Java version**
   - Problem: Java version < 21
   - Solution: Update to Java 21 or later
   - Check: `java --version`

3. **Corrupted JAR**
   - Problem: Incomplete download or build
   - Solution: Re-download or rebuild
   - Verify: Check file size is > 1MB

4. **File permissions**
   - Problem: JAR file not readable
   - Solution: `chmod 644 AbilityEngine-*.jar` (Linux/Mac)

**Verification**:

```
[Server] [INFO] Enabling AbilityEngine v1.0.0-SNAPSHOT
```

---

### Build Failures

**Symptoms**: `gradle build` fails

**Common Errors**:

#### "Could not find Paper API"

```
Solution:
1. Check internet connection
2. gradle clean build --refresh-dependencies
3. Verify repo.papermc.io is accessible
```

#### "Java version mismatch"

```
Solution:
1. Ensure Java 21 toolchain is available
2. Set JAVA_HOME to JDK 21
3. gradle clean build
```

#### "Task failed with exception"

```
Solution:
1. Check for syntax errors in source files
2. gradle clean build --stacktrace
3. Review full error output
```

---

## YAML Ability Issues

### Abilities Not Loading

**Check Console Output**:

```
[AbilityEngine] Loading abilities from: plugins/AbilityEngine/abilities/
[AbilityEngine] Loaded X abilities from YAML
```

**Debugging Steps**:

1. **Verify file location**
   ```
   plugins/AbilityEngine/abilities/my-abilities.yml  ✓
   plugins/AbilityEngine/my-abilities.yml            ✗
   ```

2. **Check file extension**
   ```
   fireball.yml   ✓
   fireball.yaml  ✓
   fireball.txt   ✗
   ```

3. **Validate YAML syntax**
   - Use https://www.yamllint.com/
   - Check indentation (spaces, not tabs)
   - Verify colons after keys

4. **Check required fields**
   ```yaml
   abilities:
     ability_id:           # Required
       display-name: ""    # Required
       actions:            # Required
         - type: ACTION
   ```

**Common YAML Errors**:

```yaml
# ✗ WRONG - Missing colon
abilities
  fireball

# ✓ CORRECT
abilities:
  fireball:

# ✗ WRONG - Tabs instead of spaces
abilities:
	fireball:  # This is a tab!

# ✓ CORRECT - Spaces
abilities:
  fireball:  # Two spaces

# ✗ WRONG - Incorrect list syntax
triggers: RIGHT_CLICK

# ✓ CORRECT - Array syntax
triggers:
  - RIGHT_CLICK
```

---

### Ability Not Triggering

**Checklist**:

1. ✓ Holding correct item
2. ✓ Trigger matches action
3. ✓ Conditions pass
4. ✓ Not on cooldown

**Debug Process**:

```yaml
# Add debug messages
actions:
  - type: SEND_MESSAGE
    message: "&eDEBUG: Ability triggered!"
  - type: SEND_MESSAGE
    message: "&eDEBUG: Player health is {health}"
  # ... rest of actions
```

**Condition Debugging**:

```yaml
# Test without conditions first
conditions: []

# Add conditions one at a time
conditions:
  - sneaking: true

# Then add more
conditions:
  - sneaking: true
  - health-above: 5.0
```

**Trigger Verification**:

```yaml
# Test all triggers
triggers:
  - RIGHT_CLICK
  - LEFT_CLICK
  - SHIFT_RIGHT_CLICK
  - SHIFT_LEFT_CLICK
```

---

### Actions Not Executing

**Problem**: Ability triggers but actions don't work

**Check Each Action**:

1. **LAUNCH_PROJECTILE**
   - Verify projectile type is valid
   - Check: `/ability info <ability_id>`

2. **DAMAGE**
   - Requires target entity
   - Only works with entity-click triggers
   - Check: `has-target: true` condition

3. **TELEPORT**
   - Check coordinates are valid
   - Forward teleport requires facing direction
   - May fail if location is unsafe

4. **COMMAND**
   - Check command syntax
   - Test command manually in console
   - Verify {player} placeholder works

**Console Logs**:

```
[AbilityEngine] Error executing action DAMAGE: No target entity
```

---

## JavaScript Script Issues

### Scripts Not Loading

**Console Messages**:

```
[AbilityEngine] Loading scripts from: plugins/AbilityEngine/scripts/
[AbilityEngine] Loaded X scripts
```

**Debugging**:

1. **Check file location**
   ```
   plugins/AbilityEngine/scripts/my-script.js  ✓
   plugins/AbilityEngine/my-script.js          ✗
   ```

2. **Check file extension**
   ```
   ability.js   ✓
   ability.txt  ✗
   ```

3. **Check JavaScript syntax**
   ```javascript
   // ✗ WRONG - Missing semicolons, wrong quotes
   engine.ability({
     id: 'fireball'  // Single quotes may cause issues
     execute: function(ctx)  // Missing colon
   })

   // ✓ CORRECT
   engine.ability({
     id: "fireball",
     execute: function(ctx) {
       // ...
     }
   });
   ```

---

### Script Syntax Errors

**Common Errors**:

```javascript
// ✗ Missing commas
engine.ability({
  id: "test"
  triggers: ["RIGHT_CLICK"]  // Missing comma!
});

// ✓ Correct
engine.ability({
  id: "test",
  triggers: ["RIGHT_CLICK"]
});

// ✗ Undefined variables
execute: function(ctx) {
  player.sendMessage("Hi!");  // 'player' not defined!
}

// ✓ Correct
execute: function(ctx) {
  ctx.player.sendMessage("Hi!");
}

// ✗ Incorrect Java interop
const Player = "org.bukkit.entity.Player";  // String, not class!

// ✓ Correct
const Player = Java.type("org.bukkit.entity.Player");
```

---

### Script Runtime Errors

**Reading Stack Traces**:

```
[ERROR] Error in script ability 'fireball':
    at execute (my-script.js:15)
    ...
```

Line 15 is where the error occurred.

**Common Runtime Errors**:

1. **NullPointerException**
   ```javascript
   // ✗ No null check
   ctx.targetEntity.damage(5.0);
   
   // ✓ Null check
   if (ctx.targetEntity !== null) {
     ctx.targetEntity.damage(5.0);
   }
   ```

2. **Invalid Bukkit API call**
   ```javascript
   // ✗ Wrong method name
   ctx.player.giveItem(item);
   
   // ✓ Correct
   ctx.player.getInventory().addItem(item);
   ```

3. **Type mismatch**
   ```javascript
   // ✗ Wrong type
   ctx.player.setHealth("10");  // String, not number
   
   // ✓ Correct
   ctx.player.setHealth(10.0);
   ```

---

## Session Issues

### Sessions Not Ending

**Problem**: Session runs forever

**Cause**: Missing end condition

**Solution**:

```javascript
engine.sessions.start(player, ability, {
  onTick: function(tickCount) {
    // ✗ WRONG - No end condition
    damageNearby();
  }
});

// ✓ CORRECT - Always have an end condition
engine.sessions.start(player, ability, {
  onTick: function(tickCount) {
    if (tickCount > 200) {  // Max 10 seconds
      engine.sessions.end(player, "ability_id");
      return;
    }
    damageNearby();
  }
});
```

---

### Sessions Causing Lag

**Problem**: Server TPS drops when sessions are active

**Causes**:

1. **Too many sessions**
   - Check: How many concurrent sessions?
   - Solution: Limit session creation

2. **Expensive tick logic**
   ```javascript
   // ✗ BAD - Every tick (20 times/second)
   onTick: function(tickCount) {
     getAllPlayers().forEach(checkDistance);
   }
   
   // ✓ GOOD - Every second
   onTick: function(tickCount) {
     if (tickCount % 20 === 0) {
       getAllPlayers().forEach(checkDistance);
     }
   }
   ```

3. **Not cleaning up**
   ```javascript
   // ✗ BAD - Task keeps running
   onStart: function() {
     taskId = engine.scheduleRepeating(...);
   }
   
   // ✓ GOOD - Cancel in onEnd
   onEnd: function() {
     engine.cancelTask(taskId);
   }
   ```

---

## Module Issues

### Module Not Discovered

**Problem**: JAR in `modules/` but not loading

**Checklist**:

1. ✓ ServiceLoader file exists
   ```
   src/main/resources/META-INF/services/
     xyz.rishabhvenu.abilityengine.api.AbilityProvider
   ```

2. ✓ File contains correct class name
   ```
   com.example.MyModule
   ```

3. ✓ Class implements AbilityProvider
   ```java
   public class MyModule implements AbilityProvider
   ```

4. ✓ JAR is not corrupted
   - Check file size > 0
   - Try unzipping to verify

**Verification**:

```
/ability module list
```

Should show your module.

---

### Module ClassNotFoundException

**Problem**: Module loads but abilities fail

**Causes**:

1. **Missing dependencies**
   - Solution: Shade dependencies into JAR

2. **Wrong classloader**
   - Solution: Ensure parent classloader is correct

3. **Paper API not available**
   - Solution: Use `compileOnly` for Paper API

---

## Performance Issues

### High Memory Usage

**Check**:

1. Memory leaks in scripts
2. Too many registered abilities
3. Large session counts
4. Scheduled tasks not cancelled

**Solution**:

```javascript
// Always cancel tasks in onEnd
var taskId;
onStart: function() {
  taskId = engine.scheduleRepeating(...);
},
onEnd: function() {
  if (taskId) engine.cancelTask(taskId);
}
```

---

### Server Lag

**Profiling**:

1. Use Spark or similar profiler
2. Check for:
   - Expensive tick logic
   - Too many event listeners
   - Large ability counts with MOVE trigger

**Optimization**:

```javascript
// ✗ BAD - MOVE trigger without conditions
triggers: ["MOVE"]

// ✓ GOOD - Limited with conditions
triggers: ["MOVE"],
conditions: [
  engine.condition.sneaking(),
  engine.condition.holdingMaterial(Material.STICK)
]
```

---

## Common Error Messages

### "Unknown ability: X"

**Cause**: Ability not registered

**Solutions**:

1. Check `/ability list`
2. Reload: `/ability reload`
3. Verify YAML/script syntax
4. Check console for load errors

---

### "Not on cooldown but won't trigger"

**Cause**: Conditions not met

**Debug**:

```yaml
# Remove all conditions temporarily
conditions: []

# If works, add conditions back one by one
```

---

### "Cannot cast X to Y"

**Cause**: Type mismatch in scripts

**Solution**: Check Java types

```javascript
// ✗ WRONG
if (ctx.targetEntity instanceof "LivingEntity")

// ✓ CORRECT
const LivingEntity = Java.type("org.bukkit.entity.LivingEntity");
if (ctx.targetEntity instanceof LivingEntity)
```

---

## Getting More Help

### Information to Provide

When asking for help, include:

1. **Server info**:
   ```
   Paper version: 1.21-XXX
   Java version: 21
   AbilityEngine version: 1.0.0-SNAPSHOT
   ```

2. **Error message** (full, from console)

3. **Configuration** (YAML or script causing issue)

4. **Steps to reproduce**

5. **What you've tried**

### Useful Commands

```bash
# Check Paper version
version

# List abilities
ability list

# Check specific ability
ability info <ability_id>

# List scripts
ability script list

# List modules
ability module list

# Reload everything
ability reload
```

---

## See Also

- [FAQ](faq.md) - Common questions
- [Best Practices](best-practices.md) - Avoid common issues
- [Scripting Guide](guides/scripting.md) - JavaScript help
- [YAML Guide](guides/yaml-abilities.md) - YAML help
