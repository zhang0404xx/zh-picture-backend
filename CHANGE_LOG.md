# 智图画廊（zh-picture-backend）修改记录文档

> 本文档记录项目的所有重要变更、功能迭代及代码修改历史。

---

## 版本说明

| 版本号 | 日期 | 说明 |
|--------|------|------|
| v0.0.1 | 2026-08-08 | 项目初始化，完成基础框架搭建 |

---

## v0.0.1 — 项目初始化（2026-08-08）

### 新增

#### 项目基础框架

- 基于 **Spring Boot 4.0.7** + **Java 21** 创建项目
- 配置 **Maven** 构建体系，包含 `spring-boot-maven-plugin` 和 `maven-compiler-plugin`
- 集成 **Lombok** 注解处理器（编译期 / 测试编译期）

#### 数据库与持久层

- 接入 **MySQL** 数据库（`zh_picture`），配置 HikariCP 连接池（最大连接数 10）
- 集成 **MyBatis-Plus 3.5.12**，配置 Mapper 扫描、驼峰映射、二级缓存、SQL 日志输出
- 配置逻辑删除字段（`isDeleted`：1=已删除，0=未删除）

#### 通用模块（`common` 包）

| 文件 | 说明 |
|------|------|
| `BaseResponse.java` | 统一响应封装类（code / data / message） |
| `ResultUtils.java` | 响应构造工具类（success / error 静态方法） |
| `ErrorCode.java` | 错误码枚举（SUCCESS / PARAMS_ERROR / NOT_LOGIN_ERROR 等 8 个） |
| `DeleteRequest.java` | 通用删除请求类（包含 id 字段） |
| `PageRequest.java` | 分页请求封装类（current / pageSize / sortField / sortOrder） |

#### 异常处理模块（`exception` 包）

| 文件 | 说明 |
|------|------|
| `BusinessException.java` | 自定义业务异常类（继承 RuntimeException） |
| `ThrowUtils.java` | 条件抛异常工具类（3 个 throwIf 重载方法） |
| `GlobalExceptionHandler.java` | 全局异常处理器（处理 BusinessException + RuntimeException） |

#### 常量模块（`constant` 包）

| 文件 | 说明 |
|------|------|
| `CommonConstant.java` | 通用常量（排序顺序：ascend / descend） |
| `FileConstant.java` | 文件常量（COS 对象存储访问地址） |
| `UserConstant.java` | 用户常量（登录态键、角色：user / admin / ban） |

#### 配置模块（`config` 包）

| 文件 | 说明 |
|------|------|
| `SwaggerConfig.java` | Swagger/Knife4j OpenAPI 文档配置（标题：智图画廊接口文档） |

#### API 文档

- 集成 **Knife4j 4.5.0**（基于 OpenAPI 3）
- 文档访问地址：`http://localhost:8123/api/doc.html`
- 配置 Springdoc 的 Swagger UI 路径和排序规则

#### 启动类

- `ZhPictureBackendApplication.java`：启用 `@SpringBootApplication`、`@MapperScan`、`@EnableAspectJAutoProxy`

#### 应用配置

- `application.yml`：服务器端口 8123、上下文路径 `/api`、数据库连接、MyBatis-Plus、Knife4j 等完整配置

---

### 待修改 / 待实现

> 以下为后续版本需要完成的工作，记录在此便于追踪。

#### 高优先级

- [ ] 创建 `entity` 实体类（User、Picture 等数据模型）
- [ ] 创建 `mapper` 数据访问接口及 XML 映射文件
- [ ] 创建 `model` 包（DTO / VO / 请求类）
- [ ] 创建 `service` 业务逻辑层
- [ ] 创建 `controller` 控制器层（RESTful API 接口）

#### 中优先级

- [ ] 修复 `CommonConstant.SORT_ORDER_DESC` 前导空格问题（`" descend"` → `"descend"`）
- [ ] 替换 `FileConstant.COS_HOST` 占位值为实际 COS 地址
- [ ] `application.yml` 数据库密码外部化（使用环境变量）
- [ ] 引入 `spring-boot-starter-validation` 参数校验框架
- [ ] `GlobalExceptionHandler` 增加 `MethodArgumentNotValidException` 处理

#### 低优先级

- [ ] 添加 `logback-spring.xml` 日志配置
- [ ] 配置 CORS 跨域策略
- [ ] 创建 `resources/mapper` 目录
- [ ] 编写 Service / Controller 层单元测试

---

### 已知问题

| 编号 | 文件 | 问题 | 状态 |
|------|------|------|------|
| BUG-001 | `CommonConstant.java` | `SORT_ORDER_DESC` 值 `" descend"` 含前导空格，可能导致排序查询异常 | 待修复 |
| BUG-002 | `FileConstant.java` | `COS_HOST` 为默认占位值，文件上传功能将不可用 | 待修复 |
| BUG-003 | `application.yml` | `mapper-locations` 指向 `classpath:mapper/*.xml`，但目录不存在 | 待修复 |

---

## 修改记录模板

> 后续版本更新时，请按照以下格式追加记录：

```markdown
## vX.X.X — 版本描述（YYYY-MM-DD）

### 新增
- [功能描述]（涉及文件）

### 修改
- [修改描述]（涉及文件）

### 修复
- [修复描述]（涉及文件，关联 BUG 编号）

### 移除
- [移除描述]（涉及文件）

### 配置变更
- [配置变更描述]（涉及文件）
```

---

*最后更新：2026-08-08*
