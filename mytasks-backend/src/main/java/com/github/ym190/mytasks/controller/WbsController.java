package com.github.ym190.mytasks.controller;

import com.github.ym190.mytasks.dto.WbsRequest;
import com.github.ym190.mytasks.dto.WbsResponse;
import com.github.ym190.mytasks.service.GeminiService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
public class WbsController {
    private final GeminiService geminiService;

    public WbsController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    // フロントからのPOSTリクエストを受け付け、AIにWBSを作らせて返す
    @PostMapping("/api/wbs")
    public WbsResponse createWbs(@RequestBody WbsRequest request) {
        // フロントから受け取ったタスク名・時間をサービスに渡してGeminiを呼び出す
        return geminiService.generateWbs(request);
    }
}
