# Decisions

## 2026-09-28：调整项目领取任务配置

Decision:

- 发布任务页和领取任务页共用一张任务图，后台配置字段统一为 `taskImage`。
- 提交内容固定只有一项，保存提交内容图标、名称和简介；“必交”由前端固定展示，不进入数据库和字典。
- 任务关键词使用 JSON 数组保存，数量限制为 2 至 5 个，单个关键词限制为 2 至 4 个字符。
- 任务专用上传接口接收 `taskImage`、`submissionIcon` 和 `companyCard`，只负责上传 OSS 并返回 URL。
- 保留历史 `public_task`、`claim_task` 数据库列，代码切换完成后暂不删除，避免影响旧数据或其他消费者。

Reason:

- 两个页面使用同一张任务图，拆分图片字段没有业务价值；文字内容应由前端灵活排版，后台只保存结构化配置。

## 2026-09-23：后台本地图形验证码

Decision:

- 后台登录使用本地生成的简单字符图片验证码，不接入腾讯云或其他第三方验证码服务。
- `GET /auth/captcha` 返回验证码标识和 PNG Base64 Data URI；答案保存到 Redis，默认 120 秒过期。
- 登录时提交 `captcha_id` 和 `captcha_code`；验证码无论校验成功还是失败都通过 Redis 脚本原子删除，防止重复尝试。
- 验证码启用开关使用 `AI4_AUTH_CAPTCHA_ENABLED`，默认开启；不改变数据库结构。

Reason:

- 后台只需要简单的人机校验，本地生成可以减少第三方依赖和额外密钥配置。
- Redis 已经是本项目现有基础设施，适合保存短期、一次性的验证码状态。

## 2026-09-22：CRM 直播课出站同步

Decision:

- 由后台主动调用 CRM 的课包直播课数量同步接口和班级直播课结束回调接口，使用现有 `RestClient`，不新增后台管理接口。
- 课包数量以数据库实时统计结果为准，使用有效班级关联的未逻辑删除直播课数量；建课、删课和停复课提交成功后再通知。
- 班级结束回调在本地状态同步为3并完成考勤结算后发送；如果存在云端录制任务，则等待录制完成后再发送。
- 回放地址使用 `live_course_video.m3u8` 的第一个地址；没有回放地址时传空字符串。
- 缺勤学生的进入、退出时间字段省略；出勤时长按进入时间到计划下课时间计算，单位为秒。
- 出站调用通过独立两线程线程池执行，结束回调使用 Redis 成功标记和补偿扫描避免一次性事件失败后永久遗漏。

Reason:

- 出站 HTTP 调用不应阻塞数据库事务，也不能在事务回滚时提前通知 CRM。
- 录像地址写入晚于课堂状态关闭，结束回调必须兼容录像异步完成。
- CRM 文档没有提供幂等字段，因此后台用 `courseId` 做本地重复通知控制。

## 2026-09-22：题目删除与禁用的关联数据处理

Decision:

- 题目禁用只更新题目主表 `status=1`，保留选项和选项属性影响，重新启用时继续使用原配置。
- 删除题目时，在同一事务内删除该题目的选项属性影响、题目选项，再将题目主表设置为 `del_flag=2`。
- 沙盘及沙盘属性不随题目删除或禁用而改变。

Reason:

- 题目子表没有独立的启用/禁用状态，保留已删除题目的子数据会产生孤儿记录。
- 题目主表保留逻辑删除记录，便于保留题目生命周期；子表没有独立业务价值，因此同步清理。

## 2026-08-12：字典项管理边界

Decision:

- 字典项归属于固定字典类型，修改接口不允许变更 `typeId`。
- `itemValue` 在同一字典类型内唯一，业务数据保存 `itemValue`，不依赖字典项 ID。
- `extra` 作为 JSON 对象对外传输，数据库继续使用 JSON 字段保存。
- 当前表没有逻辑删除字段，字典项采用物理删除；删除权限暂时复用 `system:dict:update`。
- 创建时 `sortOrder`、`status` 可省略并默认 `0`，完整修改时二者必填。

Reason:

- 稳定业务值可以避免字典项 UUID 泄漏到业务模型；JSON 扩展字段可以承载颜色等少量展示元数据。
- Flyway 当前关闭且没有独立删除权限数据，先复用现有权限，后续恢复迁移管理时再补 `system:dict:delete`。

## 2026-07-16：菜单与权限管理规则

- 权限码统一采用 `模块:资源:动作`，前后端使用同一权限码。
- 超级管理员角色编码固定为 `super_admin`，同样通过 `sys_role_menu` 明确授权，不绕过权限体系。
- 新增菜单时自动授权给已存在且启用的 `super_admin` 角色。
- 菜单已分配给角色时，必须先解除授权，才能修改 `perms` 或删除菜单。
- 禁用菜单保留角色授权，重新启用后原授权自动恢复。
- 后续角色授权由后端自动补齐祖先目录；`BUTTON` 进入权限列表但不进入动态路由。
- 后端接口级权限校验和 RBAC 外键在权限管理阶段实现；菜单外键删除策略使用 `RESTRICT`。

## 2026-07-16：服务账户表改为 ai4_user

- 正式服务账户表统一使用 `ai4_user`。
- 不修改已经执行过的 V3；新增 V6 使用 MySQL `RENAME TABLE` 保留现有结构和数据。
- V6 与账户运行时代码在同一个版本发布，避免迁移后应用仍查询旧表名。

## 2026-07-15：课程表前缀改为 ai4

- 正式课程表名统一使用 `ai4_course_package`、`ai4_lesson`、`ai4_lesson_content`。
- 不修改已经执行过的 V4，避免 Flyway checksum 校验失败。
- 新增 V5 使用 MySQL `RENAME TABLE` 一次性重命名三张关联表，已有数据和外键关系继续保留。
- 新环境会先执行 V4 创建旧名表，再立即执行 V5 改为正式表名；已有 V4 环境只执行 V5。

## 2026-07-13：课包、课节和课程内容复制方案

- 参考旧项目的业务能力，但不直接复制若依/MyBatis-Plus 代码；按当前项目的 `JdbcTemplate + Repository + Service + ApiResponse` 重新实现。
- 正式接口采用 `/course-packages` 和 `/course-lessons`，由统一 context path 自动添加 `/ai/v4`。
- 课包、课节和课程内容的主外键统一使用 `Long/BIGINT`，不保留旧项目的 `Integer/String` 混用。
- 分页必须在数据库完成，不采用旧项目先查全部再在 Java 中截取的假分页。
- 逻辑删除值统一为 `1=正常、2=删除`。
- 批量导入采用浅复制：复制课节和课程内容记录，但继续共享课程内容 `ref_id` 指向的底层资源。
- 批量导入采用全有或全无事务，不保留旧项目的部分成功结果。
- 课节详情统一返回课节基本信息和课程内容列表。
- 标签字典和 OSS 上传暂不纳入本阶段，避免引入当前项目不存在的若依基础设施。

## 2026-07-13：账户写入兼容旧字段，响应保持规范字段

- 外部服务写入账户时允许继续使用旧数据库风格字段，通过 Jackson `@JsonAlias` 映射。
- Java 内部和后台前端响应继续使用可读的 camelCase 字段，不把旧缩写扩散到新接口响应。
- 无法解析的 JSON、字段类型或日期时间属于客户端请求错误，统一返回 HTTP 400 和 `INVALID_REQUEST`。

## 2026-07-13：统一接口文档维护位置

- 项目接口文档统一维护在 `D:\Dev\Projects\ai4-boot-verify\API_DOCUMENTATION.md`。
- 该文件作为接口契约的单一文档来源，后续接口变更必须同步更新。
- 默认不再创建重复的接口说明文件，避免前后端使用到不同版本的契约。

## 2026-07-13：服务账户与后台用户分离

- 后台登录用户继续使用 `sys_user` 和 `com.ai4.user`。
- 外部服务提供的业务账户使用兼容旧系统的 `el_user`，代码归属独立 `com.ai4.account` 模块。
- 当前项目是 `el_user` 的本地持久化和后台管理方，因此使用 `AccountRepository` / `JdbcAccountRepository`，不引入远程 Client。
- `userId` 由外部服务提供；昵称为空时由当前服务设置为“默认用户”。
- 服务账户密码统一复用当前 BCrypt `PasswordEncoder`，不迁移参考项目中的 MD5 实现。
- 第一版只实现新增、分页查询和重置密码，不迁移课程包、身份、班级关联查询。
- 接口采用当前项目 REST 约定：`POST /accounts`、`GET /accounts`、`PUT /accounts/{userId}/password`。
- `/accounts` 暂不加入 JWT 公开路径。外部系统直调所需的 API Key、签名或机器 Token 方案后续单独设计。

## 2026-07-10: `/auth/profile` 先使用 JdbcTemplate + SQL 查询 RBAC 数据

Decision:

- `/auth/profile` 的角色和权限读取先使用 `JdbcTemplate` 手写 SQL 实现。
- 新增 `AuthPermissionRepository` 作为认证模块读取角色和权限的数据访问接口。
- 方法命名使用业务含义：
  - `findRoleCodesByUserId(long userId)` 对应 `sys_role.role_code`。
  - `findPermissionCodesByUserId(long userId)` 对应 `sys_menu.perms` 的业务含义，即权限码。
- 暂不引入 MyBatis、MyBatis-Plus 或 JPA。

Reason:

当前项目已经使用 `JdbcTemplate`，RBAC 查询本质是多表关联查询。现阶段手写 SQL 更直观，能清楚看到 `sys_user_role`、`sys_role`、`sys_role_menu`、`sys_menu` 之间的关系，有利于理解后台权限系统的数据流转。MyBatis 更适合后续 SQL 规模变大、动态查询增多后再引入；JPA 对权限菜单这类多表查询不如 SQL 直观。

Consequences:

- 当前优先保持技术栈简单，不额外增加 Mapper、XML、实体关联等新概念。
- Repository 层会承担 SQL 查询职责，Controller 不直接写 SQL。
- 后续如果 SQL 数量明显增多，可以再评估是否迁移到 MyBatis。
## 2026-07-10: 使用 V2 迁移落地最小 RBAC 表结构

Decision:

- 新增 `V2__create_rbac_tables.sql` 管理后台权限相关表结构。
- 当前最小 RBAC 表结构确定为：
  - `sys_user`
  - `sys_role`
  - `sys_menu`
  - `sys_user_role`
  - `sys_role_menu`
- 用户表命名从旧的 `users` 调整为 `sys_user`。
- 用户字段使用数据库下划线风格：`user_id`、`user_name`。
- 用户表当前不保留 `email` 字段，因为当前业务没有邮箱需求。
- 当前暂不为关联表添加数据库外键，先使用主键和普通索引维护关联关系。

Reason:

项目接下来要实现登录后的角色、权限和动态路由，需要先有稳定的 RBAC 数据结构。使用 Flyway V2 统一管理 5 张表，可以避免本地、测试库、开发库和服务器库继续依赖手工建表，降低字段、索引和默认值不一致导致的部署风险。

