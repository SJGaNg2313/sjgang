# Project Context

## 项目领取任务配置调整：2026-09-28

- 任务图由原来的 `publicTask`、`claimTask` 两张图调整为共用的 `taskImage`。
- 任务配置新增 `taskObjective`、`taskKeywords`、`submissionIcon`、`submissionName` 和 `submissionDescription`。
- `taskKeywords` 保存为 JSON 数组，必须配置 2 至 5 个关键词，每个关键词 2 至 4 个字符。
- 提交内容固定只有一项；图标由用户上传并保存 OSS 地址，名称最多 8 个字符，简介为 1 至 26 个字符。
- “必交”是前端固定展示内容，后台不保存、不配置，也不新增字典项。
- 上传接口改为接收 `taskImage`、`submissionIcon`、`companyCard`；旧数据库列 `public_task`、`claim_task` 暂时保留但停止读写。

## 本地图形验证码：2026-09-23

- 新增公开接口 `GET /auth/captcha`，后台使用 Java AWT 本地生成 PNG 图片并以 Base64 Data URI 返回。
- 验证码答案只保存到 Redis，默认有效期 120 秒；校验成功或失败都会原子消费，不能重复使用。
- 登录请求增加 `captcha_id` 和 `captcha_code`；验证码开关由 `AI4_AUTH_CAPTCHA_ENABLED` 控制，默认开启。
- 不接入腾讯云验证码，不新增数据库表；`/auth/captcha` 与 `/auth/login` 都不需要 JWT。

## CRM 直播课出站同步：2026-09-22

- 新增 CRM 出站客户端，使用 Spring `RestClient` 调用 CRM 提供的两个接口：课包直播课数量同步、班级直播课结束考勤回调。
- 课包数量按当前数据库实时统计有效班级关联的 `live_course.del_flag=1` 直播课；建课、删课、停复课事务提交后异步通知。
- 直播课状态同步为 `3` 后完成本地考勤结算；云端录制完成后使用 `live_course_video.m3u8` 的第一个地址作为 `liveurl` 回调 CRM。
- 结束回调学生数据来源为 `el_user`、`ai4_class_student` 和 `live_user_course_record`；缺勤学生省略进入、退出时间字段。
- 结束回调使用 Redis 成功标记避免重复通知，并定时扫描最近7天已关闭课程进行补偿。
- 默认关闭 CRM 出站同步；启用时配置 `AI4_CRM_CALLBACK_ENABLED=true`、`AI4_CRM_CALLBACK_BASE_URL` 以及超时参数。

## Account 写操作改由客户端服务负责：2026-09-05

- 后台公开接口路径和请求/响应结构保持不变：`POST /accounts`、`PUT /accounts/{userId}/password`。
- 创建客户端用户改为调用 `ai4-server POST /internal/users`。
- 重置客户端用户密码改为调用 `ai4-server POST /internal/users/reset-password`。
- 两个内部请求均携带 `X-Internal-Token`；服务端通过 `@SaIgnore` 跳过普通登录校验，再校验内部令牌。
- `el_user.password` 由客户端服务按 SM4 规则保存；后台不再使用 BCrypt 写入该字段。
- `GET /accounts` 和 `GET /accounts/{userId}` 继续由后台直接执行只读聚合查询。
- 后台新增环境变量：`AI4_CLIENT_USER_BASE_URL`、`AI4_CLIENT_USER_INTERNAL_TOKEN`、`AI4_CLIENT_USER_CONNECT_TIMEOUT_SECONDS`、`AI4_CLIENT_USER_READ_TIMEOUT_SECONDS`。
- `AI4_CLIENT_USER_INTERNAL_TOKEN` 必须与客户端服务的 `ELEPHANT_INTERNAL_USER_API_TOKEN` 一致。

## CRM 新建学员入口：2026-09-21

- 新增 CRM 专用接口 `POST /external/aiUser/add`。
- 接口请求仍只接收 `phone`、`password`，复用 `AccountService.createAccount`。
- CRM 接口不需要后台 JWT，由 `/external/**` 的 CRM IP 白名单保护。
- 创建学员后，CRM 仍需单独调用 `/external/aiClass/syncStudents` 绑定班级。
- 原有 `POST /accounts` 后台接口保持不变。


## ApiPost 接口项目范围：2026-07-23

- 当前项目对应的 ApiPost 项目为 `frozen的团队 / AI4.0后台`。
- ApiPost 分支项目 ID：`67b328d3446d000`。
- 后续查询、核对和同步 ApiPost 接口时，只处理该项目，不关注用户账号下的其他 ApiPost 项目。

## 服务账户表统一改为 ai4_user：2026-07-16

- 新增 Flyway `V6__rename_el_user_to_ai4_user.sql`，将 `el_user` 重命名为 `ai4_user`。
- 已成功执行的 V3 保持不变，避免修改 Flyway 历史和校验和。
- 账户 Repository、集成测试和接口文档均已切换到 `ai4_user`。
- 新环境会先通过 V3 创建 `el_user`，再由 V6 保留数据完成重命名。

## 课程表统一改为 ai4 前缀：2026-07-15

- 已新增 Flyway `V5__rename_course_tables_to_ai4_prefix.sql`，将课程相关表统一重命名为：
  - `ai4_course_package`
  - `ai4_lesson`
  - `ai4_lesson_content`
- V4 已在测试库执行过，因此保留 V4 原始内容和校验和；表名调整通过新增 V5 完成。
- 所有课程 Repository SQL 和课程集成测试均已切换到新表名。
- `CoursePackageController` 与 `CourseLessonController` 的所有接口方法已补充精简中文注释。
- 测试库已成功从 V4 升级至 V5，完整测试结果为 `8` 个全部通过。
- 服务器当前运行程序但未看到课程表，需要在重新部署前确认：运行中的 JAR 是否包含 V4/V5、systemd 实际数据库 URL，以及服务器库的 `flyway_schema_history` 当前版本。

## 课包与课节业务模块：2026-07-13

- 已新增独立业务包 `com.ai4.course`，实现课包、课节以及课节内容只读/复制所需链路。
- 已新增 Flyway `V4__create_course_tables.sql`，创建：
  - `edu2_ai_course_package`
  - `edu2_ai_lesson`
  - `edu2_ai_lesson_content`
- 三张表的主外键统一使用 `BIGINT`；逻辑删除统一为 `1=正常、2=删除`。
- 课包和课节列表均使用 SQL `COUNT + LIMIT/OFFSET` 真分页，默认每页 10 条，最大 100 条。
- 课包、课节列表均按 `sort_order ASC, id ASC` 排序。
- 新课节的默认排序只在目标课包范围内计算。
- 删除课包会在同一事务中软删除其课节和课程内容；删除课节会软删除所属内容。
- 批量导入课节采用事务性浅复制：
  - 创建新课节 ID。
  - 为课程内容创建新内容 ID。
  - 保留原课程内容的 `content_type`、`ref_id`、`cover` 等资源引用。
  - 任一源数据无效或目标课包存在同名课节时整体回滚。
- 课节详情返回 `lesson + contents`，不再只返回课程内容列表。
- 当前暂未迁移参考项目的若依字典和阿里云 OSS 依赖；课节标签直接保存字符串，图标接收 URL。
- 课程接口均由现有 JWT 拦截器保护，统一前缀仍为 `/ai/v4`。
- 最新验证：Flyway 测试库已升级到 V4；完整测试 `8` 个，全部通过。

## 服务账户旧字段兼容与请求错误处理：2026-07-13

- `CreateAccountRequest` 已兼容外部旧服务字段：`user_id`、`source_type`、`open_id`、`devicemodel`、`devicesysver`、`macid`、`resver`、`apkver`、`signuptime`、`login_time`。
- Java 模型和返回前端的 JSON 继续使用规范 camelCase，例如 `devicesysver -> deviceSystemVersion`、`resver -> resourceVersion`。
- `signuptime` 和 `login_time` 使用 `yyyy-MM-dd HH:mm:ss`。
- 已新增 `INVALID_REQUEST`；JSON 结构、字段类型或日期格式无法解析时统一返回 HTTP 400，不再由兜底异常返回 500。

## 服务账户管理模块：2026-07-13

- 已明确区分两类用户：
  - `sys_user`：登录当前管理后台的管理员用户。
  - `el_user`：由外部服务提供业务数据、保存到当前项目数据库并由后台管理的服务账户。
- 已新增 Flyway 迁移 `V3__create_el_user_table.sql`，表结构兼容既有 `el_user` 字段、索引和 `utf8mb4_bin` 排序规则。
- 已新增独立包 `com.ai4.account`，未复用后台用户的 `com.ai4.user` 业务链路。
- 已完成受 JWT 保护的三个接口：
  - `POST /ai/v4/accounts`：新增服务账户。
  - `GET /ai/v4/accounts`：按条件分页查询服务账户。
  - `PUT /ai/v4/accounts/{userId}/password`：重置服务账户密码。
- 创建账户时：
  - `userId` 由外部服务传入。
  - `nick` 未传或为空时保存为“默认用户”。
  - `status` 默认 `1`，`sourceType` 默认 `1`，`delFlag` 固定为 `1`。
  - 明文密码使用现有 `PasswordEncoder` 转换为 BCrypt 哈希后保存。
