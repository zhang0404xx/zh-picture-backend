# 智图画廊（zh-picture-backend）项目总结文档

> 本文档对 zh-picture-backend 项目进行全面的架构设计、技术选型、模块划分及开发进度总结。

---

## 一、项目概述

| 项目属性 | 说明 |
|----------|------|
| **项目名称** | zh-picture-backend（智图画廊后端） |
| **项目描述** | 基于 Spring Boot 的图片画廊后端服务 |
| **版本** | 0.0.1-SNAPSHOT |
| **作者** | zh |
| **服务端口** | 8123 |
| **上下文路径** | /api |
| **数据库** | zh_picture（MySQL） |
| **API 文档** | Knife4j（访问地址：`http://localhost:8123/api/doc.html`） |

---

## 二、技术栈

### 2.1 核心框架

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 (Oracle OpenJDK 21.0.10) | 开发语言 |
| Spring Boot | 4.0.7 | 应用框架（自动配置、依赖注入、Web 服务） |
| MyBatis-Plus | 3.5.12 | ORM 框架（通用 CRUD、分页插件、代码生成） |
| MySQL | - | 关系型数据库 |
| Lombok | - | 代码简化（自动生成 getter/setter/构造器等） |
| Spring AOP | 3.2.0 | 面向切面编程（日志、权限拦截等） |
| Knife4j | 4.5.0 | API 接口文档（基于 OpenAPI 3） |

### 2.2 构建工具

| 工具 | 说明 |
|------|------|
| Maven | 项目构建和依赖管理 |
| spring-boot-maven-plugin | 打包可执行 JAR（排除 Lombok） |
| maven-compiler-plugin | 编译期 Lombok 注解处理器 |

---

## 三、项目结构

```
zh-picture-backend/
├── src/main/java/com/zh/zhpicturebackend/
│   ├── common/                  # 通用模块
│   │   ├── BaseResponse.java    # 统一响应封装
│   │   ├── DeleteRequest.java   # 通用删除请求
│   │   ├── ErrorCode.java       # 错误码枚举
│   │   ├── PageRequest.java     # 分页请求封装
│   │   └── ResultUtils.java     # 响应工具类
│   ├── config/                  # 配置模块
│   │   └── SwaggerConfig.java   # Swagger/Knife4j 配置
│   ├── constant/                # 常量模块
│   │   ├── CommonConstant.java  # 通用常量（排序）
│   │   ├── FileConstant.java    # 文件常量（COS 地址）
│   │   └── UserConstant.java    # 用户常量（角色、登录态）
│   ├── exception/               # 异常处理模块
│   │   ├── BusinessException.java       # 自定义业务异常
│   │   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   │   └── ThrowUtils.java              # 抛异常工具类
│   ├── entity/                  # 实体类（待实现）
│   ├── mapper/                  # 数据访问层（待实现）
│   └── ZhPictureBackendApplication.java # 启动类
├── src/main/resources/
│   ├── application.yml          # 应用配置文件
│   ├── static/                  # 静态资源
│   └── templates/               # 模板文件
└── src/test/                    # 测试目录
```

---

## 四、模块设计详解

### 4.1 通用响应模块（common）

**设计模式**：统一响应封装 + 工具类

```
BaseResponse<T>          ← 所有 API 返回的统一格式
  ├── code: int          ← 状态码（0 = 成功）
  ├── data: T            ← 返回数据（泛型）
  └── message: String    ← 提示信息

ResultUtils              ← 快速构造响应的工具类
  ├── success(data)      → {code: 0, data: T, message: "ok"}
  └── error(...)         → {code: 错误码, data: null, message: 提示信息}
```

**标准响应示例**：

```json
// 成功响应
{
    "code": 0,
    "data": { ... },
    "message": "ok"
}

// 失败响应
{
    "code": 40000,
    "data": null,
    "message": "请求参数错误"
}
```

### 4.2 分页请求模块（common）

`PageRequest` 封装分页查询的通用参数：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| current | int | 1 | 当前页号 |
| pageSize | int | 10 | 每页大小 |
| sortField | String | null | 排序字段 |
| sortOrder | String | "ascend" | 排序顺序（升序/降序） |

### 4.3 异常处理模块（exception）

**三层设计**：

```
ErrorCode（错误码定义）
    ↓ 引用
BusinessException（业务异常类）
    ↓ 抛出
ThrowUtils（条件抛异常工具）
    ↓ 全局捕获
GlobalExceptionHandler（统一异常处理）
    ↓ 转换输出
BaseResponse（JSON 响应）
```

### 4.4 常量模块（constant）

| 常量类 | 职责 | 关键常量 |
|--------|------|----------|
| `CommonConstant` | 通用排序常量 | `SORT_ORDER_ASC`, `SORT_ORDER_DESC` |
| `FileConstant` | 文件存储常量 | `COS_HOST`（对象存储访问地址） |
| `UserConstant` | 用户角色常量 | `USER_LOGIN_STATE`, `DEFAULT_ROLE`, `ADMIN_ROLE`, `BAN_ROLE` |

