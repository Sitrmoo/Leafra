// 根构建脚本：只声明插件版本，具体配置在 :app 模块中。
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // KSP 版本必须与 Kotlin 2.0.21 严格对应；Room 编译器通过 KSP 生成代码。
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
