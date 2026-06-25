package org.example.classAssignment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiEvaluationService {

    public static class AiEvaluationResult {
        private final Integer score;
        private final String comment;

        public AiEvaluationResult(Integer score, String comment) {
            this.score = score;
            this.comment = comment;
        }

        public Integer getScore() {
            return score;
        }

        public String getComment() {
            return comment;
        }
    }

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiEvaluationResult evaluate(String title, String content, Integer totalScore, String submitContent) {
        try {
            String prompt = buildPrompt(title, content, totalScore, submitContent);
            String response = callDeepSeekApi(prompt);
            return parseResponse(response, totalScore);
        } catch (Exception e) {
            System.out.println("AI评价调用失败: " + e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String title, String content, Integer totalScore, String submitContent) {
        return "你是一位专业的教育评估助手。请根据以下作业要求和学生提交内容进行评分。\n\n" +
                "作业标题：" + title + "\n" +
                "作业要求：" + content + "\n" +
                "满分：" + totalScore + "分\n\n" +
                "学生提交内容：" + submitContent + "\n\n" +
                "请严格按照以下JSON格式返回，不要返回其他内容：\n" +
                "{\"score\": 分数, \"comment\": \"详细评语\"}";
    }

    private String callDeepSeekApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        return response.getBody();
    }

    private AiEvaluationResult parseResponse(String response, Integer totalScore) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            int jsonStart = content.indexOf("{");
            int jsonEnd = content.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = content.substring(jsonStart, jsonEnd + 1);
                JsonNode result = objectMapper.readTree(jsonStr);
                int score = result.path("score").asInt();
                score = Math.max(0, Math.min(score, totalScore));
                String comment = result.path("comment").asText();
                return new AiEvaluationResult(score, comment);
            }
        } catch (Exception e) {
            System.out.println("解析AI响应失败: " + e.getMessage());
        }
        return null;
    }

    public String evaluateAndGetComment(String title, String content, Integer totalScore, String submitContent) {
        try {
            AiEvaluationResult result = evaluate(title, content, totalScore, submitContent);
            return result == null ? null : result.getComment();
        } catch (Exception e) {
            System.out.println("AI评价调用失败: " + e.getMessage());
            return null;
        }
    }
}