- 第一版账户列表只查询 `el_user`，不查询课程包、身份或班级等参考项目关联数据。
- 列表响应不返回 `password`、`openId` 和 `delFlag`。
- 当前三个 `/accounts` 接口由 JWT 拦截器保护；外部服务如果需要直接调用新增接口，后续必须增加独立的服务身份认证，不能简单公开放行。
- 最新验证结果：
  - Java 17 编译成功，共编译 42 个源文件。
  - Flyway 已在测试库成功应用 V3。
  - `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
  - 集成测试已覆盖 JWT 登录、账户创建、默认昵称、分页查询、BCrypt 保存和密码重置。

## `/auth/profile` 接口与权限读取推进状态：2026-07-10

- 当前 `/auth/profile` 已存在，业务路径为 `/auth/profile`，真实访问路径为 `/ai/v4/auth/profile`。
- 当前接口进入 Controller 前会先经过 `JwtAuthenticationInterceptor`：
  - 读取 `Authorization` 请求头。
  - 校验 `Bearer <token>` 格式。
  - 调用 `JwtService.parseToken(token)` 解析 JWT。
  - 将解析出的 `CurrentUser` 放入 request attribute。
- `AuthController.profile(...)` 当前通过 `@RequestAttribute` 获取 `CurrentUser`，不再自行解析 Header 或 JWT。
- 当前 `ProfileResponse` 已包含：
  - `userId`
  - `userName`
  - `roles`
  - `permissions`
- 当前缺失环节是：`roles` 和 `permissions` 尚未通过 RBAC 表查询真实数据。
- 已决定先新增认证模块权限读取 Repository：
  - `AuthPermissionRepository`
  - 后续实现 `JdbcAuthPermissionRepository`
- `/auth/profile` 后续目标链路：
  - `CurrentUser.userId()`
  - 查询 `sys_user_role -> sys_role` 得到角色编码。
  - 查询 `sys_user_role -> sys_role -> sys_role_menu -> sys_menu` 得到权限码。
  - 返回真实 `roles` 和 `permissions`。
## RBAC 数据库结构与用户表切换状态：2026-07-10

- 已新增 Flyway 迁移文件：
  - `src/main/resources/db/migration/V2__create_rbac_tables.sql`
- V2 当前负责创建后台管理权限模型的 5 张表：
  - `sys_user`
  - `sys_role`
  - `sys_menu`
  - `sys_user_role`
  - `sys_role_menu`
- 用户主表已从早期 `users` 迁移到 `sys_user`。
- 当前 `sys_user` 核心字段为：
  - `user_id`
  - `user_name`
  - `password`
  - `status`
  - `created_at`
  - `updated_at`
- 当前创建用户接口请求字段已调整为：
  - `user_name`
  - `password`
- 当前创建用户接口响应字段已调整为：
  - `user_id`
  - `user_name`
- `email` 字段已从用户创建链路和 `sys_user` 表中移除，当前没有邮箱需求。
- `JdbcUserRepository` 当前通过 `JdbcTemplate` 操作 `sys_user`，不再操作旧 `users` 表。
- 测试清理逻辑当前清理 `sys_user`，并继续保护只允许清理 `ai4_boot_verify_test`。
- 本地测试库、本地开发库和服务器库均已存在 V2 定义的 5 张 `sys_*` 表。
- 最新测试结果：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

## Flyway 与测试环境状态：2026-07-09

- 已引入：
  - `spring-boot-starter-flyway`
  - `flyway-mysql`
- 已新增首个迁移文件：
  - `src/main/resources/db/migration/V1__create_users_table.sql`
- V1 定义了统一的 `users` 表结构：
  - `BIGINT AUTO_INCREMENT` 主键
  - username 唯一索引 `uk_users_username`
  - `utf8mb4_unicode_ci`
  - password 保存 BCrypt 哈希
- 测试库、开发库和服务器库的 `users` 表结构已统一。
- 三套已有数据库均已通过 Flyway baseline 纳入版本管理。
- `spring.flyway.baseline-on-migrate=false`，不再自动接管来源不明的非空数据库。
- `spring.flyway.validate-on-migrate=true`，启动时校验迁移历史。
- 当前配置中仍保留 `spring.flyway.baseline-version=1`，已有历史表时无实际影响，后续可删除。
- 最新自动化测试：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- 后续数据库结构变化必须新增 V2、V3 等迁移文件，不能修改已纳入历史的 V1。

## 测试数据库隔离状态：2026-07-09

- 已创建独立 MySQL 测试数据库 `ai4_boot_verify_test`。
- 已创建测试专用数据库用户 `ai4_test`。
- 测试类已启用 `@ActiveProfiles("test")`。
- 测试配置位于 `src/test/resources/application-test.properties`。
- 测试数据库密码通过 `AI4_TEST_DB_PASSWORD` 环境变量传入。
- 测试库 `users.id` 已修正为 `BIGINT AUTO_INCREMENT`。
- 最新测试结果：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- 当前测试清理只作用于测试数据库，不再清空默认开发数据库。

## 服务器部署状态：2026-07-09

- 包含 JWT 拦截器和统一 HTTP 401 处理的新版本已部署到开发服务器。
- 后端继续由 systemd 服务 `ai4-backend` 管理。
- 公网 HTTPS 地址继续使用：
  - `https://ai-manage-dev.xxaisys.com/ai/v4`
- 用户已通过 ApiPost 验证当前接口，结果正常：
  - `POST /users`
  - `POST /auth/login`
  - `GET /auth/profile`
- JWT 拦截器在服务器环境中已验证可用，登录接口和用户创建接口按配置放行，个人信息接口需要 Bearer Token。
- 部署前已提醒备份旧 jar 和数据库；具体备份文件是否完成需要以服务器实际文件为准。

## 当前认证与代码状态：2026-07-09

- 已新增 `JwtAuthenticationInterceptor`，在受保护请求进入 Controller 前校验 Bearer Token。
- 已新增 `WebMvcConfig` 注册拦截器。
- 当前公开路径：
  - `/auth/login`
  - `/users`
  - `/actuator/health`
  - `/error`
- `/auth/profile` 已改为读取拦截器放入 request 的 `CurrentUser`，不再自行解析 Header 和 JWT。
- JWT 认证失败统一返回 HTTP 401：
  - `UNAUTHORIZED`
  - `INVALID_TOKEN`
  - `TOKEN_EXPIRED`
- `InMemoryUserRepository` 已删除，当前唯一用户数据实现为 `JdbcUserRepository`。
- 当前 `ProfileResponse.roles` 和 `permissions` 仍为空列表，角色权限尚未接入数据库。
- 当前测试直接使用默认数据源，并在每个测试前执行 `delete from users`；测试数据库尚未隔离。
- `application.properties` 仍含本地数据库密码和开发 JWT secret 默认值，部署环境必须使用环境变量覆盖。

## 当前项目命名：2026-07-08

- 正式后端项目名已从学习验证阶段的 `ai4-boot-verify` 调整为 `ai4-admin-backend`。
- Maven 坐标当前为：
  - `groupId`: `com.ai4`
  - `artifactId`: `ai4-admin-backend`
  - `version`: `0.0.1-SNAPSHOT`
- Spring Boot 应用名当前为：
  - `spring.application.name=ai4-admin-backend`
- 打包产物名称后续会从 `ai4-boot-verify-0.0.1-SNAPSHOT.jar` 变为 `ai4-admin-backend-0.0.1-SNAPSHOT.jar`。
- 服务器部署时仍然可以统一重命名为 `/opt/ai4/backend/app.jar`，因此 systemd 的 `ai4-backend` 服务名暂不需要修改。
- 本次只调整 Maven 项目名和文档，不移动 Java 包名，不重命名启动类：
  - 当前启动类仍为 `src/main/java/com/ai4/verify/Ai4BootVerifyApplication.java`
  - 当前包扫描仍由 `@SpringBootApplication(scanBasePackages = "com.ai4")` 覆盖
- 后续如果要继续整理代码命名，可以单独评估是否把 `com.ai4.verify` 和启动类名称迁移为更正式的 `com.ai4.admin` 相关命名。

## 当前实现进度：2026-07-06 HTTPS 配置完成

- 当前开发域名：
  - `ai-manage-dev.xxaisys.com`
- 当前 HTTPS 已通过 Certbot / Let's Encrypt / Nginx 配置完成。
- 当前证书路径：
  - 证书链：`/etc/letsencrypt/live/ai-manage-dev.xxaisys.com/fullchain.pem`
  - 私钥：`/etc/letsencrypt/live/ai-manage-dev.xxaisys.com/privkey.pem`
- 当前证书到期时间：
  - `2026-10-04`
- Certbot 已配置后台自动续期任务。
- 用户已通过 ApiPost 使用 HTTPS 成功创建用户。
- 当前推荐接口访问方式：
  - `https://ai-manage-dev.xxaisys.com/ai/v4/users`
  - `https://ai-manage-dev.xxaisys.com/ai/v4/auth/login`
- 当前 HTTP 到 HTTPS 的跳转由 Nginx/Certbot 配置管理。

## 后续 JWT 配置注意事项：2026-07-06

- JWT 用于替换当前登录接口返回的 `mock-token`，让后端能够识别“登录后的请求属于哪个用户”。
- 第一版 JWT 目标应保持最小闭环：
  - 登录成功生成真实 JWT。
  - JWT 中只放必要信息，例如 `userId`、`username`、签发时间、过期时间。
  - 新增一个最小受保护接口，例如 `GET /ai/v4/auth/me`。
  - 不带 token 访问受保护接口应返回统一未认证错误。
  - 带正确 token 访问受保护接口应返回当前用户信息。
- JWT payload 可以被 Base64 解码查看，不允许放密码、验证码、数据库密码、身份证号等敏感信息。
- JWT secret 必须来自环境变量，不允许写死在代码中。
- 服务器 systemd 中后续需要增加类似配置：
  - `AI4_JWT_SECRET`
  - `AI4_JWT_EXPIRE_SECONDS`
- 同一环境下所有后端实例必须使用同一个稳定的 JWT secret，否则旧 token 会在重启或切换实例后失效。
- JWT 相关错误也必须返回统一 `ApiResponse` 结构，不能返回 HTML、纯字符串或框架默认错误页。
- 第一版暂不做复杂多端登录、refresh token、token 黑名单、强制踢下线、权限角色体系。

