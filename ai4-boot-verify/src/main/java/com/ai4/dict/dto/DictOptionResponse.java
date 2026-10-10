package com.ai4.dict.dto;

import tools.jackson.databind.JsonNode;

public record DictOptionResponse (
        //前端展示内容
        String label,
        //业务提交保存的值
        String value,
        //附加展示配置
        JsonNode extra
){
}
