package com.example.demo.controller;

import com.example.demo.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    @PostMapping("/translate")
    public Map<String, Object> translate(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String text = request.get("text");
            String targetLang = request.get("targetLang");
            if (text == null || text.isEmpty()) {
                response.put("code", "1");
                response.put("msg", "文本不能为空");
                return response;
            }
            
            String translatedText = translationService.translate(text, targetLang);
            response.put("code", "0");
            response.put("msg", "success");
            response.put("data", translatedText);
        } catch (Exception e) {
            response.put("code", "1");
            response.put("msg", "翻译失败：" + e.getMessage());
        }
        return response;
    }
} 