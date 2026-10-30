package com.github.ym190.mytasks.dto;

public record WbsRequest(
        String taskName,
        Integer targetHours,
        String timeUnit
) {}