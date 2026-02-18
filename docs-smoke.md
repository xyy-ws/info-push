# Info Push Task2 MVP 初步验收步骤

1. 启动 API
   - `node apps/info-push/api/src/server.js`
2. 检查健康
   - `curl http://127.0.0.1:8787/health`
3. 刷新并读取 feed
   - `curl 'http://127.0.0.1:8787/v1/feed?refresh=1'`
4. 更新偏好
   - `curl -X POST http://127.0.0.1:8787/v1/preferences -H 'content-type: application/json' -d '{"topics":["ai"],"pushTimes":["09:00","20:00"],"channels":["in-app","push"]}'`
5. 回读偏好
   - `curl http://127.0.0.1:8787/v1/preferences`
6. 触发一条消息并查看消息中心数据
   - `curl -X POST http://127.0.0.1:8787/v1/push/trigger`
   - `curl http://127.0.0.1:8787/v1/messages`
