# Spring AI Alibaba Workflow Agent RAG 知识库落地手册

## 0. 这份文档解决什么问题

你现在后续主要访问的统一入口是：

```text
POST /agent/workflow/execute
```

当前 Workflow 主链路已经是：

```text
用户问题
    ↓
preprocessInput
    ↓
classifyIntent
    ↓
extractEntities
    ↓
retrieveKnowledge
    ↓
routeByIntent
    ↓
业务数据加载
    ↓
Java 规则分析
    ↓
LLM 生成业务回答
    ↓
护栏校验
    ↓
最终返回
```

现在缺口在：

```text
retrieveKnowledge
```

你当前的 `KnowledgeRetrieveNode` 只是写死了几句规则文本：

```java
case "ORDER_DIAGNOSIS" -> "采购订单状态规则：...";
case "WARNING_SCAN" -> "采购执行预警规则：...";
case "SUPPLIER_SCORE" -> "供应商评分规则：...";
```

这份文档的目标是把它改成真正的 RAG 知识库：

```text
1. 使用 Redis Stack 作为向量数据库
2. 使用 Spring AI VectorStore 统一管理向量写入和检索
3. 使用 DashScope Embedding 生成向量
4. 导入业务规则文档到 Redis VectorStore
5. Workflow 执行时根据用户问题召回知识片段
6. 把召回内容放入 ragDocs
7. 让 BusinessAnswerGenerateNode 基于业务事实 + Java 规则 + RAG 知识回答
```

这版不使用 MySQL 存向量。

## 0.1 先说结论：这份文档要满足这些前提才可运行

不能脱离前提说“照着抄一定能跑”。

这份文档的代码路径已经按你当前项目结构设计，但完整运行必须满足：

```text
1. 你已经完成 Workflow 主链路，/agent/workflow/execute 当前可调用
2. 你已经完成 Agent 会话持久化表，至少 agent_session、agent_message、agent_session_state、agent_result 已存在
3. spring.data.redis 指向的是 Redis Stack，不是普通 Redis
4. Redis Stack 的 MODULE LIST 能看到 search 模块
5. DashScope API Key 可用，并且能创建 EmbeddingModel
6. Maven 能拉到 org.springframework.ai:spring-ai-starter-vector-store-redis:1.1.2
7. 你导入过知识后，再测试 /agent/rag/search 和 /agent/workflow/execute
```

如果这些前提有一个不满足，就不能保证完整运行。

这份 RAG 文档只解决：

```text
Redis Stack + VectorStore + Workflow retrieveKnowledge 接入
```

它不重复解决：

```text
会话持久化表建表
业务订单/预警/供应商 Mapper 数据正确性
Redis Stack 部署
DashScope Key 申请
```

## 1. 为什么不用 MySQL 存向量

你的项目本来就已经用了 Redis：

```yaml
spring:
  data:
    redis:
      host: ...
      port: 6379
      password: ...
```

如果再用 MySQL 存 `embedding_json`，会带来额外工作量：

```text
1. 需要新增 RAG 表
2. 需要新增 Entity / Mapper / XML
3. 需要自己写余弦相似度计算
4. 知识量稍微变大后性能不好
5. 后续迁移向量库还要重做
```

所以本项目更合理的方案是：

```text
Redis Stack
    ↓
RediSearch 向量索引
    ↓
Spring AI RedisVectorStore
    ↓
VectorStore.add / similaritySearch / delete
```

这样更贴合你当前项目。

## 2. 本版本最终架构

RAG 导入链路：

```text
POST /agent/rag/import
    ↓
AgentRagController
    ↓
AgentRagServiceImpl
    ↓
切分文档 chunk
    ↓
构建 Spring AI Document
    ↓
VectorStore.add
    ↓
DashScope Embedding 自动生成向量
    ↓
Redis Stack 保存向量和元数据
```

RAG 检索链路：

```text
POST /agent/rag/search
    ↓
AgentRagController
    ↓
AgentRagServiceImpl
    ↓
VectorStore.similaritySearch
    ↓
Redis Stack 向量召回
    ↓
返回 topK 知识片段
```

Workflow 接入链路：

```text
POST /agent/workflow/execute
    ↓
IntentClassifyNode 识别意图
    ↓
EntityExtractNode 抽取实体
    ↓
KnowledgeRetrieveNode 调 AgentRagService.search
    ↓
Redis VectorStore 召回规则片段
    ↓
RAG 片段写入 WorkflowStateKeys.RAG_DOCS
    ↓
BusinessAnswerGenerateNode 把 ragDocs 放进 Prompt
    ↓
LLM 结合规则知识生成回答
```

## 3. 业务边界

RAG 只存业务规则，不存实时业务数据。

应该导入 RAG 的内容：

```text
1. 采购订单状态流转规则
2. 到货、入库流程规则
3. 采购执行预警规则
4. 供应商评分解释规则
5. 系统操作边界
6. 业务术语说明
```

不应该导入 RAG 的内容：

```text
1. 具体采购订单状态
2. 具体采购订单数量
3. 具体供应商履约指标
4. 具体到货单、入库单数据
5. 任何需要实时查询的业务事实
```

事实优先级必须是：

```text
1. Mapper 查询出来的业务数据
2. Java 规则分析结果
3. RAG 召回的业务规则
4. LLM 语言组织
```

也就是说：

```text
订单是否存在，看 AgentQueryMapper。
供应商是否存在，看 SupplierPerformanceMapper。
风险列表有哪些，看 AgentWarningMapper。
RAG 只解释为什么这么判断。
```

## 4. 文件清单

本次不新增 SQL。

新增文件：

```text
inventory_back/src/main/java/com/xixi/agent/config/RedisVectorStoreConfig.java

inventory_back/src/main/java/com/xixi/agent/dto/RagKnowledgeImportRequest.java
inventory_back/src/main/java/com/xixi/agent/dto/RagSearchRequest.java

inventory_back/src/main/java/com/xixi/agent/vo/RagSearchResultVO.java
inventory_back/src/main/java/com/xixi/agent/vo/RagImportResultVO.java

inventory_back/src/main/java/com/xixi/agent/service/AgentRagService.java
inventory_back/src/main/java/com/xixi/agent/service/impl/AgentRagServiceImpl.java

inventory_back/src/main/java/com/xixi/agent/controller/AgentRagController.java
```

