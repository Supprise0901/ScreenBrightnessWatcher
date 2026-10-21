# ScreenBrightnessWatcher

无 Activity 界面的 Android Java 示例项目。开机广播触发后台前台服务，每 5 秒执行一次：

```sh
settings get system screen_brightness
```

结果写入 Android logcat，日志标签为 `BrightnessWatcher`。

## 在 Android Studio 中编译

1. 使用 Android Studio 打开此目录。
2. 等待 Gradle 同步；安装 Android SDK Platform 35（或按本机 SDK 环境调整 `compileSdk`）。
3. 选择 Build > Build APK(s)。
4. APK 通常生成在 `app/build/outputs/apk/debug/app-debug.apk`。

此项目提供源代码，不包含预编译 APK 或 Gradle Wrapper。

## 安装和测试

```bat
adb install -r app-debug.apk
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
adb logcat -s BrightnessWatcher:I
```

实际设备上，重启后系统会发送开机广播。部分定制系统要求用户至少手动启动/安装一次应用，或在系统设置中允许自启动。

## 重要权限说明

- 此服务使用前台服务以提高持续运行概率，因此会显示一条低优先级、常驻通知；没有应用界面，但并非完全没有系统通知。
- 命令以应用自身 UID 执行，不会自动获得 shell 或 root 身份。`settings get` 是否能成功取值取决于设备 Android 版本和厂商权限策略；查看 logcat 中的 `exit`、`output` 和异常信息。
- 把 APK 放入 `/system/app` 或 `/system/priv-app` 并不自动授予 root 权限，也不自动把应用加入 privileged permission allowlist。预置通常需要可写系统分区/定制 ROM 支持并重启。
- 对于 Android 10/11 定制车机，可先普通安装测试命令执行结果，再决定是否需要系统预置。若系统杀后台、禁用自启动或限制前台服务，仍需按厂商系统策略处理。
- 本项目不申请 root，也不调用 `su`。

## 修改执行命令

编辑 `BrightnessService.java` 中 `ProcessBuilder` 的命令字符串。为避免命令输出阻塞，建议保持单条、快速返回的命令。

## GitHub Actions 自动构建

项目已包含 `.github/workflows/build-apk.yml`。把整个项目推送到 GitHub 的 `main` 或 `master` 分支后，Actions 会自动执行 Android Debug APK 构建并上传名为 `ScreenBrightnessWatcher-debug-apk` 的 artifact。

手动触发：GitHub 仓库 → Actions → Build Android APK → Run workflow。

下载：打开对应 workflow run，在 Artifacts 区域下载 `ScreenBrightnessWatcher-debug-apk`。这是调试签名 APK；用于正式发布或系统分区预置时，建议再配置自己的签名密钥。
