# SmsAlert - 短信违停提醒

监控交警短信并实时提醒的 Android 应用。当收到指定发件人且包含关键字的短信时，立即触发通知+震动提醒。

## 功能

- 📩 实时监控短信，匹配发件人+关键词
- 🔔 高优先级通知 + 震动提醒
- ⚙️ 可自定义监控发件人和关键词
- 📝 记录匹配日志
- 🔄 开机自启 + 前台保活服务

## 默认规则

| 类型 | 默认值 |
|------|--------|
| 发件人 | 广东交警 |
| 关键词 | 未按规定停放、请立即驶离 |

## 自动构建

本项目已配置 GitHub Actions 自动构建 APK：

- Push 到 main/master 时自动构建
- 支持手动触发构建

### 下载 APK

1. 打开 [Actions 页面](https://github.com/brucexu2025xxl/SmsAlert/actions)
2. 点击最近一次成功的构建
3. 在 Artifacts 区域下载 `SmsAlert-debug-apk`
