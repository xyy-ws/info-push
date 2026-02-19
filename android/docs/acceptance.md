# Info Push Android 验收文档

本文用于 Task 7 收尾验收，覆盖运行方式、API baseUrl、导入导出与 4 个页面关键流程。

## 1. 运行方式（验证命令）

工作目录：`apps/info-push/android`

```bash
./gradlew :app:testDebugUnitTest
```

通过标准：命令执行完成，并生成 `app/build/reports/tests/testDebugUnitTest/index.html` 测试报告。

## 2. API baseUrl 验收

- 网络层入口：`app/src/main/java/com/infopush/app/data/remote/NetworkModule.kt`
- 验收点：`NetworkModule.createApi(baseUrl)` 通过外部传参注入地址，不写死环境地址。
- 验收点：业务接口均走 `/v1/*` 路径。

建议验收地址：

- Emulator: `http://10.0.2.2:3000/`
- 真机：`http://<host-ip>:3000/`

## 3. 导入 / 导出验收

- 入口页面：设置页 `SettingsScreen`
- 操作入口：
  - 导出数据
  - 导入数据（replace）
  - 导入数据（merge）

验收步骤：

1. 触发导出，确认生成 JSON。
2. 检查导出 JSON 包含 `version`、`exportedAt`、`data`。
3. 使用同一份 JSON 执行导入 replace，预期走全量替换。
4. 再次执行导入 merge，预期走合并逻辑。

## 4. 四个页面验收步骤

> 导航链路：`首页 -> 信息源 -> 收藏 -> 消息中心`。

### 4.1 首页（Feed）

1. 启动 App，默认进入首页。
2. 页面展示“首页”“为你推荐的内容将在此展示”。
3. 点击“查看信息源”，应跳转到信息源页面。

### 4.2 信息源（Sources）

1. 在信息源页确认文案“信息源”“你已订阅的信息源列表”。
2. 点击“去收藏”，应跳转到收藏页面。

### 4.3 收藏（Favorites）

1. 初始按钮文案应为“收藏”。
2. 点击收藏按钮后，按钮文案变为“已收藏”。
3. 同时出现“收藏成功”即时反馈（Toast 事件）。
4. 点击“去消息中心”，应跳转到消息中心页面。

### 4.4 消息中心（Messages）

1. 页面展示“消息中心”“系统通知与提醒会显示在这里”。
2. 点击“去设置”，应跳转到设置页（用于导入导出操作入口）。
