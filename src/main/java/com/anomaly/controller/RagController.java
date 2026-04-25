package com.anomaly.controller;

import com.anomaly.service.RagAnswerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagAnswerService ragAnswerService;

    public RagController(RagAnswerService ragAnswerService) {
        this.ragAnswerService = ragAnswerService;
    }

    /**
     * Endpoint for querying the anomaly detection system using LLM + RAG.
     */
    @PostMapping("/query")
    public String query(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return "Please provide a valid question in the JSON body, example: { \"question\": \"What recent errors exist?\" }";
        }
        return ragAnswerService.answer(question);
    }
}
