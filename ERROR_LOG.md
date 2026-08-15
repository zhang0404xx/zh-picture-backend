# 智图画廊（zh-picture-backend）错误记录文档

> 本文档记录了项目的错误码体系、异常处理机制、常见错误场景及排查指南。

---

## 一、错误码体系

项目使用 `ErrorCode` 枚举统一管理所有错误码，定义在 `com.zh.zhpicturebackend.common.ErrorCode` 中。

| 错误码 | 枚举名称 | 提示信息 | 说明 |
|--------|----------|----------|------|
| 0 | `SUCCESS` | ok | 请求成功 |
| 40000 | `PARAMS_ERROR` | 请求参数错误 | 前端传递的参数不合法（缺失、格式错误、越界等） |
| 40100 | `NOT_LOGIN_ERROR` | 未登录 | 用户未登录或登录态已过期 |
| 40101 | `NO_AUTH_ERROR` | 无权限 | 用户已登录但无操作权限（角色不匹配） |
| 40300 | `FORBIDDEN_ERROR` | 禁止访问 | 用户被封号或资源被限制访问 |
| 40400 | `NOT_FOUND_ERROR` | 请求数据不存在 | 查询的目标数据在数据库中不存在 |
| 50000 | `SYSTEM_ERROR` | 系统内部异常 | 服务端未预期的运行时异常 |
| 50001 | `OPERATION_ERROR` | 操作失败 | 业务操作执行失败（如数据库写入失败） |

### 错误码编号规则

- `0`：成功
- `4xxxx`：客户端错误（参数、认证、权限）
- `5xxxx`：服务端错误（系统异常、操作失败）

---

## 二、异常处理机制

### 2.1 架构概览

```
请求 → Controller → Service → 业务逻辑
                                    ↓ 异常抛出
                            BusinessException / RuntimeException
                                    ↓ 全局捕获
                          GlobalExceptionHandler
                                    ↓ 统一响应
                           BaseResponse (JSON)
```

### 2.2 核心组件

#### （1）BusinessException（自定义业务异常）

- **位置**：`com.zh.zhpicturebackend.exception.BusinessException`
- **继承**：`RuntimeException`
- **字段**：`code`（错误码）、`message`（异常消息，继承自 `RuntimeException`）
- **构造方式**：
  - `new BusinessException(int code, String message)` — 自定义错误码和消息
  - `new BusinessException(ErrorCode errorCode)` — 使用枚举预设值
  - `new BusinessException(ErrorCode errorCode, String message)` — 使用枚举错误码 + 自定义消息

#### （2）ThrowUtils（抛异常工具类）

- **位置**：`com.zh.zhpicturebackend.exception.ThrowUtils`
- **用法**：通过条件判断快速抛出异常，简化 `if-throw` 代码

```java
// 方式一：条件成立则抛指定异常
ThrowUtils.throwIf(condition, new BusinessException(ErrorCode.PARAMS_ERROR));

// 方式二：条件成立则抛枚举对应异常
ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

// 方式三：条件成立则抛枚举异常 + 自定义消息
ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "id 必须大于 0");
```

#### （3）GlobalExceptionHandler（全局异常处理器）

- **位置**：`com.zh.zhpicturebackend.exception.GlobalExceptionHandler`
- **注解**：`@RestControllerAdvice` — 自动拦截所有 Controller 层抛出的异常
- **处理策略**：

| 异常类型 | 处理方法 | 日志级别 | 返回内容 |
|----------|----------|----------|----------|
| `BusinessException` | `businessExceptionHandler` | `ERROR` | 返回业务错误码 + 自定义消息 |
| `RuntimeException` | `runtimeExceptionHandler` | `ERROR` | 返回 `SYSTEM_ERROR(50000)` + "系统错误" |

### 2.3 统一响应格式

所有异常最终被转换为 `BaseResponse` JSON 格式返回：

```json
{
    "code": 40000,
    "data": null,
    "message": "请求参数错误"
}
```

---

## 三、常见错误场景及处理

### 3.1 参数校验类错误（40000）

| 场景 | 触发条件 | 处理方式 |
|------|----------|----------|
| 请求参数为空 | 必填字段未传 | `ThrowUtils.throwIf(param == null, ErrorCode.PARAMS_ERROR)` |
| ID 不合法 | id <= 0 | `ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "id 必须大于 0")` |
| 分页参数越界 | current < 1 或 pageSize 异常 | 在 Controller 层校验并抛出 |
| 排序字段非法 | sortField 不在白名单中 | 校验排序字段合法性 |

### 3.2 认证权限类错误（40100 / 40101 / 40300）

