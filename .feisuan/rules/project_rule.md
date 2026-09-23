
# 开发规范指南
为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目环境与配置

- **工作目录**：`D:\springboot+ai\smart-cloud-gallery\zh-init-master\zh-picture-backend`
- **代码作者**：Administrator
- **构建工具**：Maven
- **SDK版本**：JDK 21.0.10
- **核心框架**：Spring Boot 3.4.5

## 二、项目目录结构

```text
zh-picture-backend
├── sql
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── zh
    │   │           └── zhpicturebackend
    │   │               ├── common          // 通用类（通用响应、基础类等）
    │   │               ├── config          // 配置类（MyBatis、Swagger、跨域等）
    │   │               ├── constant        // 常量定义
    │   │               ├── controller      // 控制层
    │   │               ├── entity          // (旧)实体类，建议迁移至 model.entity
    │   │               ├── exception       // 自定义异常与全局异常处理
    │   │               ├── mapper          // 数据访问层
    │   │               ├── model
    │   │               │   ├── dto         // 数据传输对象
    │   │               │   ├── entity      // 数据库实体对象
    │   │               │   └── enums       // 枚举类
    │   │               └── service
    │   │                   └── impl        // 业务逻辑实现层
    │   └── resources
    │       ├── mapper          // MyBatis XML 映射文件
    │       ├── static          // 静态资源
    │       └── templates       // 模板文件
    └── test
        └── java
            └── com
                └── zh
                    └── zhpicturebackend
```

## 三、技术栈要求

- **主框架**：Spring Boot 3.4.5
- **语言版本**：Java 21
- **ORM 框架**：MyBatis-Plus 3.5.12 (替代 JPA)
- **核心依赖**：
  - `spring-boot-starter-web`
  - `mybatis-plus-spring-boot3-starter` 3.5.12
  - `mybatis-plus-jsqlparser` 3.5.12（3.5.9 起分页插件 `PaginationInnerInterceptor` 所需，必须单独引入）
  - `com.baizhukui:knife4j-openapi3-jakarta-spring-boot-starter` 5.7.4（社区分支 knife4j-next；官方 com.github.xiaoymin 4.x 已停更，与 Boot 3.4+ 不兼容，勿回退）
  - `hutool-all` (工具类库)
  - `lombok`
  - `spring-boot-starter-aop`

## 四、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 不得直接访问数据库，必须通过 Service 层调用；配合 Knife4j 添加 API 注解 |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 必须通过 Mapper 层访问数据库；返回 DTO 而非 Entity（除非必要） |
| **Mapper**     | 数据库访问与持久化操作             | 继承 `BaseMapper`；复杂查询编写在对应 XML 文件中             |
| **Entity**     | 映射数据库表结构                   | 放置于 `model.entity` 包；不得直接返回给前端                  |

### 接口与实现分离

- 所有接口实现类需放在接口所在包下的 `impl` 子包中。

## 五、安全与性能规范

### 输入校验

- 使用 `@Valid` 与 JSR-303 校验注解（如 `@NotBlank`, `@Size` 等）
  - 注意：Spring Boot 3.x 中校验注解位于 `jakarta.validation.constraints.*`

- 禁止手动拼接 SQL 字符串，防止 SQL 注入攻击。MyBatis-Plus 查询建议使用 `QueryWrapper` 或 `LambdaQueryWrapper`。

### 事务管理

- `@Transactional` 注解仅用于 **Service 层**方法。
- 避免在循环中频繁提交事务，影响性能。

## 六、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |

### 注释规范

- 所有类、方法、字段需添加 **Javadoc** 注释。
- **注释语言**：请使用**中文**进行注释编写，保持团队协作一致性。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| Entity / DO | 数据库实体对象        | `User` / `UserDO`    |
| BO   | 业务逻辑封装对象             | `UserBO`     |
| VO   | 视图展示对象                 | `UserVO`     |
| Query| 查询参数封装对象             | `UserQuery`  |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `AllArgsConstructor`

## 七、扩展性与日志规范

### 接口优先原则

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中（如 `UserServiceImpl`）。

### 日志记录

- 使用 `@Slf4j` 注解代替 `System.out.println`。
- 生产环境建议在 `application.yml` 中关闭 MyBatis-Plus 的 SQL 标准输出 (`log-impl`)，改用日志框架记录。

### API 文档规范

- 使用 Knife4j (OpenAPI 3) 自动生成接口文档。
- 访问地址：`http://localhost:8123/api/doc.html`。
- Controller 方法需添加 `@Tag`、`@Operation` 等注解描述接口信息。

## 八、用户模块功能基线（2026-09-18）

### 接口清单

| 功能 | 方法与路径 | 权限 | 返回值 | 说明 |
|------|-----------|------|--------|------|
| 用户注册 | `POST /user/register` | 公开 | `BaseResponse<Long>` | 返回新用户 id，无需登录 |
| 用户登录 | `POST /user/login` | 公开 | `BaseResponse<LoginUserVo>` | Session 记录登录态 |
| 获取当前用户 | `GET /user/getLoginUser` | 登录用户 | `BaseResponse<LoginUserVo>` | 每次从数据库实时查询 |
| 用户注销 | `GET /user/logout` | 登录用户 | `BaseResponse<Boolean>` | 移除 Session 登录态 |
| 创建用户 | `POST /user/add` | 管理员 | `BaseResponse<Long>` | 默认密码 `123456`（加密存储） |
| 按 id 查询 | `GET /user/get` | 管理员 | `BaseResponse<UserVo>` | 参数 `id`，返回脱敏 VO |
| 更新用户 | `POST /user/update` | 管理员 | `BaseResponse<Boolean>` | 仅更新非 null 字段 |
| 删除用户 | `POST /user/delete` | 管理员 | `BaseResponse<Boolean>` | 逻辑删除，禁止删除自己 |
| 分页列表 | `POST /user/list/page` | 管理员 | `BaseResponse<Page<UserVo>>` | 条件筛选 + 排序分页 |

### 设计约定

- **脱敏返回**：面向前端的查询一律返回 `UserVo` / `LoginUserVo`，禁止直接返回 `User` 实体（含密码字段）。
- **权限校验**：管理接口使用 `@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)`（AOP 切面实现）；注册、登录为公开接口，不得加管理员权限（否则新用户无法注册）。
- **HTTP 方法语义**：`@RequestBody` 接口必须搭配 `@PostMapping`，禁止 GET + RequestBody 组合；简单查询用 GET + 查询参数。
- **路由唯一性**：同一路径不得被多个方法映射（曾因 `/register` 重复映射导致启动失败）。
- **部分更新**：依赖 MyBatis-Plus 默认 `NOT_NULL` 字段策略，DTO 未传的字段不更新。
- **逻辑删除**：`User.isDelete` 标注 `@TableLogic`，`removeById` 实际执行 `UPDATE ... SET isDelete=1`，非物理删除。
- **分页规范**：查询 DTO 继承 `PageRequest`（current/pageSize/sortField/sortOrder）；依赖 `MybatisPlusConfig` 注册的 `PaginationInnerInterceptor` 才会真正拼接 LIMIT，新增分页查询前确认该配置存在。
- **空结果语义**：`getUserVoList` 对空列表返回空集合而非抛异常（分页超界、无匹配记录属正常业务场景）。
- **DTO 字段对齐**：请求 DTO 字段名必须与实体一致（如 `userName` 而非 `username`），否则 `BeanUtils.copyProperties` 静默忽略，导致"操作成功但数据未变"。
- **查询条件构造**：统一经 `UserService.getQueryWrapper(UserQueryRequest)` 动态拼接，条件字段全部可选。

## 九、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
