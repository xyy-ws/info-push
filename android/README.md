# Info Push Android

## 当前状态（Task 1）

已初始化 Android 项目骨架（Gradle + app 模块）并补齐以下依赖占位：
- Compose
- Room
- Retrofit
- Navigation Compose

> 本阶段只完成“可测试的最小骨架”，业务功能与页面细节在后续 Task 继续实现。

## 最小运行说明

在目录 `apps/info-push/android` 执行：

```bash
./gradlew :app:testDebugUnitTest
```

预期输出：`BUILD SUCCESSFUL`

## 目录说明

- `app/src/main/java/com/infopush/app/MainActivity.kt`：应用入口骨架
- `app/src/main/java/com/infopush/app/InfoPushApp.kt`：应用根组件骨架
- `app/src/test/java/com/infopush/app/ProjectSanityTest.kt`：项目健康检查测试
