# AGENTS.md

本文件面向在本仓库工作的 AI 代理与协作者。开始改动前先读本文件；它描述了项目的目标、硬性约束、版本矩阵和验证方式。

## 1. 项目目标

**Leafora**（显示名；包名与仓库仍沿用 Leafra）：把纸质日记的扫描照片整理成一本可翻阅的电子书。

- 应用名（显示名）：`Leafora`（`app/src/main/res/values/strings.xml` 中的 `app_name`）
- 包名 / applicationId / namespace：`leafra.sitrmoo.com`（不要因为改名而修改）
- 当前阶段：**阶段 2.5 已完成（品牌视觉整合）**。书架、封面生成、日记本管理保持可用；官方 Logo 已接入启动图标与顶栏，主题统一为鼠尾草绿 + 奶油白。下一阶段为单页阅读器。
- 项目目录：`C:\Website\leafra`（Git 远程 `https://github.com/Sitrmoo/Leafra.git`，分支 `main`）

## 2. 硬性约束（不要做）

1. **不要添加 OCR、云同步、账号系统、在线 AI、广告或付费功能。**
2. **不要实现复杂翻页。** 当前只允许先做普通单页阅读；仿真立体翻页 / 3D 渲染必须等单页阅读稳定后另行讨论。
3. **不要新增存储或网络权限。** 选图只能走系统 Photo Picker（`ActivityResultContracts.PickVisualMedia` / `PickMultipleVisualMedia`），不申请 `READ_MEDIA_IMAGES`、`READ_EXTERNAL_STORAGE`、`CAMERA`、`INTERNET`。
4. **不要引入动态版本号**（`+`、`latest.release`、`SNAPSHOT`）。所有依赖版本必须显式固定。
5. **不要单独升级某个版本。** Gradle / AGP / Kotlin / Compose 是联动的一整套，见第 3 节；需要升级时整体评估并同步更新 README。
6. **不要提交**：`local.properties`、签名文件与密钥、`.env`、构建产物（`**/build/`、`.gradle/`、`*.apk`）、私人日记图片（`diary-images/`、`private-diaries/`）。
7. **不要重写整个项目。** 出问题时先定位根因，做最小修改。
8. 所有源文件、资源、注释使用 **UTF-8**，界面文案与注释使用中文。
9. **封面只能本地生成**（Compose 模板），禁止调用在线 AI、网络接口或 API Key；封面不得覆盖或改写任何原始日记照片。
10. **品牌资源以 `branding/leafora_icon.svg` 为准。** 不要自行绘制替代 Logo；更新图标时同步维护 `drawable/ic_launcher_foreground.xml`、`ic_launcher_monochrome.xml`、`ic_brand_mark.xml` 与 `branding/README.md`，并保持色板同步。

## 3. 版本矩阵（必须保持兼容）

| 组件 | 版本 | 约束 |
| --- | --- | --- |
| JDK | 17 (Temurin) | 本地与 CI 一致；AGP 8.7 要求 17 |
| Gradle | 8.9 | 由 wrapper 固定；AGP 8.7.3 最低要求 8.9 |
| Android Gradle Plugin | 8.7.3 | |
| Kotlin | 2.0.21 | |
| Compose Compiler | 2.0.21 | 通过 `org.jetbrains.kotlin.plugin.compose` 应用，**不要**再写 `composeOptions.kotlinCompilerExtensionVersion` |
| Compose BOM | 2024.10.01 | 统一 `androidx.compose.*` 版本，不要单独指定 Compose 依赖版本 |
| compileSdk / targetSdk | 34 | |
| minSdk | 26 | Android 8.0 |
| androidx.room:room-runtime / room-ktx | 2.6.1 | `minCompileSdk` 为 34；2.7+ 要求 compileSdk 35，不要单独升级 |
| androidx.room:room-compiler（KSP） | 2.6.1 | 由 KSP 生成代码 |
| KSP | 2.0.21-1.0.28 | 必须与 Kotlin 2.0.21 严格对应，升级 Kotlin 时同步升级 |