修改文件：

```text
inventory_back/pom.xml
inventory_back/src/main/resources/application-dev.yml
inventory_back/src/main/java/com/xixi/agent/workflow/node/KnowledgeRetrieveNode.java
inventory_back/src/main/java/com/xixi/agent/workflow/ProcurementWorkflowConfig.java
```

如果你之前还没有修：

```text
WorkflowStateKeys.WARNING_CONTEXT
```

也顺手在 `ProcurementWorkflowConfig` 里补 KeyStrategy。

## 5. 第一步：准备 Redis Stack

Spring AI Redis VectorStore 需要 Redis Stack。

普通 Redis 不够。

原因：

```text
向量检索依赖 RediSearch 模块。
Redis Stack 自带 RediSearch。
普通 Redis 没有 FT.CREATE / FT.SEARCH。
```

### 5.1 本地 Docker 启动 Redis Stack

如果你本地开发想单独起一个 Redis Stack：

```powershell
docker run -d --name inventory-redis-stack `
  -p 6379:6379 `
  -p 8001:8001 `
  -e REDIS_ARGS="--requirepass 123456" `
  redis/redis-stack:latest
```

说明：

```text
6379 是 Redis 端口。
8001 是 RedisInsight 管理页面端口。
123456 是开发环境密码，请按你的实际环境改。
```

如果你服务器上已经有 Redis，需要确认它是 Redis Stack，而不是普通 Redis。

### 5.2 验证 Redis Stack 模块

执行：

```powershell
redis-cli -h 127.0.0.1 -p 6379 -a 123456 MODULE LIST
```

输出里必须能看到类似：

```text
search
```

再执行：

```powershell
redis-cli -h 127.0.0.1 -p 6379 -a 123456 FT._LIST
```

如果命令不存在，说明不是 Redis Stack。

## 6. 第二步：修改 pom.xml

修改文件：

```text
inventory_back/pom.xml
```

你现在已经有：

```xml
<properties>
    <java.version>17</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <spring-ai-alibaba.version>1.1.2.1</spring-ai-alibaba.version>
    <lombok.version>1.18.38</lombok.version>
</properties>
```

补一个 Spring AI 版本属性：

```xml
<spring-ai.version>1.1.2</spring-ai.version>
```

变成：

```xml
<properties>
    <java.version>17</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <spring-ai.version>1.1.2</spring-ai.version>
    <spring-ai-alibaba.version>1.1.2.1</spring-ai-alibaba.version>
    <lombok.version>1.18.38</lombok.version>
</properties>
```

在依赖里新增：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-redis</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
```

建议放在现有 DashScope 依赖后面：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-redis</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
```

说明：

```text
spring-ai-starter-vector-store-redis 会引入：
- spring-ai-redis-store
- spring-ai-vector-store
- jedis
```

你项目原来的 `spring-boot-starter-data-redis` 可以保留。

如果 Maven 拉依赖时报下面这种错：

```text
Blocked mirror for repositories: [alimaven (http://maven.aliyun.com/...)]
```

说明你的 Maven 镜像用了 HTTP 地址，被 Maven 默认安全策略拦截。

处理方式一：把 Maven `settings.xml` 里的阿里云镜像改成 HTTPS：

```xml
<mirror>
    <id>alimaven</id>
    <mirrorOf>central</mirrorOf>
    <name>aliyun maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

处理方式二：临时从 Maven Central 拉一次依赖：

```powershell
mvn dependency:get "-Dartifact=org.springframework.ai:spring-ai-starter-vector-store-redis:1.1.2" "-DremoteRepositories=central::default::https://repo1.maven.org/maven2"
```

## 7. 第三步：修改 application-dev.yml

修改文件：

```text
inventory_back/src/main/resources/application-dev.yml
```

你现在已有 Redis 配置：

```yaml
spring:
  data:
    redis:
      host: 106.53.8.74
      port: 6379
      password: 123456
      database: 0
```

这部分保留。

再补 DashScope Embedding 和 VectorStore 配置：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
      embedding:
        enabled: true
        options:
          model: text-embedding-v3
    vectorstore:
      redis:
        initialize-schema: true
        index-name: inventory-agent-rag-index
        prefix: inventory:agent:rag:
```

完整结构大概是：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://...
    username: root
    password: ...
  data:
    redis:
      host: 你的RedisStack地址
      port: 6379
      password: 你的RedisStack密码
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          max-wait: -1ms
          min-idle: 0
        shutdown-timeout: 100ms
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
      embedding:
        enabled: true
        options:
          model: text-embedding-v3
    vectorstore:
      redis:
        initialize-schema: true
        index-name: inventory-agent-rag-index
        prefix: inventory:agent:rag:
```

注意：

```text
这里不用把 spring.data.redis.client-type 改成 jedis。
项目原有 RedisTemplate 可以继续走 Lettuce。
RAG 单独用 JedisPooled 连接 Redis Stack。
```

## 8. 第四步：新增 RedisVectorStoreConfig

为什么需要这个配置类：

```text
Spring AI 的 RedisVectorStore 支持 metadataFields。
我们需要把 docCode、bizIntent、docType 这些元数据建成可过滤字段。
这样检索 ORDER_DIAGNOSIS 时，可以只查 COMMON + ORDER_DIAGNOSIS 的知识。
```

新建文件：

```text
inventory_back/src/main/java/com/xixi/agent/config/RedisVectorStoreConfig.java
```

代码：

```java
package com.xixi.agent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorStoreConfig {

    @Bean(destroyMethod = "close")
    public JedisPooled ragJedisPooled(RedisProperties redisProperties) {
        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                .database(redisProperties.getDatabase());

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            clientConfigBuilder.password(redisProperties.getPassword());
        }
        if (redisProperties.getTimeout() != null) {
            clientConfigBuilder.timeoutMillis((int) redisProperties.getTimeout().toMillis());
        }

        return new JedisPooled(
                new HostAndPort(redisProperties.getHost(), redisProperties.getPort()),
                clientConfigBuilder.build()
        );
    }

    @Bean
    public VectorStore vectorStore(JedisPooled ragJedisPooled,
                                   EmbeddingModel embeddingModel,
                                   @Value("${spring.ai.vectorstore.redis.index-name:inventory-agent-rag-index}") String indexName,
                                   @Value("${spring.ai.vectorstore.redis.prefix:inventory:agent:rag:}") String prefix,
                                   @Value("${spring.ai.vectorstore.redis.initialize-schema:true}") boolean initializeSchema) {
        return RedisVectorStore.builder(ragJedisPooled, embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(initializeSchema)
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("docCode"),
                        RedisVectorStore.MetadataField.text("title"),
                        RedisVectorStore.MetadataField.tag("bizIntent"),
                        RedisVectorStore.MetadataField.tag("docType")
                )
                .build();
    }
}
```

这段代码在做什么：

```text
1. 复用 spring.data.redis 里的 host、port、password、database
2. 单独创建 JedisPooled 给 RedisVectorStore 使用
3. 创建 VectorStore Bean
4. 启动时自动创建 Redis 向量索引
5. 注册 docCode、title、bizIntent、docType 为可检索元数据字段
```

注意：

```text
如果你的 Redis Stack 和业务 Redis 不是同一个实例，就不要复用 spring.data.redis。
这种情况下可以单独写 agent.rag.redis.host / port / password 配置。
第一版为了少改代码，先复用现有 Redis 配置。
```

## 9. 第五步：新增 DTO / VO

### 9.1 RagKnowledgeImportRequest

新建文件：

```text
inventory_back/src/main/java/com/xixi/agent/dto/RagKnowledgeImportRequest.java
```

代码：

```java
package com.xixi.agent.dto;

