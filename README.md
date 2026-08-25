# EIF-framework (Ember Injection Framework)

[![Build Status](https://github.com/xdddno/EIF-framework/actions/workflows/build.yml/badge.svg)](https://github.com/xdddno/EIF-framework/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-8-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html)
[![C++ Version](https://img.shields.io/badge/C++-20-blue.svg)](https://isocpp.org/)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.8.9-green.svg)](https://www.minecraft.net/)

**A high-performance Minecraft 1.8.9 bytecode injection framework for advanced modding and research.**

EIF-framework is a native DLL injection system that uses C++ to inject into Java processes, leveraging JNI/JVMTI for runtime class transformation and event interception. The payload JAR is loaded entirely from memory, never touching disk, providing a clean and efficient injection mechanism.

> **Note**: This framework is specifically designed for **Lunar Client 1.8.9**. While it may work with other Minecraft 1.8.9 distributions, it is optimized and tested for Lunar Client.

**[中文文档](README_zh.md) | English Documentation**

## Table of Contents

- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [Directory Structure](#directory-structure)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Building the Project](#building-the-project)
- [Usage](#usage)
- [Module System](#module-system)
- [Injection Modes](#injection-modes)
- [Event Hooks](#event-hooks)
- [GUI System](#gui-system)
- [Property System](#property-system)
- [Unloading](#unloading)
- [Namespace Remapping](#namespace-remapping)
- [Key Components](#key-components)
- [Contributing](#contributing)
- [License](#license)
- [Disclaimer](#disclaimer)
- [Acknowledgments](#acknowledgments)

## Features

### Core Features
- **Memory-Only Injection**: Payload JAR never touches disk, loaded entirely from byte arrays
- **Runtime Class Transformation**: ASM-based bytecode patching at class load time
- **Event-Driven Architecture**: 20+ event hooks for Minecraft core methods
- **Modular Design**: Plugin-based module system with lifecycle management
- **Clean Unload**: Safe removal with classloader chain restoration
- **Cross-Platform Foundation**: C++20 native layer with JNI/JVMTI integration

### Technical Features
- **Five Injection Modes**: ON_ENTRY, ON_RETURN_THROW, ON_LDC_CONSTANT, ON_REDIRECT, ON_MODIFY_VARIABLE
- **Annotation Processor**: Compile-time code generation for type-safe event handling
- **Namespace Remapping**: Automatic Mojang named → searge obfuscation mapping
- **Custom ClassLoader**: MemoryJarClassLoader for loading classes from byte arrays
- **JVMTI Integration**: ClassFileLoadHook for intercepting all class loading
- **MetaJNI2**: Advanced C++ JNI wrapper with TLS, template caching, and compile-time signatures

### User Features
- **Click GUI**: Draggable category panels with module toggles and property editors
- **Module Management**: Enable/disable modules with keybinds and tick-based updates
- **Property System**: Boolean, Int, Float, Percent, Text, Mode, and Color properties
- **Hot Reloading**: Press END key for clean unload without JVM restart
- **Logging System**: File-based logging for debugging and monitoring

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     EIF-framework Architecture                  │
├─────────────────────────────────────────────────────────────────┤
│  Native Layer (C++)                                             │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ JarLoader.dll                                               ││
│  │ ├─ DllMain → mainThread → app()                            ││
│  │ ├─ Attach to JVM → Get JavaVM*                             ││
│  │ ├─ JNI DefineClass → MemoryJarClassLoader                  ││
│  │ ├─ Load Payload JAR from memory byte[]                     ││
│  │ ├─ ClassLoader Hijack → Insert EventClassLoader            ││
│  │ ├─ JVMTI ClassFileLoadHook → Intercept class loading       ││
│  │ │   └─ Match modifiers → ASM patch → Return new bytecode   ││
│  │ ├─ Call Main.onLoad() → Activate payload                   ││
│  │ ├─ Poll for unload signal (END key)                        ││
│  │ └─ Main.onUnload() + Cleanup                               ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  Java Layer (Payload)                                          │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ EIF Module                                                  ││
│  │ ├─ Main.java (onLoad/onUnload entry points)                ││
│  │ ├─ EventDispatcher.java (20+ event hooks)                  ││
│  │ ├─ ModuleManager.java (module lifecycle)                   ││
│  │ ├─ Click GUI (draggable panels, property editors)          ││
│  │ └─ Properties (Boolean, Int, Float, Text, Mode, Color)     ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  Patching Engine (EIF-commons)                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ ClassModifier + 5 MethodModifiers                          ││
│  │ ├─ EntryMethodModifier (method entry injection)            ││
│  │ ├─ ReturnThrowMethodModifier (return/exception capture)    ││
│  │ ├─ LDCConstantModifier (constant interception)             ││
│  │ ├─ RedirectMethodModifier (method call redirection)        ││
│  │ └─ ModifyVariableModifier (variable store interception)    ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### Injection Flow

1. **DLL Injection**: `JarLoader.dll` is injected into the Java process
2. **JVM Attachment**: C++ code attaches to the running JVM using JNI
3. **ClassLoader Setup**: `MemoryJarClassLoader` is loaded via `DefineClass`
4. **Payload Loading**: JAR bytes are loaded from memory into the custom classloader
5. **ClassLoader Hijack**: `EventClassLoader` is inserted into Minecraft's parent chain
6. **JVMTI Setup**: ClassFileLoadHook is registered for class transformation
7. **Class Transformation**: Each loaded class is checked against modifiers and patched
8. **Payload Activation**: `Main.onLoad()` is called to start the framework
9. **Event Loop**: Framework runs until END key is pressed or `requestUnload()` is called
10. **Clean Shutdown**: Classloader chain restored, transformer shutdown, DLL unloaded

## Directory Structure

```
EIF-framework/
├── .github/workflows/          # CI/CD configuration
│   └── build.yml               # GitHub Actions workflow
├── src/                        # C++ native DLL source
│   ├── main.cpp                # DLL entry, JVM attach, lifecycle
│   ├── meta_jni.hpp            # MetaJNI2: C++ JNI wrapper
│   ├── mappings.hpp            # Java class/method/field mappings
│   ├── payload.jar.hpp         # GENERATED: embedded payload JAR
│   ├── MemoryJarClassLoader.class.hpp  # GENERATED: embedded class
│   ├── jvmti/
│   │   ├── jvmti.cpp           # JVMTI: find loaded classes
│   │   └── jvmti.hpp
│   ├── transformer/
│   │   ├── transformer.cpp     # ClassFileLoadHook implementation
│   │   └── transformer.hpp
│   └── logger/
│       ├── logger.cpp          # File logging
│       └── logger.hpp
├── memory-jar-classloader/     # Java: Custom ClassLoader
│   └── src/
│       └── MemoryJarClassLoader.java
├── EIF-commons/                # Java: ASM patching engine
│   └── src/
│       └── patcher/
│           ├── ClassModifier.java
│           ├── MethodModifier.java
│           ├── EntryMethodModifier.java
│           ├── ReturnThrowMethodModifier.java
│           ├── LDCConstantModifier.java
│           ├── RedirectMethodModifier.java
│           └── ModifyVariableModifier.java
├── EIF-processor/              # Java: Annotation processor
│   └── src/
│       └── processor/
│           ├── EventHandler.java
│           └── EventHandlerProcessor.java
├── EIF/                        # Java: Payload module
│   └── src/
│       └── main/java/
│           └── com/emberinjector/
│               ├── Main.java
│               ├── EventDispatcher.java
│               ├── EventClassLoader.java
│               ├── module/
│               │   ├── Module.java
│               │   ├── ModuleManager.java
│               │   └── modules/
│               │       └── KnockbackDelay.java
│               ├── gui/
│               │   ├── ClickGui.java
│               │   └── components/
│               └── property/
│                   └── PropertyManager.java
├── File2Hex/                   # C++ tool: binary → C++ header
├── include/                    # JNI headers
├── lib/                        # JVM import library
├── tools/                      # Bundled: JDK 8, Maven, CMake
├── remapper/                   # Mapping files
│   └── 1.8.9/
│       └── 1.8.9.tiny
├── CMakeLists.txt              # CMake build configuration
├── pom.xml                     # Maven parent POM
├── build.bat                   # One-shot build script
├── NOTICE.md                   # Copyright attribution
└── README.md                   # This file
```

## Technology Stack

| Category | Technology | Version | Purpose |
|----------|------------|---------|---------|
| **Native Layer** | C++ | C++20 | DLL injection, JNI/JVMTI integration |
| **Java Layer** | Java | JDK 8 | Payload, annotation processor, ASM patching |
| **Bytecode Manipulation** | ASM | 9.9 | Runtime class transformation |
| **Build System** | Maven | 3.9.9 | Multi-module Java build |
| **Build System** | CMake | 3.31.5 | C++ build configuration |
| **Build System** | NMake | - | MSVC build tool |
| **CI/CD** | GitHub Actions | - | Automated build pipeline |
| **Game Target** | Minecraft (Lunar Client) | 1.8.9 | Target game version |
| **Networking** | Netty | 4.0.23 | Packet interception |
| **Utilities** | Guava | 17.0 | Collection utilities |
| **Serialization** | Gson | 2.2.4 | JSON handling |
| **Graphics** | LWJGL | 2.9.x | OpenGL, input handling |

### Key Dependencies

- **ASM 9.9**: High-performance JVM bytecode manipulation framework
- **Netty 4.0.23**: Asynchronous event-driven network application framework
- **Guava 17.0**: Google core libraries for Java
- **Gson 2.2.4**: Java serialization/deserialization library
- **LWJGL 2.9.x**: Lightweight Java Game Library
- **Minecraft 1.8.9**: Target game client libraries

## Prerequisites

### Required Software

1. **Visual Studio** (with MSVC compiler)
   - Required for building the C++ DLL
   - Ensure `cl.exe` and CMake are in PATH

2. **JDK 8** (bundled in `tools/jdk8u442-b06`)
   - Required for Java compilation
   - Version 8 is mandatory for Minecraft 1.8.9 compatibility

3. **Maven** (bundled in `tools/apache-maven-3.9.9`)
   - Required for multi-module Java build
   - Handles dependency resolution and artifact packaging

4. **CMake** (bundled in `tools/cmake-3.31.5`)
   - Required for C++ build configuration
   - Generates NMake build files

### Required Files

- **Minecraft 1.8.9 Named JAR**: Place in `remapper/1.8.9/` directory
  - Required for namespace remapping
  - Contains Mojang named mappings

## Building the Project

### Quick Build

```batch
build.bat
```

This single command executes the entire build pipeline:

### Build Pipeline Details

1. **File2Hex Compilation**
   ```batch
   # Compile File2Hex tool (binary → C++ header converter)
   cl.exe /EHsc /std:c++20 File2Hex/File2Hex.cpp /Fe:File2Hex.exe
   ```

2. **Maven Build**
   ```batch
   # Compile Java, run annotation processor, shade fat JAR
   mvn clean package -pl EIF -am
   ```
   - Compiles all Java modules
   - Annotation processor generates `Patcher.java`
   - TinyRemapper remaps named → searge namespaces
   - Shade plugin creates fat JAR with all dependencies

3. **File2Hex Conversion**
   ```batch
   # Convert payload.jar to C++ byte array header
   File2Hex.exe payload.jar payload.jar.hpp
   ```
   - Embeds JAR bytes as C++ array
   - Generated header included in DLL build

4. **CMake Build**
   ```batch
   # Configure and build JarLoader.dll
   cmake -G "NMake Makefiles" -B build .
   cmake --build build --config Release
   ```
   - Generates NMake build files
   - Compiles C++20 source
   - Links JNI/JVMTI libraries
   - Produces `build/JarLoader.dll`

### Build Output

- **Primary Artifact**: `build/JarLoader.dll`
- **Secondary Artifacts**:
  - `EIF/target EIF-1.0-SNAPSHOT-shaded.jar` (fat JAR)
  - `payload.jar.hpp` (generated header)

### Build Configuration

#### CMake Options

```cmake
# CMakeLists.txt key settings
set(MINECRAFT_CLASS "net/minecraft/client/Minecraft")
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
```

#### Maven Properties

```xml
<!-- pom.xml key settings -->
<maven.compiler.release>8</maven.compiler.release>
<remapper.sourceNamespace>named</remapper.sourceNamespace>
<remapper.destinationNamespace>searge</remapper.destinationNamespace>
```

## Usage

### Injection Process

1. **Start Minecraft 1.8.9**
   - Launch the game normally
   - Wait for main menu to load

2. **Inject the DLL**
   - Use a DLL injector or manual injection tool
   - Target process: `javaw.exe` (Minecraft's Java process)
   - DLL path: `build/JarLoader.dll`

3. **Verify Injection**
   - Check for `mujina_logs.txt` in game directory
   - Log should show successful JVM attachment and class loading

4. **Use the Framework**
   - Press **RIGHT SHIFT** to open Click GUI
   - Toggle modules on/off
   - Configure properties
   - Set keybinds for quick access

5. **Unload (Optional)**
   - Press **END** key for clean unload
   - Framework removes all hooks and restores classloader chain
   - Game continues running without framework

### Command Line Injection (Advanced)

```batch
# Using a DLL injector tool
injector.exe -p javaw.exe -d build/JarLoader.dll

# Or using PowerShell (Windows)
$process = Get-Process javaw
# ... (requires custom injection code)
```

### Logging

Logs are written to `mujina_logs.txt` in the game directory:

```
[2024-01-15 14:30:25] [INFO] JarLoader: Successfully attached to JVM
[2024-01-15 14:30:25] [INFO] JarLoader: Loaded MemoryJarClassLoader
[2024-01-15 14:30:25] [INFO] JarLoader: Payload JAR loaded from memory
[2024-01-15 14:30:25] [INFO] JarLoader: ClassLoader hijack successful
[2024-01-15 14:30:25] [INFO] JarLoader: JVMTI transformer registered
[2024-01-15 14:30:25] [INFO] Main: Framework initialized
[2024-01-15 14:30:25] [INFO] Main: onLoad() called
```

## Module System

### Module Architecture

The framework uses a modular architecture with lifecycle management:

```java
// Module base class
public abstract class Module {
    private boolean enabled;
    private int keyBind;
    private PropertyManager properties;
    
    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void onTick();
    public void onPacket(Packet packet) {}
}
```

### Module Manager

```java
// ModuleManager handles module lifecycle
public class ModuleManager {
    private LinkedHashMap<Class<? extends Module>, Module> modules;
    
    public void register(Module module);
    public void unregister(Module module);
    public void enable(Class<? extends Module> moduleClass);
    public void disable(Class<? extends Module> moduleClass);
    public void tick();
}
```

### Available Modules

| Module | Category | Description |
|--------|----------|-------------|
| **KnockbackDelay** | Combat | Delays velocity/explosion packets for PvP timing |
| **GuiModule** | Misc | Opens the Click GUI |

### Creating Custom Modules

```java
@ModuleInfo(
    name = "CustomModule",
    category = Module.Category.COMBAT,
    description = "A custom module example"
)
public class CustomModule extends Module {
    private final BooleanProperty enabled = new BooleanProperty("Enabled", true);
    private final IntProperty delay = new IntProperty("Delay", 5, 1, 20);
    
    @Override
    public void onEnable() {
        // Module enabled
    }
    
    @Override
    public void onDisable() {
        // Module disabled
    }
    
    @Override
    public void onTick() {
        // Called every game tick
    }
}
```

## Injection Modes

### 1. ON_ENTRY

Injects code at the beginning of a method:

```java
@EventHandler(
    type = EventType.ON_ENTRY,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "runTick"
)
public static boolean onRunTick(Canceler canceler) {
    // Custom code here
    // Return false to cancel method execution
    return true;
}
```

**Use Cases:**
- Method interception
- Pre-processing logic
- Conditional method cancellation

### 2. ON_RETURN_THROW

Captures return values or exceptions:

```java
@EventHandler(
    type = EventType.ON_RETURN_THROW,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "getMouseOver"
)
public static Entity onGetMouseOver(Thrower thrower, Entity result) {
    // Modify return value
    return customEntity;
}
```

**Use Cases:**
- Return value modification
- Exception handling
- Result filtering

### 3. ON_LDC_CONSTANT

Intercepts constant loading instructions:

```java
@EventHandler(
    type = EventType.ON_LDC_CONSTANT,
    targetClass = "net.minecraft.util.ChatComponentText",
    targetMethodName = "<init>"
)
public static Object onChatComponent(Object constant) {
    // Modify string constants
    return modifiedConstant;
}
```

**Use Cases:**
- String modification
- Constant replacement
- Debug information injection

### 4. ON_REDIRECT

Replaces method calls:

```java
@EventHandler(
    type = EventType.ON_REDIRECT,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "sendQueue",
    redirectTarget = "customSendQueue"
)
public static void onSendQueue() {
    // Custom sendQueue implementation
}
```

**Use Cases:**
- Method replacement
- Performance optimization
- API modification

### 5. ON_MODIFY_VARIABLE

Intercepts variable assignments:

```java
@EventHandler(
    type = EventType.ON_MODIFY_VARIABLE,
    targetClass = "net.minecraft.entity.player.EntityPlayer",
    targetMethodName = "onUpdate",
    variableIndex = 0
)
public static float onVariableModify(float original) {
    // Modify variable value
    return modifiedValue;
}
```

**Use Cases:**
- Variable monitoring
- Value modification
- State tracking

## Event Hooks

### Available Hooks (EventDispatcher.java)

The framework provides 20+ event hooks for Minecraft core methods:

| Hook | Method | Description |
|------|--------|-------------|
| **runTick** | `Minecraft.runTick()` | Game tick processing |
| **runGameLoop** | `Minecraft.runGameLoop()` | Main game loop |
| **clickMouse** | `Minecraft.clickMouse()` | Mouse click handling |
| **rightClickMouse** | `Minecraft.rightClickMouse()` | Right mouse click |
| **displayGuiScreen** | `Minecraft.displayGuiScreen()` | GUI screen display |
| **onPlayerUpdate** | `EntityPlayer.onUpdate()` | Player update logic |
| **onUpdateWalkingPlayer** | `EntityPlayer.onUpdateWalkingPlayer()` | Walking player update |
| **sendPacket** | `NetHandlerPlayClient.sendPacket()` | Outgoing packet |
| **channelRead0** | `NetHandlerPlayClient.channelRead0()` | Incoming packet |
| **dispatchPacket** | `NetworkManager.dispatchPacket()` | Packet dispatch |
| **attackEntity** | `PlayerControllerMP.attackEntity()` | Entity attack |
| **getMouseOver** | `Minecraft.getMouseOver()` | Mouse target detection |
| **moveEntity** | `Entity.moveEntity()` | Entity movement |
| **jump** | `Entity.jump()` | Jump action |
| **changeCurrentItem** | `PlayerControllerMP.changeCurrentItem()` | Item switching |
| **setVelocity** | `Entity.setVelocity()` | Velocity modification |
| **closeScreen** | `Minecraft.closeScreen()` | Screen closing |
| **handleExplosion** | `NetHandlerPlayClient.handleExplosion()` | Explosion handling |
| **printChatMessage** | `Minecraft.printChatMessage()` | Chat message display |
| **sendChatMessage** | `Minecraft.sendChatMessage()` | Chat message sending |

### Hook Example

```java
@EventHandler(
    type = EventType.ON_ENTRY,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "runTick"
)
public static boolean onRunTick(Canceler canceler) {
    // Custom code before tick
    System.out.println("Tick executed");
    return true; // Allow execution
}
```

## GUI System

### Click GUI Features

- **Draggable Panels**: Category-based organization (Combat, Movement, Render, Player, Misc)
- **Module Toggles**: Enable/disable modules with visual feedback
- **Property Editors**: Intuitive editing for all property types
- **Scroll Support**: Navigate through modules with mouse wheel
- **Keybind Binding**: Assign keyboard shortcuts to modules

### GUI Components

| Component | Description |
|-----------|-------------|
| **CategoryPanel** | Draggable panel for module category |
| **ModuleButton** | Toggle button for individual modules |
| **PropertySlider** | Slider for numeric properties |
| **PropertyCheckBox** | Checkbox for boolean properties |
| **PropertyDropdown** | Dropdown for mode properties |
| **ColorPicker** | Color selection for color properties |

### Opening the GUI

- **Default Keybind**: RIGHT SHIFT
- **Programmatic**: `Minecraft.getMinecraft().displayGuiScreen(new ClickGui())`

### GUI Usage

1. Press RIGHT SHIFT to open GUI
2. Drag panels to organize
3. Click module names to toggle
4. Hover over properties to edit
5. Use mouse wheel to scroll
6. Press ESC or RIGHT SHIFT to close

## Property System

### Property Types

| Type | Class | Description | Example |
|------|-------|-------------|---------|
| **Boolean** | `BooleanProperty` | On/off toggle | `new BooleanProperty("Enabled", true)` |
| **Integer** | `IntProperty` | Numeric value with range | `new IntProperty("Delay", 5, 1, 20)` |
| **Float** | `FloatProperty` | Decimal value with range | `new FloatProperty("Speed", 1.0f, 0.1f, 5.0f)` |
| **Percent** | `PercentProperty` | Percentage (0-100) | `new PercentProperty("Chance", 50)` |
| **Text** | `TextProperty` | String input | `new TextProperty("Name", "default")` |
| **Mode** | `ModeProperty` | Enum-like selection | `new ModeProperty("Mode", Mode.values())` |
| **Color** | `ColorProperty` | Color value | `new ColorProperty("Color", Color.RED)` |

### Property Example

```java
public class KnockbackDelay extends Module {
    private final BooleanProperty airDelay = new BooleanProperty("AirDelay", true);
    private final IntProperty airDelayTicks = new IntProperty("AirDelayTicks", 2, 1, 10);
    private final FloatProperty chance = new FloatProperty("Chance", 100.0f, 0.0f, 100.0f);
    private final ModeProperty target = new ModeProperty("Target", TargetMode.values());
    
    @Override
    public void onEnable() {
        airDelay.addChangeListener(new PropertyChangeListener() {
            @Override
            public void onPropertyChanged(Property property) {
                // Handle property change
            }
        });
    }
}
```

### Property Management

```java
// PropertyManager handles all properties
PropertyManager propertyManager = new PropertyManager();

// Add properties
propertyManager.register(new BooleanProperty("Enabled", true));
propertyManager.register(new IntProperty("Delay", 5, 1, 20));

// Get property values
boolean enabled = propertyManager.getBoolean("Enabled");
int delay = propertyManager.getInt("Delay");

// Set property values
propertyManager.setBoolean("Enabled", false);
propertyManager.setInt("Delay", 10);
```

## Unloading

### Clean Unload Process

The framework supports clean unload without restarting the JVM:

1. **Trigger Unload**
   - Press **END** key, or
   - Call `Main.requestUnload()` programmatically

2. **Unload Sequence**
   ```
   onUnload() callback
   ↓
   JVMTI transformer shutdown
   ↓
   ClassLoader parent chain restoration
   ↓
   DLL self-unload (FreeLibraryAndExitThread)
   ```

3. **Restoration**
   - ClassLoader chain restored to original state
   - All ASM patches removed
   - JVMTI hooks unregistered
   - Memory cleaned up

### Unload Example

```java
// Programmatic unload
Main.requestUnload();

// Or trigger via keybind
@EventHandler(
    type = EventType.ON_ENTRY,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "runTick"
)
public static boolean onRunTick(Canceler canceler) {
    if (Keyboard.isKeyDown(Keyboard.KEY_END)) {
        Main.requestUnload();
        return false;
    }
    return true;
}
```

## Namespace Remapping

### Overview

The framework uses Mojang's named mappings at development time and remaps to searge (obfuscated) names at compile time:

```xml
<!-- pom.xml -->
<remapper.sourceNamespace>named</remapper.sourceNamespace>
<remapper.destinationNamespace>searge</remapper.destinationNamespace>
```

### Mapping Files

- **Location**: `remapper/1.8.9/1.8.9.tiny`
- **Format**: Tiny v2 mapping format
- **Source**: Mojang official mappings

### How It Works

1. **Development**: Use human-readable named mappings
   ```java
   @EventHandler(targetMethodName = "runTick") // Named
   ```

2. **Compilation**: TinyRemapper automatically remaps
   ```
   named → searge
   runTick → a
   ```

3. **Runtime**: Obfuscated names used in Minecraft
   ```java
   @EventHandler(targetMethodName = "a") // Searge
   ```

### Benefits

- **Readability**: Use meaningful names during development
- **Compatibility**: Automatic obfuscation handling
- **Maintainability**: Easy to understand and modify

## Key Components

### Native Layer (C++)

| File | Responsibility |
|------|----------------|
| `src/main.cpp` | DLL entry, JVM attach, ClassLoader hijack, lifecycle |
| `src/meta_jni.hpp` | MetaJNI2: C++ JNI wrapper (TLS, template cache, compile-time signatures) |
| `src/mappings.hpp` | Java class/method/field C++ mapping declarations |
| `src/jvmti/jvmti.cpp` | JVMTI: find loaded classes, get ClassLoader |
| `src/transformer/transformer.cpp` | ClassFileLoadHook: match modifiers → ASM patch → return new bytecode |
| `src/logger/logger.cpp` | File logging (`mujina_logs.txt`) |

### Java Payload

| File | Responsibility |
|------|----------------|
| `EIF/.../Main.java` | `onLoad()` / `onUnload()` entry points |
| `EIF/.../EventDispatcher.java` | 20+ `@EventHandler` hooks for Minecraft core methods |
| `EIF/.../EventClassLoader.java` | Delegate ClassLoader, bridges Minecraft classloader with payload |

### Patching Engine (EIF-commons)

| File | Responsibility |
|------|----------------|
| `patcher/ClassModifier.java` | Target class name + MethodModifier list, ASM ClassReader/Writer orchestration |
| `patcher/MethodModifier.java` | Abstract base: parameter stack, event handler invocation, return handling |
| `patcher/EntryMethodModifier.java` | Method entry injection: Canceler + conditional early return |
| `patcher/ReturnThrowMethodModifier.java` | Return/exception capture: Thrower + swallow/propagate exception |
| `patcher/LDCConstantModifier.java` | LDC constant interception: boxing → handler call → unboxing |
| `patcher/RedirectMethodModifier.java` | Method call redirection: replace target method with event handler |
| `patcher/ModifyVariableModifier.java` | Variable store interception: by Nth assignment ordinal |

### Annotation Processor (EIF-processor)

| File | Responsibility |
|------|----------------|
| `EventHandler.java` | `@EventHandler(type, targetClass, targetMethodName, ...)` |
| `EventHandlerProcessor.java` | Compile-time annotation scan → read Tiny mappings → rename to searge → generate `Patcher.java` |

## Contributing

### Development Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/xdddno/EIF-framework.git
   cd EIF-framework
   ```

2. **Install Dependencies**
   - Visual Studio with MSVC
   - JDK 8 (bundled in tools/)
   - Maven (bundled in tools/)

3. **Build Project**
   ```batch
   build.bat
   ```

4. **Test Injection**
   - Start Minecraft 1.8.9
   - Use DLL injector with `build/JarLoader.dll`
   - Verify logs in `mujina_logs.txt`

### Code Style

- **C++**: Follow C++20 standards, use RAII, smart pointers
- **Java**: Follow Oracle Java conventions, use meaningful names
- **Documentation**: Comment complex algorithms, document public APIs

### Pull Request Process

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Reporting Issues

- Use GitHub Issues
- Include detailed reproduction steps
- Provide log files if applicable
- Specify Minecraft version and OS

## License

### Copyright

This repository contains code from multiple authors:

| Component | Source | License |
|-----------|--------|---------|
| `src/*`, `File2Hex/*` | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| `EIF-commons` (partial) | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| `EIF-processor` (partial) | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| `EIF/.../EventClassLoader.java` | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| `memory-jar-classloader` | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| EIF-framework additions | xdddno | **MIT License** |

### MIT License (EIF-framework Additions)

```
MIT License

Copyright (c) xdddno

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### Important Notes

- **All Rights Reserved**: Code from MujinaBaseV2 cannot be used without permission
- **MIT License**: Only EIF-framework original additions are open source
- See `NOTICE.md` for detailed copyright attribution

## Disclaimer

This repository contains code for **educational and research purposes only**.

- All code is provided "AS IS" without warranty of any kind
- Use at your own risk
- The authors are not responsible for any misuse or damage
- This framework is not affiliated with Mojang or Microsoft
- Use in compliance with Minecraft's Terms of Service

## Acknowledgments

### Original Authors

- **Lefraudeur**: Creator of MujinaBaseV2
- **achul123**: Fork contributor

### Dependencies

- **ASM**: ObjectWeb bytecode manipulation framework
- **Netty**: Asynchronous event-driven network framework
- **Guava**: Google core libraries for Java
- **Gson**: Java JSON library
- **LWJGL**: Lightweight Java Game Library

### Community

- Minecraft modding community
- Java bytecode engineering community
- C++/JNI development community

---

**Built with ❤️ by xdddno**

**[Back to Top](#EIF-framework-Ember-Injection-Framework)**
