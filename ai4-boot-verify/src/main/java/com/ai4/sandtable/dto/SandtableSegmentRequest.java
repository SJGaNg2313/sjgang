package com.ai4.sandtable.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 沙盘视频分段请求。 */
public record SandtableSegmentRequest(
        @DecimalMin(value = "0.00", message = "分段时间不能小于0")
        @Digits(integer = 8, fraction = 2, message = "分段时间最多8位整数和2位小数")
        BigDecimal segmentTime,

        @Size(max = 50, message = "分段名称不能超过50个字符")
        String segmentName
) {
}
