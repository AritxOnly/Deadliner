# Deadliner UI 专项治理技术方案

## 1. 背景

本次专项治理的目标有四件事：

1. 统一并优化完整的色彩方案，解决 Material 3 与 MIUIX 混用时的颜色异常与组件显示异常。
2. 新增“应用显示大小”能力，让用户可以在“跟随系统”之外选择 5-7 档应用内显示尺寸。
3. 对 MIUIX 界面做完整调试和收口，使其达到正式版本质量。
4. 上线 iOS 已有的“季节图标”和“自定义图标”能力，在 Android 上提供稳定的一致体验。

这份方案以当前代码结构为基础，先解决“主题状态分裂”和“配置入口不统一”两个根因，再落功能和质量治理。

## 2. 当前实现现状

### 2.1 主题与界面风格存在双状态源

当前项目里，界面风格和设计系统不是同一套状态：

- 页面风格由 `GlobalUtils.style` 决定，取值来自 `UiStyle`。
  - 参考：[app/src/main/java/com/aritxonly/deadliner/model/UiStyle.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/model/UiStyle.kt)
  - 参考：[app/src/main/java/com/aritxonly/deadliner/MainActivity.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/MainActivity.kt)
- 主题设计系统由 `GlobalUtils.miuixMode` 决定，`DeadlinerTheme` 会在 Material 3 和 MIUIX 之间切换。
  - 参考：[app/src/main/java/com/aritxonly/deadliner/ui/theme/Theme.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/theme/Theme.kt)
- 颜色策略还受 `GlobalUtils.miuixColor` 和 `GlobalUtils.seedColor` 影响。
  - 参考：[app/src/main/java/com/aritxonly/deadliner/localutils/GlobalUtils.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/localutils/GlobalUtils.kt)
  - 参考：[app/src/main/java/com/aritxonly/deadliner/localutils/DynamicColorsExtension.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/localutils/DynamicColorsExtension.kt)

这会导致几个典型问题：

- `style=classic/simplified` 但 `miuixMode=true` 时，页面布局与设计系统可能跨体系组合。
- `UiStyle.Miuix` 与 `miuixMode` 在语义上重复，但又不是强绑定关系。
- 设置页和引导页同时暴露“风格选择”和“MIUIX 模式开关”，用户感知和开发维护都偏混乱。

### 2.2 主题桥接已经存在，但还停留在“补丁式映射”

当前 `DeadlinerTheme` 已经做了两件事：

- 用 MaterialKolor 生成 Material 3 色板。
- 在 MIUIX 模式下，将 Material 3 色值映射到 `MiuixTheme`，再反向映射回 `MaterialExpressiveTheme`。

这说明“融合两个方案”的方向是对的，但现在的问题是：

- 色板生成入口没有收敛成单一 token 源。
- MIUIX overlay 仍通过 `DynamicColorsExtension` 在 Activity theme 上额外打补丁。
- 一部分页面通过 `ui/base/*` 做了双实现，另一部分页面仍直接依赖 Material 3 颜色或组件。

### 2.3 风格切换的实时性和一致性还不够

`MainActivity` 当前通过 `remember { UiStyle.fromKey(GlobalUtils.style) }` 读取样式，这意味着样式切换后宿主不一定能完整响应。

- 参考：[app/src/main/java/com/aritxonly/deadliner/MainActivity.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/MainActivity.kt)

而 `GlobalUtils` 虽然已经有 `styleFlow`、`seedColorFlow`、`miuixModeFlow`，但还没有真正统一为“外观配置状态流”。

### 2.4 显示大小能力当前不存在统一入口

项目内目前没有对 `densityDpi`、`fontScale`、`attachBaseContext`、`applyOverrideConfiguration` 做应用级封装，说明“应用内显示大小”还没有基础设施。

这意味着如果直接加设置项，会遇到两个现实问题：

- Compose 页面和 XML/传统 Activity 页面必须一起生效。
- 旋转、多窗口、分屏、Widget 配置页等场景需要保持一致。

## 3. 目标定义

### 3.1 最终目标

1. 建立一套“单一外观状态模型”，统一界面风格、主题色策略、显示大小、应用图标策略。
2. 建立一套“单一色彩内核”，让 Material 3 与 MIUIX 都从同一份语义 token 派生。
3. 让 MIUIX 页面不再依赖零散补丁，成为稳定可回归、可验收、可发布的正式界面。
4. 建立一套“应用图标策略模型”，统一默认图标、季节图标、自定义图标和自动切换规则。

### 3.2 非目标

本轮不建议同时做以下扩张性工作：