Consequences:

- 后续数据库结构变更继续通过 V3、V4 等新迁移文件推进，不修改已经执行过的 V1/V2。
- `/users`、`/auth/login` 当前基于 `sys_user` 工作。
- `/auth/profile` 后续可以从 `sys_user -> sys_user_role -> sys_role -> sys_role_menu -> sys_menu` 查询真实角色和权限。
- 旧 `users` 表是否删除尚未决策；当前不做破坏性删除，后续确认无依赖后再处理。

## 2026-07-09: 使用 Flyway 统一管理数据库结构版本

Decision:

- 使用 Spring Boot 4 的 `spring-boot-starter-flyway` 启用自动配置。
- MySQL 额外使用 `flyway-mysql`。
- 迁移文件统一放在 `src/main/resources/db/migration`。
- 已有测试、开发和服务器数据库通过 V1 baseline 纳入管理。
- 完成 baseline 后关闭 `baseline-on-migrate`。
- 已执行过的迁移文件不再修改，后续只新增更高版本迁移。

Reason:

本地、测试和服务器数据库之前依赖手工建表，已经出现自增、排序规则、索引名和字段注释不一致。Flyway 可以让数据库结构与代码一起版本化，降低环境差异和部署遗漏。

Consequences:

- 新数据库可以通过 V1 自动创建 `users` 表。
- 已有数据库通过 `flyway_schema_history` 记录当前版本。
- 后续角色、权限和菜单表应通过 V2 及后续迁移创建。
- 部署新版本前需要先审查迁移 SQL 并备份服务器数据库。

## 2026-07-09: 使用 MVC 拦截器统一处理 JWT 认证

Decision:

- 使用 `JwtAuthenticationInterceptor` 在 Controller 前解析和校验 Bearer Token。
- 认证成功后将 `CurrentUser` 保存到当前 request。
- Controller 使用 `@RequestAttribute` 获取当前用户，不重复处理 Authorization Header。
- 当前按用户确认继续公开 `/users`，不要求 JWT。

Reason:

后续 `/auth/routes`、`/roles` 和 `/menus` 都需要识别当前用户。将 Token 解析集中到拦截器可以避免每个 Controller 重复实现相同逻辑。

Consequences:

- `/users` 虽然不展示在前端，但仍是可通过公网直接调用的 HTTP 接口；后续需要根据真实调用方决定是否增加管理员权限、内部服务凭证或网络限制。
- 拦截器当前只完成身份认证，不负责角色和权限判断。
- 浏览器前端接入前还需要明确 CORS 和 OPTIONS 预检策略。

## 2026-07-08: Maven 项目名调整为 ai4-admin-backend

Decision:

- 将 Maven `artifactId` 从 `ai4-boot-verify` 调整为 `ai4-admin-backend`。
- 将 Maven `name` 从 `ai4-boot-verify` 调整为 `ai4-admin-backend`。
- 将 Maven `description` 调整为 `AI4 admin backend service`。
- 将 `spring.application.name` 从 `ai4-boot-verify` 调整为 `ai4-admin-backend`。
- 本次不移动 Java 包名，不重命名启动类，不修改业务接口路径。

Reason:

`ai4-boot-verify` 是早期用于验证 Spring Boot 4.1.0、WebMVC、Validation、统一响应、数据库连接和部署链路的学习验证命名。当前项目已经进入正式后台管理后端建设阶段，包含用户、登录、MySQL、BCrypt、部署、Nginx、HTTPS 和 JWT 准备工作，继续使用 `boot-verify` 容易让打包产物、文档和协作认知停留在验证项目阶段。`ai4-admin-backend` 更准确表达“AI4 后台管理后端服务”的定位。

Consequences:

- 后续 Maven 打包产物名称会变为 `ai4-admin-backend-0.0.1-SNAPSHOT.jar`。
- Spring Boot 日志中的应用名会从 `ai4-boot-verify` 变为 `ai4-admin-backend`。
- 服务器部署时仍可上传并重命名为 `/opt/ai4/backend/app.jar`，因此 systemd 服务名 `ai4-backend` 暂不调整。
- Java 包名中当前仍保留 `com.ai4.verify`，这是历史验证阶段遗留命名；是否迁移到 `com.ai4.admin` 后续单独决策。
- 本次改名不影响 `/ai/v4` 接口前缀、Controller 路径、数据库连接或 JWT 配置。

## 2026-07-06: JWT 实施前必须先固定登录态和鉴权约束

Decision:

- JWT 不急于直接实现，先明确登录态、token 传输、token 校验、错误响应和前后端约定。
- 第一版 JWT 只做最小闭环：
  - 登录成功生成 access token。
  - token 中放 `userId`、`username`、`iat`、`exp` 等必要信息。
  - 有效期暂定沿用当前 `expireIn = 7200` 秒。
  - 新增 `GET /auth/me` 作为受保护接口验证 token 链路。
  - 暂不做 refresh token、多端会话、黑名单、角色权限和强制下线。
- JWT secret 必须通过环境变量配置，不能写死在代码里。
- JWT 失败场景必须走统一响应格式。

Reason:

JWT 接入后，请求链路会从“登录接口返回成功”扩展为“登录生成 token、前端保存 token、后续请求携带 token、后端拦截并校验 token、Controller 获取当前用户”。这会暴露 token 保存、Authorization 格式、过期处理、跨域、拦截器放行、context-path、secret 稳定性、多设备策略和统一错误响应等问题。提前固定约束可以避免配置 JWT 后出现“登录本来正常，换设备、刷新页面或访问其他接口就异常”的情况。

Consequences:

- 前端后续必须统一使用请求头：
  - `Authorization: Bearer <token>`
- 后端拦截器或过滤器必须放行：
  - `POST /auth/login`
  - `POST /users`
  - `OPTIONS /**`
  - 必要的健康检查接口
- 实现时必须特别注意项目已有 context-path：
  - 外部真实路径是 `/ai/v4/...`
  - Controller 业务路径是 `/auth/login`、`/users` 等
- CORS 必须允许前端域名和 `Authorization` 请求头，否则浏览器会在预检请求阶段失败。
- JWT 错误码后续应至少考虑：
  - `UNAUTHORIZED`
  - `TOKEN_EXPIRED`
  - `INVALID_TOKEN`
- 需要和前端确认：
  - token 保存位置
  - token 过期后的跳转策略
  - 是否允许多设备同时登录
  - 统一错误码如何展示

## 2026-07-06: HTTPS 作为 JWT 前置条件

Decision:

- 当前开发域名 `ai-manage-dev.xxaisys.com` 已配置 HTTPS。
- 后续登录和 JWT token 传输必须优先使用 HTTPS 地址：
  - `https://ai-manage-dev.xxaisys.com/ai/v4/...`
- 当前证书由 Certbot / Let's Encrypt 管理：
  - `fullchain.pem`
  - `privkey.pem`
- 不再建议通过 HTTP 地址测试登录和后续鉴权接口，除非是验证 HTTP 到 HTTPS 跳转。

Reason:

JWT 一旦泄露，攻击者可以在过期前冒充用户访问接口。HTTPS 可以保护登录密码和 token 在客户端到服务器之间的传输过程，避免 HTTP 明文传输导致 token 被截获。先完成 HTTPS，再配置 JWT，是当前后端认证链路推进的正确顺序。

Consequences:

- ApiPost 和前端联调默认使用 HTTPS。
- Nginx 继续作为 HTTPS 入口，再反向代理到本机 Spring Boot `127.0.0.1:8080`。
- Spring Boot Controller 路径不因 HTTPS 改动。

## 2026-07-03: 使用 BCrypt 替换 `{noop}` 密码占位实现

Decision:

- 在 `pom.xml` 中新增 `spring-security-crypto` 依赖。
- `com.ai4.user.security.PasswordEncoder` 内部使用 `BCryptPasswordEncoder`。
- 创建用户时保存 BCrypt 哈希，不再保存 `{noop}` 前缀密码。
- 登录时通过 `BCryptPasswordEncoder.matches(...)` 校验密码。
- Controller、Service、Repository 的调用方式保持不变。

Reason:

`{noop}` 只是学习阶段的占位实现，本质上仍然接近明文保存密码，不适合继续部署和演示。BCrypt 会自动加入随机盐，并且是单向哈希，数据库泄露时不能直接还原用户明文密码。由于项目前面已经抽出了自己的 `PasswordEncoder` 组件，这次只需要替换组件内部实现，不需要改动用户创建和登录的业务流程。

Consequences:

- 新创建的用户密码会保存为 BCrypt 哈希，通常以 `$2a$`、`$2b$` 或类似前缀开头。
- 旧的 `{noop}` 密码数据不能再被当前登录逻辑校验通过。
- 开发环境可以清空 `users` 表后重新创建测试用户。
- 如果需要保留旧用户，后续要做一次性密码迁移或临时兼容逻辑；当前阶段不做兼容，保持实现简单。

## 2026-07-03: 开发域名通过 Nginx 转发后端 HTTP 接口

Decision:

- 使用 `ai-manage-dev.xxaisys.com` 作为当前开发后端访问域名。
- Nginx 的 `proxy_pass` 保持为 `http://127.0.0.1:8080`，不在 `proxy_pass` 后额外拼接 `/ai/v4`。
- `/ai/v4` 继续由 Spring Boot 的 `server.servlet.context-path=/ai/v4` 统一负责。

Reason:

域名负责对外访问入口，Nginx 负责把公网 80 请求转发到服务器内部 Spring Boot 8080。应用统一前缀已经由 Spring Boot 配置管理，如果 Nginx 再额外拼接 `/ai/v4`，容易导致路径重复或转发路径混乱。

Consequences:

- ApiPost 和前端可以使用：
  - `http://ai-manage-dev.xxaisys.com/ai/v4/...`
- 后续配置 HTTPS 时主要修改 Nginx，不需要改 Spring Boot Controller 路径。

## 2026-07-03: 删除临时验证接口，保留正式业务接口作为验证入口

Decision:

- 删除 `GET /ai/v4/verify/database` 和 `POST /ai/v4/verify/validation` 两个临时验证接口。
- 删除对应源码文件：
  - `DatabaseVerifyController`
  - `ValidationController`
  - `ValidationRequest`
- 自动化测试不再请求 `/verify/*`，改为只保留正式业务接口测试。

Reason:

这些接口最初用于验证 Spring Boot、JSON、参数校验、全局异常处理和数据库连接是否可用。现在创建用户和登录接口已经覆盖了这些链路，临时接口继续暴露在公网没有业务价值，还会增加误用和暴露内部验证能力的风险。

Consequences:

- 后续接口验证应优先使用正式接口：
  - `POST /ai/v4/users`
  - `POST /ai/v4/auth/login`
- 数据库连通性不再通过 `/verify/database` 单独验证，而是通过真实业务链路间接验证。
- 部署服务器时需要重新打包并重启服务，旧 jar 中的临时接口才会真正从公网移除。

## 2026-07-03: 使用 systemd 管理后端服务，并通过 Nginx 暴露公网入口

