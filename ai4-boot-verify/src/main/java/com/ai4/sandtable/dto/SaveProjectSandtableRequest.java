package com.ai4.sandtable.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 创建或完整修改项目沙盘请求。 */
public record SaveProjectSandtableRequest(
        @Size(max = 1000, message = "视频地址不能超过1000个字符")
        String videoUrl,

        @NotBlank(message = "收益趋势图地址不能为空")
        @Size(max = 1000, message = "收益趋势图地址不能超过1000个字符")
        String trendUrl,

        List<@NotNull(message = "视频分段项不能为空") @Valid SandtableSegmentRequest> segments,

        @NotNull(message = "沙盘属性不能为空")
        @Size(min = 3, max = 6, message = "沙盘属性必须配置3至6个")
        List<@NotNull(message = "沙盘属性项不能为空") @Valid SandtableAttributeRequest> attributes,

        @NotNull(message = "预计学习收获不能为空")
        @Size(min = 2, max = 4, message = "预计学习收获必须配置2至4个")
        List<@NotNull(message = "预计学习收获项不能为空") @Valid SandtableGainRequest> gains
) {
}