- 重做全部视觉稿或品牌升级。
- 一次性重写所有经典 XML 页面为 Compose。
- 把“显示大小”和“字体大小”一起做成双维度设置。

本轮建议只做“应用内显示大小”，保留系统字体大小行为不变。

## 4. 总体技术方案

## 4.1 外观状态统一：建立 `AppearancePreferences`

建议新增统一的外观配置模型，替代当前分散在 `GlobalUtils` 中的 `style`、`miuixMode`、`miuixColor`、`seedColor`。

建议模型如下：

```kotlin
data class AppearancePreferences(
    val uiStyle: UiStyle,
    val colorSource: ColorSource,
    val displayScale: DisplayScalePreset,
    val usePureMiuixAccent: Boolean,
    val appIconMode: AppIconMode,
)
```

建议枚举如下：

```kotlin
enum class ColorSource {
    SystemDynamic,
    SeedColor,
}

enum class DisplayScalePreset {
    FollowSystem,
    Compact,
    SlightlyCompact,
    Default,
    SlightlyLarge,
    Large,
}

enum class AppIconMode {
    Default,
    SeasonalAuto,
    SeasonalSpring,
    SeasonalSummer,
    SeasonalAutumn,
    SeasonalWinter,
    Custom,
}
```

### 设计原则

- `UiStyle` 负责“页面风格/组件体系”。
- `ColorSource` 负责“颜色来源”。
- `usePureMiuixAccent` 只作为 MIUIX 风格下的高级选项，不再作为独立总开关。
- `AppIconMode` 负责“桌面图标策略”。
- `miuixMode` 建议退役，避免和 `UiStyle.Miuix` 重复。

### 预期收益

- 从根上消除“Material 布局 + MIUIX 主题”这种半耦合状态。
- 设置页和引导页可以共用一套配置结构。
- 以后扩展主题能力时不会继续堆全局布尔值。
- 图标功能不会再额外引入独立的偏好存储分支。

## 4.2 色彩架构统一：建立 `DeadlinerColorTokens`

建议把当前“Material 3 生成色板 -> MIUIX 映射 -> 再映射回 Material 3”的流程，收敛为：

1. 先生成一份平台无关的 `DeadlinerColorTokens`。
2. Material 3 和 MIUIX 分别从 token 派生各自的 `ColorScheme`。
3. 页面层只消费语义色，不直接感知底层来源。

建议结构：

```kotlin
data class DeadlinerColorTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
)
```

### 生成策略

- `SystemDynamic`：Android 12+ 优先取系统动态色，否则退回内置种子。
- `SeedColor`：继续使用 MaterialKolor 作为主生成器。
- `usePureMiuixAccent=true` 时，只覆盖主强调色来源，不绕开整套 token 生成。

### 关键调整

- `DynamicColorsExtension` 不再承担主要配色职责，只保留必要的系统兼容桥接。
- `ThemeOverlay_Deadliner_MiuixBackground`、`ThemeOverlay_Deadliner_MiuixDefaults` 从“业务逻辑补丁”降级为“遗留 View 兼容层”。
- `ui/base/*` 中的组件统一改为从 token 派生颜色，不直接散落读取 `GlobalUtils.miuixColor`。

## 4.3 显示大小方案：应用级 `densityDpi` 覆写

### 方案选择

显示大小建议通过“应用级 `Configuration.densityDpi` 覆写”实现，而不是用页面局部缩放。

原因：

- Compose 和 View 都能跟随 `Resources` 配置变化。
- 组件尺寸、间距、图标、弹窗、设置页、详情页可以统一响应。
- 比 `Modifier.scale()` 这类视觉缩放方式更稳定，也不会破坏点击区域和测量逻辑。

### 推荐实现方式

新增统一上下文包装器，例如：

```kotlin
object DisplayScaleManager {
    fun wrap(base: Context, preset: DisplayScalePreset): Context
    fun applyTo(configuration: Configuration, preset: DisplayScalePreset)
}
```

在所有 UI Activity 上统一接入：

- `attachBaseContext(newBase)`
- 必要时 `applyOverrideConfiguration`

建议进一步抽一个基类：

- `DeadlinerBaseActivity : AppCompatActivity`
- `DeadlinerBaseComposeActivity : ComponentActivity/AppCompatActivity`

把以下逻辑统一收口进去：

- 显示大小配置注入
- 动态主题/overlay 注入
- edge-to-edge 初始化

### 档位建议

用户侧建议保留 6 档：

1. 跟随系统
2. 更紧凑
3. 稍紧凑
4. 标准
5. 稍大
6. 更大

