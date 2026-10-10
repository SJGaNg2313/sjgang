package com.ai4.skill.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.response.PageResponse;
import com.ai4.skill.dto.CreateSkillRequest;
import com.ai4.skill.dto.SkillDetailResponse;
import com.ai4.skill.dto.SkillListItemResponse;
import com.ai4.skill.dto.SkillPageQuery;
import com.ai4.skill.dto.SkillOptionResponse;
import com.ai4.skill.dto.UpdateSkillRequest;
import com.ai4.skill.model.Skill;
import com.ai4.skill.repository.SkillRepository;
import org.springframework.stereotype.Service;
// import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Skill 的分页查询、详情、创建、修改和逻辑删除 */
@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    /** 分页查询全部未删除的 Skill */
    @Transactional(readOnly = true)
    public PageResponse<SkillListItemResponse> list(SkillPageQuery query) {
        int pageNum = query.normalizedPageNum();
        int pageSize = query.normalizedPageSize();
        return new PageResponse<>(
                skillRepository.findPage(query.offset(), pageSize),
                skillRepository.count(),
                pageNum,
                pageSize
        );
    }

    /** 根据 ID 查询 Skill 完整信息，用于修改表单回填 */
    @Transactional(readOnly = true)
    public SkillDetailResponse get(String skillId) {
        return toDetailResponse(findSkill(skillId));
    }

    /** 查询全部未删除 Skill 的轻量选项。 */
    @Transactional(readOnly = true)
    public java.util.List<SkillOptionResponse> options() {
        return skillRepository.findOptions();
    }

    /** 创建 Skill，并返回保存后的完整信息 */
    @Transactional
    public SkillDetailResponse create(CreateSkillRequest request) {
        String skillName = request.skillName().trim();
        // 当前允许不同用户创建同名 Skill；如后续恢复名称唯一，再启用此校验。
        // validateNameAvailable(skillName, null);
        String skillId = UUID.randomUUID().toString();
        Skill skill = new Skill(
                skillId,
                skillName,
                request.skillContent().trim(),
                trimToNull(request.skillIcon()),
                request.triggerConditions().trim(),
                request.inputParameters().trim(),
                request.followStep().trim(),
                request.outputFormat().trim(),
                request.constraints().trim(),
                1,
                null,
                null
        );
        if (skillRepository.save(skill) == 0) {
            throw new IllegalStateException("Failed to create Skill");
        }
//        try {
//            if (skillRepository.save(skill) == 0) {
//                throw new IllegalStateException("Failed to create Skill");
//            }
//        } catch (DuplicateKeyException exception) {
//            throw new BizException(ErrorCode.SKILL_NAME_ALREADY_EXISTS);
//        }
        return get(skillId);
    }

    /** 修改指定 Skill 的全部可编辑字段 */
    @Transactional
    public SkillDetailResponse update(String skillId, UpdateSkillRequest request) {
        Skill existing = findSkill(skillId);
        String skillName = request.skillName().trim();
        // 当前允许不同用户使用相同的 Skill 名称。
        // validateNameAvailable(skillName, skillId);
        Skill updated = new Skill(
                existing.id(),
                skillName,
                request.skillContent().trim(),
                trimToNull(request.skillIcon()),
                request.triggerConditions().trim(),
                request.inputParameters().trim(),
                request.followStep().trim(),
                request.outputFormat().trim(),
                request.constraints().trim(),
                existing.delFlag(),
                existing.createdAt(),
                existing.updatedAt()
        );
        if (skillRepository.update(updated) == 0) {
            throw new BizException(ErrorCode.SKILL_NOT_FOUND);
        }
//        try {
//            if (skillRepository.update(updated) == 0) {
//                throw new BizException(ErrorCode.SKILL_NOT_FOUND);
//            }
//        } catch (DuplicateKeyException exception) {
//            throw new BizException(ErrorCode.SKILL_NAME_ALREADY_EXISTS);
//        }
        return get(skillId);
    }

    /** 逻辑删除指定 Skill */
    @Transactional
    public void delete(String skillId) {
        findSkill(skillId);
        if (skillRepository.hasTaskRelations(skillId)) {
            throw new BizException(ErrorCode.SKILL_ASSIGNED_TO_PARTNER);
        }
        if (skillRepository.softDelete(skillId) == 0) {
            if (skillRepository.hasTaskRelations(skillId)) {
                throw new BizException(ErrorCode.SKILL_ASSIGNED_TO_PARTNER);
            }
            throw new BizException(ErrorCode.SKILL_NOT_FOUND);
        }
    }

    /** 查询有效 Skill，不存在时抛出统一业务异常 */
    private Skill findSkill(String skillId) {
        return skillRepository.findById(skillId)
                .orElseThrow(() -> new BizException(ErrorCode.SKILL_NOT_FOUND));
    }

//    private void validateNameAvailable(String skillName, String excludeSkillId) {
//        if (skillRepository.existsByName(skillName, excludeSkillId)) {
//            throw new BizException(ErrorCode.SKILL_NAME_ALREADY_EXISTS);
//        }
//    }

    /** 将数据库模型转换为对外详情响应 */
    private SkillDetailResponse toDetailResponse(Skill skill) {
        return new SkillDetailResponse(
                skill.id(),
                skill.skillName(),
                skill.skillContent(),
                skill.skillIcon(),
                skill.triggerConditions(),
                skill.inputParameters(),
                skill.followStep(),
                skill.outputFormat(),
                skill.constraints()
        );
    }

    /** 将可选文本的空值和纯空格统一转换为 null */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
