package com.ai4.ai;

import com.ai4.ai.client.AiClient;
import com.ai4.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Test {
    private final AiClient aiClient;

    public Test(AiClient aiClient) {
        this.aiClient = aiClient;
    }
    @GetMapping("/ai/test")
    public ApiResponse< String> test(){
        return ApiResponse.success(aiClient.generateText("随机生成10个公司名称"));
    }
}