已固定的依赖：`androidx.core:core-ktx:1.13.1`、`androidx.lifecycle:lifecycle-runtime-ktx:2.8.6`、`androidx.activity:activity-compose:1.9.3`、`androidx.room:room-runtime:2.6.1`、`androidx.room:room-ktx:2.6.1`，加 Compose BOM（`ui`、`ui-graphics`、`ui-tooling-preview`、`material3`）与 `ksp("androidx.room:room-compiler:2.6.1")`。

Gradle Wrapper 完整性基线（改动 wrapper 时必须同步更新）：

- `gradle/wrapper/gradle-wrapper.jar` 应为官方 Gradle 8.9 版本，SHA-256：`498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17`
- `gradle-wrapper.properties` 中 `distributionSha256Sum`：`d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab`
- 不要用 `gradle wrapper` 覆盖，也不要删除 `distributionSha256Sum`。

## 4. 目录与代码组织

```text
.
├─ .github/workflows/android.yml   # CI：构建 Debug APK 并上传 Artifact
├─ app/
│  ├─ build.gradle.kts             # 模块配置与依赖（含 Room + KSP）
│  ├─ proguard-rules.pro
│  └─ src/main/
│     ├─ AndroidManifest.xml       # MainActivity + LeafraApplication，无任何用户可见权限
│     ├─ java/leafra/sitrmoo/com/
│     │  ├─ LeafraApplication.kt   # 数据库/仓库初始化、启动时清理孤立图片
│     │  ├─ MainActivity.kt        # 入口，setContent { LeafraTheme { HomeScreen(repository) } }
│     │  ├─ data/
│     │  │  ├─ DiaryRepository.kt  # 唯一数据入口：导入事务、回滚、引用计数、重命名/封面
│     │  │  ├─ ImageStore.kt       # 私有目录图片复制/删除/路径校验
│     │  │  ├─ local/              # Entities.kt、NotebookDao.kt、PageDao.kt、LeafraDatabase.kt
│     │  │  └─ model/              # Models.kt（Notebook / DiaryPage / CoverStyle / 结果类型）
│     │  └─ ui/
│     │     ├─ components/NotebookCover.kt  # 本地封面模板（配色、植物装饰、标题排版）
│     │     ├─ home/HomeScreen.kt           # 书架网格、新建/重命名/换封面/删除
│     │     ├─ home/ShelfTopBar.kt          # 固定顶栏：官方 Logo、搜索占位、菜单
│     │     └─ theme/              # Color.kt / Theme.kt / Type.kt（鼠尾草绿 + 奶油白）
│     └─ res/                      # strings、themes、自适应图标、ic_brand_mark 等
├─ branding/                       # leafora_icon.svg 官方 Logo 与品牌规范
├─ gradle/wrapper/
├─ build.gradle.kts                # 只声明插件版本并 apply false
├─ settings.gradle.kts             # rootProject.name = "Leafra"; include(":app")
├─ gradle.properties
├─ gradlew / gradlew.bat
└─ README.md
```

代码约定：

- UI 全部用 Kotlin + Jetpack Compose + Material 3，不引入 XML 布局。
- 所有用户可见文字放进 `res/values/strings.xml`，通过 `stringResource(...)` 引用，不在 Composable 里硬编码中文。
- 新界面放在 `leafra.sitrmoo.com.ui.<feature>` 下，主题色/字体只从 `ui/theme` 取。
- 新增依赖前先确认 Compose BOM 已覆盖；能不加就不加。

## 5. 构建与验证

前置条件：JDK 17；Android SDK Platform 34 + Build-Tools 34.0.0；`ANDROID_HOME` 指向 SDK，或根目录存在 `local.properties`（不提交）。