Decision:

- 后端服务不再依赖 SSH 前台窗口运行，改由 systemd 后台管理。
- 公网入口不直接暴露 Spring Boot 的 8080 端口。
- 对外开放 Nginx 的 80 端口，由 Nginx 反向代理到本机 `127.0.0.1:8080`。
- MySQL 继续只作为服务器内部服务使用，不开放公网 3306。

Reason:

systemd 可以提供后台运行、状态查看、重启管理和开机自启能力，适合服务器长期运行服务。Nginx 作为公网入口比直接暴露 Spring Boot 端口更正规，后续可以继续承载前端静态资源、域名、HTTPS、访问日志和反向代理配置。

Consequences:

- 后端运行管理方式统一为 `systemctl`。
- ApiPost 和前端访问地址从带端口的 `:8080` 变为不带端口的 80 入口：
  - `http://39.106.177.254/ai/v4/...`
- 服务器安全组不需要开放 8080 和 3306。
- 后续配置域名和 HTTPS 时，可以主要调整 Nginx，不需要改 Spring Boot 业务代码。

## 2026-07-02: 服务器部署先使用前台启动验证

Decision:

- 第一次服务器部署先使用 `java -jar app.jar` 前台启动。
- 数据库连接信息通过环境变量传入：
  - `AI4_DB_URL`
  - `AI4_DB_USERNAME`
  - `AI4_DB_PASSWORD`
- 不依赖 `application.properties` 中的本地默认数据库密码作为服务器运行配置。
- 前台验证通过后，再进入 systemd 后台运行阶段。

Reason:

前台启动可以直接看到 Spring Boot 启动日志和数据库连接错误，适合第一次部署排查问题。环境变量可以把服务器数据库账号密码和代码分离，避免把部署环境密码写死在 jar 包默认配置中。

Consequences:

- 当前服务器已验证后端 jar、MySQL、数据库表和接口链路可用。
- 后续需要把同样的环境变量配置迁移到 systemd service 文件中。
- 后续正式运行不应长期依赖 SSH 前台窗口，否则窗口关闭服务就会停止。

## 2026-07-02: 登录测试通过接口链路准备数据

Decision:

- 登录自动化测试不直接用 SQL 插入用户。
- 登录测试先调用 `POST /ai/v4/users` 创建用户，再调用 `POST /ai/v4/auth/login` 登录。
- 登录成功和密码错误都通过 MockMvc 覆盖。

Reason:

登录依赖用户创建时的密码处理结果。如果测试直接插入数据库，就可能绕过 `UserService` 和 `PasswordEncoder.encode(...)`，导致测试没有覆盖真实业务链路。通过创建用户接口准备数据，可以同时验证“创建时编码密码”和“登录时匹配密码”是否一致。

Consequences:

- 当前登录测试更接近真实请求链路。
- 如果创建用户接口或密码编码逻辑变化，登录测试能更早暴露问题。
- 测试依赖数据库清理逻辑，后续部署或测试环境拆分时需要继续关注测试数据库隔离。

## 2026-07-02: 登录失败统一使用 INVALID_USERNAME_OR_PASSWORD

Decision:

- 登录时用户名不存在和密码错误都统一使用 `ErrorCode.INVALID_USERNAME_OR_PASSWORD`。
- `AuthService` 中错误码引用必须和 `ErrorCode` 枚举保持完全一致。
- 不向前端区分“用户名不存在”还是“密码错误”。

Reason:

登录失败属于认证失败场景。统一错误码可以减少信息泄露，也能让前端只按一个失败分支处理登录失败提示。错误码集中在 `ErrorCode` 中声明，业务代码只引用同一个来源，避免拼写不一致导致编译失败或契约漂移。

Consequences:

- 当前登录失败响应会进入 `BizException -> GlobalExceptionHandler -> ApiResponse.failed(...)` 链路。
- 当前 Maven 测试已恢复通过。
- 后续新增登录测试时，应断言失败响应中的 `code` 为 `INVALID_USERNAME_OR_PASSWORD`。

## 2026-07-02: 使用 JdbcUserRepository 作为当前真实用户数据访问实现

Decision:

- 当前用户数据访问从内存实现切换到 JDBC/MySQL 实现。
- `JdbcUserRepository` 实现 `UserRepository`，通过 `JdbcTemplate` 操作 `users` 表。
- `InMemoryUserRepository` 暂时保留源码用于学习和对照，但注释掉 `@Repository`，不作为 Spring Bean 生效。
- `UserService` 和 `AuthService` 都依赖 `UserRepository` 接口，而不是依赖具体实现类。

Reason:

这样可以让业务层只关心“查询用户、判断用户名是否存在、保存用户”这些能力，不关心底层数据来自内存还是 MySQL。后续如果从 JDBC 切换到 MyBatis/JPA，只需要替换 Repository 实现，Controller 和大部分 Service 逻辑不需要跟着重写。

Consequences:

- 当前运行应用需要 MySQL 中存在 `ai4_boot_verify` 数据库和 `users` 表。
- 测试会访问真实数据库，因此已经加入 `@BeforeEach` 清理 `users` 表，降低测试之间互相污染的风险。
- 后续需要进一步完善测试数据库隔离，避免测试误删开发数据。

## 2026-07-02: 登录接口先使用 mock token 验证完整链路

Decision:

- 新增登录模块，接口为 `POST /ai/v4/auth/login`。
- 登录请求使用 `LoginRequest(username, password)`。
- 登录成功响应使用 `LoginResponse(token, tokenType, expireIn)`。
- 当前 token 暂时返回 `"mock-token-" + user.id()`，用于验证“请求 -> 查数据库 -> 校验密码 -> 统一返回”的链路。
- 用户名不存在和密码错误统一使用 `INVALID_USERNAME_OR_PASSWORD`，避免向外暴露到底是账号不存在还是密码错误。

Reason:

登录链路需要先验证数据库查询、密码校验、业务异常、统一响应是否能串起来。JWT、权限过滤器、Spring Security 可以后置，否则一次引入过多概念会增加排查难度。

Consequences:

- 当前登录接口还不是生产级认证方案，mock token 不能用于正式鉴权。
- 下一步应先修复 `AuthService` 中错误码拼写问题，再补登录成功和密码错误测试。
- 后续正式登录需要替换 mock token 为 JWT，并考虑密码从 `{noop}` 替换为 BCrypt。

## 2026-07-01: 引入 UserRepository 接口作为数据访问边界

Decision:

- 新增 `UserRepository` 接口，定义用户数据访问层当前必须提供的能力：
  - `existsByUsername(String username)`
  - `save(String username, String email, String password)`
- `InMemoryUserRepository` 改为实现 `UserRepository`。
- `UserService` 改为依赖 `UserRepository` 接口，而不是直接依赖 `InMemoryUserRepository`。
- IDEA 项目 JDK 按用户确认配置为 21；本次命令行 Maven 测试输出显示实际使用 `Java 17.0.12`；`pom.xml` 中语言版本保持 Java 17，暂不在本次改动中升级语言版本。

Reason:

接数据库前先抽出 Repository 接口，可以把“业务规则”和“数据保存方式”分开。Service 只需要知道“可以检查用户名、可以保存用户”，不需要知道底层是内存 Map、MySQL、MyBatis 还是其他实现。后续切换到真实数据库时，主要新增或替换 Repository 实现，尽量不改 Controller 和 Service 的业务流程。

Consequences:

- 当前外部接口行为不变。
- 当前仍由 `InMemoryUserRepository` 提供实际数据保存能力。
- 后续接入 MySQL / MyBatis 时，应新增数据库版 Repository 实现，并处理 Spring Bean 冲突问题。
- 如果同时保留内存实现和数据库实现，需要使用 Profile、Primary 或移除其中一个 `@Repository`，避免 Spring 找到多个 `UserRepository` Bean。

## 2026-07-01: 对齐统一响应成功码与字段校验错误字段名

Decision:

- `ApiResponse.success(data)` 成功响应从 `SUCCESS / 请求成功` 改为 `0 / success`。
- `FieldErrorItem` 中字段错误原因从 `message` 改为 `reason`。
- MockMvc 测试同步按新契约断言成功响应。

Reason:

前后端共同接口契约要求成功业务码为字符串 `"0"`，成功文案为 `"success"`，字段校验错误项使用 `reason` 表达错误原因。统一响应属于前后端长期契约，必须尽早固定，避免后续所有业务接口继承错误格式。

Consequences:

- 成功响应更符合前后端契约。
- 校验错误响应更符合前端读取字段错误的约定。
- 后续新增接口时继续使用 `ApiResponse.success(data)` 即可继承统一成功格式。

## 2026-07-01: 引入 PasswordEncoder 作为密码处理边界

Decision:

- 新增 `PasswordEncoder` 组件，负责处理创建用户时传入的原始密码。
- `UserService` 在保存用户前调用 `passwordEncoder.encode(request.password())`。
- `InMemoryUserRepository.save` 接收处理后的密码，而不是直接接收原始请求密码。
- 当前编码结果使用 `{noop}` 前缀作为占位，不作为正式加密方案。

Reason:

密码处理属于业务安全逻辑，不应放在 Controller，也不应散落在 Repository。通过独立组件承载密码处理，可以先固定分层边界，后续替换为 BCrypt 或 Spring Security 的密码编码器时，对 Controller 和 Repository 的影响更小。

Consequences:

- 当前外部接口响应不变，仍不返回 `password`。
- 当前内部保存的密码不再直接使用 `request.password()`，而是使用 `encodedPassword`。
- `{noop}` 仅用于当前阶段验证分层，不满足生产安全要求。

## 2026-07-01: 使用内存 Repository 作为数据库前的过渡层

Decision:

- 新增 `User` 作为当前阶段的用户领域对象。
- 新增 `InMemoryUserRepository`，使用内存 `Map` 暂时代替数据库。
- `UserService` 不再用固定字符串模拟用户名重复，而是通过 Repository 查询用户名是否存在。
- 重复用户名测试改为先创建用户，再用相同用户名创建第二次。

Reason:

在直接接入数据库前，先用内存 Repository 可以让分层关系更清楚：Controller 负责 HTTP，Service 负责业务，Repository 负责数据访问。这样后续把内存实现替换成数据库实现时，Controller 和大部分 Service 业务流程不需要大改。

Consequences:

- 当前内存数据只在应用运行期间存在，重启后会丢失。
- 当前密码仍是临时明文保存，仅用于分层验证；后续必须接入密码加密。
- 后续接数据库时，应优先替换 Repository 实现，而不是把数据库逻辑写进 Controller。

## 2026-07-01: 明确项目文本编码使用 UTF-8

Decision:

- 项目文本文件统一使用 UTF-8 编码。
- 新增 `.editorconfig`，要求编辑器按 UTF-8 保存文件。
- 在 `pom.xml` 中显式声明 Maven 源码编码和报告输出编码为 UTF-8。

Reason:

