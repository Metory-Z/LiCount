# 轻记 (LiCount)

一个极简、本地优先的记账 App，帮你养成坚持记账的习惯。

## 特点

- **极简快记**：打开 App 直接就是金额数字键盘，点分类、按保存，3 秒记完一笔，不靠意志力靠低摩擦
- **收支切换**：支出/收入一键切换，收入也能顺手记
- **今日提醒**：今天还没记账时，首页轻提醒
- **本地优先**：数据存在手机本地（SQLite/Room），零联网、零注册、零隐私顾虑
- **清晰统计**：本月每日收支趋势图 + 各分类支出占比一目了然
- **可改可删**：流水里的每笔记录点开就能改（金额/类型/分类/备注）
- **FlClash 风格**：超椭圆卡片、多层柔和阴影、滑动药丸底栏，清爽高级

## 技术栈

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) + KAPT
- DataStore
- minSdk 26 / targetSdk 34

## 构建与安装

推荐直接下载 [Releases](../../releases) 里的 `LiCount-v*.apk`（正式签名版）。

### 覆盖安装

所有官方构建（debug / release）统一使用包名 `com.licount` 和同一把固定签名密钥，
`versionCode` 随 CI 构建号递增，因此**新版本可直接覆盖安装，不会丢数据**。

> 早期（2026-11-18 之前）的包使用 `com.qingzhang` / `com.licount.debug` 包名且签名不固定，
> 从那些旧包升级时**需要卸载一次**（会清空旧数据），之后即可一直覆盖安装。

### 本地构建

```bash
./gradlew assembleDebug
```

release 包需要注入签名密钥（CI 由 GitHub Secrets 自动提供）：

```bash
SIGNING_KEY_PATH=licount-release.jks \
KEY_ALIAS=licount \
KEY_STORE_PASSWORD=<密码> \
KEY_PASSWORD=<密码> \
./gradlew assembleRelease
```

`licount-release.jks` 由维护者本地保管，不纳入版本库（`.gitignore` 已排除）。

## 页面

| 页面 | 说明 |
|---|---|
| 记账 | 本月支出/收入 + 支出收入切换 + 分类快捷选 + 数字键盘 |
| 流水 | 按时间倒序的所有记录，点击修改、长按删除（右侧删除按钮） |
| 统计 | 本月每日收支趋势图 + 分类占比条形图 |

## License

MIT
