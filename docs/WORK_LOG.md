# Work Log

## 2026-10-10：上传 GitHub 并审查认证实现（进行中）

Goal:

- 将当前工作目录中的项目上传到 `SJGaNg2313/sjgang`，审查源码并解释认证链路。

Completed:

- 通过 GitHub 插件确认绑定账户、目标仓库和推送权限；目标仓库为公开仓库。
- 确认当前源码及历史提交的配置中存在数据库密码和 JWT 默认密钥；本地 Coze OAuth JSON 含有私钥。
- 新增根目录 `.gitignore`，排除 Coze OAuth 配置、私钥、环境文件、依赖缓存和运行产物。
- 上传采用独立副本，在副本中移除敏感默认值，并从 GitHub 初始化提交建立导入分支；保留本地原始 Git 历史与业务代码。

Pending:

- 完成上传及 PR 创建，执行 Codex 专用代码审查和 Maven 验证。
- 将审查结果、验证限制和后续修复建议记录到本日志。

Related files:

- `.gitignore`、`ai4-boot-verify/src/main/resources/application.properties`、`ai4-boot-verify/src/main/java/com/ai4/auth`。

## 2026-09-28：项目排序与后续 CRM 班级排课

Completed:

- 项目模型、列表、详情和新增逻辑接入 `ai4_project.sort`；已有项目不自动迁移排序值，由管理员手动补齐，新项目自动追加到当前最大值之后。
- 新增 `PUT /projects/order`，要求提交当前有效课包下全部项目 ID 的完整顺序，后端校验缺失、重复和跨课包项目后写入连续排序值。
- 项目列表和 CRM 自动排课改为按 `sort ASC, id ASC`。
- 项目顺序调整只保存项目顺序，不修改已经存在的直播课、考勤、录像和班级课程关联。
- CRM 后续新建班级时，沿用原有建课逻辑，但按最新的 `sort ASC, id ASC` 读取项目并创建直播课。
- 同步更新接口文档、项目决策记录和项目工作日志。

Verification:

- 已回退旧班级课程重排、声网窗口保护和未来课程删除逻辑。
- `git diff --check` 未发现实际空白错误；本次回退后的编译验证待执行。

## 2026-09-28：调整项目领取任务配置

Completed:

- 将任务图上传参数从 `publicTask`、`claimTask` 合并为 `taskImage`。
- 新增任务目标、任务关键词、提交内容图标、提交内容名称和提交内容简介的 DTO、模型、Repository 和 Service 链路。
- 任务关键词按 2 至 5 个、单个 2 至 4 个字符校验，并以 JSON 数组保存。
- 上传接口新增 `submissionIcon`，保留 `companyCard`。
- 更新接口文档、项目上下文和技术决策文档。

Verification:

- 执行 `mvnw.cmd -q -DskipTests compile`，结果成功。

## 2026-10-09：沙盘属性目标值改为非必填

Completed:

- 移除 `SandtableAttributeRequest.target` 的 `@NotNull` 校验，`target` 允许省略或传 `null`。
- 修正沙盘属性修改时的数值比较，兼容目标值为空的新增、修改和回显场景。
- 更新唯一接口文档中沙盘属性 `target` 的请求说明和示例。
- 明确数据库列需要由各环境手动修改为可空，不新增 Flyway 迁移。

Verification:

- 待执行 Maven 编译和测试。
- 数据库新增字段由用户在测试服手动完成；本次未执行数据迁移，也未删除旧图片列。

## 2026-09-23：增加后台本地图形验证码

Completed:

- 新增 `GET /auth/captcha`，本地生成简单字符验证码图片。
- 登录增加验证码校验，验证码答案使用 Redis 保存并一次性消费。
- 新增验证码开关和有效期配置，更新接口文档、项目上下文和技术决策记录。

Verification:

- 执行 `mvnw.cmd -q -DskipTests compile`，结果成功。
- 执行 `mvnw.cmd -q -DskipTests package`，结果成功。
- 已确认验证码接口和登录字段在源码中完成接入；Redis 实例可用后可进行接口联调。

## 2026-09-22：对接 CRM 直播课出站接口

Completed:

- 新增 CRM 出站 `RestClient`、配置和请求 DTO。
- 建课、删课、CRM 停复课后异步同步课包直播课数量。
- 声网课堂结束、云端录制完成后组装学生考勤并回调 CRM。
- 新增回调成功 Redis 标记、录制等待逻辑和最近7天关闭课程补偿扫描。
- 同步 API_DOCUMENTATION、项目上下文和技术决策记录。

Verification:

- 执行 `mvnw.cmd -q -DskipTests compile`，结果成功。
- 还需要配置 CRM 测试基础地址并进行真实 HTTP 联调。

## 2026-09-22：调整题目删除逻辑

Completed:

- 删除题目时先清理题目属性影响和题目选项，再逻辑删除题目主表。
- 禁用题目仍只修改 `status`，不清理选项及属性影响。
- 沙盘和沙盘属性不受题目删除、禁用影响。
- 同步更新题目接口文档和项目决策记录。

Verification:

- 待执行编译验证。

## 2026-09-08：新增学员项目提交文件查询

Completed:

- 新增 `GET /live-courses/{courseId}/students/{studentId}/submission-files`。
- 通过直播课关联确定项目，查询学员在该项目历次提交中的全部文件节点。
- 接口只返回提交时的文件名称，不查询目录、OSS 地址、评分和 Token 用量。
- 文件按提交时间倒序、单次提交快照顺序升序排列，跨提交不去重。
- 已同步更新唯一接口文档 `API_DOCUMENTATION.md`。

Verification:

- 执行 `mvn.cmd -DskipTests compile`，结果 `BUILD SUCCESS`，编译 318 个源文件。
- 已连接本地开发库执行只读 SQL 结构校验；当前库尚未创建 `el_user_founder_roundtable_record` 及相关提交表，因此真实接口查询仍需在表结构同步后验证。

## 2026-08-12：完成字典项管理

Completed:

- 新增字典项列表、详情、创建、完整修改、物理删除五个接口。
- 新增字典项 DTO、Repository、JdbcTemplate 实现、Service、Controller 和错误码。
- 支持名称模糊查询、业务值精确查询、状态筛选、固定排序及同类型业务值唯一校验。
- `extra` 使用 Jackson 3 `JsonNode` 对外接收和返回，数据库使用 JSON 保存。
- 更新唯一接口文档和接口总表。

Verification:

- 执行 `mvnw.cmd -DskipTests package`，结果 `BUILD SUCCESS`，编译 198 个源文件。
- 当前项目没有自动化测试，本次未执行接口级数据库验证。

## 2026-07-16：菜单 CRUD 与授权约束

Completed:

- 实现菜单树、详情、新增、修改和删除接口。
- 新增菜单自动授权给已存在且启用的 `super_admin`。
- 菜单已授权时禁止修改权限码或删除；存在子菜单时禁止删除。
- 更新父菜单时阻止选择自己或后代菜单。
- 记录权限码、禁用恢复、祖先补齐和 BUTTON 授权规则。

## 2026-07-16：服务账户表重命名

Completed:

- 新增 Flyway V6，将 `el_user` 重命名为 `ai4_user`。
- 更新账户 Repository、模型注释、集成测试和接口文档使用新表名。
- 保留 V3 原始迁移不变。

Verification:

- 全局搜索确认旧表名仅存在于 V3 和 V6 的迁移历史中。

## 2026-07-15：接口注释与课程表名前缀迁移

Completed:

- 为课包和课节 Controller 的全部接口补充精简中文注释。
- 新增 Flyway V5，将三张课程表从 `edu2_ai_*` 重命名为 `ai4_*`。
- 更新全部课程 Repository SQL 和集成测试使用新表名。
- 保留 V4 不变，避免破坏已经执行的 Flyway 历史。

Verification:

- 测试库成功执行 V5，当前 schema 版本为 5。
- `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- `BUILD SUCCESS`。

Next step:

- 在服务器核对实际运行 JAR、数据源和 Flyway 历史版本。
- 重新打包部署，启动时让服务器数据库依次执行尚未应用的 V4/V5。
- 确认服务器库最终存在 `ai4_course_package`、`ai4_lesson`、`ai4_lesson_content`。

## 2026-07-13：实现课包与课节业务模块

Current goal:

参考旧管理后台的课包和课节功能，在当前项目中建立正式业务模块，并修正旧实现的假分页、ID 类型混用、排序范围和导入遗漏课程内容等问题。

Completed:

- 新增 Flyway V4，创建课包、课节、课程内容三张表。
- 新增 `com.ai4.course` 下的 DTO、Model、Repository、Service、Controller。
- 完成课包真分页、详情、新增、修改、软删除、选项、所属课节和批量排序接口。
- 完成课节真分页、详情、新增、修改、软删除、批量排序和批量导入接口。
- 批量导入会浅复制课节下所有有效课程内容，并使用事务保证整体一致性。
- 将分页响应从账户模块移动到公共响应包，供多个业务模块复用。
- 新增课程模块集成测试，覆盖分页、详情、内容浅复制和级联软删除。
- 同步更新唯一接口文档 `D:\Dev\Projects\ai4-boot-verify\API_DOCUMENTATION.md`。

Verification:

- `mvnw -DskipTests compile`：BUILD SUCCESS，编译 66 个源码文件。
- Flyway 在测试库成功应用 V4。
- `mvnw test`：Tests run: 8, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。

Next step:

- 启动本地服务，通过 ApiPost 按接口文档验证课包和课节完整链路。
- 确认后将 V4 应用到开发库，再打包部署到服务器，由服务器启动时自动应用 V4。
- 后续根据真实需求实现课程内容新增/修改接口，以及 OSS 图标上传和标签字典。

## 2026-07-13：兼容外部账户旧字段并修正错误状态码

Completed:

- 创建账户请求增加 10 个旧字段别名。
- 保持列表响应使用 `deviceSystemVersion`、`resourceVersion` 等规范字段。
- 增加 `HttpMessageNotReadableException` 处理和 `INVALID_REQUEST` 错误码。
- 测试覆盖旧字段写入、字段映射、旧日期格式和非法日期 HTTP 400。

Next step:

- 与外部服务使用真实 payload 联调。
- 机器身份认证和账户接口级权限控制后续单独推进。

## 2026-07-13：迁移服务账户管理功能

Current goal:

参考旧项目的 `AIElUserController`，在当前 Spring Boot 4.1.0 项目中迁移 `el_user` 新增、分页查询和密码重置功能，同时保持当前分层、统一响应、JWT 与 BCrypt 约定。

Completed:

- 读取并分析参考项目 Controller、Service、VO、Entity、Mapper 和列表 SQL。
- 新增 `V3__create_el_user_table.sql`，兼容既有 `el_user` 结构。
- 新增 `com.ai4.account` 下的 Controller、Service、Repository、模型和 DTO。
- 新增接口：
  - `POST /accounts`
  - `GET /accounts`
  - `PUT /accounts/{userId}/password`
- 新增 `ACCOUNT_ALREADY_EXISTS` 和 `ACCOUNT_NOT_FOUND` 错误码。
- 创建账户时支持外部 `userId`、默认昵称、默认状态和 BCrypt 密码哈希。
- 分页查询支持用户 ID、账号、昵称、手机号、状态、来源和注册日期范围筛选。
- 密码重置只更新有效账户，并继续保存 BCrypt 哈希。
- 增加账户管理最小集成测试。

Verification:

- `mvn -DskipTests compile`：`BUILD SUCCESS`，42 个源文件编译通过。
- `$env:AI4_TEST_DB_PASSWORD='2584'; mvn test`：`BUILD SUCCESS`。
- `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- 测试库 Flyway schema 已从 V2 升级到 V3。

Next step:

- 本地开发库启动应用并应用 V3 后，使用 ApiPost 验证三个账户接口。
- 确认外部服务向当前项目传输账户数据时采用 JWT、API Key、请求签名还是机器 Token。
- 后续增加 `account:list`、`account:create`、`account:reset-password` 的后端接口级授权。
- 部署前让服务器数据库执行 V3，并备份旧 jar 与数据库。

## 2026-07-10: 开始实现 `/auth/profile` 真实角色权限读取

Current goal:

让 `/auth/profile` 从当前“只返回 JWT 中的用户身份 + 空角色权限列表”，推进为“返回当前用户真实角色和权限码”。

Completed:

- 检查现有 `/auth/profile` 实现，确认当前链路为：
  - 请求携带 `Authorization: Bearer <token>`。
  - `JwtAuthenticationInterceptor` 在 Controller 前校验 JWT。
  - JWT 解析成功后生成 `CurrentUser`。
  - `CurrentUser` 通过 request attribute 传给 `AuthController.profile(...)`。
  - Controller 当前返回 `userId`、`userName`，但 `roles` 和 `permissions` 仍是空列表。
- 梳理出当前缺失环节：需要根据 `currentUser.userId()` 查询 RBAC 表，得到真实角色编码和权限码。
- 已指导新增 `AuthPermissionRepository`，用于定义认证模块查询角色和权限的能力。
- 明确现阶段使用 `JdbcTemplate + SQL` 查询 RBAC 数据，不急于引入 MyBatis/JPA。

Changed files:

- 用户已新增：`src/main/java/com/ai4/auth/repository/AuthPermissionRepository.java`
- 本次文档同步修改：
  - `docs/PROJECT_CONTEXT.md`
  - `docs/DECISIONS.md`
  - `docs/WORK_LOG.md`

Verification:

- 本阶段主要是代码阅读、接口设计和文档同步，尚未运行 Maven 测试。
- `/auth/profile` 真实角色权限查询尚未接入完成。

