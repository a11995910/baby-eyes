# 宝宝护眼卫士 👁️

一款基于Android平台的智能护眼监测应用，通过前置摄像头结合人脸检测和距离估算算法，实时监测用户与屏幕的距离，当检测到距离过近时及时提醒，帮助养成正确的用眼习惯。

## ✨ 主要功能

### 🎯 核心功能
- **实时距离检测**: 利用前置摄像头和ML Kit人脸检测技术，实时计算用户与屏幕的距离
- **智能提醒**: 当距离低于设定阈值时，自动弹出全屏警告蒙层
- **悬浮球控制**: 提供可拖动的悬浮球，方便快速控制和设置
- **后台监护**: 前台服务保证检测在后台和其他应用上方持续工作

### ⚙️ 个性化设置
- **距离阈值调节**: 支持20-50cm范围内自由调整警告距离
- **自定义提醒语**: 可设置个性化的警告文本（不超过20字）
- **检测开关**: 一键启用/暂停护眼检测功能

## 🛠️ 技术实现

### 技术栈
- **开发语言**: Kotlin
- **最低支持**: Android 8.0 (API 26)
- **目标版本**: Android 15 (API 36)
- **相机框架**: CameraX
- **人脸检测**: Google ML Kit Face Detection
- **UI框架**: Material Design 3

### 核心算法
- **距离计算公式**: `distance = (focal_length × real_IPD) / pixel_IPD`
- **人眼间距(IPD)**: 平均值6.3cm
- **防抖机制**: 连续3帧检测到距离过近才触发警告
- **焦距估算**: 基于图像分辨率和视场角动态计算

## 📱 使用说明

### 首次使用
1. **权限授权**: 首次启动会引导授予相机权限和悬浮窗权限
2. **启动检测**: 点击主界面的悬浮按钮启动护眼检测
3. **悬浮球**: 系统会显示一个可拖动的护眼球图标

### 功能操作
- **单击护眼球**: 打开设置面板，调整阈值和提醒语
- **拖动护眼球**: 可将护眼球移动到屏幕任意位置
- **长按护眼球**: 显示移除选项，可停止检测服务
- **菜单设置**: 通过应用菜单也可访问设置面板

### 距离警告
- 当检测到距离低于设定阈值时，会弹出全屏警告蒙层
- 警告会显示自定义的提醒文本
- 点击"我知道了"或等待5秒后警告自动消失

## 🔧 开发和构建

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更新版本
- Android SDK 36
- Gradle 8.13
- JDK 11

### 构建步骤
```bash
# 克隆项目
git clone https://github.com/a11995910/baby-eyes-save.git
cd baby-eyes-save

# 编译项目
./gradlew assembleDebug

# 安装到设备 (需要连接设备)
./gradlew installDebug
```

### 项目结构
```
app/src/main/java/wjcom/example/inc/
├── MainActivity.kt                 # 主Activity，权限管理和服务控制
├── service/
│   └── EyeProtectionService.kt    # 前台服务，相机和检测逻辑
├── ui/
│   └── FloatingWindowManager.kt   # 悬浮窗管理器
└── utils/
    ├── DistanceCalculator.kt      # 距离计算算法
    └── SharedPrefsManager.kt      # 配置管理
```

## 🛡️ 隐私和安全

- **本地处理**: 所有图像分析均在设备本地进行，不会上传任何数据
- **实时丢弃**: 相机帧数据用完即丢，不做任何存储
- **权限说明**: 
  - 相机权限：用于人脸检测和距离计算
  - 悬浮窗权限：用于显示护眼球和警告提醒
  - 前台服务权限：保证后台检测功能

## 📋 注意事项

- 请确保前置摄像头没有被遮挡
- 在光线充足的环境下使用效果更佳
- 建议定期清洁前置摄像头镜头
- 不同设备的相机参数可能影响距离计算精度

## 🎯 适用人群

- 有儿童的家长及监护人
- 关注用眼健康的个人用户
- 长时间使用手机的工作人群
- 教育机构和家庭用户

## 📱 系统要求

- Android 8.0 (API 26) 及以上版本
- 具备前置摄像头的设备
- 支持悬浮窗权限的系统

## 🔄 版本历史

### v1.0.0
- 基础护眼检测功能
- 可调节距离阈值和提醒语
- 悬浮球操作界面
- 全屏警告提醒

## 📞 联系支持

- 项目地址：https://github.com/a11995910/baby-eyes-save
- 问题反馈：请在 GitHub Issues 中提交
- 邮箱联系：[可选择性填写]

## 📄 开源协议

本项目遵循 MIT 开源协议。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目。

### 贡献指南
1. Fork 本项目
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

## ⭐ 致谢

感谢以下技术和库的支持：
- Google ML Kit Face Detection API
- CameraX 库
- Material Design 3
- Android Jetpack 组件

---

**让我们一起关爱眼睛健康！** 👀✨