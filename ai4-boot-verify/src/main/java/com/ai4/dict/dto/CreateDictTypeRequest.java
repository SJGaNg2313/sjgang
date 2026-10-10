package com.ai4.dict.dto;

import jakarta.validation.constraints.*;

public record CreateDictTypeRequest (

        @NotBlank(message = "字典名称不能为空")
        @Size(max = 100,message = "字典名称长度不能超过100个字符")
        String dictName,

        @NotBlank(message = "字典编码不能为空")
        @Size(max = 100,message = "字典编码长度不能超过100个字符")
        @Pattern(
                regexp = "^[a-z][a-z0-9_]*$",
                message = "字典编码只能使用小写字母、数字和下划线，并且必须以字母开头"
        )
        String dictCode,

        @Min(value = 0,message = "状态只能是0或者1")
        @Max(value = 1,message = "状态只能是0或者1")
        Integer status,

        @Size(max = 500, message = "备注长度不能超过500个字符")
        String remark
){
}