import lombok.Data;

@Data
public class RagKnowledgeImportRequest {
    private String docCode;

    private String title;

    private String docType;

    private String bizIntent;

    private String sourcePath;

    private String content;
}
```

字段说明：

```text
docCode：文档编码，例如 ORDER_STATUS_RULE_V1
title：文档标题
docType：文档类型，默认 BUSINESS_RULE
bizIntent：适用意图，COMMON / ORDER_DIAGNOSIS / WARNING_SCAN / SUPPLIER_SCORE / KNOWLEDGE_QA
sourcePath：来源路径，例如 document/状态机设计.md
content：要导入的正文
```

### 9.2 RagSearchRequest

新建文件：

```text
inventory_back/src/main/java/com/xixi/agent/dto/RagSearchRequest.java
```

代码：

```java
package com.xixi.agent.dto;

import lombok.Data;

@Data
public class RagSearchRequest {
    private String query;

    private String bizIntent;

    private Integer topK;
}
```

### 9.3 RagSearchResultVO

新建文件：

```text
inventory_back/src/main/java/com/xixi/agent/vo/RagSearchResultVO.java
```

代码：

```java
package com.xixi.agent.vo;

import lombok.Data;

@Data
public class RagSearchResultVO {
    private String id;

    private String docCode;

    private String title;

    private String docType;

    private String bizIntent;

    private String sourcePath;

    private Integer chunkNo;

    private String content;

    private Double score;
}
```

### 9.4 RagImportResultVO

新建文件：

```text
inventory_back/src/main/java/com/xixi/agent/vo/RagImportResultVO.java
```

代码：

```java
package com.xixi.agent.vo;

import lombok.Data;

@Data
public class RagImportResultVO {
    private String docCode;

    private String title;

    private String bizIntent;

    private Integer chunkCount;

    private String message;
}
```

## 10. 第六步：新增 AgentRagService

新建文件：

```text
inventory_back/src/main/java/com/xixi/agent/service/AgentRagService.java
```

代码：

```java
package com.xixi.agent.service;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.vo.RagImportResultVO;
import com.xixi.agent.vo.RagSearchResultVO;

import java.util.List;

public interface AgentRagService {
    RagImportResultVO importKnowledge(RagKnowledgeImportRequest request);

    List<RagSearchResultVO> search(RagSearchRequest request);

