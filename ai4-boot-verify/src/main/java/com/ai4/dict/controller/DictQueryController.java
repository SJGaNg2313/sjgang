package com.ai4.dict.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.dict.dto.DictOptionResponse;
import com.ai4.dict.service.DictQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 面向业务端的字典查询接口。 */
@RestController
@RequestMapping("/dicts")
public class DictQueryController {

    private final DictQueryService dictQueryService;

    public DictQueryController(DictQueryService dictQueryService) {
        this.dictQueryService = dictQueryService;
    }

    /** 根据字典编码查询当前可用的字典项。 */
    @GetMapping("/{dictCode}/items")
    public ApiResponse<List<DictOptionResponse>> getOptions(
            @PathVariable String dictCode
    ) {
        return ApiResponse.success(dictQueryService.getOptions(dictCode));
    }
}
