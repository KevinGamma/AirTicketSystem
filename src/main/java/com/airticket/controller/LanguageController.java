package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/language")
@CrossOrigin(origins = "*")
public class LanguageController {

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentLanguage() {
        Locale currentLocale = LocaleContextHolder.getLocale();
        Map<String, Object> response = new HashMap<>();
        response.put("language", currentLocale.getLanguage());
        response.put("locale", currentLocale.toString());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/switch")
    public ResponseEntity<ApiResponse<String>> switchLanguage(@RequestParam String lang) {
        Locale locale;
        switch (lang.toLowerCase()) {
            case "en":
                locale = Locale.ENGLISH;
                break;
            case "zh":
            default:
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
        }
        
        
        LocaleContextHolder.setLocale(locale);
        
        return ResponseEntity.ok(ApiResponse.success("Language switched to " + locale.getLanguage()));
    }
}