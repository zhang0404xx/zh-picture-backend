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
| E-005 | `application.yml` → `application-local.yml` | ~~数据库密码硬编码为 `123456`~~ 连接配置已迁移至 `application-local.yml`（被 .gitignore 忽略，不入 Git 仓库） | 已解决 | 如需更严格可改用环境变量/配置中心管理 |
| E-006 | `application.yml` | `mapper-locations` 配置 `classpath:mapper/*.xml`，但 `resources/mapper` 目录不存在 | **低** | 创建目录或调整配置（注：目录已建，`UserMapper.xml` 已存在） |
| E-007 | `sql/create_table.sql` | `user` 表 DDL 缺少 `shareCode`、`inviteUser` 两列（2026-09-23 核验实际库中已存在；`User` 实体与 `UserMapper.xml` 均包含这两列） | **低** | 同步 DDL 文件与实际库结构，避免依据该文件重建库时实体查询报 Unknown column |

### 4.2 架构层面

| 编号 | 问题描述 | 建议 |
|------|----------|------|
| A-001 | ~~`entity` 和 `mapper` 目录为空，数据层未实现~~ | 已完成 User 模块（User 实体、UserMapper、UserService、UserController），后续待扩展 Picture 等模块 |
| A-002 | ~~缺少 `controller` 和 `service` 包，API 层未实现~~ | 已完成用户模块 RESTful 接口（注册/登录/注销/当前用户/增/查/改/删/分页） |
| A-003 | 未引入参数校验框架（如 `spring-boot-starter-validation`） | 添加 `@Valid` + `@NotBlank` 等注解校验 |
| A-004 | 缺少统一的日志配置（如 Logback 配置文件） | 添加 `logback-spring.xml` 配置日志输出 |
| A-005 | 未配置跨域（CORS）策略 | 添加 `WebMvcConfigurer` 配置跨域 |

---

## 五、历史错误记录（已修复）

> 开发过程中排查并修复的运行时错误（用户模块、COS 配置与文件上传等），供后续排查同类问题参考。

