package com.github.ym190.mytasks.service;

import com.github.ym190.mytasks.dto.WbsItem;
import com.github.ym190.mytasks.dto.WbsRequest;
import com.github.ym190.mytasks.dto.WbsResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WbsResponse generateWbs(WbsRequest request) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=" + apiKey;

        // AIへ送るプロンプトを組み立て
        Map<String, Object> requestBody = getStringObjectMap(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String responseJson = restTemplate.postForObject(url, entity, String.class);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String aiText = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asString();

            // AIの出力を一度ツリー（JSON構造）として解析
            JsonNode aiNode = objectMapper.readTree(aiText);
            List<WbsItem> itemList;

            if (aiNode.isArray()) {
                // AIが配列 [ ... ] で返してきた場合
                itemList = objectMapper.readValue(aiText, new TypeReference<List<WbsItem>>() {});
            } else if (aiNode.has("tasks")) {
                // AIが指示通り { "tasks": [ ... ] } で返してきた場合
                itemList = objectMapper.readValue(aiNode.get("tasks").toString(), new TypeReference<List<WbsItem>>() {});
            } else {
                itemList = List.of();
            }

            // フロントが待っている { "tasks": [ ... ] } の形に包んで返す
            return new WbsResponse(itemList);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Gemini APIの呼び出しに失敗しました: " + e.getMessage());
        }
    }

    private static @NonNull Map<String, Object> getStringObjectMap(WbsRequest request) {
        String targetTime = request.targetHours() + request.timeUnit();
        String prompt = String.format(
                "タスク名「%s」を目標時間「%s」以内で完了できるようにWBS（サブタスク）に分解してください。" +
                        "子タスク名は簡潔にし、重要度を「高」「中」「低」のいずれかで設定してください。"+
                        "期限や他タスクへの影響が大きいものは「高」、通常の優先度は「中」、急がないものは「低」としてください。"+
                        "各子タスクの予想作業時間は、タスクの長さに応じて「h」または「min」から適切な単位を選んでください。"+
                        "例えば1時間以上の作業はh、1時間未満の作業はminを使い、数値と単位を別々に返してください。"+
                        "子タスク全体の合計が約「%s」になるように配分してください。" +
                        "必ず以下のJSON形式のみで出力してください。Markdownのコードブロック（```json）や余計な解説文・挨拶は一切含めないでください。\n" +
                        "{\"tasks\": [" +
                        "{\"task\": \"サブタスク名\", \"estimatedTime\": 2, \"timeUnit\": \"%s\", \"priority\": \"高\"}" +
                        "]}",
                request.taskName(),
                targetTime,
                targetTime,
                request.timeUnit()
        );

        // Gemini APIの要求フォーマット（JSON形式での出力を指定）
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );
        return requestBody;
    }
}