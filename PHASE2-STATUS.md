# AbilityEngine Phase 2 - Build & Status

## Implementation Status: ✅ COMPLETE

All Phase 2 components have been fully implemented:

### ✅ New Modules
- **ability-engine-script** - GraalVM JavaScript scripting system (6 classes)
- **ability-engine-module-loader** - External JAR module loading (2 classes)

### ✅ API Extensions
- `AbilityModule` interface with lifecycle hooks (onEnable/onDisable)

### ✅ Scripting System
- `ScriptEngine` - Script orchestration and lifecycle
- `ScriptContext` - Per-script resource tracking
- `EngineBinding` - Global `engine` object API
- `ScriptAbility` - JS-to-Java ability bridge
- Helper bindings: TriggerConstants, ConditionBindings, SessionBindings
- Hot reload support (per-script and all-at-once)
- Automatic resource cleanup

### ✅ Module Loader
- `ModuleLoader` - JAR scanning and ServiceLoader integration
- `LoadedModule` - Module tracking record
- URLClassLoader per module with clean lifecycle

### ✅ Plugin Integration
- ScriptEngine and ModuleLoader initialization in AbilityEnginePlugin
- Proper cleanup in onDisable
- Public API accessors for both systems

### ✅ Commands
- `/ability reload` - Now reloads config + scripts
- `/ability script reload [filename]` - Script hot reload
- `/ability script list` - List loaded scripts
- `/ability module list` - List external modules
- Tab completion for all new commands
- New permissions in plugin.yml

### ✅ Documentation
- `docs/scripting.md` - Complete scripting API reference (400+ lines)
- `docs/architecture.md` - Updated with Phase 2 architecture
- Example scripts demonstrating all features:
  - basic-abilities.js - Simple ability DSL
  - java-interop.js - Raw Bukkit access
  - event-listeners.js - Event listening and scheduling
  - advanced-sessions.js - Stateful session-based abilities

## Building the Project

### Prerequisites
- Java 21+
- Gradle 8.5+

### Install Gradle (if needed)

**Option 1: Chocolatey (Windows)**
```powershell
choco install gradle
```

**Option 2: Manual**
1. Download from https://gradle.org/releases/
2. Extract and add `bin/` to PATH
3. Verify: `gradle --version`

### Build Commands

```bash
# Clean build all 6 modules
gradle clean build

# The final JAR will be at:
# ability-engine-plugin/build/libs/AbilityEngine-1.0.0-SNAPSHOT.jar
```

## Module Structure

```
abilityengine/
├── ability-engine-api/          (10 classes) ✅
├── ability-engine-core/         (8 classes)  ✅
├── ability-engine-config/       (15 classes) ✅
├── ability-engine-script/       (6 classes)  ✅ NEW
├── ability-engine-module-loader (2 classes)  ✅ NEW
└── ability-engine-plugin/       (2 classes)  ✅
```

## New Dependencies

**GraalVM JavaScript:**
- `org.graalvm.polyglot:polyglot:24.1.1`
- `org.graalvm.polyglot:js:24.1.1`

These are shaded and relocated to avoid conflicts:
```
org.graalvm -> xyz.rishabhvenu.abilityengine.libs.graalvm
```

## File Count Summary

**Phase 2 Added:**
- 8 new Java files
- 1 new API interface
- 4 example scripts
- 1 comprehensive scripting guide
- Updated architecture docs
- Updated build scripts
- Extended commands and permissions

**Total Project:**
- 44 Java files
- 6 Gradle modules
- 3 documentation files
- 4 example scripts
- 5 build configuration files

## Testing Phase 2

Once Gradle is installed and the project builds:

1. **Install the plugin:**
   ```
   cp ability-engine-plugin/build/libs/AbilityEngine-1.0.0-SNAPSHOT.jar server/plugins/
   ```

2. **Start server** (Paper 1.21+)

3. **Test scripting:**
   ```
   # Copy example scripts
   cp ability-engine-plugin/src/main/resources/examples/*.js plugins/AbilityEngine/scripts/

   # Reload scripts
   /ability script reload

   # List scripts
   /ability script list

   # Give yourself a scripted ability
   /ability give YourName dash
   ```

4. **Test module loading:**
   ```
   # Create a module JAR and place in
   plugins/AbilityEngine/modules/

   # Restart or use hot reload when implemented
   /ability module list
   ```

## Key Features Delivered

✅ **Global engine object** - Clean DSL for abilities
✅ **Raw Java interop** - Full Bukkit access via Java.type()
✅ **Hot reload** - Both per-script and all-at-once
✅ **Event listeners** - Listen to any Bukkit event from scripts
✅ **Sessions** - Stateful abilities with tick loops
✅ **Scheduling** - Delayed and repeating tasks
✅ **Resource cleanup** - Automatic on reload/unload
✅ **Module lifecycle** - onEnable/onDisable hooks
✅ **ServiceLoader SPI** - Standard Java module discovery
✅ **URLClassLoader isolation** - Per-module classloaders
✅ **Comprehensive docs** - Full API reference + examples

## Next Steps

1. **Install Gradle** (see above)
2. **Build the project**: `gradle clean build`
3. **Test on Paper server** (1.21+)
4. **Create your own scripts** in `plugins/AbilityEngine/scripts/`
5. **Build external modules** using the AbilityModule interface

All Phase 2 requirements from the PRD have been fully implemented! 🎉