项目中包含中文注释、校验提示文案和中文文档。统一编码可以避免不同 IDE、PowerShell、Maven 或协作环境使用不同默认编码导致中文显示乱码或保存损坏。

Consequences:

- 后续新增 Java、properties、Markdown 等文本文件应使用 UTF-8。
- 如果 PowerShell 输出仍显示乱码，优先判断是否是终端显示编码问题，不要直接认定源码已损坏。
- 需要检查文件真实内容时，可以通过字符码点或 IDE UTF-8 视图确认。

## 2026-07-01: 引入 UserService 承载用户业务逻辑

Decision:

- 新增 `UserService` 作为用户模块业务层。
- `UserController` 不再直接组装 `CreateUserResponse`，而是调用 `userService.createUser(request)`。
- 当前用 `username = "existing"` 模拟用户名已存在的业务场景。
- 该业务场景通过 `BizException(ErrorCode.USERNAME_ALREADY_EXISTS)` 表达。

Reason:

Controller 应负责 HTTP 接入、参数校验触发和统一响应包装；用户创建、用户名重复判断、后续密码加密和数据库保存属于业务逻辑，应放在 Service 层。提前引入 Service 可以为后续接数据库和真实注册流程保留清晰边界。

Consequences:

- 当前用户创建链路变为 `Controller -> Service -> Response DTO`。
- 业务异常链路已被 MockMvc 测试覆盖。
- 后续接数据库时优先扩展 `UserService`，不要把数据库或加密逻辑写进 Controller。

## 2026-06-30: 创建用户接口使用独立 Response DTO 且不返回 password

Decision:

- `POST /users` 的请求对象继续使用 `CreateUserRequest`。
- `POST /users` 的响应对象改为 `CreateUserResponse`。
- `UserController.createUser` 返回类型使用 `ApiResponse<CreateUserResponse>`。
- 成功响应只返回 `username` 和 `email`，不返回 `password`。
- MockMvc 成功测试明确断言 `$.data.password` 不存在。

Reason:

创建用户时前端需要提交密码，但密码属于敏感数据，不应出现在接口响应中。Request DTO 和 Response DTO 分开可以清楚表达“进来的数据”和“出去的数据”不是同一个契约。

Consequences:

- 后续新增用户接口时继续遵守 Request DTO / Response DTO 分离原则。
- 创建用户接口当前仍是最小链路验证版本，尚未接 Service、数据库和密码加密。
- 后续接入真实业务时，应在 Service 层处理密码加密和用户持久化，Controller 仍返回安全的 Response DTO。

## 2026-06-30: 后端接口统一前缀使用 /ai/v4

Decision:

- 后端接口统一前缀确定为 `/ai/v4`。
- 使用 Spring Boot 配置项 `server.servlet.context-path=/ai/v4` 统一添加应用前缀。
- Controller 中不手动写 `/ai/v4`，只写业务路径，例如 `/users`、`/verify/validation`。
- MockMvc 测试中使用完整请求路径，并通过 `.contextPath("/ai/v4")` 明确应用上下文路径。

Reason:

统一前缀属于应用级入口配置，不应散落在每个 Controller 上。这样新增接口时只关注业务路径；如果未来前缀变更，只需要改配置和相关测试契约。

Consequences:

- 外部真实访问地址形如 `POST /ai/v4/users`。
- Controller 仍写 `@PostMapping("/users")`。
- 测试应写 `post("/ai/v4/users").contextPath("/ai/v4")`。
- 服务器部署、前端代理和接口文档都应统一使用 `/ai/v4`。

## 2026-06-30: 用户创建接口先使用 Request DTO，后续拆出 Response DTO

Decision:

- 当前 `CreateUserRequest` 用于接收创建用户请求中的 `username`、`email`、`password`。
- 当前最小 `UserController` 已提供 `POST /users`，用于验证 JSON 接收、参数校验和统一响应链路。
- 下一步需要新增 `CreateUserResponse`，让接口返回值不包含 `password`。

Reason:

Request DTO 表示前端传入的数据，Response DTO 表示后端返回给前端的数据。创建用户请求中允许出现密码，但响应中不应返回密码。

Consequences:

- `UserController` 当前返回 `ApiResponse<CreateUserRequest>` 只是临时验证链路。
- 后续应改为 `ApiResponse<CreateUserResponse>`。
- 测试需要覆盖成功创建用户时响应中没有 `password`。

## 2026-06-29: validationFailed 使用 ErrorCode.VALIDATION_FAILED

Decision:

- `ApiResponse.validationFailed(data)` 不再直接写死 `VALIDATION_FAILED` 和默认提示文案。
- 该方法现在通过 `ErrorCode.VALIDATION_FAILED.code()` 和 `ErrorCode.VALIDATION_FAILED.message()` 生成校验失败响应。
- `ApiResponse.success(data)` 暂时不迁移到 `ErrorCode`，因为 `ErrorCode` 当前只用于错误场景。
- `ApiResponse.failed(code, message)` 保持接收外部传入的错误码和文案，用于 `BizException` 等动态错误响应。

Reason:

校验失败属于已定义错误，应和 `INTERNAL_ERROR`、业务错误一样从统一错误码来源读取，避免同一个错误码和文案在多个文件中重复维护。

Consequences:

- 修改默认校验失败文案时，只需要改 `ErrorCode.VALIDATION_FAILED`。
- 现有 MockMvc 校验失败测试继续约束 HTTP 400、`VALIDATION_FAILED`、字段错误列表和 `traceId: null`。

## 2026-06-26: 使用 MockMvc 覆盖统一响应的最小 HTTP 契约

Decision:

- 在 `Ai4BootVerifyApplicationTests` 中保留一个成功请求和两个失败请求的 MockMvc 测试。
- 成功请求断言 HTTP 200、`SUCCESS`、成功文案、`data` 内容和 `traceId: null`。
- 失败请求分别断言缺少 `username` 与非法 `email` 时的 HTTP 400 和字段错误。

Reason:

Controller、Bean Validation 和全局异常处理共同决定 HTTP 响应。通过请求级测试固定该最小契约，可防止后续重构只保持编译通过却改变返回格式。

Consequences:

- 当前测试类共有 4 个测试方法，其中 3 个为 HTTP 契约验证。
- 新增统一响应字段或改变状态码时，应同步更新这些断言和项目记忆文档。

## 2026-06-26: 使用 ApiResponse 作为 HTTP 接口统一响应外层

Decision:

- 所有后续 HTTP 接口成功时使用 `ApiResponse<T>` 作为返回类型。
- 校验失败、业务异常和未处理异常由 `GlobalExceptionHandler` 转换为同一外层结构。
- `ApiResponse.success(data)` 负责构造成功响应；`data` 保留具体业务数据类型。

Reason:

前端只需按固定的 `code`、`message`、`data`、`traceId` 字段解析响应。Controller 不需要重复拼装响应外层，异常路径也能与成功路径保持一致。

Consequences:

- `POST /verify/validation` 已作为最小示例接入该结构。
- 新增 Controller 时优先返回 `ApiResponse<具体返回类型>`，而不是直接返回实体或请求对象。
- `traceId` 暂时为 `null`；接入请求追踪后再统一填充。

## 2026-06-26: ErrorCode 作为已定义错误的集中声明位置

Decision:

- `ErrorCode` 保存错误码和默认文案，例如 `INTERNAL_ERROR`、`VALIDATION_FAILED`、`USERNAME_ALREADY_EXISTS`。
- 业务代码可使用 `new BizException(ErrorCode.XXX)`，避免重复手写错误码和文案。
- 当前未处理异常分支已经使用 `ErrorCode.INTERNAL_ERROR`。

Reason:

集中声明可以减少拼写错误和同一错误出现多种文案的风险；变更默认文案时只需修改一处。

Consequences:

- 当前实现尚未完全迁移：`ApiResponse.validationFailed` 仍直接写入 `VALIDATION_FAILED` 的错误码和文案。
- 后续若继续统一，应在修改 `ApiResponse` 时让该方法从 `ErrorCode.VALIDATION_FAILED` 读取值，并补充对应测试。

## 2026-06-26: 使用仓库 Markdown 作为长期项目记忆

Decision:

将长期有效的项目上下文、决策和工作进度写入仓库内 Markdown 文件，而不是只依赖聊天上下文。

Reason:

长会话上下文可能被压缩、遗漏或混入不准确记忆。仓库文件可以被重复读取、审查和版本管理，更适合作为长期事实来源。

Consequences:

- `AGENTS.md` 记录 Codex 在本仓库中的工作协议。
- `docs/PROJECT_CONTEXT.md` 记录稳定项目事实。
- `docs/DECISIONS.md` 记录重要决策和原因。
- `docs/WORK_LOG.md` 记录阶段性进展、验证结果和下一步。
- 后续跨会话继续任务时，应先读取这些文件，再判断当前状态。

## 2026-07-17: 使用显式 RBAC 和方法权限注解

Decision:

- 权限关系使用 `sys_user`、`sys_user_role`、`sys_role`、`sys_role_menu`、`sys_menu`。
- 接口通过 `@RequirePermission("system:resource:action")` 声明权限。
- JWT 拦截器先认证用户，权限拦截器再查询数据库权限；无权限返回 HTTP 403。
- `super_admin` 也必须拥有真实的用户角色和角色菜单关系，不在代码中直接跳过鉴权。

Reason:

显式授权可以审计并与普通角色共用同一套逻辑，避免角色名称形成隐藏后门；方法注解使接口权限要求可见且集中由拦截器执行。

Consequences:

- `/auth/perms` 与后端接口鉴权使用同一权限查询来源。
- 新增受控接口时必须先创建对应 `BUTTON` 权限，再添加注解，避免所有用户被锁在接口之外。
- 当前权限码统一为 `system:resource:action`。

## 2026-07-17: 授权接口采用整体替换

Decision:

- `PUT /roles/{roleId}/menus` 整体替换角色菜单，并自动补齐父菜单。
- `PUT /users/{userId}/roles` 整体替换用户角色。
- 列表字段不能为 `null`，普通角色或普通用户允许使用空数组清空关系。

Reason:

前端提交最终勾选状态比维护增量添加、移除接口更简单；事务内先删除再批量插入可以保证结果与提交状态一致。

Consequences:

- 前端不能把 PUT 当作追加操作。
- 必须保留超级管理员保护规则。

## 2026-07-17: Flyway V7 初始化基础权限

Decision:

- 使用 V7 初始化三个系统管理页面和 14 个基础按钮权限。
- 迁移通过 `path`、`role_code`、`perms` 查找，不依赖不同环境中的固定主键。
- 迁移给启用的 `super_admin` 授权，但不自动把任意后台用户提升为超级管理员。

Reason:

基础权限必须先于接口注解存在，且开发、测试、服务器数据库 ID 不一致，不能使用硬编码 ID 或依赖前端手工初始化。

## 2026-07-21：已执行的 Flyway 迁移不可直接修改

Decision:

