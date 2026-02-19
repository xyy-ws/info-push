# Info Push Android

Info Push Android 客户端（Jetpack Compose + Room + Retrofit）。

## 运行方式

工作目录：`apps/info-push/android`

### 1) 执行全量单测（Task 7 验证命令）

```bash
./gradlew :app:testDebugUnitTest
```

### 2) 本地构建 Debug 包

```bash
./gradlew :app:assembleDebug
```

## API baseUrl 说明

当前网络层通过 `NetworkModule.createApi(baseUrl: String)` 注入 API 地址，接口路径为 `/v1/*`（例如 `/v1/sources/home`、`/v1/favorites`、`/v1/data/export`）。

推荐约定：

- 本地开发（Android Emulator 访问宿主机）：`http://10.0.2.2:3000/`
- 真机调试（同局域网）：`http://<你的宿主机IP>:3000/`
- 生产环境：使用正式 HTTPS 域名（必须以 `/` 结尾）

注意：`baseUrl` 需满足 Retrofit 要求（包含协议、主机，且以 `/` 结尾）。

## 导入 / 导出说明

导入导出能力由 `ImportExportUseCase` 提供，数据结构：

- `version`
- `exportedAt`
- `data`（sources / sourceItems / favorites / messages / preferences）

导入模式：

- `REPLACE`：全量替换本地数据
- `MERGE`：按合并策略写入本地数据

UI 入口位于设置页（`SettingsScreen`）：

- 导出数据
- 导入数据（replace）
- 导入数据（merge）

## 验收步骤

四个页面的验收步骤见：`docs/acceptance.md`
