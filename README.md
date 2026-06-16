# ColorOS AOD Enhance

![License](https://img.shields.io/github/license/Qjj7679/ColorOS_Aod_Enhance?style=flat-square)

ColorOS 息屏显示（AOD）增强模块（Xposed）。提供可视化配置界面，修改自动保存。

## 功能

**亮度调节**
- 熄屏前暗光环境初始 AOD 亮度
- 熄屏前亮光环境初始 AOD 亮度
- 熄屏时 AOD 自动亮度倍率（1.0～2.0）

**功能开关**
- 全天全景 AOD 支持
- 息屏全景 AOD 显示设置
- AOD 单击唤醒屏蔽

> 亮度部分已预设合理值，安装即用。如感觉过亮/过暗可在 App 中调节。
>
> 熄屏时 AOD 自动亮度倍率默认值为 **1.6**，原因：熄屏后系统会将当前环境自动亮度乘以 **0.6** 作为 AOD 亮度，导致 AOD 偏暗。倍率设为 1.6 可抵消这一削减（1.6 × 0.6 ≈ 1.0），使 AOD 亮度与当前环境亮度匹配。

## 使用

1. 安装 APK，在 Xposed/LSPosed 中激活模块
2. 勾选目标作用域：`系统界面（com.android.systemui）`、`息屏（com.oplus.aod）`
3. 重启系统界面后生效
4. 打开模块 App 配置各项参数，修改后自动保存

> ⚠️ UI 保存后配置即时写入，Hook 侧读取方式按功能不同：
> - **亮度相关**：每次触发直读 Provider，修改后即时生效（≤1s）
> - **功能开关**：进程启动时预读，修改后需**重启系统界面/息屏进程或设备**才能生效

## 构建

```bash
# Release 版（R8 混淆 + 签名）
./gradlew assembleRelease

# Debug 版
./gradlew assembleDebug
```

APK 默认按架构分离，输出路径：

```
app/build/outputs/apk/
├── release/
│   ├── app-arm64-v8a-release.apk
│   └── app-armeabi-v7a-release.apk
└── debug/
    ├── app-arm64-v8a-debug.apk
    └── app-armeabi-v7a-debug.apk
```

> Release 构建需要配置签名文件（`keystore.properties`），Debug 构建可直接使用。详见 `app/build.gradle`。

## 技术栈

| 组件 | 用途 |
|---|---|
| YukiHookAPI + KavaRef | Xposed Hook 框架 |
| Jetpack Compose + Miuix | UI 界面（MIUI 风格组件） |
| ContentProvider + SharedPreferences | 配置跨进程存储 |
| AGP 9.2.1 / Kotlin 2.4.0 / Gradle 9.5.1 | 构建系统 |

## License

MIT

## 更新日志

### v1.5 — 配置重构与性能优化

- 新增 `AodConfigContract.readRow()` 统一 UI 和 Hook 侧的 Cursor 解析逻辑
- PanoramicHook 首次写入后标记完成，后续 `getInstance()` 热路径零开销，降低通知中心卡顿概率
- SingleClickBlockHook `isSupportGesture` 回调中仅做 int 对比，无配置读取无反射
- `AtomicReference` 改为 `@Volatile var`，读写路径减少一层对象包装
- 修复 `ContentValues.getAs*()` 潜在的 NPE 风险
- 统一 Hook 日志为 `Log.d("AOD_Enhance", ...)`

### v1.4 — 适配 ColorOS 16.0.5 (Android 16)

- 适配 `setBrightnessForFallbackStrategy` 方法名变更，新旧版本兼容
- 修复首次配置丢失：首次打开 UI 修改设置不再因 Provider 未就绪而被丢弃
- 修复 Activity 冗余写入：进入页面时不再触发无意义的全量保存
- 增加容错：所有 Hook 注册包裹 `runCatching`，单点失效不影响其他功能
- 重构配置通道：统一列索引和键名到 `AodConfigContract`，消除硬编码序号和双镜像维护风险
- 添加 ProGuard 规则防止数据类被混淆