- `flyway_schema_history.success = 1` 的版本化迁移视为不可变文件。
- 后续结构调整必须新增更高版本迁移，不通过编辑旧 SQL 改变已部署历史。
- checksum mismatch 只允许在确认数据库实际结构、原始脚本和目标脚本完全一致后执行 repair；repair 不能代替结构修复。
- MySQL DDL 失败可能留下部分已提交结构，失败后必须同时检查 schema history 和实际表结构，再决定恢复或补偿方案。

Reason:

开发、测试和服务器环境会分别记录迁移 checksum。编辑已经执行的脚本会让环境历史与代码不一致；盲目 repair 只会掩盖差异。

## 2026-07-21：合伙人与 Skill 使用多对多模型

Decision:

- 独立保存 `ai4_partner` 和 `ai4_skill`。
- 使用 `ai4_partner_skill_relation(partner_id, skill_id)` 表达多对多关系，并使用联合主键阻止重复绑定。
- 关联表可保存 `sort_order`，表达 Skill 在特定合伙人下的顺序。
- Skill 的文件夹树保存在 `skill_content JSON`；Skill 名称、提示词和删除状态属于 Skill，而不属于合伙人或关联表。
- 二进制文件进入对象存储，JSON 只保存目录、文件元数据和对象 key/URL。

Reason:

一个合伙人可以绑定多个 Skill，同一个 Skill 也要被多个合伙人复用。把 `partner_id` 放在 Skill 表或把 Skill 名称 JSON 放在 Partner 表都无法正确表达该关系。

## 2026-07-21：字典模块采用精简双表设计

Decision:

- 使用 `ai4_dict_type` 和 `ai4_dict_item` 两张表。
- 字典项通过 `type_id` 关联字典类型，不重复保存 `dict_code`。
- 保留名称、编码、值、顺序、状态、备注、时间字段，并提供可空的 `extra JSON`。
- 删除若依中与其前端实现耦合或当前项目暂时无支撑的字段：`css_class`、`list_class`、`is_default`、`create_by`、`update_by`。
- 普通字典值使用字符串，不将所有值强制保存为 JSON。
- 当前使用物理删除，不增加 `del_flag`；禁用类型时其字典项不可用，删除类型前必须先处理字典项。

Reason:

双表保留了类型管理能力；使用 `type_id` 消除了字典项中重复的类型编码。精简字段可以避免把若依的 UI 约定和尚未建立的审计机制带入当前项目。

## 2026-07-23：字典编码作为稳定程序契约

Decision:

- `dict_code` 保留且全局唯一，由创建请求显式传入，创建后不允许普通修改。
- 前端通过接口文档或集中常量知道业务字段对应的 `dict_code`，不直接依赖数据库 ID。
- `id` 用于 `sys_dict_type` 与 `sys_dict_item` 内部关联；`dict_name` 仅用于可变展示名称。

Reason:

不同环境的主键可能不同，展示名称也可能调整。业务编码为前端、后端和数据库提供稳定查询入口，例如 `user_status`。

## 2026-07-23：字典 Service 隔离持久化结果与业务结果

Decision:

- Repository 使用 `Optional<T>` 表达单条数据可能不存在，使用 `List<T>` 表达零到多条结果，写操作返回影响行数。
- Service 不向 Controller 暴露 `Optional` 或 SQL 影响行数；详情不存在时转换为业务异常，创建和修改返回完整 `DictType`。
- 创建后和修改后重新查询数据库，确保响应包含数据库生成的 `created_at`、`updated_at`。

Reason:

Repository 负责忠实表达数据库结果，Service 负责业务语义，Controller 只负责 HTTP 输入输出。

## 2026-07-27：题目列表、详情和完整修改边界

Decision:

- 题目列表返回 `QuestionListResponse`，不加载选项；题目详情返回 `QuestionDetailResponse`，一次携带四个选项。
- Repository 返回数据库 model，Service 负责转换为响应 DTO，Controller 不直接暴露数据库实体。
- 题目修改采用一个请求整体提交题目主体和 A-D 四个选项，并使用 `@Transactional` 保证原子性。
- 选项标识不可通过修改接口与其他选项 ID 互换；请求中的 ID、`optionKey` 必须与当前题目原记录对应。
- 删除题目采用软删除，当前不删除选项；后续恢复能力可以复用保留的数据。

Reason:

列表不加载选项可避免逐题查询产生的额外 SQL；详情一次返回完整数据便于前端编辑。整体事务修改可避免题目与部分选项更新成功、其余选项失败的不一致状态。

## 2026-07-27：题目权限标识

Decision:

- 题目接口使用 `question:list`、`question:update`、`question:delete`。
- V15 负责创建题目管理页面和按钮权限，并自动授权给启用状态的 `super_admin`。
- 页面路径 `/questions` 和组件 `question/index` 是后端暂定值，前端确定真实路由后可通过菜单管理接口调整。

Reason:

项目没有超级管理员代码绕过机制，所有角色包括 `super_admin` 都必须拥有数据库中的显式权限关系。

## 2026-07-27：题目选项不区分对错

Decision:

- 每个选项只维护选项内容和选择后的影响，不再保存 `correct`。
- 保留 `questionType`，用于区分用户只能选择一个选项还是可以选择多个选项。
- 使用 V16 删除 `ai4_question_choice.correct`，不修改已经执行的 V8 或 V14。
- 删除请求 DTO、响应 DTO、model、Repository 和 Service 中全部正确答案相关字段与校验。

Reason:

当前题目用于呈现不同选择产生的后续影响，不用于判分；继续维护正确答案会制造错误的业务含义。
## 2026-07-30：Skill 删除保护与绑定数量

- Skill 列表项除 `skillId`、`skillName`、`skillContent` 外，统一返回 `partnerCount`。
- `partnerCount` 统计 `ai4_partner_skill_relation` 中当前 `skill_id` 的关联记录数；没有绑定时返回 0。
- Skill 存在任意合伙人绑定关系时禁止逻辑删除，返回 `SKILL_ASSIGNED_TO_PARTNER`。
- 删除接口不自动解除绑定，必须先由合伙人 Skill 解绑功能清除关联。
- Repository 的软删除 SQL 同时使用 `NOT EXISTS` 再次保护，不能只依赖 Service 的预检查。
## 2026-07-30: 合伙人接口与图片字段

- 合伙人管理提供分页列表、详情、新增、完整修改和逻辑删除五个接口。
- 合伙人必须绑定 1 至 6 个有效 Skill，绑定列表不允许重复，并保留请求中的排列顺序。
- 新增和修改在事务内保存合伙人及 Skill 关联；修改采用整体替换关联的语义。
- 删除时先物理删除合伙人 Skill 关联，再逻辑删除合伙人主记录，使 Skill 绑定计数立即更新。
- 列表通过一次分页查询和一次批量关联查询组装 Skill 名称，避免 N+1 查询。
- `large`、`medium`、`small`、`fullBody`、`avatar` 五个图片 URL 暂时均为非必填；空文本归一为
  `null`。文件格式、大小、尺寸和 MIME 类型留给后续统一上传接口校验。
- V20 创建 `partner:list/create/update/delete` 权限并授权给启用状态的 `super_admin`。
## 2026-07-30: Skill 与合伙人数据完整性

- Skill 列表的 `partnerCount` 只统计 `del_flag = 1` 的有效合伙人。
- 合伙人新增和修改绑定 Skill 时，在事务内使用 `SELECT ... FOR UPDATE` 锁定有效 Skill，避免绑定与
  Skill 删除并发执行后产生无效关联。
- 新增 `GET /skills/options`，返回全部有效 Skill 的 `skillId` 和 `skillName`。
- 合伙人使用整数 `version` 实现乐观锁；详情返回版本，修改必须回传，成功后版本自增。
- 未删除 Skill 名称和未删除合伙人名称均唯一；逻辑删除后允许重新使用名称。
- V21 使用生成列和唯一索引保证并发创建时数据库层仍能阻止重名。
## 2026-07-31：唯一课包与项目模型

Decision:

- 课包作为系统级唯一配置，只提供查询接口，不允许通过后台接口增删改。
- 不写死课包 ID；Repository 查询最多两条有效课包，只有恰好一条时才允许查询课包和创建项目。
- 删除原课节、课程内容领域及空表，使用独立的 `ai4_project` 表表达当前项目业务。
- 项目创建由后端自动绑定唯一有效课包，避免前端传入错误或跨课包的 `packageId`。
- 项目编码允许修改且允许多个有效项目重复使用；项目业务关联统一使用 `project_id`，不使用项目编码作为唯一标识。

Reason:

当前产品只有一个课包，维护完整课包 CRUD 和旧课程内容结构会制造无效复杂度。后端自动绑定唯一课包可以固定数据归属；项目编码仅作为项目配置字段展示，不参与业务关联。
## 2026-07-31：项目领取任务配置边界

Decision:

- 一个项目只允许一条领取任务配置，任务不提供删除接口。
- 任务类型采用 JSON 保存大类和格式，后端维护固定白名单并校验对应关系。
- 任务与合伙人使用独立关联表，必须绑定2至4个有效且不重复的合伙人。
- 图片字段只保存任务专用上传接口返回的 URL；创建和修改任务时三个图片 URL 均为必填。
- 公司名称生成接口不接收重复的项目和任务上下文，而是按 `projectId` 从数据库读取。
- AI 输出必须由后端解析、去重并校验；不足60个时最多重试3次，仍不足则整体失败。

Reason:

一项目一任务属于固定配置关系，数据库唯一约束比单纯 Service 检查更可靠。任务类型集合规模固定且只随任务整体读写，JSON 比拆分多张类型表更直接。AI 输出不可信，必须在业务边界内验证后才能返回。

## 2026-07-31：公司名称候选使用任务表 JSON 保存

Decision:

- 不为公司名称候选创建独立表，在 `ai4_project_task.company_names JSON` 中保存固定60个名称。
- 生成成功后整体覆盖旧数组；生成失败时保留原数据。
- 不设置单独的最终公司名称字段；V25 删除 `ai4_project_task.company_name`，所有公司名称统一保存在 `company_names` JSON 数组中。
- 候选项没有独立主键，修改接口使用数组下标定位，并整体写回 JSON。
- 修改后继续校验名称必填、最多7个字符、指定后缀和任务内不重复。

Reason:

候选集合固定、只属于单个任务且始终整体展示和替换，不需要独立查询、关联或生命周期，因此 JSON 能以更低复杂度满足当前需求。
- 项目生成与创业报告采用两张独立表，不合并为通用配置表，以隔离模块并方便后续独立扩展。
- 两个模块都是项目级单例配置，标准字段使用 `TEXT NOT NULL`，当前只校验非空，不限制字符数，也不提供删除接口。
## 2026-08-03：OSS 作为内部统一存储服务，业务接口负责上传前校验

Decision:

- 不对外暴露可任意指定目录的通用文件上传接口；统一能力下沉为 `OssStorageService`，由具体业务 Service 调用。
- 合伙人的五张图片使用一次 multipart 请求上传，不使用数组下标区分图片，而使用 `large`、`medium`、`small`、`fullBody`、`avatar` 五个固定参数名。
- 合伙人业务先校验五张图片全部存在、单张不超过 5MB、扩展名属于 png/jpg/jpeg，再调用 OSS 服务。
- 上传和新增合伙人维持两个接口：先上传取得五个 URL，再创建合伙人；五个 URL 在新增与完整修改时均为必填。
- 图片业务归属以 `ai4_partner` 的五个 URL 字段为准，不要求 OSS 对象路径包含尚未生成的 `partnerId`。
- OSS 对象键采用业务前缀、日期和 UUID，避免原文件重名导致覆盖；`publicBaseUrl` 仅用于拼接外部访问 URL。

Reason:

- 各业务图片的格式、大小、尺寸规则不同，不应固化在底层 OSS 客户端中。
- 五张合伙人图片语义固定，具名 multipart 参数比 `MultipartFile[]` 更明确，也避免前后端依赖数组顺序。
- 合伙人创建前尚无 `partnerId`，使用数据库 URL 字段建立归属能保持当前新增流程简单。

Known limitations:

- 上传成功但未提交新增时会产生暂未关联的 OSS 对象，后续需要孤立文件清理策略。
- OSS 上传不参与数据库事务；多文件上传中途网络失败时，已成功的对象不会自动回滚，后续可增加失败补偿删除。
- 当前只校验扩展名，尚未校验 MIME 和真实图片内容。
- 当前权限注解只支持单个权限码，上传接口尚未增加独立 `partner:upload` 权限。

## 2026-08-05：任务图片采用项目级三文件上传接口

Decision:

- 使用 `POST /projects/{projectId}/task/images/upload` 一次上传发布任务图、领取任务图和公司名片。
- 三个 multipart 参数固定命名为 `publicTask`、`claimTask`、`companyCard`，并与任务 DTO 和响应字段保持一致。
- `projectId` 参与有效项目校验和 OSS 对象键分区，不作为未使用的形式参数保留。
- 任务图片 URL 在任务创建与完整修改请求中均为必填。
- 当前不增加任务图片格式、MIME、尺寸或业务大小限制，也暂不增加独立上传权限。

Reason:

- 三张图片属于同一个项目任务且语义固定，一次具名 multipart 请求比三次上传或文件数组更容易建立字段对应关系。
- OSS 上传与任务数据写入仍是两个步骤，避免把远程文件存储和数据库事务强行绑定；前端先上传，再提交返回的 URL。
- 在对象键中加入 `projectId` 便于按项目定位资源；日期和 UUID 继续避免重名覆盖。

Known limitations:

- 多文件上传中途失败时，已经上传成功的对象不会自动回滚。
- 上传成功但任务保存失败或用户放弃提交时，可能形成孤立 OSS 对象，后续可统一增加清理策略。

## 2026-08-06：项目沙盘使用主表、分段表和属性表

Decision:

- 沙盘采用三张关系表，不把分段和属性保存为JSON；一个项目通过主表唯一约束只允许一份沙盘配置。
- 分段时间使用 `DECIMAL(10,2)` 保存总秒数，允许空分段列表且不限制分段数量、名称重复和时间重复。
- 分段和属性均不保留排序字段；分段按时间和ID查询，属性按ID查询。
- 沙盘属性必须为3至6个，名称最长10个字符，图标可空，目标值和初始值使用整数且允许负数。
- 属性不做全量删除重建：未变化记录保留ID，修改记录删除后新增，新属性插入，请求中缺失的旧属性删除。
- 视频上传与沙盘保存分离，上传接口只返回OSS URL；四个沙盘接口复用现有项目权限。
- 当前不考虑题目与属性关系，也不新增沙盘测试用例和Flyway迁移。

Reason:

- 分段和属性是可独立查询和维护的集合，关系表比JSON更便于后续业务关联。
- 差异同步避免未修改属性无意义地更换ID，同时符合“属性内容变化即替换为新记录”的业务语义。
- 复用项目权限避免在手动维护数据库阶段额外同步菜单和权限数据。

## 2026-08-07：暂时移除独立测试库和数据库集成测试

Decision:

- 删除当前全部数据库集成测试、测试环境配置以及仅供测试使用的两个Spring Boot Starter。
- 普通打包不再要求 `AI4_TEST_DB_PASSWORD`，不再连接独立测试库。
- 当前阶段使用开发库和ApiPost完成人工接口验证。

Trade-off:

- 打包流程更简单，但项目失去自动回归保护；业务修改后必须人工覆盖新增、查询、修改、权限和异常分支。
- 后续恢复测试时，优先使用可自动创建和销毁的隔离数据库，避免重新依赖长期共享的手工测试库。

## 2026-08-11：项目题目管理替代旧题库接口

Decision:

- 题目管理从旧 `ai4_question_bank` / `ai4_question_choice` 迁移为项目维度题目配置，当前接口只读写 `ai4_project_question`、`ai4_project_question_option`、`ai4_project_question_option_attribute`。
- 不再区分单选/多选，也不再固定 A-D 四个选项；选项数量由请求体决定。
- 选项整体影响类型保存为 `GOOD/BAD`，但每个选项对每个沙盘属性的影响值单独保存在关联明细表中。
- `month` 是前端计算的展示顺序字段，后端不自动生成、不负责排序，只保存和返回。
- 修改题目不允许换项目，因此 `PUT /questions/{questionId}` 请求体不包含 `projectId`。
- 修改时采用“更新题目主表 + 删除旧选项/属性影响 + 插入新选项/属性影响”的整体重建方案。
- 删除题目只更新题目主表 `del_flag = 2`，不删除选项和属性影响子表。
- 启用题目上限为同项目 36 个，由后端在新增启用题目或禁用改启用时校验。

Reason:

- 当前后台配置量小，整体重建选项比逐条 diff 更清晰，事务边界更简单。
- 删除题目保留子表数据有利于后续排查历史配置。
- 项目和属性数据由前端/后台配置人员按流程选择，当前阶段按用户要求不做过重校验。

Trade-offs:

- 不校验属性全集和影响值格式会降低后端保护能力，错误配置可能进入数据库；后续如前端联调暴露问题，可在 Service 增加轻量校验。
- 图片上传接口暂未细分权限，后续可以增加 `question:upload` 并同步菜单权限数据。

## 2026-08-11：题目图片上传响应只返回 URL

Decision:

- `POST /questions/image/upload` 的业务响应只返回 `url`。
- OSS `objectKey` 保留在通用存储服务内部，不向题目管理前端暴露。

Reason:

- 题目新增和修改只需要将可访问 URL 保存到 `imageUrl`，前端不使用 `objectKey`。

## 2026-08-11：沙盘属性保留 ID 并保护题目关联

Decision:

- 沙盘属性 `target`、`initial` 的 Java 类型统一改为 `BigDecimal`，对应数据库 `DECIMAL(10,2)`。
- 修改已有沙盘属性时在原 ID 上执行 `UPDATE`，不再删除后新增。
- 删除沙盘属性前检查 `ai4_project_question_option_attribute`；已被题目引用时返回 `SANDTABLE_ATTRIBUTE_IN_USE`。

Reason:

- 避免目标值和初始值的小数部分被 Java `Integer/getInt` 截断。
- 题目选项通过属性 ID 建立关联，修改属性时必须保持 ID 稳定。
- 删除保护避免题目留下无法解析属性名称的悬空关联。
## 2026-08-15：班级直播课列表关联与时间口径

- 班级、项目和直播课通过 `ai4_class_live_course` 关联，不复用旧项目的课程内容关联表。
- 第一阶段只实现 `GET /live-courses` 分页条件查询，不实现直播课新增、修改、删除和详情。
- `live_course.course_time` 按毫秒时间戳处理，API 时间参数和响应统一使用 `Asia/Shanghai` 与
  `yyyy-MM-dd HH:mm:ss`。
- 出勤人数和迟到人数按 `live_user_course_record` 中 `be_duty = 1`、`be_late = 1` 分别汇总，并通过 `attendanceCount`、`lateCount` 两个数值字段独立返回。
- 列表只返回同时关联有效班级和有效项目的未删除直播课。

## 2026-08-15：直播课新增、房间号与课程ID

Decision:

- AI4 使用 Redis `db11`，房间序号 Key 固定为 `ai4:live:room:seq:1`，普通直播课房间号格式为类型前缀 `1` 加递增序号。
- `room_id` 是业务后端生成并保存的声网频道名，不是声网预先分配的房间标识。
- `course_id` 保持旧直播业务的数字字符串口径，使用项目内雪花ID生成器；worker ID 通过 `AI4_SNOWFLAKE_WORKER_ID` 配置，默认 `1`。
- 建课请求提交 `classId`、`projectId`、`courseDate` 和 `courseDuration`；日期与班级 `start_time` 按 `Asia/Shanghai` 组合成毫秒时间戳。
- 授课教师、录制设置、拖堂时长和房间类型继承班级配置；班级与项目必须属于同一个课包。
- `live_course` 与 `ai4_class_live_course` 在一个数据库事务内写入，同班级同项目允许重复创建直播课。

Trade-offs:

- Redis递增不参与数据库事务，数据库写入失败时房间序号可能出现空洞，但不会产生重复房间号。
- 多个应用实例同时向同一直播课库生成雪花ID时，必须配置不同的 worker ID；当前默认值适用于单实例部署。
- 当前没有自动化数据库测试，关联表字段和默认值仍需通过开发库接口调用验证。

## 2026-08-17：CRM创建班级自动排课与停课处理

Decision:

- 保留旧CRM兼容路径 `/external/aiClass/add` 和 `/external/aiClass/openClose`。
- CRM创建正常班级时，按课包下有效项目一次性创建全部未来直播课；一个项目对应一节直播课。
- 第 `i` 个项目按 `startClassTime + i * courseRule` 天排课，时长由班级开始、结束时间计算。
- 项目排课顺序按 `ai4_project.sort ASC, id ASC`；已有项目的初始值由管理员手动补齐，新项目自动追加到当前最大顺序之后。
- 项目顺序调整不修改已经存在的直播课；后续新建班级时才读取最新的 `sort` 顺序创建直播课。
- CRM自动课程写入 `system_default=0`；后台手动课程继续写入 `system_default=1`。
- CRM班级及其自动直播课固定使用 `room_type=2`，与旧项目CRM大班课链路保持一致。
- 停课只逻辑删除 `status=0` 的未开始课程并删除关联，已开始及已结束课程保持不变。
- 恢复正常时只补建没有有效课程关联的项目，避免给历史项目重复建课。
- CRM `classId` 重复创建按幂等处理，返回已有内部班级ID。
- CRM专用认证暂未实现；`/external/**` 暂时排除后台JWT和权限拦截器，后续接入独立外部访问校验。

Reason:

- 当前项目一个项目对应一节直播课，直接使用 `ai4_class_live_course` 建立班级、项目和直播课关系。
- 一次性写入未来计划课比按 `courseRule` 使用定时任务逐节建课更简单，也与旧项目行为一致。
- 逻辑删除未开始课程可以保留审计信息，同时避免破坏已产生出勤和声网数据的历史课程。

