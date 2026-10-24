# Leafora

Leafora 是一个用于把**纸质日记电子化**的 Android 应用。你可以从手机相册导入日记扫描图片，把它们整理成一本电子书，并逐步获得接近纸质书的阅读体验。

- 应用名（显示名）：**Leafora**
- 包名 / applicationId：`leafra.sitrmoo.com`（保持不变）
- 仓库：`https://github.com/Sitrmoo/Leafra.git`

本仓库当前处于**搜索与设置阶段**：顶部可搜索日记本名称、页面描述与日期，收藏页与设置页可用；主题支持亮色 / 深色 / 跟随系统并持久化，页面日期显示可开关。立体书与单页阅读保持可用；高级纸张弯曲与备份尚未实现。

## 当前已实现

- Kotlin + Jetpack Compose 原生 Android 项目骨架，应用显示名为 **Leafora**。
- 品牌视觉：鼠尾草绿 + 奶油白纸张色系，官方叶片 Logo 已用于自适应启动图标、Android 13 单色主题图标与首页顶栏。
- 固定顶栏：官方 Logo 在左、可输入的搜索框居中、菜单在右（新建日记、收藏、设置、关于）。
- 顶部搜索：按日记本名称、页面描述、页面日期匹配；结果横向排列缩略图、日记本名称、页码、日期与描述；点击打开对应日记本并定位页面。
- 收藏列表：只收藏单页，与搜索结果共用布局；阅读模式与收藏列表均可收藏 / 取消收藏；页面删除后自动从列表消失。
- 设置：亮色、深色、跟随系统三种主题；页面日期显示开关（只隐藏显示，不删除已保存日期）；设置通过 DataStore 持久化。
- 关于页：展示应用名称 Leafora、当前版本与本地隐私说明。
- 书架网格：封面 + 名称 + 页数，`LazyVerticalGrid` 自适应列数，数量增多可滚动；长名称最多两行省略。
- 新建日记本：填写名称、选择预设封面风格，并通过系统 **Photo Picker** 多选初始页面（最多 30 张）。
- 本地生成封面：按名称与预设模板（奶油白纸张、鼠尾草绿、植物装饰、标题排版）用 Compose 实时绘制，不联网、不写图片文件、不覆盖原始照片。
- 日记本管理：重命名、更换封面、删除（二次确认）；删除时按引用计数清理图片。
- 图片复制到应用私有目录 `files/diary-images/`，数据库只保存相对路径；重启应用后书架与页面仍存在。
- 打开日记本：默认进入单页阅读；阅读模式提供「立体 / 单页」切换，编辑模式提供「立体 / 单页 / 大纲」。
- 阅读界面：顶部显示官方 Logo、日记本名称与菜单（编辑 / 返回书架）；单页显示，支持左右滑动或前后箭头翻页，底部显示当前页码与总页数。
- 单页阅读支持双指 1x–4x 缩放，放大后单指拖动查看细节；放大期间禁用翻页，缩小或切换页面时自动复位。
- 立体书：使用真实页面图片作为纸面内容，不重绘或修改原始照片；包含封面、书脊、纸张厚度、书底与书脊阴影，以及轻微透视。
- 跨页规则：第 1 页位于右侧（左侧显示封面），之后每两页组成一个跨页；最后一页单独显示时另一侧为封底；空白占位页正常显示为空白纸张。
- 立体书左右翻页使用 `rotationY` + `cameraDistance` 的 180° 轻量动画（650ms 缓动，拖动跟随手指，松手后吸附完成或回退）；纸背显示纸张底色，暂不映射下一张扫描图。
- 支持左右滑动翻页与箭头翻页；点击立体书左页或右页进入对应单页，单页以缩放 + 淡化聚焦进入，返回立体书时收缩退出。
- 从单页切回立体书时按页面 ID 定位所在跨页；双指 0.8x–1.5x 缩放围绕中心，不改变跨页内的页面位置关系。
- 阅读模式仅允许收藏 / 取消收藏当前页；删除、排序、描述、日期、替换图片仍只在编辑模式可用。
- 阅读进度按日记本保存：`notebooks.lastReadPageId` 记录页面 ID，重新打开时恢复到该页；页面被删除时清除进度，排序变化后仍能定位原页面。
- 大纲视图：按顺序展示所有页面缩略图；拖动手柄或长按缩略图调整顺序，松手后持久化到数据库。
- 页面编辑：追加多张图片、新建空白占位页、替换占位图片、删除页面、收藏、编辑描述、使用系统原生日期选择器设置或清除日期。
- 页码按当前顺序从 1 连续编号；日期、描述、收藏跟随稳定页面 UUID，排序后不会错位。
- 删除页面时按引用计数清理图片：只有不再被其它页面或封面引用时才删除文件。
- 图片加载失败、超大图下采样、快速连续翻页、零页面均有兜底。
- GitHub Actions 云端构建 Debug APK，并上传构建产物。
- 完整的 Gradle Wrapper（Gradle 8.9）。