这里的文案是产品文案，底层映射为一组离散 `densityDpi` 倍率。倍率值不建议现在写死在视觉文案里，而是通过 QA 校准后固化。

### 兼容原则

- 只调整显示密度，不主动改 `fontScale`。
- Widget 不跟随应用内显示大小，继续跟随系统和 Launcher 环境。
- 分屏、多窗口、旋转时保持 preset 不丢失。

## 4.4 季节图标与自定义图标方案

### 方案选择

Android 侧建议采用 `activity-alias + PackageManager#setComponentEnabledSetting(...)` 实现动态图标切换。

原因：

- 这是 Android 上最稳定、最常见的 launcher icon 切换方案。
- 不需要改启动流程，不依赖私有 API。
- 可以同时支持“默认图标”“季节图标”“自定义图标”三类模式。

### 当前现状

当前 Manifest 只有单一 launcher 入口，还没有为备用图标建立别名：

- 应用图标来自 `android:icon="@mipmap/ic_launcher"`。
- Launcher 入口目前是单一 Activity，不是多 alias 结构。
  - 参考：[app/src/main/AndroidManifest.xml](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/AndroidManifest.xml)

这意味着要上线该功能，需要补一层标准 Android 图标切换基础设施。

### 推荐架构

建议新增以下模型：

```kotlin
enum class AppIconVariant {
    Default,
    Spring,
    Summer,
    Autumn,
    Winter,
    Custom1,
    Custom2,
}
```

建议新增以下组件：

- `AppIconManager`
  - 负责 alias 启停
  - 负责读取当前生效图标
  - 负责图标切换后的状态落盘
- `SeasonResolver`
  - 根据当前日期解析当前季节
  - 为 `SeasonalAuto` 提供目标图标
- `AppIconRepository`
  - 管理 `AppIconMode` 与当前 `AppIconVariant`

### Manifest 设计

建议保留一个真实启动 Activity，再为每个图标 variant 建一个 `activity-alias`：

- `LauncherAliasDefault`
- `LauncherAliasSpring`
- `LauncherAliasSummer`
- `LauncherAliasAutumn`
- `LauncherAliasWinter`
- `LauncherAliasCustom1`
- `LauncherAliasCustom2`

每个 alias 都指向同一个 `LauncherActivity`，差异只体现在：

- `android:icon`
- `android:roundIcon`
- `android:label`
- `intent-filter` 中的 `MAIN` / `LAUNCHER`

切换时保证任意时刻只有一个 alias 处于 enabled。

### 图标资源策略

建议每个变体都具备完整的 adaptive icon 资源：

- 前景层
- 背景层
- monochrome 层
- round icon

建议资源命名规范：

- `ic_launcher_default`
- `ic_launcher_spring`
- `ic_launcher_summer`
- `ic_launcher_autumn`
- `ic_launcher_winter`
- `ic_launcher_custom_1`

这样后续接品牌活动图标或联名图标也比较容易扩展。

### 季节自动切换策略

建议支持两种模式：

1. `SeasonalAuto`
2. 手动指定季节图标

`SeasonalAuto` 的推荐行为：

- App 冷启动时检查当前季节，如果目标图标和当前 alias 不一致则切换。
- `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` 后补做一次校正。
- 不强依赖定时任务，不为切图标单独引入周期性后台任务。

季节边界建议先按月份做离散映射：

- 3-5 月：春
- 6-8 月：夏
- 9-11 月：秋
- 12-2 月：冬

### 自定义图标策略

本期建议“自定义图标”限定为“官方预置图标集合”，而不是用户上传图片。

原因：

- Android Launcher 图标切换本质上依赖预定义 alias，不能像 iOS 一样直接传任意图。
- 用户上传图标会引入裁切、蒙版、分辨率、主题图标、monochrome、审核和缓存一致性问题。

因此本期建议产品表达为：

- 默认图标
- 季节图标
- 自定义图标
  - 这里的“自定义”指用户从官方提供的若干图标主题中自行选择

### 设置页设计建议

建议在外观设置中新增“应用图标”分组：

- 跟随季节自动切换
- 默认图标
- 春季图标
- 夏季图标
- 秋季图标
- 冬季图标
- 其他自定义官方图标

如果后续图标数量较多，建议单独拆一个二级页面，并配预览卡片。

### Android 平台注意事项

- 图标切换后桌面刷新存在 launcher 差异，部分系统会有轻微延迟。
- 某些 ROM 对 disabled component 的缓存较激进，需要避免短时间反复切换。
- 通知栏小图标不建议跟随 launcher icon 变更，继续保持稳定的通知资源。
- 快捷方式图标是否跟随主图标，需要单独评估是否同步更新。

