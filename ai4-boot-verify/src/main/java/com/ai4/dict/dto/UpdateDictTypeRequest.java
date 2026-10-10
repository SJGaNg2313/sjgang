package com.ai4.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDictTypeRequest (

        @NotBlank(message = "字典名称不能为空")
        @Size(max = 100,message = "字典名称长度不能超过100个字符")
        String dictName,

        @Size(max = 500, message = "备注不能超过500个字符")
        String remark
){
}
