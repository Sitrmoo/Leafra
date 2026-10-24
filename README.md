# Leafra

Leafra 是一个用于把**纸质日记电子化**的 Android 应用。你可以从手机相册导入日记扫描图片，把它们整理成一本电子书，并逐步获得接近纸质书的阅读体验。

- 应用名：**Leafra**
- 包名 / applicationId：`leafra.sitrmoo.com`
- 仓库：`https://github.com/Sitrmoo/Leafra.git`

本仓库当前处于**项目初始化阶段**，只包含可运行的首页骨架和基础图片选择功能，尚未实现完整的书籍管理与翻页阅读。

## 当前已实现

- Kotlin + Jetpack Compose 原生 Android 项目骨架。
- 中文首页：“我的日记”、空书架提示、纸张风格配色。
- “导入日记图片”按钮：调用 Android 系统 **Photo Picker**，支持一次选择多张图片（最多 30 张）。
- 选择后显示已选图片数量，并支持重新选择。
- “阅读模式”入口：点击后用 Snackbar 提示“正在开发中”，按钮有真实反馈。
- GitHub Actions 云端构建 Debug APK，并上传构建产物。
- 完整的 Gradle Wrapper（Gradle 8.9）。
- 适配普通 Android 手机屏幕，内容区限制最大宽度，小屏可滚动。

## 当前尚未实现

- 把选中的图片复制/保存到应用私有目录。
- Room 数据库、日记元数据管理、书架列表。
- DataStore 设置存储。
- Coil 图片加载与缩略图展示。
- 单页阅读模式、仿真立体书翻页、3D 渲染。
- OCR、云同步、账号系统、在线 AI、广告或付费功能。
- 正式签名与 Google Play 发布配置。

当前选择图片后**不会持久化**，退出应用后选择记录会丢失。这是本阶段的预期行为。

## 技术栈与版本

所有版本都在构建脚本中明确固定，不使用动态版本号（如 `+` 或 `latest.release`）。

| 组件 | 固定版本 | 说明 |
| --- | --- | --- |
| JDK | 17（Temurin） | 本地与 GitHub Actions 一致 |
| 应用名 | Leafra | `strings.xml` 中的 `app_name` |
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

版本兼容关系（升级时请整体考虑，不要只改其中一个）：

- AGP 8.7.3 ↔ Gradle 8.9 ↔ JDK 17
- Kotlin 2.0.21 ↔ Compose Compiler 插件 2.0.21
- Compose BOM 2024.10.01 中的依赖均兼容 compileSdk 34 / minSdk 26

## 目录结构

```text
.
├─ .github/workflows/android.yml        # GitHub Actions 云端构建
├─ app/
│  ├─ build.gradle.kts                  # App 模块构建配置与依赖
│  ├─ proguard-rules.pro
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/leafra/sitrmoo/com/
│     │  ├─ MainActivity.kt             # 入口 Activity
│     │  └─ ui/
│     │     ├─ home/HomeScreen.kt       # 中文首页 + Photo Picker
│     │     └─ theme/                   # 纸张风格配色与字体
│     └─ res/                           # 文案、主题、启动图标
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
git commit -m "feat: rename app to Leafra"
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
3. 完成安装，桌面会出现 **Leafra** 图标。
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
- 应用没有网络权限，不上传任何图片或日记内容。
- 不包含账号系统、云端数据库、OCR 或在线 AI。
- `.gitignore` 已排除签名文件、密钥、`local.properties`、构建产物，以及 `diary-images/` 等私人日记图片目录。请勿把私人日记图片或 API 密钥提交到 Git。

## 下一步开发建议

1. 将选中的图片复制到应用私有目录，并记录到 Room。
2. 用 DataStore 保存阅读设置。
3. 引入 Coil 展示缩略图与阅读页。
4. 实现普通单页阅读模式。
5. 在单页阅读稳定后，再实现仿真翻页效果。