    List<RagSearchResultVO> search(String query, String bizIntent, Integer topK);
}
```

## 11. 第七步：新增 AgentRagServiceImpl

新建文件：

```text
inventory_back/src/main/java/com/xixi/agent/service/impl/AgentRagServiceImpl.java
```

代码：

```java
package com.xixi.agent.service.impl;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.service.AgentRagService;
import com.xixi.agent.vo.RagImportResultVO;
import com.xixi.agent.vo.RagSearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AgentRagServiceImpl implements AgentRagService {
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 80;
    private static final int DEFAULT_TOP_K = 4;
    private static final int MAX_TOP_K = 10;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.45D;
    private static final Pattern DOC_CODE_PATTERN = Pattern.compile("^[A-Z0-9_\\-]{3,64}$");

    private final VectorStore vectorStore;

    @Override
    public RagImportResultVO importKnowledge(RagKnowledgeImportRequest request) {
        validateImportRequest(request);

        String docCode = request.getDocCode().trim();
        String title = request.getTitle().trim();
        String docType = defaultIfBlank(request.getDocType(), "BUSINESS_RULE");
        String bizIntent = defaultIfBlank(request.getBizIntent(), "COMMON");
        String sourcePath = defaultIfBlank(request.getSourcePath(), "manual");

        // 同一个 docCode 重复导入时，先删除旧分片，避免召回到过期规则。
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.eq("docCode", docCode).build());

        List<String> chunks = splitContent(request.getContent());
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            int chunkNo = i + 1;
            String content = chunks.get(i);
            String id = buildChunkId(docCode, chunkNo, content);

            Document document = Document.builder()
                    .id(id)
                    .text(content)
                    .metadata(Map.of(
                            "docCode", docCode,
                            "title", title,
                            "docType", docType,
                            "bizIntent", bizIntent,
                            "sourcePath", sourcePath,
                            "chunkNo", chunkNo
                    ))
                    .build();
            documents.add(document);
        }

        vectorStore.add(documents);

        RagImportResultVO result = new RagImportResultVO();
        result.setDocCode(docCode);
        result.setTitle(title);
        result.setBizIntent(bizIntent);
        result.setChunkCount(documents.size());
        result.setMessage("知识导入成功");
        return result;
    }

    @Override
    public List<RagSearchResultVO> search(RagSearchRequest request) {
        if (request == null) {
            return List.of();
        }
        return search(request.getQuery(), request.getBizIntent(), request.getTopK());
    }

    @Override
    public List<RagSearchResultVO> search(String query, String bizIntent, Integer topK) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query.trim())
                .topK(normalizeTopK(topK))
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD);

        Filter.Expression filterExpression = buildIntentFilter(bizIntent);
        if (filterExpression != null) {
            builder.filterExpression(filterExpression);
        }

        List<Document> documents = vectorStore.similaritySearch(builder.build());
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(this::toSearchResult)
                .toList();
    }

    private void validateImportRequest(RagKnowledgeImportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("导入请求不能为空");
        }
        if (!StringUtils.hasText(request.getDocCode())) {
            throw new IllegalArgumentException("docCode不能为空");
        }
        if (!DOC_CODE_PATTERN.matcher(request.getDocCode().trim()).matches()) {
            throw new IllegalArgumentException("docCode只能包含大写字母、数字、下划线和中划线，长度3到64位");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("title不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content不能为空");
        }
    }

    private Filter.Expression buildIntentFilter(String bizIntent) {
        if (!StringUtils.hasText(bizIntent)) {
            return null;
        }

        String intent = bizIntent.trim();
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        return builder.or(
                builder.eq("bizIntent", "COMMON"),
                builder.eq("bizIntent", intent)
        ).build();
    }

    private List<String> splitContent(String content) {
        String text = content.replace("\r\n", "\n").trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            int splitEnd = findSplitPosition(text, start, end);
            String chunk = text.substring(start, splitEnd).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (splitEnd >= text.length()) {
                break;
            }
            int nextStart = splitEnd - CHUNK_OVERLAP;
            start = nextStart <= start ? splitEnd : nextStart;
        }

        return chunks;
    }

    private int findSplitPosition(String text, int start, int end) {
        if (end >= text.length()) {
            return text.length();
        }

        int minSplit = start + CHUNK_SIZE / 2;
        int newline = text.lastIndexOf('\n', end);
        if (newline > minSplit) {
            return newline;
        }

        for (int i = end - 1; i > minSplit; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '；' || c == ';' || c == '.') {
                return i + 1;
            }
        }

        return end;
    }

    private RagSearchResultVO toSearchResult(Document document) {
        RagSearchResultVO vo = new RagSearchResultVO();
        vo.setId(document.getId());
        vo.setContent(document.getText());
        vo.setScore(document.getScore());
        vo.setDocCode(getMetadata(document, "docCode"));
        vo.setTitle(getMetadata(document, "title"));
        vo.setDocType(getMetadata(document, "docType"));
        vo.setBizIntent(getMetadata(document, "bizIntent"));
        vo.setSourcePath(getMetadata(document, "sourcePath"));
        vo.setChunkNo(getIntegerMetadata(document, "chunkNo"));
        return vo;
    }

    private String getMetadata(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value == null ? null : value.toString();
    }

    private Integer getIntegerMetadata(Document document, String key) {
        Object value = document.getMetadata().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private String buildChunkId(String docCode, int chunkNo, String content) {
        return docCode + ":" + chunkNo + ":" + sha256(content).substring(0, 16);
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前JDK不支持SHA-256", e);
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
```

这段代码在做什么：

```text
1. importKnowledge 负责导入知识
2. 同一个 docCode 重复导入时，先删除旧分片
3. splitContent 把长文档切成 500 字左右的 chunk
4. 每个 chunk 转成 Spring AI Document
5. metadata 里写入 docCode、title、docType、bizIntent、sourcePath、chunkNo
6. vectorStore.add 会自动调用 EmbeddingModel 生成向量并写入 Redis Stack
7. search 会按 query 做向量检索
8. 如果传了 bizIntent，只检索 COMMON + 当前意图的知识
```

注意：

```text
不要把用户输入直接拼成 filterExpression 字符串。
这里使用 FilterExpressionBuilder，是为了避免过滤表达式注入问题。
```

## 12. 第八步：新增 AgentRagController

新建文件：

```text
inventory_back/src/main/java/com/xixi/agent/controller/AgentRagController.java
```

代码：

```java
package com.xixi.agent.controller;

import com.xixi.agent.dto.RagKnowledgeImportRequest;
import com.xixi.agent.dto.RagSearchRequest;
import com.xixi.agent.service.AgentRagService;
import com.xixi.pojo.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/rag")
@RequiredArgsConstructor
public class AgentRagController {
    private final AgentRagService agentRagService;

    @PostMapping("/import")
    public Result importKnowledge(@RequestBody RagKnowledgeImportRequest request) {
        return Result.success(agentRagService.importKnowledge(request));
    }

    @PostMapping("/search")
    public Result search(@RequestBody RagSearchRequest request) {
        return Result.success(agentRagService.search(request));
    }
}
```

说明：

```text
这个 Controller 只用于开发落地和测试。
正式环境建议给 /agent/rag/import 加管理员权限。
```

## 13. 第九步：改造 KnowledgeRetrieveNode

修改文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/node/KnowledgeRetrieveNode.java
```

把原来的写死版本替换成下面完整代码：

```java
package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.service.AgentRagService;
import com.xixi.agent.vo.RagSearchResultVO;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class KnowledgeRetrieveNode implements NodeAction {
    private final AgentRagService agentRagService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String intent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();
        String message = state.value(WorkflowStateKeys.NORMALIZED_MESSAGE, "").toString();

        String docs;
        try {
            List<RagSearchResultVO> hits = agentRagService.search(message, intent, 4);
            docs = buildRagDocs(hits, intent);
        } catch (Exception e) {
            // RAG 不应该阻断主业务链路。Redis 或向量检索异常时，使用兜底规则继续执行 Workflow。
            docs = fallbackDocs(intent);
        }

        return Map.of(WorkflowStateKeys.RAG_DOCS, docs);
    }

    private String buildRagDocs(List<RagSearchResultVO> hits, String intent) {
        if (hits == null || hits.isEmpty()) {
            return fallbackDocs(intent);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("以下内容来自 Redis Stack 知识库检索结果，只能作为业务规则参考，不能替代数据库实时业务数据：\n\n");

        int index = 1;
        for (RagSearchResultVO hit : hits) {
            builder.append("【资料").append(index).append("】")
                    .append(hit.getTitle())
                    .append("，相似度：")
                    .append(hit.getScore() == null ? "N/A" : String.format("%.4f", hit.getScore()))
                    .append("\n");
            builder.append(hit.getContent()).append("\n\n");
            index++;
        }

        return builder.toString();
    }

    private String fallbackDocs(String intent) {
        return switch (intent) {
            case "ORDER_DIAGNOSIS" -> "采购订单状态规则：WAIT_CONFIRM 待确认，IN_PROGRESS 执行中，PARTIAL_ARRIVAL 部分到货，COMPLETED 已完成。";
            case "WARNING_SCAN" -> "采购执行预警规则：待确认超时、到货停滞、待入库超时均应进入预警列表。";
            case "SUPPLIER_SCORE" -> "供应商评分规则：确认及时率、到货完成率、入库完成率、异常到货率共同影响评分。";
            default -> "";
        };
    }
}
```

完成后：

```text
retrieveKnowledge 会从 Redis VectorStore 召回知识。
如果 Redis 没有命中，才用原来的 fallbackDocs。
```

## 14. 第十步：修改 ProcurementWorkflowConfig

修改文件：

```text
inventory_back/src/main/java/com/xixi/agent/workflow/ProcurementWorkflowConfig.java
```

### 14.1 增加 import

补充：

```java
import com.xixi.agent.service.AgentRagService;
```

### 14.2 方法参数增加 AgentRagService

找到：

```java
public StateGraph procurementWorkflowGraph(ChatClient.Builder chatClientBuilder,
                                           AgentQueryMapper agentQueryMapper,
                                           ProcessDiagnosisAgentService processDiagnosisAgentService,
                                           AgentWarningMapper agentWarningMapper,
                                           SupplierPerformanceMapper supplierPerformanceMapper,
                                           AgentSessionService agentSessionService,
                                           ObjectMapper objectMapper) throws Exception {
```

改成：

```java
public StateGraph procurementWorkflowGraph(ChatClient.Builder chatClientBuilder,
                                           AgentQueryMapper agentQueryMapper,
                                           ProcessDiagnosisAgentService processDiagnosisAgentService,
                                           AgentWarningMapper agentWarningMapper,
                                           SupplierPerformanceMapper supplierPerformanceMapper,
                                           AgentSessionService agentSessionService,
                                           AgentRagService agentRagService,
                                           ObjectMapper objectMapper) throws Exception {
```

### 14.3 补 WARNING_CONTEXT 的 KeyStrategy

你现在 `WorkflowStateKeys` 里已经有：

```java
public static final String WARNING_CONTEXT = "warningContext";
```

但 `ProcurementWorkflowConfig` 里没有注册。

找到：

```java
strategies.put(WorkflowStateKeys.WARNING_ITEMS, new ReplaceStrategy());
strategies.put(WorkflowStateKeys.WARNING_ANALYSIS, new ReplaceStrategy());
strategies.put(WorkflowStateKeys.SUPPLIER_METRICS, new ReplaceStrategy());
```

改成：

```java
strategies.put(WorkflowStateKeys.WARNING_ITEMS, new ReplaceStrategy());
strategies.put(WorkflowStateKeys.WARNING_ANALYSIS, new ReplaceStrategy());
strategies.put(WorkflowStateKeys.WARNING_CONTEXT, new ReplaceStrategy());
strategies.put(WorkflowStateKeys.SUPPLIER_METRICS, new ReplaceStrategy());
```

### 14.4 修改 retrieveKnowledge 节点

找到：

```java
graph.addNode("retrieveKnowledge", node_async(new KnowledgeRetrieveNode()));
```

改成：

```java
graph.addNode("retrieveKnowledge", node_async(new KnowledgeRetrieveNode(agentRagService)));
```

## 15. 第十一步：编译

执行：

```powershell
cd D:\code\project\inventory\inventory_back
mvn -q -DskipTests compile
```

如果这里报：

```text
找不到 org.springframework.ai.vectorstore.redis.RedisVectorStore
```

说明 `spring-ai-starter-vector-store-redis` 没加成功。

如果这里报：

```text
找不到 EmbeddingModel
```

说明 DashScope starter 或 embedding 配置有问题。

## 16. 第十二步：启动项目

执行：

```powershell
cd D:\code\project\inventory\inventory_back
mvn spring-boot:run
```

启动时会创建 Redis 向量索引。

如果启动失败并看到：

```text
ERR unknown command 'FT.CREATE'
```

说明你连的是普通 Redis，不是 Redis Stack。

如果看到：

```text
NOAUTH Authentication required
```

说明 Redis 密码配置不对。

如果看到：

```text
Cannot connect to Redis
```

检查：

```text
1. spring.data.redis.host
2. spring.data.redis.port
3. 服务器安全组
4. Redis 是否允许远程连接
5. Redis Stack 是否已启动
```

## 17. 第十三步：导入最小知识库

在正式导入前，先统一字段规范。

### 17.1 导入字段规范

| 字段 | 是否关键 | 作用 | 推荐怎么填 | 不建议怎么填 | 备注 |
|---|---|---|---|---|---|
| `docCode` | 高 | 文档唯一标识，决定是否覆盖旧文档 | 大写字母、数字、下划线、中划线 | 中文、空格、随意缩写 | 同一个 `docCode` 再导入会覆盖旧内容 |
| `title` | 低 | 展示标题 | 中文短标题 | 过长、无意义标题 | 只影响展示，不影响检索逻辑 |
| `docType` | 中 | 文档类型元数据 | 固定枚举值 | 同一类文档写很多变体 | 当前不参与检索过滤，但建议规范 |
| `bizIntent` | 高 | 指定哪条 Workflow 分支能召回 | 固定枚举值 | 缩写、错拼、自定义值 | 这个字段最不能乱写 |
| `sourcePath` | 低 | 来源说明 | `manual` 或实际文档路径 | 空字符串、无意义值 | 主要用于追踪来源 |
| `content` | 高 | 真正进入向量库的正文 | 规则、FAQ、术语说明 | 实时业务数据、订单明细 | 只放规则知识，不放事实数据 |

说明：

```text
docCode 决定是不是覆盖同一份文档。
bizIntent 决定这条知识能不能被正确召回。
title 和 sourcePath 主要影响展示与追踪。
docType 当前只是 metadata，但建议保持固定枚举。
```

### 17.2 bizIntent 可用值

| 值 | 用途 | 什么时候用 |
|---|---|---|
| `COMMON` | 公共知识 | 所有分支都可能用到的规则、边界、术语 |
| `ORDER_DIAGNOSIS` | 订单诊断 | 采购订单状态流转、卡点解释、角色分工 |
| `WARNING_SCAN` | 预警扫描 | 风险识别、优先级、处理顺序 |
| `SUPPLIER_SCORE` | 供应商评分 | 履约评价、分数解释、合作建议 |
| `KNOWLEDGE_QA` | 知识问答 | 系统规则、术语、通用业务说明 |

注意：

```text
bizIntent 必须和 WorkflowIntent 对齐。
不要写成 NOSIS、WARN、SCORE 这种自定义缩写。
如果写错，文档会导入成功，但检索时可能永远召回不到。
```

### 17.3 docType 推荐枚举

| 值 | 含义 | 示例 |
|---|---|---|
| `BUSINESS_RULE` | 业务规则 | 状态流转、预警规则、评分规则 |
| `SYSTEM_RULE` | 系统边界/系统限制 | Agent 能做什么、不能做什么 |
| `FAQ` | 常见问答 | “为什么订单没完成”这类问答说明 |
| `GLOSSARY` | 术语说明 | WAIT_CONFIRM、PARTIAL_ARRIVAL 含义 |
| `MANUAL` | 操作或说明文档 | 操作手册片段、规范说明 |

说明：

```text
当前代码不会按 docType 过滤检索。
所以 docType 改了不会直接影响召回。
但为了后续扩展，建议你只用上面这几个值。
```

### 17.4 docCode 命名规范

推荐格式：

```text
<领域>_<主题>_<版本>
```

推荐示例：

| 场景 | 推荐 docCode |
|---|---|
| 公共边界 | `COMMON_AGENT_BOUNDARY_V1` |
| 订单状态规则 | `ORDER_STATUS_RULE_V1` |
| 订单入库规则 | `ORDER_INBOUND_RULE_V1` |
| 订单异常处理规则 | `ORDER_EXCEPTION_RULE_V1` |
| 预警扫描规则 | `WARNING_SCAN_RULE_V1` |
| 预警优先级规则 | `WARNING_PRIORITY_RULE_V1` |
| 供应商评分规则 | `SUPPLIER_SCORE_RULE_V1` |
| 供应商合作建议规则 | `SUPPLIER_COOP_RULE_V1` |
| 采购术语说明 | `COMMON_PROCUREMENT_GLOSSARY_V1` |

说明：

```text
如果你是更新同一份知识，保持同一个 docCode。
如果你是新增另一份知识，换一个新的 docCode。
当前代码中，同一个 docCode 再导入，会先删除旧分片，再写入新分片。
```

### 17.5 推荐填写模板

公共知识模板：

```json
{
  "docCode": "COMMON_AGENT_BOUNDARY_V1",
  "title": "采购执行Agent能力边界",
  "docType": "SYSTEM_RULE",
  "bizIntent": "COMMON",
  "sourcePath": "manual",
  "content": "..."
}
```

订单诊断知识模板：

```json
{
  "docCode": "ORDER_STATUS_RULE_V1",
  "title": "采购订单状态流转规则",
  "docType": "BUSINESS_RULE",
  "bizIntent": "ORDER_DIAGNOSIS",
  "sourcePath": "document/状态机设计.md",
  "content": "..."
}
```

预警知识模板：

```json
{
  "docCode": "WARNING_SCAN_RULE_V1",
  "title": "采购执行预警规则",
  "docType": "BUSINESS_RULE",
  "bizIntent": "WARNING_SCAN",
  "sourcePath": "manual",
  "content": "..."
}
```

供应商评分知识模板：

```json
{
  "docCode": "SUPPLIER_SCORE_RULE_V1",
  "title": "供应商履约评分解释规则",
  "docType": "BUSINESS_RULE",
  "bizIntent": "SUPPLIER_SCORE",
  "sourcePath": "manual",
  "content": "..."
}
```

### 17.6 哪些改法是安全的

| 改动 | 是否安全 | 原因 |
|---|---|---|
| 改 `title` | 是 | 只影响展示 |
| 改 `content` | 是 | 这是正常更新知识 |
| 改 `sourcePath` | 是 | 只影响来源记录 |
| 改 `docType` 从 `BUSINESS_RULE` 到 `RULE` | 勉强可以 | 当前不影响检索，但不建议乱写 |
| 保持 `docCode` 不变重新导入 | 是 | 会覆盖旧文档 |
| 改 `docCode` | 可以，但会变成新文档 | 不再覆盖旧文档 |
| 改 `bizIntent` 为非枚举值 | 否 | Workflow 检索时可能永远召回不到 |

示例：

下面这种改法不推荐：

```json
{
  "docCode": "ORDER_STATUS_RULE_V1",
  "title": "流转规则",
  "docType": "RULE",
  "bizIntent": "NOSIS",
  "sourcePath": "manual",
  "content": "采购订单状态包括 WAIT_CONFIRM、IN_PROGRESS、PARTIAL_ARRIVAL..."
}
```

原因：

```text
title 改成“流转规则”没问题。
docType 改成 RULE 当前也不会直接导致报错。
但 bizIntent=NOSIS 无法和 WorkflowIntent.ORDER_DIAGNOSIS 对上，检索时很可能召回不到。
```

正确写法应该是：

```json
{
  "docCode": "ORDER_STATUS_RULE_V1",
  "title": "流转规则",
  "docType": "BUSINESS_RULE",
  "bizIntent": "ORDER_DIAGNOSIS",
  "sourcePath": "manual",
  "content": "采购订单状态包括 WAIT_CONFIRM、IN_PROGRESS、PARTIAL_ARRIVAL..."
}
```

### 17.7 导入公共边界知识

接口：

```text
POST http://localhost:8080/agent/rag/import
```

Body：

```json
{
  "docCode": "COMMON_AGENT_BOUNDARY_V1",
  "title": "采购执行Agent能力边界",
  "docType": "BUSINESS_RULE",
  "bizIntent": "COMMON",
  "sourcePath": "manual",
  "content": "采购执行 Agent 只能做只读分析和建议，不能自动修改采购订单、到货单、入库单状态。涉及订单确认、到货登记、入库确认、取消订单等写操作，必须由系统已有业务接口和对应角色执行。Agent 回答中不得编造数据库没有返回的订单、供应商、仓库、库存、金额、数量信息。业务事实以 Mapper 查询结果为准，知识库只提供规则解释。"
}
```

预期返回：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "docCode": "COMMON_AGENT_BOUNDARY_V1",
    "title": "采购执行Agent能力边界",
    "bizIntent": "COMMON",
    "chunkCount": 1,
    "message": "知识导入成功"
  }
}
```

### 17.8 导入订单诊断知识

接口：

```text
POST http://localhost:8080/agent/rag/import
```

Body：

```json
{
  "docCode": "ORDER_STATUS_RULE_V1",
  "title": "采购订单状态流转规则",
  "docType": "BUSINESS_RULE",
  "bizIntent": "ORDER_DIAGNOSIS",
  "sourcePath": "document/状态机设计.md",
  "content": "采购订单状态包括 WAIT_CONFIRM、IN_PROGRESS、PARTIAL_ARRIVAL、COMPLETED、CLOSED、CANCELLED。WAIT_CONFIRM 表示采购订单已创建但供应商尚未确认，下一步通常需要采购员跟进供应商确认。IN_PROGRESS 表示供应商已确认订单，订单进入执行阶段，如果长时间没有到货记录，通常需要采购员跟进供应商发货。PARTIAL_ARRIVAL 表示订单已有部分到货，如果已到货数量小于采购总数量，说明仍有剩余数量未到货，需要采购员继续催收到货。如果已到货数量已经达到采购总数量但已入库数量不足，说明流程卡在入库确认阶段，需要仓库岗检查待确认入库单并完成入库确认。COMPLETED 表示采购订单到货和入库闭环已完成。CLOSED 和 CANCELLED 表示流程已终止，通常不能继续按原订单推进。"
}
```

### 17.9 导入预警扫描知识

接口：

```text
POST http://localhost:8080/agent/rag/import
```

Body：

```json
{
  "docCode": "WARNING_SCAN_RULE_V1",
  "title": "采购执行预警规则",
  "docType": "BUSINESS_RULE",
  "bizIntent": "WARNING_SCAN",
  "sourcePath": "manual",
  "content": "采购执行预警用于发现采购订单、到货单、入库单在流程中的停滞风险。待确认超时指采购订单长期停留在 WAIT_CONFIRM，优先由采购员跟进供应商确认。执行中无到货指订单已进入 IN_PROGRESS 但超过指定天数没有到货记录，优先由采购员跟进供应商发货。部分到货停滞指订单处于 PARTIAL_ARRIVAL 且长时间没有新增到货，需要采购员催促供应商补齐剩余数量。到货后未入库指到货记录存在但没有生成入库单，优先由仓库岗处理。待确认入库超时指入库单长时间处于 PENDING，优先由仓库岗确认入库。高风险通常优先于中风险处理，同一供应商或同一仓库集中出现的问题需要优先排查。"
}
```

### 17.10 导入供应商评分知识

接口：

```text
POST http://localhost:8080/agent/rag/import
```

Body：

```json
{
  "docCode": "SUPPLIER_SCORE_RULE_V1",
  "title": "供应商履约评分解释规则",
  "docType": "BUSINESS_RULE",
  "bizIntent": "SUPPLIER_SCORE",
  "sourcePath": "manual",
  "content": "供应商履约评分用于评价供应商在指定统计周期内的采购执行表现。评分应结合订单完成情况、取消或关闭订单情况、异常到货情况、到货完成情况和入库完成情况进行解释。分数越高表示履约越稳定。统计周期内没有订单时，结论应该是数据不足，而不是供应商不存在。供应商不存在只能由数据库查询明确返回不存在时才能说明。合作建议应区分优秀、良好、一般、较差。优秀供应商可以保持合作并考虑优先合作，良好供应商可以继续合作但关注局部风险，一般供应商需要观察并设定改进要求，较差供应商建议谨慎合作或启动替代供应商评估。"
}
```

## 18. 第十四步：单独测试 RAG 检索

接口：

```text
POST http://localhost:8080/agent/rag/search
```

Body：

```json
{
  "query": "订单已经全部到货但是为什么还没有完成，应该谁处理？",
  "bizIntent": "ORDER_DIAGNOSIS",
  "topK": 3
}
```

预期返回类似：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": "ORDER_STATUS_RULE_V1:1:9a5b6d...",
      "docCode": "ORDER_STATUS_RULE_V1",
      "title": "采购订单状态流转规则",
      "docType": "BUSINESS_RULE",
      "bizIntent": "ORDER_DIAGNOSIS",
      "sourcePath": "document/状态机设计.md",
      "chunkNo": 1,
      "content": "采购订单状态包括 WAIT_CONFIRM...",
      "score": 0.78
    }
  ]
}
```

