package com.ai4.ai.client.dto;

public record ArkResponseRequest (
    String model,
    //业务提示词
    String input,
    Thinking thinking
){
    /**
     * 模型思考配置
     */
    public record Thinking(
            String type
    ){}
}
