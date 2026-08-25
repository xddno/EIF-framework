# EIF-framework (Ember Injection Framework)

[![Build Status](https://github.com/xdddno/EIF-framework/actions/workflows/build.yml/badge.svg)](https://github.com/xdddno/EIF-framework/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-8-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html)
[![C++ Version](https://img.shields.io/badge/C++-20-blue.svg)](https://isocpp.org/)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.8.9-green.svg)](https://www.minecraft.net/)

**一个高性能的 Minecraft 1.8.9 字节码注入框架，用于高级模组开发和研究。**

EIF-framework 是一个原生 DLL 注入系统，使用 C++ 注入到 Java 进程中，通过 JNI/JVMTI 实现运行时类转换和事件拦截。Payload JAR 完全从内存加载，永不接触磁盘，提供干净高效的注入机制。

> **注意**: 此框架专门针对 **Lunar Client 1.8.9** 设计。虽然它可能与其他 Minecraft 1.8.9 发行版兼容，但它是为 Lunar Client 优化和测试的。

**[English Documentation](README.md) | 中文文档**

## 目录

- [特性](#特性)
- [架构概览](#架构概览)
- [目录结构](#目录结构)
- [技术栈](#技术栈)
- [前置条件](#前置条件)
- [构建项目](#构建项目)
- [使用方法](#使用方法)
- [模块系统](#模块系统)
- [注入模式](#注入模式)
- [事件钩子](#事件钩子)
- [GUI系统](#gui系统)
- [属性系统](#属性系统)
- [卸载机制](#卸载机制)
- [命名空间重映射](#命名空间重映射)
- [关键组件](#关键组件)
- [贡献指南](#贡献指南)
- [许可证](#许可证)
- [免责声明](#免责声明)
- [致谢](#致谢)

## 特性

### 核心特性
- **纯内存注入**: Payload JAR 完全从字节数组加载，永不接触磁盘
- **运行时类转换**: 基于 ASM 的类加载时字节码修补
- **事件驱动架构**: 20+ 个 Minecraft 核心方法事件钩子
- **模块化设计**: 基于插件的模块系统，支持生命周期管理
- **干净卸载**: 安全移除，恢复类加载器链
- **跨平台基础**: C++20 原生层，集成 JNI/JVMTI

### 技术特性
- **五种注入模式**: ON_ENTRY、ON_RETURN_THROW、ON_LDC_CONSTANT、ON_REDIRECT、ON_MODIFY_VARIABLE
- **注解处理器**: 编译时代码生成，类型安全的事件处理
- **命名空间重映射**: 自动 Mojang named → searge 混淆映射
- **自定义 ClassLoader**: MemoryJarClassLoader 从字节数组加载类
- **JVMTI 集成**: ClassFileLoadHook 拦截所有类加载
- **MetaJNI2**: 高级 C++ JNI 封装，支持 TLS、模板缓存和编译期签名

### 用户特性
- **Click GUI**: 可拖动的分类面板，模块切换和属性编辑器
- **模块管理**: 启用/禁用模块，支持快捷键和 tick 更新
- **属性系统**: Boolean、Int、Float、Percent、Text、Mode 和 Color 属性
- **热重载**: 按 END 键干净卸载，无需重启 JVM
- **日志系统**: 基于文件的日志，用于调试和监控

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                     EIF-framework 架构                          │
├─────────────────────────────────────────────────────────────────┤
│  原生层 (C++)                                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ JarLoader.dll                                               ││
│  │ ├─ DllMain → mainThread → app()                            ││
│  │ ├─ 附加到 JVM → 获取 JavaVM*                               ││
│  │ ├─ JNI DefineClass → MemoryJarClassLoader                  ││
│  │ ├─ 从内存 byte[] 加载 Payload JAR                          ││
│  │ ├─ ClassLoader 劫持 → 插入 EventClassLoader                ││
│  │ ├─ JVMTI ClassFileLoadHook → 拦截类加载                    ││
│  │ │   └─ 匹配修饰器 → ASM 补丁 → 返回新字节码               ││
│  │ ├─ 调用 Main.onLoad() → 激活 payload                       ││
│  │ ├─ 轮询卸载信号 (END 键)                                   ││
│  │ └─ Main.onUnload() + 清理                                  ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  Java 层 (Payload)                                             │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ EIF 模块                                                    ││
│  │ ├─ Main.java (onLoad/onUnload 入口点)                      ││
│  │ ├─ EventDispatcher.java (20+ 事件钩子)                     ││
│  │ ├─ ModuleManager.java (模块生命周期)                       ││
│  │ ├─ Click GUI (可拖动面板、属性编辑器)                       ││
│  │ └─ 属性 (Boolean, Int, Float, Text, Mode, Color)           ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  补丁引擎 (EIF-commons)                                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ ClassModifier + 5 种 MethodModifier                        ││
│  │ ├─ EntryMethodModifier (方法入口注入)                       ││
│  │ ├─ ReturnThrowMethodModifier (返回/异常捕获)                ││
│  │ ├─ LDCConstantModifier (常量拦截)                           ││
│  │ ├─ RedirectMethodModifier (方法调用重定向)                  ││
│  │ └─ ModifyVariableModifier (变量存储拦截)                    ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 注入流程

1. **DLL 注入**: `JarLoader.dll` 被注入到 Java 进程
2. **JVM 附加**: C++ 代码使用 JNI 附加到运行中的 JVM
3. **ClassLoader 设置**: 通过 `DefineClass` 加载 `MemoryJarClassLoader`
4. **Payload 加载**: JAR 字节从内存加载到自定义类加载器
5. **ClassLoader 劫持**: `EventClassLoader` 被插入 Minecraft 的父链
6. **JVMTI 设置**: 注册 ClassFileLoadHook 用于类转换
7. **类转换**: 每个加载的类都根据修饰符进行检查和修补
8. **Payload 激活**: 调用 `Main.onLoad()` 启动框架
9. **事件循环**: 框架运行直到按 END 键或调用 `requestUnload()`
10. **干净关闭**: 恢复类加载器链，关闭转换器，卸载 DLL

## 目录结构

```
EIF-framework/
├── .github/workflows/          # CI/CD 配置
│   └── build.yml               # GitHub Actions 工作流
├── src/                        # C++ 原生 DLL 源码
│   ├── main.cpp                # DLL 入口，JVM 附加，生命周期
│   ├── meta_jni.hpp            # MetaJNI2: C++ JNI 封装
│   ├── mappings.hpp            # Java 类/方法/字段映射
│   ├── payload.jar.hpp         # 生成：嵌入的 payload JAR
│   ├── MemoryJarClassLoader.class.hpp  # 生成：嵌入的类
│   ├── jvmti/
│   │   ├── jvmti.cpp           # JVMTI：查找已加载类
│   │   └── jvmti.hpp
│   ├── transformer/
│   │   ├── transformer.cpp     # ClassFileLoadHook 实现
│   │   └── transformer.hpp
│   └── logger/
│       ├── logger.cpp          # 文件日志
│       └── logger.hpp
├── memory-jar-classloader/     # Java：自定义 ClassLoader
│   └── src/
│       └── MemoryJarClassLoader.java
├── EIF-commons/                # Java：ASM 补丁引擎
│   └── src/
│       └── patcher/
│           ├── ClassModifier.java
│           ├── MethodModifier.java
│           ├── EntryMethodModifier.java
│           ├── ReturnThrowMethodModifier.java
│           ├── LDCConstantModifier.java
│           ├── RedirectMethodModifier.java
│           └── ModifyVariableModifier.java
├── EIF-processor/              # Java：注解处理器
│   └── src/
│       └── processor/
│           ├── EventHandler.java
│           └── EventHandlerProcessor.java
├── EIF/                        # Java：Payload 模块
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
├── File2Hex/                   # C++ 工具：二进制 → C++ 头文件
├── include/                    # JNI 头文件
├── lib/                        # JVM 导入库
├── tools/                      # 捆绑：JDK 8、Maven、CMake
├── remapper/                   # 映射文件
│   └── 1.8.9/
│       └── 1.8.9.tiny
├── CMakeLists.txt              # CMake 构建配置
├── pom.xml                     # Maven 父 POM
├── build.bat                   # 一键构建脚本
├── NOTICE.md                   # 版权归属
└── README.md                   # 英文文档
```

## 技术栈

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **原生层** | C++ | C++20 | DLL 注入、JNI/JVMTI 集成 |
| **Java 层** | Java | JDK 8 | Payload、注解处理器、ASM 补丁 |
| **字节码操作** | ASM | 9.9 | 运行时类转换 |
| **构建系统** | Maven | 3.9.9 | 多模块 Java 构建 |
| **构建系统** | CMake | 3.31.5 | C++ 构建配置 |
| **构建系统** | NMake | - | MSVC 构建工具 |
| **CI/CD** | GitHub Actions | - | 自动化构建管道 |
| **游戏目标** | Minecraft (Lunar Client) | 1.8.9 | 目标游戏版本 |
| **网络** | Netty | 4.0.23 | 数据包拦截 |
| **工具库** | Guava | 17.0 | 集合工具 |
| **序列化** | Gson | 2.2.4 | JSON 处理 |
| **图形** | LWJGL | 2.9.x | OpenGL、输入处理 |

### 关键依赖

- **ASM 9.9**: 高性能 JVM 字节码操作框架
- **Netty 4.0.23**: 异步事件驱动网络应用框架
- **Guava 17.0**: Google Java 核心库
- **Gson 2.2.4**: Java 序列化/反序列化库
- **LWJGL 2.9.x**: 轻量级 Java 游戏库
- **Minecraft 1.8.9**: 目标游戏客户端库

## 前置条件

### 必需软件

1. **Visual Studio**（包含 MSVC 编译器）
   - 构建 C++ DLL 必需
   - 确保 `cl.exe` 和 CMake 在 PATH 中

2. **JDK 8**（捆绑在 `tools/jdk8u442-b06`）
   - Java 编译必需
   - Minecraft 1.8.9 兼容性需要版本 8

3. **Maven**（捆绑在 `tools/apache-maven-3.9.9`）
   - 多模块 Java 构建必需
   - 处理依赖解析和 artifact 打包

4. **CMake**（捆绑在 `tools/cmake-3.31.5`）
   - C++ 构建配置必需
   - 生成 NMake 构建文件

### 必需文件

- **Minecraft 1.8.9 Named JAR**: 放置在 `remapper/1.8.9/` 目录
  - 命名空间重映射必需
  - 包含 Mojang named 映射

## 构建项目

### 快速构建

```batch
build.bat
```

此单命令执行整个构建管道：

### 构建管道详情

1. **File2Hex 编译**
   ```batch
   # 编译 File2Hex 工具（二进制 → C++ 头文件转换器）
   cl.exe /EHsc /std:c++20 File2Hex/File2Hex.cpp /Fe:File2Hex.exe
   ```

2. **Maven 构建**
   ```batch
   # 编译 Java，运行注解处理器，shade fat JAR
   mvn clean package -pl EIF -am
   ```
   - 编译所有 Java 模块
   - 注解处理器生成 `Patcher.java`
   - TinyRemapper 重映射 named → searge 命名空间
   - Shade 插件创建包含所有依赖的 fat JAR

3. **File2Hex 转换**
   ```batch
   # 将 payload.jar 转换为 C++ 字节数组头文件
   File2Hex.exe payload.jar payload.jar.hpp
   ```
   - 将 JAR 字节嵌入为 C++ 数组
   - 生成的头文件包含在 DLL 构建中

4. **CMake 构建**
   ```batch
   # 配置和构建 JarLoader.dll
   cmake -G "NMake Makefiles" -B build .
   cmake --build build --config Release
   ```
   - 生成 NMake 构建文件
   - 编译 C++20 源码
   - 链接 JNI/JVMTI 库
   - 生成 `build/JarLoader.dll`

### 构建输出

- **主要产物**: `build/JarLoader.dll`
- **次要产物**:
  - `EIF/target EIF-1.0-SNAPSHOT-shaded.jar`（fat JAR）
  - `payload.jar.hpp`（生成的头文件）

### 构建配置

#### CMake 选项

```cmake
# CMakeLists.txt 关键设置
set(MINECRAFT_CLASS "net/minecraft/client/Minecraft")
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
```

#### Maven 属性

```xml
<!-- pom.xml 关键设置 -->
<maven.compiler.release>8</maven.compiler.release>
<remapper.sourceNamespace>named</remapper.sourceNamespace>
<remapper.destinationNamespace>searge</remapper.destinationNamespace>
```

## 使用方法

### 注入过程

1. **启动 Minecraft 1.8.9**
   - 正常启动游戏
   - 等待主菜单加载

2. **注入 DLL**
   - 使用 DLL 注入器或手动注入工具
   - 目标进程：`javaw.exe`（Minecraft 的 Java 进程）
   - DLL 路径：`build/JarLoader.dll`

3. **验证注入**
   - 检查游戏目录中的 `mujina_logs.txt`
   - 日志应显示成功附加到 JVM 和类加载

4. **使用框架**
   - 按 **RIGHT SHIFT** 打开 Click GUI
   - 切换模块开/关
   - 配置属性
   - 设置快捷键快速访问

5. **卸载（可选）**
   - 按 **END** 键干净卸载
   - 框架移除所有钩子并恢复类加载器链
   - 游戏在没有框架的情况下继续运行

### 命令行注入（高级）

```batch
# 使用 DLL 注入器工具
injector.exe -p javaw.exe -d build/JarLoader.dll

# 或使用 PowerShell (Windows)
$process = Get-Process javaw
# ...（需要自定义注入代码）
```

### 日志

日志写入游戏目录中的 `mujina_logs.txt`：

```
[2024-01-15 14:30:25] [INFO] JarLoader: Successfully attached to JVM
[2024-01-15 14:30:25] [INFO] JarLoader: Loaded MemoryJarClassLoader
[2024-01-15 14:30:25] [INFO] JarLoader: Payload JAR loaded from memory
[2024-01-15 14:30:25] [INFO] JarLoader: ClassLoader hijack successful
[2024-01-15 14:30:25] [INFO] JarLoader: JVMTI transformer registered
[2024-01-15 14:30:25] [INFO] Main: Framework initialized
[2024-01-15 14:30:25] [INFO] Main: onLoad() called
```

## 模块系统

### 模块架构

框架使用具有生命周期管理的模块化架构：

```java
// 模块基类
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

### 模块管理器

```java
// ModuleManager 处理模块生命周期
public class ModuleManager {
    private LinkedHashMap<Class<? extends Module>, Module> modules;
    
    public void register(Module module);
    public void unregister(Module module);
    public void enable(Class<? extends Module> moduleClass);
    public void disable(Class<? extends Module> moduleClass);
    public void tick();
}
```

### 可用模块

| 模块 | 分类 | 描述 |
|------|------|------|
| **KnockbackDelay** | Combat | 延迟速度/爆炸数据包用于 PvP 计时 |
| **GuiModule** | Misc | 打开 Click GUI |

### 创建自定义模块

```java
@ModuleInfo(
    name = "CustomModule",
    category = Module.Category.COMBAT,
    description = "自定义模块示例"
)
public class CustomModule extends Module {
    private final BooleanProperty enabled = new BooleanProperty("Enabled", true);
    private final IntProperty delay = new IntProperty("Delay", 5, 1, 20);
    
    @Override
    public void onEnable() {
        // 模块启用
    }
    
    @Override
    public void onDisable() {
        // 模块禁用
    }
    
    @Override
    public void onTick() {
        // 每个游戏 tick 调用
    }
}
```

## 注入模式

### 1. ON_ENTRY

在方法开头注入代码：

```java
@EventHandler(
    type = EventType.ON_ENTRY,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "runTick"
)
public static boolean onRunTick(Canceler canceler) {
    // 自定义代码
    // 返回 false 取消方法执行
    return true;
}
```

**用例：**
- 方法拦截
- 预处理逻辑
- 条件方法取消

### 2. ON_RETURN_THROW

捕获返回值或异常：

```java
@EventHandler(
    type = EventType.ON_RETURN_THROW,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "getMouseOver"
)
public static Entity onGetMouseOver(Thrower thrower, Entity result) {
    // 修改返回值
    return customEntity;
}
```

**用例：**
- 返回值修改
- 异常处理
- 结果过滤

### 3. ON_LDC_CONSTANT

拦截常量加载指令：

```java
@EventHandler(
    type = EventType.ON_LDC_CONSTANT,
    targetClass = "net.minecraft.util.ChatComponentText",
    targetMethodName = "<init>"
)
public static Object onChatComponent(Object constant) {
    // 修改字符串常量
    return modifiedConstant;
}
```

**用例：**
- 字符串修改
- 常量替换
- 调试信息注入

### 4. ON_REDIRECT

替换方法调用：

```java
@EventHandler(
    type = EventType.ON_REDIRECT,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "sendQueue",
    redirectTarget = "customSendQueue"
)
public static void onSendQueue() {
    // 自定义 sendQueue 实现
}
```

**用例：**
- 方法替换
- 性能优化
- API 修改

### 5. ON_MODIFY_VARIABLE

拦截变量赋值：

```java
@EventHandler(
    type = EventType.ON_MODIFY_VARIABLE,
    targetClass = "net.minecraft.entity.player.EntityPlayer",
    targetMethodName = "onUpdate",
    variableIndex = 0
)
public static float onVariableModify(float original) {
    // 修改变量值
    return modifiedValue;
}
```

**用例：**
- 变量监控
- 值修改
- 状态跟踪

## 事件钩子

### 可用钩子 (EventDispatcher.java)

框架为 Minecraft 核心方法提供 20+ 个事件钩子：

| 钩子 | 方法 | 描述 |
|------|------|------|
| **runTick** | `Minecraft.runTick()` | 游戏 tick 处理 |
| **runGameLoop** | `Minecraft.runGameLoop()` | 主游戏循环 |
| **clickMouse** | `Minecraft.clickMouse()` | 鼠标点击处理 |
| **rightClickMouse** | `Minecraft.rightClickMouse()` | 鼠标右键点击 |
| **displayGuiScreen** | `Minecraft.displayGuiScreen()` | GUI 屏幕显示 |
| **onPlayerUpdate** | `EntityPlayer.onUpdate()` | 玩家更新逻辑 |
| **onUpdateWalkingPlayer** | `EntityPlayer.onUpdateWalkingPlayer()` | 行走玩家更新 |
| **sendPacket** | `NetHandlerPlayClient.sendPacket()` | 出站数据包 |
| **channelRead0** | `NetHandlerPlayClient.channelRead0()` | 入站数据包 |
| **dispatchPacket** | `NetworkManager.dispatchPacket()` | 数据包分发 |
| **attackEntity** | `PlayerControllerMP.attackEntity()` | 实体攻击 |
| **getMouseOver** | `Minecraft.getMouseOver()` | 鼠标目标检测 |
| **moveEntity** | `Entity.moveEntity()` | 实体移动 |
| **jump** | `Entity.jump()` | 跳跃动作 |
| **changeCurrentItem** | `PlayerControllerMP.changeCurrentItem()` | 物品切换 |
| **setVelocity** | `Entity.setVelocity()` | 速度修改 |
| **closeScreen** | `Minecraft.closeScreen()` | 屏幕关闭 |
| **handleExplosion** | `NetHandlerPlayClient.handleExplosion()` | 爆炸处理 |
| **printChatMessage** | `Minecraft.printChatMessage()` | 聊天消息显示 |
| **sendChatMessage** | `Minecraft.sendChatMessage()` | 聊天消息发送 |

### 钩子示例

```java
@EventHandler(
    type = EventType.ON_ENTRY,
    targetClass = "net.minecraft.client.Minecraft",
    targetMethodName = "runTick"
)
public static boolean onRunTick(Canceler canceler) {
    // tick 之前的自定义代码
    System.out.println("Tick executed");
    return true; // 允许执行
}
```

## GUI系统

### Click GUI 特性

- **可拖动面板**: 基于分类的组织（Combat、Movement、Render、Player、Misc）
- **模块切换**: 启用/禁用模块，提供视觉反馈
- **属性编辑器**: 所有属性类型的直观编辑
- **滚动支持**: 使用鼠标滚轮导航模块
- **快捷键绑定**: 为模块分配键盘快捷键

### GUI 组件

| 组件 | 描述 |
|------|------|
| **CategoryPanel** | 模块分类的可拖动面板 |
| **ModuleButton** | 单个模块的切换按钮 |
| **PropertySlider** | 数字属性的滑块 |
| **PropertyCheckBox** | 布尔属性的复选框 |
| **PropertyDropdown** | 模式属性的下拉菜单 |
| **ColorPicker** | 颜色属性的颜色选择器 |

### 打开 GUI

- **默认快捷键**: RIGHT SHIFT
- **编程方式**: `Minecraft.getMinecraft().displayGuiScreen(new ClickGui())`

### GUI 使用方法

1. 按 RIGHT SHIFT 打开 GUI
2. 拖动面板组织
3. 点击模块名称切换
4. 悬停属性编辑
5. 使用鼠标滚轮滚动
6. 按 ESC 或 RIGHT SHIFT 关闭

## 属性系统

### 属性类型

| 类型 | 类 | 描述 | 示例 |
|------|-----|------|------|
| **Boolean** | `BooleanProperty` | 开/关切换 | `new BooleanProperty("Enabled", true)` |
| **Integer** | `IntProperty` | 带范围的数字值 | `new IntProperty("Delay", 5, 1, 20)` |
| **Float** | `FloatProperty` | 带范围的十进制值 | `new FloatProperty("Speed", 1.0f, 0.1f, 5.0f)` |
| **Percent** | `PercentProperty` | 百分比 (0-100) | `new PercentProperty("Chance", 50)` |
| **Text** | `TextProperty` | 字符串输入 | `new TextProperty("Name", "default")` |
| **Mode** | `ModeProperty` | 类枚举选择 | `new ModeProperty("Mode", Mode.values())` |
| **Color** | `ColorProperty` | 颜色值 | `new ColorProperty("Color", Color.RED)` |

### 属性示例

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
                // 处理属性变化
            }
        });
    }
}
```

### 属性管理

```java
// PropertyManager 处理所有属性
PropertyManager propertyManager = new PropertyManager();

// 添加属性
propertyManager.register(new BooleanProperty("Enabled", true));
propertyManager.register(new IntProperty("Delay", 5, 1, 20));

// 获取属性值
boolean enabled = propertyManager.getBoolean("Enabled");
int delay = propertyManager.getInt("Delay");

// 设置属性值
propertyManager.setBoolean("Enabled", false);
propertyManager.setInt("Delay", 10);
```

## 卸载机制

### 干净卸载过程

框架支持无需重启 JVM 的干净卸载：

1. **触发卸载**
   - 按 **END** 键，或
   - 编程方式调用 `Main.requestUnload()`

2. **卸载序列**
   ```
   onUnload() 回调
   ↓
   JVMTI 转换器关闭
   ↓
   ClassLoader 父链恢复
   ↓
   DLL 自卸载 (FreeLibraryAndExitThread)
   ```

3. **恢复**
   - ClassLoader 链恢复到原始状态
   - 所有 ASM 补丁移除
   - JVMTI 钩子取消注册
   - 内存清理

### 卸载示例

```java
// 编程卸载
Main.requestUnload();

// 或通过快捷键触发
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

## 命名空间重映射

### 概述

框架在开发时使用 Mojang 的 named 映射，在编译时重映射到 searge（混淆）名称：

```xml
<!-- pom.xml -->
<remapper.sourceNamespace>named</remapper.sourceNamespace>
<remapper.destinationNamespace>searge</remapper.destinationNamespace>
```

### 映射文件

- **位置**: `remapper/1.8.9/1.8.9.tiny`
- **格式**: Tiny v2 映射格式
- **来源**: Mojang 官方映射

### 工作原理

1. **开发**: 使用人类可读的 named 映射
   ```java
   @EventHandler(targetMethodName = "runTick") // Named
   ```

2. **编译**: TinyRemapper 自动重映射
   ```
   named → searge
   runTick → a
   ```

3. **运行时**: 在 Minecraft 中使用混淆名称
   ```java
   @EventHandler(targetMethodName = "a") // Searge
   ```

### 优势

- **可读性**: 开发期间使用有意义的名称
- **兼容性**: 自动混淆处理
- **可维护性**: 易于理解和修改

## 关键组件

### 原生层 (C++)

| 文件 | 职责 |
|------|------|
| `src/main.cpp` | DLL 入口、JVM 附加、ClassLoader 劫持、生命周期 |
| `src/meta_jni.hpp` | MetaJNI2: C++ JNI 封装 (TLS、模板缓存、编译期签名) |
| `src/mappings.hpp` | Java 类/方法/字段的 C++ 映射声明 |
| `src/jvmti/jvmti.cpp` | JVMTI: 查找已加载类、获取 ClassLoader |
| `src/transformer/transformer.cpp` | ClassFileLoadHook: 匹配修饰符 → ASM 补丁 → 返回新字节码 |
| `src/logger/logger.cpp` | 文件日志 (`mujina_logs.txt`) |

### Java Payload

| 文件 | 职责 |
|------|------|
| `EIF/.../Main.java` | `onLoad()` / `onUnload()` 入口点 |
| `EIF/.../EventDispatcher.java` | 20+ `@EventHandler` 钩子，覆盖 Minecraft 核心方法 |
| `EIF/.../EventClassLoader.java` | 委托 ClassLoader，桥接 Minecraft 类加载器与 payload |

### 补丁引擎 (EIF-commons)

| 文件 | 职责 |
|------|------|
| `patcher/ClassModifier.java` | 目标类名 + MethodModifier 列表，ASM ClassReader/Writer 编排 |
| `patcher/MethodModifier.java` | 抽象基类：参数压栈、事件处理器调用、返回处理 |
| `patcher/EntryMethodModifier.java` | 方法入口注入：Canceler + 条件提前返回 |
| `patcher/ReturnThrowMethodModifier.java` | 返回/异常捕获：Thrower + 吞异常/重抛 |
| `patcher/LDCConstantModifier.java` | LDC 常量拦截：装箱 → 调处理器 → 拆箱回写 |
| `patcher/RedirectMethodModifier.java` | 方法调用重定向：替换目标方法调用为事件处理器 |
| `patcher/ModifyVariableModifier.java` | 变量赋值拦截：按第 N 次触发 |

### 注解处理器 (EIF-processor)

| 文件 | 职责 |
|------|------|
| `EventHandler.java` | `@EventHandler(type, targetClass, targetMethodName, ...)` |
| `EventHandlerProcessor.java` | 编译时扫描注解 → 读取 Tiny 映射 → 重命名为 searge → 生成 `Patcher.java` |

## 贡献指南

### 开发设置

1. **克隆仓库**
   ```bash
   git clone https://github.com/xdddno/EIF-framework.git
   cd EIF-framework
   ```

2. **安装依赖**
   - Visual Studio with MSVC
   - JDK 8（捆绑在 tools/）
   - Maven（捆绑在 tools/）

3. **构建项目**
   ```batch
   build.bat
   ```

4. **测试注入**
   - 启动 Minecraft 1.8.9
   - 使用 DLL 注入器注入 `build/JarLoader.dll`
   - 验证 `mujina_logs.txt` 中的日志

### 代码风格

- **C++**: 遵循 C++20 标准，使用 RAII、智能指针
- **Java**: 遵循 Oracle Java 约定，使用有意义的名称
- **文档**: 为复杂算法添加注释，记录公共 API

### Pull Request 流程

1. Fork 仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开 Pull Request

### 报告问题

- 使用 GitHub Issues
- 包含详细的重现步骤
- 提供日志文件（如果适用）
- 指定 Minecraft 版本和操作系统

## 许可证

### 版权

此仓库包含来自多个作者的代码：

| 组件 | 来源 | 许可证 |
|------|------|--------|
| `src/*`、`File2Hex/*` | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| `EIF-commons`（部分） | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| `EIF-processor`（部分） | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| `EIF/.../EventClassLoader.java` | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| `memory-jar-classloader` | MujinaBaseV2 (Lefraudeur) | All Rights Reserved |
| EIF-framework 新增 | xdddno | **MIT 许可证** |

### MIT 许可证（EIF-framework 新增）

```
MIT 许可证

版权所有 (c) xdddno

特此免费授予任何获得本软件及相关文档文件（"软件"）副本的人不受限制地处理
本软件的权利，包括但不限于使用、复制、修改、合并、发布、分发、再许可和/或出售
本软件副本的权利，并允许向其提供本软件的人这样做，但须满足以下条件：

上述版权声明和本许可声明应包含在本软件的所有副本或重要部分中。

本软件按"原样"提供，不附带任何明示或暗示的保证，包括但不限于对适销性、特定
用途适用性和非侵权性的保证。在任何情况下，作者或版权持有人均不对任何索赔、损害
或其他责任负责，无论是在合同诉讼、侵权诉讼或其他诉讼中，由本软件引起或与之相关。
```

### 重要说明

- **All Rights Reserved**: 未经许可不得使用 MujinaBaseV2 的代码
- **MIT 许可证**: 仅 EIF-framework 原创新增部分开源
- 详见 `NOTICE.md` 了解详细版权归属

## 免责声明

此仓库包含的代码仅供 **教育和研究目的**。

- 所有代码按"原样"提供，不附带任何明示或暗示的保证
- 使用风险自负
- 作者对任何误用或损害不承担责任
- 此框架与 Mojang 或 Microsoft 无关
- 使用时请遵守 Minecraft 的服务条款

## 致谢

### 原作者

- **Lefraudeur**: MujinaBaseV2 创建者
- **achul123**: Fork 贡献者

### 依赖项

- **ASM**: ObjectWeb 字节码操作框架
- **Netty**: 异步事件驱动网络框架
- **Guava**: Google Java 核心库
- **Gson**: Java JSON 库
- **LWJGL**: 轻量级 Java 游戏库

### 社区

- Minecraft 模组社区
- Java 字节码工程社区
- C++/JNI 开发社区

---

**由 xdddno 用 ❤️ 构建**

**[返回顶部](#EIF-framework-Ember-Injection-Framework)**
