# Phase 1 色彩对齐设计

## 1. 目标

本阶段只解决一件事：把 Deadliner 的 Material 3 与 MIUIX 配色体系对齐到同一套语义模型上。

我们不在这一阶段大规模重写页面，也不先改所有组件，而是先把颜色来源、颜色角色和跨设计系统映射统一下来。这样后续做 MIUIX 正式版调试时，看到的将是“组件差异”而不是“颜色体系本身不一致”。

## 2. 参考文档

本设计主要参考以下文档：

- MIUIX Color System
  - [https://compose-miuix-ui.github.io/miuix/guide/colors](https://compose-miuix-ui.github.io/miuix/guide/colors)
- MIUIX Theme System
  - [https://compose-miuix-ui.github.io/miuix/guide/theme](https://compose-miuix-ui.github.io/miuix/guide/theme)
- 你给的 MaterialTheme 文档
  - [https://developer.android.com/reference/kotlin/androidx/compose/material/MaterialTheme](https://developer.android.com/reference/kotlin/androidx/compose/material/MaterialTheme)
- 实际用于本项目的 Material 3 文档
  - [https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialTheme](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialTheme)
  - [https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme](https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme)

说明：

- 你给的第三个链接是 `androidx.compose.material.MaterialTheme`，属于 Compose Material。
- 我们项目当前实际使用的是 `androidx.compose.material3.MaterialTheme.colorScheme` 与 `MaterialExpressiveTheme`，所以本设计同时以 Material 3 的 `MaterialTheme` / `ColorScheme` 为主参考。

## 3. 文档结论摘要

### 3.1 MIUIX 的核心结论

根据 Miuix 文档：

- 主题通过 `MiuixTheme` 提供颜色和文本体系。
- 可以通过 `ThemeController` 选择 `System`、`Light`、`Dark`、`MonetSystem` 等模式。
- 可以直接传 `colors = ...`，不必强依赖 `ThemeController`。
- `MiuixTheme.colorScheme` 里已有完整的背景、表面、容器、分割线、禁用态等颜色角色。

对我们最重要的是：MIUIX 颜色角色比“只提供主色”更完整，尤其是这些角色有明确语义：

- `primary`
- `primaryContainer`
- `secondary`
- `secondaryContainer`
- `tertiaryContainer`
- `background`
- `surface`
- `surfaceVariant`
- `surfaceContainer`
- `surfaceContainerHigh`
- `surfaceContainerHighest`
- `outline`
- `dividerLine`
- `windowDimming`
- `onSurfaceVariantSummary`
- `onSurfaceVariantActions`

这意味着我们不应该把 MIUIX 仅仅当作“另一套主色蓝”，而应该把它当成完整的语义色系统。

### 3.2 Material 3 的核心结论

根据 Material 3 文档：

- `MaterialTheme.colorScheme` 是页面层唯一应该消费的颜色入口。
- `ColorScheme` 是一整套具名语义角色，而不是只有主色。
- 新版 `ColorScheme` 已包含更细的 surface 系列角色：
  - `surfaceContainer`
  - `surfaceContainerHigh`
  - `surfaceContainerHighest`
  - `surfaceContainerLow`
  - `surfaceContainerLowest`
  - `surfaceBright`
  - `surfaceDim`
- 还有 fixed 色系：
  - `primaryFixed`
  - `primaryFixedDim`
  - `secondaryFixed`
  - `tertiaryFixed`
  - 以及对应 `on*Fixed` 角色

对我们最重要的是：Material 3 的 `ColorScheme` 已经足够细，可以成为 Deadliner 内部的“统一语义色模型”。

## 4. 当前项目现状

当前 [Theme.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/theme/Theme.kt) 的实现是：

1. 先生成一份 Material 3 色板。
2. 在 MIUIX 模式下，把这份 M3 色板映射成 `MiuixTheme(colors = ...)`。
3. 再把 MIUIX 色板反向映射回 `MaterialExpressiveTheme(colorScheme = ...)`。

这个方向是对的，但当前实现还有四个问题：

1. 缺少独立的“内部语义 token 层”，所以现在的映射发生在框架对象之间，而不是在语义对象之间。
2. 映射规则部分是“字段对字段”直连，部分是补丁式推导，不够系统。
3. `DynamicColorsExtension` 还在 Activity 级 theme overlay 上做额外补丁，容易和 Compose 主题形成双源。
4. 页面层仍有零散逻辑直接依赖 `GlobalUtils.miuixColor`，说明颜色语义还没完全下沉到主题层。

当前第一批最值得直接治理的热点点位有：

- [Theme.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/theme/Theme.kt)
  - 现在既负责颜色来源选择，也负责 M3 -> MIUIX 映射，还负责 MIUIX -> M3 反向映射，职责过重。
- [DynamicColorsExtension.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/localutils/DynamicColorsExtension.kt)
  - 现在还会基于 `miuixMode + miuixColor` 在 Activity theme 上叠加 overlay，容易形成 View/Compose 双源。
- [SettingsComponents.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/settings/SettingsComponents.kt)
  - 现在仍然直接读取 `GlobalUtils.miuixColor` 决定容器色和文本色，说明组件颜色语义还没有完全回收到主题层。
- [AppColorScheme.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/model/AppColorScheme.kt)
  - 这是经典页面桥接色模型，字段比新版 Material 3 `ColorScheme` 稀疏，后续需要评估是否扩充或逐步退役。

## 5. Phase 1 的设计原则

本阶段采用以下原则：

### 5.1 单一真实来源

Deadliner 内部只能有一份“真实语义色模型”。

建议新增：

```kotlin
data class DeadlinerColorTokens(...)
```

后续规则是：

- Material 3 主题由 `DeadlinerColorTokens` 派生。
- MIUIX 主题也由 `DeadlinerColorTokens` 派生。
- 页面只认 `MaterialTheme.colorScheme` 和 `MiuixTheme.colorScheme`，不再直接拼全局布尔值。

### 5.2 Material 3 作为内部标准语义集

内部 token 命名以 Material 3 `ColorScheme` 为骨架。

原因：

- 我们项目 Compose 页面大量直接消费 `MaterialTheme.colorScheme.*`。
- Material 3 的 surface/container 角色已经比 MIUIX 文档公开角色更细。
- 即使页面最终是 MIUIX 组件，也可以从内部标准语义集向外派生。

换句话说：

- Material 3 `ColorScheme` 不是最终视觉标准。
- 但它是最合适的内部语义骨架。

### 5.3 MIUIX 不是“特殊分支”，而是“另一种渲染视图”

MIUIX 风格下不应维护第二套独立配色逻辑。

正确做法是：

- 同一份 token
- 一套 M3 派生规则
- 一套 MIUIX 派生规则

而不是：

- M3 一套逻辑
- MIUIX 再单独硬编码一套逻辑
- MIUIX 生成完以后再反推一套新的 Material 3 颜色

另外，页面布局风格和主题系统要明确拆开：

- `UiStyle` 只负责页面布局风格。
- MIUIX 主题能力是独立偏好，不再等价于 `UiStyle.Miuix`。
- 当前治理目标调整为：
  - `Classic` 固定使用 Material 主题。
  - `Simplified` 和 `Miuix` 两种页面风格都允许挂 Material 或 MIUIX 主题。

### 5.4 纯澎湃色模式只改强调色来源，不改语义结构

当前 `miuixColor` 的产品语义更接近“使用澎湃默认强调色”。

因此 `usePureMiuixAccent = true` 时应做的是：

- 主强调色来源切换到 MIUIX 标准主色
- 其他容器、表面、错误色、边框色仍通过统一 token 规则生成

不应该做的是：

- 直接完全绕开统一 token 体系

### 5.5 保留 MIUIX 默认的中性表面体系

MIUIX 默认主题里更有价值的不只是主蓝，还有那套更中性的：

- `background`
- `surface`
- `surfaceContainer`
- `surfaceContainerHigh`
- `surfaceContainerHighest`
- `outline`

这套中性表面色能显著减少 Material Tonal 容器在部分页面上的“染色感”，也更符合当前产品对 MIUIX 主题的预期。

因此 Phase 1 调整为：

- MIUIX 主题下默认保留 MIUIX 风格的中性 surface/background/container。
- 强调色仍可以来自系统动态色、自定义 seed，或者“纯澎湃色”。
- 如果后续需要更细控制，可以再把“MIUIX 强调色”和“MIUIX 中性表面色”拆成两个开关。

## 6. 统一颜色角色设计

建议 Phase 1 先定义如下 token 集：

```kotlin
data class DeadlinerColorTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,

    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    val background: Color,
    val onBackground: Color,
    val onBackgroundVariant: Color,

    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val onSurfaceVariantSummary: Color,
    val onSurfaceVariantActions: Color,

    val surfaceContainer: Color,
    val onSurfaceContainer: Color,
    val onSurfaceContainerVariant: Color,
    val surfaceContainerHigh: Color,
    val onSurfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onSurfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,

    val outline: Color,
    val dividerLine: Color,
    val scrim: Color,

    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
)
```

说明：

- 这不是最终 API，只是本阶段推荐骨架。
- 它保留了 Material 3 的主要 surface 结构。
- 同时保留了 MIUIX 文档里几个很有价值、Material 3 未直接等名暴露的语义：
  - `dividerLine`
  - `onBackgroundVariant`
  - `onSurfaceVariantSummary`
  - `onSurfaceVariantActions`

## 7. M3 与 MIUIX 对齐映射

## 7.1 一对一直接映射

以下角色应该尽量直接对齐：

| 内部 Token | Material 3 | MIUIX |
| --- | --- | --- |
| `primary` | `primary` | `primary` |
| `onPrimary` | `onPrimary` | `onPrimary` |
| `primaryContainer` | `primaryContainer` | `primaryContainer` |
| `onPrimaryContainer` | `onPrimaryContainer` | `onPrimaryContainer` |
| `secondary` | `secondary` | `secondary` |
| `onSecondary` | `onSecondary` | `onSecondary` |
| `secondaryContainer` | `secondaryContainer` | `secondaryContainer` |
| `onSecondaryContainer` | `onSecondaryContainer` | `onSecondaryContainer` |
| `background` | `background` | `background` |
| `onBackground` | `onBackground` | `onBackground` |
| `surface` | `surface` | `surface` |
| `onSurface` | `onSurface` | `onSurface` |
| `surfaceVariant` | `surfaceVariant` | `surfaceVariant` |
| `outline` | `outline` | `outline` |
| `error` | `error` | `error` |
| `onError` | `onError` | `onError` |
| `errorContainer` | `errorContainer` | `errorContainer` |
| `onErrorContainer` | `onErrorContainer` | `onErrorContainer` |

## 7.2 语义近似映射

以下角色需要语义映射，而不是字面映射：

| 内部 Token | Material 3 | MIUIX |
| --- | --- | --- |
| `inversePrimary` | `inversePrimary` | `primaryVariant` |
| `tertiary` | `tertiary` | `secondaryVariant` |
| `onTertiary` | `onTertiary` | `onSecondaryVariant` |
| `tertiaryContainer` | `tertiaryContainer` | `tertiaryContainer` |
| `onTertiaryContainer` | `onTertiaryContainer` | `onTertiaryContainer` |
| `onSurfaceVariant` | `onSurfaceVariant` | `onSurfaceContainerVariant` 或 `onSurfaceVariantSummary` |
| `surfaceContainer` | `surfaceContainer` | `surfaceContainer` |
| `surfaceContainerHigh` | `surfaceContainerHigh` | `surfaceContainerHigh` |
| `surfaceContainerHighest` | `surfaceContainerHighest` | `surfaceContainerHighest` |
| `surfaceContainerLow` | `surfaceContainerLow` | `surface` |
| `surfaceContainerLowest` | `surfaceContainerLowest` | `background` |
| `surfaceBright` | `surfaceBright` | `surfaceVariant` |
| `surfaceDim` | `surfaceDim` | `surfaceContainerHigh` |
| `dividerLine` | `outlineVariant` | `dividerLine` |
| `scrim` | `scrim` | `windowDimming` |

## 7.3 不建议在本阶段强行对齐的角色

Material 3 新版 `ColorScheme` 还有：

- `primaryFixed`
- `primaryFixedDim`
- `onPrimaryFixed`
- `onPrimaryFixedVariant`
- `secondaryFixed`
- `tertiaryFixed`

MIUIX 当前公开文档没有完全对等的 fixed 家族语义。

因此本阶段建议：

- 先从主 token 推导这些 fixed 角色
- 不把它们作为 MIUIX 独立真实来源
- 如果项目内暂时没大量使用，可以先用保守派生值

## 8. 颜色来源策略

Phase 1 建议只保留三种来源：

### 8.1 系统动态色

使用条件：

- Android 12+
- `AppearanceColorSource = SystemDynamic`
- 没有显式 seed color

实现来源：

- Material 3 使用 `dynamicLightColorScheme` / `dynamicDarkColorScheme`
- MIUIX 可使用 `ThemeController` Monet 模式，或者继续通过统一 token 映射

本项目建议：

- 先继续用 MaterialKolor / 系统动态色生成统一 token
- 不在这一阶段把 `ThemeController(MonetSystem)` 作为主线路切入

原因：

- 我们已经有稳定的 M3 -> MIUIX / MIUIX -> M3 映射骨架
- 先统一 token 比先切换生成器更稳

### 8.2 自定义种子色

使用条件：

- `AppearanceColorSource = SeedColor`
- `seedColorHex != null`

实现来源：

- 继续使用 MaterialKolor 生成统一 token

### 8.3 纯澎湃强调色

使用条件：

- `usePureMiuixAccent = true`

实现策略：

- 只覆盖 key color 为 MIUIX 默认主蓝
- 后续 token 仍走统一生成流程

不建议做：

- 直接跳过统一 token，手工灌满一整套颜色

## 9. 当前代码需要的改造顺序

## Step 1：抽 `DeadlinerColorTokens`

先从 [Theme.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/theme/Theme.kt) 中把当前颜色生成和映射拆成三个职责：

1. `DeadlinerColorTokenFactory`
2. `DeadlinerMaterial3ColorSchemeFactory`
3. `DeadlinerMiuixColorSchemeFactory`

## Step 2：Theme 只做组装，不做复杂推导

`DeadlinerTheme` 最终应该只做：

1. 读取 `appearanceFlow`
2. 生成 `DeadlinerColorTokens`
3. 派生 `Material 3 ColorScheme`
4. 派生 `MIUIX Colors`
5. 注入主题

而不是在 Composable 函数体里塞大量映射细节。

特别说明：

- MIUIX 模式下，`MaterialExpressiveTheme` 也应该直接使用同一份 token 派生出的 Material 3 `ColorScheme`。
- 不再保留“先生成 MIUIX Colors，再从 MIUIX Colors 反向映射回 Material 3”的链路。
- 这样 Compose 页面里的 Material 3 组件和 MIUIX 组件才会真正共享同一份语义色，而不是共享一份临时桥接结果。

## Step 3：把组件里的“颜色分支”回收到主题层

优先清理这类逻辑：

- `if (GlobalUtils.miuixColor) ... else ...`
- `if (GlobalUtils.miuixMode) ... else ...`

其中布局差异可以保留，但颜色差异应优先收口。

重点文件：

- [SettingsComponents.kt](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/settings/SettingsComponents.kt)
- [ui/base](/Users/aritxonly/Codes/Android/Deadliner/app/src/main/java/com/aritxonly/deadliner/ui/base)

## Step 4：缩减 `DynamicColorsExtension` 责任

当前 `DynamicColorsExtension` 还在做：

- 动态色 source 注入
- MIUIX background overlay
- MIUIX 默认色 overlay

Phase 1 完成后建议它只保留：

- 遗留 View 主题兼容桥接

不要再承担核心颜色生成职责。

这一步不要求一次性删掉所有 overlay，但要满足一个原则：

- Compose 主题颜色由 `DeadlinerColorTokens` 决定。
- overlay 只能补 View 体系兼容，不再反向决定 Compose 主题应该长什么样。

## 10. Phase 1 完成标准

满足以下条件时，可认为 Phase 1 完成：

1. `Theme.kt` 中存在明确的 token 层，不再直接在 M3 / MIUIX 对象之间来回硬映射。
2. `MaterialExpressiveTheme` 与 `MiuixTheme` 都来自同一份 `DeadlinerColorTokens`。
3. 主题层不再直接依赖零散 `GlobalUtils.seedColor`、`GlobalUtils.miuixColor` 组合逻辑，而是改读统一外观状态。
4. 设置页和基础组件中的主要颜色判断不再散落读取 `GlobalUtils.miuixColor`。
5. 浅色 / 深色 / 自定义 seed / 纯澎湃强调色四种组合下，核心页面主色、表面色、容器色、分割线语义一致。

## 11. 下一步开发建议

文档确认后，建议直接开始做以下代码改造：

1. 在 `ui/theme` 下新增 `DeadlinerColorTokens.kt`
2. 新增 `DeadlinerColorTokenFactory.kt`
3. 新增 `DeadlinerMaterial3ColorSchemeFactory.kt`
4. 新增 `DeadlinerMiuixColorSchemeFactory.kt`
5. 重构 `Theme.kt`
6. 清理 `SettingsComponents.kt` 中第一批颜色分支

这会是 Phase 1 最稳的落地顺序。