## 当前尚未实现

- 真实纸张弯曲 / 高级 3D 变形（当前为轻量 rotationY 翻页；纸背未映射下一张扫描图）。
- 本地备份 / 导出 / 导入。
- Coil 大图加载与沉浸式阅读打磨（当前使用 `BitmapFactory` 下采样）。
- 云同步、OCR、在线 AI、广告或付费功能。
- 正式签名与 Google Play 发布配置。


## 技术栈与版本

所有版本都在构建脚本中明确固定，不使用动态版本号（如 `+` 或 `latest.release`）。

| 组件 | 固定版本 | 说明 |
| --- | --- | --- |
| JDK | 17（Temurin） | 本地与 GitHub Actions 一致 |
| 应用名（显示名） | Leafora | `strings.xml` 中的 `app_name`；包名 / applicationId 不变 |
| 包名 / applicationId / namespace | `leafra.sitrmoo.com` | 源码目录为 `java/leafra/sitrmoo/com/` |
| Gradle | 8.9 | 由 `gradle/wrapper/gradle-wrapper.properties` 固定，Wrapper JAR 由官方 Gradle 8.9 生成 |
| Android Gradle Plugin | 8.7.3 | 要求 Gradle 8.9+、JDK 17 |
| Kotlin | 2.0.21 | 与 Compose Compiler 插件同版本 |
| Compose Compiler | 2.0.21 | 使用 `org.jetbrains.kotlin.plugin.compose` |
| Compose BOM | 2024.10.01 | 统一 Compose UI / Material3 版本 |
| compileSdk / targetSdk | 34 | 本机 SDK 已安装 API 34 |
| minSdk | 26 | Android 8.0，覆盖绝大多数在用设备 |
| androidx.core:core-ktx | 1.13.1 | |
| androidx.activity:activity-compose | 1.9.3 | 提供 Photo Picker 与 Compose 集成 |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.8.6 | |
| Material 3 | 由 Compose BOM 管理 | 不额外引入大型 UI 库 |
| androidx.room:room-runtime / room-ktx | 2.6.1 | 最低 compileSdk 34，与项目一致；2.7+ 要求 compileSdk 35 |
| androidx.room:room-compiler（KSP） | 2.6.1 | 注解处理器，通过 KSP 生成代码 |
| KSP | 2.0.21-1.0.28 | 必须与 Kotlin 2.0.21 严格对应 |
| androidx.datastore:datastore-preferences | 1.1.1 | 本地设置持久化；不涉及网络 |

版本兼容关系（升级时请整体考虑，不要只改其中一个）：

- AGP 8.7.3 ↔ Gradle 8.9 ↔ JDK 17
- Kotlin 2.0.21 ↔ Compose Compiler 插件 2.0.21 ↔ KSP 2.0.21-1.0.28
- Compose BOM 2024.10.01 中的依赖均兼容 compileSdk 34 / minSdk 26
- Room 2.6.1 的 `minCompileSdk` 为 34，升级 compileSdk 之前不要单独升级 Room

## 目录结构

