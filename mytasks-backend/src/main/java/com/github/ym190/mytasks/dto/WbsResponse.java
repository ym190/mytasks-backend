package com.github.ym190.mytasks.dto;

import java.util.List;

public record WbsResponse(
        List<WbsItem> tasks
) {}