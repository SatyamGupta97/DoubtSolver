package com.project.doubt_solver.service.impl;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.doubt_solver.model.Users;
import com.project.doubt_solver.payloads.QuestionDto;
import com.project.doubt_solver.payloads.QuizDto;
import com.project.doubt_solver.repository.UserRepository;
import com.project.doubt_solver.service.AIService;

@Slf4j
@Service
public class AIServiceImpl implements AIService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private final WebClient webClient = WebClient.create("http://localhost:11434");

    private String queryOllama(String systemInstruction, String userPrompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "llama3.1",
                    "system", systemInstruction,
                    "prompt", userPrompt,
                    "stream", false
            );

            Map<?, ?> response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String getAnswer(QuestionDto questionDto) {
        String email = questionDto.getEmail();
        String question = questionDto.getQuestion();

        Users user = userRepository.findByEmail(email);

        if (user.getDoubtCount() >= 100 && user.getRole().equals("normal")) {
            return "You’ve exceeded your free usage limit for today. Please upgrade to a standard subscription to continue.";
        }

        if (!checkQuestion(question)) {
            return "Your doubt is not related to Academics";
        }

        String systemInstruction = "You are an expert academic doubt solver. " +
                "Only answer questions related to school, college, math, science, and programming. " +
                "If unrelated, refuse to answer politely. Do not echo the question. " +
                "Provide clear, step-by-step explanations.";

        return queryOllama(systemInstruction, question);
    }

    @Override
    public String buildPrompt(String question) {
        return "";
    }

    @Override
    public boolean checkQuestion(String question) {
        return true;
//        String systemInstruction = "You are a classification bot. " +
//                "Determine if the user's prompt is about academics, school, or learning. " +
//                "Respond strictly with the word 'true' or 'false'. Provide zero explanation.";
//
//        String answer = queryOllama(systemInstruction, question);
//        log.warn("Answer for CheckQuestion: {}", answer);
//        return answer.trim().equalsIgnoreCase("true");
    }

    @Override
    public boolean checkComment(String question, String comment) {
        String systemInstruction = "You are a moderation bot. " +
                "Determine if the provided comment is relevant to the provided question. " +
                "Respond strictly with the word 'true' or 'false'. Provide zero explanation.";

        String userPrompt = "Question: " + question + "\nComment: " + comment;

        String answer = queryOllama(systemInstruction, userPrompt);
        return answer.trim().equalsIgnoreCase("true");
    }

    @Override
    public List<QuizDto> generateQuiz(String topic) {
        String systemInstruction = "You are a JSON-only quiz generator. " +
                "Generate 10 multiple-choice questions on the requested topic. " +
                "Format the output strictly as a JSON array of objects with keys: " +
                "question, optionA, optionB, optionC, optionD, answer. " +
                "Do not output markdown block ticks (```json). Output raw JSON only.";

        String plainText = queryOllama(systemInstruction, topic);
        return parseQuestionsFromPlainText(plainText);
    }

    private List<QuizDto> parseQuestionsFromPlainText(String plainText) {
        try {
            plainText = plainText.trim();

            if (plainText.startsWith("```")) {
                plainText = plainText.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            return objectMapper.readValue(
                    plainText,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QuizDto.class)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}