如果返回空数组：

```text
1. 确认 /agent/rag/import 返回成功
2. 确认 Redis Stack 里存在你当前 prefix 对应的 key
3. 确认 query 不要太短
4. 临时把 DEFAULT_SIMILARITY_THRESHOLD 从 0.45 改成 0.30
5. 确认 bizIntent 和导入时一致
```

Redis 里可以这样看 key：

```powershell
redis-cli -h 你的Redis地址 -p 你的Redis端口 KEYS "你的prefix*"
```

看索引：

```powershell
redis-cli -h 你的Redis地址 -p 你的Redis端口 FT._LIST
```

## 19. 第十五步：测试 Workflow 接入 RAG

接口：

```text
POST http://localhost:8080/agent/workflow/execute
```

### 19.1 测订单诊断

Body：

```json
{
  "message": "PO2026040011 为什么还没完成，下一步谁处理？",
  "threadId": "agt-rag-order-001"
}
```

观察点：

```text
1. intent 应该是 ORDER_DIAGNOSIS
2. data 应该是 OrderDiagnosisVO
3. answer 应该结合订单状态规则解释
4. answer 不能编造数据库没有返回的数据
```

### 19.2 测连续追问

Body：

```json
{
  "message": "那为什么不是供应商继续处理？",
  "threadId": "agt-rag-order-001"
}
```

