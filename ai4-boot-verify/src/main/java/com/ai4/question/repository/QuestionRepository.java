package com.ai4.question.repository;

import com.ai4.question.dto.CreateQuestionRequest;
import com.ai4.question.dto.ProjectQuestionListItemResponse;
import com.ai4.question.dto.QuestionAttributeResponse;
import com.ai4.question.dto.QuestionOptionRequest;
import com.ai4.question.dto.QuestionOptionResponse;
import com.ai4.question.dto.QuestionPageQuery;
import com.ai4.question.dto.QuestionProjectAttributeResponse;
import com.ai4.question.dto.UpdateQuestionRequest;
import com.ai4.question.model.ProjectQuestion;

import java.util.List;
import java.util.Optional;

/** 项目题目管理的数据访问边界。 */
public interface QuestionRepository {

    /** 查询项目名称和每个项目沙盘下的属性名称，用于题目管理筛选和表单选择。 */
    List<QuestionProjectAttributeResponse> findProjectAttributes();

    /** 查询指定项目沙盘下的属性，用于组织题前引导工作流参数。 */
    List<QuestionAttributeResponse> findAttributesByProjectId(Long projectId);

    /** 统计指定项目下未删除题目数量，支持题目文本和备注筛选。 */
    long countPage(QuestionPageQuery query);

    /** 分页查询指定项目下未删除题目列表，列表固定每页10条。 */
    List<ProjectQuestionListItemResponse> findPage(QuestionPageQuery query);

    /** 根据ID查询未删除题目主表。 */
    Optional<ProjectQuestion> findById(Long questionId);

    /** 查询题目下的选项和属性影响。 */
    List<QuestionOptionResponse> findOptionsByQuestionId(Long questionId);

    /** 统计项目下启用且未删除的题目数量。 */
    long countEnabled(Long projectId);

    /** 保存题目主表并返回生成ID。 */
    Long save(CreateQuestionRequest request);

    /** 修改题目主表，项目ID不允许修改。 */
    int update(Long questionId, UpdateQuestionRequest request);

    /** 删除题目下的选项及属性影响。 */
    void deleteOptions(Long questionId);

    /** 新增题目选项和该选项下的属性影响。 */
    void insertOption(Long questionId, QuestionOptionRequest option);

    /** 逻辑删除题目主表。 */
    int softDelete(Long questionId);
}
