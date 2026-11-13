# 智图画廊（zh-picture-backend）修改记录文档

> 本文档记录项目的所有重要变更、功能迭代及代码修改历史。

---

## 版本说明

| 版本号 | 日期 | 说明 |
|--------|------|------|
| v0.0.1 | 2026-08-08 | 项目初始化，完成基础框架搭建 |
| v0.0.2 | 2026-09-18 | 用户模块接口补全、Knife4j 依赖兼容性修复、分页插件接入 |
| v0.0.3 | 2026-09-20 | COS 对象存储配置接入、本地 profile 激活 |

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

## v0.0.2 — 用户模块接口补全与依赖兼容性修复（2026-09-18）

### 新增

- 根据 id 查询用户接口 `GET /user/get`（仅管理员，返回脱敏 `UserVo`）（`UserController.java`）
- 更新用户接口 `POST /user/update`（仅管理员，部分更新：仅更新非 null 字段）（`UserController.java`、`UserUpdateRequest.java`）
- 删除用户接口 `POST /user/delete`（仅管理员，逻辑删除，含禁止删除自己的保护）（`UserController.java`）
- 分页获取用户列表接口 `POST /user/list/page`（仅管理员，支持 id/昵称/账号/角色筛选与排序，返回 `Page<UserVo>`）（`UserController.java`）
- MyBatis-Plus 分页插件配置 `PaginationInnerInterceptor`（MySQL 方言），分页查询才会真正拼接 LIMIT（`MybatisPlusConfig.java` 新建）

### 修改

- `getUserVoList` 空列表语义修正：由抛出 `BusinessException` 改为返回空集合（分页超界、无匹配记录属正常业务场景）（`UserServiceImpl.java`）
- `UserUpdateRequest` 字段与 `User` 实体对齐：`username`→`userName`，移除实体不存在的 `email`/`phone`，补充 `userProfile`（避免 `BeanUtils.copyProperties` 静默忽略导致更新不生效）
- `.feisuan/rules/project_rule.md` 补充用户模块功能基线章节（接口清单 + 10 条设计约定），同步技术栈依赖变更

### 修复

- **Knife4j 与 Spring Boot 3.4.5 双向不兼容（两次 `NoSuchMethodError`）**：官方 `com.github.xiaoymin` 4.5.0 已停更，默认传递的 springdoc 2.3.0 调用 Spring Framework 6.2 已删除的 `ControllerAdviceBean(Object)` 构造函数；显式升级 springdoc 2.8.6 后又触发 knife4j 调用旧签名 `List getGroupConfigs()`。彻底方案：切换社区分支 `com.baizhukui:knife4j-openapi3-jakarta-spring-boot-starter:5.7.4`（内置配套 springdoc 2.8.9）（`pom.xml`）
- **`/user/register` 路由重复映射导致启动失败（Ambiguous mapping）**：`userRegister` 与 `addUser` 均映射 `GET /register`；改为 `POST /register` 与 `POST /add`（`UserController.java`）
- **注册接口权限死锁**：`userRegister` 曾标注 `@AuthCheck(ADMIN_ROLE)`，新用户无管理员角色永远无法注册，移除该注解改为公开接口（`UserController.java`）
- **登录接口 GET + `@RequestBody` 不可用**：GET 请求体无法在浏览器/axios 中发送，改为 `POST /login`（`UserController.java`）
- **分页插件类无法解析**：MyBatis-Plus 3.5.9 起 `PaginationInnerInterceptor` 拆至独立模块，补充 `mybatis-plus-jsqlparser:3.5.12` 依赖（`pom.xml`）

### 配置变更

- `pom.xml`：`com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter:4.5.0` → `com.baizhukui:...:5.7.4`
- `pom.xml`：新增 `com.baomidou:mybatis-plus-jsqlparser:3.5.12`

### 环境问题记录（非代码变更）

- Git 推送 GitHub 失败：全局代理指向未运行的 Clash（127.0.0.1:7890）；已删除全局代理配置，后续视网络情况决定是否恢复
- IDEA 报 "程序包不存在" 但 Maven 编译成功：IDEA 编译缓存与 pom 变更/命令行 clean 不同步所致，Reload Maven + Rebuild Project 解决

---

## v0.0.3 — COS 对象存储配置接入（2026-09-20）

### 新增

- 腾讯云 COS 对象存储配置（`cos.client`：host / secretId / secretKey / region / bucket，附带控制台与密钥获取地址的注释说明）（`application-local.yml`）
- 测试文件下载接口 `GET /file/test/download`（仅管理员；从 COS 下载对象并以附件流返回：文件名 URL 编码防中文乱码、设置 Content-Length、流式拷贝避免大文件占用内存、对象不存在返回 40400）（`FileController.java`、`CosManager.java`）
- 文件管理类上传/下载对象方法：`FileManager.putObject(String key, File file)`（以 `file.getPath()` 委托）与 `FileManager.downloadObject(String key)`（直接转发），内部委托 `CosManager` 完成 COS 操作（`FileManager.java`）（2026-09-23）
- `CosManager` 新增 `putObject(PutObjectRequest)` 重载：内部转发 `cosClient.putObject(request)`，支持调用方构造自定义上传请求（如设置图片处理、元数据等参数）；`FileManager.putObjectCOS` 借此以请求对象方式上传（`CosManager.java`、`FileManager.java`）（2026-09-23）

