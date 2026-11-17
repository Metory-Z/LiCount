# 轻账 (QingZhang)

一个极简、本地优先的记账 App，帮你养成坚持记账的习惯。

## 特点

- **极简快记**：打开 App 直接就是金额数字键盘，点分类、按保存，3 秒记完一笔，不靠意志力靠低摩擦
- **本地优先**：数据存在手机本地（SQLite/Room），零联网、零注册、零隐私顾虑
- **连续打卡**：顶部显示连续记账天数，形成"不想断"的心理锚点
- **清晰统计**：本月结余、收支、各分类支出占比一目了然
- **FlClash 风格**：超椭圆卡片、多层柔和阴影、滑动药丸底栏，清爽高级

## 技术栈

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) + KSP
- DataStore
- minSdk 26 / targetSdk 34

## 构建

```bash
./gradlew assembleDebug
```

debug APK 输出在 `app/build/outputs/apk/debug/`。

## 页面

| 页面 | 说明 |
|---|---|
| 记账 | 本月结余 + 连续打卡 + 数字键盘快记 |
| 流水 | 按时间倒序的所有记录，支持删除 |
| 统计 | 本月支出总额 + 分类占比条形图 |

## License

MIT
