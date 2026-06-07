# 黑马点评 Plus 微服务迁移大纲

## 整体目标

将现有 Spring Boot 4.0.6 + Java 21 单体项目拆分为微服务架构：

- **网关**: Spring Cloud Gateway
- **注册/配置中心**: Nacos
- **远程调用**: OpenFeign
- **流量保护**: Sentinel
- **消息队列**: Kafka → RabbitMQ
- **分布式事务**: Seata (AT 模式，秒杀除外)
- **搜索引擎**: Elasticsearch 替代 Redis GEO + MySQL LIKE
- **AI 推荐**: Spring AI Agent 微服务（接入 DeepSeek 等大模型）

---

## 阶段一：Docker 中间件环境部署 ✅

部署所有中间件到 Ubuntu 24.04 虚拟机 (192.168.137.128)。

**产出:**
- `docker/docker-compose.yml` — 8 个容器编排
- `docker/seata-conf/registry.conf` — Seata 注册到 Nacos
- `docker/seata-conf/application.yml` — Seata 自身配置
- `sql/hmdp_0.sql` / `sql/hmdp_1.sql` — 修复 mysqldump 头，自动初始化

**容器清单:**

| 服务 | 镜像 | 端口 |
|---|---|---|
| MySQL 8.0 | `mysql:8.0` | 3306 |
| Redis 7 | `redis:7-alpine` | 6379 |
| RabbitMQ 4.x | `rabbitmq:4-management-alpine` | 5672 / 15672 |
| Nacos 2.4.3 | `nacos/nacos-server:v2.4.3` | 8848 / 9848 |
| Sentinel Dashboard 1.8.8 | `bladex/sentinel-dashboard:1.8.8` | 8081→8080 |
| Elasticsearch 8.16 | `docker.elastic.co/elasticsearch/elasticsearch:8.16.3` | 9200 / 9300 |
| Seata 1.8.0 | `seataio/seata-server:1.8.0` | 8091 / 7091 |

**验证:**
```bash
docker compose up -d
docker compose ps   # 全部 (healthy)
```

---

## 阶段二：单体现有代码编译验证 ✅

在不动业务代码的前提下，适配 Spring Boot 4.0.6 + Java 21。

**产出:**
- `pom.xml` — java.version 17→21, spring-boot.version 3.5.4→4.0.6
- `shardingsphere.yaml` — MySQL 地址 + 密码更新
- `application.yml` — Redis/Kafka 地址更新

**已修复的兼容性问题:**

| 问题 | 修复 |
|---|---|
| `spring-boot-starter-aop` 不存在 | 改为 `spring-boot-starter-aspectj` |
| `RedisProperties` 包路径变更 | → `DataRedisProperties` (spring-boot-data-redis) |
| Jackson 3 包名不兼容 | 排除 `spring-boot-starter-jackson`，添加 `spring-boot-jackson2` |
| `Jackson2ObjectMapperBuilderCustomizer` 包变更 | import 改为 `spring-boot.jackson2.autoconfigure` |

**验证:**
```bash
export PATH="$HOME/apache-maven/apache-maven-3.9.9/bin:$PATH"
mvn clean compile -pl hmdp-core-service -am -DskipTests
# BUILD SUCCESS (21/21 modules)
```

---

## 阶段三：Kafka → RabbitMQ 迁移 ✅

重写消息队列框架层，替换底层实现。

### 3.1 改造 hmdp-mq-framework ✅

- 3 个 POM: `spring-kafka` → `spring-boot-starter-amqp`
- `AbstractProducerHandler.java` — `KafkaTemplate` → `RabbitTemplate`，`SendResult` → `void`，`ProducerRecord` → `MessagePostProcessor` 设 headers
- `AbstractConsumerHandler.java` — 日志措辞更新 (kafka→mq)，模板方法模式不变
- `MessageExtend.java` — 保持不变（MQ 无关的纯 POJO）

### 3.2 改造 hmdp-core-service ✅

- `kafka/` 包 → `rabbitmq/` 包 (8 个文件)
- `config/RabbitMQConfig.java` — 新增 Exchange/Queue/Binding 声明 + `Jackson2JsonMessageConverter`
- `@KafkaListener` → `@RabbitListener(queues = ...)`，`Acknowledgment` → `Channel.basicAck()`
- `application.yml` — `spring.kafka.*` → `spring.rabbitmq.*`
- `HmDianPingApplication.java` — 新增 `@EnableRabbit`
- 更新 7 个引用文件的 import (`VoucherOrderServiceImpl`, `VoucherReconcileLogService`, `SeckillVoucherCacheInvalidationPublisher` 等)

