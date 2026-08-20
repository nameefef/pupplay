# 🐕 PupPlay · 汪汪捕猎场

[English](README.md) · **简体中文**

给狗狗玩的安卓触屏捕猎游戏。手机平放在地上，狗狗用鼻子或爪子拍屏幕上乱窜的猎物，抓到就有音效和震动反馈。

界面支持**中文和英文**：默认跟随系统语言，也可以在应用内直接切换，不用改系统设置。

## 功能

- **32 个内置角色**，分三类：
  - 小动物：老鼠、狐狸、兔子、松鼠、猫、刺猬、浣熊、鸭子、小鸡、小羊、小猪、青蛙、小蛇、螃蟹、小鱼
  - 虫 · 鸟：甲虫、蝴蝶、蜜蜂、蜘蛛、蜻蜓、小鸟、萤火虫
  - 玩具 · 光点：**红点**、绿激光点、蓝光点、网球、飞盘、骨头、绳结玩具、泡泡、小星星、羽毛
  - 另有「大乱斗」（每只随机换角色）和「自定义图片」
- **9 种内置背景**：纯深色 / 草地 / 星空 / 雪地 / 木地板 / 地毯 / 沙滩 / 蓝天 / 树林，外加自定义图片
- **速度 5 档、数量 1–10 只、大小 10 档（0.5×–3.4×）** 可调（爪子拍不准就把猎物往大调，小型犬可以调小。单只永远不超过屏幕的三成，数量多时还会自动再收一点）
- **28 种代码合成音效**，抓到和落空各有不同的声音；可指定固定音效，也可导入自己的音频文件
- **震动反馈**：抓到是双震，落空是轻轻一记，连抓 10 只三连震；强度三档
- **防误退**：返回键与返回手势全部屏蔽，必须长按左上角小圆圈 2/3/5 秒（可选）才退出
- **自定义素材**：角色图片、背景图片、音效文件都能换成自己的，文件会复制进应用内部目录，删掉原文件也不受影响

## 为狗狗做的几个设计取舍

- **配色按狗的二色视觉调**：狗只有蓝—黄两种视锥，红绿在它眼里都是暗黄褐色。所以猎物主色用亮黄、亮蓝、白，背景压暗，对比最大。红点是特意保留的选项——狗看到的是一个暗色小点，但只要动得够快照样会追。
- **运动节奏是「静止—急冲—急停」**，不是匀速乱飘。真实猎物就是这样动的，这个节奏最能触发捕猎本能。
- **拍空了猎物会受惊窜开**，这一下最能勾着狗继续追。
- **判定范围放宽**：爪子拍不准，命中半径比图形本身大一圈。

## 下载安装

去 [Releases](../../releases) 下载 APK：

| 文件 | 说明 |
|---|---|
| `pupplay-universal.apk` | **推荐**，所有安卓手机都能装 |
| `pupplay-arm64-v8a.apk` | 64 位 ARM |
| `pupplay-armeabi-v7a.apk` | 32 位 ARM（老设备） |

> 这个 app 是纯 Kotlin 写的，**没有任何 native 库**，所以三个包内容完全一样，随便下哪个都行。

要求 Android 7.0（API 24）以上。手机上先允许「安装未知来源的应用」再点开安装。

## 自己编译

### 方式一：Android Studio（最省事）

1. 装 [Android Studio](https://developer.android.com/studio)
2. `Open` → 选择本仓库目录
3. 第一次打开它会自动下载 Gradle 和 Android SDK（几百 MB）
4. 手机开「开发者选项 → USB 调试」，插上，点绿色 ▶ 直接装到手机上

### 方式二：命令行

```bash
# 1) 装 JDK 17、Gradle、Android 命令行工具
brew install openjdk@17
brew install --cask android-commandlinetools

# 2) 装 SDK 组件并同意协议
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
yes | sdkmanager --sdk_root=$ANDROID_HOME --licenses
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 3) 编译
echo "sdk.dir=$ANDROID_HOME" > local.properties
gradle assembleRelease

# 4) 安装到手机
adb install -r app/build/outputs/apk/release/app-universal-release.apk
```

APK 产物在 `app/build/outputs/apk/`。

> **在中国大陆编译**：Gradle 拉 `dl.google.com` 的依赖大概率需要代理。在 `~/.gradle/gradle.properties` 里加：
> ```properties
> systemProp.https.proxyHost=127.0.0.1
> systemProp.https.proxyPort=7890
> systemProp.http.proxyHost=127.0.0.1
> systemProp.http.proxyPort=7890
> ```
> 端口换成你自己的。

## 玩法建议

- 手机**平放在地板上**，最好套个防摔壳或贴层膜——狗爪子指甲会划屏幕
- 刚开始用 **2～3 只、速度 2～3 档**，狗狗上手了再加
- 单次玩 **5～10 分钟**就够了，玩太久狗会亢奋过头
- 想更保险，去系统设置里打开 **「屏幕固定 / 应用固定」**，进游戏后固定住，狗狗连划出去都做不到
- 玩完给点真实的奖励（零食、玩具），不然长期只在屏幕上追而抓不到实物，有的狗会挫败

## 工程结构

```
app/src/main/java/com/easonyin/dogplay/
├── MainActivity.kt      主菜单：角色/背景/速度/数量/音效/震动/自定义素材
├── GameActivity.kt      全屏沉浸 + 屏蔽返回键 + 边缘手势排除
├── GameView.kt          游戏循环、多点触控、粒子特效、HUD、长按退出
├── Prey.kt              猎物的运动状态机（停顿—奔跑—受惊逃窜）
├── PreyType.kt          32 个角色的参数表
├── PreyRenderer.kt      32 个角色的矢量画法（Canvas 绘制，无图片素材）
├── Background.kt        9 种背景的程序化绘制
├── SoundEngine.kt       28 种音效的波形合成 + AudioTrack 播放
├── Haptics.kt           震动反馈
├── Prefs.kt             设置存储 + 自定义素材管理
└── PreviewViews.kt      菜单里的角色/背景预览格子

app/src/main/res/
├── values/strings.xml       英文（默认）
└── values-zh/strings.xml    中文
```

每个角色都是 Canvas 矢量路径绘制的，放到任何尺寸都不会糊——根本没有位图可以被拉花。美术和音效全部由代码生成，工程里没有任何图片或音频素材文件（除了启动图标）。

## 许可证

PupPlay 依 **GNU Affero General Public License v3.0** 开源,全文见 [LICENSE](LICENSE)。

你可以自由使用、研究、修改和再分发。但如果你分发修改过的版本,或者把修改过的版本作为网络服务运行,必须以同样的协议公开你的改动。

> 说明:AGPL 的网络条款(§13)在这里实际影响不大,因为这个游戏完全离线运行;真正起作用的是再分发时的传染性条款。

## 签名说明

Releases 里的包用 Android 默认 debug 密钥签名，方便直接装了试玩，**不能上架应用商店**。要正式发布请自己生成签名密钥重新打包。
