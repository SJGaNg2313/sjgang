# AI4.0 后端服务器进度同步

## 2026-07-03 更新：systemd 后台运行与 Nginx 公网访问已完成

当前后端部署已从前台验证推进到后台服务运行。

已完成：

- 后端已配置为 systemd 服务：
  - 服务名：`ai4-backend`
  - jar 路径：`/opt/ai4/backend/app.jar`
  - 工作目录：`/opt/ai4/backend`
- 后端运行所需数据库配置已写入 systemd 环境变量：
  - `AI4_DB_URL`
  - `AI4_DB_USERNAME`
  - `AI4_DB_PASSWORD`
- 后端日志目录为：
  - `/opt/ai4/logs`
- Nginx 已配置公网反向代理：
  - 外部访问 80 端口
  - 内部转发到 `127.0.0.1:8080`
  - 接口统一前缀保持 `/ai/v4`
- 阿里云安全组已开放 80 端口。
- 本地 PowerShell 和 ApiPost 已验证公网接口可访问。

当前公网访问地址：

```text
http://39.106.177.254/ai/v4/verify/database
http://39.106.177.254/ai/v4/users
http://39.106.177.254/ai/v4/auth/login
```

当前结论：

- 后端已具备后台运行能力。
- Nginx 已作为公网入口。
- Spring Boot 的 8080 端口不需要直接对公网开放。
- MySQL 的 3306 端口不需要直接对公网开放。

当前未完成：

- 域名绑定
- HTTPS
- 正式 JWT
- BCrypt 密码加密
- 临时验证接口清理或访问限制

更新时间：2026-06-30
负责人：后端开发
服务器：阿里云 ECS
公网 IP：39.106.177.254

## 1. 当前服务器状态

已购买并初始化一台阿里云 ECS 服务器，用于后续部署 AI4.0 后端服务。

当前已完成：

- 已通过阿里云 Workbench 成功登录服务器。
- 已确认服务器系统为 Ubuntu。
- 已完成系统软件包更新。
- 已重启服务器，更新已正常生效。
- 已配置本地 Windows PowerShell 通过 SSH 密钥登录服务器。
- 已创建普通用户 `ai4_dev`，用于后续非 root 登录和协作。
- 已开启 SSH 密码登录能力，用于普通用户密码登录。

## 2. SSH 登录与密钥配置进度

最初服务器创建时选择了阿里云已有密钥对，但本地没有找到对应 `.pem` 私钥文件，因此本地直接执行：

```powershell
ssh root@39.106.177.254
```

会返回：

```text
Permission denied (publickey)
```

后续处理方式：

- 在本地 Windows 重新生成了 `ed25519` SSH 密钥。
- 将本地公钥写入服务器 `/root/.ssh/authorized_keys`。
- 修正了一次文件名写错问题：误写入 `/root/.ssh/authorized.pub`，后已合并到正确文件 `/root/.ssh/authorized_keys`。
- 当前本地已可以通过 SSH 私钥登录 root 用户。

当前 root 密钥登录命令：

```powershell
ssh -i C:\Users\PC\.ssh\id_ed25519 root@39.106.177.254
```

后续建议：

- root 继续保留密钥登录。
- 不建议长期开放 root 密码登录。
- 后续应优先使用普通用户 `ai4_dev` 做日常部署操作。

## 3. 普通用户配置

已创建普通用户：

```text
ai4_dev
```

用途：

- 用于后端日常部署、维护、协作登录。
- 避免所有操作都直接使用 root，降低误操作风险。

当前已处理：

- 已创建用户 `ai4_dev`。
- 已设置用户密码。
- 已调整 SSH 配置，允许密码登录。

普通用户登录方式：

```powershell
ssh ai4_dev@39.106.177.254
```

如需让 `ai4_dev` 安装软件或执行管理员操作，需要 root 执行：

```bash
usermod -aG sudo ai4_dev
```

添加 sudo 后，需要退出并重新登录 `ai4_dev`，再使用：

```bash
sudo apt install -y xxx
```

## 4. 系统更新进度

已执行 Ubuntu 系统更新：

```bash
apt update && apt upgrade -y
```

更新结果：

- 系统软件包更新成功。
- 服务已自动重启。
- 内核提示为最新状态。
- 服务器已手动重启验证。

后续建议安装常用工具：

```bash
apt install -y curl wget git vim unzip lsof net-tools
```

用途：

