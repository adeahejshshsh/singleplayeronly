# Single Player Only — Forge 1.20.1

一个**只允许单人游戏**的 Forge 客户端模组：标题界面不再提供「多人游戏 / Realms」入口，
多人游戏界面、Realms 界面、直连界面以及「对局域网开放」界面都无法打开，会被直接弹回标题界面。

- Minecraft **1.20.1**
- Forge **47.x**（在 47.4.28 上构建）
- 纯客户端（`clientSideOnly=true`），专用服务器会自动跳过本模组，也不要求服务器安装

## 实现方式

不使用 Mixin，全部基于 Forge 公开的 `ScreenEvent`（两个处理器，见
[ClientScreenGuard.java](src/main/java/com/adeahejshshsh/singleplayeronly/client/ClientScreenGuard.java)）：

| 目标 | 手段 |
|---|---|
| 多人游戏 / Realms 界面、直连界面、局域网开放界面 | `ScreenEvent.Opening`：命中则 `setNewScreen(new TitleScreen())` |
| 标题界面的「多人游戏」「Realms」按钮 | `ScreenEvent.Init.Post`：遍历控件，命中则 `visible=false, active=false` |
| 暂停界面的「对局域网开放」按钮 | 同上（避免单机存档被对外暴露） |
| 模组入口 | `@Mod("singleplayeronly")` + `@Mod.EventBusSubscriber(bus = FORGE, value = CLIENT)` |

界面识别用三重判定，抗改名、抗混淆：

1. `instanceof` 直接判断 `JoinMultiplayerScreen` / `ConnectScreen` / `ShareToLanScreen`；
2. 类名包含 `RealmsMainScreen` / `RealmsNotificationsScreen` / `ShareToLanScreen` / `OpenToLanScreen`
   （Realms 相关类不在 Forge 公开 API 内，且不同版本包名会变）；
3. 按钮识别优先比对翻译键 `menu.multiplayer` / `menu.online` / `menu.shareToLan`，
   再回退到文本包含 `multiplayer` / `realms` / `多人游戏` / `领域`（含繁体）。

**为什么不用 Mixin**：Mixin 方案要额外维护 `MixinConfigs` 清单属性、refmap 和注入点签名，
每次 MC 版本变动都要重新对签名；`ScreenEvent` 是 Forge 的公开契约，升级时只需要重新编译。

## 源码结构

```
forge-1.20.1/
├── build.gradle                 # ForgeGradle 6 + reobfJar
├── gradle.properties            # 版本号与 mod 元数据（mods.toml 由此展开）
├── settings.gradle
├── gradlew / gradle/wrapper/    # Gradle 8.8
└── src/main/
    ├── java/com/adeahejshshsh/singleplayeronly/
    │   ├── SinglePlayerOnly.java            # @Mod 入口
    │   └── client/ClientScreenGuard.java    # 两个事件处理器
    └── resources/
        ├── META-INF/mods.toml               # modLoader=javafml, clientSideOnly=true
        ├── pack.mcmeta                      # pack_format 15
        └── icon.png                         # 128x128 图标
```

## 构建

需要 **JDK 17**，首次构建会下载 Minecraft/Forge 依赖并做映射处理：

```bash
cd forge-1.20.1
./gradlew build
```

产物：`build/libs/singleplayeronly-1.0.0.jar`（已 reobf 到 SRG 名，可直接丢进 `mods/`）。
仓库里另放了一份构建好的 `dist/singleplayeronly-1.0.0.jar`，可直接使用。

开发环境调试：`./gradlew runClient`。

## 已验证 / 未验证

已在 aarch64 设备（Termux JDK 17 / Gradle 8.8）实际跑通：

- `./gradlew clean build` → `BUILD SUCCESSFUL`，`compileJava` / `processResources` / `jar` / `reobfJar` 全部执行。
- reobf 正确：开发名 `AbstractWidget.visible` / `.active` / `.getMessage()`、`TranslatableContents.getKey()`、
  `Component.getContents()` 在成品 jar 中已是 SRG 名 `f_93624_` / `f_93623_` / `m_6035_` / `m_237508_` / `m_214077_`；
  Forge 自身 API（`ScreenEvent.*`）保持原名。
- 真实 Forge 服务端加载：用官方 installer 安装 `forge-1.20.1-47.4.28` 专用服务器，把成品 jar 放入 `mods/` 启动，
  FML 正常扫描并解析 mods.toml，按 `clientSideOnly=true` 跳过：

  ```
  [main/WARN] [ne.mi.fm.lo.mo.ModDiscoverer/SCAN]: Locator {mods folder locator at .../mods}
  returned 1 files which are client-side-only mods, but we're on a dedicated server. They will be skipped!
  ```

  日志中无 mods.toml 解析错误，也无本模组相关异常。

**未验证**：游戏内实际点击效果（构建环境没有图形界面，无法启动客户端）。所引用的类/方法/字段名均已用
Mojang 1.20.1 官方映射表逐一核对，并通过编译与 reobf 校验，但「按钮消失、界面弹回标题」的实际表现
仍需在客户端里确认一次。

## 授权

本目录下的代码与资源以 **MIT** 授权，见 [LICENSE](LICENSE)。

- 图标 `src/main/resources/icon.png` 为本工程用 Java2D 程序化绘制的原创图形，无第三方素材。
- 版权行写的是 `The Single Player Only Authors`；如需署名到个人/组织，改 [LICENSE](LICENSE) 第一行即可
  （`Copyright (c) 2026 ...`）。
- 本仓库为独立仓库；git 历史从首次提交开始，不含第三方代码或素材。
  发布时如需托管，直接 `git remote add origin <url>` 后推送即可。
- 同仓库还有 **NeoForge 1.21.1** 版本：[../neoforge-1.21.1/](../neoforge-1.21.1/README.md)（功能一致，仅加载器/版本不同）。
