# AGENTS.md

## 接口文档约定

- 当前项目唯一接口文档统一维护在 `D:\Obsidian\Obsidian Vault\API_DOCUMENTATION.md`。
- 新增、删除或修改接口路径、请求字段、响应字段、认证方式和错误码时，必须同步更新该文件。
- 不要在仓库其他位置重复创建同类接口文档；需要拆分专题时，应先在主接口文档中增加索引和链接。
- ApiPost 中只关注 `frozen的团队 / AI4.0后台` 项目，分支项目 ID 为 `67b328d3446d000`；查询、核对或同步接口时，不使用其他 ApiPost 项目。

## 工作协议

- 开始较长任务或跨会话继续任务时，先读取上一级 `D:\Dev\Projects\ai4-boot-verify\docs` 中的 `PROJECT_CONTEXT.md`、`DECISIONS.md`、`WORK_LOG.md` 相关内容。
- 不要把聊天上下文当作项目事实来源；代码事实必须通过仓库文件、测试或命令输出确认。
- 不确定时明确说明不确定，并优先检查代码或文档，不要补全不存在的信息。
- 修改代码前，先说明准备修改哪些文件和原因。
- 重要架构选择、接口约定、异常处理策略、测试策略变更，要同步记录到上一级 `docs/DECISIONS.md`。
- 多步骤任务结束或暂停前，要同步更新上一级 `docs/WORK_LOG.md`，记录当前目标、已完成事项、未完成事项、相关文件、验证结果和下一步。
- 如果发现文档与代码冲突，以代码和测试为准，并更新文档或提醒用户确认。

## 当前项目约定

- 项目是 Maven 管理的 Spring Boot 应用。
- 主要语言版本是 Java 17。
- 业务包当前位于 `src/main/java/com/ai4`。
- 测试位于 `src/test/java`，当前使用 Spring Boot Test 和 MockMvc。
- 统一响应结构使用 `com.ai4.common.response.ApiResponse`。
- 全局异常处理位于 `com.ai4.common.exception.GlobalExceptionHandler`。