| 场景 | 错误码 | 触发条件 | 处理方式 |
|------|--------|----------|----------|
| 未登录 | 40100 | Session 中无用户信息 | 检查 `USER_LOGIN_STATE` |
| 无权限 | 40101 | 用户角色不满足操作要求 | 校验用户角色是否为 `admin` |
| 被封号 | 40300 | 用户角色为 `ban` | 校验用户角色不为 `ban` |

### 3.3 数据类错误（40400）

| 场景 | 触发条件 | 处理方式 |
|------|----------|----------|
| 查询不到数据 | 数据库查询返回 null | `ThrowUtils.throwIf(result == null, ErrorCode.NOT_FOUND_ERROR)` |
| 删除目标不存在 | 删除操作影响行数为 0 | 先查询再操作，或根据返回值判断 |

### 3.4 系统类错误（50000 / 50001）

| 场景 | 错误码 | 触发条件 | 处理方式 |
|------|--------|----------|----------|
| 数据库异常 | 50000 | SQL 执行失败、连接超时 | 检查日志中的 SQL 和堆栈 |
| 操作失败 | 50001 | 业务逻辑执行未达预期 | 使用 `ErrorCode.OPERATION_ERROR` |
| 未知运行时异常 | 50000 | 未被 `BusinessException` 捕获的异常 | 查看 GlobalExceptionHandler 日志 |

---

## 四、潜在问题记录

### 4.1 代码层面

| 编号 | 文件 | 问题描述 | 严重程度 | 建议 |
|------|------|----------|----------|------|
| E-001 | `CommonConstant.java` | `SORT_ORDER_DESC` 值为 `" descend"`，前有多余空格 | **高** | 去除前导空格，改为 `"descend"` |
| E-002 | `FileConstant.java` | `COS_HOST` 仍为默认占位值 `"https://yupi.icu"` | **中** | 替换为实际的 COS 存储地址 |
| E-003 | `GlobalExceptionHandler.java` | 仅处理 `BusinessException` 和 `RuntimeException`，未覆盖 `Exception` | **中** | 考虑增加 `Exception` 兜底处理 |
| E-004 | `GlobalExceptionHandler.java` | 未处理 `MethodArgumentNotValidException`（参数校验异常） | **中** | 添加参数校验异常处理器，返回 40000 |
| E-005 | `application.yml` | 数据库密码硬编码为 `123456` | **高** | 使用环境变量或配置中心管理敏感信息 |
| E-006 | `application.yml` | `mapper-locations` 配置 `classpath:mapper/*.xml`，但 `resources/mapper` 目录不存在 | **低** | 创建目录或调整配置 |

### 4.2 架构层面

| 编号 | 问题描述 | 建议 |
|------|----------|------|
| A-001 | `entity` 和 `mapper` 目录为空，数据层未实现 | 尽快完成数据模型和数据访问层开发 |
| A-002 | 缺少 `controller` 和 `service` 包，API 层未实现 | 按模块规划 Controller/Service 结构 |
| A-003 | 未引入参数校验框架（如 `spring-boot-starter-validation`） | 添加 `@Valid` + `@NotBlank` 等注解校验 |
| A-004 | 缺少统一的日志配置（如 Logback 配置文件） | 添加 `logback-spring.xml` 配置日志输出 |
| A-005 | 未配置跨域（CORS）策略 | 添加 `WebMvcConfigurer` 配置跨域 |

---

## 五、错误排查流程

```
1. 查看前端收到的响应 JSON
   ↓
2. 根据 code 字段定位错误类型
   ├── 40000 → 检查请求参数
   ├── 40100 → 检查登录态 / Session
   ├── 40101 → 检查用户角色权限
   ├── 40300 → 检查用户是否被封号
   ├── 40400 → 确认目标数据是否存在
   ├── 50000 → 查看后端日志（控制台 / 日志文件）
   └── 50001 → 查看具体业务操作日志
   ↓
3. 查看后端控制台日志
   - GlobalExceptionHandler 会打印 ERROR 级别日志
   - MyBatis-Plus 会打印执行的 SQL（开发环境）
   ↓
4. 根据堆栈信息定位异常抛出的代码位置
   ↓
5. 修复问题并验证
```

---

## 六、错误日志格式参考

项目使用 Lombok 的 `@Slf4j` 注解输出日志，格式如下：

```
ERROR c.z.z.e.GlobalExceptionHandler - BusinessException
com.zh.zhpicturebackend.exception.BusinessException: 请求参数错误
    at com.zh.zhpicturebackend.service.XxxService.method(XxxService.java:42)
    ...
```

---

*最后更新：2026-08-08*
