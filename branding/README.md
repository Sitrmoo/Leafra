# Leafora 品牌资源

## 色板

| 用途 | 色值 |
| --- | --- |
| 鼠尾草绿（主叶片 / 品牌主色） | `#87977A` |
| 深鼠尾草（描边 / 主按钮） | `#64745B` / `#5C6E54` |
| 折页奶油白 | `#F8F4E8` |
| 纸张背景 | `#F6F0E3` |
| 图标底色 | `#F4EDDC` |
| 复古墨色 | `#3E3A31` |
| 暖棕点缀 | `#8B5E3C` |

## 文件

- `leafora_icon.svg`：官方 Logo 源文件（512 × 512 视口，透明背景）。
- 派生资源：
  - `app/src/main/res/drawable/ic_launcher_foreground.xml`：自适应图标前景（108dp 画布，内容缩放到约 60dp 并居中，落在安全区内）。
  - `app/src/main/res/drawable/ic_launcher_monochrome.xml`：Android 13+ 主题图标的单色剪影。
  - `app/src/main/res/drawable/ic_brand_mark.xml`：首页顶栏品牌图形（裁掉 SVG 留白，24 × 32dp）。
  - `app/src/main/res/values/colors.xml`：资源色（图标背景、品牌色）。
  - `app/src/main/java/leafra/sitrmoo/com/ui/theme/Color.kt`：Compose 色板。
  - `app/src/main/java/leafra/sitrmoo/com/ui/theme/Theme.kt`：Material 3 配色映射。

## 更新方式

1. 用新的官方 SVG 替换 `leafora_icon.svg`。
2. 按 512 坐标系把 `pathData` 原样转换进 `ic_launcher_foreground.xml` / `ic_brand_mark.xml`，不要手绘替代。
3. 保持 108dp 画布内约 60dp 的内容高度并居中；改动后重新构建，用启动器实际效果确认安全区。
4. 若品牌色变化，同时更新 `colors.xml`、`Color.kt` 与 `Theme.kt`，避免图标与界面脱节。

## 待补素材（可选）

- Google Play 商店要求独立的 512 × 512 PNG 图标（当前仓库只包含 Android 自适应图标资源）。
- 如需宣传图 / 商店截图，需要另行提供品牌视觉素材。