### 3.3 验证 ✅

```bash
mvn clean compile -pl hmdp-core-service -am -DskipTests
# BUILD SUCCESS (21/21 modules)
```

---

## 阶段四：微服务拆分 ✅

将单体 `hmdp-core-service` 拆分为 5 个独立可部署的微服务 + 1 个共享 API 模块。共享数据库，无服务发现/RPC，跨域只读通过 Bridge 模式（直接 DB 读取）实现。

**核心决策：Voucher + Order 合并为一个服务**

`VoucherServiceImpl → IVoucherOrderService.autoIssueVoucherToEarliestSubscriber()` 和 `VoucherOrderServiceImpl → ISeckillVoucherService + IVoucherService` 构成双向循环依赖。拆分需要分布式事务（阶段七），在阶段四无 RPC 的条件下强行拆分会破坏秒杀链路一致性。合并后 6 个实体和 8+ 个 Service 在同一进程，事务边界不变。

### 4.1 新建 `hmdp-service-api` ✅

零框架依赖纯 POJO 模块：`Result.java`, `UserDTO.java`, `LoginFormDTO.java`, `ScrollResult.java`, `UserHolder.java`, `BaseCode.java`。从 `hmdp-parameter` 迁入 `GetSubscribeStatusVo.java`。

### 4.2 提取共享组件到 `hmdp-common` ✅

迁入拦截器 (`LoginInterceptor`, `RefreshTokenInterceptor`)、常量 (`RedisConstants`, `SystemConstants`)、缓存客户端 (`CacheClient`, `RedisIdWorker`, `SimpleRedisLock`) 以及 `MvcConfig`, `WebExceptionAdvice`, `HmdpCommonAutoConfig`。新增依赖 `hmdp-service-api`、`spring-boot-starter-log4j2`、`spring-boot-starter-data-redis` (optional)、`mybatis-plus` (optional)。

### 4.3 服务拆分结果 ✅

| 服务 | 端口 | 文件数 | 特征 |
|------|------|--------|------|
| `hmdp-shop-service` | 8081 | 11 | 零跨域依赖，布隆过滤器，GEO 搜索 |
| `hmdp-user-service` | 8082 | ~12 | 用户分片表 (3)，`@ServiceLock` |
| `hmdp-voucher-service` | 8083 | 40+ | RabbitMQ + 令牌桶 + Lua + 延迟队列，含 `@EnableRabbit` |
| `hmdp-blog-service` | 8084 | 17 | UserBridge + FollowBridge |
| `hmdp-follow-service` | 8085 | 9 | UserBridge |

### 4.4 Bridge 模式（阶段五替换为 OpenFeign）

```java
@Deprecated  // Phase 5 替换为 OpenFeign
@Component
public class UserBridge extends ServiceImpl<UserMapper, User> {
    public User getById(Long userId) { return super.getById(userId); }
}
```

| 服务 | Bridge | 访问的表 |
|------|--------|----------|
| voucher | UserBridge | tb_user_info |
| follow | UserBridge | tb_user |
| blog | UserBridge, FollowBridge | tb_user, tb_follow |

每个服务的 `shardingsphere.yaml` 包含 Bridge 访问的外部表分片规则。

### 4.5 SB4 兼容性修复 ✅

- 父 POM `dependencyManagement` 对 `spring-boot-starter` 全局排除 `spring-boot-starter-logging`
- 所有服务 `application.yml` 排除 `RedissonAutoConfigurationV2`（自有 `RedissonCommonAutoConfiguration` 使用新 `DataRedisProperties`）

### 4.6 验证 ✅

```bash
mvn clean compile  # BUILD SUCCESS (27/27 modules)
```

---

## 阶段五：Nacos + Gateway + OpenFeign ✅

### 实际实现 (2025.1.1 + 2025.1.0.0)

使用 Spring Cloud 2025.1.1 + Spring Cloud Alibaba 2025.1.0.0，兼容 Spring Boot 4.0.6。

### 5a. Nacos Discovery ✅

每个微服务添加 `spring-cloud-starter-alibaba-nacos-discovery` + `@EnableDiscoveryClient`，注册到 `192.168.137.128:8848`。

