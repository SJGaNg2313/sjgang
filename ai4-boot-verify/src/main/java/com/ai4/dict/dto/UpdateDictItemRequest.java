package com.ai4.dict.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 完整修改字典项请求。 */
public record UpdateDictItemRequest(
        @NotBlank(message = "字典项名称不能为空")
        @Size(max = 100, message = "字典项名称不能超过100个字符")
        String itemLabel,

        @NotBlank(message = "字典项值不能为空")
        @Size(max = 500, message = "字典项值不能超过500个字符")
        String itemValue,

        @NotNull(message = "显示顺序不能为空")
        @Min(value = 0, message = "显示顺序不能小于0")
        Integer sortOrder,

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态只能是0或者1")
        @Max(value = 1, message = "状态只能是0或者1")
        Integer status,

        JsonNode extra,

        @Size(max = 500, message = "备注不能超过500个字符")
        String remark
) {
}