- `curl` / `wget`：接口测试、下载文件。
- `git`：拉取后端代码。
- `vim` / `nano`：编辑服务器配置。
- `unzip`：解压部署包。
- `lsof` / `net-tools`：排查端口和网络问题。

## 5. Java 运行环境规划

后端项目情况：

- 项目名称：AI4.0 / `ai4-boot-verify`
- 技术栈：Spring Boot / Maven
- 项目运行 JDK：JDK 21
- Java 语言级别：17

服务器建议安装：

```bash
apt install -y openjdk-21-jdk
```

原因：

- 项目运行环境按 JDK 21 准备。
- 项目代码语言级别保持 Java 17。
- JDK 21 可以运行和构建 Java 17 目标代码。
- 反过来，如果项目依赖 JDK 21，服务器只安装 JDK 17 可能无法运行。

安装完成后验证：

```bash
java -version
javac -version
```

如果使用 `ai4_dev` 安装，需要 sudo 权限：

```bash
sudo apt install -y openjdk-21-jdk
```

## 6. 当前后端项目进度

本地项目路径：

```text
D:\Dev\Projects\ai4-boot-verify\ai4-boot-verify
```

当前项目仍处于后端基础骨架阶段，尚未完成正式业务接口，也尚未部署到服务器。

已具备的后端基础能力：

- Spring Boot 项目骨架。
- Maven 构建配置。
- 统一响应结构 `ApiResponse`。
- 参数校验能力。
- 全局异常处理。
- 基础验证接口。

当前已有测试/验证接口：

```text
POST /verify/validation
POST /users
```

说明：

- 这些接口目前主要用于验证统一响应、参数校验和异常处理链路。
- 还不是正式业务接口。
- 当前不能作为前端正式联调依据。

## 7. 前端当前可提供信息

由于后端还没有部署，当前不能提供正式可调用的代理地址。

目前可以提供给前端的信息：

```text
公网 IP：39.106.177.254
域名：暂未配置
后端代理地址：暂未提供，后端服务尚未部署
```

如果前端只是需要提前预留配置，可以暂定：

```text
开发/测试环境后端地址：http://39.106.177.254:8080
```

但需要明确说明：

```text
该地址目前还不可用，等待后端服务部署并开放端口后再正式联调。
```

## 8. 接口前缀建议

项目名是 AI4.0，接口前缀需要和后端项目统一。

讨论过的候选方案：

```text
/ai/v4
/ai/v4
/api/v1
```

建议优先使用：

```text
/ai/v4
```

原因：

- `AI4.0` 更像产品/项目名称，而不是 API 第 4 版。
- `/ai/v4` 容易被理解为“AI API 的第 4 个版本”。
- 当前项目已最终确定使用 `/ai/v4` 作为统一接口前缀。

示例接口：

```text
POST /ai/v4/users
POST /ai/v4/auth/login
GET  /ai/v4/health
```

前端代理未来可配置为：

```text
/ai/v4 -> http://39.106.177.254:8080
```

## 9. 部署前还需要完成的事项

后端部署前需要确认：

- 服务器安装 JDK 21。
- 服务器安装 Git 或准备 jar 包上传方式。
- 确认项目端口，默认建议先使用 `8080`。
- 确认接口统一前缀为 `/ai/v4`。
- 确认阿里云安全组是否开放后端访问端口。
- 后续建议使用 Nginx 代理，不直接长期暴露 Spring Boot 的 `8080` 端口。

推荐部署阶段顺序：

1. 安装 JDK 21。
2. 安装 Git、curl、lsof 等基础工具。
3. 将后端代码或 jar 包放到服务器。
4. 使用 `java -jar` 验证项目能启动。
5. 通过 `curl localhost:8080` 验证本机访问。
6. 配置阿里云安全组或 Nginx。
7. 给前端提供正式代理地址。
8. 使用 `systemd` 管理后端服务，实现后台运行和开机自启。

## 10. 后续正式给前端的信息模板

后端部署完成后，可以按以下格式同步前端：

```text
后端环境：测试环境
公网 IP：39.106.177.254
域名：暂未配置
接口地址：http://39.106.177.254:8080
接口前缀：/ai/v4
前端代理规则：/ai/v4 -> http://39.106.177.254:8080
说明：当前为测试环境地址，后续配置域名和 HTTPS 后会切换为正式地址。
```

如果后续配置 Nginx，则可以改为：

```text
接口地址：http://39.106.177.254
接口前缀：/ai/v4
前端代理规则：/ai/v4 -> http://39.106.177.254
```