| 编号 | 错误现象 | 根因 | 修复方案 |
|------|----------|------|----------|
| H-001 | 访问 `/v3/api-docs` 报 `NoSuchMethodError: ControllerAdviceBean.<init>(Object)` | knife4j 4.5.0 传递的 springdoc 2.3.0 调用 Spring Framework 6.2 已删除的构造函数；存在 `@RestControllerAdvice` 时必触发 | 更换依赖（见 H-003） |
| H-002 | 升级 springdoc 2.8.6 后访问文档报 `NoSuchMethodError: SpringDocConfigProperties.getGroupConfigs()`（返回类型 List→Set 属签名变更） | knife4j 4.5.0 调用 springdoc 旧签名，与 H-001 构成双向不兼容，单纯调版本无法两全 | 更换依赖（见 H-003） |
| H-003 | Knife4j 与 Boot 3.4+ 兼容性（H-001 + H-002 根治） | 官方 com.github.xiaoymin 4.x 于 2024-01 停更 | 切换社区分支 `com.baizhukui:knife4j-openapi3-jakarta-spring-boot-starter:5.7.4`（内置 springdoc 2.8.9） |
| H-004 | 启动报 `Ambiguous mapping. Cannot map userController method` | `userRegister` 与 `addUser` 重复映射 `GET /user/register` | 分别改为 `POST /register`、`POST /add` |
| H-005 | 更新接口返回成功但数据未变（静默失败） | `UserUpdateRequest` 字段名（`username`/`email`/`phone`）与 `User` 实体（`userName`/`userProfile`）不一致，`BeanUtils.copyProperties` 静默忽略 | DTO 字段与实体对齐 |
| H-006 | `MybatisPlusConfig` 报 `Cannot resolve symbol 'PaginationInnerInterceptor'` | MyBatis-Plus 3.5.9 起分页插件拆至独立模块 | 补充 `mybatis-plus-jsqlparser:3.5.12` 依赖 |
| H-007 | IDEA 报 `java: 程序包com.zh.zhpicturebackend.model.entity不存在`，但命令行 Maven 编译成功 | IDEA 编译缓存与 pom 变更/命令行 clean 后的 target 不同步 | IDEA 中 Reload Maven + Rebuild Project |
| H-008 | `application-local.yml` 中的配置不生效（文件存在但从未被加载） | Spring Boot 多环境配置文件 `application-{profile}.yml` 仅在对应 profile 激活后才会加载，项目此前未配置 `spring.profiles.active` | `application.yml` 的 `spring` 节点下添加 `profiles.active: local` |
| H-009 | 启动创建 COS 客户端 Bean 报 `java.lang.IllegalArgumentException: Access key cannot be null.` | 配置类字段与 yml 键名不一致：`CosClientConfig` 字段为 `accessKey`，而 yml 中键名写成了 `secretId`；`@ConfigurationProperties` 按字段名绑定且对未知键静默忽略，`accessKey` 保持 null 传入 `BasicCOSCredentials` 抛异常。另外运行期加载的是 `target/classes` 中的构建副本，修改 `src` 下配置后未重建则不生效（该文件还曾被误命名为 `application-local.yml.xml`，错误后缀导致配置整体不加载） | 将 yml 键名统一为 `accessKey` 与 Java 字段对齐；全量重建（`mvnw clean test`）同步构建产物后启动验证通过（profile `local` 正常激活） |
| H-010 | 测试上传接口上传的 COS 对象 key 恒为 `uploads/`（文件名丢失、多次上传互相覆盖） | `String.format("uploads/" ,fileName)` 缺少 `%s` 占位符导致 fileName 参数被忽略；临时文件以含路径分隔符的 filePath 作前缀（JDK 21 会静默剔除分隔符，临时文件名不可预期） | 修正为 `String.format("uploads/%s", fileName)`；临时文件改用固定前缀 `File.createTempFile("upload-", null)` 创建（`FileController.java`） |
| H-011 | 用户注册接口执行 INSERT 报 `java.sql.SQLSyntaxErrorException: Unknown column 'user_account' in 'field list'`（MyBatis-Plus 3.5.12 + mysql-connector-j 9.1.0） | `application.yml` 中 `map-underscore-to-camel-case: true`，MyBatis-Plus 依此配置（`TableInfo.underCamel`）将实体驼峰字段转为下划线列名生成 SQL（`userAccount` → `user_account`），而库表 `user` 列名为驼峰命名（`userAccount`），`user_account` 列不存在 | 改为 `map-underscore-to-camel-case: false`（库表列名与实体属性同名，无需转换），重建同步 `target/classes` 后重启验证（`application.yml`） |
| H-012 | 创建用户接口（`/user/add`）存在多项安全问题：请求中指定的密码被固定弱密码 `123456` 无条件覆盖（管理员指定密码无效）；默认密码硬编码且为弱口令；接口鉴权注解被注释导致任意用户可创建账号 | 密码赋值未判断请求是否已指定密码（`UserAddRequest.userPassword` 经 `BeanUtils` 复制后又被覆盖）；默认密码硬编码在代码中；`@AuthCheck` 处于注释状态 | 改为优先使用请求指定密码、缺省时使用 `application.yml` 配置的初始密码（`${USER_DEFAULT_PASSWORD:ZhPicture@123}`，支持环境变量覆盖，校验 8~16 位）；恢复 `@AuthCheck(mustRole = ADMIN_ROLE)`（`UserController.java`、`application.yml`） |
| H-013 | COS 下载方法编译报错 `Cannot resolve symbol 'COSObjectRequest'`（`CosManager.downloadObject` 无法编译；`FileController` 中另有悬空在类外的下载方法草稿，引用未定义变量 `key` 导致语法错误） | `cos_api:5.6.227` 中不存在 `COSObjectRequest` 类（反编译核验 jar：仅有 `COSObject`、`COSObjectInputStream`、`GetObjectRequest` 等），代码误用了不存在的请求类型 | 改用 SDK 实际提供的重载 `cosClient.getObject(bucket, key)` 获取 `COSObject`，清理无用导入（`CosManager.java`）；移除类外草稿，正式新增测试下载接口 `GET /file/test/download`：管理员鉴权、附件流返回、文件名 URL 编码、流式拷贝、对象不存在返回 40400（`FileController.java`） |
| H-014 | `FileManager` 编译报 `无法将类 CosManager 中的方法 putObject 应用到给定类型`、`找不到符号: 方法 getObject(String,String)` | 字段声明 `private CosManager cosClient` 类型误写：所调 `putObject(PutObjectRequest)`、`getObject(String,String)` 两个重载均属 `COSClient`，`CosManager` 上不存在；错误信息指向 CosManager 的方法而非字段类型，且同包类免导入易误选 | 按委托方案调整：`FileManager` 注入 `CosManager cosManager`，实现 `putObject(String, File)`（以 `file.getPath()` 委托）与 `downloadObject(String)`（直接转发），清理 `COSClient`、`PutObjectRequest` 无用导入（`FileManager.java`） |
| H-015 | 调用 `cosManager.putObject(putObjectRequest)` 编译报 `Expected 2 arguments but found 1`（需要 `String,String`，找到 `PutObjectRequest`） | `CosManager` 仅暴露 `putObject(String, String)` 重载，接收 `PutObjectRequest` 的重载位于其内部封装的 `COSClient`，外部无法直接传入请求对象；Java 编译期按参数类型选择重载，返回类型相同不影响判定 | 在 `CosManager` 中新增 `putObject(PutObjectRequest)` 重载（内部转发 `cosClient.putObject(request)`），调用方可构造请求并设置图片处理等参数后上传（`CosManager.java`） |

---

## 六、错误排查流程

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

## 七、错误日志格式参考

项目使用 Lombok 的 `@Slf4j` 注解输出日志，格式如下：

```
ERROR c.z.z.e.GlobalExceptionHandler - BusinessException
com.zh.zhpicturebackend.exception.BusinessException: 请求参数错误
    at com.zh.zhpicturebackend.service.XxxService.method(XxxService.java:42)
    ...
```

---

*最后更新：2026-09-23*