## 当前实现进度：2026-07-03 接入 BCrypt 密码哈希

- 当前 `PasswordEncoder` 已从 `{noop}` 占位实现替换为 BCrypt。
- 新增依赖：
  - `org.springframework.security:spring-security-crypto`
- 当前密码处理行为：
  - 创建用户时：`PasswordEncoder.encode(rawPassword)` 生成 BCrypt 哈希后保存到 `users.password`。
  - 登录时：`PasswordEncoder.matches(rawPassword, encodedPassword)` 判断用户输入的明文密码是否匹配数据库中的 BCrypt 哈希。
- `UserService` 和 `AuthService` 的业务流程保持不变，仍然只依赖项目自己的 `PasswordEncoder` 组件。
- 已执行 `.\mvnw.cmd test`，结果为：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`
- 注意：切换 BCrypt 后，旧数据库中 `{noop}` 开头的密码不能再登录。开发环境建议清空旧测试用户，重新通过 `POST /ai/v4/users` 创建用户。

## 当前实现进度：2026-07-03 域名 HTTP 访问验证通过

- 当前开发域名已解析到服务器：
  - `ai-manage-dev.xxaisys.com -> 39.106.177.254`
- Nginx 已按域名转发到本机 Spring Boot：
  - public HTTP 80
  - `proxy_pass http://127.0.0.1:8080`
- 用户已通过 ApiPost 验证域名接口访问正常。
- 当前域名接口示例：
  - `http://ai-manage-dev.xxaisys.com/ai/v4/users`
  - `http://ai-manage-dev.xxaisys.com/ai/v4/auth/login`
- 当前仍未配置 HTTPS。

## 当前实现进度：2026-07-03 删除临时验证接口

- 临时验证接口已经从代码中删除：
  - `GET /ai/v4/verify/database`
  - `POST /ai/v4/verify/validation`
- 已删除的源码文件：
  - `src/main/java/com/ai4/verify/database/DatabaseVerifyController.java`
  - `src/main/java/com/ai4/verify/validation/ValidationController.java`
  - `src/main/java/com/ai4/verify/validation/ValidationRequest.java`
- 当前正式保留的业务接口：
  - `POST /ai/v4/users`
  - `POST /ai/v4/auth/login`
- 参数校验、统一响应、业务异常、数据库访问和登录链路现在都通过正式业务接口验证，不再依赖临时 `/verify/*` 接口。
- 当前自动化测试数量为 6 个，已执行 `.\mvnw.cmd test`：
  - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`
- 注意：本地代码已经删除临时接口；服务器上如果仍运行旧 jar，需要重新打包、上传并重启 `ai4-backend` 后，公网环境才会同步删除这些接口。

## 当前实现进度：2026-07-03 systemd 后台运行与 Nginx 公网访问

- 后端已从前台 `java -jar app.jar` 运行，推进到 systemd 后台服务运行。
- 当前后端服务由 systemd 管理，服务名为：
  - `ai4-backend`
- 后续常用管理命令：
  - `sudo systemctl start ai4-backend`
  - `sudo systemctl stop ai4-backend`
  - `sudo systemctl restart ai4-backend`
  - `sudo systemctl status ai4-backend`
- systemd service 中配置了后端运行所需环境变量：
  - `AI4_DB_URL`
  - `AI4_DB_USERNAME`
  - `AI4_DB_PASSWORD`
- 后端日志规划写入：
  - `/opt/ai4/logs/backend.log`
  - `/opt/ai4/logs/backend-error.log`
- Nginx 已配置反向代理，将公网 80 端口请求转发到本机 Spring Boot 8080。
- 阿里云安全组已开放 80 端口。
- 当前本地 PowerShell 和 ApiPost 已可以通过公网访问后端接口：
  - `http://39.106.177.254/ai/v4/verify/database`
  - `http://39.106.177.254/ai/v4/users`
  - `http://39.106.177.254/ai/v4/auth/login`
- 当前公网访问链路为：
  - `本地/ApiPost -> 阿里云安全组 80 -> Nginx -> 127.0.0.1:8080 -> Spring Boot -> MySQL`
- 当前不需要开放 Spring Boot 的 8080 公网端口，也不需要开放 MySQL 的 3306 公网端口。
- 当前尚未完成：
  - 域名绑定
  - HTTPS
  - 正式 JWT
  - 正式密码加密方案
  - 去除或限制临时验证接口

## 当前实现进度：2026-07-02 服务器前台部署验证通过

- 后端 jar 已上传到服务器目录：
  - `/opt/ai4/backend/app.jar`
- 服务器目录规划当前为：
  - `/opt/ai4/backend`：后端服务
  - `/opt/ai4/frontend`：前端服务
  - `/opt/ai4/backup`：备份
  - `/opt/ai4/logs`：日志
- 服务器已安装并使用 MySQL。
- 服务器 MySQL 中已创建：
  - 数据库：`ai4_boot_verify`
  - 表：`users`
  - 后端数据库用户：`ai4_app`
- 后端服务已通过环境变量连接服务器 MySQL：
  - `AI4_DB_URL`
  - `AI4_DB_USERNAME`
  - `AI4_DB_PASSWORD`
- 当前服务器前台启动验证已通过。
- 已在服务器本机通过 curl 验证：
  - `GET /ai/v4/verify/database`
  - `POST /ai/v4/users`
  - `POST /ai/v4/auth/login`
- 当前结论：
  - 服务器 Spring Boot 服务可以启动。
  - 服务器后端可以连接服务器 MySQL。
  - 数据库验证接口通过。
  - 创建用户接口通过。
  - 登录接口通过。
- 当前尚未完成：
  - systemd 后台运行
  - 开机自启
  - Nginx 反向代理
  - 公网安全组/防火墙访问验证
  - HTTPS/域名配置

## 当前实现进度：2026-07-02 登录接口自动化测试已补充

- 当前登录接口已补充 MockMvc 自动化测试：
  - `loginWithCorrectPasswordReturnsSuccessResponse`
  - `loginWithWrongPasswordReturnsBusinessErrorResponse`
- 登录成功测试流程：
  - 先调用 `POST /ai/v4/users` 创建用户，准备真实数据库数据。
  - 再调用 `POST /ai/v4/auth/login` 使用正确密码登录。
  - 断言统一成功响应：
    - `code = "0"`
    - `message = "success"`
    - `data.token` 存在
    - `data.tokenType = "Bearer"`
    - `data.expireIn = 7200`
- 密码错误测试流程：
  - 先调用 `POST /ai/v4/users` 创建用户。
  - 再调用 `POST /ai/v4/auth/login` 使用错误密码登录。
  - 断言统一业务错误响应：
    - HTTP 400
    - `code = "INVALID_USERNAME_OR_PASSWORD"`
    - `data = null`
- 当前测试数据隔离仍由 `@BeforeEach cleanDatabase()` 完成，每个测试前清空 `users` 表。
- 已执行 `.\mvnw.cmd test`，当前结果：
  - `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`
- 当前用户创建和登录的最小接口模板已经具备自动化测试保护。

## 当前实现进度：2026-07-02 登录错误码修复后状态

- `AuthService` 中登录失败错误码引用已修复：
  - 当前使用 `ErrorCode.INVALID_USERNAME_OR_PASSWORD`
  - 不再使用拼写错误的 `INVALID_USRENAME_OR_PASSWORD`
- 当前 `.\mvnw.cmd test` 已重新执行通过：
  - `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`
- 当前自动化测试覆盖范围仍是 7 个已有 MockMvc 测试：
  - Spring 上下文加载
  - 校验示例接口成功
  - 校验示例接口缺少 username
  - 校验示例接口 email 格式错误
  - 创建用户成功
  - 创建用户缺少 username
  - 重复 username 业务错误
- 当前自动化测试文件尚未补登录成功和密码错误测试。
- 用户已通过 ApiPost 手动验证：
  - 登录成功场景通过
  - 密码错误场景通过
- 当前登录链路可以视为已完成最小手动验证：
  - 请求进入 `AuthController`
  - `AuthService` 查询数据库用户
  - `PasswordEncoder.matches(...)` 校验密码
  - 成功返回统一结构和 mock token
  - 密码错误返回统一业务错误结构
- 后续建议优先补登录接口自动化测试，避免只依赖 ApiPost 手动验证。

## 当前实现进度：2026-07-02 数据库接入与登录链路检查

- 当前项目已从内存 Repository 推进到真实 MySQL/JDBC Repository。
- 当前 `application.properties` 已配置数据库连接：
  - `spring.datasource.url=${AI4_DB_URL:jdbc:mysql://localhost:3306/ai4_boot_verify?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}`
  - `spring.datasource.username=${AI4_DB_USERNAME:root}`
  - `spring.datasource.password=${AI4_DB_PASSWORD:...}`
- 当前数据库名为 `ai4_boot_verify`，用户表为 `users`。
- `JdbcUserRepository` 当前作为 Spring Bean 生效，负责操作 MySQL `users` 表。
- `InMemoryUserRepository` 当前保留源码，但 `@Repository` 已注释，不再作为 Spring Bean 注入，避免和 `JdbcUserRepository` 同时实现 `UserRepository` 后产生 Bean 冲突。
- 当前用户创建链路为：
  - `POST /ai/v4/users`
  - `UserController -> UserService -> UserRepository(JdbcUserRepository) -> MySQL users`
- 当前密码处理仍是最小占位实现：
  - 创建用户时保存 `{noop}` 前缀密码。
  - 登录校验时通过 `PasswordEncoder.matches(rawPassword, encodedPassword)` 比较。
- 当前已新增登录相关文件：
  - `src/main/java/com/ai4/auth/dto/LoginRequest.java`
  - `src/main/java/com/ai4/auth/dto/LoginResponse.java`
  - `src/main/java/com/ai4/auth/service/AuthService.java`
  - `src/main/java/com/ai4/auth/controller/AuthController.java`
