# 黑马点评 Plus — 微服务魔改版

> 原项目 [hmdp-plus](https://gitee.com/java-up-up/hmdp-plus) 由 **阿星不是程序员（JavaUp）** 开发，在经典"黑马点评"基础上补齐了令牌桶限流、Kafka 可靠消息、Lua 原子扣减、分库分表、对账一致性闭环等高并发能力。基于 Spring Boot 3 构建，是付费知识星球的专属内容。
>
> **本仓库是在原作基础上的魔改版本**，主要变更：(1) Spring Boot 3 → 4，(2) Kafka → RabbitMQ，(3) 单体架构拆分为 6 个独立微服务，(4) 为 Spring Cloud Alibaba 全套体系铺路（Nacos + Gateway + OpenFeign + Sentinel + Seata），(5) 新增 Elasticsearch 搜索引擎替代 MySQL LIKE + Redis GEO。

## 一、项目概述

"黑马点评"是 Java 学习圈中广为人知的入门实战项目，围绕"本地生活与商户运营"场景展开：商户浏览与查询、优惠券发放与抢购、达人探店、好友关注等。但原版在面试中容易被深问：流量突发时如何限流？Redis/MQ 宕机怎么办？缓存与 DB 不一致怎么办？**阿星不是程序员（JavaUp）** 的 hmdp-plus 针对这些问题给出了工程方案，本仓库在此基础上进一步魔改。

### 与原项目的差异

| 维度 | 原 hmdp-plus | 魔改版 |
|------|-------------|--------|
| Spring Boot | 3.x | **4.0.6** |
| 消息队列 | Kafka | **RabbitMQ 4.x** |
| 架构 | 单体 | **6 微服务** + Gateway |
| 跨域调用 | 进程内 | **OpenFeign**（Nacos 服务发现） |
| 服务发现 | 无 | **Nacos 3.1.0** |
| 网关 | 无 | **Spring Cloud Gateway**（:8080） |
| 搜索 | MySQL LIKE + Redis GEO | **Elasticsearch**（全文检索 + geo_distance） |
| 事务 | 本地事务 | Seata 分布式事务（AT 模式，2.5.0） |

### 6 个微服务

| 服务 | 端口 | 职责 |
|------|------|------|
| `hmdp-gateway` | 8080 | Spring Cloud Gateway 统一入口 |
| `hmdp-shop-service` | 8081 | 商铺 CRUD、布隆过滤器、MQ 同步事件 |
| `hmdp-search-service` | 8086 | ES 搜索引擎（名称全文检索/类型筛选/GEO 距离排序） |
| `hmdp-user-service` | 8082 | 用户注册/登录/信息管理（分片表） |
| `hmdp-voucher-service` | 8083 | 秒杀全链路（Lua 扣减 → RabbitMQ → 订单 → 对账） |
| `hmdp-blog-service` | 8084 | 博客/探店/点赞、Feed 滚动分页 |
| `hmdp-follow-service` | 8085 | 关注/取关/共同关注 |

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

## 二、Plus 版核心改进

相比普通版黑马点评，Plus 版在以下维度做了系统性的加固。详细设计与源码见后续章节。

**流量控制**：令牌前置授权（资格控制）+ 令牌桶限流（速率控制）双层防线，将非法请求拦截在扣减链路之外。

**缓存体系**：本地缓存（Caffeine L1）→ 布隆过滤器 → Redis（L2）→ 分布式锁 + 双重检测 → DB，四层防线解决穿透、击穿、雪崩。

**MQ 可靠性**：基于 RabbitMQ 的生产确认 + 消费手动 ACK + 死信队列（DLQ），配合 `@RepeatExecuteLimit` 幂等消费和 Outbox 模式，保证"扣减成功则订单必落库"。

**秒杀链路**：Lua 脚本在 Redis 单线程内完成库存校验→扣减→标记用户→写追踪日志，全程原子。扣减完成后异步 MQ 创建订单，响应时间从 DB 写延迟降为 Redis Lua 执行延迟。

**一致性保障**：对账日志表 `tb_voucher_reconcile_log` 记录每次操作的库存快照，定时校验 Redis/MySQL 差异并触发补偿。取消订单通过 Seata AT 模式保证跨库事务。

**分库分表**：hmdp_0 / hmdp_1 双库，8 张分片表 + 7 张广播表，Snowflake 全局 ID 生成。

## 三、快速部署

### 3.1 环境要求

| 依赖 | 版本 | 用途 |
|------|------|------|
| JDK | 21+ | 编译与运行 |
| Maven | 3.9+ | 构建（Windows 需手动下载配 PATH） |
| Node.js | 18+ | 前端构建 |
| Docker + Compose | 最新版 | 一键启动全部中间件 |

### 3.2 启动中间件

```bash
cd docker
docker compose up -d    # 启动全部 7 个容器

# 验证
docker compose ps        # 全部 STATUS 应为 Up 或 healthy
curl http://192.168.137.128:8848/nacos   # Nacos 控制台
curl http://192.168.137.128:15672         # RabbitMQ 管理界面（guest/guest）
curl http://192.168.137.128:9200          # ES 健康检查
curl http://192.168.137.128:8081          # Sentinel Dashboard（sentinel/sentinel）
```

MySQL 数据库 `hmdp_0` 和 `hmdp_1` 由 `docker/sql/` 脚本自动初始化。

### 3.3 配置连接

**方式一（推荐）**：复制环境变量模板

```bash
cp .env.example .env
# 编辑 .env 中的 MYSQL_HOST / REDIS_HOST / RABBITMQ_HOST / NACOS_HOST 等
```

**方式二**：Maven 编译参数（各服务 `shardingsphere.yaml` 通过资源过滤替换）

```bash
mvn clean compile -Dmysql.host=192.168.137.128 -Dmysql.port=3306 -Dmysql.user=root -Dmysql.password=root
```

### 3.4 上传 Nacos 配置

项目所有业务配置托管在 Nacos，配置参考文件在 `docs/nacos-config/`：

```bash
cd docs/nacos-config
bash upload.sh   # 一键上传到 Nacos（Nacos 需已启动）
```

### 3.5 构建并启动后端

```bash
mvn clean package -DskipTests

# Gateway + 6 个微服务（各开一个终端）：
java -jar hmdp-gateway/target/hmdp-gateway-0.0.1-SNAPSHOT.jar         # :8080
java -jar hmdp-shop-service/target/hmdp-shop-service-0.0.1-SNAPSHOT.jar    # :8081
java -jar hmdp-search-service/target/hmdp-search-service-0.0.1-SNAPSHOT.jar # :8086
java -jar hmdp-user-service/target/hmdp-user-service-0.0.1-SNAPSHOT.jar    # :8082
java -jar hmdp-voucher-service/target/hmdp-voucher-service-0.0.1-SNAPSHOT.jar # :8083
java -jar hmdp-blog-service/target/hmdp-blog-service-0.0.1-SNAPSHOT.jar    # :8084
java -jar hmdp-follow-service/target/hmdp-follow-service-0.0.1-SNAPSHOT.jar # :8085
```

也可用 `mvn spring-boot:run -pl <模块名>` 开发模式。注意 ShardingSphere 用 `ResourceUtils.getFile()` 读取 YAML，嵌套 JAR 无法正确解析路径，必须用 `spring-boot:run` 或 `java -jar`。

启动成功标志：Nacos 控制台「服务列表」可见 7 个服务；访问 `http://localhost:8080/api/shop-type/list` 返回数据。

### 3.6 启动前端

```bash
cd hmdp-vue3
npm install && npm run dev   # → http://localhost:5173
```

Vite 将 `/api` 代理到 `localhost:8080`（Gateway），前端零配置连通后端。

## 四、架构总览

### 4.1 模块结构

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

├── hmdp-gateway              # Spring Cloud Gateway :8080 (WebFlux)
├── hmdp-service-api-feign    # OpenFeign 接口 + FeignAuthConfig


├── hmdp-redisson-framework   # 分布式锁、布隆过滤器、幂等、延迟队列
├── hmdp-redis-tool-framework # Redis 缓存抽象 + 令牌桶限流
├── hmdp-id-generator-framework  # Snowflake ID 生成器
├── hmdp-mq-framework            # MQ 生产者/消费者抽象
└── hmdp-vue3                    # Vue 3 前端
```

所有框架模块通过 `META-INF/spring/*.imports` / `spring.factories` 自动装配，各服务无需 `@ComponentScan`。

### 4.2 跨服务调用（OpenFeign）

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

`FeignAuthConfig` RequestInterceptor 自动传播 Authorization header。

### 4.3 框架模块一览

| 模块 | 核心能力 | 关键类 |
|------|---------|--------|
| `hmdp-redisson-service-framework` | 分布式锁、布隆过滤器、幂等、延迟队列 | `ServiceLockTool`, `BloomFilterHandlerFactory`, `@RepeatExecuteLimit` |
| `hmdp-redis-tool-framework` | 多级缓存读写、Key 规范、令牌桶限流 | `RedisCache`, `CacheClient`, `RateLimitHandler` |
| `hmdp-mq-producer-framework` | 消息发送封装、DLQ 路由 | `AbstractProducerHandler`, `MessageExtend` |
| `hmdp-mq-consumer-framework` | 消费模板方法、幂等集成 | `AbstractConsumerHandler` |
| `hmdp-id-generator-framework` | Snowflake ID 生成 | `SnowflakeIdGenerator` |

### 4.4 三个核心范式

**分布式锁 — `@ServiceLock` 注解**：声明式接入，支持读锁/写锁/公平锁/非公平锁，失败可自定义降级。

**消息发送 — `AbstractProducerHandler`**：继承后得到 `sendPayload()`/`sendToDlq()` 全套能力，业务只关心 payload。

**消息消费 — `AbstractConsumerHandler`**：实现 `doConsume()` 即可，前置/后置钩子、异常处理、DLQ 路由由框架管理。

## 五、数据初始化

### 5.1 布隆过滤器初始化

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

### 5.2 Elasticsearch 全量同步

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

## 六、缓存体系

缓存的三种经典风险及本项目解法：

| 风险 | 现象 | 解法 |
|------|------|------|
| **缓存穿透** | 不存在的 ID 反复查询，绕过缓存直击 DB | 布隆过滤器 + 空值缓存 |
| **缓存击穿** | 热点 Key 过期瞬间，大量请求同时打到 DB | 分布式锁 + 双重检测（DCL） |
| **缓存雪崩** | 大量 Key 同时过期，DB 瞬间过载 | TTL 随机偏移 + 多级缓存分摊 |

### 6.1 商铺查询的四层防线（`queryByIdV4` 源码走读）

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

### 6.2 写操作：Cache-Aside 模式

```java
@Transactional
public Result update(Shop shop) {
    updateById(shop);                                  // 1. 先更新 DB
    stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId()); // 2. 再删缓存
    return Result.ok();
}
```

**删缓存而非更新缓存**：并发更新可能"写覆盖"（A 先更新 B 后更新，但 B 先写缓存 A 后写，缓存里是 A 的旧值）。删缓存让下一次读触发重建，天然最新，且删除是幂等的。

### 6.3 秒杀优惠券三级缓存（Caffeine → Redis → DB）

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

## 七、MQ 消息可靠性

秒杀链路中「Redis 扣减 → MQ 投递 → 消费者创建订单」三步必须可靠衔接。任一步失败都可能导致库存扣了但订单没生成。

### 7.1 消息框架封装

`hmdp-mq-framework` 提供三层抽象：

- **`AbstractProducerHandler`**：统一发送入口，try-catch + 成功/失败钩子 + DLQ 路由
- **`MessageExtend`**：消息包装器，自动生成 uuid（唯一 ID）、producerTime、headers
- **`AbstractConsumerHandler`**：模板方法 `beforeConsume → doConsume → afterConsumeSuccess/afterConsumeFailure`

### 7.2 发送端

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

### 7.3 消费端

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

### 7.4 可靠性全景

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

## 八、秒杀全链路与一致性保障

### 8.1 请求全链路

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

### 8.2 Java 主流程

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

### 8.3 Lua 原子扣减脚本

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

### 8.4 对账与一致性保障

秒杀涉及 Redis、RabbitMQ、MySQL 三个存储。每次操作都写入 `tb_voucher_reconcile_log`：

| 字段 | 含义 |
|------|------|
| `trace_id` | 全链路追踪 ID |
| `log_type` | DEDUCT（扣减）/ RESTORE（回滚） |
| `business_type` | SUCCESS / CANCEL / COMPENSATE |
| `before_qty` / `change_qty` / `after_qty` | 库存快照 |

**校验规则**：按 `voucher_id` 汇总 `change_qty` = 初始库存 - 当前 MySQL `stock`。不一致则触发补偿。

### 8.5 取消订单（Seata 全局事务）

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

## 九、防刷与到券提醒

### 9.1 双层防线

| 概念 | 令牌（Access Token） | 令牌桶（Token Bucket） |
|------|---------------------|----------------------|
| 目的 | 验证用户是否有抢购资格 | 控制请求速率 |
| 发放 | 活动前发给目标用户 | 系统持续运行 |
| 粒度 | 按用户+活动 | 按 IP/用户/接口 |
| 实现 | Redis 存储 | Redis Lua 令牌桶 |

资格控制 + 速率控制分离，互不干扰。

### 9.2 到券提醒

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

## 十、前端功能

前端基于 Vue 3.5 + Vite 6 + Element Plus 2.9，10 个页面。关键交互优化：

- **抢购流程优化**：页面样式重设计，抢购中弹出加载提示，只有真正抢购成功才提示成功，否则提示失败
- **取消订单**：抢购成功后支持取消，库存自动回流触发到券提醒
- **到券提醒**：优惠券售罄后可订阅，库存回流后按订阅时间最早优先通知
