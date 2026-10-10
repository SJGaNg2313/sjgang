package com.ai4.dict.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.dict.dto.CreateDictTypeRequest;
import com.ai4.dict.dto.UpdateDictTypeRequest;
import com.ai4.dict.model.DictType;
import com.ai4.dict.service.DictTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典类型管理接口。
 */
@RestController
@RequestMapping("/dict-types")
public class DictTypeController {

    private final DictTypeService dictTypeService;

    public DictTypeController(DictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    /**
     * 根据可选条件查询字典类型。
     */
    @GetMapping
    public ApiResponse<List<DictType>> list(
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictCode,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(
                dictTypeService.getAll(dictName, dictCode, status)
        );
    }

    /**
     * 根据 ID 查询字典类型详情。
     */
    @GetMapping("/{typeId}")
    public ApiResponse<DictType> get(@PathVariable String typeId) {
        return ApiResponse.success(dictTypeService.get(typeId));
    }

    /**
     * 创建字典类型。
     */
    @PostMapping
    public ApiResponse<DictType> create(
            @Valid @RequestBody CreateDictTypeRequest request
    ) {
        return ApiResponse.success(dictTypeService.create(request));
    }

    /**
     * 修改字典类型名称和备注。
     */
    @PutMapping("/{typeId}")
    public ApiResponse<DictType> update(
            @PathVariable String typeId,
            @Valid @RequestBody UpdateDictTypeRequest request
    ) {
        return ApiResponse.success(dictTypeService.update(typeId, request));
    }
}