### 5b. Spring Cloud Gateway ✅

新建 `hmdp-gateway` 模块 (端口 8080)，基于 WebFlux/Netty。路由用 Java `RouteLocator` Bean 配置（YAML 路由在 Spring Cloud 2025.1.1 中无法解析）：
- `/api/shop/**` → `lb://hmdp-shop-service`
- `/api/user/**` → `lb://hmdp-user-service`
- `/api/voucher/**` → `lb://hmdp-voucher-service`
- `/api/blog/**` → `lb://hmdp-blog-service`
- `/api/follow/**` → `lb://hmdp-follow-service`

`StripPrefix=1` 去掉 `/api` 前缀。`CorsWebFilter` 处理 CORS。认证保持在各服务自己的拦截器，Gateway 只做路由转发。

### 5c. OpenFeign ✅

新建 `hmdp-service-api-feign` 模块：
- `UserFeignClient` — `GET /user/{id}`, `POST /user/batch`
- `FollowFeignClient` — `GET /follow/followers/{followUserId}`
- `UserInfoFeignClient` — `GET /user/info/{userId}`, `POST /user/info/by-levels`

新增后端端点：`POST /user/batch`、`POST /user/info/by-levels`、`GET /follow/followers/{followUserId}`。

`FeignAuthConfig` RequestInterceptor 传播 Authorization header，`MvcConfig` 排除 Feign 内部端点。调用方启动类加 `@EnableFeignClients(basePackages = "org.javaup.feign", defaultConfiguration = FeignAuthConfig.class)`。

4 个 Bridge 类 + 关联 Mapper/Entity 已删除。

### 5d. ShardingSphere 清理 ✅

移除各服务 `shardingsphere.yaml` 中仅为 Bridge 服务的外部表广播规则，每个服务只保留自己实际访问的表。

### 5e. 前端适配 ✅

- `vite.config.js` 代理目标改为 `localhost:8080`，去掉 `rewrite`（Gateway StripPrefix 负责）
- `InfoHtml.vue` 关注博客 Tab：`indexQueryHotBlogsScroll` → `queryBlogOfFollow`（正确的 `/blog/of/follow` 端点）
- 删除 `pnpm-lock.yaml`（与 npm 冲突）

### 验证

- 6 个服务 (5 微服务 + Gateway) 注册到 Nacos
- `curl http://localhost:8080/api/shop-type/list` 通过 Gateway 返回正确数据
- Blog→User Feign 调用返回用户名
- 前端 `npm run dev` 通过 Gateway 访问所有页面

---

## 阶段六：Sentinel 流量保护 ✅

### 6.1 引入 Sentinel

每个微服务添加：
- `spring-cloud-starter-alibaba-sentinel`
- 配置 Sentinel Dashboard 地址 (`192.168.137.128:8081`)

### 6.2 保护规则

| 场景 | 规则类型 | 说明 |
|---|---|---|
| 秒杀接口 | 令牌桶 + QPS 限流 | 现有 `hmdp-redis-rate-limit-framework` 作为前置，Sentinel 作为兜底 |
| 商铺查询 | 热点参数限流 | 热门商铺高频查询保护 |
| 用户登录 | 熔断降级 | 用户服务不可用时返回友好提示 |
| Gateway | 系统自适应限流 | 入口流量整体管控 |

**实施决策:**
- Gateway 依赖 `spring-cloud-alibaba-sentinel-gateway`（非 `spring-cloud-starter-alibaba-sentinel`），避免 WebFlux 冲突
- SCA 已自动装配 `SentinelGatewayFilter` + `SentinelGatewayBlockExceptionHandler`，无需手动建 Bean
- Gateway 规则用 `GatewayFlowRule`（包 `com.alibaba.csp.sentinel.adapter.gateway.common.rule`，Sentinel 1.8.9 中从 `sc.rule` 迁移）
- `feign.sentinel.enabled=false`（Nacos 统一配置），避免 FeignClient 启动报错
- 传输端口显式分配 Gateway=8724, Shop=8719, User=8720, Voucher=8721, Blog=8722, Follow=8723
- Sentinel Dashboard Docker 端口 `8081:8858`（bladex 镜像默认 8858）
- `sentinel-datasource-extension` 需显式依赖（SCA Gateway 模块传递依赖缺失）
- `allow-circular-references: true`：Sentinel AOP 代理暴露了 VoucherServiceImpl ↔ SeckillVoucherServiceImpl 既有循环依赖
- 规则通过代码 `@PostConstruct` 初始化（Gateway: `GatewaySentinelConfig`, 服务: 各 `SentinelRulesConfig`）