## 4.5 MIUIX 正式版治理：组件、页面、回归三层收口

MIUIX 版本要成为正式版本，不能只改主题，还要把“页面实现 + 组件适配 + 视觉验收”一起拉通。

### 第一层：基础组件收口

优先治理这批双栈组件：

- `ui/base/Button.kt`
- `ui/base/Switch.kt`
- `ui/base/Checkbox.kt`
- `ui/base/Slider.kt`
- `ui/base/TopAppBar.kt`
- `ui/base/Scaffold.kt`
- `ui/base/OutlinedTextField.kt`
- `ui/base/AdaptiveNavigationSuiteScaffold.kt`
- `ui/base/AlertDialog.kt`
- `ui/base/TabRow.kt`

治理目标：

- 同一语义组件在 Material 3 / MIUIX 下尺寸、圆角、内边距、选中色、禁用态一致。
- 页面层不再写“如果是 MIUIX 就手动改颜色”的分支。

### 第二层：页面级专项调试

优先覆盖这些核心入口：

- 主界面
  - `SimplifiedHost`
  - `ModernHost`
  - `ClassicHost`
- 设置页
  - `AppearanceSettingsScreen`
  - `UiSettingsScreen`
  - `SettingsComponents`
- 添加/详情页
  - `AddDDLActivity`
  - `DeadlineDetailScreen`
- 概览/海报/AI 相关页面
  - `OverviewActivity`
  - `CaptureScreen`
  - `Poster`

治理重点：

- 背景层级是否正确。
- 容器色和强调色对比度是否足够。
- 输入框、搜索栏、弹窗、顶部栏在 MIUIX 下是否出现“色块不统一”。
- 选中态、禁用态、分割线、分组卡片在 MIUIX 下是否仍沿用 Material 3 假设。

### 第三层：验收矩阵

至少建立以下回归矩阵：

- 浅色 / 深色
- `UiStyle.Simplified` / `UiStyle.Classic` / `UiStyle.Miuix`
- 动态色 / 自定义种子色 / 纯澎湃强调色
- 显示大小 6 档
- 手机常规宽度 / 小屏高 DPI / 大屏分屏

## 5. 分阶段实施计划

## Phase 0：建模与止血

目标：先解决状态分裂问题，避免后续在错误模型上继续加代码。

输出：

- 新建 `AppearancePreferences` 与对应 repository/store。
- `GlobalUtils` 先保留兼容读写，但逐步改为代理到新 store。
- `MainActivity`、设置页、引导页改为观察统一状态流，不再各自读零散字段。
- 同步把 `appIconMode` 纳入统一外观配置。

关键改动：

- 去掉 `remember { UiStyle.fromKey(GlobalUtils.style) }` 这种只读一次的入口。
- `miuixMode` 不再作为公开主配置入口，迁移为派生或兼容字段。

## Phase 1：色彩内核统一

目标：建立单一 token 源，降低主题异常。

输出：

- `DeadlinerColorTokens`
- `MaterialColorSchemeFactory`
- `MiuixColorSchemeFactory`
- `DeadlinerTheme` 重构

关键改动：

- 收口 `Theme.kt`
- 弱化 `DynamicColorsExtension`
- 清理组件内直接依赖 `GlobalUtils.miuixColor` 的颜色分支

## Phase 2：显示大小基础设施

目标：让应用内显示大小具备可上线能力。

输出：

- `DisplayScalePreset`
- `DisplayScaleManager`
- Base Activity/Context wrapper
- 设置页显示大小入口

关键改动：

- 为所有 UI Activity 注入统一配置上下文
- 切换档位后统一 `recreate()` 当前任务栈中的页面
- 对 Compose 和 XML 页面各做一次兼容性检查

## Phase 3：应用图标能力上线

目标：补齐 Android 图标切换基础设施，并把季节/自定义图标接入设置页。

输出：

- `activity-alias` launcher 结构
- `AppIconManager`
- 图标模式设置页
- 季节自动切换逻辑

关键改动：

- 扩展 `AndroidManifest.xml`
- 增加图标资源全集
- 在启动时执行 alias 校正
- 在设置页提供图标预览与切换入口

## Phase 4：MIUIX 页面专项调试

目标：把 MIUIX 从“可用”提升为“正式版”。

输出：

- 基础组件问题清单与修复清单
- 核心页面问题清单与修复清单
- 每个页面的截图回归记录

建议执行方式：

- 先治理 `ui/base/*`
- 再治理设置页与主界面
- 最后治理表单、详情、弹窗、边角页面

## Phase 5：发布前收口