```text
.
├─ .github/workflows/android.yml        # GitHub Actions 云端构建
├─ app/
│  ├─ build.gradle.kts                  # App 模块构建配置与依赖
│  ├─ proguard-rules.pro
│  └─ src/main/
│     ├─ AndroidManifest.xml            # 只有 MainActivity，无任何用户可见权限
│     ├─ java/leafra/sitrmoo/com/
│     │  ├─ LeafraApplication.kt        # 数据库/仓库/设置初始化与孤立文件清理
│     │  ├─ MainActivity.kt             # 导航、主题、系统栏、设置注入
│     │  ├─ data/
│     │  │  ├─ DiaryRepository.kt       # 数据入口：事务、回滚、引用计数、搜索投影
│     │  │  ├─ ImageStore.kt            # 私有目录图片复制/删除
│     │  │  ├─ settings/                # DataStore 设置（主题、日期显示）
│     │  │  ├─ local/                   # Room 实体、DAO、Database（含 1→2 迁移）
│     │  │  └─ model/                   # Notebook / DiaryPage / PageResult 等
│     │  └─ ui/
│     │     ├─ about/AboutScreen.kt     # 关于页
│     │     ├─ components/NotebookCover.kt  # 本地生成封面模板
│     │     ├─ components/PageImage.kt      # 页面图片 / 空白页占位
│     │     ├─ components/FavoriteStar.kt   # 收藏星标
│     │     ├─ components/PageResultRow.kt  # 搜索/收藏共用横向条目
│     │     ├─ components/SimpleTopBar.kt   # 二级页面顶栏
│     │     ├─ favorites/FavoritesScreen.kt # 收藏列表
│     │     ├─ home/HomeScreen.kt       # 书架、搜索、新建/管理对话框
│     │     ├─ home/ShelfTopBar.kt      # 顶栏：Logo、搜索框、菜单
│     │     ├─ home/SearchFilter.kt     # 名称/描述/日期匹配
│     │     ├─ notebook/NotebookScreen.kt   # 阅读/编辑、三视图、页面弹窗
│     │     ├─ notebook/BookSpreadView.kt   # 立体书跨页、透视与翻页动画
│     │     ├─ notebook/OutlineView.kt      # 大纲缩略图与拖动排序
│     │     ├─ notebook/ReaderPage.kt       # 单页图片缩放与翻页箭头
│     │     ├─ notebook/PageEditDialog.kt   # 描述/日期/收藏/替换/删除
│     │     ├─ settings/SettingsScreen.kt   # 主题与日期设置
│     │     ├─ settings/SettingsLocals.kt   # 日期显示 CompositionLocal
│     │     └─ theme/                   # 亮色/深色配色与字体
│     └─ res/                           # 文案、主题、自适应图标与 Logo 矢量
├─ branding/                            # 官方 Logo 源文件（SVG）与品牌规范
├─ gradle/wrapper/                      # Gradle Wrapper（含官方 JAR）
├─ build.gradle.kts                     # 根构建脚本（插件版本）
├─ settings.gradle.kts
├─ gradle.properties
├─ gradlew / gradlew.bat
├─ .gitignore
└─ README.md
```

## 通过 GitHub Actions 构建 APK

本项目**不要求**你在低配置电脑上安装或运行 Android Studio。推荐流程是：把代码推到 GitHub，由云端构建 APK，手机直接下载安装。

### 1. 仓库与远程地址

本项目已关联：

```text
https://github.com/Sitrmoo/Leafra.git
```

### 2. 推送本地修改

在项目目录打开 PowerShell：

```powershell
cd C:\Website\leafra
git add .
git commit -m "feat: add bookshelf and local covers"
git push origin main
```

如果是从其他电脑重新克隆：

```powershell
git clone https://github.com/Sitrmoo/Leafra.git
cd Leafra
```

如果远程仓库已有新提交，先执行：

```powershell
git pull --rebase origin main
```

再推送。

### 3. 自动构建

推送到 `main` 分支后，工作流 `.github/workflows/android.yml` 会自动运行：

1. 用 `actions/checkout` 检出代码。
2. 安装 JDK 17（Temurin）。
3. 检查 Gradle Wrapper 文件是否存在，并给 `gradlew` 添加执行权限。
4. 执行 `./gradlew :app:assembleDebug --stacktrace --no-daemon`。
5. 把 `app/build/outputs/apk/debug/app-debug.apk` 上传为 Artifact。

也可以在 GitHub 页面手动触发：

**Actions → Build Leafra Debug APK → Run workflow → 选择 main → Run workflow**

### 4. 下载 APK

构建成功后：

1. 打开 **Actions** 页面，点击最新的绿色构建记录。
2. 在页面底部 **Artifacts** 区域下载 `leafra-debug-apk`。
3. 解压 ZIP，得到 `app-debug.apk`。

Artifact 默认保留 30 天（工作流中 `retention-days: 30`）。

### 5. Action 版本与更新方式

工作流固定使用以下稳定版本标签（不是 `@main` 或浮动分支）：

| Action | 版本 |
| --- | --- |
| `actions/checkout` | `v4.2.2` |
| `actions/setup-java` | `v4.3.0` |
| `actions/upload-artifact` | `v4.6.0` |

更新方法：

1. 查看对应 Action 的 GitHub Releases，确认新版本仍支持 `ubuntu-latest`。
2. 在单独分支中修改 `.github/workflows/android.yml` 中的版本号。
3. 触发一次手动构建验证通过后再合并。
4. 如仓库有更高安全要求，可把版本标签替换为 Release 对应的完整 commit SHA。