观察点：

```text
1. Workflow 应该沿用上一次的 ORDER_DIAGNOSIS
2. Entity 应该沿用上一次的订单号
3. RAG 应该继续召回订单状态规则
4. 回答应该围绕当前问题解释处理角色
```

### 19.3 测预警扫描

Body：

```json
{
  "message": "扫描最近 7 天采购执行风险，哪些最严重？",
  "threadId": "agt-rag-warning-001"
}
```

观察点：

```text
1. intent 应该是 WARNING_SCAN
2. data 应该是 WarningScanVO
3. answer 应该结合预警规则解释优先级
```

### 19.4 测供应商评分

Body：

```json
{
  "message": "分析供应商 1 最近 30 天履约表现，这个分数还能继续合作吗？",
  "threadId": "agt-rag-supplier-001"
}
```

观察点：

```text
1. intent 应该是 SUPPLIER_SCORE
2. data 应该是 SupplierScoreVO
3. answer 应该解释分数等级和合作建议
4. 如果没有订单，应该说数据不足，不应该说供应商不存在
```

## 20. 为什么 RAG 放在 routeByIntent 前面

你当前 Workflow 是：

```text
classifyIntent
    ↓
extractEntities
    ↓
retrieveKnowledge
    ↓
routeByIntent
```

这个顺序可以保留。