如果后续配置域名和 HTTPS，则最终形式建议为：

```text
接口地址：https://api.example.com
接口前缀：/ai/v4
```

## 11. 当前结论

当前服务器初始化工作已完成一部分，已经解决了登录、密钥、普通用户和系统更新问题。

但后端服务还没有部署，因此现在只能给前端公网 IP 和接口规划，不能给正式可调用的代理地址。

下一步后端重点是：

- 给 `ai4_dev` 确认 sudo 权限。
- 安装 JDK 21。
- 确认接口统一前缀。
- 开始实现正式业务接口。
- 部署第一个 Spring Boot 服务到服务器。
- 服务跑通后再向前端提供正式代理地址。
## 0. 当前接口前缀结论

后端统一接口前缀已确定为：

```text
/ai/v4
```

本地 Spring Boot 项目通过以下配置统一添加接口前缀：

```properties
server.servlet.context-path=/ai/v4
```

Controller 中只写业务路径，例如：

```text
/users
/verify/validation
```

因此真实访问路径为：

```text
POST /ai/v4/users
POST /ai/v4/verify/validation
```

后续服务器部署、前端代理、接口文档和测试环境说明均以 `/ai/v4` 为准。

## 0.1 当前本地后端接口进度

截至 2026-06-30，本地后端已完成用户创建接口的最小验证闭环。

当前已有真实访问路径：

```text
POST /ai/v4/verify/validation
POST /ai/v4/users
```

`POST /ai/v4/users` 当前行为：

- 请求对象：`CreateUserRequest`
- 请求字段：`username`、`email`、`password`
- 响应对象：`CreateUserResponse`
- 响应字段：`username`、`email`
- 响应中不返回 `password`
- 统一响应外层：`ApiResponse<CreateUserResponse>`

本地自动化测试结果：

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

说明：

- 当前用户接口仍是最小验证版本，尚未接数据库、Service、密码加密和真实注册逻辑。
- 该接口可以作为后续 Service 层、数据库层和部署验证的基础。

## 2026-07-17 当前服务器与部署状态

- 后端此前已经部署并可以正常启动，旧的“后端服务还没有部署”结论已经过时。
- 服务器数据库已使用 `sys_user` 作为后台登录用户表，`ai4_user` 作为服务账户表。
- 当前本地最新代码新增角色、授权和接口权限校验，尚未确认已重新部署到服务器。
- 最新 Flyway 版本为 V7，部署启动后会初始化系统管理页面和 14 个基础权限，并授权给启用的 `super_admin`。
- V7 不会自动把某个 `sys_user` 绑定为超级管理员；部署前必须确认服务器初始管理员在 `sys_user_role` 中关联 `super_admin`。
- 部署后应验证：
  - `flyway_schema_history` 中 V7 的 `success = 1`；
  - 超级管理员 `/auth/perms` 返回 14 个 `system:*` 权限；
  - 超级管理员访问 `GET /roles` 成功；
  - 无角色用户访问 `GET /roles` 返回 HTTP 403。
- 当前部署前阻塞：助手环境的完整测试仍因 `ai4_test@localhost` 凭据错误未完成；Java 编译和人工权限测试已通过。

## 2026-07-21 数据库迁移与待部署状态

- 服务器后台账号已经绑定 `super_admin`，现有 RBAC 部署前置条件满足。
- 本地后续新增了 V8-V10 相关迁移；不能再以旧文档中的“最高 V7”为准。
- V8 曾发生 checksum mismatch。部署前必须比较服务器 `flyway_schema_history` 与发布包中的 V8-V10，禁止直接上传后依赖 Flyway 自动试错。
- 当前本地 V8 已修正为只创建题库相关表，V9 单独创建合伙人相关表；原有重复建表和主键引号风险已消除。部署前仍需确认服务器迁移 checksum 与发布包一致。
- 字典模块仅完成设计，尚未创建 V11，因此当前部署包不包含字典表。
- 下一次部署前必须完成：确认并固化 V8-V10、执行完整测试、备份服务器数据库、核对迁移历史，然后再替换 JAR 并启动。

## 2026-07-27 题目管理待部署状态

- V14 扩展题目结构：答题前引导词、答题后引导词、选项影响以及 A-D 约束。
- 已新增四个受权限保护的题目接口：
  - `GET /questions`
  - `GET /questions/{questionId}`
  - `PUT /questions/{questionId}`
  - `DELETE /questions/{questionId}`
