package com.whut.training.domain.entity;

import java.time.LocalDateTime;

/**
 * 推题池实体。
 *
 * <p>记录用户提交的推题信息，包含审核状态、排序和推送元数据。submitterUsername 为冗余展示字段，由查询时 JOIN users 表填充。
 */
public record PushPoolItem(
        Long id,
        String title,
        String link,
        String description,
        Long submitterId,
        String submitterUsername,
        String status,
        Integer sortOrder,
        LocalDateTime createdAt,
        Long approvedBy,
        LocalDateTime approvedAt
) {
}
