# Single Player Only — NeoForge 1.21.1

一个**只允许单人游戏**的 NeoForge 客户端模组：标题界面不再提供「多人游戏 / Realms」入口，
多人游戏界面、Realms 界面、直连界面以及「对局域网开放」界面都无法打开，会被直接弹回标题界面。

- Minecraft **1.21.1**
- NeoForge **21.1.x**（在 21.1.267 上构建）
- Java **21**
- 客户端功能模组：服务端加载后不做任何事（事件订阅者只注册在客户端），也不会阻止你进别人的服务器

## 实现方式

不使用 Mixin，全部基于 NeoForge 公开的 `ScreenEvent`（两个处理器，见
[ClientScreenGuard.java](src/main/java/com/adeahejshshsh/singleplayeronly/client/ClientScreenGuard.java)）：

| 目标 | 手段 |
|---|---|
| 多人游戏 / Realms 界面、直连界面、局域网开放界面 | `ScreenEvent.Opening`：命中则 `setNewScreen(new TitleScreen())` |
| 标题界面的「多人游戏」「Realms」按钮 | `ScreenEvent.Init.Post`：遍历控件，命中则 `visible=false, active=false` |
| 暂停界面的「对局域网开放」按钮 | 同上（避免单机存档被对外暴露） |
| 模组入口 | `@Mod("singleplayeronly")` + `@EventBusSubscriber(bus = GAME, value = CLIENT)` |

界面识别用三重判定，抗改名、抗混淆：

1. `instanceof` 直接判断 `JoinMultiplayerScreen` / `ConnectScreen` / `ShareToLanScreen`；
2. 类名包含 `RealmsMainScreen` / `RealmsNotificationsScreen` / `ShareToLanScreen` / `OpenToLanScreen`
   （Realms 相关类不在 NeoForge 公开 API 内，且不同版本包名会变）；
3. 按钮识别优先比对翻译键 `menu.multiplayer` / `menu.online` / `menu.shareToLan`，
   再回退到文本包含 `multiplayer` / `realms` / `多人游戏` / `领域`（含繁体）。

**为什么不用 Mixin**：Mixin 方案要额外维护 `[[mixins]]` 声明、refmap 和注入点签名，
每次 MC 版本变动都要重新对签名；`ScreenEvent` 是 NeoForge 的公开契约，升级时只需要重新编译。

## 源码结构

```
neoforge-1.21.1/
├── build.gradle                 # ModDevGradle 2.0.148
├── gradle.properties            # 版本号与 mod 元数据（neoforge.mods.toml 由此展开）
├── settings.gradle
├── gradlew / gradle/wrapper/    # Gradle 9.2.1
└── src/main/
    ├── java/com/adeahejshshsh/singleplayeronly/
    │   ├── SinglePlayerOnly.java            # @Mod 入口
    │   └── client/ClientScreenGuard.java    # 两个事件处理器
    └── resources/
        ├── META-INF/neoforge.mods.toml      # modLoader=javafml
        ├── pack.mcmeta                      # pack_format 34
        └── icon.png                         # 128x128 图标
```

## 构建

需要 **JDK 21**，首次构建会下载 Minecraft/NeoForge 依赖并做 NeoForm 处理（耗时较长）：

```bash
cd neoforge-1.21.1
./gradlew build
```

产物：`build/libs/singleplayeronly-1.0.0.jar`，可直接丢进 `mods/`。
仓库里另放了一份构建好的 `dist/singleplayeronly-1.0.0.jar`，可直接使用。

开发环境调试：`./gradlew runClient`（`runServer` 也可用，便于验证打包内容）。

## 与 Forge 1.20.1 版的差异

功能与判定逻辑完全一致，差异只在加载器与版本 API：

