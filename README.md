# ColorOS AOD Enhance

![License](https://img.shields.io/github/license/Qjj7679/ColorOS_Aod_Enhance?style=flat-square)

面向 ColorOS 的 AOD 功能增强模块（YukiHookAPI/Xposed）。提供可视化配置界面，自动保存配置，并在 系统界面/息屏 进程中生效。

## 功能概览
- 亮度：
  - 熄屏前暗光环境 AOD 亮度
  - 熄屏前亮光环境 AOD 亮度
  - 熄屏时 AOD 自动亮度倍率（1.0～2.0）
- 功能开关：
  - 系统界面-全天全景 AOD 支持（必开）
  - 息屏-全天全景 AOD 开关（可选）
  - AOD 单击唤醒屏蔽（可选）

亮度部分已默认配置好，如果在使用过程中感觉 太亮/太暗 可在APP中自行调节相关参数

## 项目结构
```
app/src/main/kotlin/com/op/aod/enhance
├─ data/           # Provider + 配置存取
│  ├─ AodUiConfig.kt
│  ├─ AodConfigStore.kt
│  └─ AodConfigProvider.kt
├─ ui/             # UI 页面
│  ├─ MainActivity.kt
│  ├─ BrightnessActivity.kt
│  └─ FeaturesActivity.kt
├─ hook/           # Hook 逻辑
│  ├─ AodConfig.kt
│  ├─ MainHook.kt
│  ├─ BrightnessHook.kt
│  ├─ AodSettingsHook.kt
│  ├─ PanoramicHook.kt
│  └─ SingleClickBlockHook.kt
└─ HookEntry.kt    # YukiHookAPI 入口
```

## 构建
```bash
./gradlew assembleDebug
```
APK 输出：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 运行与生效说明
- UI 页面修改后 **自动保存**（300ms 防抖）。
- Hook 侧配置 **首次读取后缓存**，因此通常需要 **重启 系统界面/息屏 或重启设备** 才能生效。

## 配置通道
- 通过 `ContentProvider` 暴露配置：
  - authority：`com.op.aod.enhance.config`
  - path：`aod_config`

## 备注
- 目前 `AodConfigProvider` 在 Manifest 中为 `exported=true`，如需限制外部访问可调整权限或关闭导出。

## 开发环境
- Gradle Wrapper: `gradle/wrapper/gradle-wrapper.properties`
- 依赖版本统一管理：`gradle/libs.versions.toml`

## License
MIT