### 6.3 验证

- Sentinel Dashboard (http://192.168.137.128:8081) 能看到所有服务和规则
- 压测秒杀接口，限流规则生效
- 模拟用户服务宕机，熔断降级触发

---

## 阶段七：Seata 分布式事务

### 7.1 引入 Seata

- `spring-cloud-starter-alibaba-seata`
- 配置 `seataServer.properties` 推送到 Nacos `SEATA_GROUP`

### 7.2 适用范围

**使用 AT 模式的场景:**
- 退款流程（订单服务 ⇄ 优惠券服务）
- 优惠券发放（用户服务 → 优惠券服务）
- 非秒杀的正常下单

**不使用 Seata 的场景:**
- 秒杀链路 — 保持 MQ + Outbox + 对账机制（高吞吐要求，AT 模式回滚日志会严重影响性能）

### 7.3 实现要点

- 在需要分布式事务的方法上添加 `@GlobalTransactional`
- Seata TC Server 已在 Docker 中运行，Nacos 作为注册中心
- 每个微服务作为 Seata Client (TM + RM)
- undo_log 表需在 hmdp_0/hmdp_1 中创建

### 7.4 验证

- Seata Console (http://192.168.137.128:7091) 可查看全局事务状态
- 模拟退款流程异常，库存回滚成功
- 秒杀链路不受影响

---

## 阶段八：Elasticsearch 搜索引擎

### 8.1 替换范围

| 现有实现 | 替换为 | 说明 |
|---|---|---|
| Redis GEO (`GEOSEARCH`) | Elasticsearch `geo_distance` | 商铺地理位置搜索 |
| MySQL `LIKE` 模糊查询 | Elasticsearch 全文检索 | 商铺名称/描述搜索 |
| 按距离排序 | ES `_geo_distance` sort | 附近商铺排序 |

### 8.2 实现方案

- `spring-boot-starter-data-elasticsearch`
- 商铺数据双写：MySQL 写入 → MQ → ES 同步
- 首次全量同步：从 DB 批量导入 ES
- ShopController 搜索接口改为查询 ES

### 8.3 验证

```bash
# ES 健康检查
curl http://192.168.137.128:9200/_cat/health

# 搜索接口
curl "http://localhost:8085/api/shop/search?keyword=火锅&lat=39.9&lon=116.4"
```

---

## 阶段九：Spring AI Agent 服务

### 9.1 新建 hmdp-ai-agent 模块

- 依赖 `spring-ai-starter-openai`
- 对接 DeepSeek / OpenAI 等大模型
- 用户画像数据聚合到 Redis Hash（异步 MQ 事件 → 画像计算 → Redis）

### 9.2 推荐场景

| 场景 | 数据来源 | 说明 |
|---|---|---|
| 个性化优惠券推荐 | 用户画像 + 历史订单 | LLM 分析用户偏好 |
| 商铺筛选推荐 | 用户画像 + ES 搜索结果 | AI 理解用户意图，组合 ES 查询 |
| 智能搜索 | 自然语言查询 | "高性价比火锅店" → AI 解析 → ES 查询 |

### 9.3 用户画像数据流

```
业务事件 (MQ) → 画像计算服务 → Redis Hash → AI Agent 读取
                         ↑
              用户行为 + 订单历史 + 搜索记录
```

### 9.4 验证

- Agent 服务注册到 Nacos
- 通过 Gateway 调用 Agent API
- 用户输入自然语言需求，返回个性化推荐结果

---

## 阶段十：收尾与优化

- 前端 `hmdp-vue3` 适配 Gateway 端口
- 全链路压测 (JMeter / wrk)
- 完善 Prometheus 指标 + Grafana 面板
- `CLAUDE.md` / `docs/` 文档终版更新

---

## 快速参考

| 虚拟机       | 192.168.137.128                                           |
|-----------|-----------------------------------------------------------|
| Docker 目录 | `docker/`                                                 |
| Maven     | `~/apache-maven/apache-maven-3.9.9/bin/mvn`               |
| JDK       | 21 (D:\java\jdk21)                                        |
| 编译命令      | `mvn clean compile -pl hmdp-core-service -am -DskipTests` |
