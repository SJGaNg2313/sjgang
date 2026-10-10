package com.ai4.generation.model;
import java.time.LocalDateTime;
/** 项目生成配置数据库模型。 */
public record ProjectGeneration(Long id, Long projectId, String scoringCriteria,
                                LocalDateTime createdAt, LocalDateTime updatedAt) { }