## 2026-08-17：补齐 CRM 班级、学员、教师与课包统计接口

Decision:

- CRM修改班级时更新完整排课配置，保留已开始和已结束课程，逻辑删除并重建未开始课程。
- CRM学员同步继续使用手机号匹配 `el_user`；绑定写入或恢复 `ai4_class_student.state=1`，移除只更新为 `state=2`。
- 批量学员请求中任一手机号不存在时整体失败，避免 CRM 与本地班级成员出现部分同步。
- CRM教师以 `crm_teacher_id` 作为外部业务标识，以雪花ID作为本地主键；账号保持全表唯一。
- 修改教师时密码为空、空字符串或纯空白则保留原密码，有内容时重新 BCrypt 后更新。
- 课包直播课数量统计实际已经创建且 `del_flag=1` 的 `live_course`，通过班级和 `ai4_class_live_course` 归属到课包，不按项目数量推算。

Reason:

- 班级配置变化不应修改已经产生上课、出勤或声网数据的历史课程。
- 状态恢复比删除重建学员关联更适合保留班级成员历史。
- CRM修改教师资料时通常不会同时修改密码，空密码不应覆盖已有 BCrypt 哈希。
- 该接口用于读取课包下当前实际有效直播课数量，因此生命周期 `status` 不作为过滤条件，只排除逻辑删除课程。

## 2026-08-18：声网 Token 第一版约定

Decision:

- 使用 `io.agora:authentication:2.1.3` 的 Token2 Builder 统一生成 RTM、RTC Token。
- 后台系统 RTM 用户账号固定为 `-2`，系统 RTM Token 有效期固定为 24 小时。
- 观察者 RTM Token 有效期固定为 24 小时，网页录制 RTC Token 有效期固定为 72 小时，与旧项目第一版录制链路保持一致。
- 网页录制 RTC Token 第一版继续使用 `ROLE_PUBLISHER`，保持与旧项目录制链路兼容。
- 声网 REST API 使用 `restApiKey:restApiSecret` 生成 Basic Authorization 请求头。
- Token 仅在服务端按需生成，不入库、不写日志，暂不提供公开 Token Controller。

Reason:

- 第一版优先保持旧项目录制链路兼容性；RTC Token 绑定房间号和录制 UID，后续完成真实录制联调后再评估是否缩短有效期。
- 第一版优先保证旧录制页面兼容性，待完整录制链路验证后再评估是否将 RTC 角色收紧为订阅者。

## 2026-08-24：教师、后台用户与角色关系

Decision:

- 教师资料继续以 `live_teach` 为完整数据源，后台认证继续以 `sys_user` 为账号数据源，不将两张表强行合并。
- 使用 `live_teach.user_id` 关联 `sys_user.user_id`，角色只通过 `sys_user_role` 关联，不在 `live_teach` 或 `sys_user` 增加角色字段。
- CRM 新增教师时必须同时指定 `roleIds`，并在一个数据库事务中完成后台用户、教师资料和角色关系写入。
- CRM 修改教师不接收角色字段；角色变更统一复用后台用户角色授权接口。
- 两张表需要保持账号和密码一致；CRM 修改密码及后台重置密码都必须同步更新两张表。
- 教师查询接口返回 `userId`，供前端复用现有用户角色查询、角色修改和密码重置接口。

Reason:

- `live_teach` 包含 CRM 教师资料和直播登录信息，不能由较精简的 `sys_user` 替代；`sys_user` 又是现有 JWT、RBAC 链路的稳定入口。
- 通过关联字段复用现有认证和权限体系，可以避免复制角色字段、角色接口和密码重置逻辑。
- 创建过程使用事务，避免只生成后台用户、只生成教师资料或缺少角色关系的半成品数据。

## 2026-08-25：直播课详情查询口径

Decision:

- 详情接口使用 `courseId` 作为路径参数，不使用 `roomId` 作为业务主键。
- 学员明细按照产品要求分页，复用全局 `PageResponse`，默认每页 10 条。
- 迟到时间按迟到秒数返回，由 `join_time - course_time` 计算，负数按 0 处理。
- 出勤统计读取 `live_user_course_record` 已落库的最终状态，不在详情查询时重新推导并回写。
- 学员查询以班级有效成员为基础左关联上课记录，不沿用旧项目只查询有上课记录学员的方式。
- 详情录像读取用于展示的 `live_course_video`，不直接依赖声网录制过程表 `live_video_record`。

Reason:

- `courseId` 是直播课主键，房间号属于声网接入标识，职责不同。
- 缺勤学生没有进入记录，只从 `live_user_course_record` 查询会遗漏缺勤人员。
- 查询接口只读最终统计结果，可以避免接口调用产生状态修改，并保持声网事件处理与后台展示解耦。

## 2026-08-25：声网教室创建时机与幂等策略

Decision:

- 沿用旧项目的时间窗口，在直播课计划开始前10分钟创建声网教室，不在 CRM 排课时一次性创建全部未来教室。
- 定时任务默认每3秒扫描一次；`ai4.agora.enabled=false` 时不查询数据库、不调用声网。
- 不复用 `live_course.status` 标记教室是否创建；该字段继续只表示未开始、进行中、已结束和拖堂结束。
- 使用 Redis 保存每门直播课的创建成功标记，并使用带1分钟过期时间的课程级锁防止多实例重复创建。
- 声网返回 HTTP 409 表示教室已经存在，按幂等成功处理并补写 Redis 成功标记。
- 创建失败不修改课程数据，保留下一轮定时任务重试机会。

Reason:

- 提前创建全部未来教室没有必要，也偏离旧项目已经验证的开课前创建方式。
- 旧项目创建教室后将课程状态改为1，但本项目的状态1明确表示正在上课，不能承担教室创建标记职责。
- Redis 已是当前直播课房间号生成的基础设施，使用独立成功标记可以避免增加仅供接入层使用的数据库字段。

## 2026-08-26：修正灵动课堂 REST API 鉴权方式

Decision:

- 创建教室、更新教室属性和查询教室事件统一使用应用级 `EducationToken`。
- `EducationToken` 使用 `EducationTokenBuilder2.buildAppToken` 根据 App ID 和 App Certificate 生成，有效期为24小时。
- 灵动课堂 REST 请求统一使用 `Authorization: agora token={EducationToken}`。
- 原有系统 RTM Token 继续保留给 RTM 场景使用，不再作为灵动课堂 REST API 的鉴权凭证。
- 云端录制 REST API 继续使用 REST API Key/Secret 生成 Basic Authorization，不受本次修改影响。

Reason:

- 当前声网灵动课堂 REST API 要求使用应用级 EducationToken；旧项目使用的 `x-agora-token`、`x-agora-uid` 属于旧版鉴权方式，会导致当前创建教室接口返回 HTTP 401。
- 应用级 Token 的作用域包含创建教室、设置房间和查询房间，正好覆盖当前后台的三类灵动课堂 REST 调用。

## 2026-08-26：声网课堂事件与考勤同步口径

Decision:

- 定时拉取 `cmd=20` 用户进出事件和 `cmd=1` 课堂状态事件，每轮固定先处理进出事件，再处理状态事件。
- 客户端进入声网课堂时继续使用旧项目约定的 `1_{teacherId}` 和 `2_{studentId}` 作为 `userUuid`，事件落库前去掉角色前缀并校验教师或学生是否属于当前直播课。
- `live_course_room_user` 保存当前在线用户；教师和学生的最终上课记录分别保存到 `live_teach_course_record`、`live_user_course_record`。
- 第一次进入时间用于判断迟到；最后一次离开时间用于判断早退；重复进入会清除临时早退标记，课堂关闭时统一结算。
- 进入过课堂即出勤；课堂关闭时仍未出勤的有效班级学生记为缺勤；房间关闭时仍在线的用户使用关闭时间补齐离开时间且不记早退。
- 声网状态 `1/2/3` 分别同步为直播课进行中、拖堂中、完全关闭；状态1记录实际开始时间，状态3记录实际关闭时间。
- 使用课程级 Redis 锁避免多实例并发同步，使用 `roomId + cmd + sequence` 保存事件幂等标记，分页使用声网返回的 `nextId` 游标。

Reason:

- 声网同一事件可以重复查询，必须使用课堂内唯一递增的 `sequence` 防止重复落库。
- 用户可能因网络重连多次进出，只根据任意一次离开判断早退会产生误判，应以课堂结束时的最终状态结算。
- 状态3出现前仍可能有最后一批离房事件，先处理进出事件可以减少离开时间遗漏。

## 2026-08-26：声网云端录制触发与持久化

Decision:

- 沿用旧项目“首个真实用户进房后启动、课堂完全关闭后停止”的录制时机，不在创建声网教室时提前启动录制。
- 只有 `live_course.record=1` 的课程参与录制，同一课程只允许存在一条准备中、暂停中或录制中的有效任务。
- 录制过程状态保存到 `live_video_record`；停止后将 MP4/HLS 文件合并写入 `live_course_video`，后台详情继续只读取最终录像表。
- 录制网页继续使用旧项目 URL 参数约定；观察者 `userId/token/roleType` 按旧键名写入 Redis，并通过随机 `secretKey` 调用公开的暂停、继续和心跳接口。
- 录制启动由课堂进房事务提交后的应用事件触发，课堂状态3事务提交后触发停止，避免声网调用失败破坏考勤事件事务。
- 使用 Redis 课程锁、录制 UID 占用键、回调幂等键和心跳键；定时扫描已关闭课程的活动录制，补偿应用重启或临时停止失败。
- 声网回调接口第一版沿用旧项目，不增加新的签名协议；对录制网页接口使用数据库中的随机密钥校验。

Reason:

- 没有真实用户进房或没有音视频发布时提前录制，只会增加无效录制资源和费用。
- 过程表和最终展示表分离，可以保留声网任务状态，同时让后台查询不依赖供应商过程数据。
- 外部声网调用不应回滚已经确认的进房、离房和课堂状态数据，事务提交后触发更符合当前模块边界。
- 旧录制网页或相关客户端服务必须能读取当前应用写入的观察者 Redis 数据；跨服务部署时需要统一 Redis 实例和数据库编号。

## 2026-08-26：直播课删除口径

Decision:

- 后台删除直播课只允许 `status=0` 的未开始课程；状态1、2、3均拒绝删除。
- 使用 `UPDATE live_course SET del_flag=2 WHERE course_id=? AND status=0 AND del_flag=1` 原子判断并逻辑删除，不能采用先查状态再无条件更新。
- 主表删除成功后物理删除对应的 `ai4_class_live_course` 关联。
- 更新未命中时，再查询当前有效课程区分“课程不存在”和“课程状态不允许删除”。

Reason:

- 本项目状态0才是未开始，状态1表示正在上课；将状态1作为删除条件会破坏进行中的课堂。
- 条件更新可以处理课堂状态同步和后台删除同时发生的竞争，避免已开始课程被误删。

## 2026-08-26：学员账户列表身份与班级归属

Decision:

- `/accounts` 列表沿用旧项目的身份定义：存在有效班级学员关系时为“正式”，否则为“普通”。
- 身份是查询时动态计算的派生字段，不在 `el_user` 中新增身份列。
- 有效归属以 `ai4_class_student.state=1` 为准，并关联有效班级和有效课包返回名称。
- 同一学员存在多条有效班级关系时，按关联记录 `updated_at DESC、id DESC` 取最近一条，避免分页结果重复。
- 账户列表使用专用查询投影，只返回学员页面实际需要的字段，不返回完整 `Account` 模型。

Reason:

- 用户身份本质由当前班级和课包归属决定，存储独立身份列会产生状态不同步问题。
- 一个学员可能保留多条历史或异常重复关系，直接连接会放大结果集并破坏分页总数与记录数量的一致性。

## 2026-08-26：新增学员账户请求精简

Decision:

- `POST /accounts` 请求只接收必填的 `phone` 和 `password`。
- 手机号同时写入 `el_user.account` 和 `el_user.phone`，并通过现有账号唯一约束防止重复创建。
- `user_id` 由后端 `SnowflakeIdGenerator` 生成，不再由请求方提供。
- 当前 `el_user.id` 没有自增和默认值；新增账户使用同一个雪花 ID 写入 `id` 和 `user_id`。
- 昵称、状态、来源、删除标识和注册时间由后端补默认值，其余可空账户字段初始保存为 `null`。

Reason:

- 当前后台创建学员只需要建立可登录的最小账户，设备、渠道和外部平台字段均不属于创建页面输入。
- 用户 ID 属于系统内部标识，由后端统一生成比依赖前端或外部调用方更稳定。

## 2026-08-27：学员详情取最近有效直播课

Decision:

- 学员详情接口为 `GET /accounts/{userId}`，只使用学员 ID 查询，不接收前端回传的手机号和班级名称。
- 班级归属沿用列表规则：从 `state=1` 的有效关系中按 `updated_at DESC、id DESC` 取最近一条。
- 课程考虑 `status IN (0,1,2,3)` 且未删除的直播课，按 `course_time DESC、course_id DESC` 取最新一节，包含未开始课程。
- 课程进度直接返回直播课关联的项目名称；是否出勤以 `be_duty=1` 或存在 `join_time` 为准。
- 状态0表示课程未开始，此时不能判断出勤，`attended` 返回 `null`；状态1、2、3才返回布尔值。
- 普通学员和没有已开始课程的学员仍返回成功响应，缺失的班级或课程字段使用 `null`。

Reason:

- 手机号和班级属于数据库事实，使用前端传值会产生过期或伪造数据。
- 一个班级存在多节课，但该页面只展示当前最近进度，不需要返回完整课程历史。

## 2026-08-28：Coze题前引导采用同步生成并由人工确认

Decision:

- 使用 Coze OAuth JWT 服务端身份接入，不保存固定 Access Token，由 SDK 在过期前重新获取。
- OAuth JSON 作为服务器外部文件管理，代码只保存文件路径和非敏感工作流 ID。
- 题前引导使用独立接口 `POST /questions/before-guide/generate`，不混入题目新增或修改事务。
- 当前工作流只有一个大模型节点且输出约50字，第一版采用同步调用，不增加任务表、轮询接口和异步状态。
- 生成接口不自动保存，也不覆盖原值；管理员确认或修改后，再通过题目新增或修改接口持久化。
- 工作流参数中的项目介绍和属性名称由后端根据稳定 ID 查询，前端无需提交容易过期的名称。
- `coze-api 0.4.2` 要求 JJWT 0.11.5，项目登录 JWT 同步改用0.11.5等价 API，避免运行时二进制冲突。

Reason:

- AI生成结果需要人工校对，直接写库会把失败、空文本或不合适的内容覆盖到正式配置。
- 当前生成链路短，同步方式更符合点击生成后立即回填的交互，也避免无必要的异步基础设施。
- 后端查询项目和属性事实可以保证工作流上下文与当前数据库一致。

## 2026-08-28：题前引导和选项引导使用组合接口

Decision:

- 对前端提供 `POST /questions/guides/generate` 组合接口，现有题前引导接口保留兼容。
- 题前工作流调用1次；选项引导工作流按选项分别调用，每次 `user_answer` 只包含当前选项。
- 一次请求中的工作流并行执行，返回顺序严格对应请求选项顺序，使用从0开始的 `optionIndex` 回填。
- 任一工作流失败则整个接口失败，不设计部分成功响应；所有生成结果均不自动保存。

Reason:

- 页面只有一个生成按钮，组合接口可以避免前端组织1加N次请求、维护下标映射和处理部分失败。
- 两个工作流没有输出到输入的依赖，适合并行执行；后端统一编排也更容易保证参数结构一致。

## 2026-09-04：Skill 与合伙人名称允许重复

Decision:

- Skill 名称和合伙人名称允许重复，覆盖 2026-07-30 的名称唯一决定。
- 暂停 Service 中新增、修改时的名称存在性校验和重复键异常转换，原实现以注释形式保留。
- `existsByName` Repository 方法和名称重复错误码暂时保留，便于需求恢复时重新启用。
- 同一合伙人请求中的 `skillIds` 仍不允许重复，这属于关联数据完整性校验，与名称重复无关。
- 数据库需要删除 `uk_ai4_skill_active_name` 和 `uk_ai4_partner_active_name` 两个唯一索引；生成列可以保留。

Reason:

- 不同用户可能创建名称相同的 Skill 或合伙人，名称不能继续作为全局唯一标识。
- 当前表中没有创建者字段，因此现阶段的实际语义是全局允许重名，而不是按用户范围唯一。

## 2026-09-18：公司名称修改、独立直播课时间和合伙人删除规则

Decision:

- AI 生成的公司名称继续要求最多 7 个字符并以“公司”或“工作室”结尾；人工修改候选名称时只校验非空和最多 7 个字符，保留候选名称去重。
- 后台手动创建直播课新增 `courseStartTime`，与 `courseDate` 按 `Asia/Shanghai` 组合生成 `live_course.course_time`；不再使用班级 `start_time`，CRM 自动排课逻辑不变。
- 合伙人使用 `ai4_partner.del_flag` 表示合伙人生命周期；项目任务关联表中是否存在记录表示当前绑定，不增加额外状态字段。
- 删除合伙人时只检查 `ai4_project_task_partner` 是否仍有当前项目绑定；存在绑定时返回 `PARTNER_IN_USE`。没有当前项目绑定时允许归档，即使存在圆桌记录，也不删除 Skill 关联、圆桌记录或提交文件。

Reason:

- AI 生成规则和人工编辑规则是两个不同业务场景，不能继续复用同一套后缀校验。
- `live_course` 已有独立的计划时间字段，手动建课不应被班级默认开课时间限制。
- 项目绑定是当前配置关系，必须避免归档后留下可选配置；圆桌记录是历史业务数据，归档后仍需支持学生继续使用原合伙人并保持可追溯性。

## 2026-09-21：开放 CRM 新建学员入口

Decision:

- 新增独立 CRM 接口 `POST /external/aiUser/add`，不直接放开后台接口 `POST /accounts`。
- CRM 接口沿用现有 `AccountService` 和客户端服务内部用户接口，密码仍由 `ai4-server` 按客户端规则保存。
- CRM 接口不要求后台 JWT，统一由 `/external/**` 的 CRM IP 白名单拦截器保护。
- 创建学员和绑定班级保持两个独立步骤；创建接口只写入 `el_user`，不写入 `ai4_class_student`。
- 手机号重复时不覆盖原密码，沿用客户端用户服务的重复账户错误。

Reason:

- 后台管理接口和 CRM 同步接口的认证边界不同，单独入口可以避免把后台接口契约暴露给 CRM。
- 复用已有 Account 业务可以保持账户创建、密码处理和错误映射一致，避免出现两套创建逻辑。

## 2026-10-09：沙盘属性目标值改为可选

Decision:

- `SandtableAttributeRequest.target` 移除非空校验，新增和修改沙盘属性时允许省略或传 `null`。
- `SandtableAttribute`、响应 DTO 和 JDBC 写入逻辑继续使用 `BigDecimal`，不新增字段或接口。
- 属性差异比较对 `target` 使用空值安全比较；`initial` 仍保持必填。
- 由于当前不启用 Flyway，数据库中的 `ai4_project_sandtable_attribute.target` 由各环境手动调整为可空。

Reason:

- 目标值是可选业务数据，Java 请求校验与数据库列约束必须保持一致。
- 修改已有属性时，空目标值不能触发 `BigDecimal.compareTo` 空指针。

## 2026-09-21：新增学员可选昵称

Decision:

- 普通新增学员接口 `POST /accounts` 和 CRM 新增学员接口 `POST /external/aiUser/add` 共用可选字段 `nick`。
- `nick` 最大长度为255个字符，不作为必填字段；后台将其透传给 `ai-server`。
- 未传或传空昵称时，继续由 `ai-server` 按既有规则随机生成昵称。

Reason:

- `el_user` 已有昵称字段，客户端服务内部创建接口也已经支持昵称参数；后台只需透传，避免重复实现用户昵称生成逻辑。

# 2026-09-05：客户端用户密码写入统一归属 ai4-server

- `el_user` 是客户端登录账户，密码格式和登录校验统一由 `ai4-server` 负责。
- 管理后台保留 `/accounts` 对前端的接口契约，但创建用户和重置密码改为通过 `RestClient` 调用 `ai4-server` 的内部用户接口。
- 内部调用使用 `X-Internal-Token`，不转发后台 JWT，也不把内部令牌暴露给前端。
- 管理后台不引入 SM4 密钥；`sys_user` 后台用户仍使用 BCrypt，不受本次调整影响。
- Account 列表和详情属于后台聚合查询，继续通过 JdbcTemplate 读取 `el_user`、班级和直播课相关表。
- 两个服务部署在同一台服务器时，后台默认通过 `http://127.0.0.1:8081` 调用客户端服务。

## 2026-10-09：Skill 绑定下沉到项目任务范围

Decision:

- 合伙人管理只维护 `ai4_partner` 基础信息，不再在新增、修改合伙人时绑定 Skill。
- 新增 `ai4_project_task_partner_skill(task_id, partner_id, skill_id, sort_order)` 保存项目任务实际使用的 Skill。
- `ai4_project_task_partner` 继续保存任务选择的合伙人；项目任务配置请求改为按合伙人提交 `skillIds`。
- 同一个合伙人可以在不同项目任务中使用不同 Skill；同一个任务内同一合伙人的 Skill 不允许重复。
- Skill 列表的 `partnerCount`、Skill 删除保护改为读取项目任务 Skill 关联表。
- 旧的 `ai4_partner_skill_relation` 暂不删除，代码不再读写，待历史数据处理和部署验证完成后再清理。

Reason:

- 全局 `partner_id + skill_id` 关系无法区分同一合伙人在不同项目任务中的 Skill 选择。
- 项目任务关联表增加任务维度后，才能表达“项目 A 使用 Skill1、项目 B 使用 Skill2”。
