package com.ai4.skill.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.common.response.PageResponse;
import com.ai4.skill.dto.CreateSkillRequest;
import com.ai4.skill.dto.SkillDetailResponse;
import com.ai4.skill.dto.SkillListItemResponse;
import com.ai4.skill.dto.SkillPageQuery;
import com.ai4.skill.dto.SkillOptionResponse;
import com.ai4.skill.dto.UpdateSkillRequest;
import com.ai4.skill.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** Skill 管理接口 */
@RestController
@RequestMapping("/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /** 分页查询未删除的 Skill，只返回名称和内容等列表字段。 */
    @GetMapping
    public ApiResponse<PageResponse<SkillListItemResponse>> list(
            @Valid @ModelAttribute SkillPageQuery query
    ) {
        return ApiResponse.success(skillService.list(query));
    }

    /** 查询全部未删除 Skill 的轻量选项。 */
    @GetMapping("/options")
    public ApiResponse<List<SkillOptionResponse>> options() {
        return ApiResponse.success(skillService.options());
    }

    /** 根据 ID 查询 Skill 完整详情，用于修改表单回填 */
    @GetMapping("/{skillId}")
    public ApiResponse<SkillDetailResponse> get(@PathVariable String skillId) {
        return ApiResponse.success(skillService.get(skillId));
    }

    /** 创建 Skill，除图标外的业务字段均为必填 */
    @PostMapping
    public ApiResponse<SkillDetailResponse> create(
            @Valid @RequestBody CreateSkillRequest request
    ) {
        return ApiResponse.success(skillService.create(request));
    }

    /** 修改 Skill 的全部业务字段 */
    @PutMapping("/{skillId}")
    public ApiResponse<SkillDetailResponse> update(
            @PathVariable String skillId,
            @Valid @RequestBody UpdateSkillRequest request
    ) {
        return ApiResponse.success(skillService.update(skillId, request));
    }

    /** 逻辑删除指定 Skill */
    @DeleteMapping("/{skillId}")
    public ApiResponse<Void> delete(@PathVariable String skillId) {
        skillService.delete(skillId);
        return ApiResponse.success(null);
    }
}