## 安装到 Android 手机

1. 把 `app-debug.apk` 通过微信文件传输、数据线、网盘或 GitHub 下载到手机。
2. 在手机上点击 APK。系统提示“不允许安装未知来源应用”时，进入设置允许当前文件管理器/浏览器安装应用。
3. 完成安装，桌面会出现 **Leafora** 图标。
4. 最低支持 Android 8.0（API 26）。

说明：

- 这是 **debug APK**，使用 Android 默认调试签名，只适合自己测试，不能直接上架应用商店。
- 不同手机厂商的“安装未知应用”入口名称略有不同，一般在“设置 → 应用 → 特殊权限”中。
- Photo Picker 在 Android 11+ 使用系统照片选择器；在 Android 8.0–10 上，AndroidX 会自动回退到系统文件选择界面，这是正常现象。

## 可选：本地构建

不安装 Android Studio 也可以构建，只需：

- JDK 17
- Android SDK Platform 34、Build-Tools 34.0.0
- 将 `ANDROID_HOME` 指向 SDK，或在项目根目录创建 `local.properties`：

```properties
sdk.dir=C\:\\Android
```

（`local.properties` 已被 `.gitignore` 忽略，不要提交。）

Windows 下运行：

```powershell
cd C:\Website\leafra
.\gradlew.bat :app:assembleDebug
```

Linux / macOS：

```bash
./gradlew :app:assembleDebug
```

产物路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

首次构建会下载 Gradle 8.9 发行包和依赖，需要网络与一定时间。低配置电脑建议直接使用 GitHub Actions。

## 常见构建失败排查

### 1. Gradle 下载失败或超时

- 检查 Actions 日志中是否卡在 `Downloading https://services.gradle.org/distributions/gradle-8.9-bin.zip`。
- 多为 GitHub Runner 网络波动，重新运行工作流通常可解决。
- 不要随意修改 Wrapper 版本；AGP 8.7.3 需要 Gradle 8.9+。

### 2. `Unsupported class file major version` 或 Java 版本错误

- 确认工作流使用的是 JDK 17（`actions/setup-java` 中 `java-version: '17'`）。
- 本地构建时执行 `java -version`，必须是 17，而不是 8/11/21。

### 3. `SDK location not found` / 缺少 Platform 34

- GitHub 的 `ubuntu-latest` Runner 已预装 Android SDK；若平台缺失，Gradle 会在许可证已接受的情况下自动安装。
- 本地构建时设置 `ANDROID_HOME` 或 `local.properties`，并确认已安装 API 34。

### 4. `./gradlew: Permission denied`

工作流已经执行 `chmod +x ./gradlew`。如果自定义了工作流，请保留这一步，或在 Git 中把 `gradlew` 的文件模式提交为 `100755`。

### 5. Artifact 上传失败或找不到 APK

- 上传路径必须与项目结构一致：`app/build/outputs/apk/debug/app-debug.apk`。
- 如果构建步骤失败，Artifact 步骤不会执行。先在日志中定位第一条 `FAILURE` / `What went wrong`。
- 工作流设置了 `if-no-files-found: error`，路径错误会直接报错，避免上传空产物。

### 6. Gradle 与 AGP 版本不兼容

典型报错：`Minimum supported Gradle version is 8.9`。请保持：

```text
AGP 8.7.3 + Gradle 8.9 + JDK 17 + Kotlin 2.0.21
```

### 7. Compose 编译错误

Kotlin 2.0+ 必须应用 `org.jetbrains.kotlin.plugin.compose`，并且版本与 Kotlin 一致。不要手动指定旧的 `composeOptions.kotlinCompilerExtensionVersion`。

### 8. 内存不足 / Gradle daemon 崩溃

项目已限制 JVM 内存为 2048 MB。若 Runner 内存紧张，可在工作流构建命令中追加 `--max-workers=2`；不要在低配置本地电脑上同时运行 Android Studio 和 Gradle。

### 9. 中文乱码或资源编译失败

确保 `strings.xml` 等文件以 UTF-8 保存。本项目 `gradle.properties` 已设置 `-Dfile.encoding=UTF-8`。

## 数据与隐私

