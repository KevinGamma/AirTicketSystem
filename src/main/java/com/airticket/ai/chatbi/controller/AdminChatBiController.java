package com.airticket.ai.chatbi.controller;

import com.airticket.ai.chatbi.dto.ChatBiQueryRequest;
import com.airticket.ai.chatbi.dto.ChatBiQueryResponse;
import com.airticket.ai.chatbi.service.ChatBiAnalysisService;
import com.airticket.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/chat-bi")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatBiController {

    private final ChatBiAnalysisService chatBiAnalysisService;

    public AdminChatBiController(ChatBiAnalysisService chatBiAnalysisService) {
        this.chatBiAnalysisService = chatBiAnalysisService;
    }

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ChatBiQueryResponse>> query(@Valid @RequestBody ChatBiQueryRequest request) {
        try {
            ChatBiQueryResponse response = chatBiAnalysisService.analyze(request.question());
            return ResponseEntity.ok(ApiResponse.success("智能分析完成", response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("智能分析执行失败: " + ex.getMessage()));
        }
    }
}