- 登录接口业务路径为 `/auth/login`，真实访问路径为：
  - `POST /ai/v4/auth/login`
- 当前登录设计链路为：
  - `AuthController` 接收登录 JSON 并触发 `@Valid` 校验。
  - `AuthService` 根据 `username` 调用 `userRepository.findByUsername(...)` 查询数据库。
  - 用户不存在或密码错误时抛出 `BizException(ErrorCode.INVALID_USERNAME_OR_PASSWORD)`。
  - 成功时暂时返回 mock token，后续再替换为真实 JWT。
- 当前测试类已经加入数据库测试数据隔离：
  - `@BeforeEach cleanDatabase()`
  - 每个测试前执行 `delete from users`
- 2026-07-02 重新执行 `.\mvnw.cmd test` 的真实结果为编译失败：
  - `AuthService.java` 当前引用了拼写错误的 `ErrorCode.INVALID_USRENAME_OR_PASSWORD`
  - `ErrorCode.java` 中真实存在的是 `INVALID_USERNAME_OR_PASSWORD`
- 因此当前下一步应先修正 `AuthService` 中错误码引用，再补登录成功和密码错误的 MockMvc 测试。

## 当前实现进度：2026-07-01 Repository 接口与接口契约对齐

- 当前 IDEA 项目 JDK 按用户确认配置为 21，但本次命令行 `.\mvnw.cmd test` 输出显示 Maven 实际使用 `Java 17.0.12`；`pom.xml` 中的语言版本仍固定为 Java 17：
  - `<java.version>17</java.version>`
  - 这表示当前代码按 Java 17 语言级别编译，暂不使用 Java 21 专属语言特性。
- 统一成功响应已对齐前后端接口契约：
  - `ApiResponse.success(data)` 当前返回 `code = "0"`。
  - `ApiResponse.success(data)` 当前返回 `message = "success"`。
- 参数校验字段错误结构已对齐接口契约：
  - `FieldErrorItem` 字段为 `field` 和 `reason`。
- 用户模块已新增仓储接口：
  - `src/main/java/com/ai4/user/repository/UserRepository.java`
- 当前数据访问关系已调整为：
  - `UserService -> UserRepository`
  - `InMemoryUserRepository implements UserRepository`
- 当前仍然使用内存实现保存用户；后续接 MySQL / MyBatis 时，应新增数据库实现并复用 `UserRepository` 接口。
- 已执行 `.\mvnw.cmd test`，结果为 7 个测试全部通过。

## 当前实现进度（2026-07-01 密码处理）

- 用户模块已新增密码处理组件：
  - `src/main/java/com/ai4/user/security/PasswordEncoder.java`
- 当前 `PasswordEncoder` 使用最小占位实现：
  - `encode(rawPassword)` 返回 `"{noop}" + rawPassword`
- `UserService` 已注入 `PasswordEncoder`。
- 创建用户时的数据流变为：
  - `request.password()`
  - `passwordEncoder.encode(request.password())`
  - `encodedPassword`
  - `userRepository.save(username, email, encodedPassword)`
- 当前响应仍使用 `CreateUserResponse`，不会返回 `password`。
- 当前密码处理仍不是正式安全加密，只是为了固定分层位置；后续可替换为 BCrypt 等正式方案。

## 当前实现进度（2026-07-01 内存 Repository）

- 用户模块已引入最小内存 Repository：
  - `src/main/java/com/ai4/user/model/User.java`
  - `src/main/java/com/ai4/user/repository/InMemoryUserRepository.java`
- 当前用户创建链路为：
  - `UserController -> UserService -> InMemoryUserRepository`
- `InMemoryUserRepository` 当前使用：
  - `ConcurrentHashMap<String, User>` 按用户名保存用户
  - `AtomicLong` 生成内存用户 ID
- `UserService.createUser` 当前逻辑：
  - 通过 Repository 判断用户名是否已存在
  - 已存在时抛出 `BizException(ErrorCode.USERNAME_ALREADY_EXISTS)`
  - 不存在时保存用户
  - 返回不包含 `password` 的 `CreateUserResponse`
- 重复用户名测试已改为真实重复创建：
  - 第一次创建 `duplicate` 成功
  - 第二次创建同名 `duplicate` 返回 `USERNAME_ALREADY_EXISTS`
- 已执行 `.\mvnw.cmd test`，当前 7 个测试全部通过。

## 编码约定（2026-07-01）

- 项目文件统一按 UTF-8 保存。
- 已新增 `.editorconfig`，约束文本文件使用 `charset = utf-8`。
- 已在 `pom.xml` 中声明：
  - `project.build.sourceEncoding=UTF-8`
  - `project.reporting.outputEncoding=UTF-8`
- 通过字符码点检查确认，当前 Java 源文件中的中文注释和校验文案实际是正常中文。
- 当前 PowerShell 工具输出仍可能把 UTF-8 中文显示为乱码；该问题属于显示层问题，不代表源码内容一定损坏。

## 当前实现进度（2026-07-01）

- 用户模块已引入最小 Service 层：
  - `src/main/java/com/ai4/user/service/UserService.java`
- `UserController` 当前只负责：
  - 接收 `POST /ai/v4/users`
  - 触发 `@Valid @RequestBody` 参数校验
  - 调用 `UserService.createUser(request)`
  - 使用 `ApiResponse.success(response)` 返回统一响应
- `UserService.createUser` 当前负责：
  - 模拟用户名重复的业务异常：当 `username` 为 `existing` 时抛出 `BizException(ErrorCode.USERNAME_ALREADY_EXISTS)`
  - 将 `CreateUserRequest` 转换为不包含 `password` 的 `CreateUserResponse`
- 当前已验证业务异常链路：
  - `Controller -> UserService -> BizException -> GlobalExceptionHandler -> ApiResponse`
- 当前 MockMvc 测试数量为 7 个，已执行 `.\mvnw.cmd test`，结果全部通过。

## 当前实现进度（2026-06-30 晚）

- 用户模块最小创建接口已完成当前阶段闭环。
- `POST /ai/v4/users` 当前行为：
  - 请求体使用 `CreateUserRequest` 接收 `username`、`email`、`password`。
  - Controller 方法参数使用 `@Valid @RequestBody CreateUserRequest request`，触发 JSON 反序列化和参数校验。
  - 成功响应使用 `CreateUserResponse` 返回 `username`、`email`。
  - 响应中不返回 `password`。
  - 外层统一使用 `ApiResponse<CreateUserResponse>`。
- 用户模块当前相关文件：
  - `src/main/java/com/ai4/user/dto/CreateUserRequest.java`
  - `src/main/java/com/ai4/user/dto/CreateUserResponse.java`
  - `src/main/java/com/ai4/user/controller/UserController.java`
- 当前 MockMvc 测试已覆盖：
  - Spring 上下文加载。
  - `POST /ai/v4/verify/validation` 成功。
  - `POST /ai/v4/verify/validation` 缺少 `username`。
  - `POST /ai/v4/verify/validation` 非法 `email`。
  - `POST /ai/v4/users` 成功，并断言响应中不存在 `password`。
  - `POST /ai/v4/users` 缺少 `username` 返回 `VALIDATION_FAILED`。
- 已执行 `.\mvnw.cmd test`，结果为 6 个测试全部通过。

## 当前实现进度（2026-06-30）

- 后端接口统一前缀已确定为 `/ai/v4`。
- 当前通过 `src/main/resources/application.properties` 配置统一前缀：
  - `server.servlet.context-path=/ai/v4`
- Controller 中只保留业务路径，不手动写 `/ai/v4`：
  - `ValidationController`: `@PostMapping("/verify/validation")`
  - `UserController`: `@PostMapping("/users")`
- 当前真实访问路径：
  - `POST /ai/v4/verify/validation`
  - `POST /ai/v4/users`
- MockMvc 测试需要同时写完整请求路径和 `contextPath`：
  - `post("/ai/v4/verify/validation").contextPath("/ai/v4")`
- 当前 `UserController` 已存在最小创建用户接口，返回类型仍是 `ApiResponse<CreateUserRequest>`，后续需要改为 `ApiResponse<CreateUserResponse>`，避免返回 `password`。
- 用户模块当前已有：
  - `src/main/java/com/ai4/user/dto/CreateUserRequest.java`
  - `src/main/java/com/ai4/user/controller/UserController.java`
- 下一步优先补 `POST /ai/v4/users` 的 MockMvc 测试，再新增 `CreateUserResponse`。

## 当前实现进度（2026-06-29）

- `ApiResponse.validationFailed(data)` 已改为从 `ErrorCode.VALIDATION_FAILED` 读取错误码和默认文案。
- 当前校验失败响应仍保持原契约不变：
  - `code`: `VALIDATION_FAILED`
  - `message`: `请求参数校验失败`
  - `data.fieldErrors`: 字段错误列表
  - `traceId`: 当前为 `null`
- 已使用 `.\mvnw.cmd test` 验证通过，当前测试结果为 4 个测试全部通过。

## 当前实现进度（2026-06-26）

- 应用入口使用 `@SpringBootApplication(scanBasePackages = "com.ai4")`，因此 `com.ai4.common` 下的全局异常处理可被 Spring 扫描。
- `POST /verify/validation` 在请求通过校验时返回 `ApiResponse<ValidationRequest>`：
  - `code`: `SUCCESS`
  - `message`: `请求成功`
  - `data`: 原请求中的 `username` 和 `email`
  - `traceId`: 当前为 `null`