### 4.5 配置模块（config）

| 配置类 | 职责 |
|--------|------|
| `SwaggerConfig` | 配置 OpenAPI 文档信息（标题、描述、版本、联系人） |
| `application.yml` | 服务器端口、数据库连接、MyBatis-Plus、Knife4j |

---

## 五、数据库配置

| 配置项 | 值 |
|--------|-----|
| 数据库 | zh_picture |
| 驱动 | com.mysql.cj.jdbc.Driver |
| 连接池 | HikariCP（最大连接数 10） |
| 字符编码 | UTF-8 |
| 时区 | Asia/Shanghai |

### MyBatis-Plus 配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| mapper-locations | classpath:mapper/*.xml | Mapper XML 路径 |
| type-aliases-package | com.zh.zhpicturebackend.model.entity | 实体类别名包 |
| map-underscore-to-camel-case | true | 驼峰命名自动映射 |
| cache-enabled | true | 开启二级缓存 |
| log-impl | StdOutImpl | 控制台打印 SQL |
| logic-delete-field | isDeleted | 逻辑删除字段 |
| logic-delete-value | 1 / 0 | 删除值 / 未删除值 |

---

## 六、启动类配置

`ZhPictureBackendApplication` 启用了以下功能：

| 注解 | 作用 |
|------|------|
| `@SpringBootApplication` | Spring Boot 自动配置 + 组件扫描 |
| `@MapperScan("...mapper")` | 自动扫描 Mapper 接口 |
| `@EnableAspectJAutoProxy(exposeProxy = true)` | 启用 AOP 代理，允许通过 `AopContext.currentProxy()` 获取代理对象 |

---

## 七、项目开发现状

### 7.1 已完成

- [x] 项目基础框架搭建（Spring Boot + MyBatis-Plus + MySQL）
- [x] 统一响应封装（BaseResponse + ResultUtils）
- [x] 错误码体系设计（ErrorCode 枚举）
- [x] 全局异常处理机制（GlobalExceptionHandler）
- [x] 抛异常工具类（ThrowUtils）
- [x] 分页请求封装（PageRequest）
- [x] 通用删除请求（DeleteRequest）
- [x] 常量定义（CommonConstant / FileConstant / UserConstant）
- [x] Swagger/Knife4j API 文档配置
- [x] 数据库连接及 MyBatis-Plus 配置
- [x] 逻辑删除配置

### 7.2 待实现

- [ ] **entity 实体层**：数据库表对应的实体类（如 User、Picture 等）
- [ ] **mapper 数据访问层**：MyBatis-Plus Mapper 接口及 XML
- [ ] **model 模型层**：DTO（数据传输对象）、VO（视图对象）、请求类
- [ ] **service 业务层**：核心业务逻辑实现
- [ ] **controller 控制器层**：RESTful API 接口定义
- [ ] **用户模块**：登录、注册、权限校验
- [ ] **图片模块**：图片上传、查询、管理
- [ ] **文件存储**：COS 对象存储集成（需替换 COS_HOST）
- [ ] **参数校验**：引入 validation 框架，添加 `@Valid` 注解校验
- [ ] **单元测试**：Service / Controller 层测试用例

---

## 八、API 接口文档

| 项目 | 地址 |
|------|------|
| Knife4j 文档页面 | `http://localhost:8123/api/doc.html` |
| Swagger UI | `http://localhost:8123/api/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8123/api/v3/api-docs` |

---

## 九、启动与运行

### 9.1 环境要求

- JDK 21+
- MySQL 数据库（已创建 `zh_picture` 库）
- Maven 3.6+

### 9.2 启动步骤

```bash
# 1. 确保 MySQL 已启动并创建数据库
CREATE DATABASE zh_picture DEFAULT CHARACTER SET utf8mb4;

# 2. 修改 application.yml 中的数据库连接信息

# 3. 编译并启动
mvnw spring-boot:run

# 4. 访问 API 文档
# http://localhost:8123/api/doc.html
```

---

## 十、后续规划建议

| 优先级 | 任务 | 说明 |
|--------|------|------|
| **P0** | 完成 entity/mapper 层 | 建立数据模型，完成数据库表设计 |
| **P0** | 完成 service/controller 层 | 实现核心 API 接口 |
| **P1** | 用户认证与权限模块 | 登录注册、Session 管理、角色权限 |
| **P1** | 图片上传与存储 | 集成 COS 对象存储 |
| **P2** | 参数校验框架 | 引入 `spring-boot-starter-validation` |
| **P2** | 跨域配置 | 添加 CORS 支持 |
| **P3** | 日志配置优化 | 添加 logback-spring.xml，按环境区分日志级别 |
| **P3** | 敏感信息外部化 | 数据库密码等使用环境变量管理 |
| **P3** | 单元测试覆盖 | 编写 Service/Controller 测试用例 |

---

*最后更新：2026-08-08*