```powershell
# 项目根目录
.\gradlew.bat --version                 # 应报告 Gradle 8.9 + JVM 17
.\gradlew.bat :app:assembleDebug --stacktrace
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

- 必须用 wrapper，不要用系统 `gradle`。
- 首次构建需联网下载 Gradle 发行包与依赖。
- 本机已知噪音（非代码问题，可忽略）：
  - `Could not connect to Kotlin compile daemon` → 会自动回退到进程内编译，构建仍可成功；持续出现可执行 `.\gradlew.bat --stop` 后重试。
  - `Unable to strip the following libraries ... libandroidx.graphics.path.so` 属于打包提示。
  - `JAVA_HOME` 可能未设置，此时依赖 PATH 中的 `java`，请自行确认 `java -version` 为 17。

改动完成前至少要保证 `:app:assembleDebug` 通过；不要声称构建成功而没有实际执行。

## 6. CI

`.github/workflows/android.yml`：`push` 到 `main` 或手动 `workflow_dispatch` 触发，`ubuntu-latest` + JDK 17（Temurin）+ Gradle 缓存。

流程：校验 wrapper 文件存在 → `chmod +x ./gradlew` → `./gradlew --version` → `./gradlew :app:assembleDebug --stacktrace --no-daemon` → 上传 `app/build/outputs/apk/debug/app-debug.apk` 为 Artifact `leafra-debug-apk`（`if-no-files-found: error`，保留 30 天）。

修改 CI 时注意：

- Action 使用固定发布标签（`actions/checkout@v4.2.2`、`actions/setup-java@v4.3.0`、`actions/upload-artifact@v4.6.0`），不要改成 `@main`。
- 保留 `chmod +x ./gradlew`（Windows 下提交的 `gradlew` 可能没有可执行位）。
- Artifact 路径必须与 Gradle 实际输出一致。

## 7. 进度基线（2026-10-24 书架阶段核对）

已完成：

- Gradle Wrapper 8.9 完整（jar + properties + 两个启动脚本），校验和与官方一致。
- 应用显示名统一为 `Leafora`（`strings.xml` 的 `app_name`），包名 / applicationId / namespace 未变。
- 品牌视觉：官方 `branding/leafora_icon.svg` 已转换为自适应启动图标前景、Android 13 单色主题图标与首页顶栏图形；图标底色为奶油白。
- 主题：Material 3 色板统一为鼠尾草绿 + 奶油白（含 `surfaceContainer` 等角色），状态栏 / 导航栏固定深色图标。
- 固定顶栏：官方 Logo（`ic_brand_mark`）、搜索框占位、菜单（新建日记 / 收藏 / 设置 / 关于）；未实现项给出明确的“开发中”提示。
- 书架：`LazyVerticalGrid` 自适应列数，可滚动；封面 + 名称 + 页数，长名称最多两行省略。
- 本地封面：`ui/components/NotebookCover.kt` 提供 4 套模板（奶油白·鼠尾草 / 米杏·橄榄 / 雾蓝·灰绿 / 暖棕·干花），按名称实时绘制，不产生图片文件、不联网、不覆盖原始照片。
- 新建日记本：名称 + 预设封面 + Photo Picker 初始页面（最多 30 张）；导入失败会回滚文件并删除刚创建的日记本。
- 日记本管理：重命名、更换封面、删除（二次确认）；删除按引用计数清理图片。
- Room 版本 2：`notebooks.coverStyle`（默认 0），提供 1→2 显式 Migration，不重建数据库。
- 无存储/网络权限；Manifest 仅 `MainActivity` 一个导出组件。
- 本地 `:app:assembleDebug` 构建通过，产出 debug APK。

未实现（按顺序推进，不要跳步）：

1. 普通单页阅读模式，并接入 `setLastReadPage`。
2. 搜索逻辑（当前只有占位入口）。
3. 收藏与设置页面。
4. DataStore 阅读设置；Coil 图片加载。
5. 备份 / 导出 / 导入（另行讨论）。
6. 单页阅读稳定后再做仿真翻页。

明确不做：OCR、云同步、账号系统、在线 AI、广告、付费。

## 8. 提交前自检

- [ ] `:app:assembleDebug` 通过，且实际看到了 `BUILD SUCCESSFUL`。
- [ ] `AndroidManifest.xml` 没有新增权限，也没有新增导出组件（除 `MainActivity`）。
- [ ] 版本号没有被无意升级；Compose 依赖仍由 BOM 管理。
- [ ] 新文案在 `strings.xml` 中，且文件为 UTF-8。
- [ ] 没有把 `local.properties`、密钥、`*.apk`、`build/` 加入 Git。
- [ ] 封面仍由本地模板生成，没有新增网络 / API Key，也没有写入或覆盖图片文件。
- [ ] 启动图标仍由 `ic_launcher_foreground` / `ic_launcher_monochrome` 组成，顶栏 Logo 未在多个页面重复堆叠。
- [ ] 没有触碰第 2 节列出的禁止功能。
