# note_easy

note_easy 是一个基于 Android 的简洁高效记事应用，旨在帮助用户轻松记录和管理自己的笔记。项目包含多种便捷功能，并集成部分 AI 能力，让你的记录、整理更加智能和高效。

## 主要功能

- 新建、编辑、删除笔记
- 笔记分类管理
- 搜索和快速定位笔记
- AI 智能处理（如自动摘要、内容智能分析等）
- 数据本地安全存储
- 简洁友好的用户界面

## 截图展示
<img width="1080" height="2296" alt="00ff779d806409185e3694031b9dde9a" src="https://github.com/user-attachments/assets/65685d59-1653-4d42-b7bb-09ad2c5e41be" />
<img width="1080" height="2305" alt="96ee3018137602062734a38bb15197e5" src="https://github.com/user-attachments/assets/b15ace32-2fa9-41d2-9fa0-ff7beae9ef2c" />
<img width="1080" height="2943" alt="5b0e760a98859de1d8709d000738cb55" src="https://github.com/user-attachments/assets/707063e9-a53e-4262-a4bf-8dc5cc6f9767" />


## 项目结构

```
note_easy/
├── app/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   └── finalwork/
│   │                       └── AIProcessor.java
│   └── ...
├── ...
```

- `AIProcessor.java`: 负责 AI 相关的核心处理逻辑。

## 安装与运行

1. **克隆项目**
   ```bash
   git clone https://github.com/Zhouzoei/note_easy.git
   ```
2. **导入到 Android Studio**
   - 打开 Android Studio，选择 `Open an existing project`，选择刚下载的项目目录。
3. **编译并运行**
   - 连接设备或者启动模拟器，点击 `Run` 按钮即可体验应用功能。

## 依赖环境

- Android Studio Flamingo 或更高版本
- JDK 17 或以上
- Gradle 8.x+
- Android SDK (建议 33 以上)

## 核心模块介绍

- **AIProcessor.java**  
  集成人工智能相关功能，例如基于自然语言处理的笔记建议、自动摘要等，提升笔记智能化体验。
- **UI 界面**  
  使用 Material Design，提供流畅、直观的操作体验。

## 贡献指南

欢迎大家为本项目贡献代码，提交 issue 或 PR：

1. fork 本仓库
2. 创建新分支 (`git checkout -b feature-xxx`)
3. 提交更改 (`git commit -am 'Add new feature'`)
4. 推送到分支 (`git push origin feature-xxx`)
5. 提交 Pull Request

## License

本项目采用 MIT 协议详情参见 [LICENSE](./LICENSE)。
