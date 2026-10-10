package com.ai4.ai.client;

public interface AiClient {

    /**
     * 根据提示词生成文本
     *
     * @param prompt 提示词
     * @return 模型最终生成的文本
     */
    String generateText(String prompt);
}