原因：

```text
retrieveKnowledge 已经能拿到 intent 和 message。
它可以按 COMMON + 当前 intent 做过滤检索。
检索到的规则知识会被后面的业务分支共用。
```

不要把 RAG 放到最前面。

原因：

```text
没有 intent 时，只能全库检索，噪声更大。
先识别意图，再按 intent 检索，命中更稳。
```

## 21. 当前版本的能力边界

当前版本已经具备：

```text
1. Redis Stack 存知识向量
2. VectorStore 自动生成向量并写入 Redis
3. 按 docCode 重复导入覆盖
4. 按 bizIntent 过滤检索
5. Workflow 自动召回 ragDocs
6. 订单诊断、预警扫描、供应商评分都能使用知识库
```

当前版本暂不做：

```text
1. Markdown 文件上传
2. document 目录自动扫描
3. 知识版本审批
4. 多租户知识隔离
5. rerank 重排
6. RAG 检索过程落 agent_message
```

原因：

```text
先保证主链路稳定可演示。
这些能力后续可以逐步加。
```

## 22. 常见问题

### 22.1 启动时报 unknown command FT.CREATE

原因：

```text
连接的是普通 Redis，不是 Redis Stack。
```

处理：

```text
换 Redis Stack。
用 MODULE LIST 检查是否有 search 模块。
```