### 修改

- 激活本地环境配置 `spring.profiles.active: local`，使被 .gitignore 忽略的 `application-local.yml`（存放密钥等敏感信息）真正被 Spring Boot 加载（`application.yml`）
- 文件末尾悬空的“对象存储配置”注释改为指向 `application-local.yml` 的说明，避免误导（`application.yml`）

### 修复

- **启动创建 COS 客户端 Bean 报 `java.lang.IllegalArgumentException: Access key cannot be null.`**：`application-local.yml` 键名 `secretId` 与 `CosClientConfig` 字段 `accessKey` 不一致，`@ConfigurationProperties` 按字段名绑定且对未知键静默忽略，`accessKey` 为 null 传入 `BasicCOSCredentials` 抛异常；且运行期加载的是 `target/classes` 中的构建副本，修改 `src` 下配置后未重建则改动不生效。键名统一为 `accessKey` 后全量重建（`mvnw clean test`）同步并验证启动成功（`application-local.yml`）
- **测试上传接口上传的 COS 对象 key 恒为 `uploads/`（文件名丢失、互相覆盖）**：`String.format("uploads/" ,fileName)` 缺少 `%s` 占位符；临时文件此前以含路径分隔符的 filePath 作前缀。修正为 `String.format("uploads/%s", fileName)`，临时文件改用固定前缀 `File.createTempFile("upload-", null)`（`FileController.java`）
- **用户注册接口报 `Unknown column 'user_account' in 'field list'`**：`map-underscore-to-camel-case: true` 使 MyBatis-Plus 把实体驼峰字段转为下划线列名生成 SQL（`userAccount` → `user_account`），而数据库列名为驼峰命名导致不匹配；改为 `false` 后 MP 直接使用字段名作列名（`application.yml`）
- **创建用户接口（/user/add）安全问题修复**：请求中指定的密码被固定弱密码 `123456` 无条件覆盖（管理员指定密码无效）；默认密码硬编码为弱口令；接口鉴权被注释。修正为：优先使用请求指定密码、缺省时使用 `zh-picture.user.default-password` 配置的初始密码（默认 `ZhPicture@123`，支持环境变量 `USER_DEFAULT_PASSWORD` 覆盖），密码校验 8~16 位；恢复管理员鉴权 `@AuthCheck(mustRole = ADMIN_ROLE)`（`UserController.java`、`application.yml`）
- **COS 下载方法无法编译（`Cannot resolve symbol 'COSObjectRequest'`）**：`cos_api:5.6.227` 不存在 `COSObjectRequest` 类（仅有 `COSObject`、`COSObjectInputStream`、`GetObjectRequest` 等），`CosManager.downloadObject` 误用不存在的请求类；改用 `cosClient.getObject(bucket, key)` 重载获取 `COSObject`，并清理无用导入。同时移除 `FileController` 中悬空在类外、引用未定义变量的下载方法草稿（`CosManager.java`、`FileController.java`）
- **FileManager 编译失败（`putObject(PutObjectRequest)` 无法应用 / `getObject(bucket, key)` 找不到符号）**：字段类型误写为 `CosManager`（字段名 `cosClient`，所调两个重载均属 `COSClient`，同包类免导入导致误选）；按委托方案调整为注入 `CosManager cosManager` 并实现上传/下载方法，清理无用导入（`FileManager.java`）（2026-09-23）

### 配置变更

- `application-local.yml`：写入 COS 配置（当前为占位值，需替换为实际的 SecretId / SecretKey / 存储桶信息）
- `application.yml`：`spring` 节点新增 `profiles.active: local`
- `application-local.yml`：键名 `cos.client.secretId` → `cos.client.accessKey`（与 `CosClientConfig` 字段对齐，修复凭证绑定失败）（2026-09-23）
- `application.yml` → `application-local.yml`：数据库连接配置（`spring.datasource` + hikari 连接池）迁移至本地专属配置，账号密码不再进入 Git 仓库（关联 E-005）（2026-09-23）
- `application.yml`：`mybatis-plus.configuration.map-underscore-to-camel-case`：`true` → `false`（数据库列名为驼峰命名，需关闭下划线转换，修复注册报 Unknown column）（2026-09-23）
- `application.yml`：新增 `zh-picture.user.default-password` 初始密码配置（默认值 `ZhPicture@123`；`${USER_DEFAULT_PASSWORD:...}` 支持环境变量覆盖）（2026-09-23）

### 环境问题记录（非代码变更）

- 终端直接执行 `mvnw` 报“不支持发行版本 21”：系统默认 `JAVA_HOME` 指向 JDK 11，需先设置 `JAVA_HOME=D:\Java\jdk21`；IDEA 内构建运行不受影响
- 清理 git 暂存区误命名残留文件 `application-local.yml.xml`（Spring 配置文件的错误后缀永不会被加载；工作区中该文件已删除，本次仅从暂存区移除）

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

*最后更新：2026-09-23*
