
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
  - `mybatis-plus-spring-boot3-starter`
  - `knife4j-openapi3-jakarta-spring-boot-starter` (API 文档)
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

## 八、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