### 22.2 找不到 VectorStore Bean

检查：

```text
1. pom.xml 是否添加 spring-ai-starter-vector-store-redis
2. RedisVectorStoreConfig 是否在 com.xixi.agent.config 包下
3. 是否能注入 EmbeddingModel
4. application-dev.yml 是否配置 DashScope API Key
```

### 22.3 找不到 EmbeddingModel Bean

检查：

```text
1. pom.xml 是否有 spring-ai-alibaba-starter-dashscope
2. application-dev.yml 是否有 spring.ai.dashscope.api-key
3. application-dev.yml 是否有 spring.ai.dashscope.embedding.enabled=true
4. 环境变量 AI_DASHSCOPE_API_KEY 是否存在
```

### 22.4 检索为空

检查：

```text
1. 是否先调用 /agent/rag/import
2. docCode 是否合法
3. bizIntent 是否匹配
4. Redis 里是否有 inventory:agent:rag:* key
5. DEFAULT_SIMILARITY_THRESHOLD 是否过高
```

### 22.5 重复导入后旧知识还被召回

检查：

```text
1. docCode 是否和上次完全一致
2. docCode 是否包含非法字符导致校验失败
3. vectorStore.delete 是否执行成功
4. Redis 索引是否使用同一个 prefix 和 index-name
```

### 22.6 正式环境不想暴露导入接口

当前：

```text
/agent/rag/import
```

是为了开发测试方便。

正式环境建议：

```text
1. 只允许 ADMIN 角色访问
2. 或者只保留内部运维脚本导入
3. 或者导入完成后先临时关闭这个接口
```

## 23. 第一版完成标准

完成后必须满足：

```text
1. Redis Stack 的 MODULE LIST 能看到 search
2. mvn -q -DskipTests compile 通过
3. 应用启动时不报 FT.CREATE 错误
4. /agent/rag/import 能成功返回 chunkCount
5. Redis 里能看到 inventory:agent:rag:* key
6. /agent/rag/search 能返回相似片段
7. /agent/workflow/execute 能正常返回 answer + data
8. 订单诊断回答能结合订单状态规则
9. 预警扫描回答能结合预警优先级规则
10. 供应商评分回答能结合评分解释规则
11. 连续追问仍能结合 threadId 恢复上下文
```

## 24. 后续升级方向

第一版跑通后，再考虑：

```text
1. 从 document 目录读取 Markdown 并批量导入
2. 上传 Markdown 文件自动导入
3. 给 RAG 导入接口加管理员权限
4. RAG 检索结果落 agent_message，方便审计
5. 使用 DashScope rerank 对 topK 重排
6. 根据不同 agentType 建不同 Redis index
7. 给知识加 version、status、operator 字段
8. 支持禁用某个 docCode
```

## 25. 总结

这版 RAG 的本质是：

```text
Redis Stack 负责向量存储和相似度检索
Spring AI VectorStore 负责统一 add / search / delete
DashScope Embedding 负责把文本变成向量
KnowledgeRetrieveNode 负责把召回结果塞进 ragDocs
BusinessAnswerGenerateNode 负责结合业务事实和规则知识回答
```

最关键的原则：

```text
业务事实不能从 RAG 来。
业务事实必须从 Mapper 来。
RAG 只负责解释规则、补充背景、提升回答专业度。
```

这样做比 MySQL 存向量更符合你当前项目：

```text
1. 你已经使用 Redis
2. Redis Stack 可以直接充当向量数据库
3. Spring AI 已经提供 RedisVectorStore
4. 代码少，不需要新增 RAG 表和 Mapper XML
5. 后续扩展到文件导入、审计、rerank 更顺
```

## 26. 参考资料

```text
Spring AI Redis Vector Store:
https://docs.spring.io/spring-ai/reference/api/vectordbs/redis.html

Redis Stack:
https://redis.io/docs/latest/operate/oss_and_stack/install/install-stack/
```
