package com.ai4.skill.repository;

import com.ai4.skill.dto.SkillListItemResponse;
import com.ai4.skill.dto.SkillOptionResponse;
import com.ai4.skill.model.Skill;

import java.util.List;
import java.util.Optional;

/** Skill 数据访问接口 */
public interface SkillRepository {

    /** 统计全部未删除的 Skill */
    long count();

    /** 分页查询未删除的 Skill 列表 */
    List<SkillListItemResponse> findPage(int offset, int limit);

    List<SkillOptionResponse> findOptions();

    boolean existsByName(String skillName, String excludeSkillId);

    /** 根据 ID 查询未删除的 Skill 完整数据 */
    Optional<Skill> findById(String skillId);

    /** 判断 Skill 是否存在项目任务绑定关系 */
    boolean hasTaskRelations(String skillId);

    /** 新增 Skill */
    int save(Skill skill);

    /** 修改未删除的 Skill*/
    int update(Skill skill);

    /** 逻辑删除 Skill*/
    int softDelete(String skillId);
}
