# 黑马点评 Plus — 微服务企业级实战项目

> 围绕"本地生活与商户运营"场景的全栈项目，涵盖商户搜索、优惠券秒杀、达人探店、好友关注等业务。源自 [hmdp-plus](https://gitee.com/java-up-up/hmdp-plus)（阿星不是程序员），重构为 **Spring Boot 4 + Spring Cloud Alibaba 微服务架构**，并新增 Elasticsearch 搜索引擎与 LLM 驱动的 AI Agent 服务。

## 一、项目概述

这是一个企业级微服务实战项目，业务场景贴近真实生活服务类应用：

- **用户侧**：浏览商铺、搜索附近好店、抢购优惠券、订阅到券提醒、关注好友看探店动态
- **商户侧**：商铺管理、优惠券发放、秒杀活动配置
- **技术侧**：从单体到微服务的完整演进路径，覆盖缓存穿透/击穿/雪崩、分布式事务、消息可靠性、搜索引擎、AI 推理等面试高频考点

项目的核心价值不是"实现功能"，而是**每个功能背后都有一整套生产级方案**——缓存怎么防穿透？秒杀库存扣减如何原子化？MQ 消息丢了怎么办？AI 怎么理解"想吃火锅"然后搜出结果？下面每个章节逐一拆解。

### 7 个微服务

| 服务 | 端口 | 职责 |
|------|------|------|
| `hmdp-gateway` | 8080 | Spring Cloud Gateway 统一入口 |
| `hmdp-shop-service` | 8081 | 商铺 CRUD、布隆过滤器、MQ 同步事件 |
| `hmdp-search-service` | 8086 | ES 搜索引擎（名称全文检索/类型筛选/GEO 距离排序） |
| `hmdp-user-service` | 8082 | 用户注册/登录/信息管理（分片表） |
| `hmdp-voucher-service` | 8083 | 秒杀全链路（Lua 扣减 → RabbitMQ → 订单 → 对账） |
| `hmdp-blog-service` | 8084 | 博客/探店/点赞、Feed 滚动分页 |
| `hmdp-follow-service` | 8085 | 关注/取关/共同关注 |
| `hmdp-ai-agent-service` | 8087 | AI Agent（LLM 智能搜索/个性化推荐/行为采集） |

### 核心技术栈

| 层 | 技术 |
|---|---|
| 框架 | Spring Boot 4.0.6, Java 21 |
| ORM | MyBatis-Plus 3.5.16 + ShardingSphere 5.3.2 |
| 数据库 | MySQL 8.0 (双库分片: hmdp_0 / hmdp_1) |
| 缓存/锁 | Redis 7, Redisson 3.52.0, Caffeine |
| 消息队列 | RabbitMQ 4.x (发布确认 + 幂等消费 + DLQ) |
| 搜索引擎 | Elasticsearch 8.16.3 (elasticsearch-java 8.17.4) |
| 限流 | 自研令牌桶 + 滑动窗口 |
| ID 生成 | Snowflake 雪花算法 |
| 鉴权 | Sa-Token 1.43.0 |
| 服务发现 | Nacos 3.1.0 (Spring Cloud Alibaba 2025.1.0.0) |
| 远程调用 | OpenFeign |
| 流量保护 | Sentinel 1.8.9 (Gateway + 服务级) |
| 前端 | Vue 3.5 + Vite 6 + Element Plus 2.9 + Pinia 3 |

## 二、核心技术亮点

**AI 智能服务**：接入 DeepSeek 等大模型，实现自然语言搜索（"西湖区高分火锅，人均100以内"→自动识别美食类型、筛选区域和价格）、基于用户画像的个性化券推荐、结合好友动态的商铺推荐。用户行为通过 4 个服务埋点 → MQ → 三层记忆系统（MySQL 审计 + Redis 快照 + Hash 增量画像），推荐越来越懂你。详见[§三](#三ai-agent-智能服务)。

**秒杀链路**：Lua 脚本在 Redis 单线程内完成库存校验→扣减→标记用户→写追踪日志，全程原子。扣减完成后异步 MQ 创建订单，响应时间从 DB 写延迟降为 Redis Lua 执行延迟。

**流量控制**：令牌前置授权（资格控制）+ 令牌桶限流（速率控制）+ Sentinel 全局 QPS（Gateway + 服务双层），将非法请求和超量请求拦截在系统核心链路之外。

**缓存体系**：Caffeine 本地缓存（L1）→ Redis 分布式缓存（L2）→ 布隆过滤器 + 空值缓存（防穿透）→ 分布式锁 + 双重检测（防击穿），四层防线层层设防。

**MQ 可靠性**：RabbitMQ 发布确认 + 消费手动 ACK + 死信队列（DLQ），配合 `@RepeatExecuteLimit` 幂等消费，保证"扣减成功则订单必落库"。

**一致性保障**：对账日志表 `tb_voucher_reconcile_log` 记录每次操作的库存快照，定时校验 Redis/MySQL 差异并触发补偿。取消订单通过 Seata AT 模式保证跨库事务。

**全文搜索**：Elasticsearch 替代 MySQL LIKE，支持中文分词 + geo_distance 距离排序 + 多字段组合筛选，搜索从秒级降到毫秒级。

**分库分表**：hmdp_0 / hmdp_1 双库，8 张分片表 + 7 张广播表，Snowflake 全局 ID 生成。

## 三、AI Agent 智能服务

hmdp-ai-agent-service（端口 8087）是项目的"智能大脑"，为原有系统注入三项 AI 能力：用自然语言搜索商铺、根据个人偏好推荐优惠券、结合好友动态推荐商铺。

### 3.1 为什么需要？

传统搜索的痛点："火锅"两个字，用户得先猜到商铺列表里有叫这名字的店，否则搜不到。而 AI 能理解"火锅是一种美食"，自动把查询映射到正确的类型筛选。

传统推荐的痛点：所有用户看到一样的"热门推荐"。AI 能根据你是谁——买过什么券、常逛哪片区、消费水平如何——给出针对你个人的推荐理由。

### 3.2 整体架构

```
浏览器/前端
  │
  ▼
Gateway (:8080)  ── /api/ai/** ──► hmdp-ai-agent-service (:8087)
                                      │
                         ┌────────────┼────────────┐
                         │            │            │
                       搜索         券推荐       商铺推荐
                         │            │            │
                    LLM 查类型ID   LLM 分析偏好  LLM 结合好友
                         │            │            │
                    Feign→ES     Feign→券列表  Feign→关注列表
                         │            │            │
                    后置过滤        返回Top5      返回Top5
                         │
                    返回结果 + 意图说明
```

三条链路共用一个 LLM 客户端（OkHttp 调 DeepSeek），按场景拼不同的 System Prompt。搜索走 Function Calling 提取结构化参数，推荐走纯文本推理返回 JSON 结果。

### 3.3 为什么不直接用 Spring AI？

Spring AI 1.0.x 面向 Spring Boot 3.x，Spring AI 2.0.0-Mx 支持 SB4 但是里程碑版本，已知与项目中的 reactor-core 和 ES 客户端版本冲突。

OkHttp 直调方案仅约 200 行代码就覆盖了全部需求：`LlmClient` 负责 HTTP 通信和 JSON 解析，`ToolDefinition`/`LlmToolResponse` 是通用抽象，OpenAI 兼容 API 零依赖冲突。

### 3.4 智能搜索 — "西湖区高分火锅店，人均100以内"

这是三个功能中最复杂的一个。核心挑战是：用户说的是人话，ES 要的是结构化参数（typeId、area、priceRange）。LLM 的 Function Calling 能力恰好解决这个"翻译"问题。

**流程**：

```
用户输入 "西湖区评分高的火锅店，人均100以内"
  │
  ▼
① AiSearchService 构建"工具"定义（告诉 LLM：
   "你有这样一个 extractSearchParams 函数可以调用，
   参数包括 keyword, typeId(1=美食,2=KTV,...), area, avgPriceMin/Max, sortBy"）
  │
  ▼
② LLM 判断：这是商铺搜索 → 调用 extractSearchParams
   返回：{ "typeId": 1, "area": "西湖区", "avgPriceMax": 100, "sortBy": "score" }
  │
  ▼
③ 如果 LLM 不调用工具（用户说了和搜索无关的话）
   → 返回 "抱歉，我只能帮您搜索商铺，例如：……"
  │
  ▼
④ 拿 typeId=1 → Feign 调 search-service → ES 返回所有美食商铺
  │
  ▼
⑤ postFilter：内存中筛 area 含"西湖区"、avgPrice≤100
  │
  ▼
⑥ 返回结果 + 友好意图说明：
   "区域西湖区，人均100元以内，为您找到3家商铺"
```

**工具定义是这个功能的心脏**。它不是硬编码的 if-else，而是一个通用的序列化结构：

```java
// AiSearchService — 构建工具定义，告诉 LLM 它能调什么、参数是什么
ToolDefinition searchTool = new ToolDefinition(
    "extractSearchParams",       // 工具名 — LLM 用它来决定是否调用
    "从自然语言中提取商铺搜索参数。与商铺搜索无关的查询不要调用此工具。"
        + "\n可用的商铺类型及ID: 1:美食, 2:KTV, 3:美发, ...",
    Map.of(
        "type", "object",
        "properties", Map.of(
            "keyword", Map.of("type", "string",
                "description", "商铺名称关键字，不要将菜系/食物类型名填入此字段"),
            "typeId", Map.of("type", "integer",
                "description", "食物类(火锅/川菜/日料)→1(美食)，KTV→2，美发→3"),
            "area", Map.of("type", "string"),
            "avgPriceMin", Map.of("type", "number"),
            "avgPriceMax", Map.of("type", "number"),
            "sortBy", Map.of("type", "string", "enum", List.of("score","avgPrice","distance"))
        )
    )
);
```

这个设计的精妙之处在于：**加一个搜索维度不需要写新代码**。未来要支持"按距离排序"或"按评分筛选"，只需在 properties Map 里加一个键值对，LLM 就会自动理解并提取。

**LLM 调用与分发**：

```java
// LlmClient — chatWithTools 方法的响应解析（OpenAI 兼容 API）
public LlmToolResponse chatWithTools(String systemPrompt, String userMessage,
                                      List<ToolDefinition> tools) {
    JSONObject body = buildRequestBody(systemPrompt, userMessage);
    body.set("tools", buildToolsArray(tools));
    body.set("tool_choice", "auto");              // ★ 告诉 LLM 自己决定是否调工具

    String raw = doRequest(body, 0);              // OkHttp POST → /v1/chat/completions
    return parseToolResponse(raw);
}

// AiSearchService — 根据 LLM 决策分发
LlmToolResponse result = llmClient.chatWithTools(prompt, query, List.of(searchTool));
if (!result.isToolCalled()) {
    return buildOutOfScope();                     // LLM 判断"不关我事" → 友好拒绝
}
SearchIntent intent = parseIntent(result.getArguments());
executeSearch(intent);                            // typeId→Feign→ES→postFilter
```

**为什么这样设计？** 如果把类型映射（火锅→美食）写成 Java switch-case，每新增一种商铺类型都要改代码、重新部署。把这件事交给 LLM，新增类型只要在工具描述中加一行文字。更重要的是，LLM 能处理"想吃辣的"→映射到川菜/火锅、处理"约会去处"→映射到高评分浪漫类型——这种语义泛化能力是硬编码做不到的。

**兜底策略 — typeId 优先于 keyword**：LLM 被明确引导"将菜系/食物类型映射到 typeId，不要放到 keyword 字段"。keyword 仅在用户明确说到商铺名称时才填充。这避免了全文搜索"火锅"时因为商铺名没包含"火锅"二字而返回空的结果。

### 3.5 个性化推荐 — 让推荐"懂你"

**优惠券推荐**的思路很直接：你是谁 → 有什么券 → 哪些最合适 → 给出理由。

```
POST /api/ai/recommend/voucher  { "userId": 1 }
  │
  ▼
① UserProfileService.formatForPrompt(userId)
   → "本周购买3次，浏览商铺12次，最近搜索:茶饮,烧肉,居酒屋"
  │
  ▼
② 如果画像为空（新用户）→ 降级：拉历史订单 + 用户等级做冷启动
  │
  ▼
③ Feign 调 voucher-service → 拿当前可用券列表（id, 标题, 面值, 门槛）
  │
  ▼
④ 拼 Prompt：
   "你是一个个性化推荐引擎。
    用户偏好: 本周购买3次，最近搜索茶饮、烧肉……
    以下是以 JSON 格式的可用券: [{id:1, title:"80元代金券", payValue:80……}]
    返回 Top5，JSON 数组，字段: id, name, reason, score (0-1)"
  │
  ▼
⑤ LLM 推理 → 返回 [{"id":1,"name":"80元代金券","reason":"本周购买活跃，性价比高","score":0.8}]
```

**商铺推荐**比券推荐多了一个维度——社交信号：

```java
// AiShopRecommendService — 社交上下文采集
StringBuilder socialCtx = new StringBuilder();
var followings = followInternalFeignClient.getFollowings(userId).getData();
if (followings != null && !followings.isEmpty()) {
    socialCtx.append("关注了").append(followings.size()).append("位用户");
}
// → Prompt 中加入 "社交关系: 关注了12位用户"
// → LLM 会将"好友常去""社交热门"等信号纳入推荐理由
```

这就是为什么券推荐的回答偏"性价比/消费匹配"，而商铺推荐的回答会出现"适合与朋友聚会""好友常去"——上下文不同。

### 3.6 用户画像系统 — 三层记忆

这是整个 Agent 服务的底座。没有画像，推荐就是盲推。

```
业务事件                               AI 推荐时读取
  │                                       │
  ▼                                       ▼
UserBehaviorConsumer                UserProfileService
  │                                  .formatForPrompt()
  ├──① tb_user_behavior                 │
  │   (MySQL 审计日志，只写不读)         │
  │                                     ▼
  ├──② user:behavior:recent:{id}       ③ user:profile:{id}
  │   (Redis List, LTRIM 50)          (Redis Hash, 推荐直接读)
  │
  └──③ user:profile:{id}
      (Redis Hash, 增量更新)
```

**为什么三层？** 最直观的做法是把原始事件流直接塞进 Prompt："用户搜了A、B、C，看了X、Y、Z，买了M、N……"。但几十条事件 = 几百 Token = 每次推荐浪费。更糟的是，LLM 每次都要重新从原始事件中归纳偏好，结果不稳定。

三层各司其职：
- **MySQL 层**：审计和重算来源。画像算错了可以从原始事件重放重建
- **Redis List 层**：最近 50 条行为的原文快照，用于需要查看"最近具体做了什么"的场景
- **Redis Hash 层**：聚合后的偏好摘要。推荐时直接格式化为 `"本周购买3次，浏览12次"` 进 Prompt，不用重新归纳

**增量更新 vs 全量重算**：

```java
// UserProfileService — 消费事件时增量更新 Hash
public void updateProfile(UserBehaviorEvent event) {
    String key = "user:profile:" + event.getUserId();

    switch (event.getEventType()) {
        case "PURCHASE":
            // 原子自增购买次数，无需先查再写
            redis.opsForHash().increment(key, "purchaseFrequency", 1);
            break;
        case "VIEW_SHOP":
            redis.opsForHash().increment(key, "shopViews", 1);
            break;
        case "SEARCH":
            // 最近搜索词 LPUSH + LTRIM，天然保持最新 5 条
            redis.opsForList().leftPush("user:behavior:recent:" + userId, keyword);
            redis.opsForList().trim(key, 0, 49);
            break;
    }
    redis.expire(key, 30, TimeUnit.DAYS);
}
```

所有更新操作都用了 Redis 原子命令（`hincrby`/`lpush`/`ltrim`），天然幂等——同一条消息重复消费不会导致计数翻倍。

**冷启动**：新用户画像为空 → `formatForPrompt` 返回空字符串 → `AiVoucherRecommendService` 检测到降级拉取历史订单和等级做冷启动。随着用户持续使用，画像自动积累，此后推荐不再需要降级。

### 3.7 行为采集管道

画像系统依赖各服务主动上报用户行为。这是分布式的——6 个服务各自埋点，通过 RabbitMQ 汇聚到 ai-agent-service：

```
shop-service    ──→ VIEW_SHOP ──┐
blog-service    ──→ LIKE_BLOG  ─┤
search-service  ──→ SEARCH    ──┼──→ user_behavior_topic ──→ UserBehaviorConsumer
voucher-service ──→ PURCHASE  ──┘        │
                                          ├─ MySQL (tb_user_behavior)
                                          ├─ Redis List (最近50条)
                                          └─ Redis Hash (增量画像)
```

每个服务新增一个约 15 行的 `UserBehaviorProducer`，继承框架的 `AbstractProducerHandler`：

```java
@Component
public class UserBehaviorProducer extends AbstractProducerHandler<MessageExtend<UserBehaviorEvent>> {
    public void send(UserBehaviorEvent event) {
        sendPayload(prefix + "-" + USER_BEHAVIOR_TOPIC, event);
    }
}
// 调用处只需要一行：
userBehaviorProducer.send(new UserBehaviorEvent(userId, "VIEW_SHOP", shopId, "shop", now));
```

消费端 `UserBehaviorConsumer` 继承 `AbstractConsumerHandler`，实现 `doConsume()`：写 DB → 更新 Redis 画像。框架自动处理消息反序列化、异常捕获、DLQ 路由。

---

## 四、快速部署

> 从零到跑通，6 步约 10 分钟。默认中间件运行在 `192.168.137.128`（Docker VM），后端在 `localhost`。

### 第一步：环境检查

```bash
java --version      # 需要 21+
mvn --version       # 需要 3.9+
node --version      # 需要 18+
docker --version    # 需要 Docker + Compose
```

### 第二步：启动中间件

```bash
cd docker
docker compose up -d        # MySQL + Redis + RabbitMQ + Nacos + Sentinel + ES + Seata

# 等 30 秒让容器就绪，然后验证
docker compose ps            # 全部 STATUS 应为 Up 或 healthy
curl -s http://192.168.137.128:8848/nacos   | head -1  # Nacos OK
curl -s http://192.168.137.128:9200          | head -1  # ES OK
```

> MySQL 数据库 `hmdp_0` / `hmdp_1` 和 AI 库 `hmdp_ai` 由 `docker/sql/` 自动建表。ES 索引由服务首次启动时自动创建。

### 第三步：配置环境变量

```bash
cp .env.example .env
# 默认值已指向 192.168.137.128，如果你的 VM IP 不同，编辑 .env 覆盖
```

### 第四步：上传 Nacos 配置 & 编译

```bash
cd docs/nacos-config && bash upload.sh && cd ../..
mvn clean compile -q    # -Dmysql.host=... 可按需覆盖
```

### 第五步：启动后端

```bash
# 推荐：开发模式（支持热更新，ShardingSphere 兼容）
mvn spring-boot:run -pl hmdp-gateway &
mvn spring-boot:run -pl hmdp-shop-service &
mvn spring-boot:run -pl hmdp-search-service &
mvn spring-boot:run -pl hmdp-user-service &
mvn spring-boot:run -pl hmdp-voucher-service &
mvn spring-boot:run -pl hmdp-blog-service &
mvn spring-boot:run -pl hmdp-follow-service &
mvn spring-boot:run -pl hmdp-ai-agent-service &

# 或：打包运行
# mvn clean package -DskipTests
# java -jar hmdp-gateway/target/hmdp-gateway-0.0.1-SNAPSHOT.jar
# ...（7 个服务各开一个终端）
```

**验证**：打开 Nacos 控制台 `http://192.168.137.128:8848/nacos` → 服务列表应出现 7 个服务 + 1 个 Gateway。

### 第六步：启动前端

```bash
cd hmdp-vue3
npm install && npm run dev    # → http://localhost:5173
```

### 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| Gateway 启动报 WebFlux 冲突 | 引了 Servlet 版 Sentinel | 确认 `hmdp-gateway` POM 用的是 `spring-cloud-alibaba-sentinel-gateway` |
| `java -jar` 启动报 ShardingSphere YAML 找不到 | 嵌套 JAR 路径问题 | 换 `mvn spring-boot:run`，或先 `jar xf` 解压再运行 |
| ES 查询报 `geo_point` 错误 | dynamic mapping 未识别为 geo_point | 删索引重启 search-service，`EsFullSyncInit` 会重建并显式映射 |
| Nacos 服务列表为空 | 服务未注册成功 | 检查各服务启动日志中 `nacos registry` 字样，确认 `bootstrap.yml` 中 `nacos.discovery.server-addr` 正确 |
| 前端页面空白 / 列表无数据 | JSONbig 精度问题 | 确保 `src/utils/request.js` 的 `transformResponse` 使用了 `JSONbig({ storeAsString: true })`，并在数据使用处做 `Number()` 转换 |

## 五、架构总览

### 5.1 架构全景图

```
                    ┌───────────────────────────────────────────────┐
                    │           Docker 192.168.137.128              │
                    │                                               │
  ┌──────────┐      │  ┌─────────┐ ┌───────┐ ┌──────────┐         │
  │ 浏览器    │      │  │ MySQL   │ │ Redis │ │RabbitMQ  │         │
  │ Vue3 SPA │      │  │ :3306   │ │:6379  │ │:5672/15672│        │
  │ :5173    │      │  │ 双库分片 │ │       │ │ 4.x      │         │
  └────┬─────┘      │  └────▲────┘ └──▲────┘ └────▲─────┘         │
       │            │       │         │           │                │
       │ /api →     │  ┌────┴─────────┴───────────┴─────┐          │
       │            │  │        Nacos :8848              │          │
  ┌────▼────────────┼──┤   服务发现 + 配置中心 3.1.0     │          │
  │    Gateway      │  └────────────────┬───────────────┘          │
  │    :8080        │                   │                           │
  │  Sentinel       │  ┌────────────────┼───────────────────┐      │
  │  网关限流       │  │       Sentinel :8081→8858         │      │
  └────┬────────────┼──┤       流量监控 Dashboard          │      │
       │            │  └────────────────┼───────────────────┘      │
       │ 负载均衡    │                   │                           │
       │ 路由分发    │  ┌────────────────┼───────────────────┐      │
       │            │  │       Seata    :8091                │      │
  ┌────┼────────────┼──┤     分布式事务 TC                   │      │
  │    │            │  └────────────────┼───────────────────┘      │
  │    │            │                                               │
  │    │    ┌───────┴───────────────────────────────┐              │
  │    │    │        ES :9200 (搜索引擎 8.16.3)     │              │
  │    │    └───────────────────────────────────────┘              │
  │    │            └──────────────────────────────────────────────┘
  │    │
  │    ├── /api/shop/** ──────► shop-service    :8081 ── 商铺CRUD
  │    ├── /api/shop/of/** ───► search-service  :8086 ── ES全文检索
  │    ├── /api/user/** ──────► user-service    :8082 ── 用户/登录
  │    ├── /api/voucher/** ───► voucher-service :8083 ── 秒杀全链路
  │    ├── /api/blog/** ──────► blog-service    :8084 ── 探店/Feed
  │    ├── /api/follow/** ────► follow-service  :8085 ── 关注/取关
  │    └── /api/ai/** ────────► ai-agent-service:8087 ── LLM智能大脑
  │                                    │
  │                          ┌─────────┼─────────┐
  │                          │ Feign    │ Feign   │ OkHttp
  │                          ▼          ▼         ▼
  │                     search-service  voucher   DeepSeek API
  │                     shop-service    follow    (外部 LLM)
  │                     user-service
  │
  │   ◄─── OpenFeign 跨服务调用 ─── MQ 异步解耦 ─── Sentinel 流量防护
  │
  │   Redis ── 缓存/分布式锁/布隆/令牌桶/用户画像
  │   MySQL ── ShardingSphere 双库分片 + Seata AT 分布式事务
  └────────────────────────────────────────────────────────────────
```

**数据流向**：前端所有请求统一走 Gateway → Nacos 发现服务实例 → Sentinel 做流量保护 → 路由到对应微服务。服务间需要跨库查询时通过 OpenFeign 调用（带 Auth header 传播），异步事件通过 RabbitMQ 发布/消费。AI Agent 作为独立服务，通过 Feign 获取业务数据后调用 DeepSeek 推理。

### 5.2 模块结构

```
hmdp-plus (父 POM)
├── hmdp-service-api          # 零依赖共享 POJO (Result, UserDTO, UserHolder, BaseCode)
├── hmdp-common               # 共享常量、拦截器、MvcConfig、缓存客户端
├── hmdp-parameter            # 共享 DTO/VO
├── hmdp-sharding             # SnakeYAML 兼容补丁 (ShardingSphere + SB4)

├── hmdp-shop-service         # :8081 商铺查询/类型/GEO、布隆过滤器、MQ 同步
├── hmdp-search-service       # :8086 ES 搜索（MQ 消费 shop 同步）
├── hmdp-user-service         # :8082 用户注册/登录/信息（含分片表）
├── hmdp-voucher-service      # :8083 秒杀全链路（RabbitMQ + 令牌桶 + Lua）
├── hmdp-blog-service         # :8084 博客/探店/点赞（Feign→User+Follow）
├── hmdp-follow-service       # :8085 关注/取关（Feign→User）
├── hmdp-ai-agent-service     # :8087 AI Agent（LLM 搜索/推荐/行为采集）

├── hmdp-gateway              # Spring Cloud Gateway :8080 (WebFlux)
├── hmdp-service-api-feign    # OpenFeign 接口 + FeignAuthConfig


├── hmdp-redisson-framework   # 分布式锁、布隆过滤器、幂等、延迟队列
├── hmdp-redis-tool-framework # Redis 缓存抽象 + 令牌桶限流
├── hmdp-id-generator-framework  # Snowflake ID 生成器
├── hmdp-mq-framework            # MQ 生产者/消费者抽象
└── hmdp-vue3                    # Vue 3 前端
```

所有框架模块通过 `META-INF/spring/*.imports` / `spring.factories` 自动装配，各服务无需 `@ComponentScan`。

### 5.3 跨服务调用（OpenFeign）

```java
@FeignClient(name = "hmdp-user-service", path = "/user")
public interface UserFeignClient {
    @GetMapping("/{id}")
    Result<UserDTO> getById(@PathVariable("id") Long id);
}
```

| 调用方 | Feign Client | 目标服务 |
|--------|-------------|----------|
| voucher | UserInfoFeignClient | hmdp-user-service |
| follow | UserFeignClient | hmdp-user-service |
| blog | UserFeignClient, FollowFeignClient | hmdp-user-service, hmdp-follow-service |
| ai-agent | SearchFeignClient, VoucherInternalFeignClient, FollowInternalFeignClient, ShopTypeFeignClient | hmdp-search-service, hmdp-voucher-service, hmdp-follow-service, hmdp-shop-service |
| voucher | AiVoucherRankingClient | hmdp-ai-agent-service |

`FeignAuthConfig` RequestInterceptor 自动传播 Authorization header。

### 5.4 框架模块一览

| 模块 | 核心能力 | 关键类 |
|------|---------|--------|
| `hmdp-redisson-service-framework` | 分布式锁、布隆过滤器、幂等、延迟队列 | `ServiceLockTool`, `BloomFilterHandlerFactory`, `@RepeatExecuteLimit` |
| `hmdp-redis-tool-framework` | 多级缓存读写、Key 规范、令牌桶限流 | `RedisCache`, `CacheClient`, `RateLimitHandler` |
| `hmdp-mq-producer-framework` | 消息发送封装、DLQ 路由 | `AbstractProducerHandler`, `MessageExtend` |
| `hmdp-mq-consumer-framework` | 消费模板方法、幂等集成 | `AbstractConsumerHandler` |
| `hmdp-id-generator-framework` | Snowflake ID 生成 | `SnowflakeIdGenerator` |

### 5.5 三个核心范式

**分布式锁 — `@ServiceLock` 注解**：声明式接入，支持读锁/写锁/公平锁/非公平锁，失败可自定义降级。

**消息发送 — `AbstractProducerHandler`**：继承后得到 `sendPayload()`/`sendToDlq()` 全套能力，业务只关心 payload。

**消息消费 — `AbstractConsumerHandler`**：实现 `doConsume()` 即可，前置/后置钩子、异常处理、DLQ 路由由框架管理。

## 六、数据初始化

### 6.1 布隆过滤器初始化

布隆过滤器是"可能误判存在、但绝不漏判不存在"的概率数据结构，作为缓存穿透的第一道防线：查询前先判断 ID 是否合法，不合法直接返回，不走 Redis 和 DB。

底层使用 Redisson 的 `RBloomFilter`（基于 Redis BitMap），通过 `BloomFilterHandlerFactory` 按业务维度管理（商铺、优惠券等）。服务启动时 `@PostConstruct` 自动从 DB 全量加载：

```java
@Order(1)
@Component
public class BloomFilterDataInit {
    @Resource private IShopService shopService;
    @Resource private BloomFilterHandlerFactory bloomFilterHandlerFactory;

    @PostConstruct
    public void init() {
        List<Shop> shopList = shopService.list();
        for (Shop shop : shopList) {
            bloomFilterHandlerFactory
                .get(BLOOM_FILTER_HANDLER_SHOP)
                .add(String.valueOf(shop.getId()));
        }
    }
}
```

多实例同时写 Redis BitMap 是幂等的。后续新增商铺时 `ShopServiceImpl.saveShop()` 会增量写入。

### 6.2 Elasticsearch 全量同步

搜索服务启动时（`@Order(2)`），如果 ES 索引为空，从 MySQL 批量导入全部商铺数据（每批 500 条）：

```java
@Order(2)
@Component
public class EsFullSyncInit {
    @PostConstruct
    public void init() {
        // 删除旧索引 → 创建新索引（显式声明 geo_point 映射）
        esClient.indices().delete(d -> d.index("shop"));
        esClient.indices().create(c -> c
                .index("shop")
                .mappings(m -> m.properties("location", p -> p.geoPoint(g -> g))));
        // 为空则全量同步
        if (esClient.count(c -> c.index("shop")).count() == 0) {
            shopSearchService.fullSync();
        }
    }
}
```

**`geo_point` 必须显式映射**：ES dynamic mapping 会将 `{lat, lon}` 识别为普通 `object`，导致 geo_distance 查询报错。日常增删改通过 MQ 异步增量同步 ES 索引。

## 七、缓存体系

缓存的三种经典风险及本项目解法：

| 风险 | 现象 | 解法 |
|------|------|------|
| **缓存穿透** | 不存在的 ID 反复查询，绕过缓存直击 DB | 布隆过滤器 + 空值缓存 |
| **缓存击穿** | 热点 Key 过期瞬间，大量请求同时打到 DB | 分布式锁 + 双重检测（DCL） |
| **缓存雪崩** | 大量 Key 同时过期，DB 瞬间过载 | TTL 随机偏移 + 多级缓存分摊 |

### 7.1 商铺查询的四层防线（`queryByIdV4` 源码走读）

```java
public Shop queryByIdV4(Long id){
    // 第 1 层：Redis 缓存命中直接返回
    Shop shop = redisCache.get(
        RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY, id), Shop.class);
    if (Objects.nonNull(shop)) { return shop; }

    // 第 2 层：布隆过滤器拦截非法 ID
    if (!bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_SHOP)
            .contains(String.valueOf(id))) {
        return null;  // 布隆说"一定不存在" → 直接拒绝
    }

    // 第 3 层：空值缓存短路（之前查过 DB 确认不存在）
    if (redisCache.hasKey(
            RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY_NULL, id))) {
        return null;
    }

    // 第 4 层：分布式锁 + 双重检测 + DB 回填
    RLock lock = serviceLockTool.getLock(LockType.Reentrant,
        LOCK_SHOP_KEY, new String[]{String.valueOf(id)});
    lock.lock();
    try {
        // ★ 双重检测：再次确认 null cache 和 entity cache（可能被其他线程写入）
        if (redisCache.hasKey(RedisKeyBuild.createRedisKey(
                RedisKeyManage.CACHE_SHOP_KEY_NULL, id))) { return null; }
        shop = redisCache.get(RedisKeyBuild.createRedisKey(
                RedisKeyManage.CACHE_SHOP_KEY, id), Shop.class);
        if (Objects.nonNull(shop)) { return shop; }

        // 查 DB
        shop = getById(id);
        if (Objects.isNull(shop)) {
            // DB 也没有 → 写空值缓存（短 TTL，防穿透）
            redisCache.set(RedisKeyBuild.createRedisKey(
                RedisKeyManage.CACHE_SHOP_KEY_NULL, id), "这是一个空值",
                CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return null;
        }
        // DB 有数据 → 写实体缓存
        redisCache.set(RedisKeyBuild.createRedisKey(
            RedisKeyManage.CACHE_SHOP_KEY, id), shop, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    } finally {
        lock.unlock();
    }
}
```

**请求流程**：

```
请求 → [L1] Redis entity cache 命中？ → 返回
          ↓ miss
        [L2] 布隆过滤器 contains(id)？ → 不存在 → return null
          ↓ 存在
        [L3] Redis null cache 命中？ → 命中 → return null
          ↓ miss
        [L4] 获取分布式锁 → 双重检测（null cache / entity cache）
          ↓ 两次都 miss
        查 DB → null 则写 null cache : 写 entity cache → 释放锁 → 返回
```

**为什么需要双重检测？** 100 个请求同时发现缓存未命中，排队竞争同一把锁。第 1 个拿锁的线程查 DB 回写缓存，后续 99 个拿锁后先再查一次缓存——第 1 个已经重建好了，不需要再打 DB。

### 7.2 写操作：Cache-Aside 模式

```java
@Transactional
public Result update(Shop shop) {
    updateById(shop);                                  // 1. 先更新 DB
    stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId()); // 2. 再删缓存
    return Result.ok();
}
```

**删缓存而非更新缓存**：并发更新可能"写覆盖"（A 先更新 B 后更新，但 B 先写缓存 A 后写，缓存里是 A 的旧值）。删缓存让下一次读触发重建，天然最新，且删除是幂等的。

### 7.3 秒杀优惠券三级缓存（Caffeine → Redis → DB）

秒杀优惠券比商铺更热，引入 Caffeine 本地缓存作为 L1。完整执行路径如下（源码 `SeckillVoucherServiceImpl.queryByVoucherId()`）：

```
请求
  → [L1]  Caffeine 本地缓存         命中 → 返回（纳秒级）
           ↓ miss
  → [L2]  Redis 分布式缓存          命中 → 回填 L1 → 返回（毫秒级）
           ↓ miss
  → [L3-①] 布隆过滤器 contains(id)？不存在 → 抛异常
           ↓ 存在
  → [L3-②] Redis 空值缓存命中？     命中 → 抛异常
           ↓ 无空值记录
  → [L3-③] 获取分布式锁
           ↓ 拿到锁
  → [L3-④] 双重检测：再查 Redis 实体缓存 + 空值缓存
           ↓ 均 miss
  → [L3-⑤] 查 DB → 写 Redis → 写 Caffeine → 返回
```

**为什么布隆过滤器在 Redis 之后而不是之前？** 布隆过滤器底层是 Redisson `RBloomFilter`（基于 Redis BitMap），查它也需要一次网络调用。热数据大概率命中 L2 Redis，先查 Redis 能省掉这次调用。布隆只在 Redis miss 时才发挥作用——拦截非法 ID，避免无意义的 DB 查询。这和商铺查询 `queryByIdV4` 的逻辑一致：布隆始终在 Redis miss 之后、打 DB 之前。

查询方法用 `@ServiceLock(lockType = LockType.Read)`（读锁，可并发）。Caffeine 自动 LRU 淘汰，多实例各自维护一份，由 Redis TTL 过期自然触发刷新。

## 八、MQ 消息可靠性

秒杀链路中「Redis 扣减 → MQ 投递 → 消费者创建订单」三步必须可靠衔接。任一步失败都可能导致库存扣了但订单没生成。

### 8.1 消息框架封装

`hmdp-mq-framework` 提供三层抽象：

- **`AbstractProducerHandler`**：统一发送入口，try-catch + 成功/失败钩子 + DLQ 路由
- **`MessageExtend`**：消息包装器，自动生成 uuid（唯一 ID）、producerTime、headers
- **`AbstractConsumerHandler`**：模板方法 `beforeConsume → doConsume → afterConsumeSuccess/afterConsumeFailure`

### 8.2 发送端

```java
public final void sendMqMessage(String exchange, M message) {
    try {
        rabbitTemplate.convertAndSend(exchange, "", message, msg -> {
            // 透传 headers 到 RabbitMQ 消息属性
            message.getHeaders().forEach((k, v) -> {
                if (k != null && v != null) msg.getMessageProperties().setHeader(k, v);
            });
            return msg;
        });
        afterSendSuccess(exchange, message);
    } catch (Exception ex) {
        afterSendFailure(exchange, message, ex);  // 打日志 + 记录异常
    }
}

// DLQ 路由（发送失败后补偿）
public final <T> void sendToDlq(String originalExchange, T payload, String reason) {
    String dlqExchange = originalExchange + ".DLQ";
    sendRecord(dlqExchange, MessageExtend.of(payload)
            .setHeaders(Map.of("dlqReason", reason)));
}
```

### 8.3 消费端

```java
// AbstractConsumerHandler 模板
public final void consume(MessageExtend<T> message) {
    if (beforeConsume(message)) {
        try { doConsume(message); }
        catch (Throwable t) { afterConsumeFailure(message, t); throw t; }
    }
    afterConsumeSuccess(message);
}

// 秒杀订单创建的具体实现
@RepeatExecuteLimit(name = SECKILL_VOUCHER_ORDER, keys = {"#message.uuid"})  // ★ 幂等
@Transactional(rollbackFor = Exception.class)
public boolean createVoucherOrderV2(MessageExtend<SeckillVoucherMessage> message) {
    // 1. 幂等校验：同用户同券是否已有 NORMAL 订单
    if (existsNormalOrder(message.getUserId(), message.getVoucherId())) {
        throw new HmdpFrameException(BaseCode.VOUCHER_ORDER_EXIST);
    }
    // 2. DB 扣减库存（乐观锁 WHERE stock > 0）
    // 3. INSERT tb_voucher_order
    // 4. INSERT tb_voucher_order_router（分片路由）
    // 5. Redis 缓存订单（60s，让前端快速查"已抢到"）
    // 6. INSERT tb_voucher_reconcile_log（对账日志）
}
```

**双重幂等**：`@RepeatExecuteLimit` 以 `message.uuid` 为幂等键，即使业务层 `existsNormalOrder` 漏过（极端并发），框架也会拦截。

**防无限重投**：Nacos 配置 `default-requeue-rejected: false`，消费失败的消息不 requeue，由 `afterConsumeFailure` 路由到 DLQ 等待人工处理。

### 8.4 可靠性全景

```
sendPayload(topic, payload)
    ↓
AbstractProducerHandler.sendMqMessage()
    ├── 成功 → afterSendSuccess（记日志）
    └── 失败 → afterSendFailure → sendToDlq(DLQ, payload, reason)
                                          ↓
                                   死信队列 .DLQ → 人工补偿
                   RabbitListener on <topic>.queue
                        ↓
           AbstractConsumerHandler.consume()
              ├── beforeConsume
              ├── doConsume（@RepeatExecuteLimit 幂等）
              │     ├── 成功 → afterConsumeSuccess → ACK
              │     └── 失败 → afterConsumeFailure → DLQ
              └── throw → RabbitMQ 感知（不 requeue）
```

## 九、秒杀全链路与一致性保障

### 9.1 请求全链路

```
用户点击"抢购"
  → 令牌前置授权 (SeckillAccessTokenService) — 验证抢购资格令牌
  → 令牌桶限流 (RateLimitHandler) — 按用户/IP 维度放行
  → 秒杀信息查询 (queryByVoucherId) — Caffeine L1 → Redis L2 → DB L3
  → 库存预热 (loadVoucherStock) — MySQL stock → Redis
  → 用户等级校验 (verifyUserLevel) — Feign 查 user-service
  → ★ Lua 原子扣减 (seckillVoucher.lua) — 校验时间/库存/重复 → 扣减 → 标记 → 日志
  → MQ 投递 (SeckillVoucherProducer) — 发送订单创建消息
  → 立即返回 orderId — 不等 DB 落库
  → [异步] MQ 消费者创建订单 (createVoucherOrderV2) — 幂等 → DB 扣减 → 订单 → 对账
```

关键设计：前端看到的是"抢购成功"而非"订单创建成功"。Lua 完成即返回，响应时间从 DB 写延迟降为 Redis Lua 执行延迟（毫秒级）。

### 9.2 Java 主流程

```java
public Result<Long> doSeckillVoucherV2(Long voucherId) {
    SeckillVoucherFullModel model = seckillVoucherService.queryByVoucherId(voucherId);
    seckillVoucherService.loadVoucherStock(voucherId);
    Long userId = UserHolder.getUser().getId();
    verifyUserLevel(model, userId);                        // Feign 查等级
    long orderId = snowflakeIdGenerator.nextId();

    // 构建 Lua 参数：3 个 Key + 9 个 Arg
    SeckillVoucherDomain domain = seckillVoucherOperate.execute(keys, args);
    if (!domain.getCode().equals(BaseCode.SUCCESS.getCode())) {
        throw new HmdpFrameException(BaseCode.getRc(domain.getCode()));
    }

    // 扣减成功 → 发送 MQ（含 beforeQty/deductQty/afterQty 快照）
    seckillVoucherProducer.sendPayload(prefix + "-" + SECKILL_VOUCHER_TOPIC,
        new SeckillVoucherMessage(userId, voucherId, orderId, traceId,
            domain.getBeforeQty(), domain.getDeductQty(), domain.getAfterQty(), false));
    return Result.ok(orderId);
}
```

### 9.3 Lua 原子扣减脚本

Redis 单线程执行 Lua 保证下面全部操作的原子性：

```lua
local stockKey = KEYS[1]       -- 库存
local seckillUserKey = KEYS[2] -- 已购用户 Set
local traceLogKey = KEYS[3]    -- 追踪日志 Hash

-- 1. 时间窗口校验（用 Redis TIME 命令，不依赖客户端时钟）
if nowMillis < beginTime then return '{"code": 10002}' end  -- 未开始
if nowMillis > endTime   then return '{"code": 10003}' end  -- 已结束
if status == 2           then return '{"code": 10011}' end  -- 已禁用

-- 2. 库存校验
local stock = redis.call('get', stockKey)
if not stock or tonumber(stock) <= 0 then return '{"code": 10005}' end

-- 3. 重复购买校验（Set O(1) 成员判断）
if redis.call('sismember', seckillUserKey, userId) == 1 then
    return '{"code": 10006}'
end

-- 4. ★ 原子操作：扣减 + 标记 + 日志
redis.call('incrby', stockKey, -1)
redis.call('sadd', seckillUserKey, userId)
redis.call('hset', traceLogKey, traceId, cjson.encode({...}))

-- 5. 返回库存快照
return string.format('{"code": 0, "beforeQty": %s, "deductQty": %s, "afterQty": %s}',
    beforeQty, 1, beforeQty - 1)
```

**为什么是 Lua 而不是 Java 锁？** `get stock → 判断 → decr → sadd` 四条命令在 Lua 中一次网络往返原子执行。Java 端调四条命令需要四次往返，中间可能被其他请求插入。

### 9.4 对账与一致性保障

秒杀涉及 Redis、RabbitMQ、MySQL 三个存储。每次操作都写入 `tb_voucher_reconcile_log`：

| 字段 | 含义 |
|------|------|
| `trace_id` | 全链路追踪 ID |
| `log_type` | DEDUCT（扣减）/ RESTORE（回滚） |
| `business_type` | SUCCESS / CANCEL / COMPENSATE |
| `before_qty` / `change_qty` / `after_qty` | 库存快照 |

**校验规则**：按 `voucher_id` 汇总 `change_qty` = 初始库存 - 当前 MySQL `stock`。不一致则触发补偿。

### 9.5 取消订单（Seata 全局事务）

```java
@GlobalTransactional(timeoutMills = 60000)
@Transactional(rollbackFor = Exception.class)
public Boolean cancel(CancelVoucherOrderDto dto) {
    // 1. 查订单 → 更新状态为 CANCEL
    // 2. 写入对账日志（RESTORE + CANCEL）
    // 3. DB 库存 +1（Seata AT 保证跨库回滚）
    // 4. 成功后同步 Redis：
    //    - rollbackRedisVoucherData（库存 +1、移除已购 Set）
    //    - 更新每日 Top 买家 ZSet
    //    - ★ 自动发券给队列中下一位候选人
}
```

## 十、防刷与到券提醒

### 3.1 双层防线

| 概念 | 令牌（Access Token） | 令牌桶（Token Bucket） |
|------|---------------------|----------------------|
| 目的 | 验证用户是否有抢购资格 | 控制请求速率 |
| 发放 | 活动前发给目标用户 | 系统持续运行 |
| 粒度 | 按用户+活动 | 按 IP/用户/接口 |
| 实现 | Redis 存储 | Redis Lua 令牌桶 |

资格控制 + 速率控制分离，互不干扰。

### 3.2 到券提醒

库存归零后用户可订阅"到券提醒"，取消订单或补货时自动发给最早订阅的用户。

**数据模型**：Redis ZSet `seckill:subscribe:zset:{voucherId}`，score = 订阅时间，value = userId。

```java
public boolean autoIssueVoucherToEarliestSubscriber(Long voucherId, Long excludeUserId) {
    String candidate = findEarliestCandidate(voucherId, excludeUserId);
    // 从 ZSet 按 score 升序扫描，过滤已购用户和 cancel 原用户
    if (candidate == null) return false;
    return issueToCandidate(voucherId, candidate, model);
    // issueToCandidate 流程：校验等级 → Lua 扣减 → MQ 投递 → 订单创建
    // 与用户主动抢购完全一致，区别是消息中标记为自动发券
}
```

## 十一、前端功能

前端基于 Vue 3.5 + Vite 6 + Element Plus 2.9，10 个页面。关键交互优化：

- **抢购流程优化**：页面样式重设计，抢购中弹出加载提示，只有真正抢购成功才提示成功，否则提示失败
- **取消订单**：抢购成功后支持取消，库存自动回流触发到券提醒
- **到券提醒**：优惠券售罄后可订阅，库存回流后按订阅时间最早优先通知