目标：让方案具备上线稳定性。

输出：

- 验收 checklist
- 回归测试记录
- 配置迁移策略
- 风险兜底开关

建议保留的兜底：

- MIUIX 纯色策略可保留高级开关或开发者开关
- 显示大小能力可在远期再扩展为实验性字体缩放，但本期不上

## 6. 配置迁移策略

旧配置到新配置建议如下：

- `style` -> `AppearancePreferences.uiStyle`
- `seed_color` -> `AppearancePreferences.colorSource/seed`
- `miuix_color` -> `AppearancePreferences.usePureMiuixAccent`
- `app_icon_mode` -> `AppearancePreferences.appIconMode`
- `miuix_mode` -> 迁移期兼容字段

迁移原则：

- 已选择 `style=miuix` 的用户，迁移后默认进入 MIUIX 风格。
- 已开启 `miuix_mode=true` 但 `style!=miuix` 的用户，迁移时优先保留页面风格，颜色策略按兼容逻辑映射。
- 新装用户不再看到“MIUIX Mode”这种底层开关文案，而是直接看到“界面风格”和“主题色策略”。

## 7. 风险与对策

### 风险 1：Compose 与 View 页面对显示大小响应不一致

对策：

- 用 `Configuration` 级方案，不做局部 scale。
- 在 Add/Edit/Settings 这类混合页面先做样板验证。

### 风险 2：MIUIX 颜色映射后对比度下降

对策：

- token 层单独做对比度检查。
- 对 `surfaceContainer`、`outlineVariant`、`onSurfaceVariant` 做人工校准，不完全依赖自动映射。

### 风险 3：配置切换后页面没有完整刷新

对策：

- 统一改为状态流驱动。
- 外观配置切换后触发 Activity `recreate()` 或任务级重建。

### 风险 4：旧配置兼容导致逻辑分叉长期保留

对策：

- 给兼容字段设迁移窗口。
- 完成两个版本发布后移除 `miuixMode` 旧逻辑。

### 风险 5：Android 桌面对 alias 图标刷新不及时

对策：

- 切换后只提示“桌面图标可能稍后刷新”。
- 不在短时间内连续自动切换。
- 自动模式只在启动或系统关键广播后校正一次。

## 8. 验收标准

满足以下条件时，认为专项治理完成：

1. 主题配置入口统一，不再同时暴露相互重叠的 `style` / `miuixMode` 概念。
2. Material 3 与 MIUIX 共用单一色彩 token 源，核心页面无明显颜色异常。
3. 应用显示大小支持“跟随系统 + 5 个固定档位”或“跟随系统 + 6 个固定档位”，切换后核心页面稳定。
4. 季节图标和自定义图标支持切换，切换后主流 Launcher 可稳定生效。
5. MIUIX 风格下，主界面、设置页、添加页、详情页、概览页均完成可视回归。
6. 深色模式、分屏、高 DPI 设备下无阻塞级显示问题。

## 9. 推荐的首批落地文件

建议优先改造这些文件：

- [app/src/main/java/com/aritxonly/deadliner/ui/theme/Theme.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/theme/Theme.kt)
- [app/src/main/java/com/aritxonly/deadliner/localutils/GlobalUtils.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/localutils/GlobalUtils.kt)
- [app/src/main/java/com/aritxonly/deadliner/localutils/DynamicColorsExtension.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/localutils/DynamicColorsExtension.kt)
- [app/src/main/java/com/aritxonly/deadliner/MainActivity.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/MainActivity.kt)
- [app/src/main/java/com/aritxonly/deadliner/ui/settings/AppearanceSettings.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/settings/AppearanceSettings.kt)
- [app/src/main/java/com/aritxonly/deadliner/ui/settings/UiSettings.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/settings/UiSettings.kt)
- [app/src/main/java/com/aritxonly/deadliner/ui/settings/SettingsComponents.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/settings/SettingsComponents.kt)
- [app/src/main/AndroidManifest.xml](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/AndroidManifest.xml)
- [app/src/main/java/com/aritxonly/deadliner/ui/base](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/base)

## 10. 建议的下一步

建议按下面顺序推进：

1. 先做 Phase 0，把外观状态统一建模。
2. 紧接着做 Phase 1，先把主题内核收口。
3. 然后上 Phase 2，把显示大小基础设施接进来。
4. 接着做 Phase 3，把季节图标和自定义图标能力接进统一外观配置。
5. 最后集中做 Phase 4 的 MIUIX 专项调试和回归。

这样做的好处是，后面的 MIUIX 调试不会继续建立在分裂状态和补丁色板上，返工会少很多。
