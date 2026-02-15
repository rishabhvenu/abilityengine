# Installation

This guide covers installing AbilityEngine on your Paper server.

---

## Prerequisites

### Server Requirements

- **Minecraft Version**: Paper 1.21 or later
- **Java Version**: Java 21 or later

!!! info "Paper Required"
    AbilityEngine requires Paper API and will not work on Spigot or CraftBukkit. Download Paper from [papermc.io](https://papermc.io/downloads).

### Build Requirements (For Building from Source)

If you're building from source:

- **Java JDK**: 21 or later
- **Gradle**: 8.5 or later (or use the included Gradle wrapper)

---

## Installation Steps

### Option 1: Pre-built JAR (Recommended)

1. Download the latest `AbilityEngine-X.X.X-SNAPSHOT.jar` from releases
2. Copy the JAR file to your server's `plugins/` folder
3. Start or restart your server
4. The plugin will create the following directory structure:

```
plugins/AbilityEngine/
├── abilities/          # YAML ability files
├── scripts/            # JavaScript files
├── modules/            # External JAR modules
└── config.yml          # Plugin configuration (if needed)
```

### Option 2: Build from Source

#### 1. Install Gradle

=== "Windows (Chocolatey)"

    ```powershell
    choco install gradle
    ```

=== "Windows (Manual)"

    1. Download Gradle 8.5+ from [gradle.org/releases](https://gradle.org/releases/)
    2. Extract to `C:\Gradle` (or your preferred location)
    3. Add `C:\Gradle\bin` to your System PATH
    4. Verify: `gradle --version`

=== "Linux/Mac"

    ```bash
    # Using SDKMAN (recommended)
    curl -s "https://get.sdkman.io" | bash
    sdk install gradle 8.5
    
    # Or download manually from gradle.org
    ```

=== "Using Gradle Wrapper (No Installation)"

    If Gradle is not installed, you can use the included wrapper:
    
    ```bash
    # Unix/Mac
    ./gradlew build
    
    # Windows
    gradlew.bat build
    ```

#### 2. Clone and Build

```bash
# Clone the repository
git clone https://github.com/yourusername/abilityengine.git
cd abilityengine

# Build the plugin
gradle clean build

# Or with wrapper
./gradlew clean build
```

#### 3. Locate the JAR

The built plugin will be at:

```
ability-engine-plugin/build/libs/AbilityEngine-1.0.0-SNAPSHOT.jar
```

#### 4. Install on Server

```bash
# Copy to your Paper server
cp ability-engine-plugin/build/libs/AbilityEngine-1.0.0-SNAPSHOT.jar /path/to/server/plugins/
```

---

## Verifying Installation

1. Start your Paper server
2. Check the console for a startup message from AbilityEngine
3. Run `/ability list` to verify the plugin is loaded
4. Check that the `plugins/AbilityEngine/` directory was created

Example startup output:

```
[AbilityEngine] Enabling AbilityEngine v1.0.0-SNAPSHOT
[AbilityEngine] Loaded 3 abilities from YAML
[AbilityEngine] Loaded 2 scripts
[AbilityEngine] Loaded 0 external modules
[AbilityEngine] AbilityEngine enabled successfully
```

---

## Directory Structure

After installation, AbilityEngine creates the following structure:

```
plugins/AbilityEngine/
├── abilities/                    # YAML ability configurations
│   └── example-abilities.yml     # Example file (auto-generated)
├── scripts/                      # JavaScript files
│   └── examples/                 # Example scripts (optional)
│       ├── basic-abilities.js
│       ├── java-interop.js
│       ├── event-listeners.js
│       └── advanced-sessions.js
└── modules/                      # External JAR modules (empty by default)
```

### Abilities Directory

Place YAML ability configuration files here. Files must end with `.yml` or `.yaml`.

**Example**: `plugins/AbilityEngine/abilities/my-abilities.yml`

### Scripts Directory

Place JavaScript files here. All `.js` files are automatically loaded on server start.

**Example**: `plugins/AbilityEngine/scripts/custom-abilities.js`

### Modules Directory

Place external JAR module files here. Modules must implement the `AbilityProvider` or `AbilityModule` interface.

**Example**: `plugins/AbilityEngine/modules/my-ability-pack-1.0.0.jar`

---

## Permissions

All commands default to `op` permission level. Available permissions:

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

## Troubleshooting

### Plugin not loading

**Problem**: AbilityEngine doesn't appear in `/plugins` list

**Solutions**:

- Verify you're using Paper 1.21+ (not Spigot or CraftBukkit)
- Verify Java 21+ is installed: `java --version`
- Check server logs for error messages
- Ensure the JAR file is not corrupted (re-download or rebuild)

### Build errors

**Problem**: `gradle build` fails

**Solutions**:

- Ensure Java 21+ is installed: `java --version`
- Ensure Gradle 8.5+ is installed: `gradle --version`
- Clear Gradle cache: `gradle clean --refresh-dependencies`
- Check internet connection (Gradle needs to download dependencies)

### "Could not find Paper API" error

**Problem**: Build fails with Paper dependency error

**Solutions**:

- Verify internet connection (Paper API is downloaded from `repo.papermc.io`)
- Try: `gradle clean build --refresh-dependencies`
- Check if Paper Maven repository is accessible

---

## Next Steps

Now that AbilityEngine is installed:

- [Quick Start Tutorial](quick-start.md) - Create your first ability in 5 minutes
- [YAML Abilities Guide](../guides/yaml-abilities.md) - Learn the YAML syntax
- [JavaScript Scripting Guide](../guides/scripting.md) - Write abilities with JavaScript
- [Commands Reference](../reference/commands.md) - Full command documentation

---

## Updating AbilityEngine

To update to a new version:

1. Stop your server
2. Replace the old JAR in `plugins/` with the new version
3. Start your server
4. Run `/ability reload` to reload abilities and scripts

!!! warning "Backup First"
    Always backup your `plugins/AbilityEngine/` directory before updating, especially your custom abilities, scripts, and modules.
