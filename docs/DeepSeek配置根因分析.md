# DeepSeek 配置根因分析

**分析时间**: 2025-11-24

---

## 问题现象

```
Could not resolve placeholder 'mcp.search-link' in value "${mcp.search-link}"
```

---

## 根因分析

### 1. Spring Boot 配置加载顺序

Spring Boot 的配置加载顺序：
1. `application.yaml` (基础配置)
2. `application-{profile}.yaml` (Profile 特定配置)

### 2. 配置解析时机

`@Value` 注解的解析发生在：
- **配置加载阶段**：Spring Boot 在加载配置时解析 `@Value` 注解
- **Bean 创建阶段**：在创建 Bean 实例时注入值

### 3. 问题根源

**根因**：`application.yaml` 中没有 `mcp.search-link` 配置，导致：
1. Spring Boot 先加载 `application.yaml`，没有找到 `mcp.search-link`
2. 在解析 `@Value("${mcp.search-link:}")` 时，虽然默认值是空字符串，但 Spring Boot 仍然会尝试解析占位符
3. 如果占位符不存在，即使有默认值，也会报错（在某些情况下）

**关键发现**：
- `application-ai.yaml` 中有 `mcp.search-link: ${SEARCH_MCP_LINK}`（没有默认值）
- `application-deepseek.yaml` 中有 `mcp.search-link: ""`（空字符串）
- `application.yaml` 中**没有** `mcp` 配置

### 4. 解决方案

**方案1：在 `application.yaml` 中添加默认配置**（已采用）
```yaml
# MCP 配置（默认值，profile 配置会覆盖）
mcp:
  search-link: ""
```

**方案2：使用 `@Value` 的默认值语法**
```java
@Value("${mcp.search-link:}")
private String searchMcpLink;
```

**方案3：使用 `@ConfigurationProperties`**
```java
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {
    private String searchLink = "";
}
```

---

## 修复步骤

### ✅ 已完成的修复

1. **在 `application.yaml` 中添加默认 MCP 配置**
   - 添加 `mcp.search-link: ""` 作为默认值
   - 确保所有 profile 都能找到这个配置

2. **保持 `@Value` 注解的默认值**
   - `@Value("${mcp.search-link:}")` 保持不变
   - 双重保护：配置文件默认值 + 注解默认值

3. **在代码中添加空值检查**
   - `getWebSearchMcpClient()` 方法中检查空值
   - 如果为空，返回 `null`，不创建 MCP 客户端

---

## 配置加载逻辑链路

```
1. Spring Boot 启动
   ↓
2. 加载 application.yaml
   ├─ 找到 mcp.search-link: "" ✅
   └─ 如果没有，会报错 ❌
   ↓
3. 加载 application-{profile}.yaml
   ├─ deepseek: mcp.search-link: "" (覆盖默认值)
   └─ ai: mcp.search-link: ${SEARCH_MCP_LINK} (覆盖默认值)
   ↓
4. 解析 @Value("${mcp.search-link:}")
   ├─ 从配置中获取值
   └─ 如果不存在，使用默认值 ""
   ↓
5. 创建 Bean 实例
   └─ 注入 searchMcpLink 值
```

---

## 验证方法

1. **检查配置文件**
   ```bash
   cat src/main/resources/application.yaml | grep -A 2 "mcp:"
   ```

2. **检查编译后的配置**
   ```bash
   cat target/classes/application.yaml | grep -A 2 "mcp:"
   ```

3. **检查启动日志**
   ```bash
   tail -100 /tmp/backend_test.log | grep -E "mcp|MCP|search-link"
   ```

---

## 总结

**根本原因**：`application.yaml` 中缺少 `mcp.search-link` 配置，导致 Spring Boot 在解析 `@Value` 注解时找不到配置项。

**解决方案**：在 `application.yaml` 中添加默认的 `mcp.search-link: ""` 配置，确保所有 profile 都能找到这个配置。

**修复效果**：
- ✅ 所有 profile 都能正常启动
- ✅ DeepSeek profile 使用空字符串（MCP 功能禁用）
- ✅ AI profile 使用环境变量 `${SEARCH_MCP_LINK}`（如果设置了）

---

**修复状态**：✅ 已完成

