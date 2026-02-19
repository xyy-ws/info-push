# Info Push Android 项目总结（交接版）

更新时间：2026-02-19  
分支：`feature/info-push-app`

## 1) 当前结论
项目已达到 **v0.1.0-beta 可用状态**，可用于个人日常使用（信息源管理、抓取、收藏、内置阅读、导入导出）。

## 2) 已完成能力
- Android 端本地独立数据（Room）
- 信息源管理：新增 / 启停 / 删除 / 批量管理
- AI 搜索信息源 + 一键添加
- 添加源严格准入：先探测，探测通过才入库
- 首页按信息源切换与刷新
- 收藏：添加、取消、删除、实时过滤
- 内置浏览器：返回/前进/刷新/外部打开/收藏/分享/复制
- 链接规范化与跳转解析降级
- JSON 导入导出（系统文件选择器）
- 错误提示可读化（403/空源/不支持等）
- 顶部全局 Info Push 标题栏移除

## 3) 已确认不做 / 已降级
- 消息中心入口已从设置页移除（低价值）
- 收藏页搜索保持“实时过滤”，不加额外搜索按钮

## 4) 架构要点（便于下次续开发）
- **本地主数据源**：Room（sources/source_items/favorites/preferences/messages）
- **远端角色**：
  - discover-sources（候选发现）
  - source add probe（新增源准入验证）
  - per-source collect/items（按源抓取）
- **关键原则**：
  - 展示与状态以本地为准
  - 添加源必须后端探测通过
  - 错误提示优先用户可读文案

## 5) 近期关键提交（回看入口）
- `4c9b3c9` docs(android): 增加个人使用说明
- `ce3b8ec` chore(android): 稳定性收尾与准发布说明
- `dfdb31b` feat(android): 弱化消息中心并增强源管理与收藏筛选
- `3dd881b` fix(android): 优化信息源报错原因展示
- `cea5709` fix(android): AI添加改为探测通过后入库
- `1ebb116` fix(api): 收紧中文源相关性并减少无关召回
- `ff0b005` fix(api): 添加源前强制探测并修复Atom误判

## 6) 已知限制
- 部分第三方 RSS（特别是部分 RSSHub）受上游 403 影响
- 源健康状态为轻量实现（非后台定时探针）
- 内置浏览器个别站点首次加载可能仍有轻微等待

## 7) 下次继续开发建议（优先级）
1. 源健康状态增强：增加主动探测与分源更细粒度失败统计
2. 失败源一键修复建议（推荐替代 URL / 镜像）
3. 浏览器阅读体验增强（阅读模式、字体、夜间细化）

## 8) 快速继续开发步骤
1. 拉取分支：`feature/info-push-app`
2. 先读文档：
   - `apps/info-push/android/docs/personal-user-guide.md`
   - `apps/info-push/android/docs/acceptance-checklist.md`
   - `apps/info-push/android/docs/project-handover-summary.md`（本文件）
3. 回归命令：
   - `./gradlew --no-daemon :app:testDebugUnitTest`
   - `./gradlew --no-daemon :app:assembleDebug`
4. 先从“源探测与健康状态链路”做验证再加新功能