Next step:

- 新增 `JdbcAuthPermissionRepository`，使用 SQL 查询：
  - 用户角色：`sys_user_role -> sys_role`
  - 用户权限：`sys_user_role -> sys_role -> sys_role_menu -> sys_menu`
- 改造 `AuthController.profile(...)`，将 `List.of()` 替换为真实查询结果。
- 准备最小 RBAC 测试数据，用 ApiPost 验证 `roles` 和 `permissions` 能返回真实值。
## 2026-07-10: 完成用户表切换与 Flyway V2 RBAC 表同步

Current goal:

将手动设计的后台管理 RBAC 表结构纳入 Flyway 版本管理，并让用户创建、登录链路从旧 `users` 表切换到新的 `sys_user` 表。

Completed:

- 确认当前最小 RBAC 表结构：
  - `sys_user`
  - `sys_role`
  - `sys_menu`
  - `sys_user_role`
  - `sys_role_menu`
- 新增 `V2__create_rbac_tables.sql`。
- 删除测试库中手动创建的 5 张 `sys_*` 表后，由 Flyway 自动重建。
- 本地测试库、本地开发库和服务器库均已同步存在 5 张 `sys_*` 表。
- 用户创建链路已切换为使用 `user_name` 和 `password`，不再使用 `email`。
- 用户响应已调整为返回 `user_id` 和 `user_name`，不返回 `password`。
- `JdbcUserRepository` 已改为操作 `sys_user`。
- 测试清理逻辑已改为清理 `sys_user`。
- 处理并修复了迁移过程中的问题：
  - `username` 与 `user_name` 字段不一致
  - `status` 字段缺少默认值
  - 测试断言仍读取旧响应字段 `username`

Changed files:

- `src/main/resources/db/migration/V2__create_rbac_tables.sql`
- `src/main/java/com/ai4/user/dto/CreateUserRequest.java`
- `src/main/java/com/ai4/user/dto/CreateUserResponse.java`
- `src/main/java/com/ai4/user/model/User.java`
- `src/main/java/com/ai4/user/repository/UserRepository.java`
- `src/main/java/com/ai4/user/repository/JdbcUserRepository.java`
- `src/main/java/com/ai4/user/service/UserService.java`
- `src/main/java/com/ai4/auth/service/AuthService.java`
- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`

Verification:

- 本地测试库连接使用 `AI4_TEST_DB_PASSWORD` 环境变量。
- Maven 测试已通过：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- 服务器重新部署后，服务器库也已确认存在 5 张 `sys_*` 表。

Next step:

- 开始实现真实 RBAC 数据读取链路。
- 优先让 `/auth/profile` 返回真实角色和权限。
- 后续再实现 `/auth/routes` 动态路由接口。

## 2026-07-09: 完成认证收尾、部署、测试隔离和 Flyway 接入

Completed:

- 完成 JWT MVC 拦截器并注册到 Spring MVC。
- `/auth/profile` 改为读取拦截器保存的 `CurrentUser`。
- 认证失败统一返回 HTTP 401。
- 清理无效的内存 Repository、无用依赖和过时注释。
- 新版本部署到开发服务器，并通过 HTTPS 域名验证用户创建、登录和 profile。
- 创建独立 MySQL 测试库 `ai4_boot_verify_test` 和测试用户 `ai4_test`。
- 使用 `@ActiveProfiles("test")` 和 `application-test.properties` 隔离测试数据源。
- 测试密码改由 `AI4_TEST_DB_PASSWORD` 环境变量提供。
- 测试清库前增加数据库名称保护，拒绝清理非测试数据库。
- 统一测试库、开发库和服务器库的 users 表结构、字符集、索引名和字段注释。
- 引入 Flyway Starter 和 MySQL 模块。
- 创建 `V1__create_users_table.sql`。
- 三套已有数据库完成 Flyway baseline。
- 关闭 `baseline-on-migrate`，保留迁移历史校验。

Verification:

- 最新测试结果：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- 开发服务器部署及 HTTPS 接口验证通过。
- 测试库、开发库和服务器库均已建立 Flyway 迁移历史。

Next step:

- 删除已无必要的 `spring.flyway.baseline-version=1`。
- 设计角色、权限、菜单及关联表。
- 使用 V2 迁移创建权限模型表。
- 完善 `/auth/profile` 的真实角色和权限数据。
- 实现 `/auth/routes`。

## 2026-07-09: 完成 MySQL 测试数据库隔离

Completed:

- 创建独立测试数据库 `ai4_boot_verify_test`。
- 创建测试专用用户 `ai4_test`。
- 新增 `src/test/resources/application-test.properties`。
- 测试类启用 `@ActiveProfiles("test")`。
- 使用 `AI4_TEST_DB_PASSWORD` 提供测试密码。
- 修正测试库 `users.id` 缺少 `AUTO_INCREMENT` 导致的插入失败。

Verification:

- 最新 Surefire 报告：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- 测试数据库隔离已验证可用。

Next step:

给测试清库逻辑增加数据库名称保护，然后确认现有开发库和服务器库的 `SHOW CREATE TABLE users`，为引入 Flyway 做准备。

## 2026-07-09: 部署 JWT 拦截器版本并完成线上验证

Completed:

- 将当前 `ai4-admin-backend` 新版本部署到开发服务器。
- 后端继续通过 systemd 后台运行。
- 通过 HTTPS 域名使用 ApiPost 验证接口。
- 用户确认当前已测试接口均正常。

Verified endpoints:

- `POST https://ai-manage-dev.xxaisys.com/ai/v4/users`
- `POST https://ai-manage-dev.xxaisys.com/ai/v4/auth/login`
- `GET https://ai-manage-dev.xxaisys.com/ai/v4/auth/profile`

Current conclusion:

- Nginx、HTTPS、systemd、MySQL、BCrypt、JWT 和 MVC 拦截器组成的开发环境链路可用。
- 下一阶段优先处理测试数据库隔离和数据库迁移，再开始角色、权限、菜单模型。

## 2026-07-09: 全项目审查与 JWT 拦截器收尾

Completed:

- 检查 Maven 配置、应用配置、全部 Java 源码、测试和项目记忆文档。
- 清理 `AuthController` 中不再使用的 `JwtService` 构造参数和 import。
- 删除已被 JDBC 实现替代的 `InMemoryUserRepository`。
- 重写并精简 `JdbcUserRepository` 注释，不改变 SQL 行为。
- 修正 `encodedPassword` 命名和少量格式问题。
- 确认 JWT 拦截器已注册，`/auth/profile` 已读取 request 中的 `CurrentUser`。

Verification:

- 已执行 `.\mvnw.cmd test`。
- `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`
- 注意：该测试使用默认 MySQL，并执行 `delete from users`，因此本次测试也会清空当前默认数据库的用户数据。

Open risks:

- 测试数据库未隔离。
- `/users` 当前公开且公网可调用。
- 数据库密码和开发 JWT secret 存在默认值。
- 没有数据库迁移脚本，服务器表结构依赖人工维护。
- 没有用户状态、角色、权限和菜单数据模型。
- 没有 CORS/OPTIONS 正式配置。

## 2026-07-09: 完善 JWT 认证异常并整理源码注释

Current goal:

让未登录、非法 Token 和过期 Token 使用统一 HTTP 401 响应，并按当前真实实现补齐精简中文注释。

Completed:

- 新增 `UnauthorizedException`，用于区分认证失败和普通业务失败。
- `GlobalExceptionHandler` 新增 HTTP 401 认证异常处理。
- `AuthController` 将缺少或错误的 Bearer Token 转换为 `UNAUTHORIZED`。
- `JwtService` 将非法 Token 和过期 Token 分别转换为 `INVALID_TOKEN` 和 `TOKEN_EXPIRED`。
- 为 JWT、认证 DTO、统一响应、用户模型、Repository 和测试补充精简中文注释。
- 修正“mock token”“尚未接数据库”“使用内存 Repository”等过时注释。
- 未修改接口路径、响应结构和业务执行逻辑。

Verification:

- 已执行 `.\mvnw.cmd test`。
- `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

Next step:

使用 ApiPost 验证 `/auth/profile` 的正确 Token、缺少 Token、非法 Token 和过期 Token 场景，再为 JWT 认证链路补充自动化测试。

## 2026-07-08: 调整 Maven 项目名为正式后台后端命名

Current goal:

在继续 JWT 实现前，先把项目从早期验证阶段名称 `ai4-boot-verify` 调整为更适合正式后台管理后端的 Maven 项目名。

Completed:

- 将 `pom.xml` 中的 Maven `artifactId` 改为 `ai4-admin-backend`。
- 将 `pom.xml` 中的 Maven `name` 改为 `ai4-admin-backend`。
- 将 `pom.xml` 中的 Maven `description` 改为 `AI4 admin backend service`。
- 将 `application.properties` 中的 `spring.application.name` 改为 `ai4-admin-backend`。
- 同步更新项目记忆文档中的当前项目命名和决策记录。
- 明确本次不修改 Java 包名、不移动启动类、不调整接口路径。

Changed files:

- `pom.xml`
- `src/main/resources/application.properties`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- Maven 输出显示当前项目坐标为 `com.ai4:ai4-admin-backend`。
- Spring Boot 测试日志中的应用名已显示为 `[ai4-admin-backend]`。
- 已使用 `rg "ai4-boot-verify"` 搜索，未发现普通文本内容中继续引用旧项目名。
- 测试结果：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

Next step:

继续 JWT 最小闭环实现：在已经完成 JWT 依赖和 `JwtProperties` 配置的基础上，下一步新增 `CurrentUser` 与 `JwtService`。

## 2026-07-06: 同步 HTTPS 完成状态与 JWT 配置风险清单

Current goal:

在正式开始 JWT 实现前，把当前已完成的 HTTPS 配置、证书路径、HTTPS 验证结果，以及后续 JWT 需要重点规避的问题写入项目记忆文档。

Completed:

- 记录 HTTPS 当前状态：
  - 域名：`ai-manage-dev.xxaisys.com`
  - HTTPS 已配置完成
  - ApiPost 已通过 HTTPS 成功创建用户
  - 证书链路径：`/etc/letsencrypt/live/ai-manage-dev.xxaisys.com/fullchain.pem`
  - 私钥路径：`/etc/letsencrypt/live/ai-manage-dev.xxaisys.com/privkey.pem`
  - 证书到期时间：`2026-10-04`
  - Certbot 已配置自动续期
- 记录 JWT 后续实施风险：
  - token 保存位置不一致
  - `Authorization: Bearer <token>` 格式错误
  - token 过期处理缺失
  - JWT secret 不稳定或多实例不一致
  - 拦截器误拦截登录/注册接口
  - `/ai/v4` context-path 与业务路径匹配混乱
  - CORS 未放行 `Authorization`
  - JWT 错误没有走统一 `ApiResponse`
  - 多设备登录策略未约定
  - token 中误放敏感信息
- 明确第一版 JWT 只做最小闭环：
  - 登录生成真实 access token
  - 新增 `GET /auth/me`
  - 校验 token 后返回当前用户
  - 不做 refresh token、黑名单、多端管理和角色权限

Changed files:

- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 本次只更新 Markdown 文档，未修改 Java 代码，未运行 Maven 测试。

Next step:

正式配置 JWT 前，先确认前后端约定：

1. token 字段名继续使用 `token`。
2. 请求头统一使用 `Authorization: Bearer <token>`。
3. token 有效期暂定 7200 秒。
4. 过期后前端清 token 并跳转登录页。
5. 第一版是否允许多设备同时登录。

## 2026-07-03: 接入 BCrypt 并确认域名访问

Current goal:

在域名访问已经通过后，把用户密码从 `{noop}` 占位实现替换为 BCrypt 哈希，并确认现有创建用户、登录接口不受影响。

Completed:

- 确认域名解析：
  - `ai-manage-dev.xxaisys.com -> 39.106.177.254`
- 确认 Nginx 代理配置原则：
  - `proxy_pass http://127.0.0.1:8080`
  - 不在 `proxy_pass` 后拼接 `/ai/v4`
- 用户已通过 ApiPost 验证域名接口访问正常。
- 在 `pom.xml` 中新增：
  - `org.springframework.security:spring-security-crypto`
- 修改 `PasswordEncoder`：
  - `encode(rawPassword)` 改为生成 BCrypt 哈希。
  - `matches(rawPassword, encodedPassword)` 改为使用 BCrypt 校验。
- 保持 `UserService` 和 `AuthService` 调用方式不变。

Changed files:

- `pom.xml`
- `src/main/java/com/ai4/user/security/PasswordEncoder.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

Current state:

- 本地代码已使用 BCrypt。
- 新创建用户的数据库密码不再是 `{noop}123456`，而应是 BCrypt 哈希。
- 旧的 `{noop}` 测试用户无法再用当前登录逻辑登录。

Next step:

下一步建议：

1. 清空开发库旧的 `{noop}` 测试用户。
2. 重新通过域名创建一个新用户。
3. 在数据库确认 `users.password` 已变为 BCrypt 哈希。
4. 重新打包、上传服务器并重启 `ai4-backend`。
5. 再用域名验证创建用户和登录接口。

## 2026-07-03: 删除临时验证接口

Current goal:

清理已经完成使命的临时 `/verify/*` 验证接口，避免部署后继续暴露开发验证入口，并把自动化测试收敛到正式业务接口。

Completed:

- 删除数据库临时验证接口：
  - `src/main/java/com/ai4/verify/database/DatabaseVerifyController.java`
- 删除参数校验临时验证接口：
  - `src/main/java/com/ai4/verify/validation/ValidationController.java`
  - `src/main/java/com/ai4/verify/validation/ValidationRequest.java`
- 重写测试文件，移除 3 个 `/ai/v4/verify/validation` 相关测试。
- 保留正式业务接口测试：
  - 创建用户成功
  - 创建用户参数校验失败
  - 重复用户名业务异常
  - 登录成功
  - 登录密码错误

Changed files:

- `src/main/java/com/ai4/verify/database/DatabaseVerifyController.java`
- `src/main/java/com/ai4/verify/validation/ValidationController.java`
- `src/main/java/com/ai4/verify/validation/ValidationRequest.java`
- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已搜索确认代码中没有残留：
  - `verify/database`
  - `verify/validation`
  - `ValidationController`
  - `ValidationRequest`
  - `DatabaseVerifyController`
- 已执行 `.\mvnw.cmd test`。
- 结果：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

Current state:

- 临时验证接口已从本地代码删除。
- 当前测试通过正式业务接口继续覆盖主要链路。
- 服务器当前如果仍运行旧 jar，需要重新部署新 jar 后才会移除公网临时接口。

Next step:

下一步建议把当前 `{noop}` 密码占位实现替换为 BCrypt。替换前需要明确：已有数据库中的 `{noop}` 密码数据不能直接被 BCrypt 校验，需要清空开发测试数据或做临时兼容。

## 2026-07-03: 配置后端后台运行与公网 Nginx 访问

Current goal:

把后端从前台验证运行推进到可长期运行的服务器部署方式，并完成公网接口访问验证。

Completed:

- 配置 systemd 后台服务：
  - 服务名：`ai4-backend`
  - 后端 jar：`/opt/ai4/backend/app.jar`
  - 工作目录：`/opt/ai4/backend`
  - 数据库环境变量：`AI4_DB_URL`、`AI4_DB_USERNAME`、`AI4_DB_PASSWORD`
- 配置后端日志目录：
  - `/opt/ai4/logs`
- 配置 Nginx 反向代理：
  - 公网 80 端口
  - 转发到本机 Spring Boot `127.0.0.1:8080`
  - 保留统一接口前缀 `/ai/v4`
- 阿里云安全组已开放 80 端口。
- 本地 PowerShell 和 ApiPost 已验证公网接口可访问。

Changed files:

- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 用户确认本地 PowerShell 和 ApiPost 均可正常访问服务器接口。
- 当前公网访问地址示例：
  - `http://39.106.177.254/ai/v4/verify/database`
  - `http://39.106.177.254/ai/v4/users`
  - `http://39.106.177.254/ai/v4/auth/login`

Current state:

- 后端已具备后台运行能力。
- Nginx 已作为公网入口。
- Spring Boot 8080 不需要直接对公网开放。
- MySQL 3306 不需要直接对公网开放，Navicat 继续建议走 SSH 隧道。

Next step:

后续部署方向建议：

1. 绑定域名。
2. 配置 HTTPS。
3. 增加正式健康检查接口或使用 Actuator 健康检查。
4. 替换 mock token 为 JWT。
5. 替换 `{noop}` 密码处理为 BCrypt。
6. 清理或限制临时验证接口。

## 2026-07-02: 服务器前台部署验证通过

Current goal:

完成后端 jar 在阿里云 ECS 上的第一次前台启动验证，并确认服务器后端可以连接服务器 MySQL。

Completed:

- 使用服务器目录规划：
  - `/opt/ai4/backend`
  - `/opt/ai4/frontend`
  - `/opt/ai4/backup`
  - `/opt/ai4/logs`
- 将本地打包产物上传为：
  - `/opt/ai4/backend/app.jar`
- 在服务器安装并准备 MySQL。
- 在服务器 MySQL 中创建：
  - `ai4_boot_verify` 数据库
  - `users` 表
  - `ai4_app` 后端专用数据库用户
- 使用环境变量前台启动后端：
  - `AI4_DB_URL`
  - `AI4_DB_USERNAME`
  - `AI4_DB_PASSWORD`
- 通过服务器本机 curl 完成接口验证。

Changed files:

- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`
- `D:\Dev\Projects\ai4-boot-verify\SERVER_PROGRESS.md`

Verification:

- 用户确认以下部署验证步骤全部通过：
  - MySQL 可用
  - 数据库和表已创建
  - 后端服务前台启动成功
  - `GET /ai/v4/verify/database` 通过
  - `POST /ai/v4/users` 通过
  - `POST /ai/v4/auth/login` 通过

Current state:

- 当前后端已经能在服务器以前台方式运行。
- 当前服务关闭 SSH 前台窗口或按 `Ctrl + C` 会停止。
- 当前还没有配置 systemd 后台运行和开机自启。

Next step:

下一步配置 systemd 服务：

1. 新建 `/etc/systemd/system/ai4-backend.service`。
2. 把 jar 路径、工作目录和数据库环境变量写入 service。
3. 使用 `systemctl start/status/restart ai4-backend` 管理服务。
4. 验证服务后台运行。
5. 再考虑公网端口、安全组和 Nginx。

## 2026-07-02: 补充登录接口自动化测试

Current goal:

把 ApiPost 已手动验证通过的登录成功和密码错误场景补进自动化测试，减少后续手动回归成本。

Completed:

- 在 `Ai4BootVerifyApplicationTests.java` 新增登录成功测试：
  - `loginWithCorrectPasswordReturnsSuccessResponse`
- 在 `Ai4BootVerifyApplicationTests.java` 新增密码错误测试：
  - `loginWithWrongPasswordReturnsBusinessErrorResponse`
- 两个测试都先通过 `POST /ai/v4/users` 创建用户，再调用 `POST /ai/v4/auth/login`。
- 在测试代码中增加中文注释，说明为什么先创建用户，以及测试覆盖了哪条链路。

Changed files:

- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：
  - `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

Current state:

- 用户创建接口已有自动化测试。
- 登录成功接口已有自动化测试。
- 登录密码错误已有自动化测试。
- 当前项目已经具备“请求 -> Service -> Repository -> MySQL -> 统一响应”的最小模板。

Next step:

可以开始部署前准备，但建议按顺序做：

1. 先梳理部署目标：服务器 JDK、MySQL、端口、防火墙、环境变量。
2. 本地先打包：`.\mvnw.cmd clean package`。
3. 再上传 jar 到服务器。
4. 在服务器配置数据库连接环境变量。
5. 用 `java -jar` 做第一次前台启动验证。
6. 验证 `/ai/v4/...` 接口后，再配置后台运行或 systemd 服务。

## 2026-07-02: 修复登录错误码引用并重新验证

Current goal:

确认登录错误码拼写问题是否已修复，并把最新验证状态同步到项目记忆文档。

Completed:

- 检查 `AuthService.java`：
  - 当前已使用 `ErrorCode.INVALID_USERNAME_OR_PASSWORD`
  - 两处旧的 `INVALID_USRENAME_OR_PASSWORD` 已不存在
- 检查 `Ai4BootVerifyApplicationTests.java`：
  - 当前仍为 7 个自动化测试
  - 尚未包含登录成功和密码错误的 MockMvc 自动化测试
- 重新执行 `.\mvnw.cmd test`。
- 记录用户通过 ApiPost 完成的手动验证：
  - 登录成功通过
  - 密码错误通过

Changed files:

- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：
  - `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`
- 手动验证：
  - 用户确认 ApiPost 登录成功场景通过
  - 用户确认 ApiPost 密码错误场景通过

Current state:

- 登录错误码拼写问题已修复。
- 现有自动化测试全部通过。
- 登录接口完成了最小手动验证。
- 登录接口还缺少自动化测试保护。

Next step:

下一步建议补两个登录接口 MockMvc 测试：

1. 登录成功返回 `code = "0"`、`message = "success"`、`data.token` 存在。
2. 密码错误返回 HTTP 400、`code = "INVALID_USERNAME_OR_PASSWORD"`。

## 2026-07-02: 同步数据库接入与登录链路当前状态

Current goal:

把当前真实代码状态同步到项目记忆文档，避免只依据聊天记忆继续推进。

Completed:

- 读取 `AGENTS.md`、`docs/PROJECT_CONTEXT.md`、`docs/DECISIONS.md`、`docs/WORK_LOG.md`。
- 检查当前登录相关代码：
  - `AuthController`
  - `AuthService`
  - `LoginRequest`
  - `LoginResponse`
  - `ErrorCode`
- 检查当前用户数据访问相关代码：
  - `UserRepository`
  - `JdbcUserRepository`
  - `InMemoryUserRepository`
- 检查当前测试：
  - `Ai4BootVerifyApplicationTests`
  - 已存在 `@BeforeEach cleanDatabase()`，每个测试前清理 `users` 表。
- 执行 `.\mvnw.cmd test` 验证当前真实状态。

Changed files:

- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 当前结果：`BUILD FAILURE`。
- 失败原因：`AuthService.java` 引用了不存在的 `ErrorCode.INVALID_USRENAME_OR_PASSWORD`。
- `ErrorCode.java` 中当前真实错误码为 `INVALID_USERNAME_OR_PASSWORD`。

Current state:

- MySQL/JDBC 用户 Repository 已接入。
- 用户创建接口已走真实数据库。
- 登录接口代码已开始搭建。
- 当前仓库尚未编译通过，需要先修复错误码拼写。
- 当前测试文件尚未包含登录成功和密码错误两个 MockMvc 测试。

Next step:

1. 修复 `AuthService` 中两处错误码拼写：
   - 从 `INVALID_USRENAME_OR_PASSWORD`
   - 改为 `INVALID_USERNAME_OR_PASSWORD`
2. 重新执行 `.\mvnw.cmd test`。
3. 编译通过后，补充登录成功测试。
4. 再补充密码错误测试。
5. 后续再考虑正式 JWT、BCrypt、部署和测试数据库隔离增强。

## 2026-07-01: 对齐接口契约并抽出 UserRepository 接口

Current goal:

在接入真实数据库前，先把当前用户创建链路的数据访问边界固定下来，并同步修正统一响应与字段校验结构，使当前代码更贴近前后端共同接口契约。

Completed:

- 修正统一成功响应：
  - `ApiResponse.success(data)` 返回 `code = "0"`。
  - `ApiResponse.success(data)` 返回 `message = "success"`。
- 修正字段校验错误项：
  - `FieldErrorItem` 从 `message` 字段改为 `reason` 字段。
- 新增用户仓储接口：
  - `src/main/java/com/ai4/user/repository/UserRepository.java`
- 修改内存仓储实现：
  - `InMemoryUserRepository implements UserRepository`
- 修改业务层依赖：
  - `UserService` 从依赖 `InMemoryUserRepository` 改为依赖 `UserRepository`。
- 明确当前 Java 状态：
  - IDEA 项目 JDK 按用户确认配置为 21。
  - 本次命令行 Maven 测试输出显示实际使用 `Java 17.0.12`。
  - `pom.xml` 语言版本仍为 17。

Changed files:

- `src/main/java/com/ai4/common/response/ApiResponse.java`
- `src/main/java/com/ai4/common/response/FieldErrorItem.java`
- `src/main/java/com/ai4/user/repository/UserRepository.java`
- `src/main/java/com/ai4/user/repository/InMemoryUserRepository.java`
- `src/main/java/com/ai4/user/service/UserService.java`
- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- 结果：`BUILD SUCCESS`。

What was learned:

- 接口表示“业务层需要什么能力”，实现类表示“这些能力现在怎么完成”。
- 当前 `UserRepository` 是用户数据访问边界。
- 内存实现只是临时实现，后续可以替换为 MySQL / MyBatis 实现。
- Service 依赖接口后，后续替换数据来源时对业务层影响更小。

Next step:

下一步可以开始数据库接入前的第一步：选择并加入 MySQL / MyBatis / Flyway 依赖，设计最小用户表，并决定测试环境如何隔离真实数据库数据。

## 2026-07-01: 引入最小 PasswordEncoder

Current goal:

在不引入完整安全框架的前提下，先为用户创建链路加入密码处理边界，避免原始密码直接从 Request 流入 Repository。

Completed:

- 新增 `src/main/java/com/ai4/user/security/PasswordEncoder.java`。
- `PasswordEncoder` 当前使用 `{noop}` 前缀作为最小占位处理。
- 修改 `UserService`：
  - 注入 `PasswordEncoder`。
  - 创建用户时先调用 `passwordEncoder.encode(request.password())`。
  - 保存用户时传入处理后的密码。
- 未新增单元测试；当前阶段只记录分层调整，外部接口契约不变。

Changed files:

- `src/main/java/com/ai4/user/security/PasswordEncoder.java`
- `src/main/java/com/ai4/user/service/UserService.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 用户确认该阶段代码已完成。
- 当前未在本次文档同步后重新执行测试；建议后续继续执行 `.\mvnw.cmd test` 确认 7 个 MockMvc 测试仍通过。

What was learned:

- 密码可以从 Request DTO 进入后端，但不应原样保存。
- Service 负责协调密码处理和数据保存。
- 独立 `PasswordEncoder` 让后续替换正式加密方案更容易。

Next step:

后续可以把 `{noop}` 占位实现替换为 BCrypt；也可以先继续推进数据库接入前的结构整理。

## 2026-07-01: 引入内存 Repository

Current goal:

在用户模块中加入最小数据访问层，用内存 Repository 暂时代替数据库，建立 `Controller -> Service -> Repository` 的完整分层。

Completed:

- 新增 `User` 领域对象，用于表示当前内存中保存的用户数据。
- 新增 `InMemoryUserRepository`：
  - 使用 `ConcurrentHashMap` 保存用户。
  - 使用 `AtomicLong` 生成内存 ID。
  - 提供 `existsByUsername` 和 `save` 方法。
- 修改 `UserService`：
  - 注入 `InMemoryUserRepository`。
  - 通过 Repository 判断用户名是否已存在。
  - 通过 Repository 保存新用户。
  - 返回不包含 `password` 的 `CreateUserResponse`。
- 修改重复用户名测试：
  - 不再依赖 `username = existing` 的硬编码模拟。
  - 改为在同一个测试中先创建 `duplicate`，再重复创建 `duplicate`。

Changed files:

- `src/main/java/com/ai4/user/model/User.java`
- `src/main/java/com/ai4/user/repository/InMemoryUserRepository.java`
- `src/main/java/com/ai4/user/service/UserService.java`
- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- 结果：`BUILD SUCCESS`。

What was learned:

- Repository 负责数据访问，当前用内存 Map 模拟数据库。
- Service 负责业务规则，例如用户名不能重复。
- Controller 不直接关心数据如何保存。
- 内存 Repository 是过渡方案，数据不会持久化到磁盘或数据库。

Next step:

下一步可以继续做密码处理的最小过渡方案，例如先引入 `PasswordEncoder` 风格的本地组件，或者继续推进真正数据库接入前的接口和分层整理。

## 2026-07-01: 明确并加固项目 UTF-8 编码约定

Current goal:

处理中文显示乱码问题，确认是文件内容损坏还是终端显示编码问题，并为项目增加长期编码约束。

Completed:

- 检查 Java 源文件中的中文内容。
- 使用字符码点确认 `UserService.java`、`CreateUserRequest.java` 中的中文实际是正常 UTF-8 内容。
- 确认当前乱码主要来自 PowerShell/工具输出显示层，而不是这些 Java 文件本身已经损坏。
- 新增 `.editorconfig`，统一约束项目文本文件编码为 UTF-8。
- 在 `pom.xml` 中新增：
  - `project.build.sourceEncoding=UTF-8`
  - `project.reporting.outputEncoding=UTF-8`

Changed files:

- `.editorconfig`
- `pom.xml`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- 结果：`BUILD SUCCESS`。

Next step:

如果 IDE 中仍显示中文乱码，需要在 IDE 中确认文件编码为 UTF-8；如果只是 PowerShell 输出乱码，可以在当前终端会话中尝试 `chcp 65001` 或设置输出编码。

## 2026-07-01: 引入 UserService 并验证业务异常链路

Current goal:

在用户模块中引入最小 Service 层，让 Controller 与业务处理分离，并验证业务异常可以通过全局异常处理统一返回。

Completed:

- 新增 `src/main/java/com/ai4/user/service/UserService.java`。
- 给 `UserService` 添加业务含义注释。
- 将 `UserController` 改为通过构造方法注入 `UserService`。
- 将 `UserController.createUser` 中的响应对象组装逻辑移动到 `UserService.createUser`。
- 在 `UserService.createUser` 中增加最小业务异常模拟：
  - 当 `request.username()` 等于 `existing` 时，抛出 `BizException(ErrorCode.USERNAME_ALREADY_EXISTS)`。
- 在 MockMvc 测试中新增业务异常测试：
  - `POST /ai/v4/users`
  - `username = existing`
  - 预期 HTTP 400
  - 预期 `code = USERNAME_ALREADY_EXISTS`
  - 预期 `message = 用户名已存在`

Changed files:

- `src/main/java/com/ai4/user/service/UserService.java`
- `src/main/java/com/ai4/user/controller/UserController.java`
- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- 结果：`BUILD SUCCESS`。

What was learned:

- `@Service` 表示业务层组件，Spring 会把它注册成 Bean。
- Controller 可以通过构造方法注入 Service。
- Controller 负责 HTTP 层，Service 负责业务层。
- `BizException` 可以从 Service 层抛出，再由 `GlobalExceptionHandler` 统一转换为 `ApiResponse`。

Next step:

下一步可以继续做用户模块的最小持久化准备：先设计 User 领域对象或内存 Repository，再逐步过渡到数据库；也可以先清理中文编码显示问题，避免注释和测试文案继续乱码。

## 2026-06-30: 用户创建接口最小闭环完成

Current goal:

完成 `POST /ai/v4/users` 的最小接口闭环：接收创建用户请求、触发参数校验、返回统一响应，并避免响应中包含 `password`。

Completed:

- 新增 `CreateUserResponse`，作为创建用户成功后的响应 DTO。
- 修改 `UserController.createUser`：
  - 入参仍使用 `CreateUserRequest`。
  - 返回类型改为 `ApiResponse<CreateUserResponse>`。
  - 响应对象只包含 `username` 和 `email`。
- 给 `POST /ai/v4/users` 补充 MockMvc 成功测试。
- 给 `POST /ai/v4/users` 补充缺少 `username` 的校验失败测试。
- 修正用户创建成功测试，断言响应中不存在 `password`。

Changed files:

- `src/main/java/com/ai4/user/dto/CreateUserResponse.java`
- `src/main/java/com/ai4/user/controller/UserController.java`
- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`
- `D:\Dev\Projects\ai4-boot-verify\SERVER_PROGRESS.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- 结果：`BUILD SUCCESS`。

What was learned:

- `CreateUserRequest` 表示前端传入的数据，允许包含 `password`。
- `CreateUserResponse` 表示后端返回给前端的数据，不应包含 `password`。
- Controller 可以把 Request DTO 转换成 Response DTO。
- MockMvc 可以用 `jsonPath("$.data.password").doesNotExist()` 固定“不返回敏感字段”的接口契约。

Next step:

下一阶段建议引入最小 Service 层，例如 `UserService`，让 Controller 不直接组装响应对象，为后续接数据库、密码加密和业务异常处理做准备。

## 2026-06-30: 统一接口前缀与用户接口推进

Current goal:

将后端统一接口前缀确定为 `/ai/v4`，并继续推进用户模块最小接口闭环。

Current state:

- `application.properties` 已配置 `server.servlet.context-path=/ai/v4`。
- `ValidationController` 当前业务路径为 `/verify/validation`。
- `UserController` 当前业务路径为 `/users`。
- MockMvc 中验证接口已使用：
  - `post("/ai/v4/verify/validation")`
  - `.contextPath("/ai/v4")`
- 用户已确认现有测试通过。

Completed:

- 明确 Controller 中不手动写 `/ai/v4`。
- 明确 `/ai/v4` 由 `server.servlet.context-path` 统一添加。
- 修正并验证了 MockMvc 使用 context path 的方式。
- 确认当前用户模块已有 `CreateUserRequest` 和 `UserController`。

Related files:

- `src/main/resources/application.properties`
- `src/main/java/com/ai4/verify/validation/ValidationController.java`
- `src/main/java/com/ai4/user/controller/UserController.java`
- `src/main/java/com/ai4/user/dto/CreateUserRequest.java`
- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`
- `D:\Dev\Projects\ai4-boot-verify\SERVER_PROGRESS.md`

Next step:

1. 给 `POST /ai/v4/users` 补 MockMvc 成功测试。
2. 给 `POST /ai/v4/users` 补缺少 `username` 的校验失败测试。
3. 新增 `CreateUserResponse`。
4. 修改 `UserController`，避免返回 `password`。
5. 重新执行 `.\mvnw.cmd test`。
6. 将最终结果同步回项目记忆文档和服务器进度文档。

## 2026-06-29: 完成校验失败错误码集中化

Current goal:

完成 `ApiResponse.validationFailed` 的剩余迁移，让校验失败响应从 `ErrorCode.VALIDATION_FAILED` 读取错误码和默认文案。

Completed:

- 读取 `AGENTS.md`、`docs/PROJECT_CONTEXT.md`、`docs/DECISIONS.md`、`docs/WORK_LOG.md` 后继续任务。
- 检查 `ApiResponse.java`、`ErrorCode.java` 和当前 MockMvc 测试，确认下一步只需要改 `ApiResponse.java`。
- 在 `ApiResponse.java` 中引入 `com.ai4.common.exception.ErrorCode`。
- 将 `validationFailed(data)` 的硬编码错误码和文案替换为：
  - `ErrorCode.VALIDATION_FAILED.code()`
  - `ErrorCode.VALIDATION_FAILED.message()`

Changed files:

- `src/main/java/com/ai4/common/response/ApiResponse.java`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 已执行 `.\mvnw.cmd test`。
- 结果：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- 结果：`BUILD SUCCESS`。

Next step:

可以开始正式搭业务骨架，但建议仍按“一次一个文件/一个概念”的方式推进。下一步可优先设计最小用户模块骨架，例如 DTO、Controller、Service、Repository 的边界，而不是直接写完整业务。

## 2026-06-26: 统一响应与异常处理验证

Current goal:

在开始正式业务模块前，验证 Spring Boot 4.1.0 下 WebMVC、请求校验、统一响应和全局异常处理的最小可用链路。

Completed:

- 应用入口已配置扫描根包 `com.ai4`。
- 已存在统一响应类型：`ApiResponse`、`FieldErrorItem`、`ValidationErrorData`。
- 已存在错误与异常基础设施：`ErrorCode`、`BizException`、`GlobalExceptionHandler`。
- `GlobalExceptionHandler` 已处理 `MethodArgumentNotValidException`、`BizException` 和兜底 `Exception`。
- `ValidationController` 的 `POST /verify/validation` 已在校验成功后调用 `ApiResponse.success(request)`。
- 已新增校验通过时的成功响应测试，覆盖 HTTP 200、`SUCCESS`、成功文案、`data` 和 `traceId: null`。
- 现有测试同时覆盖缺少 `username` 与非法 `email` 的 400 校验失败响应。

Related implementation files:

- `src/main/java/com/ai4/verify/Ai4BootVerifyApplication.java`
- `src/main/java/com/ai4/verify/validation/ValidationController.java`
- `src/main/java/com/ai4/verify/validation/ValidationRequest.java`
- `src/main/java/com/ai4/common/response/ApiResponse.java`
- `src/main/java/com/ai4/common/response/FieldErrorItem.java`
- `src/main/java/com/ai4/common/response/ValidationErrorData.java`
- `src/main/java/com/ai4/common/exception/ErrorCode.java`
- `src/main/java/com/ai4/common/exception/BizException.java`
- `src/main/java/com/ai4/common/exception/GlobalExceptionHandler.java`
- `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`

Verification:

- 已执行 `./mvnw.cmd test`，结果为 `BUILD SUCCESS`。
- 新增成功响应测试后，用户已确认 `./mvnw.cmd test` 通过；测试源码现有 4 个测试方法。
- 已覆盖的场景是 Spring 上下文加载、合法请求成功、缺少 `username`、非法 `email`。
- 本轮由助手再次执行 Maven 测试时进程长时间无新增输出后被终止，因此没有新增的可记录 Surefire 输出；以用户已确认的通过结果和仓库测试源码为当前状态。
- 已尝试 `./mvnw.cmd clean test` 以强制重新编译，但命令在长时间无新增输出后被终止；该次执行不构成验证结论。

Next step:

下一步先只处理 `ApiResponse.java`：让 `validationFailed` 从 `ErrorCode.VALIDATION_FAILED` 读取错误码与文案，完成当前错误码集中化的剩余迁移，并使用现有校验失败测试验证行为不变。

## 2026-06-26

Current goal:

建立一套适合长上下文会话使用的项目记忆文件，降低跨会话遗漏和幻觉风险。

Completed:

- 确认仓库原本没有 `AGENTS.md`。
- 确认仓库原本只有一个 Markdown 说明文件：`HELP.md`。
- 读取 `pom.xml`，确认项目是 Java 17 / Maven / Spring Boot 项目。
- 读取主要校验接口、统一响应、全局异常处理和测试文件，提取当前项目事实。
- 创建 `AGENTS.md`、`docs/PROJECT_CONTEXT.md`、`docs/DECISIONS.md`、`docs/WORK_LOG.md`。

Changed files:

- `AGENTS.md`
- `docs/PROJECT_CONTEXT.md`
- `docs/DECISIONS.md`
- `docs/WORK_LOG.md`

Verification:

- 尚未运行测试；本次只新增 Markdown 文档。

Open questions:

- 是否需要把 `HELP.md` 改名或补充为更正式的 `README.md`。
- 是否需要修复当前 Java 源文件中中文字符串的编码显示问题。

Next step:

之后每次完成重要实现或暂停长任务时，更新本文件；每次形成重要技术选择时，更新 `docs/DECISIONS.md`。

## 2026-07-17: 角色与权限管理

Completed:

- 完成角色 CRUD、角色状态管理和超级管理员保护。
- 完成角色菜单授权，并自动补齐父菜单。
- 完成用户角色授权、禁用角色校验和最后一个启用超级管理员保护。
- 新增 `@RequirePermission`、`PermissionAuthorizationInterceptor`、`ForbiddenException` 和 HTTP 403 处理。
- 给用户、角色、菜单及授权接口添加 14 个权限码。
- 新增 Flyway V7，统一三个环境的基础页面和按钮权限数据。
- 修复角色创建时误把 `roleSort` 当作 `status` 的问题。
- 修复 `@Pattern` 和 `@NotBlank` 误用于 Integer 字段的 Bean Validation 问题。
- 更新 `API_DOCUMENTATION.md`，接口总表已与 41 个 Controller 映射一致。

Verification:

- `./mvnw.cmd test-compile`：BUILD SUCCESS。
- 人工验证超级管理员调用 `GET /roles` 成功。
- 人工验证无角色用户调用 `GET /roles` 返回 HTTP 403 / `FORBIDDEN`。
- 人工验证 `/auth/profile`、`/auth/perms`、`/auth/routes` 对登录用户正常。
- 人工验证超级管理员 `/auth/perms` 返回 14 个权限码。
- 助手执行完整 `./mvnw.cmd test` 时，测试库连接报 `Access denied for user 'ai4_test'@'localhost'`，因此没有完整自动化测试通过结论。

Next step:

- 修复测试配置中的 `ai4_test` 凭据并运行完整测试。
- 在 JWT 请求链中实时检查 `sys_user.status`，使禁用用户的旧 Token 立即失效。
- 完成后打包并部署，让服务器执行 Flyway V7；确认服务器初始管理员已绑定 `super_admin`。

## 2026-07-21：数据库迁移复盘与新模块设计

Completed:

- 新增并验证题库表设计：`ai4_question_bank`、`ai4_question_choice`。
- 处理 V8 checksum mismatch，明确了已执行迁移不可修改的规则。
- 新增合伙人初始表设计，并确认合伙人与 Skill 实际为多对多关系。
- 设计 V10：独立 `ai4_skill`，新增 `ai4_partner_skill_relation`，Skill 文件夹树使用 JSON 保存。
- 排查 V10 两次 SQL 失败：MySQL `--` 注释后缺少空格，以及 `UPDATE` 拼写为 `UPDAET`。
- 修复测试清理顺序：先删除 `sys_user_role`，再删除用户；测试管理员自动绑定 `super_admin`。
- 最近一次完整 Maven 测试结果：9 个测试全部通过，`BUILD SUCCESS`。
- 参考若依完成字典模块精简设计，确定使用字典类型表和字典项表。

Open risks:

- 当前本地 V8 已修正为仅包含题库相关表，V9 单独包含合伙人初始表；重复建表和主键单引号问题已经消除。部署前仍需核对各环境 V8/V9 checksum。
- 尚未确认 V10 在当前开发库是否最终 `success = 1`，也未确认三张目标表是否完整。
- 字典模块目前只有设计，没有创建 V11、Repository、Service、Controller 或接口文档。

Next step:

1. 查询三个环境的 `flyway_schema_history` 中 V8、V9、V10 状态和 checksum，确认与修正后的迁移文件一致。
2. 检查 `ai4_partner`、`ai4_skill`、`ai4_partner_skill_relation` 的实际结构。
3. 固化 V8-V10，不再编辑成功版本；若环境不一致，使用新的补偿迁移处理。
4. V10 状态确认后编写 `V11__create_dictionary_tables.sql`。
5. 字典 SQL 完成后再设计字典管理接口、权限码和缓存策略。

## 2026-07-23：字典类型基础链路

Completed:

- V11 成功创建 `sys_dict_type` 和 `sys_dict_item`。
- V12 将字典类型备注改为可空且最大 500 字符。
- 完成字典类型 model、创建/修改 DTO 校验、Repository 接口和 JDBC 实现。
- 修正 JDBC 实现误导入 Flyway `JdbcTemplate` 的问题，统一使用 Spring `JdbcTemplate`。
- 修正动态查询拼接时 `AND` 前必须保留空格的问题。
- 完成字典类型 Service 的详情、列表、创建和修改逻辑。
- 新增字典类型 Controller 的四个接口。

Pending:

- 新增字典管理菜单和 `system:dict:list/create/update` 权限数据，否则新接口会返回 403。
- 增加修改状态接口，然后补 `system:dict:delete` 和删除保护。
- 实现字典项 Repository、Service、Controller。
- 实现业务读取接口 `GET /dicts/{dictCode}/items`。
- 完成自动化测试和接口文档后再考虑缓存。


## 2026-07-23：ApiPost 接口用例与中文字段说明

Completed:

- 仅维护 ApiPost `frozen的团队 / AI4.0后台`（项目 ID `67b328d3446d000`）中的 `服务 / ai4-boot-verify` 接口。
- 为 41 个接口补充或完善请求/响应示例、实际请求字段中文说明和接口注意事项。
- 补充 JWT、`system:*` 权限、整体替换授权、超级管理员保护、软删除、课节浅复制、日期格式和密码安全等注意事项。
- 修正 `/auth/routes`：由错误的 POST 及登录参数调整为无请求体的 GET，请求身份只从 Bearer Token 获取。
- 清理导入过程中产生的重复分组，并将带句号的旧“用户角色授权接口。”替换为已完善的“用户角色授权接口”。

Verification:

- `服务 / ai4-boot-verify` 最终为 9 个接口分类、41 个接口。
- 共检查 118 个实际请求字段，中文说明缺失数为 0。
- ApiPost 根目录仅保留原有“本地”和“服务”，没有残留临时导入分组。

## 2026-07-27：题目管理查询、修改、删除和权限

Completed:

- 完成 `QuestionBank`、`QuestionChoice` 对应的列表和详情响应 DTO。
- 扩展 `QuestionRepository` 与 `JdbcQuestionRepository`：更新题目、更新指定题目选项、逻辑删除题目。
- 完成 `QuestionService`：列表转换、详情组装、事务修改、选项业务校验和逻辑删除。
- 完成 `QuestionController` 四个接口：列表、详情、修改、删除。
- 新增错误码 `QUESTION_NOT_FOUND`。
- 新增 V15，创建题目管理菜单及三个权限，并授权给启用中的 `super_admin`。
- 将四个题目接口同步到 ApiPost `AI4.0后台 / 本地 / ai4-boot-verify / 题目管理接口`。
- 更新 `API_DOCUMENTATION.md`，补充接口总表、字段、请求示例、业务规则和权限说明。

Verification:

- 已执行 `mvn test-compile`。
- 结果：`BUILD SUCCESS`，117 个主源码文件和 2 个测试源码文件编译通过。

Pending:

- 启动应用执行 V15，并确认 `flyway_schema_history.success = 1`。
- 重新登录后确认 `/auth/perms` 包含三个 `question:*` 权限。
- 对四个题目接口执行开发库数据流转测试。
- 工作流契约确定后实现生成题目接口。
- 增加 `imageUrls`、`optionValue` 的 JSON 合法性校验及自动化测试。

## 2026-07-27：删除题目选项正确答案字段

Completed:

- 新增 V16，删除 `ai4_question_choice.correct`。
- 从 `QuestionChoice`、`QuestionChoiceResponse`、`UpdateQuestionChoiceRequest` 删除 `correct`。
- 更新 JDBC 查询和修改 SQL，不再读取或写入 `correct`。
- 删除 Service 中单选、多选正确答案数量校验；保留题型和 A-D 完整性校验。
- 更新接口文档和 ApiPost 修改题目请求示例。

Verification:

- 测试库成功从 V15 迁移到 V16。
- 完整测试结果：`Tests run: 9, Failures: 0, Errors: 0, BUILD SUCCESS`。
## 2026-07-30：Skill 绑定数量与删除保护

Completed:

- `SkillListItemResponse` 新增 `partnerCount`。
- Skill 分页 SQL 统计 `ai4_partner_skill_relation` 中每个 Skill 的绑定数量。
- `SkillRepository` 新增合伙人绑定检查。
- Skill 存在绑定时禁止逻辑删除，并新增 `SKILL_ASSIGNED_TO_PARTNER`。
- 软删除 SQL 增加 `NOT EXISTS` 保护。
- 更新唯一接口文档和 ApiPost 中的分页列表、逻辑删除接口。
- 执行 `mvn -q -DskipTests compile`，编译通过。

Next step:

- 开发合伙人接口时实现 Skill 绑定和解绑，并复用该删除保护规则。
## 2026-07-30: 合伙人管理接口

Completed:

- 新增合伙人 model、请求/响应 DTO、分页参数、Repository、Service 和 Controller。
- 完成 `GET /partners`、`GET /partners/{partnerId}`、`POST /partners`、
  `PUT /partners/{partnerId}`、`DELETE /partners/{partnerId}`。
- 完成 Skill 数量、重复 ID、有效状态校验和事务化关联替换。
- 合伙人删除会清理关联并软删除主记录。
- 五个图片 URL 改为非必填，空文本保存为 `null`。
- 新增 V20 合伙人菜单和四个权限。
- 更新唯一接口文档 `D:\Dev\Projects\ai4-boot-verify\API_DOCUMENTATION.md`。
- 在 ApiPost `AI4.0后台 / 本地 / ai4-boot-verify` 下新增“合伙人管理接口”分组，并同步分页、
  详情、新增、修改、逻辑删除五个接口。

Verification:

- `.\mvnw.cmd -q -DskipTests compile`：通过。
- 首次直接运行测试因当前进程未设置 `AI4_TEST_DB_PASSWORD`，MySQL 返回 1045；未进入迁移校验。
- 在命令进程内设置测试库密码后运行 `.\mvnw.cmd test`：9 个测试全部通过，`BUILD SUCCESS`。
- Flyway 成功校验 21 个迁移，并将测试库从 V19 迁移至 V20。

Pending:

- 启动开发环境后重新登录，确认 `/auth/perms` 返回四个 `partner:*` 权限。
## 2026-07-30: Skill 与合伙人完整性优化

Completed:

- Skill 绑定计数改为只统计未删除合伙人。
- 合伙人绑定 Skill 时使用 `FOR UPDATE` 处理绑定与删除并发。
- 新增 `GET /skills/options` 轻量选项接口。
- 合伙人详情和修改增加 `version` 乐观锁。
- Skill 与合伙人名称对未删除记录唯一，删除后允许复用。
- 新增 V21 生成列、唯一索引和合伙人版本字段。
- 更新接口文档及 ApiPost Skill、合伙人相关接口。

Verification:

- `.\mvnw.cmd -q -DskipTests compile`：通过。
- V21 在测试库执行成功，Flyway 当前版本为 21。
- `.\mvnw.cmd test`：9 个测试全部通过，`BUILD SUCCESS`。
## 2026-07-31：课包与项目管理重构

Completed:

- 新增 V22，创建 `ai4_project`，删除空的旧课节和课程内容表，并初始化项目菜单及四个权限。
- 新增项目 model、请求/响应 DTO、分页参数、Repository、Service 和 Controller。
- 项目管理实现分页筛选、详情、新增、完整修改、逻辑删除、编码唯一校验和唯一课包自动绑定。
- 课包模块缩减为 `GET /course-package` 单一查询接口。
- 删除旧课包维护、课节、课程内容相关 Java 代码和旧集成测试。
- 更新 `ErrorCode`、测试配置和唯一接口文档，移除旧接口说明。
- 新增项目模块集成测试。

Verification:

- Flyway 成功校验 23 个迁移并将测试库从 V21 升级到 V22。
- `mvn test`：10 个测试全部通过，`BUILD SUCCESS`。

Deployment notes:

- 部署前确认服务器旧 `ai4_lesson`、`ai4_lesson_content` 表确实不需要保留数据；V22 会物理删除这两张表。
- 服务器必须恰好保留一条 `del_flag = 1` 的有效课包，否则课包查询和项目新增会返回 `COURSE_PACKAGE_CONFIGURATION_INVALID`。
- 部署并执行 V22 后重新登录，确认 `/auth/perms` 包含四个 `project:*` 权限。

Follow-up:

- 根据项目总量固定为 8 个的业务确认，项目列表取消分页、名称筛选和编码筛选。
- 列表响应缩减为 `id`、`projectName`；详情响应缩减为 `projectName`、`projectIntro`、`projectCode`。
- 调整集成测试并重新执行完整测试：10 个测试全部通过，`BUILD SUCCESS`。
## 2026-07-31：项目领取任务模块

Completed:

- 新增 V23，创建任务主表、任务合伙人关联表及四个任务权限。
- 新增 `com.ai4.task` 的 model、DTO、Repository、Service 和 Controller。
- 实现任务查询、首次创建、完整修改和AI生成公司名称四个接口。
- 实现任务类型白名单、2至4个合伙人、重复/失效合伙人、公司名称长度和后缀校验。
- 公司名称生成最多调用火山3次，解析 JSON 数组并返回60个合法且不重复的候选。
- 修复 V23 首次测试暴露的合伙人外键排序规则不一致问题，关联表改为 `utf8mb4_bin`。
- 新增任务模块集成测试，并使用 Mock AI 避免测试消耗真实火山额度。

Verification:

- 测试库成功从 V22 迁移到 V23。
- `mvn test`：12个测试全部通过，`BUILD SUCCESS`。

Follow-up:

- 新增 V24，为任务表增加 `company_names JSON`。
- 公司名称生成由“只返回候选”调整为“生成成功后整体保存并返回”；再次生成整体替换。
- 新增 `PUT /projects/{projectId}/task/company-names/{index}`，按下标修改候选名称。
- 集成测试已覆盖生成持久化、任务详情回显、候选修改和超长名称校验。
- 测试库成功迁移到 V24；完整测试仍为12个全部通过。
- 需求确认不存在最终选中的公司名称；新增 V25 删除 `company_name`，并从任务请求、响应、模型和 SQL 中移除对应单值字段。
- 新增 V26，创建 `ai4_project_generation` 和 `ai4_project_report`，增加六个权限按钮及 `super_admin` 授权。
- 完成项目生成和创业报告各3个接口；新增集成测试后共14个测试全部通过。
## 2026-08-03：OSS 基础链路与合伙人五图上传

Completed:

- 增加 OSS 配置绑定、上传响应 DTO 和 `OssStorageService`。
- 删除对外通用文件上传 Controller，避免业务方绕过具体文件规则。
- 新增 `POST /partners/images/upload`，一次接收大图、中图、小图、全身图和头像。
- 合伙人 Service 在上传前统一校验五张图片非空、单张不超过 5MB、扩展名为 png/jpg/jpeg。
- 五张图片分别上传到对应业务前缀，响应返回五个 OSS URL。
- multipart 配置调整为单文件 5MB、单请求 30MB。
- 合伙人新增和完整修改请求中的五个图片 URL 改为必填。
- 更新唯一接口文档 `D:\Dev\Projects\ai4-boot-verify\API_DOCUMENTATION.md`，补充接口总表、multipart 字段、响应示例、业务流程、错误码和 OSS 目录说明。
- ApiPost `AI4.0后台 / 本地 / ai4-boot-verify / 合伙人管理接口` 已存在“上传合伙人图片”接口，路径和五个必填 File 参数已核对。

Verification:

- 代码检查确认 Controller 调用的方法名已与 Service 的 `uploadImages` 一致。
- Maven 编译未完成：运行中的本地进程占用 `target/classes/application.properties`，资源复制阶段返回 `AccessDeniedException`，尚未形成 BUILD SUCCESS 结论。

Pending:

- 停止占用 `target` 的本地应用后重新执行编译和测试。
- 明确上传接口权限，优先考虑独立 `partner:upload` 权限。
- 补 MIME/真实图片内容校验、多文件上传失败补偿和孤立对象清理。
- 使用 ApiPost 实际上传五张图片，再携带返回 URL 创建合伙人，验证完整数据流转。

## 2026-08-05：项目任务三图上传

Completed:

- 新增 `POST /projects/{projectId}/task/images/upload`，一次接收 `publicTask`、`claimTask`、`companyCard` 三个必填文件。
- 补齐 `projectId` 从 Controller 到 Service 的调用链，上传前校验项目有效。
- OSS 对象键前缀调整为按项目隔离的 `task/{projectId}/public-task`、`claim-task`、`company-card`。
- 任务新增和完整修改 DTO 的三个图片 URL 增加 `@NotBlank`，与已具备上传能力的业务状态一致。
- 修正图片校验方法名为 `validateImage`，并显式声明三个 multipart 参数名。
- multipart 基础设施上限调整为单文件 20MB、单请求 60MB；任务业务暂不增加其他文件规则。
- 更新项目上一级的唯一接口文档和记忆文档；不在源码项目内保存 `docs`。

Verification:

- 静态检查确认 Controller、Service、DTO 和响应字段签名一致，旧方法拼写已清除。
- Maven 已调用 javac 编译 166 个源文件，未报告 Java 编译错误；最终因本地运行进程占用 `target` 状态文件而未形成 `BUILD SUCCESS`。

## 2026-08-06：项目沙盘模块

Completed:

- 新增 `com.ai4.sandtable` 的 model、DTO、Repository、Service和Controller。
- 实现 `GET/POST/PUT /projects/{projectId}/sandtable` 和 `POST /projects/{projectId}/sandtable/video/upload`。
- 完成项目存在校验、项目级单例校验、视频必填、分段参数校验和3至6个属性校验。
- 分段按完整列表替换；属性按ID差异同步，覆盖未变化、新增、修改、删除四种情况。
- 新增四个沙盘错误码，并复用 `project:list/create/update` 权限。
- 更新唯一接口文档 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`，并修正仓库 `AGENTS.md` 中的文档路径。
- 按用户要求未创建沙盘测试用例。

Verification:

- Maven调用javac编译180个源文件时未报告Java源码错误。
- 构建最终因本地运行进程占用 `target/maven-status` 文件而失败，尚无 `BUILD SUCCESS` 结论。
- 静态检查确认Repository未使用已删除的分段和属性排序字段，测试目录没有沙盘测试文件。

## 2026-08-07：移除测试库依赖

Completed:

- 删除4个数据库集成测试文件和 `src/test/resources/application-test.properties`。
- 从POM移除 `spring-boot-starter-actuator-test`、`spring-boot-starter-webmvc-test`。
- 全项目确认没有 `AI4_TEST_DB_PASSWORD`、`ai4_boot_verify_test` 和测试Starter残留。

Verification:

- 直接执行不带密码、不带 `-DskipTests` 的 `mvn package`，Maven没有请求或连接测试数据库。
- 打包在资源复制阶段因本地运行进程占用 `target/classes/application.properties` 失败，与测试和数据库密码无关。

## 2026-08-07：扩展沙盘趋势图与预计学习收获

Completed:

- 沙盘主模型、请求、响应和Repository增加`trendUrl`/`trend_url`。
- 新增预计学习收获model、DTO、查询、新增、删除和差异同步逻辑，数量校验为2至4条。
- 新增收益趋势图上传接口，复用`project:update`权限和OSS服务。
- 新增`SANDTABLE_GAIN_INVALID`和`SANDTABLE_TREND_IMAGE_EMPTY`错误码。
- 更新唯一接口文档，不新增Flyway和测试。

Verification:

- Maven调用javac编译184个源文件，未报告Java源码错误。
- 最终仍因本地运行进程占用`target/maven-status`而未形成`BUILD SUCCESS`。

## 2026-08-07：项目列表增加配置完成状态

Completed:

- 新增`ProjectListItem`查询投影和`configurationStatus`响应字段。
- 项目列表SQL一次关联任务、项目生成、沙盘和创业报告，根据四条主记录是否全部存在返回`COMPLETED`或`INCOMPLETE`。
- 状态为实时派生值，未修改数据库结构；Controller路径和权限保持不变。
- 更新唯一接口文档。

Verification:

- 静态检查确认旧`projectRepository.findAll()`调用已全部替换。
- Maven调用javac编译185个源文件，未报告Java源码错误；最终仍因本地运行进程占用Maven状态文件失败。

## 2026-08-11：项目题目管理接口重写

Completed:

- 删除旧题目管理 Java model/DTO 中 `QuestionBank`、`QuestionChoice`、固定四选项和单选/多选相关链路。
- 新增项目题目主表 model：`ProjectQuestion`。
- 新增题目管理请求/响应 DTO：项目属性、分页查询、列表项、详情、选项、选项属性影响、新增、修改、图片上传响应。
- 重写 `QuestionRepository` 和 `JdbcQuestionRepository`，切换到 `ai4_project_question`、`ai4_project_question_option`、`ai4_project_question_option_attribute`。
- 重写 `QuestionService`：项目属性查询、分页列表、详情、新增、完整修改、逻辑删除和 OSS 图片上传。
- 重写 `QuestionController`，当前接口包括项目属性、分页列表、详情、新增、修改、删除和图片上传。
- 更新唯一接口文档 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`，替换旧第 18 章题目管理说明，并更新接口总表。

Verification:

- 执行 `./mvnw.cmd -DskipTests package`，结果 `BUILD SUCCESS`。
- 未运行测试；当前项目已移除测试库依赖，本次按跳过测试的打包验证。

Self-check:

- 与既有风格一致：Controller 返回 `ApiResponse`，分页返回 `PageResponse`，Service 使用事务，Repository 使用 `JdbcTemplate`。
- 与既有风格不完全一致：`QUESTION_ENABLED_LIMIT_EXCEEDED` 使用 `new BizException(String, String)` 临时返回，未写入 `ErrorCode` 枚举；原因是当前 `ErrorCode` 文件历史中文编码较乱，避免本次扩大改动。
- 与既有权限风格不完全一致：`POST /questions/image/upload` 当前无 `@RequirePermission`，只要求登录；这与合伙人图片上传、任务图片上传现状一致，但后续建议增加独立上传权限。
- 当前按用户要求未做项目存在校验、属性全集校验、影响值格式校验和 `GOOD/BAD` 枚举校验。

Pending:

- 将新增题目管理接口同步到 ApiPost `AI4.0后台 / 本地 / ai4-boot-verify / 题目管理接口`。
- 后续如果前端联调需要，可增加 `question:upload` 权限以及影响值格式校验。

ApiPost follow-up:

- 已删除 `AI4.0后台 / 本地 / ai4-boot-verify / 题目管理接口。` 下旧的 `/questions/project-options` 和旧列表接口。
- 已重新创建 7 个题目管理接口节点：项目属性、分页列表、详情、新增、修改、逻辑删除、图片上传。
- 受当前 Apipost MCP `create_target` 参数校验限制，POST/PUT/DELETE/上传接口仅同步了名称、方法和路径；复杂请求体、multipart 参数和响应示例以 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md` 为准。

Follow-up review:

- 删除题目图片上传响应中的 `objectKey`，当前仅返回 `url`。
- 同步更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md` 的响应示例和字段约定。
- 执行 `mvn -DskipTests package`，结果 `BUILD SUCCESS`。
- 发现题目 Controller 的 6 个业务权限注解处于注释状态，且已有 V15 权限初始化缺少 `question:create`；该权限链路需要单独统一，尚未在本次响应字段调整中修改。

## 2026-08-11：修复沙盘属性精度与题目关联稳定性

Completed:

- 将沙盘属性请求、响应、Model 的 `target`、`initial` 从 `Integer` 改为 `BigDecimal`。
- JDBC 查询改用 `getBigDecimal`，支持数据库 `DECIMAL(10,2)` 的小数值。
- 新增属性原逻辑保持不变；修改已有属性改为按原 ID 更新。
- 删除属性前查询题目选项属性关联，存在引用时返回 `SANDTABLE_ATTRIBUTE_IN_USE` 并回滚整个沙盘修改事务。
- 更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md` 的字段类型、属性同步规则和错误码。

Verification:

- 执行 `mvn -DskipTests package`，结果 `BUILD SUCCESS`，编译 191 个源文件。
- 当前项目没有自动化测试，本次未执行接口级回归测试。

## 2026-08-11：限制题目选项数量

Completed:

- 新增和修改题目请求的 `options` 统一限制为2至4个。
- 参数数量不符合要求时通过统一参数校验返回 `VALIDATION_FAILED`。
- 同步更新唯一接口文档中的字段规则、新增规则和修改规则。

Verification:

- 执行 `mvn -DskipTests package`，结果 `BUILD SUCCESS`。

## 2026-08-11：profile 返回角色名称

Completed:

- `/auth/profile` 的 `roles` 从 `sys_role.role_code` 改为返回 `sys_role.role_name`。
- Repository 方法同步由 `findRoleCodesByUserId` 重命名为 `findRoleNamesByUserId`。
- 权限码查询 `/auth/perms` 和角色授权逻辑不受影响。
- 同步更新唯一接口文档的响应示例和字段说明。

Verification:

- 执行 `mvn -DskipTests package`，结果 `BUILD SUCCESS`。
## 2026-08-15：班级直播课分页查询

Completed:

- 新增 `com.ai4.livecourse` 模块的查询 DTO、响应 DTO、Repository、Service 和 Controller。
- 实现 `GET /live-courses`，支持房间号、班级名称、教师名称、上课时间范围和状态筛选。
- 使用 `COUNT + LIMIT/OFFSET` 分页，默认每页 10 条，最大 100 条。
- 一次 SQL 关联 `live_course`、`ai4_class_live_course`、`ai4_class`、`ai4_project` 和 `live_teach`。
- 子查询分别汇总 `live_user_course_record` 的出勤和迟到人数，通过 `attendanceCount`、`lateCount` 两个数值字段返回。
- 同步更新唯一接口文档 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`。

Verification:

- 执行 `mvn -DskipTests package`，结果 `BUILD SUCCESS`，编译 207 个主源码文件。
- 当前项目无数据库自动化测试，本次未执行接口级数据验证。

Pending:

- 使用开发库实际数据验证关联表字段、毫秒时间戳、筛选条件和出勤统计结果。
- 确认后续直播课详情、新增、修改和删除业务规则。

## 2026-08-15：班级选项与直播课新增

Completed:

- 引入 Spring Data Redis，配置 AI4 使用 Redis `db11`，并通过 Actuator 健康检查确认本地隧道连接正常。
- 新增 `RoomIdGenerator`，使用 `ai4:live:room:seq:1` 原子递增生成普通直播课房间号。
- 新增通用 `SnowflakeIdGenerator`，为 `live_course.course_id` 生成数字字符串ID。
- 新增 `com.ai4.classroom` 模块，实现 `GET /classes/options` 有效班级选项查询。
- 新增 `POST /live-courses`，校验有效班级、有效项目、同课包和班级建课配置。
- 将开课日期与班级上课时间按 `Asia/Shanghai` 组合为毫秒时间戳，继承班级教师、录制、拖堂和房间类型配置。
- 在同一事务中写入 `live_course` 和 `ai4_class_live_course`，返回 `courseId` 和 `roomId`。
- 更新唯一接口文档 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`。

Verification:

- 执行 `.\mvnw.cmd -DskipTests package`，结果 `BUILD SUCCESS`，编译 218 个主源码文件。
- `/actuator/health` 已由用户本地调用并返回 `UP`，Redis连接成功。

Pending:

- 使用开发库实际调用 `GET /classes/options` 和 `POST /live-courses`，验证 `ai4_class_live_course` 实际字段及直播课默认值。
- 调用新增接口后在 Redis Insight 的 `db11` 检查 `ai4:live:room:seq:1` 是否递增。
- 将两个新增接口同步到 ApiPost 的 AI4.0后台本地目录。

## 2026-08-15：直播课列表响应与查询精简

Completed:

- 将直播课列表原 `attendanceLate=x/x` 字符串拆分为 `attendanceCount` 和 `lateCount` 两个数值字段。
- `ClassInfo` 仅保留创建直播课实际使用的班级配置，移除未使用的班级 ID 和班级名称。
- 分页总数查询不再关联和聚合 `live_user_course_record`，出勤统计只在当前页数据查询中执行。
- 同步更新唯一接口文档、项目上下文和决策记录。

Verification:

- 执行 `mvn -DskipTests package`，结果 `BUILD SUCCESS`，编译 218 个主源码文件。
- 当前项目没有自动化数据库测试，本次未执行接口级数据验证。

## 2026-08-17：CRM班级同步和自动创建直播课

Completed:

- 新增 `com.ai4.externalcrm` 模块及 CRM 班级请求、排课模型、Repository、Service 和 Controller。
- 实现 `POST /external/aiClass/add`，保存CRM班级并为正常班级按课包项目批量创建直播课。
- 实现 `POST /external/aiClass/openClose`；停课取消未开始课程，恢复时补建缺失课程。
- CRM班级ID重复推送时直接返回已有内部班级ID，不重复建班和建课。
- CRM教师ID通过 `live_teach.crm_teacher_id` 转换为内部教师ID。
- 扩展项目Repository的按课包查询，以及直播课Repository的存在性检查、未开始课程查询、逻辑删除和关联删除能力。
- 区分手动课程 `system_default=1` 与CRM自动课程 `system_default=0`。
- 按最终确认将CRM班级和自动直播课统一为 `room_type=2`，与旧项目保持一致。
- 自动排课项目顺序调整为读取 `ai4_project.sort ASC, id ASC`；该顺序只影响后续新建班级。
- 同步更新唯一接口文档、项目上下文和决策记录。

Verification:

- 执行 `mvn -DskipTests package`，结果 `BUILD SUCCESS`，编译225个主源码文件。
- 当前项目没有自动化数据库测试，本次尚未连接开发库调用CRM接口。

Pending:

- `/external/**` 已排除后台JWT和权限拦截器；后续实现CRM专用访问认证。
- 使用开发库验证班级插入、项目批量排课、停课、恢复和重复推送。

## 2026-08-17：补齐 CRM 外部同步接口

Completed:

- 新增 `PUT /external/aiClass/update`，修改班级配置并仅重建未开始直播课。
- 新增 `POST /external/aiClass/syncStudents`，按手机号批量绑定或恢复班级学员。
- 新增 `POST /external/aiClass/removeStudents`，按手机号将班级学员状态改为已移除。
- 新增 `POST /external/liveTeach/add` 和 `POST /external/liveTeach/update`。
- 教师修改接口支持密码为空时保留原密码，有内容时重新 BCrypt 后更新。
- 新增 `GET /external/aiClass/coursePackageLiveStat`，返回有效课包ID、名称和实际有效直播课数量。
- 同步更新唯一接口文档、项目上下文和决策记录。

Verification:

- 执行 `mvn -DskipTests compile`，结果 `BUILD SUCCESS`，编译240个主源码文件。
- 当前项目没有自动化数据库测试，本次未连接开发库执行接口级数据验证。

Pending:

- 使用开发库验证班级修改后未开始课程重建、学员恢复绑定、教师密码保留和课包统计结果。
- `/external/**` 仍未接入 CRM 专用认证。

Correction:

- 修正课包直播课统计口径：不再使用 `COUNT(ai4_project.id)`。
- 当前通过 `ai4_class -> ai4_class_live_course -> live_course` 统计 `del_flag=1` 的不同直播课ID，不限制课程状态。

## 2026-08-18：声网 Token 生成基础服务

Completed:

- 新增声网认证依赖 `io.agora:authentication:2.1.3`。
- 新增 `AgoraTokenService`，支持系统 RTM Token、观察者 RTM Token、录制 RTC Token和 REST Basic Authorization 请求头生成。
- 系统账号固定为 `-2`；观察者 RTM Token 固定为 24 小时，录制 RTC Token 固定为 72 小时。
- 增加声网启用状态、App ID、App Certificate、REST Key/Secret 和录制 UID 的必要校验。
- Token 生成结果为空时直接失败，不输出任何密钥或 Token 日志。

Verification:

- 执行 `mvn -DskipTests compile`，结果为 `BUILD SUCCESS`，共编译 242 个主源码文件。
- 本次未新增 HTTP 接口，因此未修改 `API_DOCUMENTATION.md` 和 ApiPost。

Pending:

- 在直播课详情与录像业务中接入 Token 服务、观察者身份、云端录制启动和停止流程。
- 使用真实声网环境完成 RTM、RTC Token 及录制页面联调。

## 2026-08-19：声网教室与云端网页录制客户端

Completed:

- 新增 `AgoraClassroomClient`，实现创建教室、更新教室自定义属性和按游标拉取教室事件。
- 新增 `AgoraCloudRecordingClient`，实现 `acquire`、网页录制 `start`、`query` 和同步 `stop`。
- 创建教室参数沿用旧项目约定：举手上限 12、学生角色上限 505，课程时长和拖堂时间由分钟转换为秒。
- 网页录制使用结构化 DTO 代替旧项目的 JSON 字符串模板，保留 720p、30 FPS、HLS/MP4、WebGL 和阿里云 OSS 配置结构。
- 录制资源有效期固定为 72 小时；录制模式固定为 `web`。
- 新增 `AgoraClientException` 和统一 RestClient 支持层，只记录操作名、业务标识、HTTP 状态和声网 requestId，不记录 Token、Authorization 或 OSS 密钥。
- 第一版不启用 HTTP 自动重试，避免 `create/start/stop` 等非幂等请求被客户端重复提交；后续由业务服务按具体状态决定是否重试。

Verification:

- 执行 `mvn -DskipTests compile`，结果为 `BUILD SUCCESS`，共编译 256 个主源码文件。
- 当前客户端尚未接入 Controller、定时任务和数据库录制服务，因此本次未修改接口文档和 ApiPost。

Pending:

- 实现声网教室创建的业务服务并接入排课后的创建房间任务。
- 实现录制 UID、观察者身份、`live_video_record` 持久化及录制启动/查询/停止编排。
- 使用开发环境声网应用和测试房间完成真实 HTTP 联调。

## 2026-08-24：教师后台账号、角色和密码同步

Completed:

- 为 `CreateCrmTeacherRequest` 增加必填 `roleIds`。
- CRM 新增教师改为在同一事务中写入 `sys_user`、`live_teach`、`sys_user_role`。
- 增加角色存在、启用、重复及 `super_admin` 分配校验。
- CRM 修改教师时同步修改后台账号和直播教师账号；密码非空时只 BCrypt 一次并同步两张表。
- 后台用户密码重置增加 `live_teach` 同步更新，并保持事务一致性。
- 新增教师管理模块及 `GET /teachers`，支持账号、昵称模糊查询并返回关联的后台 `userId`。
- 将 `externalcrm` 和本次新增教师代码中的英文 JavaDoc 统一改为中文。
- 更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`，补充教师新增角色、教师查询及密码同步约定。

Verification:

- 执行 `.\mvnw.cmd -DskipTests compile`，结果为 `BUILD SUCCESS`，共编译 262 个主源码文件。
- 本次未连接开发数据库执行接口级数据验证。

Pending:

- 使用开发库验证 CRM 新增教师后三张表的数据一致性。
- 验证 CRM 修改教师和后台重置密码后，`sys_user` 与 `live_teach` 的 BCrypt 哈希保持一致。

## 2026-08-25：直播课详情与学员分页

Completed:

- 新增 `GET /live-courses/{courseId}`。
- 新增直播课详情、录像、学员明细和学员分页 DTO。
- Repository 增加基本信息、出勤统计、学员总数、分页学员和录像地址查询。
- Service 负责组合详情，计算实际上课秒数、生成录像名称并解析 `live_course_video.m3u8`。
- 学员列表改为从班级有效成员出发左关联上课记录，未进入教室的学员也可以展示。
- 增加 `LIVE_COURSE_NOT_FOUND` 错误码。
- 更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md` 的接口列表及班级直播课章节。

Verification:

- 两次执行 `.\mvnw.cmd -DskipTests compile` 均为 `BUILD SUCCESS`，最终编译 268 个主源码文件。
- 本次未连接开发数据库执行接口级查询验证。

Pending:

- 使用开发库真实课程验证班级学员数量、四项出勤统计和迟到秒数。
- 使用实际 `live_course_video.m3u8` 数据验证单 URL 与 JSON 数组两种存储格式。

## 2026-08-25：接入声网教室定时创建

Completed:

- 新增 `AgoraRoomProvisionData`，只承载创建声网教室所需的直播课数据。
- Repository 增加待创建教室查询，扫描未来10分钟内开始且当前尚未结束的有效直播课。
- 新增 `AgoraRoomProvisioningService`，定时调用 `AgoraClassroomClient.createRoom`。
- 使用课程级 Redis 锁避免多实例并发创建，创建成功后写入7天幂等标记。
- HTTP 409 按教室已经存在处理成功，其他异常记录后由下一轮任务重试。
- 启用 Spring Scheduling，并增加可通过 `AI4_AGORA_ROOM_PROVISION_DELAY_MS` 调整的扫描间隔配置。
- 修正后台手动建课仍可能写入 `room_type=4` 的旧逻辑，手动和 CRM 自动建课现统一固定为 `room_type=2`。
- 本次没有新增或修改 HTTP 接口，因此未修改接口文档和 ApiPost。

Verification:

- 执行 `.\mvnw.cmd -DskipTests compile`，结果为 `BUILD SUCCESS`，共编译270个主源码文件。
- 本次未连接真实声网环境创建教室。

Pending:

- 在开发环境准备一条未来10分钟内开始的直播课，验证声网教室创建和 Redis 成功标记。
- 实现教师、学生进出教室事件同步，以及课程状态和考勤数据维护。

## 2026-08-26：补充声网教室调度诊断日志

Completed:

- 应用启动完成后明确记录声网教室创建任务是否启用；禁用时提示检查 `AI4_AGORA_ENABLED`。
- 每次真正调用声网前记录 `courseId`、`roomId` 和计划开课时间。
- 增加待创建课程扫描异常日志，避免 SQL 或数据库异常只由调度框架兜底输出。
- 保留原有提前10分钟扫描、Redis锁、成功标记和失败重试逻辑不变。

Verification:

- 执行 `.\mvnw.cmd -DskipTests compile`，结果为 `BUILD SUCCESS`，共编译270个主源码文件。
- 尚需部署后确认 systemd 环境中的 `AI4_AGORA_ENABLED=true`，再进行真实声网创建测试。

## 2026-08-26：修复声网教室创建 HTTP 401

Completed:

- 定位创建教室 HTTP 401 的原因：灵动课堂客户端沿用了旧版 RTM Token 请求头。
- `AgoraTokenService` 增加应用级 EducationToken 鉴权请求头生成逻辑。
- `AgoraClassroomClient` 的创建教室、更新教室属性和查询教室事件统一改为 `Authorization: agora token=...`。
- 保留原有 RTM、RTC Token 和云端录制 Basic 鉴权逻辑。

Verification:

- 执行 `.\mvnw.cmd -DskipTests compile`，结果为 `BUILD SUCCESS`，共编译270个主源码文件。
- 尚需部署后使用真实声网项目验证创建教室成功，并确认 Redis 写入课程创建成功标记。

## 2026-08-26：实现声网课堂事件和考勤同步

Completed:

- 将声网事件分页结果从松散 `Map` 改为结构化事件 DTO，覆盖课堂状态和教师、学生进出事件。
- 新增待同步课程查询，只扫描已创建、未关闭且仍处于声网事件保留窗口内的直播课。
- 新增每5秒执行的事件同步任务，依次拉取 `cmd=20` 和 `cmd=1`，支持 `nextId` 分页。
- 使用 Redis 课程锁、分页游标和 `roomId + cmd + sequence` 幂等标记，防止多实例或重复查询造成重复处理。
- 教师和学生进入时记录在线状态、首次进入、出勤和迟到；退出时记录最后离开及临时早退状态。
- 课堂关闭时补齐未进入学生的缺勤记录、在线用户离开时间，并统一结算教师和学生的缺勤、早退状态。
- 课堂状态同步到 `live_course.status/start_time/end_time`。
- 新增可选环境变量 `AI4_AGORA_EVENT_SYNC_DELAY_MS`，默认同步间隔为5000毫秒。

Verification:

- 首次编译发现项目使用 Jackson 3，已将新增代码从旧包名改为 `tools.jackson`。
- 再次执行 `.\mvnw.cmd -DskipTests compile`，结果为 `BUILD SUCCESS`，共编译279个主源码文件。
- 本机没有 MySQL CLI，本轮未直接验证手动导入的直播事件相关表结构及真实声网事件落库。

Pending:

- 部署后使用教师和学生各进入、退出一次测试课堂，核对在线表、教师记录、学生记录和直播课状态。
- 本后台只负责创建教室与消费事件；教师、学生客户端获取 RoomUserToken 的接口仍应在实际客户端服务中按登录身份实现。

## 2026-08-26：实现声网云端录制编排

Completed:

- 新增录制课程、录制任务和最终录像模型，以及 `live_video_record`、`live_course_video` 的 JDBC Repository。
- 在首个合法教师或学生进房事件提交后启动网页录制；课程未开启录制或已有活动任务时直接跳过。
- 沿用旧项目录制页面参数，生成观察者 RTM Token、录制 RTC Token、随机录制 UID 和随机回调密钥，并按旧键名保存观察者 Redis 身份。
- 扩展声网云端录制客户端，支持 acquire、start、update、query 和 stop。
- 新增录制网页暂停/继续、心跳以及声网录制状态回调接口，并从后台 JWT 和权限拦截中排除。
- 课堂状态3提交后停止录制，解析云端文件并更新 `live_video_record` 与 `live_course_video`。
- 新增关闭课程录制补偿任务，处理应用重启或临时声网请求失败造成的活动录制遗留。
- 使用 Redis 课程锁、录制 UID、心跳和回调幂等键，防止重复启动和重复处理回调。
- 更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`、项目上下文和技术决策文档。
- 已替换 ApiPost 中旧的账户列表节点，新节点名称为“分页查询学员账户”，节点 ID 为 `6c3cfaf2ec72000`。

Verification:

- 执行 `\.\mvnw.cmd -DskipTests compile`，结果为 `BUILD SUCCESS`，共编译291个主源码文件。
- 尚未进行真实音视频录制联调；需要客户端进入课堂并发布媒体后验证 OSS 文件、回调和最终录像落库。

Pending:

- 部署时设置 `AI4_AGORA_RECORD_PAGE_URL`，并确认声网云端录制使用的 OSS AccessKey 具备目标 Bucket 写入权限。
- 与旧录制网页联调观察者身份和 Token 参数，并确认录制页相关服务与本项目使用同一 Redis 实例和数据库编号。

## 2026-08-26：新增未开始直播课删除接口

Completed:

- 新增 `DELETE /live-courses/{courseId}`。
- 新增 `LIVE_COURSE_DELETE_FORBIDDEN` 错误码，区分课程不存在和课程已经开始。
- Repository 使用 `status=0 AND del_flag=1` 条件原子逻辑删除课程。
- 课程删除成功后，在同一事务中删除对应的 `ai4_class_live_course` 关联。
- 更新接口文档、项目上下文和技术决策文档。
- 已同步到 ApiPost `frozen的团队 / AI4.0后台` 的班级直播课目录，节点 ID 为 `6c38d6cd0c70000`。

Verification:

- 执行 Maven `-DskipTests compile`，结果为 `BUILD SUCCESS`，共编译291个主源码文件。
- 本轮未连接数据库执行真实删除，部署后需分别使用状态0和状态1课程验证成功及拒绝分支。

## 2026-08-26：完善学员账户分页列表

Completed:

- 将 `GET /accounts` 列表响应收敛为学员管理页面需要的九个字段。
- 关联 `ai4_class_student`、`ai4_class` 和 `ai4_course_package`，返回当前班级名称和课包名称。
- 沿用旧项目规则动态返回“正式/普通”身份，不新增冗余身份字段。
- 使用 `ROW_NUMBER()` 选取最近一条有效班级关系，避免同一学员多条关系造成重复分页记录。
- 更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`、项目上下文和技术决策文档。

Verification:

- 执行 Maven `-DskipTests compile`，结果为 `BUILD SUCCESS`，共编译291个主源码文件。
- 本轮未连接开发数据库执行真实列表查询；部署前需使用有班级和无班级学员分别验证“正式/普通”分支。

## 2026-08-26：精简新增学员账户接口

Completed:

- 将 `CreateAccountRequest` 从完整账户字段精简为 `phone`、`password` 两个必填字段。
- `AccountService` 接入现有 `SnowflakeIdGenerator` 生成学员 `userId`。
- 修复实际 `el_user.id` 无默认值导致的新增失败：保存 SQL 显式写入 `id`，并与 `user_id` 共用生成的雪花 ID。
- 手机号同时保存为账号和手机号；昵称、状态、来源、删除标识和注册时间由后端补齐。
- 更新接口文档、项目上下文和技术决策文档。
- 已替换 ApiPost 中旧的新增账户节点，新节点名称为“新增学员账户”，节点 ID 为 `6c3d605b7872000`。

Verification:

- 执行 Maven `-DskipTests compile`，结果为 `BUILD SUCCESS`，共编译291个主源码文件。
- 本轮未连接数据库执行真实新增；部署前需验证正常创建和重复手机号两个分支。

## 2026-08-27：新增学员详情接口

Completed:

- 新增 `GET /accounts/{userId}` 及专用响应 DTO、Repository 查询模型。
- 使用窗口函数分别选择最近有效班级和该班级最新一节状态0、1、2、3的直播课。
- 状态0未开始课程不判断学员出勤，`attended` 返回 `null`。
- 返回项目名称作为课程进度，并关联教师和学员考勤记录。
- 普通学员或没有已开始课程时保留基础信息，相关字段返回 `null`。
- 修正直播课详情在状态2拖堂期间实际时长错误返回0的问题。
- 更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`、项目上下文和技术决策文档。
- 已同步到 ApiPost 账户管理目录，当前节点 ID 为 `6c3dd9d73c72000`。

Verification:

- 执行 Maven `-DskipTests compile`，结果为 `BUILD SUCCESS`，共编译293个主源码文件。
- 本轮未连接数据库执行真实详情 SQL；需分别验证普通学员、正式但无已开始课程学员、有出勤记录学员三种情况。

## 2026-08-28：接入Coze题前引导工作流

Completed:

- 读取导出的 Coze 工作流包，确认工作流 ID `7678625341916659718`、4个输入字段和 `output` 输出字段。
- 新增可选启用的 Coze OAuth JWT 配置、通用工作流客户端以及同步、异步、执行记录查询能力。
- 新增 `POST /questions/before-guide/generate` 及请求响应 DTO。
- 生成前查询项目介绍和当前项目沙盘属性名称，将选项的属性 ID 转换为名称后提交工作流。
- 生成结果只返回前端，不自动写入题目表。
- 修复 Spring Boot 4 使用 Jackson 3 导致的 `ObjectMapper` 类型不匹配。
- 修复 Coze SDK 与项目 JJWT 0.13.0 的运行时冲突，将三项 JJWT 依赖统一到0.11.5并调整登录 JWT 等价 API。
- 更新唯一接口文档、项目上下文、技术决策和服务器进度文档。

Verification:

- 执行 `mvn.cmd -q -DskipTests clean compile`，结果为 `BUILD SUCCESS`。
- Maven 依赖树确认 `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 均为0.11.5，Coze SDK 为0.4.2。
- 本地调用已越过 OAuth JWT 创建阶段，JJWT `NoSuchMethodError` 已修复；完整工作流输出仍需使用已发布工作流继续验证。

## 2026-08-28：新增题目引导组合生成接口

Completed:

- 读取选项引导工作流，确认工作流 ID `7678645700088021046`，输出字段为 `output`。
- 新增 `POST /questions/guides/generate` 及组合请求、响应 DTO。
- 后端并行调用1次题前引导工作流和每个选项1次选项引导工作流。
- 选项引导的 `user_answer` 每次只传当前选项；响应返回 `optionIndex`、`optionText`、`afterGuide`。
- 新增 `COZE_QUESTION_AFTER_GUIDE_WORKFLOW_ID` 配置，并更新接口文档和服务器配置记录。
- 已同步到 ApiPost 题目管理目录，节点 ID 为 `6c60d4fb7470000`。

Verification:

- 执行 `mvn.cmd -q -DskipTests clean compile`，结果为 `BUILD SUCCESS`。
- 尚未使用真实工作流完成组合接口联调；部署前需验证2个和4个选项两种请求。

## 2026-08-31：优化定时任务与Coze线程池

Completed:

- 确认声网定时任务线程池配置为3个线程，并使用 `ai4-scheduling-` 日志前缀。
- 新增Spring管理的 `cozeGuideExecutor`，固定10个线程、队列容量50。
- `QuestionGuideGenerationService` 改为注入并复用共享线程池，不再按请求创建和关闭线程池。
- 线程池饱和时使用 `CallerRunsPolicy`，避免任务静默丢失并对调用方形成反压。

Verification:

- 执行 `mvn.cmd -q -DskipTests clean compile`，结果为 `BUILD SUCCESS`。

## 2026-08-31：复用OSS客户端并加固直播录制锁

Completed:

- 新增Spring管理的延迟初始化OSS客户端，第一次上传后由所有上传业务复用。
- OSS客户端改为应用关闭时统一释放，不再为每个文件重复创建和关闭连接资源。
- 直播录制Redis锁改用随机UUID作为锁值。
- 使用Lua脚本完成“锁值一致才删除”，避免旧持有者误删过期后重新建立的锁。
- 保持现有上传接口、录制流程和60秒锁TTL不变。

Verification:

- 执行 `mvn.cmd -q -DskipTests compile`，结果为 `BUILD SUCCESS`。

## 2026-09-04：放开 Skill 与合伙人名称重复限制

Completed:

- 注释 Skill 和合伙人新增、修改时的名称唯一预检查，保留原校验方法供后续恢复。
- 取消 Service 对名称唯一索引异常的转换，保留相关 Repository 方法和错误码。
- 保持合伙人 Skill ID 去重、有效性校验、并发锁和乐观锁逻辑不变。
- 明确数据库需手动删除 Skill、合伙人名称唯一索引，生成列暂时保留。

Verification:

- 执行 `mvn.cmd -q -DskipTests compile`，结果为 `BUILD SUCCESS`。
## 2026-09-05：统一客户端用户密码处理

- 已核对管理后台与 `D:\Dev\Projects\ai4-server` 的密码流程，确认后台 BCrypt 与客户端服务 SM4 不兼容。
- 已新增 Account 内部服务 `RestClient`、配置属性和内部请求/响应 DTO。
- 已将 Account 创建用户、重置密码改为调用 `ai4-server` 内部接口。
- 已删除 Account Repository 中直接新增 `el_user`、直接修改 `el_user.password` 的代码和不再使用的 `Account` 模型。
- Account 列表与详情查询保持不变，前端接口路径和主要响应结构保持不变。
- 已补充客户端服务未配置、内部认证失败、服务不可用和处理失败错误码。
- 已同步 ApiPost“新增学员账户”和“重置学员账户密码”，新节点 ID 分别为 `6d0a20c12870000`、`6d0a21fea072000`。
- 验证：`mvn.cmd -q -DskipTests compile` 执行成功。
- 待联调：启动 `ai4-server` 并配置一致的内部令牌后，验证新增账户、客户端密码登录及重置密码后旧登录态失效。
## 2026-09-10：学员提交文件接口返回公开访问地址

Completed:

- `GET /live-courses/{courseId}/students/{studentId}/submission-files` 响应增加 `fileUrl`。
- 查询通过 `submission_node.file_id` 左关联 `file_resource.file_id`，读取状态为 `ACTIVE` 或 `RETAINED` 的 `public_url`。
- 保留文件快照的审计语义：资源缺失、已清理或没有公开地址时仍返回文件名，`fileUrl` 为 `null`。
- 已同步更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`。

Verification:

- 执行 `mvn.cmd -q -DskipTests compile`，结果成功。

## 2026-09-18：调整公司名称、独立直播课和合伙人删除

Completed:

- 放宽人工修改 AI 公司名称的规则：保留非空、最多 7 个字符和候选名称去重，取消人工修改时的后缀限制；AI 生成规则保持不变。
- 新增手动建课请求字段 `courseStartTime`，使用请求日期和时间生成直播课计划时间，不再读取班级 `start_time`。
- 合伙人删除改为只校验当前 `ai4_project_task_partner` 绑定；有绑定时返回 `PARTNER_IN_USE`，无绑定时将合伙人归档，不清理 Skill、圆桌记录或提交文件。
- 更新接口文档及错误码说明。

Verification:

- 执行 `mvnw.cmd -q -DskipTests clean compile`，结果为 `BUILD SUCCESS`。

## 2026-09-21：新增 CRM 新建学员接口

Completed:

- 新增 `POST /external/aiUser/add`，供 CRM 创建客户端学员账户。
- 复用 `AccountService` 和 `ai-server /internal/users`，没有新增第二套密码保存逻辑。
- 保留 `POST /accounts` 后台接口及 JWT 认证不变。
- CRM 接口由现有 `/external/**` IP 白名单统一保护。
- 更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`、项目上下文和技术决策文档。

Verification:

- 执行 `mvnw.cmd -q -DskipTests compile`，结果成功。
- 待部署后使用 CRM 白名单 IP 验证创建成功、重复手机号和未授权来源三个分支。

## 2026-09-21：新增学员可选昵称

Completed:

- `CreateAccountRequest` 新增非必填 `nick`，最大长度255个字符。
- 普通新增学员和 CRM 新增学员共用该字段，并由 `AccountService` 透传到 `ai-server`。
- 未传或传空时保留服务端随机昵称逻辑。
- 同步更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`。

Verification:

- 执行 `mvnw.cmd -q -DskipTests compile`，结果成功。

## 2026-10-09：项目任务范围 Skill 绑定

Completed:

- 新增项目任务、合伙人、Skill 关联模型和请求/响应 DTO。
- 项目任务新增、修改和查询改为使用 `partners[].partnerId` 与 `partners[].skillIds`。
- 新增任务配置时校验有效合伙人、有效 Skill、合伙人重复和同一合伙人下 Skill 重复。
- 合伙人新增、修改、详情和列表移除 Skill 绑定字段及旧关联表读写逻辑。
- Skill 使用数量和删除保护改为查询 `ai4_project_task_partner_skill`。
- 更新 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`，说明新的请求结构和错误码。
- 保留 `ai4_partner_skill_relation` 作为历史表，未执行删除。

Verification:

- `mvnw.cmd -q -DskipTests compile`：通过。
- `mvnw.cmd -q -DskipTests clean compile`：待本轮最终检查确认。

Pending:

- 前端需要将项目任务请求从 `partnerIds` 改为 `partners`。
- 已有任务若尚未写入 `ai4_project_task_partner_skill`，需要重新配置其 Skill。
