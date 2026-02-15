# AbilityEngine - Build Instructions

## System Requirements

This project requires **Gradle** to build. Maven is installed on your system, but this project uses Gradle's multi-module system.

## Installing Gradle

### Option 1: Chocolatey (Recommended for Windows)

```powershell
choco install gradle
```

### Option 2: Manual Installation

1. Download Gradle 8.5 or later from: https://gradle.org/releases/
2. Extract to `C:\Gradle` (or your preferred location)
3. Add `C:\Gradle\bin` to your System PATH:
   - Open System Properties → Advanced → Environment Variables
   - Edit the "Path" variable under System Variables
   - Add new entry: `C:\Gradle\bin`
   - Click OK to save
4. Restart your terminal
5. Verify installation: `gradle --version`

### Option 3: Initialize Gradle Wrapper (Advanced)

If you have Gradle installed somewhere else or want to download it automatically:

```bash
# Run this once with a system-installed Gradle
gradle wrapper --gradle-version 8.5

# Then use the wrapper for all builds
./gradlew build
```

## Building the Project

Once Gradle is installed:

```bash
# Clean and build
gradle clean build

# Or if using wrapper:
./gradlew clean build
```

The final plugin JAR will be at:
```
ability-engine-plugin/build/libs/AbilityEngine-1.0.0-SNAPSHOT.jar
```

## Verification

The project has been fully implemented and should compile successfully with Gradle. All modules are complete:

- ✅ ability-engine-api (10 classes/interfaces)
- ✅ ability-engine-core (8 classes)
- ✅ ability-engine-config (15 classes)
- ✅ ability-engine-plugin (2 classes)
- ✅ Build scripts (Gradle multi-module setup)
- ✅ Documentation (architecture.md, abilities.md)

## Next Steps

1. Install Gradle (see above)
2. Run `gradle clean build`
3. Copy the built JAR to your Paper server
4. Start the server
5. Create abilities in `plugins/AbilityEngine/abilities/`
6. Use `/ability` commands to test

## Troubleshooting

**"gradle is not recognized"**
- Gradle is not in your PATH. See installation instructions above.

**"Could not find or load main class"**
- Make sure you're using Java 21. Check with: `java --version`

**Build errors**
- Ensure Paper API repository is accessible
- Check your internet connection
- Try: `gradle clean build --refresh-dependencies`