| 项 | Forge 1.20.1 | NeoForge 1.21.1 |
|---|---|---|
| Java | 17 | 21 |
| 构建插件 | ForgeGradle 6 | ModDevGradle 2.0.148 |
| 混淆处理 | 需要 `reobfJar`（SRG 名） | 不需要，运行时直接用官方名 |
| 元数据文件 | `META-INF/mods.toml` | `META-INF/neoforge.mods.toml` |
| 依赖字段 | `mandatory=true` | `type="required"` |
| 事件订阅注解 | `@Mod.EventBusSubscriber(bus = FORGE)` | `@EventBusSubscriber(bus = GAME)` |
| 事件包名 | `net.minecraftforge.client.event` | `net.neoforged.neoforge.client.event` |
| 资源包格式 | `pack_format 15` | `pack_format 34` |
| 客户端限定 | `clientSideOnly=true` | 无此字段；服务端加载但不注册客户端事件 |

## 已验证 / 未验证

- `./gradlew build` → `BUILD SUCCESSFUL`，`compileJava` / `processResources` / `jar` 全部执行，无编译警告。
- 打包内容正确：`META-INF/neoforge.mods.toml` 展开为 `loaderVersion="[1,)"`、`license="MIT"`、
  依赖 `neoforge [21.1.0,)` 与 `minecraft [1.21.1]`（均为 `required` / `CLIENT`）；`pack_format 34`；图标在 jar 根目录。
- 字节码检查：类中 MC 成员引用是**官方名**（`AbstractWidget.visible/active/getMessage`、
  `TranslatableContents.getKey`、`Component.getContents`），NeoForge 侧为
  `ScreenEvent$Opening#getNewScreen/setNewScreen`、`ScreenEvent$Init$Post#getListenersList`；
  1.20.5+ 起不再需要 reobf。
- 运行时验证：官方 installer 装 **NeoForge 21.1.267 专用服务器**，把成品 jar 放进 `mods/` 启动：

  ```
  Found mod file "singleplayeronly-1.0.0.jar" [locator: {mods folder locator ...}, reader: mod manifest]
  Single Player Only 1.0.0 (singleplayeronly)
  [modloading-worker-0/INFO] [co.ad.si.SinglePlayerOnly/]: SinglePlayerOnly loaded: multiplayer functions blocked.
  Done (2.475s)! For help, type "help"
  ```

  没有 `NoClassDefFoundError`/`ClassNotFoundException`，说明 `Dist.CLIENT` 过滤生效、服务端不会被客户端类拖垮。

**未验证**：游戏内实际点击效果（构建环境没有图形界面，无法启动客户端）。所引用的类/方法/字段名均已用
Mojang 1.21.1 官方映射表与 NeoForge 21.1.267 的 jar 逐一核对，但「按钮消失、界面弹回标题」的实际表现
仍需在客户端里确认一次。

## 在 Android / Termux 上构建的已知问题

ModDevGradle 通过 NeoFormRuntime 处理 Minecraft 依赖，而 NFRT 2.0.31 用
`ProcessHandle.current().info().command()` 取 java 可执行文件路径。Termux 的 exec 拦截会把该值改写成
`/apex/com.android.runtime/bin/linker64`，导致 `createMinecraftArtifacts` 的 `patch` 步骤失败：

```
error: expected absolute path: "-jar"
```

桌面 / 普通 Linux 环境没有这个问题。若确实要在 Termux 上构建，可把
`~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoform-runtime/2.0.31/*/neoform-runtime-2.0.31-all.jar`
里 `ExternalJavaToolAction` 的 `ToolCoordinate` 构造器改为 `useHostJavaExecutable = false`
（把该处的 `iconst_1` 改成 `iconst_0`），NFRT 就会改用 Gradle 工具链的 java。本仓库的 `dist/` jar
就是修好后用 `./gradlew build` 构建的。

## 授权

本工程代码与资源以 **MIT** 授权，见 [LICENSE](LICENSE)。

- 图标 `src/main/resources/icon.png` 为本工程用 Java2D 程序化绘制的原创图形，无第三方素材。
- 版权行写的是 `The Single Player Only Authors`；如需署名到个人/组织，改 [LICENSE](LICENSE) 第一行即可。
- 同仓库还有 **Forge 1.20.1** 版本：[../forge-1.20.1/](../forge-1.20.1/README.md)（功能一致，仅加载器/版本不同）。
