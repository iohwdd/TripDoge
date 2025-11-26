# 对话历史 API 测试脚本

## 测试前提
1. 后端服务已启动（端口 7979）
2. 已登录并获取 Token
3. 有可用的角色ID

## 测试步骤

### 1. 获取角色列表
```bash
curl -X POST http://localhost:7979/api/roles/list \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=your_session_id" \
  -d '{}'
```

### 2. 获取对话历史（需要先有对话记录）
```bash
curl -X POST http://localhost:7979/api/chat/1/history \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=your_session_id" \
  -d '{}'
```

### 3. 预期响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "conversationId": "xxx",
      "roleId": 1,
      "messageType": "user",
      "content": "你好",
      "createTime": "2025-11-24 10:00:00"
    },
    {
      "id": 2,
      "conversationId": "xxx",
      "roleId": 1,
      "messageType": "assistant",
      "content": "你好！",
      "createTime": "2025-11-24 10:00:01"
    }
  ]
}
```

### 4. 验证字段
- ✅ `messageType` 字段存在（不再是 `role`）
- ✅ `roleId` 字段正确设置
- ✅ `createTime` 字段格式正确
- ✅ 数据按时间顺序返回

## 前端验证

1. 打开浏览器开发者工具（F12）
2. 访问对话页面：`http://localhost:5173/user/chat/1`
3. 检查 Network 标签页中的 API 请求
4. 验证响应数据格式是否正确
5. 验证历史消息是否正确显示