- 请求校验失败时，`GlobalExceptionHandler` 返回 `400 BAD_REQUEST` 和 `VALIDATION_FAILED` 响应结构。
- `GlobalExceptionHandler` 还处理业务异常和未处理异常：
  - `BizException` 返回 `400 BAD_REQUEST`，错误码与文案来自异常对象。
  - 未处理异常会记录错误日志并返回 `500 INTERNAL_SERVER_ERROR`；该分支通过 `ErrorCode.INTERNAL_ERROR` 获取错误码和文案。
- 当前自动化测试覆盖 Spring 上下文、校验通过后的 `SUCCESS` 响应、缺少 `username`、错误 `email` 格式。

## 项目概览

- 项目名：`ai4-admin-backend`
- 类型：Spring Boot / Maven 后端项目
- Java 版本：17
- Maven 坐标：
  - `groupId`: `com.ai4`
  - `artifactId`: `ai4-admin-backend`
  - `version`: `0.0.1-SNAPSHOT`
- Spring Boot parent：`org.springframework.boot:spring-boot-starter-parent:4.1.0`

## 主要依赖

- `spring-boot-starter-webmvc`
- `spring-boot-starter-actuator`
- `spring-boot-starter-validation`
- 测试依赖：
  - `spring-boot-starter-webmvc-test`
  - `spring-boot-starter-actuator-test`

## 当前代码结构

- 应用入口：`src/main/java/com/ai4/verify/Ai4BootVerifyApplication.java`
- 校验示例接口：
  - `src/main/java/com/ai4/verify/validation/ValidationController.java`
  - `src/main/java/com/ai4/verify/validation/ValidationRequest.java`
- 通用异常：
  - `src/main/java/com/ai4/common/exception/BizException.java`
  - `src/main/java/com/ai4/common/exception/ErrorCode.java`
  - `src/main/java/com/ai4/common/exception/GlobalExceptionHandler.java`
- 通用响应：
  - `src/main/java/com/ai4/common/response/ApiResponse.java`
  - `src/main/java/com/ai4/common/response/FieldErrorItem.java`
  - `src/main/java/com/ai4/common/response/ValidationErrorData.java`
- 测试：
  - `src/test/java/com/ai4/verify/Ai4BootVerifyApplicationTests.java`

## 当前行为

- `POST /verify/validation` 接收 `ValidationRequest`，并使用 Jakarta Bean Validation 校验请求体。
- 校验失败时，`GlobalExceptionHandler` 捕获 `MethodArgumentNotValidException`，返回 `400 BAD_REQUEST`。
- 校验失败响应使用：
  - `code`: `VALIDATION_FAILED`
  - `message`: `请求参数校验失败`
  - `data.fieldErrors`: 字段错误列表
  - `traceId`: 当前为 `null`

## 验证方式

优先使用 Maven 测试验证：

```powershell
.\mvnw.cmd test
```

## 已知注意事项

- 部分 Java 源文件中的中文字符串在当前读取输出中显示为乱码，需要在后续修改时确认文件实际编码和 IDE 编码设置。
- `target/` 是构建输出目录，不应作为项目长期记忆来源。
- `.maven-repository/` 是本地 Maven 缓存相关目录，通常不应手动修改。

## 当前实现进度（2026-07-17：RBAC 权限管理）

- 当前代码中共有 41 个业务接口，唯一接口文档为项目上级目录的 `API_DOCUMENTATION.md`。
- 后台登录用户表为 `sys_user`；服务账户表为 `ai4_user`，两者不能混用。
- RBAC 数据关系为：`sys_user -> sys_user_role -> sys_role -> sys_role_menu -> sys_menu`。
- 已完成角色 CRUD：查询、详情、创建、修改、状态切换、删除。
- 已完成角色菜单授权：查询菜单 ID、整体替换授权、自动补齐父菜单。
- 已完成用户角色授权：查询角色 ID、整体替换角色、禁止分配禁用角色。
- `super_admin` 是数据库中的显式角色，不使用代码绕过鉴权：
  - 不允许删除或禁用；
  - 不允许清空菜单授权；
  - 不允许移除系统最后一个启用超级管理员用户的该角色。
- `/auth/routes` 只返回启用的 `DIR` 和 `MENU`；`/auth/perms` 返回启用的权限码。
- 用户、角色、菜单 Controller 已通过 `@RequirePermission` 声明所需权限。
- `PermissionAuthorizationInterceptor` 在 JWT 拦截器之后执行；缺少权限返回 HTTP 403 和 `FORBIDDEN`。
- Flyway 当前最高版本为 V7：
  - 补齐系统、用户、角色、菜单管理页面；
  - 初始化 14 个 `system:*` 按钮权限；
  - 清理旧权限 `user:list`；
  - 自动给启用的 `super_admin` 授权；
  - 不自动选择某个 `sys_user` 绑定超级管理员角色。
- `test-compile` 已通过，超级管理员和无角色用户的权限行为已人工验证。
- 当前完整 `mvn test` 在助手环境中仍因 `ai4_test@localhost` 密码错误而未完成。

### 当前已知缺口

- 被禁用的后台用户，其已签发 JWT 尚未在每次请求时重新检查 `sys_user.status`，旧 Token 可能持续有效到过期；部署后应优先补该检查。
- 尚未为账户和课程接口设计 `system:*` 业务权限，它们当前只要求 JWT 登录。

## 当前实现进度（2026-07-21：V8-V10、题库、合伙人与字典设计）

- 后台登录用户继续使用 `sys_user`；业务账户继续使用 `ai4_user`，二者职责不可混用。
- RBAC 已完成用户、角色、菜单、角色菜单授权、用户角色授权和接口权限校验；服务器已有账号绑定 `super_admin`。
- 自动化测试已修复测试数据清理顺序和测试管理员角色绑定，最近一次完整结果为：`Tests run: 9, Failures: 0, Errors: 0, BUILD SUCCESS`。测试库密码通过环境变量 `AI4_TEST_DB_PASSWORD` 注入。
- V8 用于题库相关结构，目标表为 `ai4_question_bank` 和 `ai4_question_choice`。V8 曾在执行成功后被修改并产生 checksum mismatch；开发库在确认实际表结构与本地目标一致后修复了历史 checksum。
- V9 用于合伙人相关初始结构，目标表为 `ai4_partner` 和旧结构 `ai4_partner_skill`。
- 合伙人与 Skill 的业务关系已明确为多对多：一个合伙人可绑定多个 Skill，一个 Skill 也可绑定多个合伙人。
- V10 的目标是把旧 `ai4_partner_skill` 重构为独立的 `ai4_skill`，并新增关联表 `ai4_partner_skill_relation`。Skill 文件夹树保存在 `ai4_skill.skill_content` JSON 中；二进制文件只保存对象存储地址或 key，不把 base64 文件内容放入 JSON。
- V10 曾因 SQL 注释格式和 `UPDAET` 拼写错误失败；MySQL DDL 可能部分提交，因此是否成功必须以目标库 `flyway_schema_history` 和实际表结构共同确认，不能只看本地 SQL 文件。
- 当前本地 V8 已修正为只创建 `ai4_question_bank` 和 `ai4_question_choice`；V9 单独创建合伙人相关初始表，主键反引号也已修正。后续仍须遵守已执行迁移不可修改的规则。

### 字典模块设计（尚未实现）

- 采用精简的双表设计：`ai4_dict_type` 保存字典类型，`ai4_dict_item` 保存字典项。
- `ai4_dict_type` 字段：`id`、`dict_name`、`dict_code`、`status`、`remark`、`created_at`、`updated_at`。
- `ai4_dict_item` 字段：`id`、`type_id`、`item_label`、`item_value`、`sort_order`、`status`、`extra`、`remark`、`created_at`、`updated_at`。
- `dict_code` 唯一；同一类型下 `item_value` 唯一；字典项通过 `type_id` 关联类型，不重复保存 `dict_code`。
- `item_value` 使用 `VARCHAR(500)` 保存常规标量；可选的 `extra JSON` 保存扩展信息。
- 不引入若依特有的 `css_class`、`list_class`、`is_default`，暂不引入 `create_by`、`update_by`。
- 当前选择物理删除，因此暂不添加 `del_flag`；删除字典类型前必须先处理其字典项。
- 禁用字典类型后，其下字典项整体不可用；`sort_order` 由业务显式维护，不使用自增排序。
- 数据库是字典真实数据源。只有监控证明查询压力需要优化时，才在 Service 层增加本地缓存或 Redis，并在字典变更后失效缓存。

## 当前实现进度（2026-08-15：班级直播课）

- `GET /live-courses` 已实现房间号、班级、教师、时间范围、状态条件分页查询，并通过 `attendanceCount`、`lateCount` 独立返回出勤和迟到人数。
- `GET /classes/options` 返回有效班级的 `classId/className`，供建课页面选择。
- `POST /live-courses` 接收班级ID、项目ID、开课日期和课节时长，创建直播课并写入班级项目关联。
- AI4 使用 Redis `db11` 和 `ai4:` Key 前缀；普通直播课房间序号 Key 为 `ai4:live:room:seq:1`。
- `room_id` 由 Redis序号生成并作为声网频道名；`course_id` 由雪花ID生成器生成。
- 开课日期与班级 `start_time` 按 `Asia/Shanghai` 组合，教师和直播配置从班级继承。
- 当前阶段没有直播课详情、修改和删除接口，也没有数据库自动化测试。
- 已新增CRM班级同步：`POST /external/aiClass/add` 创建班级并按课包项目自动排课，`POST /external/aiClass/openClose` 处理停课与恢复。
- 自动排课不是定时任务；创建班级时立即按 `courseRule` 计算并写入所有未来课程。
- 自动排课项目顺序固定按 `ai4_project.id ASC`。
- CRM班级和自动直播课固定使用 `room_type=2`。
- CRM接口专用认证尚未接入；`/external/**` 当前不要求后台登录，后续增加独立外部访问校验。
- 若 V10 最终成功，字典建表迁移使用 V11；在确认 V10 状态前不得执行新的版本迁移。

