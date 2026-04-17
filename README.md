---
AIGC:
    ContentProducer: Minimax Agent AI
    ContentPropagator: Minimax Agent AI
    Label: AIGC
    ProduceID: "00000000000000000000000000000000"
    PropagateID: "00000000000000000000000000000000"
    ReservedCode1: 304602210084adb76b796a31b6683c1b095817bb329024a69a349547d5c15478dad33a91b0022100f461de1a163354591bd7003121ae01494441f7602387f30dc6565bfd52323bbf
    ReservedCode2: 3045022100afb802eca4d6cfaab39e64a516d96c06deb0ab93cd59b04ac4a1f2e895f6baf00220707293a7dcaed6a0b451a17a261647b0869a8bb4e520d632aedd23d3d9ae803a
---

# PureBeat - 本地音乐播放器

一款简洁的 Android 本地音乐播放器，支持多种音频格式，无需登录，保护隐私。

## 功能特性

- **多种播放模式**：列表循环、随机播放、单曲循环
- **文件夹管理**：按文件夹组织音乐文件
- **自定义歌单**：创建和管理个人歌单
- **自定义背景**：支持设置应用背景图片
- **后台播放**：支持后台播放和通知栏控制
- **多种音频格式**：支持 MP3、WAV、FLAC、AAC、OGG 等格式

## 技术栈

- **语言**：Java
- **最低 SDK**：Android 8.0 (API 26)
- **目标 SDK**：Android 14 (API 34)
- **音频播放**：Media3 (ExoPlayer)
- **数据库**：Room Database
- **图片加载**：Glide

## 项目结构

```
app/src/main/java/com/purebeat/
├── activity/          # Activity 页面
│   ├── MainActivity.java           # 主页面
│   ├── PlayerActivity.java         # 播放器页面
│   ├── PlaylistDetailActivity.java # 歌单详情
│   └── FolderDetailActivity.java   # 文件夹详情
├── fragment/          # Fragment 页面
│   ├── SongsFragment.java         # 歌曲列表
│   ├── FoldersFragment.java       # 文件夹列表
│   ├── PlaylistsFragment.java      # 歌单列表
│   └── SettingsFragment.java       # 设置页面
├── adapter/           # RecyclerView 适配器
├── database/          # Room 数据库
│   ├── AppDatabase.java           # 数据库实例
│   ├── PlaylistEntity.java        # 歌单实体
│   └── PlaylistSongCrossRef.java  # 歌单歌曲关联
├── dao/               # Data Access Object
├── model/             # 数据模型
│   ├── Song.java                  # 歌曲模型
│   ├── Folder.java                # 文件夹模型
│   ├── Playlist.java               # 歌单模型
│   └── PlaybackMode.java           # 播放模式枚举
├── service/           # 服务
│   ├── MusicPlaybackService.java   # 音乐播放服务
│   └── MusicController.java        # 音乐控制器
├── receiver/          # 广播接收器
└── util/              # 工具类
    └── MusicScanner.java          # 音乐扫描器
```

## 代码质量保证

### 内存管理优化

- **Activity/Fragment 生命周期管理**：在 `onDestroy()` 中正确清理 Handler 回调、ExecutorService 和 Service 连接
- **WeakReference 回调**：MusicController 使用 WeakReference 避免 Activity/Fragment 内存泄漏
- **线程安全集合**：播放列表使用 CopyOnWriteArrayList 确保线程安全
- **状态检查**：在 UI 更新前检查 `isFinishing()`、`isDestroyed()`、`isAdded()` 等状态

### 主线程保护

- **异步数据库操作**：所有 Room 数据库操作在独立的 ExecutorService 中执行
- **音乐扫描异步化**：MusicScanner 在后台线程扫描本地音乐文件
- **图片加载优化**：使用 Glide 进行异步图片加载和缓存

### 生命周期管理

- **按需更新**：Activity/Fragment 不可见时暂停进度更新，节省资源
- **资源释放**：正确释放 MediaSession、ExoPlayer 和播放器监听器
- **状态恢复**：Activity 重新可见时恢复更新循环

## 构建说明

### 方式一：使用 GitHub Actions（推荐）

1. 将代码推送到 GitHub 仓库
2. GitHub Actions 将自动构建 Debug APK
3. 在 Actions 页面下载构建产物

### 方式二：本地构建

1. 确保已安装 Android Studio 和 JDK 17
2. 克隆仓库
3. 在 Android Studio 中打开项目
4. 点击 Build > Build Bundle(s) / APK(s) > Build APK(s)

### 推送代码到 GitHub

```bash
# 添加远程仓库
git remote add origin https://github.com/DullWoodKnife/purebeat-music-player.git

# 推送代码
git push -u origin main
```

## 权限说明

应用需要以下权限：
- `READ_MEDIA_AUDIO`：读取音频文件（Android 13+）
- `READ_EXTERNAL_STORAGE`：读取外部存储（Android 12 及以下）
- `FOREGROUND_SERVICE`：后台播放服务
- `POST_NOTIFICATIONS`：发送通知
- `WAKE_LOCK`：保持屏幕唤醒

## 版本信息

- **版本号**：1.0.0
- **最低系统**：Android 8.0 (API 26)
- **作者**：DullWoodKnife

## 许可证

本项目仅供学习交流使用。