- 当前版本不申请存储权限，通过系统 Photo Picker 读取你主动选择的图片。
- 导入的图片复制到应用私有目录 `files/diary-images/`，数据库 `leafra.db` 只保存相对路径；卸载应用时随应用数据一起删除。
- 应用没有网络权限，不上传任何图片或日记内容。
- 搜索与收藏只读取本地 Room 数据；主题与日期显示设置保存在本地 DataStore，与日记数据分开。
- 不包含账号系统、云端数据库、OCR 或在线 AI。
- `.gitignore` 已排除签名文件、密钥、`local.properties`、构建产物，以及 `diary-images/` 等私人日记图片目录。请勿把私人日记图片或 API 密钥提交到 Git。

## 品牌与视觉

- 官方 Logo 源文件：`branding/leafora_icon.svg`（512 × 512，透明背景）；派生规则见 `branding/README.md`。
- 启动器图标：`res/mipmap-anydpi-v26/ic_launcher.xml` 与 `ic_launcher_round.xml`，前景为 `drawable/ic_launcher_foreground.xml`（108dp 画布、内容约 60dp 居中），并包含 Android 13 主题图标用的 `drawable/ic_launcher_monochrome.xml`。
- 首页顶栏品牌图形：`drawable/ic_brand_mark.xml`，只在首页顶部出现，不随内容滚动，也不在其它页面重复。
- 色板：鼠尾草绿 `#87977A`、深鼠尾草 `#5C6E54`、奶油白 `#F8F4E8`、纸张背景 `#F6F0E3`、墨色 `#3E3A31`；Compose 映射见 `ui/theme/Color.kt` 与 `Theme.kt`。
- 图标底色为奶油白 `#F4EDDC`；所有图形均为本地资源，不请求网络。

## 数据模型与关键约定

- `notebooks`：`id`（UUID）、`name`、`coverImagePath`（可选的真实照片封面，当前不写入）、`coverStyle`（本地封面模板，默认 0）、`createdAt`、`updatedAt`、`lastReadPageId`（阅读进度，页面 ID）。
- `pages`：`id`（UUID）、`notebookId`（外键，级联删除）、`sortOrder`、`entryDate?`、`description?`、`imagePath?`、`isBlank`、`isFavorite`、`createdAt`。
- 搜索/收藏列表使用 Repository 的 `PageResult` 投影（页面 + 日记本名称 + 动态页码），不新增数据库表，也不复制页面数据。
- 设置保存在 DataStore（`leafra_settings`）中，不进入 Room 数据库：主题模式与页面日期显示开关与日记数据彼此独立。
- 数据库版本为 2；版本 1 → 2 通过 `ALTER TABLE notebooks ADD COLUMN coverStyle` 迁移，不重建数据库、不清空已有数据。
- 页码不落库，由 Repository 按当前顺序计算（从 1 开始连续）；页面以 `id` 寻址，不能把页码当主键。
- 搜索匹配日记本名称、页面描述与页面日期（多位常见日期格式包含匹配），结果始终来自本地 Room 数据。
- 拖动排序时把 `sortOrder` 重写为 1..n，页面 ID 不变；日期、描述、收藏按页面 ID 读写，因此重排不会错位。
- 阅读进度按日记本保存 `lastReadPageId`（页面 ID，不是页码）：打开时按 ID 定位，页面已删除则清除并回退到第一页；翻页只更新该字段，不改变日记本的 `updatedAt`。
- 立体书跨页由页面顺序实时计算，不落库：第 1 页在右侧（左侧封面），之后每两页一个跨页，最后一页单独时另一侧为封底；单页与立体书之间按页面 ID 互转。
- 空白占位页表示为 `isBlank = true` 且 `imagePath = null`；替换图片后写入私有目录相对路径并置 `isBlank = false`，与“图片缺失”有明确的界面区分。
- 封面由 Compose 按名称与模板实时绘制，不生成图片文件；删除日记本只删除数据库记录与仍被引用的原始照片。
- 导入是“先复制文件，再单事务写入”；任一步失败都会删除本次文件并回滚事务。
- 删除页面 / 日记本时按引用计数清理图片；数据库未引用的孤立图片在应用启动时清理。

## 下一步开发建议

1. 高级 3D 纸张变形：在现有 rotationY 翻页、透视与阴影基础上改进纸背映射、纸张弯曲和手势；仍坚持 Compose 轻量方案，不引入游戏引擎。
2. 本地备份 / 导出 / 导入（本阶段未实现）。
3. Coil 大图加载与沉浸式阅读打磨（当前为 `BitmapFactory` 下采样）。
4. 搜索体验优化：结果按相关度排序、最近搜索或全文索引（当前为本地包含匹配）。