## 当前实现进度（2026-07-23：字典类型第一阶段）

- 字典表最终使用系统模块前缀：`sys_dict_type`、`sys_dict_item`。
- V11 已成功创建两张字典表；V11 已执行后保持不可变。
- V12 已将 `sys_dict_type.remark` 调整为 `VARCHAR(500) NULL`。
- `dict_code` 保留为稳定业务标识并具有唯一约束；`id` 仅用于数据库内部关联，`dict_name` 用于展示。
- `dict_code` 由管理员创建时传入，只允许小写字母、数字和下划线且必须以字母开头；创建后不允许通过普通修改接口变更。
- 已建立 `DictType`、`DictItem` model，以及创建、修改字典类型 DTO。
- 已实现 `DictTypeRepository`、`JdbcDictTypeRepository` 和 `DictTypeService` 的条件查询、详情、创建、修改。
- Repository 单条查询返回 `Optional<DictType>`；Service 将不存在转换为 `DICT_TYPE_NOT_FOUND`。列表查询返回空 `List`，不因没有数据抛异常。
- 创建流程：检查编码唯一、生成 UUID、补默认状态 0、保存后按 ID 重查，以返回数据库生成的时间字段。
- 修改流程：先读取原记录，只替换名称和备注，保留编码、状态和创建时间；检查影响行数后重查最新数据。
- 新增字典类型 Controller：列表、详情、创建、修改，路径统一为 `/dict-types`。
- Controller 使用待初始化权限：`system:dict:list`、`system:dict:create`、`system:dict:update`。在权限数据迁移完成前，受控接口会返回 HTTP 403。

## 当前实现进度（2026-07-27：题目管理第一阶段）

- V14 为 `ai4_question_bank` 增加答题前后引导词，为 `ai4_question_choice` 增加选项影响，并约束选项标识为 A、B、C、D。
- 题目当前仅支持 `single_choice` 和 `multiple_choice`；每题固定四个选项，每个选项对应一个影响文本。
- 已实现题目列表、题目与选项详情、完整修改和逻辑删除四个接口，统一路径为 `/questions`。
- 列表使用 `QuestionListResponse`，不加载选项、不暴露 `delFlag`；详情使用 `QuestionDetailResponse`，包含四个 `QuestionChoiceResponse`。
- 修改请求由 `UpdateQuestionRequest` 和 `UpdateQuestionChoiceRequest` 表达；题目主体与四个选项在同一事务内更新。
- Service 校验选项必须完整覆盖 A-D、选项 ID 必须属于当前题目且不能错配标识。
- 删除使用 `del_flag = 2` 逻辑删除，选项暂不物理删除，通过题目状态统一隐藏。
- V15 创建题目管理菜单和 `question:list`、`question:update`、`question:delete`，并授权给启用中的 `super_admin`。
- 业务确认每个选项只表达选择后的影响，不区分正确或错误；V16 删除 `ai4_question_choice.correct`，Java model、DTO、SQL 和 Service 正确答案校验同步删除。
- 工作流生成题目接口尚未实现；待工作流输入输出契约确定后，再增加生成、结果校验和事务落库。
- `imageUrls`、`optionValue` 当前使用 Java `String` 对应 MySQL JSON；尚未增加应用层 JSON 合法性校验。
- ApiPost 已在 `AI4.0后台 / 本地 / ai4-boot-verify / 题目管理接口` 中创建上述四个接口。
## 当前实现进度（2026-07-30：Skill 绑定保护）

- Skill 分页列表新增 `partnerCount`，表示当前 Skill 被多少个未删除合伙人在项目任务中使用。
- Skill 删除前检查 `ai4_project_task_partner_skill`；存在项目任务关联时返回 `SKILL_ASSIGNED_TO_PARTNER`，不会自动解绑。
- `JdbcSkillRepository.softDelete` 使用 `NOT EXISTS` 在更新时再次确认没有绑定关系。
- 唯一接口文档和 ApiPost `AI4.0后台 / 本地 / ai4-boot-verify / Skill管理接口` 已同步列表响应与删除规则。
- Java 编译验证通过。
## 2026-07-30: 合伙人管理

- 新增 `com.ai4.partner` 模块，包含 model、DTO、repository、service 和 controller。
- 接口前缀为 `/partners`，提供分页列表、详情、新增、完整修改和逻辑删除。
- 合伙人与 Skill 的实际使用关系按项目任务保存；一个任务配置2至4个合伙人，每个合伙人配置1至6个有效 Skill。
- 图片 URL 字段当前均可为空，等待统一 OSS 上传接口完成后由前端回传 URL。
- 权限迁移为 `V20__seed_partner_permissions.sql`。
## 2026-07-30: Skill 与合伙人完整性增强

- V21 为合伙人增加 `version`，并为有效 Skill、有效合伙人名称增加条件唯一约束。
- Skill 新增轻量选项接口 `GET /skills/options`，复用 `skill:list` 权限。
- 合伙人绑定 Skill 前对 Skill 行加事务锁；合伙人修改使用乐观锁防止覆盖更新。
- Skill 绑定计数只统计有效合伙人。
## 当前实现进度（2026-07-31：课包与项目管理重构）

- 系统只保留一个有效课包，不提供课包新增、修改、删除和分页接口；`GET /course-package` 查询数据库中唯一有效课包，不写死课包 ID。
- 原课节和课程内容业务已移除，V22 删除空表 `ai4_lesson_content`、`ai4_lesson`，旧 Java Controller、Service、Repository、Model 和 DTO 同步删除。
- 新增 `ai4_project` 和 `com.ai4.project` 模块，提供项目分页列表、详情、新增、修改和逻辑删除五个接口。
- 项目名称必填且最长 10 个字符；项目介绍必填且最长 25 个字符；项目编码必填、最长 64 个字符，允许多个有效项目重复使用。
- 新增项目时前端不传 `packageId`，Service 自动绑定数据库中唯一有效课包；有效课包数量不是 1 时拒绝新增。
- 项目删除使用 `del_flag = 2`；项目编码不作为业务关联键，所有业务关联继续使用 `project_id`。
- V22 初始化项目管理菜单及 `project:list/create/update/delete` 权限，并授权给启用状态的 `super_admin`。
- 新增 `ProjectModuleIntegrationTests`，覆盖课包单查、项目 CRUD、分页筛选、参数校验、编码重复、逻辑删除和权限。
- 2026-07-31 完整测试结果：`Tests run: 10, Failures: 0, Errors: 0, BUILD SUCCESS`；测试库已成功迁移到 V22。
- 项目数量不再限制为 8 个，`GET /projects` 返回全部未删除项目并按 `sort ASC, id ASC` 排序；项目详情仍按 ID 查询。

## 当前实现进度（2026-07-31：项目领取任务模块）

- 一个项目只能配置一个领取任务，使用 `ai4_project_task.project_id` 唯一约束保证；任务不提供删除接口。
- 任务字段为 `taskTypes`、`companyPositioning`、`companyCard`、`companyNames`、`publicTask`、`claimTask`、`taskName`。
- `taskTypes` 使用 JSON 保存两级选择：`IMAGE`、`TEXT`、`MEDIA` 及各自允许格式；Service 负责归一化和对应关系校验。
- 每个任务必须关联2至4个有效合伙人，通过 `ai4_project_task_partner` 保存顺序并禁止重复。
- 三张图片只保存 OSS URL，创建和完整修改任务时 `publicTask`、`claimTask`、`companyCard` 均为必填。
- 公司名称最多7个字符且必须以“公司”或“工作室”结尾；AI 根据项目名称、项目介绍、任务名称和公司定位生成60个候选。
- AI 输出最多尝试3次，后端解析 JSON 数组、去重并重新校验；候选名称不会自动落库，由修改任务接口保存最终选择。
- V23 创建两张任务表及 `task:list/create/update/generate` 权限，并授权启用的 `super_admin`。
- 完整测试结果：12个测试全部通过，测试库已成功迁移到 V23。
- V24 在 `ai4_project_task` 增加 `company_names JSON`；60个候选名称生成成功后整体保存，再次生成整体覆盖。
- 新增按数组下标修改公司名称接口，继续校验7字符限制、指定后缀和任务内不重复；不存在单独的最终公司名称字段，60个名称全部保存在 `companyNames` 中。
- V26 新增项目生成和创业报告两套独立配置，分别保存必填且暂不限长度的 `scoringCriteria` 和 `reviewCriteria`。
- 两种配置都按 `project_id` 保证项目级唯一，分别提供查询、首次创建和修改接口，不提供删除接口。
## OSS 与合伙人图片上传：2026-08-03

- 项目已引入阿里云 OSS 配置：`endpoint`、`bucketName`、`accessKeyId`、`accessKeySecret`、`publicBaseUrl`，敏感凭证由运行环境变量提供。
- `OssStorageService` 是内部统一存储能力，不再暴露通用 `/files/upload` Controller，具体业务必须先完成自己的文件规则校验。
- 合伙人图片接口为 `POST /partners/images/upload`，使用 `multipart/form-data` 一次提交 `large`、`medium`、`small`、`fullBody`、`avatar` 五张图片。
- 五个文件参数全部必填；每张不超过 5MB，扩展名只允许 `png`、`jpg`、`jpeg`；请求总上限为 30MB。
- 图片上传成功后分别进入 `partner/large`、`partner/medium`、`partner/small`、`partner/full-body`、`partner/avatar` 前缀，日期与 UUID 用于组织对象和避免覆盖。
- OSS 目录只是对象键前缀，相同目录会被复用，不会创建传统意义上的重复目录。
- 上传响应只返回五个 URL。前端随后调用新增或修改合伙人接口，把 URL 保存到 `ai4_partner` 的对应字段；图片与合伙人的归属由数据库记录确定。
- `CreatePartnerRequest` 和 `UpdatePartnerRequest` 的五个图片 URL 已改为必填，最大长度均为 1024。
- 当前上传接口受全局 JWT 保护，但尚未配置独立的 `partner:upload` 权限。