- V15 新增题目管理菜单和 `question:list`、`question:update`、`question:delete`，并授权给启用中的 `super_admin`。
- Java 与测试源码已通过 `mvn test-compile`；尚未记录本轮完整 `mvn test` 结果。
- V16 删除 `ai4_question_choice.correct`；题目选项只维护内容和选择后的影响，不再区分正确或错误。
- 部署后必须确认 V14、V15、V16 均为 `success = 1`，重新登录并验证 `/auth/perms` 返回三个 `question:*` 权限。
- 页面路径 `/questions` 和组件 `question/index` 为暂定值，前端确定实现后可通过菜单接口修改。

## 2026-08-05 任务图片上传状态

- 新增 `POST /projects/{projectId}/task/images/upload`，一次上传发布任务图、领取任务图和公司名片。
- 三个文件参数及响应字段统一为 `publicTask`、`claimTask`、`companyCard`，全部必填。
- 上传前校验 `projectId` 对应有效项目，OSS 路径按项目 ID 隔离。
- 任务创建和修改请求中的三个图片 URL 已改为必填；前端应先上传，再提交任务数据。
- 服务器部署时必须配置 OSS 相关环境变量，否则调用上传接口会失败。
- 当前 multipart 上限为单文件 20MB、单请求 60MB；任务图片没有额外格式、MIME 和尺寸限制。

## 2026-08-24 教师后台账号与角色同步

- `live_teach` 必须存在 `user_id BIGINT` 字段，用于关联 `sys_user.user_id`。
- CRM 新增教师请求新增必填数组 `roleIds`，例如 `"roleIds": [2]`，不能传字符串 `"2"`。
- 新增教师会同时写入 `sys_user`、`live_teach`、`sys_user_role`；任一步骤失败会整体回滚。
- 新增 `GET /teachers`，支持按 `userName`、`nickName` 模糊查询有效教师。
- CRM 修改教师和后台重置密码会同步维护 `sys_user`、`live_teach` 两张表的账号密码。
- 部署后应验证新增教师能够后台登录、角色关系正确、教师列表可查询，并验证两条密码修改链路。

## 2026-08-25 直播课详情

- 新增 `GET /live-courses/{courseId}?pageNum=1&pageSize=10`。
- 返回基本信息、实际时长、出勤/缺勤/迟到/早退人数、录像列表和分页学员明细。
- 部署数据库必须存在 `live_course_video`，且至少包含 `course_id`、`m3u8` 字段。
- 详情依赖 `live_user_course_record` 已正确维护 `be_duty`、`lack`、`be_late`、`leave_early`、`join_time` 和 `stream_uuid`。
- 部署后应分别使用未开课、进行中、已结束课程验证实际时长，并使用缺勤和迟到学员验证分页结果。

## 2026-08-28 Coze题前引导工作流部署配置

- 新增 `POST /questions/before-guide/generate`，采用同步方式生成题前引导，不自动保存数据库。
- 本地已统一 JJWT 版本为0.11.5；服务器必须部署本次重新打包的完整 JAR，不能只复用旧依赖或旧进程。
- 服务器需保存外部 OAuth 配置文件 `/opt/ai4/backend/config/coze/coze_oauth_config.json`，目录权限建议700，文件权限建议600，所有者为 `ai4_dev`。
- systemd 需配置 `COZE_ENABLED=true`、`COZE_BASE_URL=https://api.coze.cn`、`COZE_OAUTH_CONFIG_PATH`、`COZE_TIMEOUT_SECONDS=90`、`COZE_QUESTION_BEFORE_GUIDE_WORKFLOW_ID=7678625341916659718`。
- 配置后执行 `systemctl daemon-reload` 并重启服务；服务器时间必须同步，且需要能够访问 `api.coze.cn:443`。

## 2026-08-28 Coze题目引导组合接口部署配置

- 新增 `POST /questions/guides/generate`，一次生成题前引导和全部选项引导，不自动保存数据库。
- systemd 需要新增 `COZE_QUESTION_AFTER_GUIDE_WORKFLOW_ID=7678645700088021046`。
- 原有 `COZE_QUESTION_BEFORE_GUIDE_WORKFLOW_ID=7678625341916659718` 和 OAuth JWT 配置继续保留。
- 部署后先使用2个选项验证返回2条 `optionGuides`，再使用4个选项验证并行调用和返回顺序。
