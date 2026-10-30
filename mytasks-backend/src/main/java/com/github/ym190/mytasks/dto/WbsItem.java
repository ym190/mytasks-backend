package com.github.ym190.mytasks.dto;

public record WbsItem(
        String task,
        int estimatedTime,
        String timeUnit,
        String priority
) {}