## 任务三图上传：2026-08-05

- 新增 `POST /projects/{projectId}/task/images/upload`，使用一次 multipart 请求上传 `publicTask`、`claimTask`、`companyCard` 三个必填文件。
- Controller 将路径中的 `projectId` 传入 Service；Service 上传前调用项目查询逻辑确认项目有效。
- OSS 前缀按项目隔离为 `task/{projectId}/public-task`、`task/{projectId}/claim-task`、`task/{projectId}/company-card`。
- 上传响应字段与任务请求字段统一，前端取得 URL 后再调用任务创建或修改接口持久化。
- 任务图片当前没有扩展名、MIME、尺寸或业务文件大小限制；multipart 基础设施上限为单文件 20MB、单请求 60MB。
- 上传接口当前仅要求登录，按产品确认暂不增加独立 `task:upload` 权限。

## 项目沙盘模块：2026-08-06

- 一个有效项目最多配置一份沙盘，使用 `ai4_project_sandtable.project_id` 唯一约束保证，不提供删除接口。
- 沙盘包含必填视频URL、零个或多个视频分段以及3至6个属性；分段时间使用 `DECIMAL(10,2)` 保存总秒数。
- 新增查询、首次创建、完整修改和视频上传四个接口，路径统一为 `/projects/{projectId}/sandtable`。
- 视频通过OSS上传到 `sandtable/{projectId}/video`，暂不增加业务格式和大小限制，仍受全局multipart限制。
- 分段无排序字段，查询按 `segment_time ASC, id ASC`；修改时整体替换。
- 属性无排序字段，查询按 `id ASC`；图标可空，目标值可空，目标值和初始值支持小数及负数，初始值仍为必填。
- 属性修改采用差异同步：未变化保留，新增直接插入，修改删除旧记录后新增，请求中消失的旧属性删除。
- 沙盘接口暂时复用 `project:list/create/update` 权限，不新增权限数据。
- 按用户要求不新增沙盘测试用例；Flyway当前关闭，三张表由开发库、测试库和服务器库手动保持一致。

## 测试库与自动化测试移除：2026-08-07

- 当前不再维护独立测试数据库，已删除 `src/test/java` 下4个数据库集成测试和 `application-test.properties`。
- POM已移除 `spring-boot-starter-actuator-test` 与 `spring-boot-starter-webmvc-test`，项目当前没有自动化测试源码。
- `mvn package` 不再读取 `AI4_TEST_DB_PASSWORD`，也不会连接 `ai4_boot_verify_test`。
- 后续功能验证暂时依靠开发库与ApiPost人工测试；恢复自动化测试时应重新设计不依赖共享MySQL测试库的方案。

## 沙盘收益趋势与预计学习收获：2026-08-07

- 沙盘主表新增必填`trend_url`，Java接口字段为`trendUrl`。
- 新增`ai4_project_sandtable_gain`，每份沙盘必须保存2至4条预计学习收获；`gain`必填且最多5个字符，`icon`可空且最多255个字符。
- 收获查询按ID升序；修改采用与属性相同的差异同步语义。
- 新增`POST /projects/{projectId}/sandtable/trend/upload`，multipart字段为`trend`，OSS前缀为`sandtable/{projectId}/trend`。
- 趋势图必填，但上传时暂不增加格式、尺寸和业务大小限制，仍受系统全局multipart限制。
- 数据库由用户手动修改，本轮不增加Flyway和测试。

## 项目列表配置完成状态：2026-08-07

- `GET /projects`新增`configurationStatus`，取值为`COMPLETED`或`INCOMPLETE`。
- 状态不写入项目表，Repository通过领取任务、项目生成、沙盘推演和创业报告四张主表实时计算。
- 四个模块记录全部存在才返回完成，任意缺失返回未完成。
- 新增`ProjectListItem`查询投影模型，避免把派生状态加入完整`Project`数据库模型。

## 当前实现进度（2026-08-11：项目题目管理重写）

- 当前题目管理已从旧题库模型重写为“项目题目管理”，不再使用 `ai4_question_bank` 和 `ai4_question_choice` 作为当前接口数据源。
- 新业务表为：
  - `ai4_project_question`：题目主表。
  - `ai4_project_question_option`：题目选项表。
  - `ai4_project_question_option_attribute`：选项对沙盘属性的影响值表。
- 当前接口统一路径仍为 `/questions`，已实现：
  - `GET /questions/project-attributes`：查询项目名称及项目沙盘属性名称。
  - `GET /questions`：按 `projectId` 分页查询未删除题目，固定每页 10 条，支持 `questionText` 和 `remark` 模糊筛选。
  - `GET /questions/{questionId}`：查询题目详情，用于修改回填。
  - `POST /questions`：新增题目、选项和选项属性影响。
  - `PUT /questions/{questionId}`：完整修改题目，项目不可修改，选项和属性影响整体重建。
  - `DELETE /questions/{questionId}`：只逻辑删除题目主表，不删除子表。
  - `POST /questions/image/upload`：上传题目图片到 OSS，前缀为 `questions/images`。
- 题目字段包括：`projectId`、`imageUrl`、`imageDesc`、`questionText`、`month`、`beforeGuide`、`status`、`remark`。
- `month` 由前端计算并传入，后端只保存和返回；列表返回 `month`，前端自行排序。
- 选项保存 `optionText`、`impactType`、`afterGuide`；`impactType` 后端按约定保存 `GOOD/BAD`。
- 每个选项下的不同沙盘属性通过 `attributeEffect` 独立保存，放在 `ai4_project_question_option_attribute`。
- 新增或修改启用题目时，后端检查同一项目启用且未删除题目不能超过 36 个；超过时返回 `QUESTION_ENABLED_LIMIT_EXCEEDED`。
- 按当前需求，后端不校验项目是否存在、不校验属性全集、不校验 `attributeEffect` 格式、不校验 `impactType` 枚举合法性。
- 图片上传接口当前只要求登录，尚未细分 `question:upload` 权限，也未增加图片大小、格式、尺寸等业务限制。

## CRM 外部同步接口（2026-08-17）

- CRM班级接口当前包括新增、完整修改、开课/停课、批量绑定学员、批量移除学员和课包自动建课数量查询。
- 修改班级时只重建 `status=0` 的未开始直播课，已开始和已结束课程保持不变。
- 学员根据手机号匹配有效 `el_user`；绑定关系位于 `ai4_class_student`，`state=1` 表示正常，`state=2` 表示移出班级。
- CRM教师同步接口包括新增和修改，使用 `crm_teacher_id` 映射本地教师；修改时空密码不更新数据库密码。
- 课包统计接口的 `liveCourseCount` 是当前已经创建且 `del_flag=1` 的实际直播课数量，通过班级和直播课关联表统计，不限制课程状态。
- 所有 `/external/**` 当前不经过后台JWT和权限拦截，CRM专用认证仍待实现。

## 教师后台账号与角色同步（2026-08-24）

- 教师同时承担后台用户和直播教师两种身份：`sys_user` 保存后台登录账号，`live_teach` 保存完整教师资料，两者通过 `live_teach.user_id -> sys_user.user_id` 关联。
- CRM 新增教师接口 `POST /external/liveTeach/add` 新增必填字段 `roleIds`；角色 ID 不保存到教师表，而是写入 `sys_user_role`。
- CRM 新增教师在同一事务中创建 `sys_user`、`live_teach` 和 `sys_user_role`，两张账号表保存同一份 BCrypt 密码哈希。
- 新增教师时角色必须存在、启用且不能重复；CRM 不允许直接分配 `super_admin`。
- CRM 修改教师会同步修改 `sys_user.user_name` 和 `live_teach.username`；密码为空时两张表均保留原值，有内容时只编码一次并同步更新。
- `GET /teachers` 查询有效教师，支持 `userName`、`nickName` 可选模糊筛选，不分页；返回 `id`、`userId`、`userName`、`nickName`。
- 教师角色查询和修改继续复用 `/users/{userId}/roles`；后台密码重置继续复用 `/users/{userId}/password`。
- `PUT /users/{userId}/password` 现在会在同一事务中同步更新与该用户关联的 `live_teach.password`；非教师后台用户不受影响。

## 直播课详情（2026-08-25）

- 新增 `GET /live-courses/{courseId}`，通过直播课主键查询基本信息、出勤统计、录像和分页学员明细。
- 学员分页参数为 `pageNum/pageSize`，默认 `1/10`，最大每页 100 条。
- 实际上课时长统一返回 `actualDurationSeconds`：进行中使用当前时间减实际开始时间，结束后使用实际结束时间减实际开始时间。
- 出勤、缺勤、迟到、早退人数汇总 `live_user_course_record` 的四个最终状态字段。
- 学员列表从 `ai4_class_student` 的当前有效成员出发并左关联上课记录，保证从未进教室的缺勤学员仍可展示。
- `lateSeconds` 返回迟到秒数；`probeId` 返回 `live_user_course_record.stream_uuid`。
- 录像读取 `live_course_video.m3u8`，支持单个 URL 或 JSON URL 数组；没有录像时返回空数组。

## 声网课堂事件同步（2026-08-26）

