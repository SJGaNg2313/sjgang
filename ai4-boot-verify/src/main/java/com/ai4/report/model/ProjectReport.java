package com.ai4.report.model;
import java.time.LocalDateTime;
/** 创业报告配置数据库模型。 */
public record ProjectReport(Long id, Long projectId, String reviewCriteria,
                            LocalDateTime createdAt, LocalDateTime updatedAt) { }
