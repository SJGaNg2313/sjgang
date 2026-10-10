package com.ai4.dict.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.dict.dto.CreateDictItemRequest;
import com.ai4.dict.dto.DictItemResponse;
import com.ai4.dict.dto.UpdateDictItemRequest;
import com.ai4.dict.service.DictItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 字典项管理接口。 */
@RestController
@RequestMapping
public class DictItemController {

    private final DictItemService dictItemService;

    public DictItemController(DictItemService dictItemService) {
        this.dictItemService = dictItemService;
    }

    /** 查询指定字典类型下的字典项。 */
    @GetMapping("/dict-types/{typeId}/items")
    public ApiResponse<List<DictItemResponse>> list(
            @PathVariable String typeId,
            @RequestParam(required = false) String itemLabel,
            @RequestParam(required = false) String itemValue,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(dictItemService.list(typeId, itemLabel, itemValue, status));
    }

    /** 查询字典项详情。 */
    @GetMapping("/dict-items/{itemId}")
    public ApiResponse<DictItemResponse> get(@PathVariable String itemId) {
        return ApiResponse.success(dictItemService.get(itemId));
    }

    /** 在指定字典类型下创建字典项。 */
    @PostMapping("/dict-types/{typeId}/items")
    public ApiResponse<DictItemResponse> create(
            @PathVariable String typeId,
            @Valid @RequestBody CreateDictItemRequest request
    ) {
        return ApiResponse.success(dictItemService.create(typeId, request));
    }

    /** 完整修改字典项，所属字典类型不可修改。 */
    @PutMapping("/dict-items/{itemId}")
    public ApiResponse<DictItemResponse> update(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateDictItemRequest request
    ) {
        return ApiResponse.success(dictItemService.update(itemId, request));
    }

    /** 删除字典项。 */
    @DeleteMapping("/dict-items/{itemId}")
    public ApiResponse<Void> delete(@PathVariable String itemId) {
        dictItemService.delete(itemId);
        return ApiResponse.success(null);
    }
}