- 定时任务默认每5秒拉取已创建且尚未关闭课堂的事件，可通过 `AI4_AGORA_EVENT_SYNC_DELAY_MS` 调整。
- `cmd=20` 同步教师和学生进出课堂，`cmd=1` 同步课堂状态；每轮先处理进出事件，再处理状态事件。
- 客户端 `userUuid` 沿用 `1_{teacherId}`、`2_{studentId}`，后台去掉前缀后校验当前直播课归属。
- `live_course_room_user` 保存当前在线用户，`live_teach_course_record` 和 `live_user_course_record` 保存最终教师、学生考勤。
- 出勤以是否进入过课堂为准；迟到使用首次进入时间；早退使用最后离开时间；缺勤在课堂状态变为3时统一结算。
- 状态事件会同步 `live_course.status`，状态1写入 `start_time`，状态3写入 `end_time`。
- Redis 保存课程级同步锁、声网分页游标和事件 sequence 幂等标记，相关键默认保留7天。
- 本后台尚未提供教师或学生客户端 RoomUserToken 接口；真实进入课堂仍由客户端服务完成身份校验和 Token 签发。

## 声网云端录制（2026-08-26）

- `record=1` 的直播课在首个合法教师或学生进房事件提交后启动声网网页录制，`record=2` 不启动。
- 启动流程依次生成录制 UID、观察者身份、RTM Token、录制 RTC Token和旧版录制页 URL，然后调用声网 acquire/start。
- 观察者身份兼容旧键名 `live:Observer:user:{uid}`、`live:Observer:course:0:{roomId}`，录制页相关服务必须和本项目读取同一 Redis 数据库。
- `live_video_record` 保存资源 ID、SID、录制状态、随机密钥和文件列表；完成后把可展示的 MP4/HLS 地址写入 `live_course_video`。
- 课堂状态同步为3后停止录制；定时补偿任务每30秒扫描已关闭但仍处于活动状态的录制任务。
- `/liveVideoRecord/update`、`/heartbeat` 和 `/recordingCallback71` 是录制网页或声网使用的内部接口，不经过后台 JWT 和菜单权限校验。
- 真实端到端验证依赖客户端进入声网课堂并发布音视频流；仅创建教室或手工修改数据库状态无法验证录像文件生成。
- 部署除既有声网与 OSS 配置外，还必须设置 `AI4_AGORA_RECORD_PAGE_URL`；录制存储区域默认 `3`，可由 `AI4_AGORA_RECORDING_STORAGE_REGION` 覆盖。

## 直播课删除（2026-08-26）

- 新增 `DELETE /live-courses/{courseId}`，仅允许删除 `status=0` 的未开始直播课。
- 删除采用带状态条件的原子更新，将 `live_course.del_flag` 设为2，并在同一事务内删除 `ai4_class_live_course` 关联。
- 状态1、2、3分别表示进行中、拖堂中和完全关闭，均返回 `LIVE_COURSE_DELETE_FORBIDDEN`。

## 学员账户列表（2026-08-26）

- `GET /accounts` 已改为返回学员管理页面所需字段：`userId`、`phone`、`className`、`packageName`、`identity`、`deviceModel`、`deviceSystemVersion`、`signupTime`、`loginTime`。
- `identity` 不存储在 `el_user`，查询时根据当前有效班级关系动态计算：存在 `ai4_class_student.state=1` 的有效归属为“正式”，否则为“普通”。
- 班级名称和课包名称来自最近一条有效班级关系；存在多条关系时按 `updated_at DESC、id DESC` 取第一条。
- Repository 使用专用列表 DTO 和窗口函数完成投影，避免一对多关联造成同一学员重复、分页记录数失真。
- 原有 `userId`、账号、昵称、手机号、状态、来源和注册日期范围筛选参数保持可用。

## 新增学员账户精简（2026-08-26）

- `POST /accounts` 当前请求体只有 `phone` 和 `password`，两个字段均必填。
- 后端使用雪花 ID 生成 `userId`，并将手机号同时保存到 `account` 和 `phone`。
- 当前数据库 `el_user.id` 不是自增列；新增时同一个雪花 ID 同时写入 `id` 和 `user_id`。
- 后端默认保存 `nick=默认用户`、`status=1`、`sourceType=1`、`delFlag=1` 和当前注册时间；设备、渠道、OpenID 等字段初始为空。
- 密码继续使用 BCrypt 保存；手机号对应的有效账号已存在时返回 `ACCOUNT_ALREADY_EXISTS`。

## 学员详情（2026-08-27）

- 新增 `GET /accounts/{userId}`，返回学员 ID、手机号、所属班级及最近一节已开始直播课。
- 直播课字段包括 `courseId`、`projectName`、`teacherName`、`courseTime`、`actualDurationSeconds`、`status`、`attended`。
- 最近直播课包含状态0、1、2、3，按计划上课时间和课程 ID 倒序取第一条；状态0表示未开始。
- 状态0课程的 `attended` 返回 `null`；状态1、2、3根据上课记录返回 `true` 或 `false`。
- 实际时长使用 `end_time-start_time`；状态1或2尚未结束时使用当前时间减 `start_time`；没有开始时间时返回0。
- 普通学员没有班级时课程字段为空；学员不存在时返回 `ACCOUNT_NOT_FOUND`。

## Coze OAuth JWT 与题前引导生成（2026-08-28）

- 项目已接入 `com.coze:coze-api:0.4.2`，使用外部 OAuth JWT JSON 文件创建 `CozeAPI`，配置文件和私钥不进入代码仓库。
- `ai4.coze.enabled` 默认关闭；启用后由 SDK 维护短期 Access Token，不在日志中输出 Token 或私钥。
- 通用客户端支持同步执行、异步提交和异步执行记录查询；当前题前引导只使用同步执行。
- 新增 `POST /questions/before-guide/generate`，输入项目 ID、图片描述、题目文本以及2至4个选项和属性影响。
- 后端查询项目介绍和沙盘属性名称，按工作流字段 `Project_Introduction`、`imageDesc`、`questionText`、`option_information` 组织参数。
- 工作流 ID 为 `7678625341916659718`，配置项为 `COZE_QUESTION_BEFORE_GUIDE_WORKFLOW_ID`。
- 接口只返回 `beforeGuide`，不写数据库；前端回填后仍通过原题目新增或修改接口保存。

## Coze组合生成题目引导（2026-08-28）

- 选项引导工作流已确认是通用“选项引导”，不包含合伙人专属字段，工作流 ID 为 `7678645700088021046`。
- 新增 `POST /questions/guides/generate`，前端点击一次即可生成题前引导和全部选项的选后引导。
- 后端调用题前工作流1次，并把每个选项分别作为 `user_answer` 调用选项引导工作流；最多5个调用并行执行。
- 返回的 `optionGuides` 与请求选项顺序一致，`optionIndex` 从0开始，并同时返回 `optionText` 供前端核对。
- 任一工作流失败时组合接口整体失败，不返回部分结果；接口不写数据库，仍由题目新增或修改接口最终保存。

## 线程池配置（2026-08-31）

- 声网三个 `@Scheduled` 任务使用Spring调度池，默认配置为3个线程，线程名前缀为 `ai4-scheduling-`。
- Coze组合生成不再为每次请求临时创建线程池，统一复用Spring管理的 `cozeGuideExecutor`。
- Coze共享池固定10个工作线程、等待队列容量50，线程名前缀为 `coze-guide-`；队列和线程都饱和时由请求线程执行任务形成反压。
- Coze共享池只在应用关闭时由Spring执行 `shutdown`，业务请求不得主动关闭该线程池。

## OSS客户端复用与直播录制锁（2026-08-31）

- OSS客户端改为Spring管理的延迟初始化单例，第一次上传时创建，后续所有上传复用同一实例。
- 应用关闭时由Spring统一调用OSS客户端的 `shutdown`，上传业务不再逐文件创建和关闭客户端。
- 直播录制课程锁使用随机UUID作为持有者标识，不再保存固定值 `1`。
- 释放录制锁时通过Redis Lua脚本比较锁值，只有当前持有者才能删除锁，避免过期后误删其他线程重新取得的锁。
- 录制锁TTL继续保持60秒，录制启动、停止和业务接口保持不变。

## Skill 与合伙人名称重复规则（2026-09-04）

- Skill 名称和合伙人名称改为允许重复，2026-07-30 的有效名称唯一规则不再适用。
- Service 中的名称唯一校验及对应重复键异常转换已暂停，原实现保留为注释。
- `existsByName` 和名称重复错误码保留，方便后续恢复唯一规则。
- 项目任务中同一合伙人的 Skill ID 不允许重复；同一个 Skill 可以被多个任务和合伙人复用。
- 数据库需手动删除 `uk_ai4_skill_active_name`、`uk_ai4_partner_active_name` 唯一索引；`active_skill_name`、`active_partner_name` 生成列可以继续保留。
- 当前 Skill 和合伙人表没有创建者字段，因此系统只是全局允许重名，尚不能表达记录属于哪个创建用户。

## 学员创建昵称（2026-09-21）

- `POST /accounts` 和 `POST /external/aiUser/add` 的请求体共用可选字段 `nick`，最大长度255个字符。
- 后台将 `nick` 透传给 `ai-server` 的内部用户创建接口；未传或传空时由 `ai-server` 随机生成默认昵称。

## 项目任务 Skill 绑定（2026-10-09）

- 合伙人新增、修改接口不再接收或保存 `skillIds`；合伙人管理不再承担 Skill 绑定步骤。
- 项目任务新增和修改请求使用 `partners` 数组，每项包含 `partnerId` 和 `skillIds`。
- 项目任务的实际 Skill 关系保存在手动创建的 `ai4_project_task_partner_skill` 表中；`ai4_project_task_partner` 继续保存任务与合伙人的关系。
- `ProjectTaskService` 校验合伙人和 Skill 均为未删除记录，并校验同一合伙人下 Skill 不重复。
- Skill 的 `partnerCount` 和逻辑删除保护改为查询项目任务 Skill 关联表。
- `ai4_partner_skill_relation` 暂作为历史表保留，应用代码不再使用；已有任务若没有新表关联，需要重新配置 Skill。
